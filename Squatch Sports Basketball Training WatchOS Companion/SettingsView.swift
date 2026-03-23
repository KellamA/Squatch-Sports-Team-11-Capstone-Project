import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appData: AppDataStore
    @State private var notificationsEnabled = true
    @State private var soundEnabled = true
    @State private var watchSyncEnabled = true
    @State private var hapticsEnabled = true

    var body: some View {
        Form {
            Section("Preferences") {
                Toggle("Notifications", isOn: $notificationsEnabled)
                Toggle("Sound Effects", isOn: $soundEnabled)
                Toggle("Apple Watch Sync", isOn: $watchSyncEnabled)
                Toggle("Haptics", isOn: $hapticsEnabled)
            }

            Section("Watch Status") {
                HStack {
                    Text("Connection")
                    Spacer()
                    Text(watchSyncEnabled ? "Connected" : "Disconnected")
                        .foregroundStyle(watchSyncEnabled ? .green : .secondary)
                }

                HStack {
                    Text("Last Sync")
                    Spacer()
                    Text("Just now")
                        .foregroundStyle(.secondary)
                }
            }

            Section("Data") {
                Button("Reset Sample Data", role: .destructive) {
                    appData.resetSampleData()
                }
            }

            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0")
                        .foregroundStyle(.secondary)
                }

                HStack {
                    Text("App")
                    Spacer()
                    Text("Squatch Sports")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}
