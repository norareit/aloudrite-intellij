---
kind: bug
status: in-progress
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

## Amendments
