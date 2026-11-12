---
kind: feature
status: approved
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
