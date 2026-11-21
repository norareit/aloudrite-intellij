---
kind: feature
status: implemented
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
- 2026-11-21T13:22:54Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the
  developer's Linux x64 host, on the unit's branch at `241a850`, at the human's request, ran the measurement of
  requirement 9 with `kokoro-env` (`kokoro` 0.9.4, PyTorch 2.14.0) and `Kokoro-82M` in place. The first run failed with
  259 lines of durations for 258 rows: `KModel` without a `repo_id` prints a warning to the standard output, which the
  measurement read as a line. Added a task to `plan.md` and passed `repo_id="hexgrad/Kokoro-82M"`, which with a local
  configuration and weights downloads nothing; the script's tests pass. The second run ran and was not skipped, but left
  out every piece, 130 of 130 at 1.0× and at 1.5×, so it gives no share. Two causes hold for every piece. First, the
  audio by the durations is about 0.63 s longer than that of the engine at 1.0× and 0.43 s at 1.5×: for "Markdown
  features" the durations give 1.90 s, of which the padding tokens are 16 frames at the start and 7 at the end, and the
  engine 1.27 s, shorter than the 1.33 s of the tokens between the paddings, while the plugin trims nothing; so
  sherpa-onnx or its converted model gives less silence at the ends than the Kokoro Python package, and a check of the
  whole lengths within 2% cannot pass. Second, eSpeak NG joins short words, so that pieces such as "A heading of the
  third level", with 5 phoneme words for 6 words, cannot be paired one to one. Requirements 5 and 9 cannot be met as
  written; the share of requirement 9 is not recorded, and its task stays open. The status is `in-progress`
- 2026-11-21T13:30:01Z [human] accepted findings C1 and C2 of the reviews of `e1f7f25` and `8792d91` as fixed: the tests
  of `scripts/kokoro-durations.py` with a stand-in model, and the Python bytecode removed and ignored. The entry was
  written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-11-21T13:30:01Z [human] decided, after the run of requirement 9 left out every piece, that the unit goes back to
  the specifier, to take the durations from `model.onnx` by ONNX Runtime for Java rather than from the Kokoro Python
  package, to count joined words as a group, and to measure the 11 sentences of the trial of unit 0028 as a second set.
  The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-11-21T13:30:01Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the developer's
  Linux x64 host, on the unit's branch at `9972bd7`, corrected `spec.md`. Read the trial of unit 0028 in the transcript
  of its session: it ran `model.onnx` with ONNX Runtime on 11 sentences made into phonemes a word at a time, with its
  own copy of the estimate. Found that `model.onnx` has a second output, `onnx::Shape_3411`, the durations, which the
  first version of this spec denied. Ran it with ONNX Runtime for Java 1.22.0 on the plugin's tokens for "Markdown
  features" in Heart at 1.0×: the durations add up to its 45,000 samples, and the engine's 30,447 samples lie within
  them 6,524 samples after the start, at a mean difference of 7% of the level. The context now says how the trial
  measured, that its shares do not depend on the release of sherpa-onnx, and why the first version failed. Requirement 3
  takes the durations from `model.onnx` and moves them back by where the engine's audio starts in the model's;
  requirement 5 counts joined words as a group; requirement 6, in place of the 2% of the lengths, leaves out a piece
  whose audio cannot be found within the model's at 20%; requirement 7 adds the trial's sentences as a second set; and
  requirements 8 to 12 follow, numbered on from there, with `README.md` asking no Python. Decided with the human: ONNX
  Runtime for Java, the groups and the second set. Decided by me and open to the human: a group right for any of its
  words, and the 20%. The corrected spec waits for the human's approval before the work goes on. The status is
  `in-progress`
- 2026-11-21T13:33:40Z [human] approved the corrected spec of `52b6c12`, with a group of joined words right for any of
  its words and the 20% for finding the engine's audio. The plan, written for the Kokoro Python package, is to be
  revised by the planner before the work goes on. The entry was written by Claude Code, anthropic/claude-opus-5-5, on
  the human's word. The status is `in-progress`
- 2026-11-21T13:38:56Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session, on the unit's
  branch at `3f73e5d`, revised `plan.md` against the corrected spec of `52b6c12` and the tree. Read
  `process.md`, the standards and `roles/planner.md` of the installed kit. Removed the three tasks of the Kokoro Python
  package, writing `scripts/kokoro-durations.py`, passing it `repo_id` and testing it with a stand-in model, since the
  corrected spec takes the durations from `model.onnx` by ONNX Runtime for Java and rejects the package; their commit
  points stay ticked, as those commits were made. Kept as done the tasks of the phoneme words, the moments and the
  share of a piece and of a speed, reworded to what of them still holds. Adapted the tasks of the measurement class,
  of `README.md`, of the run and of the check of `src/main`, and unticked them. Added the tasks of removing the check of
  the lengths, the words' texts and their bare phonemes, the groups of joined words by an alignment of least edit
  distance, the token ids and the style of `voices.bin`, the placement of the engine's audio at 20%, the spans of placed
  rows, following by groups with the sets in the table, the dependency of the tests, and removing
  `scripts/kokoro-durations.py`, `scripts/test_kokoro_durations.py` and the line `__pycache__/` of `.gitignore`. Found
  that the fp32 model is in its folder here and ONNX Runtime 1.22.0 is on Maven Central, that `tokens.txt` names no
  padding token, so the plan takes the id 0 as sherpa-onnx pads, that `voices.bin` holds 54 speakers of 510 by 256
  values, and that sherpa-onnx and ONNX Runtime for Java each ship a `libonnxruntime.so`. Not checked: the names of the
  model's inputs, which row of the style sherpa-onnx takes, whether the engine's callback gives one array per row, and
  whether both runtimes run in one JVM, since running the downloaded runtime here was not allowed; the plan makes the
  measurement fail on the first and the third, takes the row of the style as the Kokoro package does, and has the
  implementer stop on the last. The status stays `in-progress`
- 2026-11-21T13:55:53Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the sandbox, on the unit's branch at `24a9ab1`,
  worked the open tasks of the revised plan. Committed the logic of the groups, the placement, the spans and the
  following by groups with their tests, then added ONNX Runtime for Java 1.22.0 to the tests, rewrote
  `HighlightFollowingMeasurement.kt`, removed `scripts/kokoro-durations.py`, its tests and the line `__pycache__/` of
  `.gitignore`, and rewrote the section of `README.md`. Both runtimes ran in one JVM, and the engine gave one array of
  samples per row. Necessary work that the plan had wrong: the plan took the style of a row of `n` tokens as row `n - 1` of
  the speaker, as the Kokoro package does. Run on "Markdown features" with rows `n - 1`, `n`, `n + 1` and `n + 2`, the
  engine's audio differs from the model's by 107%, 8%, 83% and 112%, and the model's length is 45,600, 45,000, 45,000 and
  44,400 samples, so sherpa-onnx takes row `n`. Changed `styleOf` and its tests to row `n` and the plan to say so. Not
  written: the test of the plan's last case for the groups, a tie between one word to one phoneme word and one join with
  one split at equal cost, since a tie needs contrived strings, so the tie rule is untested. The model's inputs are
  `tokens`, `style` and `speed` and its outputs `audio` and `onnx::Shape_3411`, as the measurement checks. `./gradlew
  cleanTest test` reported 959 tests, 7 skipped and 0 failed with the measurement skipped, and `git diff --stat
  origin/master -- src/main` is empty, and `git diff origin/master -- build.gradle.kts` adds the one dependency. The
  measurement was run in the sandbox with the fp32 model and `HighlightFollowingTest`, on the tree of this entry without
  the log. Shares, with the pieces counted and left out: the sample at 1.0× 77.4% with 4.9% of the time in groups, 82 and
  48; at 1.5× 75.7% with 4.4%, 100 and 30; the 11 sentences of the trial, which the plugin cuts into 13 pieces, at 1.0×
  80.9% with 5.7%, 5 and 8, and at 1.5× 71.4% with 5.0%, 10 and 3. Those are shares of the pieces that were counted, and
  the left out are mostly the long ones, so they are not the share of the sample, and are not to be put beside the 54% and
  52% of the trial of unit 0028. Reasons of those left out, by kind. Sample at 1.0×: 36 where the engine's audio differs by
  34 to 126% at the best place, 9 where eSpeak NG reads a word as several phoneme words (`AloudRite.`, `src`, `config`,
  `recordFoundBoat`, `name`, `boats`, `Logbook` and such), 2 where the runs of text are not as many as the words and 1
  where eSpeak NG gives no phonemes. At 1.5×: 18, 9, 2 and 1. Trial at 1.0×: 8 where the audio differs, and at 1.5×: 3.
  More than a quarter of the pieces are left out at 1.0× in both sets, 48 of 130 and 8 of 13, so the plan says to stop for
  the planner and not to change its rules. Not found out: why the long pieces do not place, as the short ones do with
  differences of about 8%; at 1.0× twice as many fail as at 1.5×, and the failing pieces are mostly a whole sentence of
  one row. Not checked whether the model's audio for a long row differs from run to run, or whether sherpa-onnx does
  something else to a long row. The task of the run and the commit point of the shares stay open. The status is
  `in-progress`
- 2026-11-21T14:08:07Z [human] decided, after the implementer's run left out 48 of the 130 pieces of the sample at 1.0×
  because their audio did not place within 20%, that the engine's audio is to be placed by its level and its silences
  rather than by the samples at one place, and that the spec is corrected for it. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-11-21T14:08:07Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the developer's
  Linux x64 host, on the unit's branch at `5c3099e`, corrected `spec.md` again. On "Each kind of Markdown comes first,
  in a short section of its own under a heading that names it." in Heart at 1.0×, with ONNX Runtime for Java 1.22.0 and
  the engine's audio saved by a test that was not committed: two runs of `model.onnx` give the same durations and audio
  7% apart; the engine's audio lines up with the model's 250 ms after its start until the comma after "first" and 550 ms
  after it from there; read in sherpa-onnx at `v2.0.2` that `GeneratedAudio::ScaleSilence` shortens every run of samples
  of magnitude at most 0.01 that lasts at least 0.2 s to its first 20%, with the default scale of 0.2 that the plugin
  does not change, on every run of the Kokoro model; and found that the model's audio with that rule applied has 113,667
  samples against the engine's 113,701, and 30,446 against 30,447 for "Markdown features", at 1.3% and 1.1% of the level
  apart in steps of 10 ms with no shift. The context now says so; requirement 3 carries the times of the durations
  through that rule; requirement 6 leaves out a piece whose shortened audio differs from the engine's by more than 1% in
  length or 10% in level; requirement 9 tests the shortening and the check. Decided by me and open to the human:
  sherpa-onnx's own rule in place of a search for one place, which refines the human's choice, and the 1% and 10%. The
  corrected spec waits for the human's approval, and then the planner revises the plan and the implementer changes the
  placement. The status is `in-progress`
- 2026-11-21T14:11:50Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session, on the unit's
  branch at `2401b19`, at the human's request, revised `plan.md` against the spec as corrected in `2401b19` and the
  tree. Read `process.md`, the standards and `roles/planner.md` of the installed kit. Removed the task of the
  placement of the engine's audio at one offset with the 20%, since the corrected spec replaces it by sherpa-onnx's rule
  for silences and the check of requirement 6, and the task of the spans of placed rows, whose spans the corrected spec
  carries through that rule. Added the tasks of removing the placement and its six tests, the shortening of silences,
  carrying a time through it, the check of requirement 6 at 1% of the length and 10% of the level over the piece, the
  spans of carried rows, and the changes of the measurement and of `README.md` from the placement to the shortening,
  each with its tests. Reworded the ticked tasks of the measurement and of `README.md` to what of them still holds, and
  unticked the check of `src/main`, the run of `./gradlew test` and nothing else. Found that `GenerationConfig` of
  sherpa-onnx 2.0.2 sets its silence scale to 0.2 when made, so the plan takes the scale from
  `kokoroGenerationConfig`, and that the Java API holds neither the magnitude 0.01 nor the 0.2 s, so they are
  constants of the measurement. Not checked: the source of `GeneratedAudio::ScaleSilence`, which could not be fetched
  here, so the plan follows the rule as the spec states it, at least 4,800 samples and the kept part truncated to whole
  samples, and the check of requirement 6 catches a piece where that differs from sherpa-onnx. Left as it was: the
  ticked task of the groups names a test of a tie that the implementer recorded as not written. The approval of the
  spec as corrected in `2401b19` is not recorded. The status stays `in-progress`
- 2026-11-21T14:12:46Z [human] approved the corrected spec of `2401b19`, with the durations carried through the rule by
  which sherpa-onnx 2.0.2 shortens silences, and the 1% for the lengths and 10% for the levels of requirement 6. The
  plan as revised in `a06259f` is to be implemented. The entry was written by Claude Code, anthropic/claude-opus-5-5,
  on the human's word. The status is `in-progress`
- 2026-11-21T14:25:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the sandbox, on the unit's branch,
  carried the durations through the shortening of silences as the revised plan says, and ran the measurement. Committed
  as `b229959`: `HighlightFollowing.kt` has `shortenSilences`, `carriedThrough`, `disagreementOfShortened` and
  `CarriedRow`, and `spansOf` carries both ends of each word; the placement and its six tests are gone; the measurement
  shortens the model's audio of each row with the silence scale of `kokoroGenerationConfig` and checks the piece against
  the engine's by requirement 6; `README.md` says so. Verified: `./gradlew test` on the final tree reports 970 tests,
  7 skipped, 0 failures, with `HighlightFollowingTest` at 55 tests and the measurement skipped without its variable;
  `git diff --stat 3ca4a79 -- src/main` is empty and `build.gradle.kts` gained the one dependency of the tests. The
  shares below are from `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING=1 ./gradlew cleanTest test --tests
  com.ritenoar.aloudrite.HighlightFollowingMeasurement` on Linux x64 (`build/highlight-following.txt`), with the fp32
  model and Heart, run on `b229959`. Share of the time within words in which the word shown is the word heard, with
  the share of that time in groups of joined words, and the pieces counted and left out. The sample: 1.0× 72.2% with
  5.0% in groups, 118 counted and 12 left out; 1.5× 72.8% with 5.2%, 110 and 20. The 11 sentences of the trial, which
  the plugin cuts into 13 pieces: 1.0× 74.8% with 4.7%, 13 and 0; 1.5× 71.6% with 4.8%, 11 and 2. Beside them, the trial
  of unit 0028 had 54% at 1.0× and 52% at 1.5×, but it made its phonemes a word at a time and counted with its own copy
  of the estimate over whole sentences, so the two are not the same measure. Left out, by kind. Sample at 1.0×: 9 where
  eSpeak NG reads a word as several phoneme words, 2 where the runs of text are not as many as the words, 1 where
  eSpeak NG gives no phonemes. At 1.5×: the same 9, 2 and 1, and 8 where the shortened audio does not stand for the
  engine's. Trial at 1.5×: 2 of that last kind. Of those 10 together, 7 differ in level and 3 in length, the sizes being in
  the file. At 1.0× none of either set is left out for the audio. At most
  15% of a set's pieces is left out at either speed, under the quarter at which the plan says to stop. Not checked: why
  those 10 differ, which may be the noise of the decoder moving a silence across 0.01, as the spec expects, nor the
  effect of the left out pieces on the shares. The status is `in-progress`
- 2026-11-21T14:24:39Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0032 at `41bfb2c`
  from `0a72ee1`, against the corrected spec. Returned P1 and C3, saved in
  `.reviews/0032-measure-how-well-the-word-highlight-follows-the-voice/review-20261121T142439Z.md`. Independently ran
  `./gradlew cleanTest test`, `npm run check-changes` and the opt-in `HighlightFollowingMeasurement` command of
  `README.md` with the fp32 model in place; all passed, with the opt-in measurement not skipped. Verified that the
  results required by R11 are recorded, so previous S1 no longer applies. The independent run gave sample shares of
  72.0% and 72.7%, and trial shares of 74.9% and 72.5%, at 1.0 and 1.5 respectively, with counts in the findings file.
  Not verified: speaker-to-highlight latency or a macOS run. The status stays `in-progress`
- 2026-11-21T14:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, took the
  findings of `review-20261121T142439Z.md`. C3 (blocking, no test of the tie rule of the alignment): applied. Added
  `whereOneToOneAndOneJoinWithOneSplitCostTheSameAGroupOfOneWordEachIsTaken` to `HighlightFollowingTest.kt` with the
  reviewer's strings, `aa`, `a`, `a` against `a`, `aa`, `a`, which asserts three groups of one word each. Verified that it
  fails with `joinsAndSplits` removed from the comparison of `AlignmentCost` and passes with it, and so the omission
  recorded earlier is made good. P1 (stale marks in `plan.md`): applied, the check-changes box and the commit point of the
  shares are ticked, as `npm run check-changes` passes and the shares were committed in `41bfb2c`. On the final tree at
  `5274673`, `./gradlew test` is green, with `HighlightFollowingTest` at 56 tests and the measurement skipped without its
  variable, and `npm run check-changes` passes. The reviewer's independent run gave other shares and counts than the
  recorded ones, as the decoder's noise moves which pieces are left out; both are under the quarter. The status stays
  `in-progress`; the human decides the findings before `implemented`
- 2026-11-21T16:06:33Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0032 at `850ced5`
  from `41bfb2c`, verifying the corrections for C3 and P1. Returned no new findings on spec conformance, plan fidelity
  or code quality. Saved the review in
  `.reviews/0032-measure-how-well-the-word-highlight-follows-the-voice/review-20261121T160633Z.md`. Independently ran
  `./gradlew cleanTest test` and `npm run check-changes`; both passed, with all 56 `HighlightFollowingTest` tests run,
  including the tie case. The opt-in model measurement was not repeated because its code is unchanged. Did not
  independently reproduce the implementer's historical mutation run or check speaker latency or macOS. The status
  stays `in-progress`
- 2026-11-21T17:04:53Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  x64 host, on the unit's branch at `3b9f337`, at the human's request, ran
  `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING=1 ./gradlew cleanTest test --tests
  com.ritenoar.aloudrite.HighlightFollowingMeasurement` with the fp32 model and Heart; it ran and was not skipped. The
  sample: 1.0× 72.3% with 5.1% in groups, 114 counted and 16 left out; 1.5× 73.0% with 5.1%, 113 and 17. The trial:
  1.0× 75.4% with 5.1%, 12 and 1; 1.5× 71.6% with 4.6%, 13 and 0. These lie within a point of the shares of the
  sandbox and of the reviewer, as the noise of the decoder moves them. Seen in the file: the items of the numbered
  lists score lowest of the sample, from 33% to 47%, while the estimate weighs the number by its one digit and the voice
  says it as a word. The status is `in-progress`
- 2026-11-21T17:04:53Z [human] accepted findings C3 and P1 of the review of `41bfb2c` as fixed: the test of the tie rule
  of the alignment, and the ticked marks of `plan.md`, which the delta review of `850ced5` found corrected. Every
  blocking finding of the reviews is decided. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the
  human's word
- 2026-11-21T17:04:53Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on the unit's branch, at the human's request, set the status. `./gradlew test` passed in the reviews of
  `850ced5`, and the commits after it change only `record.md`. `npm run check-changes` passes. Every task of `plan.md`
  is ticked. The status is `implemented`

## Amendments
