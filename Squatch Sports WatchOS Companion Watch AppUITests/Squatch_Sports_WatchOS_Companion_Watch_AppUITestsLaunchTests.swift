//
//  Squatch_Sports_WatchOS_Companion_Watch_AppUITestsLaunchTests.swift
//  Squatch Sports WatchOS Companion Watch AppUITests
//
//  Created by Kellam Adams on 2/4/26.
//

import XCTest

final class Squatch_Sports_WatchOS_Companion_Watch_AppUITestsLaunchTests: XCTestCase {

    override class var runsForEachTargetApplicationUIConfiguration: Bool {
        true
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testLaunch() throws {
        let app = XCUIApplication()
        app.launch()

        // Insert steps here to perform after app launch but before taking a screenshot,
        // such as logging into a test account or navigating somewhere in the app

        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Launch Screen"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
