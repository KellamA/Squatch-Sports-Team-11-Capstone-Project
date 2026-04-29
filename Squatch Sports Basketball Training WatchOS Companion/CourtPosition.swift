import Foundation

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
