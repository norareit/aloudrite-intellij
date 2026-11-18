---
kind: feature
status: in-progress
timestamp: 2026-10-25T23:57:05Z
---

# Tidy the reading tool window

## Log

- 2026-10-25T23:57:05Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `master` at `74cd5c7`, at the human's request, wrote `spec.md` of a new unit from a capture of the tool window that
  the human found cramped since unit 0022, the causes the specifier found in it and the suggestions the human took:
  groups under titled separators, the bar across the pane with the times under it, pause and stop as icons in the
  title bar, the remark on the speed as the field's tooltip, the model with a link to its settings, and one left edge.
  Read `process.md`, the standards and `roles/specifier.md` of the installed kit, 8.2.0. Made it a feature and
  numbered it 0024, after 0023. Decided by me and open to the human: no title over the reading, `About` once instead
  of the two `~` of unit 0022, and the times in the grey of a comment. Left beyond the request: a first button that
  starts a reading while nothing is read. The status is `draft`
- 2026-10-26T00:01:37Z [human] approved the spec. The status is `approved`
- 2026-11-12T21:36:23Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by AgentRite's orchestrator on the branch
  `feature-0024-tidy-the-reading-tool-window` at `ef43223`, wrote `plan.md` against that tree and the IntelliJ
  Platform 2026.2.3 it builds on. Decided in the plan: pause, resume and stop as two new actions set with
  `setTitleActions` and not registered in `plugin.xml`, rather than the tab's actions, which take a title bar for the
  tab; the groups as nested `VerticalLayout`s with a gap of 6 within a group and 14 between groups; the times as two
  comment labels of `ComponentPanelBuilder`; the link through an opener handed to the panel; and one left edge by
  removing the insets of the minus button and of the separators' labels, confirmed by the captures. The tests of the
  text buttons move to a new `ReadingTitleActionsTest`. The status stays `approved`
- 2026-11-12T22:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator on the
  branch `feature-0024-tidy-the-reading-tool-window`, began the work. The status is `in-progress`
- 2026-11-12T22:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built the title actions
  (`5522572`), the three groups with the bar, the times line, the field's tooltip and the model row with the link
  (`feature-0024 Group the reading tool window under titled separators`), and the README paragraphs. Two departures
  from the plan, both small: the test of a press by another project calls `actionPerformed` itself, since
  `ActionUtil.performAction` asks a `MockProject` for services it has not, as `pressTheKeyOf` already does; and the
  left-edge test counts the border inset of a label only, since a progress bar and a button paint from their bounds.
  Verified: `./gradlew test` and `./gradlew buildPlugin` passed on the tree before the README commit, which changed no
  code; the tests with the real engine were not part of this unit's change and were not singled out. Not verified, and
  open: requirement 12, the captures of the sandbox IDE. This sandbox has no display and no `xwd`, `xwininfo` or
  `Xvfb`, so no capture was taken and the task for it in `plan.md` stays unticked. What the tests cannot show and the
  captures are for: whether the label of a `TitledSeparator` starts at the left edge in the IDE's look and feel,
  whether the wrapped name of the model is cut in a narrow pane (the plan's fallback is a preferred height of
  `modelNameLabel` for the width it is given), and the gaps in both themes. Requirement 13 is the developer's
- 2026-11-12T21:44:29Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a full review of unit 0024 at
  `0f5ac8601c33493cb8fd49e73a8d4d71cdf4b2c5`, against merge base `ef43223` with `origin/master`.
  Returned S1, S2 and P1, saved in `.reviews/0024-tidy-the-reading-tool-window/review-20261112-round1.md`.
  Verified `./gradlew cleanTest test` (641 tests, no failures, four skipped), `./gradlew test buildPlugin` and
  `npm run check-changes`; reproduced the narrow model row's insufficient height with the platform's components.
  Could not verify the IDE captures or the developer's Linux check. The status stays `in-progress`
- 2026-11-12T23:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator, took
  the findings of `.reviews/0024-tidy-the-reading-tool-window/review-20261112-round1.md`. S1 accepted by the definition
  of the orchestrator, and not a decision of the human: the model row is now as high as its name wrapped at the width the
  pane gives it, through a `getPreferredSize` of the row that reads the HTML view of the label, with the regression test
  `theRowOfTheModelIsHighEnoughForTheNameWrappedInANarrowPane`, which failed on the earlier code and passes now, at 134
  and 200 pixels. P1 accepted by the same definition: the verification and commit-point boxes of `plan.md` are ticked
  for work done, the pending ones being the captures and the conditional correction commit. S2 is not declined and is
  not fixable here: the sandbox has no display, so the captures of requirement 12 and the developer's check of
  requirement 13 stay open for the human. Verified on the corrected tree: `./gradlew test buildPlugin` passed. The
  status is `in-progress`
- 2026-11-12T21:49:08Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of unit 0024 at
  `5a2f657afd7036d3cd7f514c259bbf4d4262c4ab`, compared against the preceding review's commit `0f5ac86`.
  Verified the corrections for S1 and P1; returned the still-open S2 and no new findings, saved in
  `.reviews/0024-tidy-the-reading-tool-window/review-20261112-round2.md`. Verified
  `./gradlew cleanTest test buildPlugin` (642 tests, no failures, four skipped), `npm run check-changes` and
  `git diff --check 0f5ac86 HEAD`. Could not verify the required IDE captures or the developer's Linux check.
  The status stays `in-progress`
- 2026-11-12T21:50:41Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of unit 0024 at
  `eabc7e3cca79b32e9925e92deb369b8ef3375fd2`, compared against the preceding review's commit `5a2f657`.
  The delta holds only the preceding review's record entry. Returned the still-open S2 and no new findings,
  saved in `.reviews/0024-tidy-the-reading-tool-window/review-20261112-round3.md`. Verified
  `./gradlew test buildPlugin` (test and archive tasks up to date), `npm run check-changes` and
  `git diff --check 5a2f657 HEAD`. Could not verify the required IDE captures or the developer's Linux check.
  The status stays `in-progress`
- 2026-11-18T12:04:29Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on the branch `feature-0024-tidy-the-reading-tool-window`, at the human's request, took the captures of
  requirement 12, the open S2 of the reviews. Started the sandbox IDE with `./gradlew runIde`, opened
  `samples/markdown-features.md` and captured only the sandbox IDE's window with `xwd -id`, found by the process of
  `runIde`, since the developer's own IDE on this project also has a title ending in `[aloudrite-intellij]`. Captured
  while nothing was read, while the file was read and while it was paused, at the width the tool window opened with,
  about 470 pixels, and at about 190 pixels, where the model's name wraps, in the themes Islands Dark and Islands
  Light, the IDE's defaults, at the IDE's default scale. The first captures, at `cc33728`, showed no line of the times
  under the bar: with the tool window open before the reading, its two labels, the wrapping and copyable comment of
  `createCommentComponent`, were laid out with no text and kept a height of 0 once the times were set. The tests had
  built the pane only after the reading started. Added the task for it to `plan.md` (`e600358`), wrote
  `theLineOfTheTimesIsAsHighAsItsTextWhenThePaneWasLaidOutBeforeTheReading`, which failed on that code, and made both
  labels with `createNonWrappingCommentComponent`, the comment that neither wraps nor is copyable, in the same font
  and grey (`9ccf6a1`). The captures of `9ccf6a1` showed, in both themes and at both widths: R1, the reading
  without a title, then "Reading speed" and "Speech model" as titled separators whose line runs to the right edge of
  the pane, the rows within a group 7 to 9 pixels apart and the groups 27 pixels apart; R4, the bar from the left
  edge to the right edge of the pane under the name of the file; R5, `About 4:12 left` at the left and `4:30 in all`
  at the right edge, smaller than the name and in the grey of a comment, both fitting at 190 pixels; R7, the model
  on the left and `Settings…` at the right edge, the name wrapped to two lines in full at 190 pixels with the link at
  the top of the row, and a click on the link opened Settings at Tools › AloudRite; R8, measured on the pixels in both
  themes at both widths, the text of every label, the titles of the separators, the bar and the line of the times
  starting in one column, and the icon of the minus button one pixel to its right. Also seen, of R2 and R3: the title
  bar showed pause and stop greyed out while nothing was read, the pause icon then the resume icon with "Paused
  reading", the stop icon in red, and the tooltips "Pause Reading Ctrl+Alt+Shift+Space" and "Stop Reading
  Ctrl+Alt+Shift+O"; a press of each paused, resumed and stopped the reading. The IDE shows the title bar buttons
  only while the tool window is active or under the mouse, as it does for every tool window. Noted and not changed,
  as this unit does not touch it: at 190 pixels the name of the file is cut with an ellipsis, as
  `Reading markdown-featur…`. Verified on `9ccf6a1`: `./gradlew cleanTest test buildPlugin` passed, 643 tests with
  no failures and four skipped. The unit changes neither the speech engine nor the text handed to it. Requirement 13,
  the developer's check, stays open. The status stays `in-progress`
- 2026-11-18T12:09:32Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, recorded the developer's check of requirement 13 on `6e293e5`, which the developer made in the sandbox IDE on
  Linux x64 while nothing was read, while a file was read and while it was paused, and reported as done. The developer
  remarked that in a very narrow pane the title "AloudRite" is cut first, then the name of the model, and in a
  narrower one still the title bar buttons are hidden, and found none of it an issue, as no one would make the pane
  that small. The title and its buttons are cut and hidden by the IDE, as in every tool window, and the name of the
  model wraps at its spaces until one word with the link no longer fits; requirement 7 asks for the wrapping of now,
  which the tests cover down to 134 pixels. The verification box for requirement 13 in `plan.md` is ticked. The
  commits since the last review, `e600358`, `9ccf6a1` and `6e293e5`, have not been reviewed. The status stays
  `in-progress`
