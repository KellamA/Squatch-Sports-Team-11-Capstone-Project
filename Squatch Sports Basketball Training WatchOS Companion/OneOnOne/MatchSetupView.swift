import SwiftUI

/// Pick an opponent, colors, and rules, then start the match.
struct MatchSetupView: View {
    @EnvironmentObject var store: OneOnOneStore
    @Environment(\.dismiss) private var dismiss

    let initialOpponent: Friend?
    let onStart: () -> Void

    @State private var opponentID: UUID?
    @State private var userSide: PlayerSide = .red
    @State private var settings = MatchSettings()

    var body: some View {
        NavigationStack {
            Form {
                Section("Opponent") {
                    Picker("Playing against", selection: $opponentID) {
                        ForEach(store.friends) { friend in
                            Text(friend.name).tag(Optional(friend.id))
                        }
                    }
                }

                Section {
                    Picker("Your color", selection: $userSide) {
                        ForEach(PlayerSide.allCases) { side in
                            Text(side.title).tag(side)
                        }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(userSide.color.opacity(0.2))
                } header: {
                    Text("Your Color")
                } footer: {
                    Text("\(opponentName) will be \(userSide.opponent.title).")
                }

                Section("Rules") {
                    Picker("Scoring", selection: $settings.scoring) {
                        ForEach(ScoringSystem.allCases) { system in
                            Text(system.title).tag(system)
                        }
                    }
                    .onChange(of: settings.scoring) { _, system in
                        settings.targetScore = system.defaultTargetScore
                    }

                    Stepper(value: $settings.targetScore, in: MatchSettings.targetRange) {
                        LabeledContent("Play to", value: "\(settings.targetScore)")
                    }

                    Toggle("Win by 2", isOn: $settings.winByTwo)
                    Toggle("Make it, take it", isOn: $settings.makeItTakeIt)
                }

                Section("First Ball") {
                    Picker("Who starts with the ball", selection: $settings.firstPossession) {
                        ForEach(PlayerSide.allCases) { side in
                            Text(name(for: side)).tag(side)
                        }
                    }

                    Button {
                        settings.firstPossession = Bool.random() ? .red : .blue
                    } label: {
                        Label("Flip a coin", systemImage: "circle.lefthalf.filled")
                    }
                }
            }
            .navigationTitle("New Match")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Start") { start() }
                        .bold()
                        .disabled(selectedOpponent == nil)
                }
            }
            .onAppear {
                opponentID = initialOpponent?.id ?? store.friends.first?.id
            }
        }
    }

    private var selectedOpponent: Friend? {
        store.friends.first { $0.id == opponentID }
    }

    private var opponentName: String {
        selectedOpponent?.name ?? "Your opponent"
    }

    private func name(for side: PlayerSide) -> String {
        "\(side == userSide ? store.playerName : opponentName) (\(side.title))"
    }

    private func start() {
        guard let opponent = selectedOpponent else { return }
        store.startMatch(against: opponent, userSide: userSide, settings: settings)
        onStart()
    }
}
