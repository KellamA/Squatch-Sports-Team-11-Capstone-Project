package com.squatchsports.training.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.presentation.theme.SquatchSportsTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var connectivity: WorkoutConnectivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectivity = WorkoutConnectivity(applicationContext)
        setContent {
            SquatchSportsTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2_800)
                    showSplash = false
                }
                Crossfade(targetState = showSplash, label = "splash") { isSplashVisible ->
                    if (isSplashVisible) {
                        SquatchSplashScreen()
                    } else {
                        WearWorkoutApp(connectivity)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectivity.startListening()
    }

    override fun onStop() {
        connectivity.stopListening()
        super.onStop()
    }
}
