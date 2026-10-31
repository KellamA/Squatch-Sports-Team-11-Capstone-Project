package com.squatchsports.training.nearby

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.squatchsports.training.shared.NearbyMatchMessage
import com.squatchsports.training.shared.NearbyMatchPayloads
import com.squatchsports.training.shared.NearbyPlayerIdentity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class NearbyPeer(val endpointId: String, val identity: NearbyPlayerIdentity)
data class NearbyConfirmation(val peer: NearbyPeer, val code: String)
data class NearbyConnectionState(
    val searching: Boolean = false,
    val peers: List<NearbyPeer> = emptyList(),
    val connectingEndpointId: String? = null,
    val confirmation: NearbyConfirmation? = null,
    val connectedPeer: NearbyPeer? = null,
    val status: String = "Not connected",
    val error: String? = null,
)

/** One encrypted point-to-point connection. Neither discovery nor acceptance is automatic. */
class NearbyMatchService(context: Context) {
    private val appContext = context.applicationContext
    private val client = Nearby.getConnectionsClient(appContext)
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "NearbyMatchIO").apply { isDaemon = true } }
    private val preferences = appContext.getSharedPreferences("squatch_nearby", Context.MODE_PRIVATE)
    private val deviceId = preferences.getString("deviceId", null) ?: UUID.randomUUID().toString().also {
        check(preferences.edit().putString("deviceId", it).commit())
    }
    private var localName = "You"
    private var generation = 0
    private var lifecycle: ConnectionLifecycleCallback? = null
    private var candidate: NearbyPeer? = null
    private var candidateIsHost = false
    private val incomingStreams = ConcurrentHashMap<Long, InputStream>()
    private val outgoingStreams = ConcurrentHashMap<Long, InputStream>()
    private var connectionTimeout: Runnable? = null

    var state by mutableStateOf(NearbyConnectionState())
        private set
    var onConnected: ((NearbyPlayerIdentity, Boolean) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onMessage: ((String, NearbyMatchMessage) -> Unit)? = null
    var onSynchronize: (() -> Unit)? = null

    private val retry = object : Runnable {
        override fun run() {
            if (state.connectedPeer == null) return
            onSynchronize?.invoke()
            main.postDelayed(this, 5_000)
        }
    }

    fun start(displayName: String) {
        if (state.connectedPeer != null || state.connectingEndpointId != null) return
        if (nearbyRuntimePermissions(Build.VERSION.SDK_INT).any {
                ContextCompat.checkSelfPermission(appContext, it) != PackageManager.PERMISSION_GRANTED
            }) {
            state = state.copy(error = "Allow the nearby permissions before searching.")
            return
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) != ConnectionResult.SUCCESS) {
            state = state.copy(error = "Google Play services must be installed and up to date.")
            return
        }
        stop()
        localName = displayName.trim().ifBlank { "You" }
        val token = generation
        lifecycle = connectionCallbacks(token)
        state = NearbyConnectionState(searching = true, status = "Looking for nearby Android players")
        val name = NearbyMatchPayloads.identity(NearbyPlayerIdentity(deviceId, localName))
        try {
            client.startAdvertising(name, SERVICE_ID, lifecycle!!,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
                .addOnFailureListener { failSearch(token, it) }
            client.startDiscovery(SERVICE_ID, discoveryCallbacks(token),
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
                .addOnFailureListener { failSearch(token, it) }
        } catch (error: SecurityException) { failSearch(token, error) }
    }

    fun connect(peer: NearbyPeer) {
        val callback = lifecycle ?: return
        if (!state.searching || state.connectedPeer != null || state.connectingEndpointId != null) return
        val token = generation
        candidate = peer
        candidateIsHost = true
        state = state.copy(connectingEndpointId = peer.endpointId, error = null, status = "Connecting to ${peer.identity.name}")
        scheduleTimeout(token, peer.endpointId)
        client.requestConnection(NearbyMatchPayloads.identity(NearbyPlayerIdentity(deviceId, localName)), peer.endpointId, callback)
            .addOnFailureListener { if (token == generation && candidate?.endpointId == peer.endpointId) connectionFailed(it) }
    }

    fun accept() {
        val confirmation = state.confirmation ?: return
        val token = generation
        state = state.copy(confirmation = null, status = "Waiting for the other player to confirm")
        client.acceptConnection(confirmation.peer.endpointId, payloadCallbacks(token))
            .addOnFailureListener { if (token == generation) connectionFailed(it) }
    }

    fun reject() {
        val endpoint = candidate?.endpointId ?: return
        client.rejectConnection(endpoint)
        clearPending()
        state = state.copy(connectingEndpointId = null, confirmation = null, status = "Connection declined")
    }

    /** Stop advertising on leaving the screen, but keep an already-confirmed game connection. */
    fun stopSearching() {
        runCatching { client.stopAdvertising(); client.stopDiscovery() }
        state = state.copy(searching = false, peers = emptyList())
    }

    fun stop() {
        generation++
        main.removeCallbacks(retry)
        clearPending()
        runCatching { client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints() }
        incomingStreams.values.forEach { runCatching { it.close() } }; incomingStreams.clear()
        outgoingStreams.values.forEach { runCatching { it.close() } }; outgoingStreams.clear()
        val wasConnected = state.connectedPeer != null
        state = NearbyConnectionState()
        lifecycle = null
        if (wasConnected) onDisconnected?.invoke()
    }

    fun send(peerId: String, message: NearbyMatchMessage) {
        val peer = state.connectedPeer?.takeIf { it.identity.id == peerId } ?: return
        val token = generation
        val bytes = runCatching { NearbyMatchPayloads.encode(message) }.getOrElse {
            state = state.copy(error = "This game is too large to sync. Keep the saved game and contact support.")
            return
        }
        // Nearby BYTES payloads are limited to 32 KiB. Long event logs use a bounded stream instead.
        val payload = if (bytes.size <= 32 * 1024) Payload.fromBytes(bytes) else {
            if (outgoingStreams.size >= 2) return // Periodic synchronization retries the newest state.
            val stream = ByteArrayInputStream(bytes)
            Payload.fromStream(stream).also { outgoingStreams[it.id] = stream }
        }
        client.sendPayload(peer.endpointId, payload).addOnFailureListener {
            outgoingStreams.remove(payload.id)?.close()
            if (token == generation) state = state.copy(error = "Sync interrupted. Plays are saved and will retry.")
            Log.w(TAG, "Nearby send failed", it)
        }
    }

    private fun discoveryCallbacks(token: Int) = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val identity = NearbyMatchPayloads.decodeIdentity(info.endpointName) ?: return
            main.post {
                if (token != generation || !state.searching || identity.id == deviceId) return@post
                state = state.copy(peers = state.peers.filterNot { it.endpointId == endpointId } + NearbyPeer(endpointId, identity))
            }
        }
        override fun onEndpointLost(endpointId: String) {
            main.post { if (token == generation) state = state.copy(peers = state.peers.filterNot { it.endpointId == endpointId }) }
        }
    }

    private fun connectionCallbacks(token: Int) = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val identity = NearbyMatchPayloads.decodeIdentity(info.endpointName)
            main.post {
                if (token != generation) return@post
                if (identity == null || identity.id == deviceId || info.authenticationDigits.isBlank() || state.connectedPeer != null ||
                    (candidate != null && candidate?.endpointId != endpointId)) {
                    client.rejectConnection(endpointId)
                    return@post
                }
                val peer = NearbyPeer(endpointId, identity)
                candidate = peer
                candidateIsHost = !info.isIncomingConnection
                state = state.copy(connectingEndpointId = endpointId, confirmation = NearbyConfirmation(peer, info.authenticationDigits),
                    status = "Compare the connection code on both phones", error = null)
                scheduleTimeout(token, endpointId)
            }
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            main.post {
                if (token != generation || candidate?.endpointId != endpointId) return@post
                val peer = candidate!!
                val isHost = candidateIsHost
                clearPending()
                if (result.status.isSuccess) {
                    stopSearching()
                    state = state.copy(connectedPeer = peer, connectingEndpointId = null, confirmation = null,
                        status = "Connected to ${peer.identity.name}", error = null)
                    onConnected?.invoke(peer.identity, isHost)
                    main.removeCallbacks(retry)
                    main.postDelayed(retry, 5_000)
                } else {
                    state = state.copy(connectingEndpointId = null, confirmation = null,
                        status = "Not connected", error = "Connection was declined or failed. Try searching again.")
                }
            }
        }
        override fun onDisconnected(endpointId: String) {
            main.post {
                if (token != generation) return@post
                if (state.connectedPeer?.endpointId == endpointId) {
                    main.removeCallbacks(retry)
                    incomingStreams.values.forEach { runCatching { it.close() } }; incomingStreams.clear()
                    outgoingStreams.values.forEach { runCatching { it.close() } }; outgoingStreams.clear()
                    state = state.copy(connectedPeer = null, status = "Disconnected — your match is saved")
                    onDisconnected?.invoke()
                } else if (candidate?.endpointId == endpointId) connectionFailed(IllegalStateException("Disconnected before confirmation"))
            }
        }
    }

    private fun payloadCallbacks(token: Int) = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val peer = state.connectedPeer?.takeIf { it.endpointId == endpointId } ?: return
            val stream = if (payload.type == Payload.Type.STREAM) payload.asStream()?.asInputStream() else null
            if (stream != null) {
                if (incomingStreams.size >= 2) { stream.close(); return }
                incomingStreams[payload.id] = stream
                main.postDelayed({ incomingStreams.remove(payload.id)?.let { runCatching { it.close() } } }, 30_000)
            }
            io.execute {
                val bytes = runCatching {
                    if (stream != null) stream.use { readBounded(it) } else payload.asBytes()
                }.getOrNull()
                incomingStreams.remove(payload.id)
                val message = bytes?.let(NearbyMatchPayloads::decode)
                main.post {
                    if (token != generation || state.connectedPeer?.endpointId != endpointId) return@post
                    if (message == null) state = state.copy(error = "Unsupported or invalid nearby data. Update both apps.")
                    else {
                        state = state.copy(error = null)
                        runCatching { onMessage?.invoke(peer.identity.id, message) }.onFailure {
                            Log.w(TAG, "Unable to save nearby play", it)
                            state = state.copy(error = "Unable to save this play. It will retry after reconnecting.")
                        }
                    }
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                outgoingStreams.remove(update.payloadId)?.let { runCatching { it.close() } }
                if (update.status != PayloadTransferUpdate.Status.SUCCESS) {
                    incomingStreams.remove(update.payloadId)?.let { runCatching { it.close() } }
                }
            }
        }
    }

    private fun scheduleTimeout(token: Int, endpointId: String) {
        connectionTimeout?.let(main::removeCallbacks)
        connectionTimeout = Runnable {
            if (token == generation && candidate?.endpointId == endpointId) {
                client.disconnectFromEndpoint(endpointId)
                connectionFailed(IllegalStateException("Confirmation timed out"))
            }
        }.also { main.postDelayed(it, 45_000) }
    }

    private fun clearPending() {
        connectionTimeout?.let(main::removeCallbacks)
        connectionTimeout = null
        candidate = null
    }

    private fun connectionFailed(error: Exception) {
        Log.w(TAG, "Nearby connection failed", error)
        candidate?.endpointId?.let { client.disconnectFromEndpoint(it) }
        clearPending()
        state = state.copy(connectingEndpointId = null, confirmation = null, status = "Not connected",
            error = "Could not connect. Check Bluetooth, Wi-Fi and permissions, then search again.")
    }

    private fun failSearch(token: Int, error: Exception) {
        if (token != generation || !state.searching || state.connectedPeer != null) return
        Log.w(TAG, "Nearby discovery failed", error)
        stop()
        state = state.copy(error = "Could not search. Enable Bluetooth/Wi-Fi and allow nearby permissions. On Android 12L or older, also enable Location.")
    }

    private fun readBounded(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            require(output.size() + count <= NearbyMatchPayloads.MAX_PAYLOAD_BYTES)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private companion object {
        const val SERVICE_ID = "com.squatchsports.training.nearby.v1"
        const val TAG = "NearbyMatch"
    }
}
