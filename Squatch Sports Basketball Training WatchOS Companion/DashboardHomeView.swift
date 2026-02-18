import SwiftUI

struct DashboardHomeView: View {
    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14)
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {

                    // Header
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Squatch Sports")
                            .font(.title2).bold()
                        Text("Choose what you want to work on today.")
                            .foregroundStyle(.secondary)
                    }
                    .padding(.top, 8)

                    // Card grid
                    LazyVGrid(columns: columns, spacing: 14) {

                        NavigationLink {
                            iPhoneWorkoutView()
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
                            PlaceholderView(
                                title: "History",
                                subtitle: "Past sessions",
                                systemImage: "clock.arrow.circlepath"
                            )
                        } label: {
                            DashboardCard(
                                title: "History",
                                subtitle: "Past sessions",
                                systemImage: "clock.arrow.circlepath"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            PlaceholderView(
                                title: "Analytics",
                                subtitle: "Trends & stats",
                                systemImage: "chart.bar.xaxis"
                            )
                        } label: {
                            DashboardCard(
                                title: "Analytics",
                                subtitle: "Trends & stats",
                                systemImage: "chart.bar.xaxis"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            PlaceholderView(
                                title: "Settings",
                                subtitle: "App preferences",
                                systemImage: "gearshape.fill"
                            )
                        } label: {
                            DashboardCard(
                                title: "Settings",
                                subtitle: "App preferences",
                                systemImage: "gearshape.fill"
                            )
                        }
                        .buttonStyle(.plain)
                    }

                    // Tip card
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Tip")
                            .font(.headline)
                        Text("We’ll connect these pages to real workout + watch data next.")
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .background(.thinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.top, 4)
                }
                .padding()
            }
            .navigationTitle("Dashboard")
        }
    }
}

// Reusable dashboard card
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
        .frame(maxWidth: .infinity, minHeight: 120, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct PlaceholderView: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.system(size: 44, weight: .semibold))
                .symbolRenderingMode(.hierarchical)

            Text(title).font(.title2).bold()
            Text(subtitle)
                .foregroundStyle(.secondary)

            Text("Coming soon — we’ll wire this up next.")
                .foregroundStyle(.secondary)
                .padding(.top, 8)
        }
        .padding()
        .navigationTitle(title)
    }
}
