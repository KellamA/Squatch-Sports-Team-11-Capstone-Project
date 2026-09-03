package com.squatchsports.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.data.AppDataStore
import com.squatchsports.training.model.DrillOption

private sealed interface AppScreen {
    data object Dashboard : AppScreen
    data object Drills : AppScreen
    data class DrillDetail(val drill: DrillOption) : AppScreen
    data class Workout(val drillName: String) : AppScreen
    data object History : AppScreen
    data object Analytics : AppScreen
    data object Goals : AppScreen
    data object Settings : AppScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquatchSportsApp(
    appData: AppDataStore,
    connectivity: WorkoutConnectivity,
) {
    var backStack by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Dashboard)) }
    val current = backStack.last()
    val navigate: (AppScreen) -> Unit = { backStack = backStack + it }
    val goBack = { if (backStack.size > 1) backStack = backStack.dropLast(1) }

    BackHandler(enabled = backStack.size > 1, onBack = goBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle(current)) },
                navigationIcon = {
                    if (backStack.size > 1) {
                        IconButton(onClick = goBack) {
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
            AppScreen.Settings -> SettingsScreen(appData, contentPadding)
        }
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
}
