package com.squatchsports.training.shared

/**
 * Host-authoritative nearby play. Guest phone/watch commands use one durable FIFO, and snapshots
 * are never echoed as local plays. Transport callbacks and repository mutations use one thread.
 */
class NearbyMatchCoordinator(
    private val repository: OneOnOneRepository,
    private val send: (peerId: String, message: NearbyMatchMessage) -> Unit,
) {
    var connectedPeer: NearbyPlayerIdentity? = null
        private set
    private var connectionHost = false
    val canHost: Boolean
        get() {
            val peer = connectedPeer ?: return false
            val session = repository.data.nearbySession
            return if (session?.peerId == peer.id && session.status == NearbyMatchStatus.ACTIVE)
                session.role == NearbyMatchRole.HOST else connectionHost
        }

    fun connected(peer: NearbyPlayerIdentity, isHost: Boolean) {
        connectedPeer = peer
        connectionHost = isHost
        synchronize()
    }

    fun disconnected() { connectedPeer = null }

    fun startMatch(side: PlayerSide, settings: MatchSettings): Boolean {
        val peer = connectedPeer ?: return false
        if (!canHost) return false
        return repository.startNearbyMatch(peer, side, settings)
    }

    /** Called after every persisted change, including plays arriving through the Wear OS service. */
    fun onRepositoryChange() {
        val session = repository.data.nearbySession ?: return
        if (session.peerId != connectedPeer?.id) return
        if (session.role == NearbyMatchRole.HOST) sendSnapshot() else sendPending()
    }

    fun synchronize() {
        val session = repository.data.nearbySession ?: return
        if (session.peerId != connectedPeer?.id) return
        if (session.role == NearbyMatchRole.HOST) sendSnapshot() else {
            send(session.peerId, NearbyMatchMessage.Request(session.matchId))
            sendPending()
        }
    }

    fun perform(action: MatchCommandAction, event: MatchEvent? = null) {
        val match = repository.data.activeMatch ?: return
        val session = repository.data.nearbySession
        if (session?.role == NearbyMatchRole.GUEST && session.status == NearbyMatchStatus.ACTIVE) {
            repository.queueNearbyCommands(listOf(WatchMatchCommand(match.id, action, event)))
        } else when (action) {
            MatchCommandAction.RECORD -> repository.record(requireNotNull(event))
            MatchCommandAction.UNDO -> repository.undo()
            MatchCommandAction.FINISH -> repository.finishMatch()
            MatchCommandAction.CANCEL -> repository.cancelMatch()
        }
    }

    fun handleWatchCommands(commands: List<WatchMatchCommand>) {
        val session = repository.data.nearbySession
        if (session?.role == NearbyMatchRole.GUEST && session.status == NearbyMatchStatus.ACTIVE)
            repository.queueNearbyCommands(commands)
        else repository.handleCommands(commands)
    }

    fun receive(peerId: String, message: NearbyMatchMessage) {
        if (connectedPeer?.id != peerId) return
        when (message) {
            is NearbyMatchMessage.Snapshot -> {
                val session = repository.data.nearbySession
                // Only the accepting phone can join a new game; an established guest keeps its role on reconnect.
                val existingGuest = session?.role == NearbyMatchRole.GUEST && session.peerId == peerId &&
                    session.matchId == message.value.match.id
                if (!canHost || existingGuest) repository.applyNearbySnapshot(peerId, message.value)
            }
            is NearbyMatchMessage.Commands -> {
                val session = repository.data.nearbySession ?: return
                if (session.role != NearbyMatchRole.HOST || session.peerId != peerId) return
                repository.handleCommands(message.values.filter { it.matchId == session.matchId })
                // Repeated commands still get an acknowledgement if the previous reply was lost.
                sendSnapshot()
            }
            is NearbyMatchMessage.Request -> {
                val session = repository.data.nearbySession ?: return
                if (session.role == NearbyMatchRole.HOST && session.peerId == peerId &&
                    (message.matchId == null || message.matchId == session.matchId)) sendSnapshot()
            }
        }
    }

    private fun sendPending() {
        val session = repository.data.nearbySession ?: return
        if (session.role != NearbyMatchRole.GUEST || session.peerId != connectedPeer?.id || session.pendingCommands.isEmpty()) return
        send(session.peerId, NearbyMatchMessage.Commands(session.pendingCommands.take(100)))
    }

    private fun sendSnapshot() {
        val data = repository.data
        val session = data.nearbySession ?: return
        if (session.role != NearbyMatchRole.HOST || session.peerId != connectedPeer?.id) return
        val match = data.activeMatch ?: session.terminalMatch ?: return
        send(session.peerId, NearbyMatchMessage.Snapshot(NearbyGameSnapshot(match, data.playerName,
            session.guestName, session.status, data.revision, data.processedCommandIds)))
    }
}
