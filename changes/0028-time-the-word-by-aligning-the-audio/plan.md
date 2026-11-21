# Time the highlighted word by aligning the audio

## Approach

The work is in two phases, in the order the spec's decision "Measured before the rest is built" sets. Phase 1 builds
only what the measurement needs: the wrapper of the aligner, the pure logic that turns its intervals into the starts of
the words and chooses the word shown, the aligner model as a known package put in place by hand, the test with the real
aligner, and the measurement of unit 0032 extended to it. The implementer then runs the measurement, records its shares
and stops for the human's word on whether to go on. Phase 2, only after that word, builds the alignment at play time,
the fallback, the download, the settings page, the removal, the documentation and the checks.

**The aligner.** sherpa-onnx 2.0.2 has `com.k2fsa.sherpa.onnx.ForcedAligner`, built from a `ForcedAlignerConfig` with
`modelDir` the model folder and `lexicon` its `lexicon.txt`. `createStream()` gives an `OfflineStream`, which is given
the samples by `acceptWaveform(samples, sampleRate)`, and `compute(stream, text)` returns a `ForcedAlignmentResult` whose
`getTiers()` are `AlignmentTier`s of `AlignmentInterval`s with `getStart()`, `getEnd()` in seconds and `getText()`. Both
the stream and the aligner are released by `release()`. The plugin names these classes in one file, `ForcedWordAligner.kt`,
as `KokoroSpeechEngine.kt` names those of the engine. Behind it is an interface, `WordAligner : AutoCloseable`, with one
function that takes the samples of a piece, their sample rate and the words to align, and returns the intervals of the
tier of words as plain values, `AlignedInterval(startSeconds, endSeconds, text)`. The tier of words is the one named
`words`, as the Montreal Forced Aligner names it; the test with the real aligner (R7) is where that is confirmed, and
where the implementer sees whether `acceptWaveform` takes the 24,000 Hz of Kokoro as it is, as the trial of the spec did.

**The words given to the aligner.** `HeardWord` gains `text`, the characters of the word that its piece holds, which
`ReadingSegments` has at hand where it computes the weight. The words given to the aligner for a piece are, for each
`HeardWord`, its text split at white space, each part stripped at both ends of the characters of the punctuation
categories that the weight leaves out, lowercased in `Locale.ROOT`, and the empty parts dropped. A word therefore has
parts exactly when its weight is above 0: the words of punctuation only, which the estimate never shows either, are not
given to the aligner and are never shown by the alignment. A word that stands for a block, such as "Code example", has
several parts, and its start is the start of its first part. The aligner is given the parts joined by single spaces.

**From intervals to starts.** The intervals whose text is not blank are taken in order, one per part given. Where their
number is not the number of parts, or a start is before the start of the interval before it, the piece has no
alignment, which is the case of requirement 3 of no start for each word. Otherwise each word with parts has the start of
the interval of its first part, in samples of the piece's audio, `(startSeconds * sampleRate).toLong()`. This is a pure
function, `wordStartsOf`, in `WordAlignment.kt`, which returns `PieceAlignment(starts: List<Long?>)`, null for the
words without parts, or null for no alignment.

**The word shown.** `HeardWordChoice` in `HeardWordChoice.kt` takes over the place of `HeardWordEstimate` in a reading.
It holds one `HeardWordEstimate`, which it tells every position as today, so that the estimate is current whenever a piece
falls back to it, and the alignments of the pieces by segment number. At a position in a piece with an alignment, the word
shown is the last word with a start whose start plus the delay is at or before the position, and the first word with a
start where there is none yet, so that the sentence still changes at the start of the piece as it does now. Through the
silence between two words it therefore stays on the word before (R4). At a position in a piece without an alignment, and
in a silence, the places are the estimate's, which in a silence are the end of the piece before, as in unit 0025. The
delay is in samples, from the milliseconds and the sample rate. The function that picks the word from the starts, the
position and the delay is pure and is what the measurement uses too, so that the measurement measures the word the
plugin shows.

**The delay.** `WORD_DELAY_MILLISECONDS = 70` in `HeardWordChoice.kt` is the start of R2. So that the developer can set
it by ear without a build per value, the plugin reads the system property `aloudrite.word.delay.milliseconds`, which
`runIde` sets only where the Gradle property `wordDelayMilliseconds` is given, as `./gradlew runIde
-PwordDelayMilliseconds=90`. An installed plugin never has it and uses the constant. Once the developer has set the time,
the constant is changed to it and the record holds it.

**The aligner model.** `ALIGNER_MODEL` in `AlignerModel.kt` is the package `english-mfa-v3.1.0` at
`https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2`, 47,567,475 bytes, SHA-256
`86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da`, unpacking to the folder `english-mfa-v3.1.0` of
51,887,069 bytes, in the folder of the speech models, `userModelsFolder()`. It is in place where `final.alimdl`,
`lda.mat`, `tree`, `phones.txt`, `lexicon.txt` and `meta.json` are files there. The installer, the offer and the download
service take an `InstallablePackage`, an interface of the package name, the `ModelPackage`, the check of being in place
and the name shown in the progress, which `SpeechModel` and the aligner model implement. That change is a refactoring and
is committed on its own before the behaviour that uses it.

**Alignment at play time.** `PieceAligning` in `PieceAligning.kt` owns the aligner and one thread of its own, a bounded
executor of one thread named "AloudRite alignment", so that aligning never holds the engine or the player and a reading
never waits for it (R3). Its collaborators are handed in: whether the aligner model is in place, how to create a
`WordAligner`, how to run work on its thread, and how to log a failure. It is asked to align a piece with its samples,
sample rate and words, and calls back with the `PieceAlignment` or nothing. Before each piece it looks whether the aligner
model is in place: where it is and no aligner exists, it creates one, which keeps the loading of the model off the way to
the first audio, since the first piece is queued only once it is made in full; where it is not and an aligner exists, it
releases it, which follows a removal; where it is not, it gives nothing, and the piece shows the estimate (R5). A failure
of creating or aligning is logged as a warning, gives nothing, and ends neither the reading nor the highlights (R3). It is
released with the service.

In `SpeechService`, `makeTheAudioOf` keeps the samples of a piece of words as they are put, and once the engine has made the
whole piece for a making that is still current, hands them to `PieceAligning`. Its callback goes to
`Reading.aligned(making, segment, alignment)`, which keeps it only while the making is current. A new making, after a
change of the speed, makes new audio for the same segment numbers, so `startAMaking` drops the alignments kept. A pause
and a resume keep them, since a resume plays the piece again from its first run, whose positions are those the alignment
was made on.

**The download, the page and the removal (R5).** The download of a speech model installs the speech model and then the
aligner model where it is not in place, in one attempt and one background task. A failure of the aligner model after the
speech model is in place still runs what follows the speech model, so a reading waiting for it starts, and reports the
failure with its retry, which then downloads the aligner model alone, since the speech model is skipped as in place. The
questions before a download state the size of the aligner model in the download and on disk where it is not on the
machine. The page gets a row for the aligner model under "Speech model", "Word timing (english-mfa-v3.1.0)", with its
sizes and whether it is on the machine or downloading, and a "Download" button that is enabled only where a speech model
is in place, the aligner model is not, and no download runs. It has no "Remove" button. Removing a speech model that
leaves no speech model in place moves the aligner model folder aside in the same attempt, and the question of the removal
says so and counts its space.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSegments.kt`: `HeardWord.text`
- `src/main/kotlin/com/ritenoar/aloudrite/WordAlignment.kt` (new): `AlignedInterval`, `WordAligner`, the parts of the words
  to align, `PieceAlignment`, `wordStartsOf`, and the pure choice of the word from the starts
- `src/main/kotlin/com/ritenoar/aloudrite/ForcedWordAligner.kt` (new): the `WordAligner` of sherpa-onnx
- `src/main/kotlin/com/ritenoar/aloudrite/AlignerModel.kt` (new): `ALIGNER_MODEL`, its folder and its check of being in place
- `src/main/kotlin/com/ritenoar/aloudrite/HeardWordChoice.kt` (new): `HeardWordChoice`, `WORD_DELAY_MILLISECONDS` and the
  reading of the system property
- `src/main/kotlin/com/ritenoar/aloudrite/PieceAligning.kt` (new): the aligner at play time
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: the samples of a piece, the alignments of a reading, the
  choice in place of the estimate, the new collaborator
- `src/main/kotlin/com/ritenoar/aloudrite/InstallablePackage.kt` (new), `SpeechModel.kt`, `ModelInstaller.kt`,
  `ModelDownloadOffer.kt`, `ModelDownloadService.kt`, `PackageDownload.kt`, `PackageUnpacking.kt`: the installable package
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadMessages.kt`: the aligner model in the questions
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechModelManagement.kt`, `ReadAloudAction.kt`, `ReadingSpeedConfigurable.kt`:
  the download with a speech model, on its own, and the removal
- `build.gradle.kts`: the Gradle property of the delay for `runIde`
- `src/test/kotlin/com/ritenoar/aloudrite/WordAlignmentTest.kt`, `HeardWordChoiceTest.kt`, `PieceAligningTest.kt`,
  `AlignerModelTest.kt` (new), `ReadingSegmentsTest.kt`, `HeardWordEstimateTest.kt`, `HeardReadingServiceTest.kt`,
  `SpeechStandIns.kt`, `ModelInstallerTest.kt`, `ModelDownloadServiceTest.kt`, `ModelDownloadFlowTest.kt`,
  `ModelDownloadMessagesTest.kt`, `SpeechModelManagementTest.kt`, `ReadingSpeedConfigurableTest.kt`, `ReadAloudActionTest.kt`
- `src/test/kotlin/com/ritenoar/aloudrite/TrialSentences.kt` (new), `RealAlignerTest.kt` (new), `HighlightFollowing.kt`,
  `HighlightFollowingTest.kt`, `HighlightFollowingMeasurement.kt`, `FirstAudioMeasurement.kt`
- `README.md`, `THIRD-PARTY.md`
- `changes/0028-time-the-word-by-aligning-the-audio/record.md`

## Tasks

### Phase 1: what the measurement needs

- [x] In `ReadingSegmentsTest.kt`, test that each `HeardWord` has as `text` the characters of the word its piece holds,
  also for a word that the limit of a piece cuts and for words that stand for a block, then add `text` to `HeardWord` and
  set it in `ReadingSegments.kt` from the same substring the weight is taken of. Update the other constructions of
  `HeardWord` in the tests (R1)
- [x] In `WordAlignmentTest.kt`, test the parts of the words to align: split at white space, stripped of punctuation at
  both ends and not within, lowercased, a word of punctuation only with no parts, a word that stands for a block with
  several, and the text given to the aligner as the parts joined by single spaces. Then add them to `WordAlignment.kt`,
  with the categories of punctuation taken from `ReadingSegments.kt` rather than repeated (R1)
- [x] In `WordAlignmentTest.kt`, test `wordStartsOf`: the starts in samples of each word's first part, blank intervals
  skipped, null for a word without parts, no alignment where the intervals with text are fewer or more than the parts and
  where a start is before the one before. Then add `AlignedInterval`, `PieceAlignment` and `wordStartsOf` (R1, R3)
- [x] In `WordAlignmentTest.kt`, test the choice of the word from the starts, a position and a delay in samples: the
  first word with a start before the first start plus the delay, the last word whose start plus the delay is at or
  before the position, the word before through a gap between two words, words without a start never chosen, and a delay
  of 0. Then add the function (R1, R2, R4)
- [x] Add `WordAligner` to `WordAlignment.kt` and `ForcedWordAligner.kt`, which creates the `ForcedAligner` from a folder,
  aligns one piece with a stream it releases in a `finally`, returns the intervals of the tier `words`, fails with the
  names of the tiers where there is none, and releases the aligner at `close` (R1)
- [x] In `AlignerModelTest.kt`, test that the aligner model is in place only with all six files, and its folder in a
  models folder. Then add `AlignerModel.kt` with `ALIGNER_MODEL` and the facts of the approach. Its download comes in
  phase 2 (R5)
- [x] Move the 11 sentences of the trial from the companion of `HighlightFollowingMeasurement.kt` to
  `TrialSentences.kt` as `TRIAL_SENTENCES`, since the test with the real aligner is their second use (R7)
- [x] Add `RealAlignerTest.kt`, skipped with `assumeTrue` where the fp32 model or the aligner model is not in its
  folder. With a `KokoroSpeechEngine` of the fp32 model and Heart, it makes each sentence of the trial at 1.0× and at
  1.5×, aligns it with a `ForcedWordAligner` and the parts of `ReadingSegments` for the sentence, and asserts that the
  intervals with text are one per part, in the order of the text, each with its start at or before its end and both
  within the audio, and that `wordStartsOf` gives an alignment. Put the aligner model in place in the sandbox by hand
  from its address, checking its SHA-256, and run it with the model in place; a skipped run is no pass (R7)
- [x] In `HighlightFollowingTest.kt`, test how a piece is followed by the alignment: `followPiece` takes the starts of the
  piece, or none, and the delays, and counts at each moment the matching of the estimate and of the alignment at each
  delay, with a piece without starts counting by the estimate for the alignment too and saying so. Test that the table
  gives per set and speed the share by the estimate and the shares by the alignment with no delay and with the delay side
  by side, names the pieces that counted by the estimate, and gives the share by the estimate alone with the line that the
  aligner model is missing where it was not given. Then change `HighlightFollowing.kt`, choosing the word by the
  function of `WordAlignment.kt` (R6, R11)
- [x] Change `HighlightFollowingMeasurement.kt`: where the aligner model is in place, a `ForcedWordAligner` aligns the
  engine's audio of each piece with its parts before the piece is followed, a failure or no alignment counting the piece
  by the estimate with the reason; where it is not, the measurement runs as before and says the aligner model is missing.
  The delays are 0 and `WORD_DELAY_MILLISECONDS`, so add `HeardWordChoice.kt` with only that constant at this point.
  Update the section of the measurement in `README.md` under "Testing" for the alignment and the aligner model (R11)
- [x] Run the measurement with the command of `README.md` on Linux x64 with Heart and the fp32 model and the aligner model
  in place, and log in `record.md` the shares of each set and speed by the estimate, by the alignment and by the alignment
  70 ms later, and the pieces that counted by the estimate (R12)
- [x] Stop and ask the human whether to go on, logging the question in `record.md`. Phase 2 starts only after the human's
  word is in the record, as the spec's decision "Measured before the rest is built" asks (R13)

### Phase 2: the alignment at play time

- [x] In `HeardWordChoiceTest.kt`, test `HeardWordChoice` with plain values: the word by the alignment in a piece that has
  one, by the estimate in a piece that has none and in a silence, a switch to the alignment when it arrives during the
  piece, the alignments dropped by `forget`, and the delay from the milliseconds and the sample rate. Then add the class
  and the reading of `aloudrite.word.delay.milliseconds`, which falls back to the constant where the property is missing
  or not a whole number from 0 (R1 to R4)
- [x] Add to the task `runIde` of `build.gradle.kts` the system property `aloudrite.word.delay.milliseconds` from the
  Gradle property `wordDelayMilliseconds` where given (R2)
- [x] In `PieceAligningTest.kt`, with a stand-in `WordAligner` and the work run where the test says: no alignment and no
  aligner made where the aligner model is not in place, one aligner made for several pieces, the aligner released when
  the model is no longer in place, a failure of creating or aligning logged and given as nothing, and the release at
  dispose. Then add `PieceAligning.kt` (R3, R5)
- [x] In `SpeechStandIns.kt`, add a stand-in aligner whose answer, failure and moment of running the test sets, kept as
  the works are, and hand the service its `PieceAligning`. In `HeardReadingServiceTest.kt`, test with the stand-ins of
  unit 0006: the word by the alignment once it is ready, by the estimate before it and for a piece whose alignment failed
  or gave no start for each word, a failure logged and the reading going on to its end with its highlights, an alignment
  of the making before a change of the speed not used, the alignments kept over a pause and a resume, and the first sound
  not waiting for any alignment. Then change `SpeechService.kt` as the approach says, with `HeardWordChoice` in place of
  `HeardWordEstimate` in `Reading` (R1 to R4, R6)
- [x] Extend `FirstAudioMeasurement.kt` with a run through `SpeechService` with the real engine of the fp32 model, a
  stand-in player and the real aligner, against the same run with an aligner model that is never in place, five times
  each, from the call of `read` to the first write, and write both medians to its table. Run it with both models in
  place and log in `record.md` that the difference is no more than 50 ms, or what it is (R8)
- [x] Commit the refactoring of the installable package on its own: add `InstallablePackage`, implement it by
  `SpeechModel`, and make `ModelInstaller`, `ModelDownloadOffer`, `downloadPackage` and `unpackPackage` take it, with the
  progress naming it, and no change of behaviour, the tests passing as they are but for their types (R5)
- [x] In `ModelInstallerTest.kt`, test installing the aligner model with the stand-in package server of the tests, and two
  packages one after the other in one attempt. In `ModelDownloadServiceTest.kt` and `ModelDownloadFlowTest.kt`, test the
  download of a speech model with the aligner model where it is missing and without it where it is in place, a failure of
  the aligner model after the speech model that still runs what follows the speech model and reports with a retry that
  downloads the aligner model alone, and the download of the aligner model alone. Then change `ModelDownloadService.kt`
  and the callers (R5)
- [x] In `ModelDownloadMessagesTest.kt`, test that the three questions before a download state the aligner model's size
  in the download and on disk where it is not on the machine and not where it is, and the question of the removal of the
  last speech model with the aligner model. Then change `ModelDownloadMessages.kt` and its callers in
  `SpeechModelManagement.kt` and `ReadAloudAction.kt` (R5)
- [x] In `SpeechModelManagementTest.kt`, test the download of the aligner model on its own and the removal of the last
  speech model taking the aligner model with it, and of one that leaves another in place not taking it. Then change
  `SpeechModelManagement.kt` (R5)
- [ ] In `ReadingSpeedConfigurableTest.kt`, test the row of the aligner model: its comment of sizes and place, and its
  "Download" enabled only where a speech model is in place, the aligner model is not and no download runs. Then add it to
  `ReadingSpeedConfigurable.kt`, and look at the page in the sandbox IDE as `AGENTS.md` says, capturing only its window (R5)
- [x] In `README.md`, describe how the word is now found and what is shown while a piece has no alignment, in the
  paragraph of the highlights, without changing what it says of the default of the word highlight; add the aligner model
  with its size, its download with a speech model and on its own, its folder, its removal and putting it in place by
  hand to "The speech model"; and add `RealAlignerTest` beside the other tests with the real engine. In `THIRD-PARTY.md`,
  add the aligner model under "Downloaded at the request of the user" with its license, CC BY 4.0, and its attribution (R9)
- [ ] Ask the developer for the check of R10 on Linux x64 and on macOS on Apple Silicon, at 1.0× and 1.5× with the word
  highlight checked, with `-PwordDelayMilliseconds` to set the delay by ear, and log in `record.md` the time set and, for
  each platform and speed, whether the highlight keeps up with the voice and whether it still skips words (R2, R10)
- [ ] Set `WORD_DELAY_MILLISECONDS` to the time set by ear, run the measurement again on Linux x64 with Heart and the fp32
  model, and log its shares and the pieces that counted by the estimate in `record.md` (R2, R12)
- [x] Apply the findings of the first round of the review that were accepted as correct, S1, S4, P1, C1 and C2, each with
  its test: the question of a download or a removal made under the lock, the real test bounded by the audio, the active
  package published at each switch, the aligner's result validated against the parts and the audio, and one production
  path for the text given to the aligner
- [ ] Once the human has decided the blocking findings of the review, log in `record.md` the human's decision on keeping
  the unit and, where it is kept, on whether "Highlight the word being read" should start checked (R13)

## Verification

- [x] `./gradlew test` passes, with `RealAlignerTest` and the tests with the real engine run with both models in place,
  not skipped
- [x] `./gradlew buildPlugin` passes, with the archive within its limit
- [x] `npm run check-changes` passes after the commits

## Commit Points

- [x] `feature-0028 Write the plan`: `plan.md` and the log entry in `record.md`
- [x] `feature-0028 Align the words of a piece`: the text of `HeardWord`, `WordAlignment.kt`, `ForcedWordAligner.kt`,
  `AlignerModel.kt`, their tests, `TrialSentences.kt` and `RealAlignerTest.kt`
- [x] `feature-0028 Measure the word by the alignment beside the estimate`: the measurement, its logic and tests, and
  `README.md`
- [x] `feature-0028 Record the shares of the alignment`: the log entries of the run and of the question to the human
- [x] `feature-0028 Show the word by the alignment of its piece`: `HeardWordChoice`, `PieceAligning`, `SpeechService`,
  `build.gradle.kts` and their tests
- [x] `feature-0028 Measure the first audio with the alignment`: `FirstAudioMeasurement.kt` and the log entry
- [x] `feature-0028 Install any package of the plugin`: the refactoring of the installable package alone
- [x] `feature-0028 Download the aligner model with a speech model`: the download, the questions, the page, the removal
  and their tests
- [x] `feature-0028 Describe the alignment`: `README.md` and `THIRD-PARTY.md`
- [x] `feature-0028 Apply the review of the first round`: the corrections of S1, S4, P1, C1 and C2 and the log entry
- [ ] `feature-0028 Record the check and the shares with the delay set by ear`: the constant and the log entries
