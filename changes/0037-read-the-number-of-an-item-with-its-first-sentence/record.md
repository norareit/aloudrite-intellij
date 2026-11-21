---
kind: feature
status: approved
timestamp: 2026-11-21T11:46:18Z
---

# Read the number of an item with its first sentence

## Log

- 2026-11-21T11:46:18Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `1aefa27`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a feature, since unit 0006 judged the pause at a number fine and
  this changes a decided reading, and numbered it 0037, after 0036. Handed a numbered list through `spokenPartsOf`,
  `piecesOf` and `EspeakPhonemes` of the plugin in a test that was not committed: with `.` the number is a piece of its
  own, said as a sentence of one word, and with `)` it runs into the first word. Tried with `piper-phonemize` 1.5.9 that
  `1, Climb the stairs and unlock the lamp room.` is one sentence in `en-us` and `en`. Decided with the human: the
  number read as part of the first sentence of its item. Decided by me and open to the human: a comma after the number
  for both delimiters, kept as long as the delimiter, and the check by ear on Linux only. Left beyond the request:
  nothing. The status is `draft`
- 2026-11-21T11:52:44Z [human] approved the spec. The status is `approved`
- 2026-11-21T12:05:35Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on the unit's branch, wrote `plan.md`
  against the tree at `1e311df`. Read `process.md`, the standards and `roles/planner.md` of the installed kit. The
  number keeps its place, its marks and its length, and only its text changes: the count and a comma for an item
  with words, and the count and a full stop for an item with none. Every requirement has a task. The status stays
  `approved`

## Amendments
