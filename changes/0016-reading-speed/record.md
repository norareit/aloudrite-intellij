---
kind: feature
status: draft
timestamp: 2026-10-21T14:46:34Z
---

# Reading speed

## Log

- 2026-10-20T12:01:29Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `33ea6fb`, at the human's request, wrote `spec.md` of a new unit: the speed of the reading can
  be made slower and faster. The human found, testing the plugin, that they miss it. Read `process.md` and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0016, after 0015, and wrote it to be
  built after units 0015 and 0012. Asked before writing, the human chose: the controls in the tool window of unit
  0015; a change during a reading starts the piece that is playing again at the new speed; the speed is kept across
  restarts of the IDE. Decided by me and open to the human: a range from 0.5 to 2.0 in steps of 0.1, shown as "1.0×";
  a settings page under Tools that shows the same speed; one speed for the whole IDE; the speed made by the engine and
  not by playing faster; no limit on the time to the new speed, which the developer measures instead; the pauses of
  unit 0010 as long at every speed. Left beyond the request: pauses that follow the speed, and keyboard shortcuts for
  it. The status is `draft`
- 2026-10-20T12:07:01Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, corrected the context of `spec.md` on
  unit 0012, which another session revised after this spec was written: unit 0012 keeps a stop button on the tab
  beside the pause, and gives the tool window a pause, a resume and a stop. Nothing else of this spec depends on
  where the stop is. The status stays `draft`
- 2026-10-21T14:46:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `6811f0e`, at the human's request, revised `spec.md`. The human asked that not every speed be
  listened to on both machines: requirement 14 now has every speed listened to on Linux, on the first five paragraphs
  of `samples/ten-thousand-words.md`, and requirement 15 has `samples/markdown-features.md` read to its end at 0.5,
  1.5 and 2.0 on Linux and on macOS. The human asked for a field in the tool window to type the speed in, so that a
  large step is one change and the piece is not made again for every press on a button: requirement 3. The human
  took in the first item beyond the request, silences that follow the speed: requirement 10. Decided by me and open to
  the human: the two seconds after "code example" stay two seconds; the field takes a point as the decimal sign and
  one decimal at the most, and sets the value on Enter or when it loses focus; a change during a silence goes on at
  the piece after it; the changes during a reading are checked on Linux only. Added beyond the request: a wait after
  a press on a button, and a comma as the decimal sign. Brought the context up to date with units 0015 and 0012,
  which are implemented, and renumbered the requirements to 16. The status stays `draft`

## Amendments
