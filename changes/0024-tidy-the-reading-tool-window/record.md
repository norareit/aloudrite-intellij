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
