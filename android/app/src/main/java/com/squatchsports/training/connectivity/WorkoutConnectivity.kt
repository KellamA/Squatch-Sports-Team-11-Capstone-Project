package com.squatchsports.training.connectivity

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.squatchsports.training.shared.CourtPosition
import com.squatchsports.training.shared.WorkoutPaths
import com.squatchsports.training.shared.WorkoutPayloads
import java.util.concurrent.CopyOnWriteArraySet

class WorkoutConnectivity(context: Context) : MessageClient.OnMessageReceivedListener {
    private val appContext = context.applicationContext
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val shotListeners = CopyOnWriteArraySet<(Int) -> Unit>()

    var workoutActive: Boolean = false
        private set
    var currentPosition: CourtPosition? = null
        private set
    var currentDrill: String? = null
        private set

    fun startListening() {
        messageClient.addListener(this)
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    fun addShotListener(listener: (Int) -> Unit) {
        shotListeners.add(listener)
    }

    fun removeShotListener(listener: (Int) -> Unit) {
        shotListeners.remove(listener)
    }

    fun sendWorkoutStarted(drillName: String, position: CourtPosition?) {
        workoutActive = true
        sendBatch(
            buildList {
                add(WorkoutPaths.WORKOUT_STARTED to byteArrayOf())
                add(WorkoutPaths.DRILL_INFO to WorkoutPayloads.drill(drillName))
                if (position != null) add(WorkoutPaths.POSITION to WorkoutPayloads.position(position))
            },
        )
    }

    fun sendWorkoutStopped() {
        workoutActive = false
        currentPosition = null
        currentDrill = null
        sendMessage(WorkoutPaths.WORKOUT_STOPPED)
    }

    fun sendShotToWatch(value: Int) {
        sendMessage(WorkoutPaths.SHOT, WorkoutPayloads.shot(value))
    }

    fun sendPositionUpdate(position: CourtPosition) {
        sendMessage(WorkoutPaths.POSITION, WorkoutPayloads.position(position))
    }

    override fun onMessageReceived(event: MessageEvent) {
        mainHandler.post {
            when (event.path) {
                WorkoutPaths.WORKOUT_STARTED -> workoutActive = true
                WorkoutPaths.WORKOUT_STOPPED -> {
                    workoutActive = false
                    currentPosition = null
                    currentDrill = null
                }
                WorkoutPaths.SHOT -> WorkoutPayloads.decodeShot(event.data)?.let { value ->
                    shotListeners.forEach { it(value) }
                }
                WorkoutPaths.POSITION -> {
                    currentPosition = WorkoutPayloads.decodePosition(event.data)
                }
                WorkoutPaths.DRILL_INFO -> {
                    currentDrill = WorkoutPayloads.decodeDrill(event.data)
                }
            }
        }
    }

    private fun sendMessage(path: String, data: ByteArray = byteArrayOf()) {
        sendBatch(listOf(path to data))
    }

    private fun sendBatch(messages: List<Pair<String, ByteArray>>) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    messages.forEach { (path, data) ->
                        messageClient.sendMessage(node.id, path, data)
                            .addOnFailureListener { error ->
                                Log.w(TAG, "Unable to send $path to ${node.displayName}", error)
                            }
                    }
                }
            }
            .addOnFailureListener { error -> Log.w(TAG, "Unable to find connected watch", error) }
    }

    private companion object {
        const val TAG = "WorkoutConnectivity"
    }
}
