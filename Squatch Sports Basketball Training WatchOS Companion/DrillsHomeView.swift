import SwiftUI

struct DrillOption: Identifiable {
    let id = UUID()
    let name: String
    let subtitle: String
    let systemImage: String
    let instructions: [String]
    let targetText: String
    let isAvailable: Bool
    let courtSpots: [String]
    let shotsPerSpot: Int
}

struct DrillsHomeView: View {
    private let drills: [DrillOption] = [
        DrillOption(
            name: "Form Shooting",
            subtitle: "Close range mechanics",
            systemImage: "target",
            instructions: [
                "Start close to the basket.",
                "Focus on arc, touch, and follow-through.",
                "Try to make 20 before moving back."
            ],
            targetText: "Target: 20 makes close range",
            isAvailable: true,
            courtSpots: [],
            shotsPerSpot: 0
        ),
        DrillOption(
            name: "Free Throws",
            subtitle: "Routine + consistency",
            systemImage: "circle.dotted",
            instructions: [
                "Use the same routine every rep.",
                "Track makes out of 25 attempts.",
                "Focus on balance and finish."
            ],
            targetText: "Target: 20/25 free throws",
            isAvailable: true,
            courtSpots: [],
            shotsPerSpot: 0
        ),
        DrillOption(
            name: "Spot Shooting",
            subtitle: "5 spots around the arc",
            systemImage: "mappin.and.ellipse",
            instructions: [
                "Shoot from 5 court spots.",
                "Move to the next spot after finishing the current one.",
                "Track makes and misses for each location."
            ],
            targetText: "Target: 5 spots, 10 shots each",
            isAvailable: true,
            courtSpots: [
                "Left Corner",
                "Left Wing",
                "Top of Key",
                "Right Wing",
                "Right Corner"
            ],
            shotsPerSpot: 10
        ),
        DrillOption(
            name: "Catch & Shoot",
            subtitle: "Quick feet + release",
            systemImage: "bolt.fill",
            instructions: [
                "Start ready to catch and shoot.",
                "Focus on quick footwork and balance.",
                "Track makes out of 25 or 50 shots."
            ],
            targetText: "Target: 25 makes catch-and-shoot",
            isAvailable: true,
            courtSpots: [],
            shotsPerSpot: 5
        ),
        DrillOption(
            name: "Off the Dribble",
            subtitle: "Create space shots",
            systemImage: "shuffle",
            instructions: [
                "Use one or two dribbles before each shot.",
                "Work on balance after creating space.",
                "Track makes from both directions."
            ],
            targetText: "Target: 20 makes off the dribble",
            isAvailable: true,
            courtSpots: [],
            shotsPerSpot: 5
        ),
        DrillOption(
            name: "Midrange Series",
            subtitle: "Elbows + short corners",
            systemImage: "square.grid.2x2",
            instructions: [
                "Work from both elbows and both short corners.",
                "Focus on rhythm and consistency.",
                "Track total makes across all spots."
            ],
            targetText: "Target: 5 makes from each midrange spot",
            isAvailable: true,
            courtSpots: [
                "Left Elbow",
                "Right Elbow",
                "Left Short Corner",
                "Right Short Corner"
            ],
            shotsPerSpot: 5
        ),
        DrillOption(
            name: "3PT Series",
            subtitle: "Volume + rhythm",
            systemImage: "basketball.fill",
            instructions: [
                "Shoot game-speed threes from around the arc.",
                "Focus on rhythm, balance, and follow-through.",
                "Track makes over a full shooting set."
            ],
            targetText: "Target: 25 made threes",
            isAvailable: true,
            courtSpots: [
                "Left Corner",
                "Left Wing",
                "Top of Key",
                "Right Wing",
                "Right Corner"
            ],
            shotsPerSpot: 5
        ),
        DrillOption(
            name: "Finishing",
            subtitle: "Rim touch & angles",
            systemImage: "figure.run",
            instructions: [
                "Finish at the rim from different angles.",
                "Work both left and right hand finishes.",
                "Track makes and focus on touch."
            ],
            targetText: "Target: 20 made finishes",
            isAvailable: true,
            courtSpots: [],
            shotsPerSpot: 5
        )
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
                                Text(drill.name)
                                    .font(.headline)

                                Text(drill.subtitle)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }

                            Spacer()

                            if !drill.isAvailable {
                                Text("Soon")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 6)
                    }
                }
            }
        }
        .navigationTitle("Drills")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct DrillDetailView: View {
    let drill: DrillOption

    private var totalPlannedShots: Int {
        drill.courtSpots.count * drill.shotsPerSpot
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Image(systemName: drill.systemImage)
                        .font(.system(size: 30, weight: .semibold))
                        .symbolRenderingMode(.hierarchical)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(drill.name)
                            .font(.title3)
                            .bold()

                        Text(drill.subtitle)
                            .foregroundStyle(.secondary)
                    }
                }

                Divider()

                VStack(alignment: .leading, spacing: 8) {
                    Text("Goal")
                        .font(.headline)

                    Text(drill.targetText)
                        .foregroundStyle(.secondary)
                }

                if !drill.courtSpots.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Court Spots")
                            .font(.headline)

                        ForEach(Array(drill.courtSpots.enumerated()), id: \.offset) { index, spot in
                            HStack {
                                Text("\(index + 1). \(spot)")
                                Spacer()
                                Text("\(drill.shotsPerSpot) shots")
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 2)
                        }

                        Text("Total planned shots: \(totalPlannedShots)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.thinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Instructions")
                        .font(.headline)

                    ForEach(drill.instructions, id: \.self) { step in
                        Text("• \(step)")
                            .foregroundStyle(.secondary)
                    }
                }

                if drill.isAvailable {
                    NavigationLink {
                        iPhoneWorkoutView(selectedDrill: drill.name)
                    } label: {
                        Text("Start Drill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                } else {
                    Button {
                    } label: {
                        Text("Start Drill (Coming Soon)")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(true)
                }
            }
            .padding()
        }
        .navigationTitle(drill.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
