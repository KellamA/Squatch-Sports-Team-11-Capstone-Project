import SwiftUI

struct AnalyticsView: View {
    @EnvironmentObject var appData: AppDataStore

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Performance Overview")
                    .font(.headline)

                LazyVGrid(columns: [
                    GridItem(.flexible(), spacing: 14),
                    GridItem(.flexible(), spacing: 14)
                ], spacing: 14) {
                    AnalyticsCard(title: "Makes", value: "\(appData.totalMakes)")
                    AnalyticsCard(title: "Attempts", value: "\(appData.totalAttempts)")
                    AnalyticsCard(title: "FG%", value: "\(appData.shootingPercentage)%")
                    AnalyticsCard(title: "Sessions", value: "\(appData.sessionsCompleted)")
                }

                if let best = appData.bestSession {
                    InfoBlock(
                        title: "Best Session",
                        value: "\(best.drill) • \(best.makes)/\(best.attempts) • \(best.percentage)%"
                    )
                }

                if let recent = appData.mostRecentSession {
                    InfoBlock(
                        title: "Recent Trend",
                        value: "Last workout was \(recent.drill) with \(recent.makes) makes on \(recent.attempts) attempts."
                    )
                }

                InfoBlock(
                    title: "Favorite Drill",
                    value: appData.favoriteDrill
                )

                InfoBlock(
                    title: "Current Focus",
                    value: appData.currentFocus
                )
            }
            .padding()
        }
        .navigationTitle("Analytics")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct AnalyticsCard: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .foregroundStyle(.secondary)

            Text(value)
                .font(.title2)
                .bold()
        }
        .padding()
        .frame(maxWidth: .infinity, minHeight: 100, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct InfoBlock: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)

            Text(value)
                .foregroundStyle(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
