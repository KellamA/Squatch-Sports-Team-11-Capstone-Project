import SwiftUI

struct ContentView: View {
    @EnvironmentObject var connectivity: WorkoutConnectivity
    @State private var inputValue: String = ""
    @State private var sent: Bool = false

    var body: some View {
        VStack {
            if !connectivity.workoutActive {
                Text("Waiting for workout to start on iPhone...")
                    .padding()
            } else {
                Text("Workout Active! Enter an integer:")
                TextField("Enter integer", text: $inputValue)
                    .frame(width: 100)
                    .padding()
                Button("Send to iPhone") {
                    if let value = Int(inputValue) {
                        connectivity.sendValueToPhone(value)
                        sent = true
                    }
                }
                if sent {
                    Text("Sent!")
                        .foregroundColor(.green)
                }
            }
        }
        .padding()
    }
}

#Preview {
    ContentView().environmentObject(WorkoutConnectivity.shared)
}
