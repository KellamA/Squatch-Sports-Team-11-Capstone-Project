import SwiftUI

struct DashboardHomeView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("Main") {
                    NavigationLink("Start Workout") { iPhoneWorkoutView() }
                    NavigationLink("Drills") { ComingSoonView(title: "Drills") }
                    NavigationLink("History") { ComingSoonView(title: "History") }
                    NavigationLink("Analytics") { ComingSoonView(title: "Analytics") }
                    NavigationLink("Settings") { ComingSoonView(title: "Settings") }
                }
            }
            .navigationTitle("Dashboard")
        }
    }
}

struct ComingSoonView: View {
    let title: String
    var body: some View {
        VStack(spacing: 10) {
            Text(title).font(.title2).bold()
            Text("Coming soon — we’ll wire this up later.")
                .foregroundStyle(.secondary)
        }
        .padding()
        .navigationTitle(title)
    }
}
