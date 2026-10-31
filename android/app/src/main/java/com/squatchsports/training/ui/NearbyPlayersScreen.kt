package com.squatchsports.training.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.squatchsports.training.data.OneOnOneStore
import com.squatchsports.training.nearby.NearbyConnectionState
import com.squatchsports.training.nearby.NearbyMatchService
import com.squatchsports.training.nearby.NearbyPeer
import com.squatchsports.training.nearby.nearbyRuntimePermissions
import com.squatchsports.training.shared.MatchSettings
import com.squatchsports.training.shared.NearbyMatchRole
import com.squatchsports.training.shared.PlayerSide
import com.squatchsports.training.shared.ScoringSystem

@Composable
fun NearbyPlayersScreen(store: OneOnOneStore, nearby: NearbyMatchService, contentPadding: PaddingValues, onOpenMatch: () -> Unit) {
    val context = LocalContext.current
    val permissions = remember { nearbyRuntimePermissions(Build.VERSION.SDK_INT) }
    var permissionError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmAbandon by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (permissions.all { granted[it] == true || ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            permissionError = null
            nearby.start(store.data.playerName)
        } else permissionError = "Nearby access was denied. Local 1v1 still works. Allow access in app settings to discover another phone."
    }
    var previousMatchId by remember { mutableStateOf(store.data.activeMatch?.id) }
    val matchId = store.data.activeMatch?.id
    LaunchedEffect(matchId) {
        if (matchId != null && matchId != previousMatchId && store.data.nearbySession?.matchId == matchId) onOpenMatch()
        previousMatchId = matchId
    }
    DisposableEffect(nearby) {
        onDispose { if (nearby.state.connectedPeer == null) nearby.stop() else nearby.stopSearching() }
    }
    NearbyPlayersContent(
        state = nearby.state, canHost = store.nearby.canHost, hasActiveMatch = store.data.activeMatch != null,
        contentPadding = contentPadding, permissionError = permissionError,
        onSearch = {
            permissionError = null
            if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
                nearby.start(store.data.playerName)
            else launcher.launch(permissions.toTypedArray())
        },
        onSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) },
        onConnect = nearby::connect, onDisconnect = nearby::stop, onAccept = nearby::accept, onReject = nearby::reject,
        onResume = onOpenMatch, onStart = { side, settings -> store.nearby.startMatch(side, settings) },
        canAbandon = store.data.nearbySession?.role == NearbyMatchRole.GUEST && store.data.activeMatch != null &&
            nearby.state.connectedPeer?.identity?.id != store.data.nearbySession?.peerId,
        onAbandon = { confirmAbandon = true },
    )
    if (confirmAbandon) AlertDialog(
        onDismissRequest = { confirmAbandon = false }, title = { Text("Abandon this local copy?") },
        text = { Text("This discards your queued plays and match on this phone only. It cannot cancel the opponent's game or save its result here. Use this only if you cannot reconnect to the host.") },
        confirmButton = { TextButton(onClick = { store.abandonNearbyMatch(); nearby.stop(); confirmAbandon = false }) { Text("Discard Local Copy") } },
        dismissButton = { TextButton(onClick = { confirmAbandon = false }) { Text("Keep Match") } },
    )
}

/** Stateless connection UI; tests never request permissions, advertise, or accept a real peer. */
@Composable
fun NearbyPlayersContent(
    state: NearbyConnectionState,
    canHost: Boolean,
    hasActiveMatch: Boolean,
    contentPadding: PaddingValues,
    permissionError: String?,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onConnect: (NearbyPeer) -> Unit,
    onDisconnect: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onResume: () -> Unit,
    onStart: (PlayerSide, MatchSettings) -> Unit,
    canAbandon: Boolean = false,
    onAbandon: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(state.status, style = MaterialTheme.typography.titleMedium)
        Text("Play another nearby Android phone without an internet connection. Both players need this app open, Bluetooth and Wi-Fi enabled.")
        Text("Nearby access is used only to find and connect to your opponent. Older Android versions also require Location to be enabled.", style = MaterialTheme.typography.bodySmall)
        if (permissionError != null || state.error != null) MaterialCard {
            Text(permissionError ?: state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            if (permissionError != null) TextButton(onClick = onSettings) { Text("Open App Settings") }
        }
        if (state.connectedPeer == null) {
            Button(onClick = onSearch, enabled = !state.searching && state.connectingEndpointId == null) { Text("Find Nearby Players") }
            if (state.searching) {
                Text("Looking for players…")
                OutlinedButton(onClick = onDisconnect) { Text("Stop Searching") }
            }
            state.peers.forEach { peer ->
                MaterialCard {
                    Text(peer.identity.name, fontWeight = FontWeight.Bold)
                    Button(onClick = { onConnect(peer) }, enabled = state.connectingEndpointId == null) { Text("Connect to ${peer.identity.name}") }
                }
            }
        } else {
            MaterialCard {
                Text("Opponent: ${state.connectedPeer.identity.name}", fontWeight = FontWeight.Bold)
                Text("Connection confirmed on both phones", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }
            if (!hasActiveMatch) {
                if (canHost) NearbyMatchOptions(onStart)
                else Text("Waiting for the host to start the match.", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (hasActiveMatch) {
            MaterialCard {
                Text("A match is already saved. Resume it, or end it before starting another.")
                Button(onClick = onResume) { Text("Resume Match") }
                if (canAbandon) TextButton(onClick = onAbandon) { Text("Cannot reconnect? Abandon local copy") }
            }
        }
        Text("Android-to-Android only. iPhones currently use a different, Apple-only connection system.", style = MaterialTheme.typography.bodySmall)
    }
    state.confirmation?.let { confirmation ->
        AlertDialog(onDismissRequest = onReject,
            title = { Text("Confirm nearby connection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connecting to ${confirmation.peer.identity.name}")
                    Text(confirmation.code, style = MaterialTheme.typography.displaySmall)
                    Text("Compare this code on both phones. Accept only if the codes match and this is your opponent.")
                }
            },
            confirmButton = { TextButton(onClick = onAccept) { Text("Codes Match — Accept") } },
            dismissButton = { TextButton(onClick = onReject) { Text("Reject") } },
        )
    }
}

@Composable
private fun NearbyMatchOptions(onStart: (PlayerSide, MatchSettings) -> Unit) {
    var side by rememberSaveable { mutableStateOf(PlayerSide.RED) }
    var scoring by rememberSaveable { mutableStateOf(ScoringSystem.ONES_AND_TWOS) }
    var target by rememberSaveable { mutableIntStateOf(11) }
    var winByTwo by rememberSaveable { mutableStateOf(true) }
    var makeItTakeIt by rememberSaveable { mutableStateOf(false) }
    var first by rememberSaveable { mutableStateOf(PlayerSide.RED) }
    Text("You host this game", style = MaterialTheme.typography.titleMedium)
    Text("Your color")
    PlayerSide.entries.forEach { RuleChoice(it.title, side == it) { side = it } }
    Text("Scoring")
    ScoringSystem.entries.forEach { value -> RuleChoice(value.title, scoring == value) { scoring = value; target = value.defaultTarget } }
    Row(Modifier.fillMaxWidth()) {
        Text("Play to $target", Modifier.weight(1f))
        OutlinedButton(onClick = { target-- }, enabled = target > MatchSettings.TARGET_RANGE.first) { Text("−") }
        OutlinedButton(onClick = { target++ }, enabled = target < MatchSettings.TARGET_RANGE.last) { Text("+") }
    }
    RuleSwitch("Win by 2", winByTwo) { winByTwo = it }
    RuleSwitch("Make it, take it", makeItTakeIt) { makeItTakeIt = it }
    Text("First ball")
    PlayerSide.entries.forEach { value -> RuleChoice(value.title + " starts", first == value) { first = value } }
    Button(onClick = { onStart(side, MatchSettings(scoring, target, winByTwo, makeItTakeIt, first)) }, modifier = Modifier.fillMaxWidth()) {
        Text("Start Nearby 1v1")
    }
}
