package com.squatchsports.training.connectivity

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.squatchsports.training.shared.CelebrationMessage
import com.squatchsports.training.shared.CourtPosition
import com.squatchsports.training.shared.GamificationPayloads
import com.squatchsports.training.shared.WatchStats
import com.squatchsports.training.shared.WorkoutPaths
import com.squatchsports.training.shared.WorkoutPayloads

data class ShotEvent(val value: Int, val sequence: Int)
data class PositionEvent(val position: CourtPosition, val sequence: Int)
data class DrillEvent(val name: String, val sequence: Int)
data class CelebrationEvent(val message: CelebrationMessage, val sequence: Int)

class WorkoutConnectivity(context: Context) :
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {
    private val messageClient = Wearable.getMessageClient(context.applicationContext)
    private val nodeClient = Wearable.getNodeClient(context.applicationContext)
    private val dataClient = Wearable.getDataClient(context.applicationContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Latest streak/goal snapshot from the phone, or null until one arrives. */
    var stats by mutableStateOf<WatchStats?>(null)
        private set
    var celebrationEvent by mutableStateOf<CelebrationEvent?>(null)
        private set

    var workoutActive by mutableStateOf(false)
        private set
    var workoutSequence by mutableIntStateOf(0)
        private set
    var shotEvent by mutableStateOf<ShotEvent?>(null)
        private set
    var positionEvent by mutableStateOf<PositionEvent?>(null)
        private set
    var drillEvent by mutableStateOf<DrillEvent?>(null)
        private set

    fun startListening() {
        messageClient.addListener(this)
        dataClient.addListener(this)
        loadSavedStats()
    }

    fun stopListening() {
        messageClient.removeListener(this)
        dataClient.removeListener(this)
    }

    /** Reads the stats the phone last published, so the watch shows them right away on launch. */
    private fun loadSavedStats() {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(WorkoutPaths.STATS).build()
        dataClient.getDataItems(uri).addOnSuccessListener { items ->
            items.forEach { item -> item.data?.let(::applyStats) }
            items.release()
        }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            val item = event.dataItem
            if (event.type == DataEvent.TYPE_CHANGED && item.uri.path == WorkoutPaths.STATS) {
                item.data?.let(::applyStats)
            }
        }
    }

    private fun applyStats(data: ByteArray) {
        Log.d(COMM_TAG, "watch <= data   ${WorkoutPaths.STATS}  ${data.decodeToString()}")
        GamificationPayloads.decodeStats(data)?.let { decoded -> mainHandler.post { stats = decoded } }
    }

    fun sendShotToPhone(value: Int) {
        sendMessage(WorkoutPaths.SHOT, WorkoutPayloads.shot(value))
    }

    fun sendPositionUpdate(position: CourtPosition) {
        sendMessage(WorkoutPaths.POSITION, WorkoutPayloads.position(position))
    }

    fun sendWorkoutStopped() {
        sendMessage(WorkoutPaths.WORKOUT_STOPPED)
    }

    override fun onMessageReceived(event: MessageEvent) {
        Log.d(COMM_TAG, "watch <- phone  ${event.path}  ${event.data.decodeToString()}")
        mainHandler.post {
            when (event.path) {
                WorkoutPaths.WORKOUT_STARTED -> {
                    workoutActive = true
                    workoutSequence += 1
                }
                WorkoutPaths.WORKOUT_STOPPED -> {
                    workoutActive = false
                    workoutSequence += 1
                }
                WorkoutPaths.SHOT -> WorkoutPayloads.decodeShot(event.data)?.let { value ->
                    shotEvent = ShotEvent(value, (shotEvent?.sequence ?: 0) + 1)
                }
                WorkoutPaths.POSITION -> WorkoutPayloads.decodePosition(event.data)?.let { position ->
                    positionEvent = PositionEvent(position, (positionEvent?.sequence ?: 0) + 1)
                }
                WorkoutPaths.DRILL_INFO -> {
                    drillEvent = DrillEvent(
                        WorkoutPayloads.decodeDrill(event.data),
                        (drillEvent?.sequence ?: 0) + 1,
                    )
                }
                WorkoutPaths.CELEBRATION -> GamificationPayloads.decodeCelebration(event.data)?.let { message ->
                    celebrationEvent = CelebrationEvent(message, (celebrationEvent?.sequence ?: 0) + 1)
                }
            }
        }
    }

    private fun sendMessage(path: String, data: ByteArray = byteArrayOf()) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, data)
                        .addOnSuccessListener {
                            Log.d(COMM_TAG, "watch -> phone  $path  ${data.decodeToString()}")
                        }
                        .addOnFailureListener { error ->
                            Log.w(TAG, "Unable to send $path to ${node.displayName}", error)
                        }
                }
            }
            .addOnFailureListener { error -> Log.w(TAG, "Unable to find connected phone", error) }
    }

    private companion object {
        const val TAG = "WorkoutConnectivity"

        /** Filter Logcat by this tag to see every phone/watch message. */
        const val COMM_TAG = "SquatchComm"
    }
}
