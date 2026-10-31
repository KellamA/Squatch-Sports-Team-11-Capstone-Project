package com.squatchsports.training.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squatchsports.training.data.OneOnOneStore
import com.squatchsports.training.shared.MatchCommandAction
import com.squatchsports.training.shared.MatchEvent
import com.squatchsports.training.shared.MatchEventType
import com.squatchsports.training.shared.MatchSettings
import com.squatchsports.training.shared.MatchState
import com.squatchsports.training.shared.OneOnOneMatch
import com.squatchsports.training.shared.PlayerSide
import com.squatchsports.training.shared.ScoringSystem
import java.util.Date
import kotlin.random.Random

private fun PlayerSide.color(): Color = if (this == PlayerSide.RED) Color(0xFFE83845) else Color(0xFF2978F5)

@Composable
fun OneOnOneHomeScreen(
    store: OneOnOneStore,
    contentPadding: PaddingValues,
    onSetup: (String?) -> Unit,
    onResume: () -> Unit,
    onNearby: () -> Unit = {},
) {
    val data = store.data
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var removeId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MaterialCard {
            Text("Your 1v1 Record", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { draft = data.playerName; dialog = "name"; error = null }) {
                Text("Your name: ${data.playerName} · Edit")
            }
            val record = data.overallRecord
            Text("${record.wins} Wins   ·   ${record.losses} Losses   ·   ${record.winPercentage}% Win Rate")
        }
        data.activeMatch?.let { match ->
            MaterialCard {
                Text("Match in progress", fontWeight = FontWeight.Bold)
                Text("${match.name(PlayerSide.RED, data.playerName)} ${match.state.red.points} – ${match.state.blue.points} ${match.name(PlayerSide.BLUE, data.playerName)}")
                Button(onClick = onResume) { Text(if (match.state.isOver) "Review result" else "Resume Match") }
                if (data.nearbySession?.pendingCommands?.any { it.action == MatchCommandAction.CANCEL } == true) {
                    Text("Cancellation is queued. Reconnect to the host to finish cancelling on both phones.")
                }
            }
        }
        OutlinedButton(onClick = onNearby, modifier = Modifier.fillMaxWidth()) { Text("Nearby Players") }
        Button(onClick = { onSetup(null) }, enabled = data.friends.isNotEmpty() && data.activeMatch == null) {
            Text("New Match")
        }
        Text("Friends", style = MaterialTheme.typography.titleLarge)
        if (data.friends.isEmpty()) Text("Add the people you play against, then start a match.")
        data.friends.forEach { friend ->
            MaterialCard {
                Text(friend.name, fontWeight = FontWeight.Bold)
                val record = data.recordAgainst(friend.id)
                Text(if (record.games == 0) "No games yet" else "You're ${record.text}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSetup(friend.id) }, enabled = data.activeMatch == null) { Text("Play ${friend.name}") }
                    TextButton(onClick = { removeId = friend.id }) { Text("Remove") }
                }
            }
        }
        OutlinedButton(onClick = { dialog = "friend"; draft = ""; error = null }) { Text("Add Friend") }
        Text("Friends are stored on this phone, not linked to online accounts.", style = MaterialTheme.typography.bodySmall)
        if (data.matches.isNotEmpty()) {
            Text("Recent Games", style = MaterialTheme.typography.titleLarge)
            data.matches.take(10).forEach { match ->
                MaterialCard {
                    Text("${if (match.userWon == true) "W" else "L"}   ${match.state[match.userSide].points} – ${match.state[match.userSide.opponent].points} vs ${match.opponentName}", fontWeight = FontWeight.Bold)
                    val date = Date(match.endDateMillis ?: match.startDateMillis)
                    Text("${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}")
                    Text("Fouls ${match.state[match.userSide].fouls} · Turnovers ${match.state[match.userSide].turnovers} · Steals ${match.state[match.userSide].steals}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (dialog != null) AlertDialog(
        onDismissRequest = { dialog = null },
        title = { Text(if (dialog == "friend") "Add Friend" else "Your Name") },
        text = {
            Column {
                OutlinedTextField(value = draft, onValueChange = { draft = it; error = null }, label = { Text("Name") }, singleLine = true, isError = error != null)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (draft.isBlank()) error = "Enter a name."
                else if (dialog == "friend" && store.addFriend(draft) == null) error = "That friend already exists."
                else {
                    if (dialog == "name") store.setPlayerName(draft)
                    dialog = null
                }
            }) { Text(if (dialog == "friend") "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
    )
    if (removeId != null) AlertDialog(
        onDismissRequest = { removeId = null }, title = { Text("Remove friend?") },
        text = { Text("Past matches and any match in progress will be kept.") },
        confirmButton = { TextButton(onClick = { store.removeFriend(removeId!!); removeId = null }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removeId = null }) { Text("Cancel") } },
    )
}

@Composable
fun MatchSetupScreen(store: OneOnOneStore, initialOpponentId: String?, contentPadding: PaddingValues, onStart: () -> Unit) {
    var opponentId by rememberSaveable { mutableStateOf(initialOpponentId ?: store.data.friends.firstOrNull()?.id) }
    var userSide by rememberSaveable { mutableStateOf(PlayerSide.RED) }
    var scoring by rememberSaveable { mutableStateOf(ScoringSystem.ONES_AND_TWOS) }
    var target by rememberSaveable { mutableIntStateOf(11) }
    var winByTwo by rememberSaveable { mutableStateOf(true) }
    var makeItTakeIt by rememberSaveable { mutableStateOf(false) }
    var firstPossession by rememberSaveable { mutableStateOf(PlayerSide.RED) }
    val opponent = store.data.friends.firstOrNull { it.id == opponentId }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Opponent", style = MaterialTheme.typography.titleMedium)
        store.data.friends.forEach { friend ->
            RuleChoice(friend.name, friend.id == opponentId) { opponentId = friend.id }
        }
        HorizontalDivider()
        Text("Your color", style = MaterialTheme.typography.titleMedium)
        PlayerSide.entries.forEach { side -> RuleChoice(side.title, userSide == side) { userSide = side } }
        Text("${opponent?.name ?: "Your opponent"} will be ${userSide.opponent.title}.")
        HorizontalDivider()
        Text("Scoring", style = MaterialTheme.typography.titleMedium)
        ScoringSystem.entries.forEach { system ->
            RuleChoice(system.title, scoring == system) { scoring = system; target = system.defaultTarget }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Play to $target", Modifier.weight(1f))
            OutlinedButton(onClick = { target-- }, enabled = target > MatchSettings.TARGET_RANGE.first,
                modifier = Modifier.semantics { contentDescription = "Decrease target score" }) { Text("−") }
            OutlinedButton(onClick = { target++ }, enabled = target < MatchSettings.TARGET_RANGE.last,
                modifier = Modifier.semantics { contentDescription = "Increase target score" }) { Text("+") }
        }
        RuleSwitch("Win by 2", winByTwo) { winByTwo = it }
        RuleSwitch("Make it, take it", makeItTakeIt) { makeItTakeIt = it }
        Text("First ball", style = MaterialTheme.typography.titleMedium)
        PlayerSide.entries.forEach { side ->
            val name = if (side == userSide) store.data.playerName else opponent?.name ?: "Opponent"
            RuleChoice("$name (${side.title})", firstPossession == side) { firstPossession = side }
        }
        OutlinedButton(onClick = { firstPossession = if (Random.nextBoolean()) PlayerSide.RED else PlayerSide.BLUE }) { Text("Flip a coin") }
        Button(
            onClick = {
                if (opponent != null && store.startMatch(opponent.id, userSide,
                        MatchSettings(scoring, target, winByTwo, makeItTakeIt, firstPossession))) onStart()
            },
            enabled = opponent != null && store.data.activeMatch == null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start Match") }
    }
}

@Composable
internal fun RuleChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(label) }
    }
}

@Composable
internal fun RuleSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
fun LiveMatchScreen(store: OneOnOneStore, contentPadding: PaddingValues, onLeave: () -> Unit, onDone: () -> Unit,
    nearbyStatus: String? = null) {
    val match = store.data.activeMatch
    LaunchedEffect(match?.id) { if (match == null) onDone() }
    if (match == null) return
    val state = match.state
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        store.data.nearbySession?.takeIf { it.matchId == match.id }?.let { session ->
            Text("Nearby ${session.role.name.lowercase()} · ${nearbyStatus ?: "Peer connection"}", style = MaterialTheme.typography.bodySmall)
            if (session.pendingCommands.isNotEmpty()) {
                Text("${session.pendingCommands.size} plays awaiting the host. Reconnect to sync.", color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(buildList {
            add("First to ${match.settings.targetScore}")
            add(match.settings.scoring.title)
            if (match.settings.winByTwo) add("Win by 2")
            if (match.settings.makeItTakeIt) add("Make it, take it")
        }.joinToString(" · "))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerSide.entries.forEach { side ->
                Column(
                    Modifier.weight(1f).background(side.color().copy(alpha = 0.18f)).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${match.name(side, store.data.playerName)}${if (state.possession == side) " 🏀" else ""}", fontWeight = FontWeight.Bold)
                    Text("${state[side].points}", style = MaterialTheme.typography.displayLarge, color = side.color(),
                        modifier = Modifier.semantics { contentDescription = "${side.title} score ${state[side].points}" })
                    Text("Fouls ${state[side].fouls} · TO ${state[side].turnovers}", style = MaterialTheme.typography.bodySmall)
                    if (state.isGamePoint(side, match.settings)) Text("GAME POINT", fontWeight = FontWeight.Bold)
                }
            }
        }
        MatchCourt(match, state, store.data.playerName)
        Text(match.events.lastOrNull()?.event?.summary { match.name(it, store.data.playerName) } ?: "Log your first play")
        if (!state.isOver) {
            PlayerSide.entries.forEach { side ->
                Text("${match.name(side, store.data.playerName)} · ${side.title}", color = side.color(), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(match.settings.scoring.insidePoints, match.settings.scoring.outsidePoints).forEach { points ->
                        Button(onClick = { store.record(MatchEvent(MatchEventType.SCORE, side, points)) },
                            colors = ButtonDefaults.buttonColors(containerColor = side.color()), modifier = Modifier.weight(1f)
                                .semantics { contentDescription = "${side.title} plus $points" }) { Text("+$points") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(MatchEventType.FOUL to "Foul", MatchEventType.TURNOVER to "TO", MatchEventType.STEAL to "Steal").forEach { (type, title) ->
                        OutlinedButton(onClick = { store.record(MatchEvent(type, side)) }, modifier = Modifier.weight(1f)
                            .semantics { contentDescription = "${side.title} ${type.name.lowercase()}" }) { Text(title) }
                    }
                }
            }
            OutlinedButton(onClick = { store.record(MatchEvent(MatchEventType.POSSESSION, state.possession.opponent)) }) {
                Text("Missed shot – ball to ${match.name(state.possession.opponent, store.data.playerName)}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = store::undo, enabled = match.events.isNotEmpty()) { Text("Undo last play") }
            TextButton(onClick = onLeave) { Text("Leave Match") }
        }
    }
    state.winner?.let { winner ->
        val saving = store.data.nearbySession?.pendingCommands?.any { it.action == MatchCommandAction.FINISH } == true
        AlertDialog(
            onDismissRequest = onLeave,
            title = { Text(if (match.userWon == true) "You win!" else "${match.name(winner, store.data.playerName)} wins!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${state[winner].points} – ${state[winner.opponent].points}", style = MaterialTheme.typography.headlineLarge)
                    val record = store.data.recordAgainst(match.opponentId)
                    Text("Your record vs ${match.opponentName}: ${record.wins + if (match.userWon == true) 1 else 0}-${record.losses + if (match.userWon == false) 1 else 0}")
                    Text("Red / Blue")
                    Text("Fouls: ${state.red.fouls} / ${state.blue.fouls}")
                    Text("Turnovers: ${state.red.turnovers} / ${state.blue.turnovers}")
                    Text("Steals: ${state.red.steals} / ${state.blue.steals}")
                    if (store.data.nearbySession != null) {
                        TextButton(onClick = onLeave) { Text("Pause / reconnect") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = store::finishMatch, enabled = !saving) { Text(if (saving) "Saving…" else "Save Match") } },
            dismissButton = { TextButton(onClick = store::undo, enabled = !saving) { Text("Undo last play") } },
        )
    }
}

/** Same half-court presentation as iOS: offense at the top of the key, defender closer to the hoop. */
@Composable
private fun MatchCourt(match: OneOnOneMatch, state: MatchState, playerName: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.3f).background(Color(0xFF17251D))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val line = Color.White.copy(alpha = 0.65f)
            val stroke = Stroke(2.dp.toPx())
            drawRect(line, Offset(w * 0.05f, h * 0.07f), Size(w * 0.9f, h * 0.88f), style = stroke)
            drawRect(line, Offset(w * 0.32f, h * 0.07f), Size(w * 0.36f, h * 0.36f), style = stroke)
            drawCircle(Color(0xFFFF9500), w * 0.035f, Offset(w * 0.5f, h * 0.15f), style = stroke)
            drawLine(line, Offset(w * 0.42f, h * 0.11f), Offset(w * 0.58f, h * 0.11f), stroke.width)
            drawArc(line, 0f, 180f, false, Offset(w * 0.32f, h * 0.31f), Size(w * 0.36f, h * 0.24f), style = stroke)
            drawArc(line, 0f, 180f, false, Offset(w * 0.10f, -h * 0.38f), Size(w * 0.80f, h * 1.16f), style = stroke)
        }
        PlayerSide.entries.forEach { side ->
            val hasBall = state.possession == side
            val x = if (hasBall) 0.5f else if (side == PlayerSide.RED) 0.40f else 0.60f
            val y = if (hasBall) 0.68f else 0.45f
            Box(Modifier.offset(maxWidth * x - 22.dp, maxHeight * y - 22.dp).size(44.dp)
                .background(side.color(), CircleShape)
                .semantics { contentDescription = "${match.name(side, playerName)}${if (hasBall) ", has the ball" else ""}" },
                contentAlignment = Alignment.Center) {
                Text(if (hasBall) "🏀" else match.name(side, playerName).take(1).uppercase(), color = Color.White)
            }
        }
    }
}
