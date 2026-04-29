# Sprint 3 Report (Mar 9, 2026 – Apr 28, 2026)

## What's New (User Facing)
* Added the remaining drills into the app, including Catch & Shoot, Off the Dribble, Midrange Series, 3PT Series, and Finishing
* Expanded the iPhone drill flow so users can open drill details, review goals/instructions, and start workouts directly from the drill page
* Added court-position structure for drills like Midrange Series and 3PT Series
* Improved workout summaries so completed drill sessions now show cleaner results
* Continued improving History and Analytics so new completed sessions appear across the app
* Polished the overall demo flow so the app now feels more connected from dashboard to workout to session review

## Work Summary (Developer Facing)
For Sprint 3, the focus was on turning the project from a set of working prototype pieces into a more complete end-to-end workout flow. The team continued improving both the iPhone and watch experience, but a major part of the sprint was making the app feel more connected and demo-ready. On the iPhone side, the drill workflow was expanded so that users could move from the dashboard, to drill selection, to drill details, to the workout screen, and then see completed sessions reflected in other parts of the app. The remaining drills from the older Squatch Sports version were added into the new app, and drills that depend on shot location were given more court-position structure. Overall, Sprint 3 helped move the project much closer to a real client-demo-ready prototype instead of separate screens and isolated flows.

## Unfinished Work
Even though the core workflow is now working, there are still areas that need improvement. Persistent long-term storage still needs more work so session data is more reliable across app restarts. There are also still edge cases around fast repeated watch input and temporary connection drops. In addition, some advanced features such as stronger recovery behavior, offline queuing, and deeper court-position support are still planned for next semester.

## Completed Issues/User Stories
Here are links to the issues that we completed in this sprint:
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/12
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/13
* https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/14

## Incomplete Issues/User Stories
Here are links to issues we worked on but did not complete in this sprint:
* Long-term persistence still needs stronger support so workout data remains more reliable after app restarts
* Watch-to-phone sync works in the normal flow, but connection recovery still needs more polish
* Additional reconnect behavior and more advanced drill-specific features are still planned for next semester

## Code Files for Review
Please review the following code files, which were actively developed during this sprint, for quality:
* [DrillsHomeView.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/DrillsHomeView.swift)
* [iPhoneWorkoutView.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/iPhoneWorkoutView.swift)
* [CourtPosition.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/CourtPosition.swift)
* [AppDataStore.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/AppDataStore.swift)

## Retrospective Summary

### Here's what went well:
* The team split work more clearly across iOS, watchOS, connectivity/shared state, testing, and documentation
* The demo flow became much easier to explain because the app now follows a real user path from dashboard to drill to workout to session review
* The remaining drills were added, which made the app feel much more complete and realistic
* Shared workout data now connects more naturally to History and Analytics

### Here's what we'd like to improve:
* Improve long-term persistence so workout data remains more reliable over time
* Strengthen recovery for fast repeated input and temporary connection drops
* Keep polishing the watch and iPhone experience so the app feels smoother during live use
* Continue improving advanced drill-specific behavior, especially for court-position-based drills

### Here are changes we plan to implement in the next sprint:
* Improve persistence and session recovery
* Add an Email signup and networking updates
* Strengthen reconnect behavior and watch-to-phone reliability
* Expand advanced court-position features and drill-specific tracking
