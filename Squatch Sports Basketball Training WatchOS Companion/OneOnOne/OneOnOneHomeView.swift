import SwiftUI

/// 1v1 hub: your record, a paused match to resume, friends with head-to-head records, and recent games.
struct OneOnOneHomeView: View {
    @EnvironmentObject var store: OneOnOneStore

    @State private var showSetup = false
    @State private var setupOpponent: Friend?
    @State private var showMatch = false
    @State private var startRequested = false
    @State private var showAddFriend = false
    @State private var newFriendName = ""
    @State private var editingName = false
    @State private var nameDraft = ""

    var body: some View {
        List {
            Section {
                recordCard
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }

            Section {
                NavigationLink {
                    NearbyPlayersView()
                        .environmentObject(store)
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "person.2.wave.2.fill")
                            .font(.title3)
                            .foregroundStyle(.blue)

                        VStack(alignment: .leading, spacing: 2) {
                            Text("Nearby 1v1")
                                .font(.headline)

                            Text("Connect to another player nearby")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }

            if let match = store.activeMatch {
                Section("Match in progress") {
                    Button {
                        showMatch = true
                    } label: {
                        ActiveMatchRow(match: match, playerName: store.playerName)
                    }
                }
            }

            Section {
                ForEach(store.friends) { friend in
                    FriendRow(friend: friend, record: store.record(against: friend)) {
                        setupOpponent = friend
                        showSetup = true
                    }
                }
                .onDelete { offsets in
                    offsets.map { store.friends[$0] }.forEach(store.removeFriend)
                }

                Button {
                    newFriendName = ""
                    showAddFriend = true
                } label: {
                    Label("Add Friend", systemImage: "person.badge.plus")
                }
            } header: {
                Text("Friends")
            } footer: {
                if store.friends.isEmpty {
                    Text("Add the people you play against, then start a match.")
                }
            }

            if !store.matches.isEmpty {
                Section("Recent Games") {
                    ForEach(store.matches.prefix(10)) { match in
                        MatchHistoryRow(match: match)
                    }
                }
            }
        }
        .navigationTitle("1v1")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    setupOpponent = nil
                    showSetup = true
                } label: {
                    Label("New Match", systemImage: "plus")
                }
                .disabled(store.friends.isEmpty || store.activeMatch != nil)
            }
        }
        .sheet(isPresented: $showSetup, onDismiss: {
            // Open the match only after the setup sheet has finished closing.
            if store.activeMatch != nil && startRequested {
                startRequested = false
                showMatch = true
            }
        }) {
            MatchSetupView(initialOpponent: setupOpponent) {
                startRequested = true
                showSetup = false
            }
        }
        .fullScreenCover(isPresented: $showMatch) {
            MatchView()
        }
        .alert("Add Friend", isPresented: $showAddFriend) {
            TextField("Name", text: $newFriendName)
                .textInputAutocapitalization(.words)
            Button("Add") { store.addFriend(named: newFriendName) }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("Enter the name of someone you play 1v1 against.")
        }
        .alert("Your Name", isPresented: $editingName) {
            TextField("Name", text: $nameDraft)
                .textInputAutocapitalization(.words)
            Button("Save") {
                let trimmed = nameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty { store.playerName = trimmed }
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("This is the name shown on the scoreboard.")
        }
    }

    private var recordCard: some View {
        let record = store.overallRecord

        return VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Your 1v1 Record")
                        .font(.headline)
                    Button {
                        nameDraft = store.playerName
                        editingName = true
                    } label: {
                        Label(store.playerName, systemImage: "pencil")
                            .font(.subheadline)
                    }
                    .buttonStyle(.borderless)
                }
                Spacer()
                Text("🏀")
                    .font(.system(size: 34))
            }

            HStack {
                stat("\(record.wins)", "Wins", color: .green)
                stat("\(record.losses)", "Losses", color: .red)
                stat("\(record.winPercentage)%", "Win Rate", color: .primary)
            }
        }
        .padding()
        .background(
            LinearGradient(
                colors: [PlayerSide.red.color.opacity(0.25), PlayerSide.blue.color.opacity(0.25)],
                startPoint: .leading,
                endPoint: .trailing
            ),
            in: RoundedRectangle(cornerRadius: 18)
        )
    }

    private func stat(_ value: String, _ label: String, color: Color) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundStyle(color)
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct FriendRow: View {
    let friend: Friend
    let record: HeadToHeadRecord
    let onPlay: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(friend.name.prefix(1).uppercased())
                .font(.headline)
                .frame(width: 36, height: 36)
                .background(Color.secondary.opacity(0.2), in: Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(friend.name)
                    .font(.headline)
                Text(record.games == 0 ? "No games yet" : "You're \(record.text)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button("Play", action: onPlay)
                .buttonStyle(.borderedProminent)
                .tint(PlayerSide.red.color)
        }
    }
}

private struct ActiveMatchRow: View {
    let match: OneOnOneMatch
    let playerName: String

    var body: some View {
        let state = match.state
        HStack {
            Text(match.name(for: .red, playerName: playerName))
                .foregroundStyle(PlayerSide.red.color)
            Text("\(state.red.points) – \(state.blue.points)")
                .monospacedDigit()
                .bold()
            Text(match.name(for: .blue, playerName: playerName))
                .foregroundStyle(PlayerSide.blue.color)
            Spacer()
            Text("Resume")
                .font(.subheadline.weight(.semibold))
            Image(systemName: "chevron.right")
                .font(.caption)
        }
        .foregroundStyle(.primary)
    }
}

private struct MatchHistoryRow: View {
    let match: OneOnOneMatch

    var body: some View {
        let state = match.state
        let won = match.userWon == true
        let mine = state[match.userSide].points
        let theirs = state[match.userSide.opponent].points

        HStack(spacing: 12) {
            Text(won ? "W" : "L")
                .font(.headline.weight(.black))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(won ? Color.green : Color.red, in: RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text("\(mine) – \(theirs) vs \(match.opponentName)")
                    .font(.subheadline.weight(.semibold))
                    .monospacedDigit()
                Text((match.endDate ?? match.startDate).formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Label("\(state[match.userSide].fouls)", systemImage: "hand.raised.fill")
                .font(.caption)
                .foregroundStyle(.secondary)
                .accessibilityLabel("\(state[match.userSide].fouls) fouls")
        }
    }
}



#Preview {
    NavigationStack {
        OneOnOneHomeView()
            .environmentObject(
                OneOnOneStore(
                    defaults: UserDefaults(
                        suiteName: "OneOnOneHomePreview"
                    )!
                )
            )
    }
}
