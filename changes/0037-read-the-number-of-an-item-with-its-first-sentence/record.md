---
kind: feature
status: in-progress
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
- 2026-11-21T12:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, began the work. The status is `in-progress`
- 2026-11-21T12:21:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built tasks 1 to 10 of the plan up to `b685305`. `numberOf` gives the count, and `partsOfItem` writes it with `, `, or with `. ` for an item with no words, so an item of struck out words reads `2. ` as it did before for `.`, trailing space included, and as `2. ` instead of `2).` for `)`. The tests of R1 to R4 and R6 were changed with the code in one working session and were not each seen to fail before the code was written. `./gradlew test buildPlugin` passed on `b685305` with 912 tests, 6 skipped, none of them `EspeakPhonemesTest`, whose five tests ran with the model in place, so R7 was run and not skipped. `ReadingSegmentsTest` cases on the existing markers pass unchanged. The `npm run check-changes` run and the developer's check of R9 are still open. The task of R9 is not done

## Amendments
