package com.squatchsports.training.presentation

import android.app.Application
import com.squatchsports.training.connectivity.WorkoutConnectivity

/** A single durable outbox across activity recreation; old callbacks cannot overwrite a new queue. */
class WearTrainingApplication : Application() {
    lateinit var connectivity: WorkoutConnectivity
        private set

    override fun onCreate() {
        super.onCreate()
        connectivity = WorkoutConnectivity(this)
    }
}
