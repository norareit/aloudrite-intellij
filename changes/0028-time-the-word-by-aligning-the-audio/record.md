---
kind: feature
status: in-progress
timestamp: 2026-11-19T21:25:28Z
---

# Time the highlighted word by aligning the audio

## Log

- 2026-11-19T21:25:28Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `master` at `4e900a6`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0028, after 0027. Before writing it,
  ran a trial outside the project: confirmed that the Kokoro model returns the durations of its tokens and that
  sherpa-onnx 1.13.8 and 2.0.2 discard them, and measured the forced aligner of sherpa-onnx 2.0.2 against those
  durations, with the results the spec's context gives. Found that sherpa-onnx 2.0.2 skips words not in the lexicon
  where 1.13.8 speaks them. Decided with the human: the unit itself, after the trial, and that its record holds the
  human's decision on the default of the word highlight of unit 0027. Decided by me and open to the human: forced
  alignment over the durations of the model, the delay of 70 ms set by ear, and the aligner model downloaded with a
  speech model rather than shipped in the plugin archive. Left beyond the request: keeping the times of a piece played
  again. The status is `draft`
- 2026-11-19T22:36:38Z [human] approved the spec. The status is `approved`
- 2026-11-20T00:47:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `f2b4f6e`, at the human's request, set the status back to `draft`. The planner, in a run of the
  orchestrator on the unit's branch at `537afd2`, had stopped before writing `plan.md`: under sherpa-onnx 2.0.2 every
  word the lexicon lacks is skipped, numbers, abbreviations, words of code and symbols among them, because 2.0
  dropped eSpeak NG, so requirement 5 could not be met as written. The human had the branch deleted, since the work
  on the unit had not started, and with it the planner's entry in this record. Unit 0030 moves the plugin to
  sherpa-onnx 2 with the plugin's own fallback to eSpeak NG, and the human wants this spec corrected once unit 0030 is
  implemented, as its requirement 15 names what is to change, and approved again only then. Requirement 5, the
  context on the risk of moving to 2.0.2, and the reason of requirement 11 for the check on macOS are among what is
  to be looked at again. The planner's facts for the plan, kept here: the aligner model is published at
  `https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2`, 47,567,475 bytes,
  SHA-256 `86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da`, and unpacks to the folder
  `english-mfa-v3.1.0` of 51,887,069 bytes, holding `final.alimdl`, `lda.mat`, `tree`, `phones.txt`, `lexicon.txt`,
  `meta.json`, a `README.md` and a test file `en.wav`. Its Java API is `ForcedAligner`, configured by the model folder
  and its `lexicon.txt`, whose `compute` takes a stream given the samples and the text and returns tiers of intervals
  with a start, an end and a text. The native library of 2.0.2 is about 0.6 MB larger per platform than that of
  1.13.8. The status is `draft`
- 2026-11-20T14:49:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `8e716ff`, at the human's request once the review of unit 0030 at `f905ca7` returned no blocking
  finding, and before unit 0030 is set to `implemented`, corrected `spec.md` from the entry of requirement 15 in the
  record of unit 0030. Read `process.md`, the standards and `roles/specifier.md` of the installed kit. The context now
  says that the plugin uses sherpa-onnx 2.0.2 since unit 0030 and makes its own phonemes, so that every word is spoken,
  and that the trial ran under 1.13.8 and is taken to hold, since unit 0030 measured the audio within 0.5% of its length
  under 1.13.8; the paragraph on the risk of moving to 2.0.2 is gone. Requirement 5, that every word is still spoken
  under 2.0, and its test with the real engine in requirement 7 are removed as done by unit 0030, and the requirements
  after it are renumbered, from 6 to 11 now 5 to 10. Requirement 9 names `THIRD-PARTY.md` for the license of the aligner
  model, and requirement 10 asks for the check on macOS because the aligner is a part of the native library not used
  before. The decision on forced alignment now weighs the cost after unit 0030, which no longer includes the conversion
  of text to phonemes, keeps forced alignment and is open to the human. The sizes of the plugin archive are those after
  unit 0030, and a non-goal names the move of release. Requirement 8 is unchanged: its "before this unit" is now the
  state after unit 0030. The spec is to be approved again. The status is `draft`
- 2026-11-20T15:13:05Z [human] approved the spec. The status is `approved`
- 2026-11-21T17:10:02Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `f944eac`, at the human's request, set the status back to `draft` and corrected `spec.md`, before any
  work on the unit had started and with no branch of it. Read `process.md`, the standards and `roles/specifier.md` of
  the installed kit. The human wants the unit measured against the baseline of unit 0032 and dropped if it gains too
  little for its side effects. The context now gives the shares of unit 0032, 72.3% and 73.0% for the sample and 75.4%
  and 71.6% for the trial, from the run on the host at `3b9f337`, why they are higher than the 54% and 52% of the
  trial, and the low shares of the items of numbered lists. Requirement 11 has the measurement of unit 0032 measure the
  word by the alignment beside the estimate, in one run, with no delay and with the delay, and requirement 12 the
  record hold its shares. Requirement 6 covers the measurement's choice of the word with tests, and requirement 13,
  which was 11, adds the human's decision on keeping the unit before the decision on the default. Decided with the
  human: the decision to keep or drop the unit. Decided by me and open to the human: no share fixed in advance as the
  bar, comparing in one run against the estimate, and measuring before the download, the settings and the fallback
  are built. Left beyond the request: weighing a number in the estimate by its spoken word. The spec is to be
  approved again. The status is `draft`
- 2026-11-21T17:14:23Z [human] approved the spec. The status is `approved`
- 2026-11-21T17:19:59Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a run of the orchestrator in the
  sandbox, wrote `plan.md` against the tree of `feature-0028-time-the-word-by-aligning-the-audio` at `41daa51`. Read
  `process.md`, the standards and `roles/planner.md` of the installed kit. Found the Java API of the aligner in
  `sherpa-onnx-jvm-2.0.2.jar`: `ForcedAligner`, `ForcedAlignerConfig`, `OfflineStream`, `ForcedAlignmentResult`,
  `AlignmentTier` and `AlignmentInterval`. The plan has two phases, as the spec's decision on measuring before the rest
  is built asks: the alignment, the aligner model put in place by hand, the test with the real aligner and the
  measurement first, then a stop for the human's word on whether to go on, then the alignment at play time, the
  download, the page, the removal and the documentation. Decided in the plan, as how and not what: the words of
  punctuation only, which the estimate never shows, are not given to the aligner and are never shown by it; before the
  first word's start plus the delay, a piece shows its first word, so that the sentence changes at the start of the
  piece as now; the aligner runs on a thread of its own and is made at the first piece aligned, off the way to the first
  audio; the delay can be set for `runIde` by a Gradle property, so that the developer sets it by ear without a build
  per value. The sandbox holds both Kokoro models and not the aligner model, which the implementer puts in place by hand.
  The status stays `approved`
- 2026-11-21T17:29:55Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a run of the orchestrator in the sandbox, began the
  work on `feature-0028-time-the-word-by-aligning-the-audio`. Read `process.md`, the standards, `roles/implementer.md`,
  `spec.md`, `plan.md` and this record. Built phase 1 of the plan, in the commits `b068f23` and `6e505a3`: the text of
  `HeardWord`, `WordAlignment.kt` with the parts of the words, `wordStartsOf` and the pure choice of the word, the
  wrapper `ForcedWordAligner.kt`, `AlignerModel.kt`, `TrialSentences.kt`, `RealAlignerTest.kt`, and the measurement of
  unit 0032 extended to the alignment, with `HeardWordChoice.kt` holding only `WORD_DELAY_MILLISECONDS = 70`. Put the
  aligner model in place by hand in `~/.local/share/aloudrite/english-mfa-v3.1.0` from the address of the plan, after
  checking that the archive is 47,567,475 bytes with the SHA-256 of the plan. The status is `in-progress`
- 2026-11-21T17:29:55Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran the measurement of phase 1 on Linux x64
  (the sandbox, `Linux amd64`) with Heart, the fp32 model and the aligner model in place, at `6e505a3` but for the edit of
  `README.md` in the same commit, with `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING=1 ./gradlew cleanTest test --tests
  com.ritenoar.aloudrite.HighlightFollowingMeasurement`. The share of the moments within words in which the word shown was
  the word heard, by the estimate, by the alignment with no delay and by the alignment shown 70 ms later:
  `samples/markdown-features.md` at 1.0×: 71.8%, 86.4%, 97.3%, and at 1.5×: 72.3%, 81.2%, 90.4%; the 11 sentences of the
  trial at 1.0×: 74.9%, 83.8%, 96.6%, and at 1.5×: 73.0%, 78.7%, 91.6%. The pieces counted: 114 and 111 for the sample, 13
  and 12 for the trial, with 16, 19, 0 and 1 left out as in unit 0032. One piece of the sample, number 19, "Strikethrough",
  counted by the estimate at both speeds, as the aligner gave no tier at all for a text of which every word is out of its
  lexicon; its share by the estimate was 100% anyway. No other piece counted by the estimate. Two runs differ by about a
  point, as the spec says. The delay of 70 ms was not set by ear, since that is the check of requirement 10 in phase 2.
  Not measured: the first audio (requirement 8), and any platform but Linux x64
- 2026-11-21T17:29:55Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew test` on the tree of `6e505a3`,
  with both Kokoro models and the aligner model in place. It passed, with `RealAlignerTest` run and not skipped: the 11
  sentences of the trial at 1.0× and at 1.5×, in all their pieces, gave one interval to each part, in order and within
  the audio. Stopped before phase 2, as the plan and the spec's decision on measuring before the rest is built ask, and
  asked the human whether to go on, since the unit costs a download of 45 MB, a part of the native library not used
  before and up to 50 ms on the start of a reading, against a gain from about 72% to about 97% at 1.0× and to about 90% at
  1.5× with the delay of 70 ms. Phase 2 is not started. The status stays `in-progress`
- 2026-11-21T17:43:48Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a session on the host's sandbox, took the word of the human
  that the unit goes on after the shares of phase 1, as the session was told, and began phase 2. The status stays
  `in-progress`
- 2026-11-21T17:44:40Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built the alignment at play time in `b8d2a50`:
  `HeardWordChoice`, `PieceAligning`, the samples of a piece handed to it by `SpeechService`, the Gradle property
  `wordDelayMilliseconds` for `runIde`, and their tests with the stand-ins. Extended `FirstAudioMeasurement` with a run
  of `SpeechService` with the real engine of the fp32 model, a player that only notes its first write and the real
  aligner, against the same run with an aligner model never in place. Ran it on Linux x64 (`Linux amd64`, three engine
  threads) with both models in place: the first write came after 831, 806, 748, 757 and 753 ms with the aligner, median
  757 ms, and after 797, 803, 749, 756 and 741 ms without it, median 756 ms. The difference of the medians is 1 ms, within
  the 50 ms of requirement 8, on this machine and with this piece. Not measured: macOS on Apple Silicon, and a machine with
  fewer processors, where the aligner's thread competes more with the engine for them
- 2026-11-21T17:54:15Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built the download, the questions, the page and the
  removal of the aligner model in this commit, after the refactoring of the installable package in `1170257`, which kept
  the tests as they were but for their types. Differences from the plan, none of which changes what the spec asks:
  `downloadPackage` and `unpackPackage` keep taking a `ModelPackage` and gained a last parameter `progressName`, which
  defaults to "the speech model", so that the progress names the package without a change of the texts that the tests
  look for; the download service takes `installedWith`, which names what is installed after a package in the same
  attempt and defaults to nothing, so that the service tests that do not concern the aligner model stay as they are; the
  title of the notification of a failed download is now "AloudRite could not download a model", since it also reports
  the aligner model. The aligner model is installed after the speech model in the same attempt, and what follows the
  speech model runs after both, also where the aligner model failed. Not done: the look at the page in the sandbox IDE
  that `AGENTS.md` asks for, as this sandbox has no display and no `xwd`, so the task of the page stays open for the
  developer's own look, which the check of requirement 10 also needs. The layout of the new row is covered only by the
  tests of its texts and its enabled state
- 2026-11-21T17:54:21Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, described the alignment in `README.md` and
  `THIRD-PARTY.md`: how the word is found and what a piece without an alignment shows, the delay and the Gradle property
  for `runIde`, the aligner model with its size, its download, its folder, its removal and putting it in place by hand,
  `RealAlignerTest` and the new run of `FirstAudioMeasurement`. The license of the aligner model, CC BY 4.0, is taken from
  the `README.md` inside its package, which the sandbox holds unpacked, and says that it is as declared by the upstream
  model card, which was not read. The paragraph of the highlights keeps its sentence on the default of the word highlight
  as it was. The commands of putting the model in place by hand were not run as written, apart from the download and the
  checksum of the archive in phase 1; their `tar` line is that of the speech models
- 2026-11-21T17:54:29Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew test buildPlugin` on the code of
  `a602519`, which the commit of the documents after it leaves as it is, with both Kokoro models and the aligner model in
  place: 1,052 tests, 0 failures, 8 skipped. The skipped are the measurements that need their environment variable
  (`EngineThreadsMeasurement`, `EngineSilenceMeasurement`, `HighlightFollowingMeasurement`, `ReadingTimeMeasurement`,
  `EngineLengthsMeasurement`, and the two of `FirstAudioMeasurement`) and `RealModelPackageTest`; `RealAlignerTest` ran and
  was not skipped. The plugin archive was built within its limit. `npm run check-changes` passed after the commits. What is
  left of phase 2 needs the developer: the look at the page, the check of requirement 10 on Linux x64 and macOS on Apple
  Silicon with `-PwordDelayMilliseconds`, the constant set by ear with the measurement run again, and, after the review,
  the decision of requirement 13. The status stays `in-progress`
- 2026-11-21T18:05:51Z [OpenCode, openai/gpt-6.2-sol] as the reviewer, completed the full review of unit 0028 at
  `5fef79ba740bda7a56848692f544c3bf2e4a70d0`. Returned S1 through S5, P1, C1 and C2 in
  `.reviews/0028-time-the-word-by-aligning-the-audio/review-round-1-5fef79b.md`. Ran the test suite, plugin build,
  change check and opt-in measurements on Linux x64. Could not verify the settings layout, the developer's listening
  checks and by-ear delay, the first-audio comparison against the pre-unit revision, the exact interval bounds of
  the native trial, installation of the published package through the plugin, or the upstream model attribution
- 2026-11-21T18:14:10Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took the findings of
  `review-round-1-5fef79b.md` and applied S1, S4, P1, C1 and C2, which I accept as correct, added as a task to `plan.md`.
  S1: the question of a download is made once the lock is held, and a removal takes the lock before it asks, so that the
  aligner model that the question names is the one that is downloaded or removed, and a test asserts the lock is held while
  either question is open. S4: `RealAlignerTest` bounds each end by the audio, with no margin, and passes with the real
  aligner, run and not skipped. P1: the download service publishes the change when it switches to the aligner model, after
  storing it, and the start of a download no longer overwrites it; a test covers the sequence of what is downloaded at
  each change. C1: `wordStartsOf` also requires the texts of the intervals to be the parts and each start to be a time
  within the audio, with an end not before it, and so gives the fallback otherwise; its tests cover other texts of the same
  number, `NaN`, negative and late starts. C2: the unused `textToAlign` and its test are removed, and the wrapper alone
  joins the words. Ran `./gradlew test` on the tree of these changes: it passed, with `RealAlignerTest` run. Not applied,
  and open for the human: S2, S3 and S5. On S2, the pre-unit baseline can be measured by running the same harness on the
  merge base, which I propose to do only if the human wants it, since the 1 ms of the proxy compares two modes of the new
  code. On S5, the reuse of an alignment over a resume is a few lines and has tests; the unit needs the human's word on
  keeping it or removing it. S3 is the checks of the developer. The status stays `in-progress`
- 2026-11-21T18:22:54Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, recorded the human's decision, given in the session,
  to keep BTR1 of the spec, the reuse of the alignment of a piece over a pause and a resume, as built. This settles S5 of
  `review-round-1-5fef79b.md`: no code or test changes. S2 and S3 stay open. The status stays `in-progress`
- 2026-11-21T18:25:07Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, measured the baseline that S2 of
  `review-round-1-5fef79b.md` asks for, as the human told me to. In a git worktree of `41daa51`, the tree before the unit,
  I put a test with the same boundaries as the service run of `FirstAudioMeasurement`: from the call of `read` of
  `SpeechService`, with the real engine of the fp32 model, Heart, 1.0× and a player that only notes its first write, to that
  first write, nine runs, and no aligner since there was none. I ran it and the run of this tree alternately, twice each, on
  Linux x64 (`Linux amd64`, three engine threads) in the sandbox. The test of the baseline was not committed and the
  worktree is removed. Medians of the milliseconds to the first write: the tree before the unit 766 and 757 ms (nine runs
  each, the first run of each 982 and 994 ms); this tree with the aligner 799 and 806 ms and with an aligner model never in
  place 761 and 755 ms (five runs each). The aligner therefore adds about 40 ms, 33 to 49 ms by the four pairs of medians,
  to the baseline, which is under the 50 ms of requirement 8 on this machine by a thin margin and with the noise of runs
  of five, and the code of the unit with no aligner model adds nothing that shows: 761 and 755 against 766 and 757 ms.
  The cost is the aligner of the first piece being made and loading its model while the engine makes the rest of the
  piece, which is the part this unit changes; it was not split further. Not measured: macOS on Apple Silicon, a machine
  with fewer processors, and the real Read Aloud action and player. S2 is answered with these numbers for the human to
  weigh. The status stays `in-progress`
- 2026-11-21T18:33:31Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, recorded the human's decision, given in the session,
  to accept the added start of about 40 ms measured against the tree before the unit as meeting requirement 8, with no
  change to reduce it. This settles S2 of `review-round-1-5fef79b.md`. S3 stays open. The status stays `in-progress`
- 2026-11-21T18:40:17Z [OpenCode, openai/gpt-6.2-sol] as the reviewer, completed the delta review of unit 0028 at
  `c3f7522903107917b6d95155efe46024c20a142b` against `5fef79ba740bda7a56848692f544c3bf2e4a70d0`. Returned S1, S3,
  P2 and C1 in `.reviews/0028-time-the-word-by-aligning-the-audio/review-round-2-c3f7522.md`. Ran the test suite,
  plugin build and change check on Linux x64. Could not rerun the temporary first-audio baseline or verify the
  developer's listening checks, by-ear delay or settings layout in this sandbox
- 2026-11-21T18:44:38Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took the findings of
  `review-round-2-c3f7522.md` and applied S1, P2 and C1, added as a task to `plan.md`. S1: the download service decides
  what is installed with a model when the download starts, with the lock held and the question answered, and a retry
  carries that set and adds none, so a package that went missing afterwards is not downloaded on a retry; a test removes the
  aligner model between a failure and the retry. P2: two tests of `SpeechModelManagementTest` change the models at the
  moment the lock is taken, and assert that the question of a download and of a removal state the aligner model as it is
  then. I did not check that they fail with the question made before the lock. C1: a start must lie before the end of the
  audio, so that a sample holds it, and an end must be a finite time not before its start; the cases of the review, a start
  at the end of the piece and an infinite end, are in `WordAlignmentTest`. Proposed decline, for the human to decide: the
  review also asks to bound each end by the audio in this validator. Only starts are used, and the native code may round an
  end a little past the audio, so a bound on ends would send a piece that is placed right to the estimate for no gain, while
  `RealAlignerTest` still holds the ends to the audio on the trial. Ran `./gradlew test` on the tree of these changes: it
  passed, with `RealAlignerTest` run. S3 stays open. The status stays `in-progress`
- 2026-11-21T20:10:18Z [human] accepted the decline that the implementer proposed for the part of C1 of
  `review-round-2-c3f7522.md` that asks to bound each end of an interval by the audio: only the starts are used, and
  `RealAlignerTest` holds the ends to the audio on the trial. The rest of C1, and S1 and P2, were applied in `2dcc1d9`
  and wait for the delta review. S3 stays open. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the
  human's word. The status is `in-progress`
- 2026-11-21T20:47:48Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux x64 host, on the
  unit's branch at `cc20fac`, at the human's request, looked at the page "AloudRite" under Settings › Tools in the
  sandbox IDE, started with `./gradlew runIde`, and captured only the window of its settings dialog. The host holds both
  Kokoro models and not the aligner model. Under "Speech model", below the two models, the row "Word timing
  (english-mfa-v3.1.0)" shows an enabled "Download" in the column of the other buttons, with the comment "48 MB
  download, 52 MB on disk, not on this machine" below it in the style of the others. Nothing is cut off or out of line.
  Closed the dialog with Cancel, changing nothing, and downloaded nothing. Ticked the task of the page in `plan.md`. Not
  checked: the row while the aligner model downloads or once it is in place. The status stays `in-progress`
- 2026-11-21T22:06:13Z [human] decided that nothing more tells a listener who had a speech model before this unit that the aligner
  model can be downloaded, beyond its row on the page "AloudRite", since no build of the plugin has been given to anyone
  but the developer. A listener who downloads a first speech model gets the aligner model with it, by requirement 5. The
  entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word. The status is `in-progress`
- 2026-11-21T22:11:21Z [human] checked the unit on Linux x64 in the sandbox IDE at `af45845`, with the delay of 70 ms, and downloaded
  the aligner model from its row on the page "AloudRite". Listened to the first part of `samples/markdown-features.md`,
  where the word highlight followed the voice well, and to the first part of its long prose at the end, which the
  estimate follows worst, where it was better with the aligner model than before. The delay stays at 70 ms. The speeds
  listened at were not named one by one. The check on macOS on Apple Silicon is to follow. The entry was written by
  Claude Code, anthropic/claude-opus-5-5, on the human's word. The status is `in-progress`
- 2026-11-21T22:16:51Z [human] checked the unit on macOS on Apple Silicon in the sandbox IDE, on the branch as pushed at the entry
  before, with the delay of 70 ms: downloaded the aligner model from its row on the page "AloudRite", and listened to
  the first part of `samples/markdown-features.md` and the first part of its long prose at 1.0× and at 1.5×. At both
  speeds the word highlight followed the voice well. With the check on Linux x64, this answers S3 of the review as far
  as the checks of the developer go, and the delay set by ear is 70 ms. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word. The status is `in-progress`
- 2026-11-21T22:20:34Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux x64 host, on the
  unit's branch at `f01dcdd`, at the human's request, took the delay set by ear in the checks of the developer, 70 ms,
  which `WORD_DELAY_MILLISECONDS` already holds, so no code changes. Ran `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING=1
  ./gradlew cleanTest test --tests com.ritenoar.aloudrite.HighlightFollowingMeasurement` with Heart, the fp32 model and
  the aligner model in place, which the developer downloaded from the page "AloudRite"; it ran and was not skipped. The
  share by the estimate, by the alignment with no delay and by the alignment with the delay of 70 ms, for requirement
  12: `samples/markdown-features.md` at 1.0× 72.1%, 86.5% and 97.3%, with 115 pieces counted and 15 left out; at 1.5×
  72.7%, 80.9% and 90.7%, with 112 and 18. The 11 sentences of the trial at 1.0× 74.8%, 83.7% and 96.7%, with 13 and 0;
  at 1.5× 70.1%, 79.0% and 90.8%, with 12 and 1. One piece counted by the estimate, at both speeds of the sample: number
  19, "Strikethrough", for which the aligner gave no tier, as in phase 1. Then ran `./gradlew cleanTest test buildPlugin`
  with both Kokoro models and the aligner model in place: 1,059 tests, 0 failures, 0 errors, 8 skipped, the opt-in
  measurements and `RealModelPackageTest`, with `RealAlignerTest`, `KokoroModelTest`, `KokoroVoicesTest`,
  `KokoroSpeechEngineTest` and `KokoroTtsConfigTest` run and none skipped. The plugin archive is 21,945,254 bytes.
  `npm run check-changes` passes. Ticked the tasks of the check, of the delay and of its commit point. Open: the delta
  review of `2dcc1d9` and what follows it, and the decision of requirement 13. The status stays `in-progress`

## Amendments
