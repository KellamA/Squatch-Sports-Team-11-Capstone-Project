#if os(watchOS)
import SwiftUI

@main
struct SquatchSportsWatchOSCompanionApp: App {
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            Group {
                if showSplash {
                    SplashView()
                        .background(Color.white)
                        .environment(\.colorScheme, .light)
                        .transition(.opacity)
                        .task {
                            try? await Task.sleep(for: .seconds(1.8))
                            await MainActor.run {
                                withAnimation(.easeInOut(duration: 0.25)) {
                                    showSplash = false
                                }
                            }
                        }
                } else {
                    WatchRootView()
                        .environmentObject(WorkoutConnectivity.shared)
                }
            }
            .animation(.easeInOut(duration: 0.25), value: showSplash)
        }
    }
}

struct WatchRootView: View {
    @EnvironmentObject var connectivity: WorkoutConnectivity
    @State private var inputValue: String = ""
    @State private var sent: Bool = false

    var body: some View {
        VStack {
            if !connectivity.workoutActive {
                Text("Waiting for workout to start on iPhone...")
                    .multilineTextAlignment(.center)
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
#endif

