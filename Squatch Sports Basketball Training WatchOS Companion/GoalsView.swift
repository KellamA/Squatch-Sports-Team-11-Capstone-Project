import SwiftUI

struct GoalsView: View {
    @EnvironmentObject var appData: AppDataStore

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                GoalProgressCard(
                    title: "Daily Shot Goal",
                    current: appData.todayAttempts,
                    target: appData.dailyShotGoal,
                    label: "Shots"
                )

                GoalProgressCard(
                    title: "Weekly Session Goal",
                    current: appData.weeklySessionsCompleted,
                    target: appData.weeklySessionGoal,
                    label: "Sessions"
                )

                GoalProgressCard(
                    title: "Make Goal",
                    current: appData.todayMakes,
                    target: 100,
                    label: "Makes"
                )

                VStack(alignment: .leading, spacing: 8) {
                    Text("Target FG%")
                        .font(.headline)

                    Text("\(appData.targetFGGoal)%")
                        .font(.title2)
                        .bold()

                    Text("Current FG% is \(appData.shootingPercentage)%. \(goalMessage)")
                        .foregroundStyle(.secondary)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.thinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding()
        }
        .navigationTitle("Goals")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var goalMessage: String {
        if appData.shootingPercentage >= appData.targetFGGoal {
            return "You are hitting your target."
        } else {
            return "Keep shooting to close the gap."
        }
    }
}

struct GoalProgressCard: View {
    let title: String
    let current: Int
    let target: Int
    let label: String

    var progress: Double {
        guard target > 0 else { return 0 }
        return min(Double(current) / Double(target), 1.0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.headline)

            Text("\(current) / \(target) \(label)")
                .foregroundStyle(.secondary)

            ProgressView(value: progress)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
