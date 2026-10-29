import SwiftUI

struct GoalsView: View {
    @EnvironmentObject var appData: AppDataStore

    @State private var editing: GoalType?
    @State private var editText = ""

    var body: some View {
        let progress = appData.goalProgress
        let completed = progress.filter(\.isComplete).count

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("\(completed) of \(progress.count) goals complete")
                    .font(.headline)

                ForEach(progress, id: \.type) { goal in
                    GoalDetailCard(goal: goal) {
                        editText = "\(appData.goalTargets.target(for: goal.type))"
                        editing = goal.type
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Goals")
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            editing.map { "Edit \($0.title) goal" } ?? "",
            isPresented: Binding(get: { editing != nil }, set: { if !$0 { editing = nil } }),
            presenting: editing
        ) { type in
            TextField("Target (\(type.unit))", text: $editText)
                .keyboardType(.numberPad)
            Button("Save") {
                if let value = Int(editText), type.range.contains(value) {
                    appData.updateGoal(type, to: value)
                }
            }
            .disabled(!isValid(editText, for: type))
            Button("Cancel", role: .cancel) { }
        } message: { type in
            Text("Between \(type.range.lowerBound) and \(type.range.upperBound)")
        }
    }

    private func isValid(_ text: String, for type: GoalType) -> Bool {
        guard let value = Int(text) else { return false }
        return type.range.contains(value)
    }
}

/// Animated progress ring: orange while in progress, green when complete.
struct GoalRing<Content: View>: View {
    let goal: GoalProgress
    let size: CGFloat
    let lineWidth: CGFloat
    @ViewBuilder let content: Content

    @State private var animatedFraction: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.secondary.opacity(0.2), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: animatedFraction)
                .stroke(
                    goal.isComplete ? Color.goalGreen : Color.streakOrange,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
            content
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(.easeOut(duration: 0.9)) { animatedFraction = goal.fraction }
        }
        .onChange(of: goal.fraction) { _, newValue in
            withAnimation(.easeOut(duration: 0.9)) { animatedFraction = newValue }
        }
    }
}

/// Label shown inside a goal ring.
struct GoalRingLabel: View {
    let goal: GoalProgress
    var font: Font = .subheadline

    var body: some View {
        Text(goal.isComplete ? "✓" : "\(Int(goal.fraction * 100))%")
            .font(font)
            .bold()
            .foregroundStyle(goal.isComplete ? Color.goalGreen : .primary)
    }
}

/// Compact ring row for the dashboard. Tapping it opens the Goals screen.
struct TodayGoalsCard: View {
    let goals: [GoalProgress]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Goals")
                .font(.headline)

            HStack {
                ForEach(goals.filter { $0.type != .targetFG }, id: \.type) { goal in
                    VStack(spacing: 4) {
                        GoalRing(goal: goal, size: 72, lineWidth: 7) {
                            GoalRingLabel(goal: goal)
                        }
                        .padding(.bottom, 2)

                        Text(goal.type.title)
                            .font(.caption)
                            .foregroundStyle(.primary)
                        Text("\(goal.current)/\(goal.target)")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .contentShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct GoalDetailCard: View {
    let goal: GoalProgress
    let onEdit: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            GoalRing(goal: goal, size: 64, lineWidth: 6) {
                GoalRingLabel(goal: goal, font: .callout)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(goal.type.title)
                    .font(.headline)
                Text(progressText)
                    .foregroundStyle(.secondary)
                Text(statusText)
                    .font(.caption)
                    .foregroundStyle(goal.isComplete ? Color.goalGreen : .secondary)
            }

            Spacer()

            Button(action: onEdit) {
                Image(systemName: "pencil")
                    .font(.title3)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("Edit \(goal.type.title) goal")
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var progressText: String {
        switch goal.type {
        case .targetFG: "\(goal.current)% today • target \(goal.target)%"
        default: "\(goal.current) / \(goal.target) \(goal.type.unit)"
        }
    }

    private var statusText: String {
        if goal.isComplete { return "Complete! 🏆" }
        switch goal.type {
        case .targetFG where !goal.eligible:
            return "Take at least \(GoalTracker.minFGAttempts) shots today to count"
        case .targetFG:
            return "\(goal.target - goal.current)% to go"
        case .weeklySessions:
            return "\(goal.target - goal.current) more this week"
        default:
            return "\(goal.target - goal.current) more today"
        }
    }
}

#Preview {
    NavigationStack {
        GoalsView()
            .environmentObject(AppDataStore())
    }
}
