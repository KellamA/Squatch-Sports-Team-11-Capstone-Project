//
//  OneOnOneUITests.swift
//  Squatch Sports Basketball Training WatchOS CompanionUITests
//
//  Plays a full 1v1 match: add a friend, start a match, log plays, win, and save.
//

import XCTest

final class OneOnOneUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting"]
        app.launch()
    }

    @MainActor
    func testPlayAndSaveAMatch() throws {
        // Dashboard → 1v1
        openOneOnOne()

        // Add a friend
        app.buttons["Add Friend"].tap()
        let nameField = app.alerts.textFields.firstMatch
        XCTAssertTrue(nameField.waitForExistence(timeout: 2))
        nameField.typeText("Kaleb")
        app.alerts.buttons["Add"].tap()
        XCTAssertTrue(app.staticTexts["Kaleb"].waitForExistence(timeout: 2))
        attachScreenshot("1 - 1v1 home")

        // Start a match with the default rules (first to 11, 1s and 2s, win by 2)
        app.buttons["Play"].tap()
        let start = app.buttons["Start"]
        XCTAssertTrue(start.waitForExistence(timeout: 2))
        attachScreenshot("2 - match setup")
        start.tap()

        let redPlusTwo = app.buttons["Red plus 2"]
        XCTAssertTrue(redPlusTwo.waitForExistence(timeout: 3))

        // A few plays, including fouls and a turnover
        redPlusTwo.tap()
        app.buttons["Blue foul"].tap()
        app.buttons["Blue plus 1"].tap()
        app.buttons["Red turnover"].tap()
        attachScreenshot("3 - match in progress")

        // Red runs away with it: 2 + 5 × 2 = 12, Blue has 1
        for _ in 0..<5 { redPlusTwo.tap() }

        let save = app.buttons["Save Match"]
        XCTAssertTrue(save.waitForExistence(timeout: 2))
        attachScreenshot("4 - game over")
        save.tap()

        // Back on the 1v1 screen with the game in history
        XCTAssertTrue(app.staticTexts["12 – 1 vs Kaleb"].waitForExistence(timeout: 3))
        attachScreenshot("5 - record updated")
    }

    @MainActor
    func testUndoRemovesTheLastPlay() throws {
        openOneOnOne()
        app.buttons["Add Friend"].tap()
        app.alerts.textFields.firstMatch.typeText("Kaleb")
        app.alerts.buttons["Add"].tap()
        app.buttons["Play"].tap()
        app.buttons["Start"].tap()

        let bluePlusTwo = app.buttons["Blue plus 2"]
        XCTAssertTrue(bluePlusTwo.waitForExistence(timeout: 3))
        bluePlusTwo.tap()
        XCTAssertTrue(app.staticTexts["Last play: Kaleb +2"].exists)

        app.buttons["Undo last play"].tap()
        XCTAssertTrue(app.staticTexts["Tap a button to log the first play"].waitForExistence(timeout: 2))
    }

    /// Scrolls the dashboard until the 1v1 card is on screen (the grid loads lazily), then opens it.
    private func openOneOnOne() {
        let card = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Play a friend'")).firstMatch
        XCTAssertTrue(app.staticTexts["Squatch Sports"].waitForExistence(timeout: 5))
        for _ in 0..<6 where !(card.exists && card.isHittable) {
            app.swipeUp()
        }
        XCTAssertTrue(card.exists && card.isHittable, "1v1 card not found on the dashboard")
        card.tap()
    }

    private func attachScreenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
