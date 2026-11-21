---
kind: feature
status: in-progress
timestamp: 2026-11-21T11:30:54Z
---

# Measure how well the word highlight follows the voice

## Log

- 2026-11-21T11:30:54Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `6f83f0c`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0032, after 0031. Found that `model.onnx`
  of the fp32 package has the audio as its one output and no durations, and that the developer's `kokoro-env` holds
  `kokoro` 0.9.4 and PyTorch 2.14.0 and `Kokoro-82M` the weights of Kokoro v1.0 and the voice `af_heart`. Decided with
  the human: a measurement that is kept and that unit 0028 can run again rather than a run whose result is only
  recorded, the durations of the model by the Kokoro Python package as the truth, and Heart as the only voice. Decided
  by me and open to the human: every piece of the sample rather than 11 sentences, 1.0× and 1.5×, only the time within
  words counted, the 2% for the lengths, and the run on the host. Left beyond the request: counting the pauses, and
  repeating the forced alignment of the trial. The status is `draft`
- 2026-11-21T11:43:08Z [human] approved the spec. The status is `approved`
- 2026-11-21T13:03:51Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the sandbox, wrote `plan.md` against the tree
  of `feature-0032-measure-how-well-the-word-highlight-follows-the-voice` at `3ca4a79`. Read `process.md`, the
  standards and `roles/planner.md` of the installed kit. Found that the engine does not give out the rows of phonemes it
  hands sherpa-onnx, so the plan repeats in the measurement the two lines that build its `PhonemeRows`, and that the
  sandbox holds the fp32 model but not `kokoro-env` or `Kokoro-82M`, so the run of requirement 9 is a task for the host.
  The status stays `approved`
- 2026-11-21T13:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the sandbox, built the unit on
  `feature-0032-measure-how-well-the-word-highlight-follows-the-voice` and moved the status to `in-progress`. Wrote
  `HighlightFollowing.kt` and `HighlightFollowingTest.kt` tests first, `scripts/kokoro-durations.py`,
  `HighlightFollowingMeasurement.kt` and the README section. `./gradlew test` reported 940 tests, 7 skipped and 0
  failed on the tree of this entry, with `HighlightFollowingTest` run (25 tests) and `HighlightFollowingMeasurement`
  skipped as its variable is not set. `git diff --stat origin/master -- src/main` is empty. Not checked: the script and
  the measurement have not been run, since the sandbox holds neither `kokoro-env` nor `Kokoro-82M`, so the task of the run
  on the host (requirement 9) is left open in `plan.md`, as are its commit point, and the pairing, the 2% rule and the
  vocabulary check are untested against the real engine. The Python script has no test of its own, as the
  tests of requirement 7 cover the logic in Kotlin. The status is `in-progress`

## Amendments
