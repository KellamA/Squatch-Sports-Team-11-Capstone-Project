import SwiftUI

struct iPhoneWorkoutView: View {
    let selectedDrill: String

    @EnvironmentObject var appData: AppDataStore
    @ObservedObject var connectivity = WorkoutConnectivity.shared

    @State private var workoutActive = false
    @State private var workout: Workout? = nil
    @State private var workoutStartDate: Date? = nil
    @State private var lastReceivedValue: Int? = nil

    @State private var makes: Int = 0
    @State private var misses: Int = 0
    @State private var swishes: Int = 0

    private var totalMakes: Int { makes + swishes }
    private var attempts: Int { totalMakes + misses }

    private var subtitleText: String {
        if selectedDrill == "Spot Shooting" {
            return workoutActive ? "5 court spots • track each location" : "5 spots around the arc"
        } else if selectedDrill == "Free Throws" {
            return workoutActive ? "Active free throw session" : "25 attempt routine"
        } else if selectedDrill == "Form Shooting" {
            return workoutActive ? "Active form shooting session" : "Close range mechanics"
        } else {
            return workoutActive ? "Active drill session" : "Ready to start"
        }
    }

    private var fgPercent: Double {
        guard attempts > 0 else { return 0 }
        return (Double(totalMakes) / Double(attempts)) * 100.0
    }

    var body: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text(selectedDrill)
                    .font(.title2)
                    .bold()

                Text(subtitleText)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .leading, spacing: 10) {
                Text(workoutActive ? "Active Workout" : "No Active Workout")
                    .font(.headline)

                HStack {
                    stat("Makes", "\(totalMakes)")
                    stat("Misses", "\(misses)")
                    stat("Swish", "\(swishes)")
                    stat("Att", "\(attempts)")
                }

                Text("FG%: \(fgPercent, specifier: "%.1f")")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding()
            .background(.thinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))

            HStack(spacing: 12) {
                Button("Start Workout") {
                    let start = Date()
                    let w = Workout(id: UUID(), startDate: start, userValue: nil)
                    workout = w
                    workoutStartDate = start
                    workoutActive = true

                    makes = 0
                    misses = 0
                    swishes = 0
                    lastReceivedValue = nil

                    connectivity.sendWorkoutStarted()
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
                .disabled(workoutActive)

                Button("Stop Workout") {
                    let endDate = Date()

                    if workoutActive && attempts > 0 {
                        let session = WorkoutSession(
                            drill: selectedDrill,
                            makes: totalMakes,
                            misses: misses,
                            swishes: swishes,
                            attempts: attempts,
                            startDate: workoutStartDate ?? endDate,
                            endDate: endDate
                        )
                        appData.addSession(session)
                    }

                    workoutActive = false
                    connectivity.sendWorkoutStopped()
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
                .disabled(!workoutActive)
            }

            Group {
                if let v = lastReceivedValue {
                    Text("Last watch code: \(v)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Text("Waiting for watch data…")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            VStack(spacing: 10) {
                Text("Quick Log (Debug)")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                HStack(spacing: 10) {
                    Button("MAKE") {
                        if workoutActive { makes += 1 }
                    }
                    .buttonStyle(.borderedProminent)

                    Button("MISS") {
                        if workoutActive { misses += 1 }
                    }
                    .buttonStyle(.bordered)

                    Button("SWISH") {
                        if workoutActive { swishes += 1 }
                    }
                    .buttonStyle(.bordered)
                }
            }

            if let w = workout {
                Divider()

                VStack(alignment: .leading, spacing: 6) {
                    Text("Workout Summary")
                        .font(.title3)
                        .bold()

                    Text("Drill: \(selectedDrill)")
                        .foregroundStyle(.secondary)

                    Text("Started: \(w.startDate.formatted())")
                        .foregroundStyle(.secondary)

                    Text("Makes: \(totalMakes)  Misses: \(misses)  Swishes: \(swishes)")
                    Text("Attempts: \(attempts)   FG%: \(fgPercent, specifier: "%.1f")")
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Workout")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(connectivity.$receivedValue) { newValue in
            guard let v = newValue else { return }
            lastReceivedValue = v
            if workoutActive {
                applyWatchValue(v)
            }
        }
    }

    private func applyWatchValue(_ v: Int) {
        switch v {
        case 0:
            misses += 1
        case 1:
            makes += 1
        case 2:
            swishes += 1
        default:
            break
        }
    }

    private func stat(_ title: String, _ value: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title3)
                .bold()

            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    NavigationStack {
        iPhoneWorkoutView(selectedDrill: "Form Shooting")
            .environmentObject(AppDataStore())
    }
}
