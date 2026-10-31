package com.squatchsports.training.connectivity

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.squatchsports.training.presentation.WearTrainingApplication
import com.squatchsports.training.shared.MatchPaths

/** Acknowledgements can drain the remaining outbox chunks even after the watch screen closes. */
class MatchStateListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val connectivity = (application as WearTrainingApplication).connectivity
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == MatchPaths.STATE) {
                event.dataItem.data?.clone()?.let(connectivity::applyMatchUpdate)
            }
        }
    }
}
