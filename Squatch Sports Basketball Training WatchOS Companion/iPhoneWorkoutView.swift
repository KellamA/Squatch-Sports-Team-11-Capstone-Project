//
//  iPhoneWorkoutView.swift
//  Squatch Sports Basketball Training WatchOS Companion
//
//  Kaleb — Feb 11
//

import SwiftUI

struct iPhoneWorkoutView: View {
    @State private var workoutActive = false
    @State private var workout: Workout? = nil

    @ObservedObject var connectivity = WorkoutConnectivity.shared
    @State private var lastReceivedValue: Int? = nil

    // Session stats
    @State private var makes: Int = 0
    @State private var misses: Int = 0
    @State private var swishes: Int = 0

    private var totalMakes: Int { makes + swishes }
    private var attempts: Int { totalMakes + misses }

    private var fgPercent: Double {
        guard attempts > 0 else { return 0 }
        return (Double(totalMakes) / Double(attempts)) * 100.0
    }

    var body: some View {
        VStack(spacing: 16) {

            // Stats card
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

            // Start / Stop
            HStack(spacing: 12) {
                Button("Start Workout") {
                    let w = Workout(id: UUID(), startDate: Date(), userValue: nil)
                    workout = w
                    workoutActive = true

                    // Reset stats
                    makes = 0; misses = 0; swishes = 0
                    lastReceivedValue = nil

                    connectivity.sendWorkoutStarted()
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
                .disabled(workoutActive)

                Button("Stop Workout") {
                    workoutActive = false
                    connectivity.sendWorkoutStopped()
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
                .disabled(!workoutActive)
            }

            // Incoming watch data (debug)
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

            // Debug buttons (until watch sends real events)
            VStack(spacing: 10) {
                Text("Quick Log (Debug)")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                HStack(spacing: 10) {
                    Button("MAKE") { if workoutActive { makes += 1 } }
                        .buttonStyle(.borderedProminent)

                    Button("MISS") { if workoutActive { misses += 1 } }
                        .buttonStyle(.bordered)

                    Button("SWISH") { if workoutActive { swishes += 1 } }
                        .buttonStyle(.bordered)
                }
            }

            // Summary
            if let w = workout {
                Divider()
                VStack(alignment: .leading, spacing: 6) {
                    Text("Workout Summary")
                        .font(.title3).bold()

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
        .onReceive(connectivity.$receivedValue) { newValue in
            guard let v = newValue else { return }
            lastReceivedValue = v
            if workoutActive {
                applyWatchValue(v)
            }
        }
    }

    // Map watch integer -> shot event (update these codes if needed)
    private func applyWatchValue(_ v: Int) {
        switch v {
        case 0: misses += 1
        case 1: makes += 1
        case 2: swishes += 1
        default:
            break
        }
    }

    private func stat(_ title: String, _ value: String) -> some View {
        VStack(spacing: 4) {
            Text(value).font(.title3).bold()
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    NavigationStack { iPhoneWorkoutView() }
}
