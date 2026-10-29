import Foundation
import SwiftUI

struct CourtPosition: Codable, Equatable {
    let row: Int
    let column: Int
    let name: String?
    let rowPercent: Double
    let columnPercent: Double

    init(row: Int, column: Int, name: String? = nil, rowPercent: Double, columnPercent: Double) {
        self.row = row
        self.column = column
        self.name = name
        self.rowPercent = rowPercent
        self.columnPercent = columnPercent
    }
}

struct CourtPositions {

    static let spotShooting: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Left Corner", rowPercent: 0.20, columnPercent: 0.10),
        CourtPosition(row: 0, column: 1, name: "Left Wing", rowPercent: 0.54, columnPercent: 0.27),
        CourtPosition(row: 0, column: 2, name: "Top of Key", rowPercent: 0.64, columnPercent: 0.50),
        CourtPosition(row: 0, column: 3, name: "Right Wing", rowPercent: 0.54, columnPercent: 0.73),
        CourtPosition(row: 0, column: 4, name: "Right Corner", rowPercent: 0.20, columnPercent: 0.90)
    ]

    static let freeThrow: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Free Throw Line", rowPercent: 0.37, columnPercent: 0.50)
    ]

    static let formShooting: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Close Range", rowPercent: 0.22, columnPercent: 0.50)
    ]

    static let midrangeSeries: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Left Elbow", rowPercent: 0.43, columnPercent: 0.32),
        CourtPosition(row: 0, column: 1, name: "Right Elbow", rowPercent: 0.43, columnPercent: 0.68),
        CourtPosition(row: 0, column: 2, name: "Left Short Corner", rowPercent: 0.22, columnPercent: 0.22),
        CourtPosition(row: 0, column: 3, name: "Right Short Corner", rowPercent: 0.22, columnPercent: 0.78)
    ]

    static let threePointSeries: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Left Corner", rowPercent: 0.20, columnPercent: 0.10),
        CourtPosition(row: 0, column: 1, name: "Left Wing", rowPercent: 0.54, columnPercent: 0.27),
        CourtPosition(row: 0, column: 2, name: "Top of Key", rowPercent: 0.64, columnPercent: 0.50),
        CourtPosition(row: 0, column: 3, name: "Right Wing", rowPercent: 0.54, columnPercent: 0.73),
        CourtPosition(row: 0, column: 4, name: "Right Corner", rowPercent: 0.20, columnPercent: 0.90)
    ]

    static let catchAndShoot: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Catch & Shoot Spot", rowPercent: 0.50, columnPercent: 0.50)
    ]

    static let offTheDribble: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Off Dribble Spot", rowPercent: 0.50, columnPercent: 0.50)
    ]

    static let finishing: [CourtPosition] = [
        CourtPosition(row: 0, column: 0, name: "Rim Finish", rowPercent: 0.20, columnPercent: 0.50)
    ]

    static let defaultPosition = CourtPosition(
        row: 0,
        column: 0,
        name: "Center",
        rowPercent: 0.50,
        columnPercent: 0.50
    )

    static func positions(for drillName: String) -> [CourtPosition] {
        switch drillName {
        case "Spot Shooting":
            return spotShooting
        case "Free Throws":
            return freeThrow
        case "Form Shooting":
            return formShooting
        case "Midrange Series":
            return midrangeSeries
        case "3PT Series":
            return threePointSeries
        case "Catch & Shoot":
            return catchAndShoot
        case "Off the Dribble":
            return offTheDribble
        case "Finishing":
            return finishing
        default:
            return [defaultPosition]
        }
    }
}

// MARK: - Court Drawing
// Shared by the watch shot logger and the iPhone 1v1 match screen.

struct SimplifiedCourtView: View {
    /// Colors the edges to match the watch swipe directions (green = make, red = miss, pink = swish).
    /// Turn off where the colors would mean something else, like the red/blue 1v1 screen.
    var showsSwipeEdges = true

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let height = geo.size.height
            
            ZStack {
                // WOOD FLOOR - Brighter polished look
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 0.85, green: 0.70, blue: 0.52),
                                Color(red: 0.78, green: 0.62, blue: 0.45),
                                Color(red: 0.82, green: 0.66, blue: 0.48)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                
                // Polished shine overlay
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.12),
                                Color.clear,
                                Color.white.opacity(0.08)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                
                // Wood grain lines - smaller planks for watch size
                ForEach(0..<20, id: \.self) { i in
                    Rectangle()
                        .fill(Color.black.opacity(0.08))
                        .frame(width: 1, height: height)
                        .position(x: CGFloat(i) * width / 20 + width / 40, y: height / 2)
                }
                
                // Horizontal wood plank lines
                ForEach(0..<12, id: \.self) { i in
                    Rectangle()
                        .fill(Color.black.opacity(0.06))
                        .frame(width: width, height: 0.5)
                        .position(x: width / 2, y: CGFloat(i) * height / 12 + height / 24)
                }
                
                // Outer court boundary - WHITE LINES with colored sections
                // Top edge - GREEN
                Rectangle()
                    .fill(showsSwipeEdges ? Color.green.opacity(0.6) : Color.white)
                    .frame(width: width, height: 4)
                    .position(x: width / 2, y: 2)

                // Bottom edge - RED
                Rectangle()
                    .fill(showsSwipeEdges ? Color.red.opacity(0.6) : Color.white)
                    .frame(width: width, height: 4)
                    .position(x: width / 2, y: height - 2)

                // Left edge - WHITE
                Rectangle()
                    .fill(Color.white)
                    .frame(width: 4, height: height)
                    .position(x: 2, y: height / 2)

                // Right edge - PINK
                Rectangle()
                    .fill(showsSwipeEdges ? Color.pink.opacity(0.6) : Color.white)
                    .frame(width: 4, height: height)
                    .position(x: width - 2, y: height / 2)
                
                // HOOP AT TOP (rim)
                Circle()
                    .fill(Color.red.opacity(0.9))
                    .frame(width: width * 0.095, height: width * 0.095)
                    .position(x: width / 2, y: height * 0.14)
                
                Circle()
                    .stroke(Color.orange, lineWidth: 2)
                    .frame(width: width * 0.095, height: width * 0.095)
                    .position(x: width / 2, y: height * 0.14)
                
                // Backboard - More detailed rectangular shape
                ZStack {
                    // Main backboard rectangle
                    Rectangle()
                        .fill(Color.white.opacity(0.85))
                        .frame(width: width * 0.24, height: height * 0.055)
                    
                    // Border to make it stand out
                    Rectangle()
                        .stroke(Color.white, lineWidth: 2)
                        .frame(width: width * 0.24, height: height * 0.055)
                    
                    // Inner box (target square)
                    Rectangle()
                        .stroke(Color.white, lineWidth: 1.5)
                        .frame(width: width * 0.12, height: height * 0.035)
                }
                .position(x: width / 2, y: height * 0.085)
                
                // Paint area (rectangular, proportional)
                Rectangle()
                    .stroke(Color.white, lineWidth: 2)
                    .frame(width: width * 0.36, height: height * 0.29)
                    .position(x: width / 2, y: height * 0.26)
                
                // Free throw line (matches paint width)
                Rectangle()
                    .fill(Color.white)
                    .frame(width: width * 0.36, height: 2)
                    .position(x: width / 2, y: height * 0.37)
                
                // Large solid semi-circle BELOW the free throw line (bottom half of circle)
                Circle()
                    .trim(from: 0.0, to: 0.5)
                    .stroke(Color.white, lineWidth: 2)
                    .frame(width: width * 0.36, height: width * 0.36)
                    .position(x: width / 2, y: height * 0.37)
                
                // Dashed semi-circle at TOP of the key (inside the paint, near backboard)
                Circle()
                    .trim(from: 0.5, to: 1.0) // Bottom half of circle
                    .stroke(Color.white, style: StrokeStyle(lineWidth: 2, dash: [3, 2]))
                    .frame(width: width * 0.36, height: width * 0.36)
                    .rotationEffect(.degrees(180)) // Flip it so the arc faces down
                    .position(x: width / 2, y: height * 0.13)
                
                // Lane hash marks - Left side
                ForEach(0..<4, id: \.self) { i in
                    Rectangle()
                        .fill(Color.white)
                        .frame(width: 8, height: 2)
                        .position(x: width / 2 - width * 0.18, y: height * 0.15 + CGFloat(i) * height * 0.055)
                }
                
                // Lane hash marks - Right side
                ForEach(0..<4, id: \.self) { i in
                    Rectangle()
                        .fill(Color.white)
                        .frame(width: 8, height: 2)
                        .position(x: width / 2 + width * 0.18, y: height * 0.15 + CGFloat(i) * height * 0.055)
                }
                
                // THREE-POINT LINE (larger, fills watch properly)
                Path { path in
                    let centerX = width / 2
                    let centerY = height * 0.12
                    let radius = width * 0.52
                    
                    // Arc
                    path.addArc(
                        center: CGPoint(x: centerX, y: centerY),
                        radius: radius,
                        startAngle: .degrees(38),
                        endAngle: .degrees(142),
                        clockwise: false
                    )
                    
                    // Calculate endpoints
                    let angle = 38.0 * .pi / 180.0
                    let leftX = centerX - radius * cos(angle)
                    let leftY = centerY + radius * sin(angle)
                    let rightX = centerX + radius * cos(angle)
                    let rightY = centerY + radius * sin(angle)
                    
                    // Left corner line UP to top (with spacing)
                    path.move(to: CGPoint(x: leftX, y: leftY))
                    path.addLine(to: CGPoint(x: leftX, y: height * 0.06))
                    
                    // Right corner line UP to top (with spacing)
                    path.move(to: CGPoint(x: rightX, y: rightY))
                    path.addLine(to: CGPoint(x: rightX, y: height * 0.06))
                }
                .stroke(Color.white, lineWidth: 2.5)
                
                // SQUATCH LOGO at center court with circle background
                ZStack {
                    // White circle background
                    Circle()
                        .fill(Color.white.opacity(0.85))
                        .frame(width: width * 0.28, height: width * 0.28)
                    
                    // Black ring border
                    Circle()
                        .stroke(Color.black, lineWidth: 2)
                        .frame(width: width * 0.28, height: width * 0.28)
                    
                    // Logo
                    Image("Mascot_BigFoot")
                        .resizable()
                        .scaledToFit()
                        .frame(width: width * 0.22, height: width * 0.22)
                }
                .position(x: width / 2, y: height * 0.78)
            }
        }
    }
}
