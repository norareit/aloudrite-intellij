---
kind: feature
status: implemented
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
- 2026-11-21T12:15:57Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0037 at `357ecae` in full
  against merge base `1e311df` with `origin/master`. Returned S1, S2 and P1 in
  `.reviews/0037-read-the-number-of-an-item-with-its-first-sentence/review-round-1-357ecae.md`.
  `./gradlew cleanTest test buildPlugin` passed with 912 tests, zero failures and six skipped; all five
  `EspeakPhonemesTest` tests ran without skips. `npm run check-changes` passed. A production-code JShell probe
  confirmed that an item beginning with inline code leaves the comma number in a Words part of its own for both
  delimiters. Could not verify the developer's listening result for R9. The status stays `in-progress`
- 2026-11-21T12:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, took the findings of `.reviews/0037-read-the-number-of-an-item-with-its-first-sentence/review-round-1-357ecae.md`. Accepted S1 by the definition of the orchestrator, not as a decision of the human: an item that began with inline code had a silence between its number and the first word, so the number was a piece of its own. The silences that stand right after the number are now dropped, since the comma is the pause there. This changes the quarter of a second before the code at the start of an item, which R5 leaves as it is, and is a consequence the human may weigh. Added the test for both delimiters, run to fail without the change and to pass with it. Accepted P1 by the definition of the orchestrator: added the `)` selection test of the fourth task. The first two tasks name a run of the new tests to see them fail first, which was not done for the other tests, which were written with the code. The tests of S1 were run red first. Their boxes are ticked for the final coverage that passes, not for a red run that took place. S2 is the developer's check of R9 and stays open. `./gradlew test buildPlugin` passed on `a4f73b1`
- 2026-11-21T12:21:56Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0037 at `5c49d4d` as a delta
  from `357ecae`, the commit named in the previous full review. Saved the verification of S1 and P1 and the remaining
  findings S2 and P1 in `.reviews/0037-read-the-number-of-an-item-with-its-first-sentence/review-round-2-5c49d4d.md`.
  `./gradlew cleanTest test buildPlugin` passed with 914 tests, zero failures and six skipped; all five
  `EspeakPhonemesTest` tests ran without skips. `npm run check-changes` passed. Repeated the production-code probe
  for both delimiters with leading inline code. Could not verify the developer's R9 listening result or the
  historical red-run claim from the final tree. The status stays `in-progress`
- 2026-11-21T12:55:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, took the findings of \`.reviews/0037-read-the-number-of-an-item-with-its-first-sentence/review-round-2-5c49d4d.md\`. Accepted P1 by the definition of the orchestrator, not as a decision of the human: the plan now says that its boxes stand for the final coverage and not for red runs that were not made, and describes the follow-up of S1 and its change of the pause before inline code. No code changed. S2 stays open as the developer's check of R9
- 2026-11-21T12:24:21Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0037 at `4db536d` as a delta
  from `5c49d4d`, the commit named in the preceding review. Saved the verification of P1 and the remaining finding
  S2 in `.reviews/0037-read-the-number-of-an-item-with-its-first-sentence/review-round-3-4db536d.md`.
  `./gradlew cleanTest test buildPlugin` passed with 914 tests, zero failures and six skipped; all five
  `EspeakPhonemesTest` tests ran without skips. `npm run check-changes` passed. No production or test code changed
  in this delta. Could not verify the developer's R9 listening result or historical red runs from the final tree.
  The status stays `in-progress`
- 2026-11-21T12:31:48Z [human] read the two numbered lists of `samples/markdown-features.md` in the sandbox IDE on Linux
  x64, in Heart and in Emma. In both voices the number is heard as the start of the first sentence of its item, and the
  stumble after it is gone. The pause after the number is that of a comma, which the human finds rather short, and
  decided not to make longer, since that would need a rule of its own for the number. With this, requirement 9 is met.
  The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-11-21T12:31:48Z [human] accepted finding S1 of the review of `357ecae`, the number cut off as a piece of its own
  before an item that begins with inline code, as fixed in `a4f73b1` and found corrected by the delta review of
  `5c49d4d`. With it, every blocking finding of the reviews is decided. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-11-21T12:31:48Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on the unit's branch at `f68dab1`, at the human's request, ticked the task of requirement 9 and its commit point
  in `plan.md`, and ran `./gradlew test --rerun` with both models in place: 914 tests, no failures and no errors, six
  skipped, the five opt-in measurements and `RealModelPackageTest`, and none of the tests with the real engine or the
  real data of eSpeak NG skipped. `npm run check-changes` passes. Every task of `plan.md` is ticked. The status is
  `implemented`

## Amendments
