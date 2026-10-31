package com.squatchsports.training.connectivity

import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import com.squatchsports.training.TrainingApplication
import com.squatchsports.training.shared.MatchPaths

/** The Data Layer delivers durable command batches here even if the phone activity is closed. */
class MatchListenerService : WearableListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectivity: WorkoutConnectivity
        get() = (application as TrainingApplication).connectivity

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == MatchPaths.COMMANDS) {
                // Copy before the framework releases the DataEventBuffer.
                val data = event.dataItem.data?.clone() ?: return@forEach
                mainHandler.post { connectivity.receiveMatchCommands(data) }
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == MatchPaths.REQUEST) mainHandler.post { connectivity.onMatchRequest?.invoke() }
    }

    override fun onPeerConnected(peer: Node) {
        mainHandler.post { connectivity.onMatchRequest?.invoke() }
    }
}
