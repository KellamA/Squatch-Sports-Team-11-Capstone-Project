// WatchMatchBridge.swift
// Connects the iPhone's 1v1 match to the Apple Watch. The iPhone keeps the real match and rules:
// every change is sent to the watch as a WatchMatchSnapshot, and plays logged on the watch
// arrive as WatchMatchCommands and are recorded here like taps on the iPhone screen.

import Foundation

extension OneOnOneStore {
    /// The app's store. Shared so the watch link works even when iOS launches the app in the
    /// background to deliver a play from the watch.
    static let shared = OneOnOneStore(defaults: AppDefaults.current)

    /// What the watch should show for the match in progress, or nil when there is none.
    var watchSnapshot: WatchMatchSnapshot? {
        guard let match = activeMatch else { return nil }
        let state = match.state
        return WatchMatchSnapshot(
            matchID: match.id,
            updatedAt: Date(),
            userSide: match.userSide,
            userName: playerName,
            opponentName: match.opponentName,
            scoring: match.settings.scoring,
            targetScore: match.settings.targetScore,
            redPoints: state.red.points,
            bluePoints: state.blue.points,
            redFouls: state.red.fouls,
            blueFouls: state.blue.fouls,
            possession: state.possession,
            winner: state.winner,
            lastPlay: match.events.last?.event.summary { match.name(for: $0, playerName: playerName) },
            canUndo: !match.events.isEmpty
        )
    }

    /// Applies a command from the watch. Ignored if it was meant for a different (older) match.
    func handle(_ command: WatchMatchCommand) {
        guard activeMatch?.id == command.matchID else { return }
        switch command.action {
        case .record(let event):
            record(event)
        case .undo:
            undoLastEvent()
        case .finish:
            finishMatch()
        }
    }
}

enum WatchMatchBridge {
    /// Call once at launch.
    static func connect(store: OneOnOneStore, connectivity: WorkoutConnectivity) {
        connectivity.onMatchCommand = { [weak store] command in
            store?.handle(command)
        }
        store.onActiveMatchChange = { [weak store, weak connectivity] in
            connectivity?.publishMatch(store?.watchSnapshot)
        }
        // The watch asks when it opens or reconnects, in case it missed an update.
        connectivity.onMatchRequest = { [weak store, weak connectivity] in
            connectivity?.publishMatch(store?.watchSnapshot)
        }
        // Send the current state in case a match was in progress when the app last closed.
        connectivity.publishMatch(store.watchSnapshot)
    }
}
