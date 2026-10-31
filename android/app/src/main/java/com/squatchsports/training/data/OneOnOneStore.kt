package com.squatchsports.training.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.squatchsports.training.shared.MatchCommandAction
import com.squatchsports.training.shared.MatchEvent
import com.squatchsports.training.shared.NearbyMatchCoordinator
import com.squatchsports.training.shared.NearbyMatchMessage
import com.squatchsports.training.shared.MatchSettings
import com.squatchsports.training.shared.MatchStorage
import com.squatchsports.training.shared.OneOnOneData
import com.squatchsports.training.shared.OneOnOneRepository
import com.squatchsports.training.shared.PlayerSide
import com.squatchsports.training.shared.WatchMatchCommand

/** Shared by the UI and wearable listener service. Do not create per-activity copies. */
class OneOnOneStore(context: Context, preferencesName: String = "squatch_one_on_one") {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val repository = OneOnOneRepository(object : MatchStorage {
        override fun read(): String? = preferences.getString("state", null)
        override fun write(value: String) {
            // Persist plays and their processed IDs atomically before publishing an acknowledgement.
            check(preferences.edit().putString("state", value).commit()) { "Unable to save 1v1 match" }
        }
    })
    var data by mutableStateOf(repository.data)
        private set
    var onChange: ((OneOnOneData) -> Unit)? = null
    var sendNearbyMessage: ((String, NearbyMatchMessage) -> Unit)? = null
    val nearby = NearbyMatchCoordinator(repository) { peer, message -> sendNearbyMessage?.invoke(peer, message) }

    init {
        repository.onChange = {
            data = it
            nearby.onRepositoryChange()
            onChange?.invoke(it)
        }
    }

    fun setPlayerName(name: String) = repository.setPlayerName(name)
    fun addFriend(name: String) = repository.addFriend(name)
    fun removeFriend(id: String) = repository.removeFriend(id)
    fun startMatch(opponentId: String, userSide: PlayerSide, settings: MatchSettings) =
        repository.startMatch(opponentId, userSide, settings)
    fun record(event: MatchEvent) = nearby.perform(MatchCommandAction.RECORD, event)
    fun undo() = nearby.perform(MatchCommandAction.UNDO)
    fun finishMatch() = nearby.perform(MatchCommandAction.FINISH)
    fun cancelMatch() = nearby.perform(MatchCommandAction.CANCEL)
    fun abandonNearbyMatch() = repository.abandonNearbyMatch()
    fun handleCommands(commands: List<WatchMatchCommand>) = nearby.handleWatchCommands(commands)
}
