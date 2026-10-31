# Sprint 5 review presentation

## Ready to use

- **Deck:** [Sprint5_Review.pptx](Sprint5_Review.pptx)
- **Slide overview:** [Slide_Overview.png](Slide_Overview.png) — native QuickLook renders of all 16 slides.
- **Speaker scripts:** [Sprint5_Presenter_Notes.md](Sprint5_Presenter_Notes.md) — also embedded in all 16 slides.
- **Recording plan:** [../Video/Sprint5_Demo.md](../Video/Sprint5_Demo.md)
- **Build/test screenshot:** [../Testing/Android_Test_Results.png](../Testing/Android_Test_Results.png)
- **Verification and handoff text:** [../Testing/Android_Verification.md](../Testing/Android_Verification.md)
- **Package/layout validation:** [Deck_Validation.json](Deck_Validation.json)

## Updated completion status

- **Kellam:** Android/Wear OS 1v1 and nearby Android-to-Android support are **merged into `main` in [PR #38](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/38)**, merge commit `c4b0c76`. [Issue #32](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32) is closed. These statuses were checked on GitHub before updating the deck.
- **Kaleb:** nearby iPhone 1v1 is **tested and complete**, per Kellam Adams's explicit team confirmation. This replaces the original draft's pending-testing wording. It is team-provided confirmation, not a new assistant-run Swift test session, test count or latency measurement.
- **Board reconciliation:** GitHub follow-up ticket [#31](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31) remains open. It was not silently closed or described as closed by the Android merge; the deck distinguishes the team's feature-completion confirmation from that ticket state.
- **Still separate:** Android physical Nearby validation, client distribution, collected player feedback, and the actual recording/upload. Android-to-iPhone multiplayer is not implemented.

## Minimal layout and required order

This revision has **16 slides**, keeping the minimal visual style without dropping the backlog. Shorter titles, larger text, three-point feature summaries and simple Kanban tables replace dense status explanations. The complete Sprint 5 closeout and Sprint 6 task lists are visible on separate slides; only technical detail, source attribution, testing limits and demo instructions move into the notes. Kellam remains the owner shown for #36 and #37; the ordinary **MERGED** badge matches the other merged objectives.

The deck starts from the actual Sprint 4 PowerPoint package: same 16:9 size, master/layout, orange/charcoal palette, dark cover/conclusion, header hierarchy, cards, and team-oriented feature sections. Arial provides consistent rendering on macOS without requiring Calibri. The cover uses the app's existing Squatch Sports wordmark and mascot.

| Required section | Slides |
|---|---|
| Intro | 1 |
| Objectives | 2 |
| Features/Demo | 3–6 |
| Deployment & Testing | 7 |
| Kanban | 8–10 |
| Client Meetings | 11 |
| Report | 12 |
| AI | 13 |
| Achievements & Challenges | 14 |
| Next Steps | 15 |
| Conclusion | 16 |

The saved package is checked for 16 slides, notes on every slide, the exact required section order, valid ZIP contents and in-bounds shapes. All slides are also rendered with macOS QuickLook and visually reviewed. This is native presentation-import validation, not a claim of testing inside Microsoft PowerPoint itself.

## Sources and evidence

- Draft report and MoMs: `origin/melvin-sprint5-docs`, commit `86171e9`. That draft predates the current Android merge and team confirmation, so its older status wording must be reconciled before submission. The branch was used as a source, not merged as part of this presentation request.
- Android implementation: PR #38 / `c4b0c76` on `main`.
- Fresh Android verification on merged code: **121/121 JVM unit tests**, both debug APKs built.
- Last clean emulator UI run of this implementation: **13/13 checks** (9 phone, 4 watch), zero failures/errors. The XML timestamps are retained rather than relabeling those tests as a new iOS or physical-device run.
- Additional real-app paired-emulator smoke: watch → phone scoring, phone → watch scoring, watch Undo and match clearing passed.
- Kaleb's completion is from Kellam's current team confirmation. No new Swift test counts were invented or attributed to the assistant.

`../Testing/Android_Test_Results.png` captures a page built from actual Gradle logs and JUnit XML. It is labeled as an evidence page, **not** a terminal screenshot. Raw logs, XML-derived suite summaries, APK hashes and merge metadata are saved beside it.

The images in `Assets/` are genuine emulator captures. The 4–2 game is a temporary demo fixture, not a real player game or user-test result; it was discarded without saving a win/loss result. The second watch image shows the controls after scrolling.

## Before recording / submission

1. Pull the updated `main` and review the deck together; adjust presenter assignments as needed.
2. Reconcile older pending/local-only wording in the report using the updated completion statuses and attached Android evidence.
3. Use Kaleb's team's verified demo for the iPhone nearby segment. Keep the distinction between team-provided testing and the Android checks reproduced here.
4. Do not claim physical Android Nearby acceptance until tested on two real Android phones. The Apple and Android transports are different; no mixed-platform match is supported.
5. Reconcile ticket #31 with the team's completion status separately; this presentation update does not assert that GitHub ticket is already closed.
6. Record the presentation/demos, upload the actual video as unlisted, then add its real URL to the report and recording document. No recording, video upload or course submission is claimed by these repository merges.
7. Client deployment, latency measurements and recorded player feedback remain follow-ups (#33–34).
