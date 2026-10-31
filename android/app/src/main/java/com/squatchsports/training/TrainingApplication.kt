package com.squatchsports.training

import android.app.Application
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.data.OneOnOneStore
import com.squatchsports.training.nearby.NearbyMatchService

/** One authoritative match store even when Wear OS wakes the phone app in the background. */
class TrainingApplication : Application() {
    lateinit var matchStore: OneOnOneStore
        private set
    lateinit var connectivity: WorkoutConnectivity
        private set

    lateinit var nearby: NearbyMatchService
        private set

    override fun onCreate() {
        super.onCreate()
        matchStore = OneOnOneStore(this)
        connectivity = WorkoutConnectivity(this)
        nearby = NearbyMatchService(this)
        matchStore.sendNearbyMessage = nearby::send
        nearby.onConnected = matchStore.nearby::connected
        nearby.onDisconnected = matchStore.nearby::disconnected
        nearby.onMessage = matchStore.nearby::receive
        nearby.onSynchronize = matchStore.nearby::synchronize
        matchStore.onChange = { connectivity.publishMatch(it.watchUpdate) }
        connectivity.onMatchCommands = { commands ->
            matchStore.handleCommands(commands)
            connectivity.publishMatch(matchStore.data.watchUpdate)
        }
        connectivity.onMatchRequest = {
            connectivity.loadPendingMatchCommands()
            connectivity.publishMatch(matchStore.data.watchUpdate)
        }
    }
}
