import SwiftUI

struct DashboardHomeView: View {
    @EnvironmentObject var appData: AppDataStore

    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14)
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Squatch Sports")
                            .font(.title2)
                            .bold()

                        Text("Choose what you want to work on today.")
                            .foregroundStyle(.secondary)
                    }
                    .padding(.top, 8)

                    TodaySummaryCard(
                        shotsMade: appData.todayMakes,
                        shotsAttempted: appData.todayAttempts,
                        sessionsThisWeek: appData.weeklySessionsCompleted
                    )

                    LazyVGrid(columns: columns, spacing: 14) {
                        NavigationLink {
                            iPhoneWorkoutView(selectedDrill: "General Workout")
                        } label: {
                            DashboardCard(
                                title: "Start Workout",
                                subtitle: "Track shots live",
                                systemImage: "play.circle.fill"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            DrillsHomeView()
                        } label: {
                            DashboardCard(
                                title: "Drills",
                                subtitle: "Pick a drill",
                                systemImage: "figure.basketball"
                            )
                        }
                        .buttonStyle(.plain)
                        NavigationLink {
                            HistoryView()
                        } label: {
                            DashboardCard(
                                title: "History",
                                subtitle: "Past sessions",
                                systemImage: "clock.arrow.circlepath"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            AnalyticsView()
                        } label: {
                            DashboardCard(
                                title: "Analytics",
                                subtitle: "Trends and stats",
                                systemImage: "chart.bar.xaxis"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            GoalsView()
                        } label: {
                            DashboardCard(
                                title: "Goals",
                                subtitle: "Track your targets",
                                systemImage: "target"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            SettingsView()
                        } label: {
                            DashboardCard(
                                title: "Settings",
                                subtitle: "App preferences",
                                systemImage: "gearshape.fill"
                            )
                        }
                        .buttonStyle(.plain)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Today’s Focus")
                            .font(.headline)

                        Text(appData.currentFocus)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.thinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding()
            }
            .navigationTitle("Dashboard")
        }
    }
}

struct TodaySummaryCard: View {
    let shotsMade: Int
    let shotsAttempted: Int
    let sessionsThisWeek: Int

    private var percentage: Int {
        guard shotsAttempted > 0 else { return 0 }
        return Int((Double(shotsMade) / Double(shotsAttempted)) * 100)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Today")
                .font(.headline)

            Text("Current workout progress")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            HStack(spacing: 12) {
                SummaryStatView(value: "\(shotsMade)", label: "Makes")
                SummaryStatView(value: "\(shotsAttempted)", label: "Attempts")
                SummaryStatView(value: "\(percentage)%", label: "FG")
                SummaryStatView(value: "\(sessionsThisWeek)", label: "This Week")
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct SummaryStatView: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.headline)

            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct DashboardCard: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Image(systemName: systemImage)
                .font(.system(size: 26, weight: .semibold))
                .symbolRenderingMode(.hierarchical)

            Text(title)
                .font(.headline)

            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(2)

            Spacer(minLength: 0)
        }
        .padding()
        .frame(maxWidth: .infinity, minHeight: 128, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .contentShape(RoundedRectangle(cornerRadius: 18))
    }
}
