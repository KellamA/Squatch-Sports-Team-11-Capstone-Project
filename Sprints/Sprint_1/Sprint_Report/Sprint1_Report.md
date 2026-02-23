# Sprint 1 Report (Feb 3, 2026 – Feb 22, 2026)

## YouTube link of Sprint 1 Video (Make this video unlisted)
* TODO: Paste unlisted YouTube link here

## What's New (User Facing)
* Implemented connectivity between the main iOS app and the companion watchOS app
* Added an initial loading/splash screen + basic navigation flow into the app
* Built a Dashboard home screen that lets users jump into drills/workouts quickly
* Added a Drills/Workouts home view to start a shooting workout
* Implemented basic workout session tracking for shooting stats (misses, makes, swishes) and showing session results
* Created a branded loading interface when booting up the app

## Work Summary (Developer Facing)
For Sprint 1, the focus was to understand the client’s requirements and build a strong foundation for the app. This sprint included core UI work (loading/splash screen, dashboard, and drills/workouts screens), setting up connectivity between the iOS app and the watchOS companion app, and starting the workout tracking system. We are satisfied with what we completed, but most features are still first-pass and will need polish and expansion. Overall, we are ahead of the schedule for the class because a solid portion of the base implementation is already in place.

## Unfinished Work
We completed everything we planned (and more), but several features were intentionally built in a basic form with the plan to improve them later. Most unfinished work is related to workout tracking:

* **Shot logging UI is still clunky.** We can track shooting statistics (misses, makes, swishes), but the current method requires entering integers using the device keyboard, which is not suitable for the final product. In Sprint 2, we plan to replace this with a faster UI interaction (directional swipe/tap-based input) instead of typing.
* **No workout persistence yet.** Workouts track accurately during a session, but there is no long-term saving system. If the user closes the app, the session data resets. Implementing persistence and workout history is a key Sprint 2 goal.

## Completed Issues/User Stories
Here are links to the issues that we completed in this sprint:
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/5
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/6
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/8

## Incomplete Issues/User Stories
Here are links to issues we worked on but did not complete in this sprint:
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/9 <<We started improving the UI, but focused this sprint on getting the core watch ↔ iOS workflow working first.>>
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/7 <<Keyboard-based shot entry is still in place; the swipe/tap logging UI is planned for the next sprint.>>
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/4 <<Workout history/persistence is not implemented yet; sessions currently reset when the app closes and will be saved in the next sprint.>>

## Code Files for Review
Please review the following code files, which were actively developed during this sprint, for quality:
* [SplashView.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/SplashView.swift)
* [DashboardHomeView.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/DashboardHomeView.swift)
* [SharedWorkout.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/SharedWorkout.swift)

## Retrospective Summary

### Here's what went well:
* We aligned on client requirements early and avoided building the wrong thing
* The app now has a real working flow: launch → dashboard → drills/workout
* iOS ↔ watchOS connectivity is set up, which is a big technical milestone
* We got basic workout tracking working end-to-end

### Here's what we'd like to improve:
* Make the watch logging UI less clunky (remove keyboard-style input and keep interactions fast during drills)
* Improve sync reliability and edge cases (temporary disconnects, retries, clearer “synced/not synced” status)
* Add more UI polish and consistency (spacing, labels, haptics/feedback, empty/error states)
* Tighten up dev workflow (clearer issue breakdown, smaller PRs, and basic testing/verification before merge)

### Here are changes we plan to implement in the next sprint:
* Replace keyboard-based stat entry with swipe/tap based UI controls
* Add persistent workout saving so stats remain after closing the app
* Improve polish and consistency on the dashboard + drills screens (layout, spacing, labels)
* Add a simple workout history view once persistence is working
