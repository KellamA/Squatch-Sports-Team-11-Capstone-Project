import SwiftUI

extension PlayerSide {
    var color: Color {
        switch self {
        case .red: Color(red: 0.91, green: 0.22, blue: 0.27)
        case .blue: Color(red: 0.16, green: 0.47, blue: 0.96)
        }
    }
}

/// Live 1v1 scorekeeping: red vs blue scoreboard, the court with both players on it, and controls.
struct MatchView: View {
    @EnvironmentObject var store: OneOnOneStore
    @Environment(\.dismiss) private var dismiss

    @State private var showLeaveOptions = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let match = store.activeMatch {
                let state = match.state

                VStack(spacing: 12) {
                    topBar(match)
                    Scoreboard(match: match, state: state, playerName: store.playerName)
                    MatchCourt(state: state) { match.name(for: $0, playerName: store.playerName) }
                        .frame(maxHeight: .infinity)
                    lastEventLine(match)
                    controls(match, state: state)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
                .sensoryFeedback(.impact(weight: .medium), trigger: match.events.count)

                if let winner = state.winner {
                    GameOverOverlay(
                        match: match,
                        winner: winner,
                        playerName: store.playerName,
                        record: store.friends.first { $0.id == match.opponentID }.map { store.record(against: $0) },
                        onSave: {
                            store.finishMatch()
                            dismiss()
                        },
                        onUndo: store.undoLastEvent
                    )
                    .transition(.opacity.combined(with: .scale(scale: 0.9)))
                }
            } else {
                // The match was saved or cancelled.
                Color.clear.onAppear { dismiss() }
            }
        }
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: store.activeMatch?.state)
        .preferredColorScheme(.dark)
        .confirmationDialog("Leave match?", isPresented: $showLeaveOptions, titleVisibility: .visible) {
            Button("Pause and leave") { dismiss() }
            Button("End without saving", role: .destructive) {
                store.cancelMatch()
                dismiss()
            }
            Button("Keep playing", role: .cancel) { }
        } message: {
            Text("A paused match can be resumed from the 1v1 screen.")
        }
    }

    // MARK: - Sections

    private func topBar(_ match: OneOnOneMatch) -> some View {
        HStack {
            Button {
                showLeaveOptions = true
            } label: {
                Image(systemName: "xmark")
                    .font(.headline)
                    .frame(width: 36, height: 36)
                    .background(.white.opacity(0.12), in: Circle())
            }
            .accessibilityLabel("Leave match")

            Spacer()

            VStack(spacing: 2) {
                Text("1v1")
                    .font(.headline)
                Text(rulesText(match.settings))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button {
                store.undoLastEvent()
            } label: {
                Image(systemName: "arrow.uturn.backward")
                    .font(.headline)
                    .frame(width: 36, height: 36)
                    .background(.white.opacity(0.12), in: Circle())
            }
            .disabled(match.events.isEmpty)
            .accessibilityLabel("Undo last play")
        }
        .foregroundStyle(.white)
        .padding(.top, 4)
    }

    private func lastEventLine(_ match: OneOnOneMatch) -> some View {
        Text(match.events.last.map { logged in
            "Last play: \(logged.event.summary { match.name(for: $0, playerName: store.playerName) })"
        } ?? "Tap a button to log the first play")
            .font(.footnote)
            .foregroundStyle(.secondary)
            .contentTransition(.opacity)
    }

    private func controls(_ match: OneOnOneMatch, state: MatchState) -> some View {
        VStack(spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                PlayerControls(side: .red, scoring: match.settings.scoring, record: store.record)
                PlayerControls(side: .blue, scoring: match.settings.scoring, record: store.record)
            }

            Button {
                store.record(.possession(state.possession.opponent))
            } label: {
                Label("Missed shot – ball to \(match.name(for: state.possession.opponent, playerName: store.playerName))",
                      systemImage: "arrow.left.arrow.right")
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(state.possession.opponent.color.opacity(0.25), in: RoundedRectangle(cornerRadius: 12))
                    // Swap the name instantly; a crossfade overlaps the old and new names.
                    .contentTransition(.identity)
            }
            .foregroundStyle(.white)
        }
    }

    private func rulesText(_ settings: MatchSettings) -> String {
        var parts = ["First to \(settings.targetScore)", settings.scoring.title]
        if settings.winByTwo { parts.append("Win by 2") }
        if settings.makeItTakeIt { parts.append("Make it, take it") }
        return parts.joined(separator: " • ")
    }
}

// MARK: - Scoreboard

private struct Scoreboard: View {
    let match: OneOnOneMatch
    let state: MatchState
    let playerName: String

    var body: some View {
        HStack(spacing: 0) {
            panel(for: .red)

            VStack(spacing: 4) {
                Text("VS")
                    .font(.caption.weight(.black))
                    .foregroundStyle(.white.opacity(0.6))
                Text("to \(match.settings.targetScore)")
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.5))
            }
            .frame(width: 44)

            panel(for: .blue)
        }
        .padding(6)
        .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 20))
    }

    private func panel(for side: PlayerSide) -> some View {
        let tally = state[side]
        let hasBall = state.possession == side

        return VStack(spacing: 4) {
            HStack(spacing: 4) {
                Text(match.name(for: side, playerName: playerName))
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                if hasBall {
                    Text("🏀")
                        .font(.subheadline)
                        .transition(.scale.combined(with: .opacity))
                }
            }

            Text("\(tally.points)")
                .font(.system(size: 56, weight: .black, design: .rounded))
                .monospacedDigit()
                .contentTransition(.numericText(value: Double(tally.points)))

            HStack(spacing: 10) {
                Text("Fouls \(tally.fouls)")
                    .accessibilityLabel("\(tally.fouls) fouls")
                Text("TO \(tally.turnovers)")
                    .accessibilityLabel("\(tally.turnovers) turnovers")
            }
            .monospacedDigit()
            .font(.caption2.weight(.semibold))
            .foregroundStyle(.white.opacity(0.8))

            if state.isGamePoint(for: side, settings: match.settings) {
                Text("GAME POINT")
                    .font(.caption2.weight(.heavy))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(.yellow, in: Capsule())
                    .foregroundStyle(.black)
            }
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(side.color.gradient)
                .opacity(hasBall ? 1 : 0.55)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(.white.opacity(hasBall ? 0.8 : 0), lineWidth: 2)
        )
    }
}

// MARK: - Court

/// The training court with both players on it. Whoever has the ball stands at the top of the key.
private struct MatchCourt: View {
    let state: MatchState
    let name: (PlayerSide) -> String

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            ZStack {
                SimplifiedCourtView(showsSwipeEdges: false)

                ForEach(PlayerSide.allCases) { side in
                    let offense = state.possession == side
                    PlayerMarker(side: side, name: name(side), hasBall: offense)
                        .position(
                            x: size.width * (offense ? 0.5 : (side == .red ? 0.42 : 0.58)),
                            y: size.height * (offense ? 0.64 : 0.46)
                        )
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 18))
        }
        .aspectRatio(0.9, contentMode: .fit)
        .frame(maxWidth: .infinity)
    }
}

private struct PlayerMarker: View {
    let side: PlayerSide
    let name: String
    let hasBall: Bool

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Circle()
                .fill(side.color.gradient)
                .frame(width: 38, height: 38)
                .overlay(Circle().stroke(.white, lineWidth: 2.5))
                .shadow(color: side.color.opacity(0.8), radius: hasBall ? 10 : 3)
                .overlay(
                    Text(name.prefix(1).uppercased())
                        .font(.headline.weight(.black))
                        .foregroundStyle(.white)
                )

            if hasBall {
                Text("🏀")
                    .font(.system(size: 18))
                    .offset(x: 8, y: 6)
                    .transition(.scale)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(name)\(hasBall ? ", has the ball" : "")")
    }
}

// MARK: - Controls

private struct PlayerControls: View {
    let side: PlayerSide
    let scoring: ScoringSystem
    let record: (MatchEvent) -> Void

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                scoreButton(points: scoring.insidePoints)
                scoreButton(points: scoring.outsidePoints)
            }
            HStack(spacing: 6) {
                smallButton("Foul", systemImage: "hand.raised.fill") { record(.foul(side)) }
                smallButton("TO", systemImage: "arrow.uturn.left") { record(.turnover(side)) }
                smallButton("Steal", systemImage: "hand.point.up.left.fill") { record(.steal(side)) }
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func scoreButton(points: Int) -> some View {
        Button {
            record(.score(side, points: points))
        } label: {
            Text("+\(points)")
                .font(.title2.weight(.black))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(side.color.gradient, in: RoundedRectangle(cornerRadius: 14))
                .foregroundStyle(.white)
        }
        .accessibilityLabel("\(side.title) plus \(points)")
    }

    private func smallButton(_ title: String, systemImage: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: systemImage)
                    .font(.caption)
                Text(title)
                    .font(.caption2.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(side.color.opacity(0.22), in: RoundedRectangle(cornerRadius: 10))
            .foregroundStyle(.white)
        }
        .accessibilityLabel("\(side.title) \(title == "TO" ? "turnover" : title.lowercased())")
    }
}

// MARK: - Game over

private struct GameOverOverlay: View {
    let match: OneOnOneMatch
    let winner: PlayerSide
    let playerName: String
    /// Record against this friend before this game is saved.
    let record: HeadToHeadRecord?
    let onSave: () -> Void
    let onUndo: () -> Void

    var body: some View {
        let state = match.state
        let winnerName = match.name(for: winner, playerName: playerName)
        let userWon = match.userWon == true

        ZStack {
            Color.black.opacity(0.7).ignoresSafeArea()

            VStack(spacing: 14) {
                Text(userWon ? "🏆" : "💪")
                    .font(.system(size: 64))
                Text(userWon ? "You win!" : "\(winnerName) wins!")
                    .font(.largeTitle.weight(.black))
                    .foregroundStyle(winner.color)
                Text("\(state[winner].points) – \(state[winner.opponent].points)")
                    .font(.title.weight(.bold))
                    .monospacedDigit()

                if let record {
                    let updated = HeadToHeadRecord(
                        wins: record.wins + (userWon ? 1 : 0),
                        losses: record.losses + (userWon ? 0 : 1)
                    )
                    Text("You are \(updated.text) against \(match.opponentName)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                HStack(spacing: 18) {
                    statColumn("Fouls", red: state.red.fouls, blue: state.blue.fouls)
                    statColumn("Turnovers", red: state.red.turnovers, blue: state.blue.turnovers)
                    statColumn("Steals", red: state.red.steals, blue: state.blue.steals)
                }
                .padding(.vertical, 6)

                Button(action: onSave) {
                    Text("Save Match")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(winner.color.gradient, in: RoundedRectangle(cornerRadius: 14))
                        .foregroundStyle(.white)
                }

                Button("Undo last play", action: onUndo)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(24)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 28))
            .padding(24)
        }
    }

    private func statColumn(_ title: String, red: Int, blue: Int) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            HStack(spacing: 8) {
                Text("\(red)").foregroundStyle(PlayerSide.red.color)
                Text("\(blue)").foregroundStyle(PlayerSide.blue.color)
            }
            .font(.headline.weight(.bold))
        }
    }
}

#Preview {
    let store = OneOnOneStore(defaults: UserDefaults(suiteName: "MatchViewPreview")!)
    let friend = store.addFriend(named: "Kaleb") ?? Friend(name: "Kaleb")
    store.startMatch(against: friend, userSide: .red, settings: MatchSettings())
    store.record(.score(.red, points: 2))
    store.record(.foul(.blue))
    return MatchView().environmentObject(store)
}
