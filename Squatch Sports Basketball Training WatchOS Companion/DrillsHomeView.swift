//
//  DrillsHomeView.swift
//  Squatch Sports Basketball Training WatchOS Companion
//
//  Created by kaleb juniour on 2/18/26.
//

import SwiftUI

struct DrillOption: Identifiable {
    let id = UUID()
    let name: String
    let subtitle: String
    let systemImage: String
}

struct DrillsHomeView: View {
    private let drills: [DrillOption] = [
        DrillOption(name: "Form Shooting", subtitle: "Close range mechanics", systemImage: "target"),
        DrillOption(name: "Free Throws", subtitle: "Routine + consistency", systemImage: "circle.dotted"),
        DrillOption(name: "Spot Shooting", subtitle: "5 spots around the arc", systemImage: "mappin.and.ellipse"),
        DrillOption(name: "Catch & Shoot", subtitle: "Quick feet + release", systemImage: "bolt.fill"),
        DrillOption(name: "Off the Dribble", subtitle: "Create space shots", systemImage: "shuffle"),
        DrillOption(name: "Midrange Series", subtitle: "Elbows + short corners", systemImage: "square.grid.2x2"),
        DrillOption(name: "3PT Series", subtitle: "Volume + rhythm", systemImage: "basketball.fill"),
        DrillOption(name: "Finishing", subtitle: "Rim touch & angles", systemImage: "figure.run")
    ]

    var body: some View {
        List {
            Section("Drills") {
                ForEach(drills) { drill in
                    NavigationLink {
                        DrillDetailView(drill: drill)
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: drill.systemImage)
                                .font(.system(size: 18, weight: .semibold))
                                .frame(width: 28)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(drill.name).font(.headline)
                                Text(drill.subtitle)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 6)
                    }
                }
            }
        }
        .navigationTitle("Drills")
    }
}

struct DrillDetailView: View {
    let drill: DrillOption

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: drill.systemImage)
                    .font(.system(size: 30, weight: .semibold))
                    .symbolRenderingMode(.hierarchical)

                VStack(alignment: .leading, spacing: 2) {
                    Text(drill.name).font(.title3).bold()
                    Text(drill.subtitle).foregroundStyle(.secondary)
                }
            }

            Divider()

            VStack(alignment: .leading, spacing: 8) {
                Text("Goal").font(.headline)
                Text("This page is a placeholder. Next we’ll connect it to real workout tracking + watch events.")
                    .foregroundStyle(.secondary)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Planned features").font(.headline)
                Text("• Start/stop drill\n• Target makes/attempts\n• Receive makes/misses/swishes from Watch\n• Save session summary")
                    .foregroundStyle(.secondary)
            }

            Button { } label: {
                Text("Start Drill (Coming Soon)")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(true)

            Spacer()
        }
        .padding()
        .navigationTitle(drill.name)
    }
}
