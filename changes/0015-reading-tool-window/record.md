---
kind: feature
status: in-progress
timestamp: 2026-10-20T13:51:43Z
---

# Reading tool window

## Log

- 2026-10-20T12:01:29Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `33ea6fb`, at the human's request, wrote `spec.md` of a new unit: an "AloudRite" tool window
  that shows the reading and has a "Stop" button. The human asked for a way to make the speech faster and slower, and
  asked how the controls should be shown, since unit 0012 already adds a button to the tab and the speed would add
  two more. Read `process.md` and `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0015,
  after 0014. Asked before writing, the human chose: a tool window for the controls of a reading, with the tab button
  kept to start one; and a unit for the tool window first, with unit 0012 revised to stop there, and then a unit for
  the speed. Decided by me and open to the human: the tool window does not open by itself; it is docked on the right;
  every open project shows the one reading of the IDE, with the name of its project when it is another one. Left
  beyond the request: opening the tab of the file from its name, and opening the tool window at the first reading.
  The status is `draft`
- 2026-10-20T12:07:01Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, named unit 0016 in the non-goal on the
  speed of `spec.md`, which said a later unit, now that unit 0016 is written. The status stays `draft`
- 2026-10-20T13:41:03Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, on the human's word, put the button of
  the tool window on the tool window bar on the right by default: requirement 1 says the button is there for a
  project opened for the first time with the plugin and that the user can remove or move it, requirement 9 has the
  developer find it, and a decision says why, against the guidelines of JetBrains, which hide a plugin's button by
  default under "More tool windows". The tool window itself still does not open by itself. The status stays `draft`
- 2026-10-20T13:42:16Z [human] approved the spec as it stands, in the main session on the Linux host, with the button of
  the tool window on the tool window bar by default, and with the choices the specifier left open: the tool window
  does not open by itself and is docked on the right. Nothing of "Beyond the request" is taken. The status is
  `approved`. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T13:48:12Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  on `feature-0015-reading-tool-window` cut from `master` at `3f4c129`, wrote `plan.md`: a panel in an "AloudRite"
  tool window, docked on the right, that asks `SpeechService` for the current reading and stops it, and follows it
  by a topic of the application that the service publishes to at every change; and the button put on the tool window
  bar once per project with `ToolWindow.isShowStripeButton`, remembered in the project's `PropertiesComponent`. Read
  `process.md` and `roles/planner.md` of the installed kit. `./gradlew test` on that tree ran 261 tests, none failed,
  two skipped. The status is `planned`
- 2026-10-20T13:51:43Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on `feature-0015-reading-tool-window` at `3f5b747`, began the plan. Read `process.md` and
  `roles/implementer.md` of the installed kit, and `spec.md`, `plan.md` and `record.md` of the unit. The status is
  `in-progress`
- 2026-10-20T13:57:29Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, changed in `plan.md` how the tests
  of the panel make a reading of another project, before writing them that way. A press with a `MockProject` as the
  event's project fails in `ActionUtil.performAction`, which asks that project for its `DumbService` and finds none, and
  a `Proxy` of `Project` has no services either. The `MockProject` is now only the own project of a panel, which
  compares it by identity and calls nothing else on it, and the file is pressed with the default project, so that every
  reading of the tests still starts with a press on the tab button. The panel of the `MockProject` then shows the name
  of the default project. The spec is not touched. `./gradlew test` ran 276 tests: none failed, and two,
  `RealModelPackageTest` and `EngineThreadsMeasurement`, were skipped
- 2026-10-20T13:59:11Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did tasks 1 to 8 of `plan.md` at
  commit points A to D, the last of them `2602231`. On that tree `./gradlew test` ran 278 tests: none failed, and two,
  `RealModelPackageTest` and `EngineThreadsMeasurement`, were skipped, as on `master`. `./gradlew buildPlugin` passed,
  and `npm run check-changes` passed. Not run by me: the line of the default `onChange` of `SpeechService` that
  publishes the change, which the tests do not reach, and everything of the tool window on a real project, which are the
  developer's check of task 9, still to be done on Linux x64 and on macOS on Apple Silicon. The status stays
  `in-progress`
- 2026-10-20T14:04:07Z [human] decided, in the main session on the Linux host, that the developer checks this unit on
  Linux x64 only, and not also on macOS on Apple Silicon: the tool window has no native code, no audio output and
  nothing else that differs between the platforms, and moving the branch to the Mac for each check costs more than it
  finds. The human also asked for a rule in `AGENTS.md` that asks for the check on macOS only where a unit touches what
  differs between the platforms. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T14:04:07Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, on the human's decision, narrowed
  requirement 9 of `spec.md` to Linux x64 and added the decision "Checked by the developer on Linux only", with its
  reason and the rejected check on both platforms. Read `roles/specifier.md` of the installed kit. The status stays
  `in-progress`
- 2026-10-20T14:04:07Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, made task 9 of `plan.md` and its
  line under Verification ask for the check on Linux x64 only, as the spec now does, and added the rule the human asked
  for under "Building, running and testing" in `AGENTS.md`, in a commit of its own. The status stays `in-progress`
- 2026-10-20T14:10:54Z [human] found, in the developer's check on Linux x64, the tool window taking about half of the
  IDE window, with "Reading markdown-features.md" and a "Stop" button stretched across its whole width. The human asked
  for the button to be made small, and left the width of the tool window to the platform and to the user for now. The
  entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T14:10:54Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, added task 10 to `plan.md` and did
  it: the stop button is held at the west of a row of its own, so it keeps its preferred width, while the labels still
  fill the width of the pane to wrap. Two tests were added first: the one of the wide pane failed, the button 784 pixels
  wide where its preferred width is 61. Aligning the whole layout to the left, tried first, failed the test of the
  narrow pane, since the label of nothing read then took its preferred width, and was dropped. The panel was not looked
  at again in the IDE after this. `./gradlew test` ran 280 tests: none failed, and two, `RealModelPackageTest` and
  `EngineThreadsMeasurement`, were skipped. The status stays `in-progress`
- 2026-10-20T14:20:37Z [human] did part of the developer's check of task 9 on Linux x64. In the sandbox IDE, in one
  project, the AloudRite project itself, which had been opened in the sandbox before this branch: the button of the tool
  window is on the tool window bar on the right and a click on it opens the tool window; the tool window is under View ›
  Tool Windows; the icon was seen in the dark theme. The tool window was resized narrower, which suits its content
  better. With the plugin built from the branch installed in their own IntelliJ IDEA, a reading was stopped from the
  tool window while another file was selected, and from the tool window of a second project, and both times the tool
  window showed the right text. Not checked: a project opened for the first time in the sandbox, the icon in the light
  theme, which the human declined, the tool window moved to another side, floated and put in a window of its own, which
  the human declined so as to keep their layout, and the button removed from the bar. Which commit the installed plugin
  was built from was not said. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T14:26:10Z [human] did the rest of the developer's check of task 9 on Linux x64 in the sandbox IDE,
  restarted with the build of `a8fffc6`. In their words: "I moved it to several places, pinned it, unpinned it, removed
  it, readded it... All those things", taken by the human to cover moving the tool window to other sides, floating it
  and putting it in a window of its own, and removing its button from the bar and putting it back. Not said: whether the
  tool window kept showing and stopping the reading in each of those places, and whether it was where it was put after
  the project was closed and reopened. Still not checked: a project opened for the first time in the sandbox, and the
  icon in the light theme, which the human declined. No check failed. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T14:26:10Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, ticked task 9 of `plan.md` on what
  the human checked, as the last two entries record, with what was not checked left for the review to weigh against
  requirement 9. The status stays `in-progress` until the review
- 2026-10-20T14:26:46Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, ticked task 3 of `plan.md`, done at
  commit point B and missed then, and the list under Verification. On `fed3b34`, whose code is that of `a8fffc6`,
  `./gradlew test` ran 280 tests: none failed, and two, `RealModelPackageTest` and `EngineThreadsMeasurement`, were
  skipped, as on `master`; `./gradlew buildPlugin` passed; and `npm run check-changes` passed. Every task of `plan.md`
  is ticked. The unit is ready for review, and the status stays `in-progress` until the human has decided its findings

## Amendments
