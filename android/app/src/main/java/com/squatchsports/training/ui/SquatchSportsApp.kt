package com.squatchsports.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.data.AppDataStore
import com.squatchsports.training.data.OneOnOneStore
import com.squatchsports.training.model.DrillOption
import com.squatchsports.training.nearby.NearbyMatchService

private sealed interface AppScreen {
    data object Dashboard : AppScreen
    data object Drills : AppScreen
    data class DrillDetail(val drill: DrillOption) : AppScreen
    data class Workout(val drillName: String) : AppScreen
    data object History : AppScreen
    data object Analytics : AppScreen
    data object Goals : AppScreen
    data object Settings : AppScreen
    data object OneOnOne : AppScreen
    data object NearbyPlayers : AppScreen
    data class MatchSetup(val opponentId: String?) : AppScreen
    data object Match : AppScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquatchSportsApp(
    appData: AppDataStore,
    connectivity: WorkoutConnectivity,
    matchStore: OneOnOneStore,
    nearby: NearbyMatchService,
) {
    var backStack by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Dashboard)) }
    val current = backStack.last()
    val navigate: (AppScreen) -> Unit = { backStack = backStack + it }
    val goBack = { if (backStack.size > 1) backStack = backStack.dropLast(1) }

    var showLeaveMatch by remember { mutableStateOf(false) }
    val requestBack = { if (current == AppScreen.Match && matchStore.data.activeMatch != null) showLeaveMatch = true else goBack() }
    BackHandler(enabled = backStack.size > 1, onBack = requestBack)
    LaunchedEffect(matchStore.data.activeMatch?.id) {
        if (matchStore.data.activeMatch == null) showLeaveMatch = false
    }

    // Keep the watch's streak/goal display current, and mirror each popup on the watch.
    val watchStats = appData.watchStats
    LaunchedEffect(watchStats) { connectivity.publishStats(watchStats) }
    val celebration = appData.celebrations.firstOrNull()
    LaunchedEffect(celebration) { celebration?.let { connectivity.sendCelebration(it.toMessage()) } }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle(current)) },
                    navigationIcon = {
                        if (backStack.size > 1) {
                            IconButton(onClick = requestBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                )
            },
        ) { contentPadding ->
            when (current) {
                AppScreen.Dashboard -> DashboardScreen(
                    appData = appData,
                    contentPadding = contentPadding,
                    onStartWorkout = { navigate(AppScreen.Workout("General Workout")) },
                    onDrills = { navigate(AppScreen.Drills) },
                    onOneOnOne = { navigate(AppScreen.OneOnOne) },
                    onHistory = { navigate(AppScreen.History) },
                    onAnalytics = { navigate(AppScreen.Analytics) },
                    onGoals = { navigate(AppScreen.Goals) },
                    onSettings = { navigate(AppScreen.Settings) },
                )
                AppScreen.Drills -> DrillsScreen(
                    contentPadding = contentPadding,
                    onDrillSelected = { navigate(AppScreen.DrillDetail(it)) },
                )
                is AppScreen.DrillDetail -> DrillDetailScreen(
                    drill = current.drill,
                    contentPadding = contentPadding,
                    onStart = { navigate(AppScreen.Workout(current.drill.name)) },
                )
                is AppScreen.Workout -> WorkoutScreen(
                    selectedDrill = current.drillName,
                    appData = appData,
                    connectivity = connectivity,
                    contentPadding = contentPadding,
                )
                AppScreen.History -> HistoryScreen(appData, contentPadding)
                AppScreen.Analytics -> AnalyticsScreen(appData, contentPadding)
                AppScreen.Goals -> GoalsScreen(appData, contentPadding)
                AppScreen.Settings -> SettingsScreen(appData, contentPadding, connectivity)
                AppScreen.OneOnOne -> OneOnOneHomeScreen(matchStore, contentPadding,
                    onSetup = { navigate(AppScreen.MatchSetup(it)) }, onResume = { navigate(AppScreen.Match) },
                    onNearby = { navigate(AppScreen.NearbyPlayers) })
                AppScreen.NearbyPlayers -> NearbyPlayersScreen(matchStore, nearby, contentPadding,
                    onOpenMatch = { navigate(AppScreen.Match) })
                is AppScreen.MatchSetup -> MatchSetupScreen(matchStore, current.opponentId, contentPadding,
                    onStart = { backStack = backStack.dropLast(1) + AppScreen.Match })
                AppScreen.Match -> LiveMatchScreen(matchStore, contentPadding,
                    onLeave = { showLeaveMatch = true }, onDone = goBack, nearbyStatus = nearby.state.status)
            }
        }

        if (showLeaveMatch) AlertDialog(
            onDismissRequest = { showLeaveMatch = false },
            title = { Text("Leave match?") },
            text = {
                Column {
                    Text("Pause to resume later, or discard this match without saving it.")
                    TextButton(onClick = {
                        showLeaveMatch = false
                        matchStore.cancelMatch()
                        goBack()
                    }) { Text("End without saving") }
                }
            },
            confirmButton = { TextButton(onClick = { showLeaveMatch = false; goBack() }) { Text("Pause and leave") } },
            dismissButton = { TextButton(onClick = { showLeaveMatch = false }) { Text("Keep playing") } },
        )
        CelebrationOverlay(appData.celebrations.firstOrNull(), onDismiss = appData::dismissCelebration)
    }
}

private fun screenTitle(screen: AppScreen): String = when (screen) {
    AppScreen.Dashboard -> "Dashboard"
    AppScreen.Drills -> "Drills"
    is AppScreen.DrillDetail -> screen.drill.name
    is AppScreen.Workout -> "Workout"
    AppScreen.History -> "History"
    AppScreen.Analytics -> "Analytics"
    AppScreen.Goals -> "Goals"
    AppScreen.Settings -> "Settings"
    AppScreen.OneOnOne -> "1v1"
    AppScreen.NearbyPlayers -> "Nearby Players"
    is AppScreen.MatchSetup -> "New Match"
    AppScreen.Match -> "1v1 Match"
}
