---
kind: feature
status: draft
timestamp: 2026-10-20T13:41:03Z
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

## Amendments
