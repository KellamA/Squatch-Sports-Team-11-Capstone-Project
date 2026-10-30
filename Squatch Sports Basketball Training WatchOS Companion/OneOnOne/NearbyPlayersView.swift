
import SwiftUI
import MultipeerConnectivity

struct NearbyPlayersView: View {

    @EnvironmentObject var store: OneOnOneStore
    @StateObject private var nearby = NearbyMatchService.shared

    @State private var showMatch = false
    @State private var hostSide: PlayerSide = .red
    @State private var settings = MatchSettings()

    var body: some View {
        List {

            Section {
                HStack {
                    Circle()
                        .fill(
                            nearby.connectedPeers.isEmpty
                            ? Color.orange
                            : Color.green
                        )
                        .frame(width: 10, height: 10)

                    Text(nearby.statusText)
                        .font(.subheadline)
                }
            }

            Section("Nearby Players") {
                if nearby.discoveredPeers.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Looking for players...")
                            .font(.headline)

                        Text("Both players need the app open on their iPhone.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                } else {
                    ForEach(nearby.discoveredPeers, id: \.self) { peer in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(peer.displayName)
                                    .font(.headline)

                                Text(
                                    nearby.connectedPeers.contains(peer)
                                    ? "Connected"
                                    : "Nearby"
                                )
                                .font(.caption)
                                .foregroundStyle(
                                    nearby.connectedPeers.contains(peer)
                                    ? Color.green
                                    : Color.secondary
                                )
                            }

                            Spacer()

                            if nearby.connectedPeers.contains(peer) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(.green)
                            } else {
                                Button("Connect") {
                                    nearby.connect(to: peer)
                                }
                                .buttonStyle(.borderedProminent)
                            }
                        }
                    }
                }
            }

            if let peer = nearby.connectedPeers.first {

                Section("Match") {

                    LabeledContent(
                        "Opponent",
                        value: peer.displayName
                    )

                    Picker("Your Color", selection: $hostSide) {
                        ForEach(PlayerSide.allCases) { side in
                            Text(side.title).tag(side)
                        }
                    }
                    .pickerStyle(.segmented)

                    Picker("Scoring", selection: $settings.scoring) {
                        ForEach(ScoringSystem.allCases) { system in
                            Text(system.title).tag(system)
                        }
                    }
                    .onChange(of: settings.scoring) { _, system in
                        settings.targetScore = system.defaultTargetScore
                    }

                    Stepper(
                        value: $settings.targetScore,
                        in: MatchSettings.targetRange
                    ) {
                        LabeledContent(
                            "Play to",
                            value: "\(settings.targetScore)"
                        )
                    }

                    Toggle(
                        "Win by 2",
                        isOn: $settings.winByTwo
                    )

                    Toggle(
                        "Make it, take it",
                        isOn: $settings.makeItTakeIt
                    )

                    Picker(
                        "First Ball",
                        selection: $settings.firstPossession
                    ) {
                        ForEach(PlayerSide.allCases) { side in
                            Text(side.title).tag(side)
                        }
                    }

                    Button {
                        startMatch(against: peer)
                    } label: {
                        Label(
                            "Start Nearby 1v1",
                            systemImage: "basketball.fill"
                        )
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.activeMatch != nil)
                }
            }
        }
        .navigationTitle("Nearby Players")
        .navigationBarTitleDisplayMode(.inline)

        .onAppear {
            NearbyMatchCoordinator.shared.connect(store: store)

            if nearby.connectedPeers.isEmpty {
                nearby.start(displayName: store.playerName)
            }
        }

        .onChange(of: store.activeMatch?.id) { _, matchID in
            if matchID != nil {
                showMatch = true
            }
        }

        .fullScreenCover(isPresented: $showMatch) {
            MatchView()
                .environmentObject(store)
        }
    }

    private func startMatch(against peer: MCPeerID) {
        let started = NearbyMatchCoordinator.shared.startNearbyMatch(
            against: peer.displayName,
            hostSide: hostSide,
            settings: settings
        )

        if started {
            showMatch = true
        }
    }
}

#Preview {
    NavigationStack {
        NearbyPlayersView()
            .environmentObject(
                OneOnOneStore(
                    defaults: UserDefaults(
                        suiteName: "NearbyPlayersPreview"
                    )!
                )
            )
    }
}

