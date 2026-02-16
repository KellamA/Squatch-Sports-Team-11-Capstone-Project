// iPhoneWorkoutView.swift
import SwiftUI

struct iPhoneWorkoutView: View {
    @State private var workoutActive = false
    @State private var workout: Workout? = nil
    @ObservedObject var connectivity = WorkoutConnectivity.shared
    @State private var receivedValue: Int? = nil

    var body: some View {
        VStack(spacing: 30) {
            if !workoutActive {
                Button("Start Workout") {
                    let w = Workout(id: UUID(), startDate: Date(), userValue: nil)
                    workout = w
                    workoutActive = true
                    connectivity.sendWorkoutStarted()
                    receivedValue = nil
                }
            } else {
                Button("Stop Workout") {
                    workoutActive = false
                    connectivity.sendWorkoutStopped()
                }
                .foregroundColor(.red)
                if let value = receivedValue ?? connectivity.receivedValue {
                    Text("Received value from watch: \(value)")
                        .font(.headline)
                        .foregroundStyle(.green)
                    Button("Save to Workout") {
                        if var w = workout {
                            w.userValue = value
                            workout = w
                        }
                    }
                } else {
                    Text("Waiting for value from watch...")
                        .font(.subheadline)
                }
            }

            if let w = workout, let val = w.userValue {
                Divider()
                Text("Workout Summary")
                    .font(.title2)
                Text("Started: \(w.startDate.formatted())")
                Text("User Value: \(val)")
            }
        }
        .padding()
        .onReceive(connectivity.$receivedValue) { newValue in
            receivedValue = newValue
        }
    }
}

#Preview {
    iPhoneWorkoutView()
}
