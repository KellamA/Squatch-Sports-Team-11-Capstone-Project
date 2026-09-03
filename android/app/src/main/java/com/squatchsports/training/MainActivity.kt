package com.squatchsports.training

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.data.AppDataStore
import com.squatchsports.training.ui.SquatchSportsApp
import com.squatchsports.training.ui.theme.SquatchSportsTheme

class MainActivity : ComponentActivity() {
    private lateinit var appData: AppDataStore
    private lateinit var connectivity: WorkoutConnectivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appData = AppDataStore(applicationContext)
        connectivity = WorkoutConnectivity(applicationContext)

        setContent {
            SquatchSportsTheme {
                SquatchSportsApp(appData = appData, connectivity = connectivity)
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
