import SwiftUI

extension Color {
    static let streakOrange = Color(red: 1.0, green: 0.584, blue: 0.0)
    static let goalGreen = Color(red: 0.204, green: 0.78, blue: 0.349)
}

/// Dashboard card with the current and best streak, the last 7 days, and the next milestone.
struct StreakCard: View {
    let streak: StreakResult
    let trainingDays: Set<Date>

    @State private var flameScale: CGFloat = 1

    private var active: Bool { streak.current > 0 }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                Text("🔥")
                    .font(.system(size: 40))
                    .scaleEffect(flameScale)
                    .grayscale(active ? 0 : 1)
                    .opacity(active ? 1 : 0.4)

                VStack(alignment: .leading, spacing: 2) {
                    Text("\(streak.current)-day streak")
                        .font(.title2)
                        .bold()
                        .foregroundStyle(active ? Color.streakOrange : .primary)

                    Text(statusText)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                VStack(spacing: 2) {
                    Text("\(streak.best)")
                        .font(.headline)
                    Text("Best")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            WeekStrip(trainingDays: trainingDays)

            if let next = StreakCalculator.nextMilestone(after: streak.current) {
                let previous = StreakCalculator.milestones.last { $0 <= streak.current } ?? 0
                let remaining = next - streak.current

                VStack(alignment: .leading, spacing: 4) {
                    ProgressView(value: Double(streak.current - previous), total: Double(next - previous))
                        .tint(.streakOrange)

                    Text("\(remaining) more training day\(remaining == 1 ? "" : "s") to reach \(next)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .task(id: active) {
            // Pulse a few times, then stop. An endless animation keeps the screen redrawing.
            guard active else { return }
            for _ in 0..<3 {
                withAnimation(.easeInOut(duration: 0.35)) { flameScale = 1.15 }
                try? await Task.sleep(for: .milliseconds(350))
                withAnimation(.easeInOut(duration: 0.35)) { flameScale = 1 }
                try? await Task.sleep(for: .milliseconds(350))
            }
        }
    }

    private var statusText: String {
        guard streak.current > 0, let last = streak.lastTrainingDay else {
            return "Log a workout to start a streak"
        }
        let today = Calendar.current.startOfDay(for: Date())
        switch StreakCalculator.daysBetween(last, today) {
        case 0: return "You trained today. Streak is safe!"
        case 1: return "Train today to grow it, or take a rest day"
        default: return "Rest day used. Train today to keep it alive!"
        }
    }
}

/// The last 7 days: filled = trained, outlined = no workout, bold ring = today.
private struct WeekStrip: View {
    let trainingDays: Set<Date>

    private var days: [Date] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        return (0...6).reversed().compactMap { calendar.date(byAdding: .day, value: -$0, to: today) }
    }

    var body: some View {
        HStack {
            ForEach(days, id: \.self) { day in
                let trained = trainingDays.contains(day)
                let isToday = Calendar.current.isDateInToday(day)

                VStack(spacing: 4) {
                    Text(day.formatted(.dateTime.weekday(.narrow)))
                        .font(.caption2)
                        .fontWeight(isToday ? .bold : .regular)
                        .foregroundStyle(.secondary)

                    ZStack {
                        Circle()
                            .fill(trained ? Color.streakOrange : Color.clear)
                        Circle()
                            .stroke(isToday ? Color.primary : Color.secondary.opacity(0.4), lineWidth: isToday ? 2 : 1)
                        if trained {
                            Text("🔥")
                                .font(.system(size: 13))
                        }
                    }
                    .frame(width: 30, height: 30)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

#Preview {
    StreakCard(
        streak: StreakResult(current: 4, best: 6, lastTrainingDay: Calendar.current.startOfDay(for: Date())),
        trainingDays: [Calendar.current.startOfDay(for: Date())]
    )
    .padding()
}
