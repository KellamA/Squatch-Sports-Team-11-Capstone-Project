import Foundation

/// Connects the nearby iPhone session to the existing 1v1 store.
///
/// Local actions:
/// iPhone / Apple Watch -> OneOnOneStore -> nearby opponent
///
/// Remote actions:
/// nearby opponent -> OneOnOneStore -> paired Apple Watch
final class NearbyMatchCoordinator {

    static let shared = NearbyMatchCoordinator()

    private let nearby = NearbyMatchService.shared
    private weak var store: OneOnOneStore?

    /// Only events belonging to this match are sent over the nearby connection.
    private var nearbyMatchID: UUID?

    private init() {}

    /// Call when entering the nearby 1v1 flow.
    func connect(store: OneOnOneStore) {
        self.store = store

        store.onLocalMatchEvent = { [weak self] matchID, event in
            guard let self,
                  self.nearbyMatchID == matchID else { return }

            self.nearby.send(
                .event(matchID: matchID, event: event)
            )
        }

        store.onLocalUndo = { [weak self] matchID in
            guard let self,
                  self.nearbyMatchID == matchID else { return }

            self.nearby.send(
                .undo(matchID: matchID)
            )
        }

        store.onLocalFinish = { [weak self] matchID in
            guard let self,
                  self.nearbyMatchID == matchID else { return }

            self.nearby.send(
                .finish(matchID: matchID)
            )

            self.nearbyMatchID = nil
        }

        store.onLocalCancel = { [weak self] matchID in
            guard let self,
                  self.nearbyMatchID == matchID else { return }

            self.nearby.send(
                .cancel(matchID: matchID)
            )

            self.nearbyMatchID = nil
        }

        nearby.onMessage = { [weak self] message in
            self?.handle(message)
        }
    }

    /// Host creates the shared match and tells the connected phone to create
    /// the same match with the opposite player side.
    @discardableResult
    func startNearbyMatch(
        against opponentName: String,
        hostSide: PlayerSide,
        settings: MatchSettings
    ) -> Bool {

        guard let store,
              !nearby.connectedPeers.isEmpty else {
            return false
        }

        let opponent = friend(named: opponentName, in: store)
        let matchID = UUID()

        nearbyMatchID = matchID

        store.startMatch(
            id: matchID,
            against: opponent,
            userSide: hostSide,
            settings: settings
        )

        let setup = NearbyMatchSetup(
            matchID: matchID,
            hostName: store.playerName,
            guestName: opponentName,
            hostSide: hostSide,
            settings: settings
        )

        nearby.send(.startMatch(setup))

        return true
    }

    private func handle(_ message: NearbyMatchMessage) {
        guard let store else { return }

        switch message {

        case .startMatch(let setup):
            let host = friend(
                named: setup.hostName,
                in: store
            )

            nearbyMatchID = setup.matchID

            store.startMatch(
                id: setup.matchID,
                against: host,
                userSide: setup.hostSide.opponent,
                settings: setup.settings
            )

        case .event(let matchID, let event):
            guard nearbyMatchID == matchID else { return }

            store.recordRemote(
                event,
                matchID: matchID
            )

        case .undo(let matchID):
            guard nearbyMatchID == matchID else { return }

            store.undoRemote(
                matchID: matchID
            )

        case .finish(let matchID):
            guard nearbyMatchID == matchID else { return }

            store.finishRemote(
                matchID: matchID
            )

            nearbyMatchID = nil

        case .cancel(let matchID):
            guard nearbyMatchID == matchID else { return }

            store.cancelRemote(
                matchID: matchID
            )

            nearbyMatchID = nil
        }
    }

    private func friend(
        named name: String,
        in store: OneOnOneStore
    ) -> Friend {

        if let existing = store.friends.first(where: {
            $0.name.caseInsensitiveCompare(name) == .orderedSame
        }) {
            return existing
        }

        if let added = store.addFriend(named: name) {
            return added
        }

        // This should only be reached if the name is already present.
        return store.friends.first {
            $0.name.caseInsensitiveCompare(name) == .orderedSame
        }!
    }
}
