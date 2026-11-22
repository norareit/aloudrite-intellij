---
kind: bug
status: implemented
timestamp: 2026-11-22T02:10:18Z
---

# The aligner test allows an end rounded past the audio

## Log

- 2026-11-22T02:10:18Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `53b0463`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a bug and numbered it 0039, after 0038. The failure was reproduced
  on the host while unit 0036 was being closed, with the counts its record gives, and the decision of unit 0028 to keep
  the check on the ends in `RealAlignerTest` was read in its record. Decided by me and open to the human: the test is
  fixed and not the aligner's results, the bound on the ends is loosened by one step of the aligner's times rather than
  dropped, and the step is to be found in sherpa-onnx or its model rather than assumed to be 10 ms. Left beyond the
  request: bounding the ends in `wordStartsOf`, and repeated runs of the full suite as verification. The status is
  `draft`
- 2026-11-22T11:31:37Z [human] approved the spec. The status is `approved`
- 2026-11-22T11:33:37Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on the unit's branch at `1d5313f`,
  wrote `plan.md`. Read `process.md`, the standards and `roles/planner.md` of the installed kit. The fix takes several
  steps, a unit test, a helper, the change to the test and runs with the models, so it has a plan. Found the step of
  requirement 2 in the configuration of the aligner's model: the `features` of `meta.json` of `english-mfa-v3.1.0` hold
  a `frame_shift` of 10 ms, which agrees with the whole hundredths of the failure, so the step is 10 ms. The status
  stays `approved`
- 2026-11-22T11:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, began the work
  and built it from `plan.md`. Wrote `AlignedEndsTest` first and saw it fail to compile, then `AlignedEnds.kt` with the
  step of 10 ms and `isEndWithinTheAudio`, and used it in `RealAlignerTest`. No file under `src/main` or
  `core/src/main` differs from `origin/master`. With both models in place, `./gradlew :core:cleanTest :core:test --tests
  com.ritenoar.aloudrite.core.RealAlignerTest` ran 2 tests, none skipped, and passed, and `./gradlew cleanTest test`
  passed, each once in the sandbox. The failure is intermittent and was not reproduced in the sandbox, so the runs do not
  show it gone, only that the unit test passes with the values of the failure. The status is `in-progress`
- 2026-11-22T11:38:28Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0039 at
  `510c518a38eec60015d3c6af9cb7a8a6e93c3ad1`, against the merge base with `origin/master`, and returned no findings
  on spec conformance, plan fidelity or code quality. Saved the review in
  `.reviews/0039-the-aligner-test-allows-an-end-rounded-past-the-audio/review-20261122T113828Z.md`.
  The regression tests, real-aligner tests (2 tests, none skipped), `./gradlew cleanTest test`,
  `npm run check-changes` and the diff whitespace check passed. Confirmed the model's frame shift is 10 ms.
  Could not independently verify the claimed test-first sequence from the single implementation commit.
  The status stays `in-progress`
- 2026-11-22T11:55:11Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  x64 host, closed the unit at the human's request. The review returned no findings, so none was left to decide, every
  task of `plan.md` is ticked and `npm run check-changes` passed at `5099108`. Ran the repeated runs of item 2 of
  "Beyond the request" in the spec on the host, with the fp32 Kokoro model and the aligner model in place, each in a
  worktree of its own. `./gradlew :core:cleanTest :core:test --tests com.ritenoar.aloudrite.core.RealAlignerTest`
  alone failed in 2 of 9 runs on `master` at `9c2aa54`: once on "matches" in "Two candles and a box of matches." at
  1.5×, ending at 1.39 s in 1.385292 s of audio, and once on "lamp" in the sentence of the trial that begins "The
  keeper came twice a month", ending at 2.40 s in 2.397208 s of audio. Both ends are whole hundredths and lie less
  than one step past the audio, so the failure comes without the rest of the suite and on more than the one sentence
  the spec names, and the spec's 3 of 3 passes alone were too few to show otherwise. The same command passed in 30 of
  30 runs on the unit's branch at `5099108`, 2 tests each, none skipped, and `./gradlew cleanTest test` passed in 4 of
  4 runs there, which were stopped early because the test alone answers the question. This commit changes only this
  record, so the runs hold for its tree. The entry of the implementer above is stamped `11:40:00Z`, after its commit
  `510c518` of `11:36:04Z` and after the review, which suggests its time was not taken from the clock; it is left as
  written. The status is `implemented`

## Amendments
