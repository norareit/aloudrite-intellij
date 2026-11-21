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
- 2026-11-21T13:11:07Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0032 at `e1f7f25`
  against merge base `3ca4a79` with `origin/master`. Returned S1 and C1, saved in
  `.reviews/0032-measure-how-well-the-word-highlight-follows-the-voice/review-20261121T131024Z.md`. Independently ran
  `./gradlew cleanTest test`, which passed including the 25 new Kotlin tests, and `npm run check-changes`, which passed.
  The opt-in highlight measurement was skipped; could not verify the Python bridge with its real dependencies or the
  required developer Linux x64 shares because this working copy lacks the Python environment and weights. No findings
  on plan fidelity. The status stays `in-progress`
- 2026-11-21T13:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the sandbox, took the review
  `review-20261121T131024Z.md` of `e1f7f25`. Accepted C1 by the definition of the orchestrator, and not as a decision of
  the human: split `durations_of` out of `scripts/kokoro-durations.py` and added `scripts/test_kokoro_durations.py`, whose
  3 tests with a stand-in model cover the line protocol, the speed, the style chosen by the length of the phonemes and
  the output, run by `python3 -I -m unittest discover -s scripts -p 'test_*.py'` (3 tests, passed), which `README.md` now
  names. Added the task to `plan.md`. S1 is true and stays open: the run on the host of requirement 9 is for the
  developer, and nothing in the sandbox can close it. The status is `in-progress`
- 2026-11-21T13:14:07Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0032 at `8792d91`
  from `e1f7f25`, verifying the correction for C1 and returning existing S1 and new non-blocking C2. Saved the review
  in `.reviews/0032-measure-how-well-the-word-highlight-follows-the-voice/review-20261121-round2.md`. Independently ran
  the three Python tests with `python3 -B -I -m unittest discover -s scripts -p 'test_*.py'`, `./gradlew cleanTest test`
  and `npm run check-changes`; all passed. The highlight measurement was skipped, and real Python inference and the
  developer's Linux x64 numerical results remain unverified because the environment and weights are absent. The
  status stays `in-progress`
- 2026-11-21T13:55:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the sandbox, took the delta
  review `review-20261121-round2.md` of `8792d91`. Accepted C2 by the definition of the orchestrator, and not as a
  decision of the human: removed the two tracked `.pyc` files of `scripts/__pycache__/` and added `__pycache__/` to
  `.gitignore`. S1 stays open, as before: the run on the host of requirement 9 is for the developer. The status is
  `in-progress`
- 2026-11-21T13:16:22Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0032 at `0a72ee1`
  from `8792d91`, verifying the correction for C2 and returning existing S1 with no new findings on any axis. Saved
  the review in `.reviews/0032-measure-how-well-the-word-highlight-follows-the-voice/review-20261121-round3.md`.
  Independently ran `python3 -I -m unittest discover -s scripts -p 'test_*.py'` (3 tests passed),
  `./gradlew cleanTest test` and `npm run check-changes`; both suites passed. Confirmed that both bytecode paths are
  untracked and ignored. The highlight measurement was skipped; real Python inference and the developer's Linux x64
  numerical results remain unverified because the Python environment and weights are absent. The status stays
  `in-progress`

## Amendments
