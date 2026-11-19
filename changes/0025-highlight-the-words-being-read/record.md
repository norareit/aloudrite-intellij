---
kind: feature
status: in-progress
timestamp: 2026-11-19T13:16:49Z
---

# Highlight the words being read

## Log

- 2026-11-19T13:16:49Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `b4d5317`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0025, after 0024. Checked that the
  Java binding of sherpa-onnx 1.13.8 hands over only samples, with no times for words. Decided with the human: a light
  highlight of the sentence and a strong one of the estimated word, the editor in this unit and the preview in unit
  0026, and scrolling by itself. Decided by me and open to the human: the sentence rather than the piece, the
  highlights staying through a silence, when the scrolling stops and comes back, and colours from keys of the
  plugin's own. Left beyond the request: a setting that turns the highlights off, and a page for their colours. The
  status is `draft`
- 2026-11-19T13:30:22Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `375cc6a`, at the human's request, took in item 1 of "Beyond the request", a checkbox on the
  plugin's settings page that turns the highlights off, as requirement 15, and renumbered the requirements after it.
  The checkbox also covers the preview of unit 0026. Decided by me and open to the human: its wording, and that it is
  checked until cleared. Item 2, a page for the colours, stays out. The status is `draft`
- 2026-11-19T13:31:36Z [human] approved the spec. The status is `approved`
- 2026-11-19T13:52:18Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by AgentRite's orchestrator, on
  `feature-0025-highlight-the-words-being-read` at `05e3d29`, wrote `plan.md` against the tree of that branch. Read
  `process.md`, the standards and `roles/planner.md` of the installed kit, 8.4.0. Every task names the requirements it
  delivers, and every requirement has a task. Requirement 18 is left to the developer. Decided for the plan, as how and
  not as what is true:
    * The reading keeps its placed parts, so that one cut gives both the engine's segments and the places of the words
    * The word's share is the one the bar of unit 0022 already uses
    * The position is read from the player when the editor asks, 20 times a second
    * Edits are mapped through a log of the document's changes from the start of the reading
    * A reading whose file changed between the press and its start, as during a download, shows no highlights
    * Every caret move in an editor ends its following
  The status is `approved`
- 2026-11-19T14:05:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator, on
  `feature-0025-highlight-the-words-being-read` at `395ae8c`, began the work. Read `process.md`, the standards and
  `roles/implementer.md`. No review files exist for the unit yet. The status is `in-progress`
