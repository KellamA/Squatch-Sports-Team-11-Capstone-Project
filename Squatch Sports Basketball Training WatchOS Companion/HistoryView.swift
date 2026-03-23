import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var appData: AppDataStore

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if appData.sessions.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 34))

                        Text("No workout history yet")
                            .font(.headline)

                        Text("Complete a workout and your session history will show here.")
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 100)
                } else {
                    ForEach(appData.sessions) { session in
                        HistoryRow(session: session)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct HistoryRow: View {
    let session: WorkoutSession

    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: session.endDate)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "basketball")
                .font(.system(size: 22))
                .padding(10)
                .background(Color(.systemGray6))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 6) {
                Text(session.drill)
                    .font(.headline)

                Text("\(session.makes)/\(session.attempts) shots")
                    .foregroundStyle(.secondary)

                Text(formattedDate)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }

            Spacer()

            Text("\(session.percentage)%")
                .font(.headline)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(.systemGray6))
                .clipShape(Capsule())
        }
        .padding()
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}
