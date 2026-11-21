# Measure how well the word highlight follows the voice

## Approach

The measurement is a test class beside the other measurements, `HighlightFollowingMeasurement`, run on demand with
`ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING` set. It has three sides, and only the first and the last touch anything
outside plain values:

1. **The plugin's side**, from the plugin's own code. The pieces are `ReadingSegments(placedWhatToRead(text)
   .placedParts).segments` for `samples/markdown-features.md`, as `ReadingTimeMeasurement` cuts them for a reading from
   the start. A `KokoroSpeechEngine` on the fp32 model makes the audio of each piece of words with Heart, and the
   measurement counts its samples. The rows of phoneme tokens that the engine hands sherpa-onnx are made again by a
   `PhonemeRows` built as `KokoroSpeechEngine` builds it, from `tokens.txt` of the model folder and
   `EspeakPhonemes::sentencesOf`, after the engine has pointed eSpeak NG at the data of the model, with the language
   of Heart. The engine does not give its rows out, and the unit changes nothing outside the tests (R10), so the two
   lines that build the `PhonemeRows` are repeated in the measurement.
2. **The word shown**, from the plugin's estimate (R2). One `ReadingTime` and one `HeardWordEstimate` serve the whole
   reading at a speed, as one `Reading` of `SpeechService` holds them. `ReadingTime` is told every piece's samples with
   `audioMade` and `segmentMade` before any moment is heard, and at each moment the measurement calls
   `readingTime.heard(segment, samplesIntoIt)` and then `estimate.heard(segment, samplesIntoIt,
   readingTime.shareOfTheSegmentHeard())`, which is what `Reading.heard` does. The estimate is called at every moment of
   a piece, also those that are not counted, because it never moves back within a piece and so depends on the moments
   before. The word shown is `estimate.places.word`, a range of the file. A piece being played whose audio is not all
   made yet gets a share from an estimated length in the plugin; the measurement has every piece made in full, which
   with 30 seconds of audio made ahead is the case for nearly every piece of a reading.
3. **The word heard**, from the durations of the Kokoro Python package (R3). A Python script,
   `scripts/kokoro-durations.py`, loads `KModel` of `kokoro` with `config.json` and `kokoro-v1_0.pth` of the weights
   folder, so that nothing is downloaded, and the voice pack `voices/<voice>.pt`. It reads lines of a speed, a tab and
   the phonemes of one row, and writes for each the durations `KModel` predicts, `pred_dur`, as integers separated by
   spaces, which are one per token with the padding token at both ends. The style is `pack[len(phonemes) - 1]`, as
   `KPipeline` of `kokoro` takes it. The measurement writes every row of both speeds to `build/highlight-following-rows.txt`,
   starts the script once with `kokoro-env/bin/python` and that file as its input, and reads one line per row. A line
   whose number of durations is not the number of tokens of the row plus two fails the run with the row named, since
   then the vocabularies of the two differ and nothing of the run can be trusted.

The pure logic lives in a test source file of its own, `HighlightFollowing.kt`, and takes and returns plain values:

* **Phoneme words**: in a row, a phoneme word is a maximal run of tokens that are neither the space token nor
  punctuation, by the same Unicode categories of punctuation as `ReadingSegments` uses for the weight of a word. The
  stress and length marks of the phonemes are modifier letters and belong to the word. The phoneme words of a piece
  are those of its rows in order
* **Pairing** (R5): the words of the plugin that weigh 0, which are punctuation only and which eSpeak NG says
  nothing for, are left out of the pairing, since the estimate never shows them where any word of the piece weighs
  more. The rest are paired in order one to one with the phoneme words where the two are as many, and otherwise the
  piece is left out with "<n> phoneme words for <m> words"
* **Spans**: a row's durations are frames of 600 samples at 24 kHz. The rows lie end to end, each beginning where the
  one before ends, its own padding tokens included, and a phoneme word runs from the first sample of its first token
  to the end of its last. The run fails where the engine's sample rate is not 24000
* **Lengths** (R5): the length by the durations is the sum of every row's frames times 600, and the piece is left out
  with both lengths in seconds and how far apart they are in percent where they differ by more than 2% of the length
  of the engine's audio. Two lengths of 0 agree. Should sherpa-onnx put anything between the rows, the lengths of a
  piece of several rows differ and it is left out, so the end to end rows of the spans cannot go unnoticed
* **Moments** (R4): a piece's moments are every 10 ms of the engine's audio, 240 samples, from 0 to before its end.
  The word heard at a moment is the phoneme word whose span holds it, and a moment that no span holds is not counted
* **Shares** (R6): a piece's share is its counted moments at which the word shown is the paired word of the word
  heard, over its counted moments. The share of a speed is the sum of those moments over all counted pieces, over the
  sum of their counted moments, which is weighting each piece by its time. A counted piece with no counted moments,
  as one whose words all weigh 0, shows its share as `-`
* **The table** (R6): for each speed a line with its share, the seconds of words it counted, and the numbers of pieces
  counted and left out, and then a line per piece with its number, its share or `left out:` and the reason, and its
  text on one line and cut after 50 characters as `EngineSilenceMeasurement` shows it

## Files

- `src/test/kotlin/com/ritenoar/aloudrite/HighlightFollowing.kt`: new, the pure logic of the measurement
- `src/test/kotlin/com/ritenoar/aloudrite/HighlightFollowingTest.kt`: new, its unit tests
- `src/test/kotlin/com/ritenoar/aloudrite/HighlightFollowingMeasurement.kt`: new, the measurement, which runs the
  engine and the script and writes `build/highlight-following.txt`
- `scripts/kokoro-durations.py`: new, the durations of the Kokoro Python package for rows of phonemes
- `scripts/test_kokoro_durations.py`: new, the tests of the script with a stand-in model, added for finding C1
- `README.md`: the measurement under "Testing", and what it needs
- `changes/0032-measure-how-well-the-word-highlight-follows-the-voice/record.md`: the log, and the shares of the run on
  the host

Nothing under `src/main/` changes (R10).

## Tasks

- [x] In `HighlightFollowingTest.kt`, write the tests of the phoneme words and of the pairing, then add to
  `HighlightFollowing.kt` the function that gives the phoneme words of a piece's rows as ranges of token indices per
  row, and the function that pairs them with the `HeardWord`s of a `ReadingSegment` or gives the reason it cannot.
  Cover: words split by the space token; punctuation tokens, as `,`, `.`, `—` and `“`, are in no word, also where they
  touch one without a space; the stress mark `ˈ` and the length mark `ː` are in the word; the phoneme words of several
  rows follow one another; words that weigh 0 are left out of the pairing; as many phoneme words as words pair in
  order; one more phoneme word than words, as for a number that eSpeak NG says as two words, and one fewer, give the
  reason with both numbers (R5, R7)
- [x] Write the tests of the spans, the lengths and the moments, then add the functions: the spans of the phoneme words
  in samples of the piece from the rows and their durations, with the padding token's duration first and last in each
  row and the rows end to end; the length by the durations; whether two lengths agree within 2% of the engine's length,
  and the reason with both lengths and the difference where they do not; the moments every 240 samples before the
  length of the engine's audio; and the phoneme word heard at a moment, or none. Cover: a padding token's time and a
  space's time hold no word; a moment on the first sample of a word holds it and one on the sample after its last does
  not; the second row's words begin after the whole of the first row, its closing padding included; lengths 2% apart
  agree and lengths just over 2% apart do not; two lengths of 0 agree; a length of 480 samples gives the moments 0 and
  240, and one of 481 gives 0, 240 and 480 (R3, R4, R5, R7)
- [x] Write the tests of the share of a piece and of a speed, then add the function that follows one piece: it takes the
  segment number, the segment's paired words, the spans, the engine's sample count, and the reading's `ReadingTime` and
  `HeardWordEstimate`, calls both at every moment as `Reading.heard` does, and returns the counted and the matching
  moments. Add the sum over pieces of a speed and the table. Cover, with `ReadingSegments` of a short Markdown text and
  made-up spans, without a model: spans that agree with the letters of the words give a share of 1; spans that put a
  long word first, where the estimate moves on too early, give the share that the moments make by hand; moments in
  pauses are not counted; the share of a speed weights two pieces by their counted moments and not by the piece; a
  piece with no counted moments shows `-`; the table names each left out piece with its reason, and gives each speed's
  share, seconds counted and numbers of pieces counted and left out (R2, R4, R6, R7)
- [x] Write `scripts/kokoro-durations.py`, taking the weights folder and the voice name as its two arguments, as
  described under the approach: `KModel(config=..., model=...)` in evaluation mode, the voice pack loaded with
  `torch.load(..., weights_only=True)`, the durations under `torch.no_grad()` from
  `model(phonemes, pack[len(phonemes) - 1], speed, return_output=True).pred_dur`, and one output line per input line,
  flushed (R3)
- [x] Write `HighlightFollowingMeasurement.kt`. It is skipped with `assumeTrue` where
  `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING` is not set, where the platform has no speech, where the fp32 model,
  `SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }`, is not in place, and where
  `kokoro-env/bin/python`, `Kokoro-82M/config.json`, `Kokoro-82M/kokoro-v1_0.pth` or `Kokoro-82M/voices/af_heart.pt`
  is missing, each with a message naming what is missing. It takes Heart as `voiceNamed("af_heart")`, the speeds
  `parsedSpeed("1.0")` and `parsedSpeed("1.5")`, and the threads of `engineThreadCount`. It makes one engine, the rows
  and the engine's sample count of every piece of words at both speeds, runs the script once with the error output
  passed through and fails where it ends with another code than 0, follows each piece that pairs and whose lengths
  agree with one `ReadingTime` and one `HeardWordEstimate` per speed, writes the table, headed by the model file, the
  voice, the operating system and the architecture, to `build/highlight-following.txt` and prints it, and asserts that
  each piece of words is counted or left out at each speed. Its comment says what it measures, for which unit, and the
  command, as the other measurements' comments do (R1, R2, R3, R5, R6)
- [x] In `README.md` under "Testing", after `FirstAudioMeasurement`, add `HighlightFollowingMeasurement` in the form of
  the others: what it measures, that it runs only with `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING` set and is reported as
  skipped where the fp32 model, the Python environment or the weights are missing, that it writes
  `build/highlight-following.txt`, and its command. Say how to set up what it needs: a Python environment
  `kokoro-env` in the working copy with `kokoro` 0.9.4 and PyTorch 2.14.0, as `python3 -m venv kokoro-env` and
  `kokoro-env/bin/pip install kokoro==0.9.4 torch==2.14.0`, and the weights of Kokoro v1.0 in `Kokoro-82M`, the
  `config.json`, `kokoro-v1_0.pth` and `voices/af_heart.pt` of the Hugging Face repository `hexgrad/Kokoro-82M`, with
  the address https://huggingface.co/hexgrad/Kokoro-82M, both ignored by git. Say that it is run on the host, since the
  sandbox reaches neither the Python Package Index nor Hugging Face (R8)
- [x] Pass `repo_id="hexgrad/Kokoro-82M"` to `KModel` in `scripts/kokoro-durations.py`, so that the package does not
  print its warning on that name to the standard output, which the measurement reads as a line of durations and so
  fails with one line more than rows. With a local `config.json` and weights the name downloads nothing (R3)
- [ ] Run the measurement on the developer's Linux x64 machine with the command of `README.md`, and log in `record.md`
  the share at 1.0× and at 1.5×, the pieces counted and left out at each with the reasons of those left out, and beside
  them the 54% and 52% of the trial of unit 0028 under sherpa-onnx 1.13.8, with the note that the trial used 11
  sentences that are not recorded and its own way of counting. The sandbox has neither the Python environment nor the
  weights, so this task is done in a session on the host or by the developer, and an implementer in the sandbox leaves
  it open and says so in its log entry (R9)
- [x] Add `scripts/test_kokoro_durations.py`, with the stand-in model and the command in `README.md`, so that the script's
  protocol, speed and style selection are tested without the weights (finding C1 of the review)
- [x] Before the last commit of the implementation, check that `git diff --stat master -- src/main` is empty, so that
  the plugin reads, sounds and highlights as before (R10)

## Verification

- [x] `./gradlew test` passes, with `HighlightFollowingTest` run and `HighlightFollowingMeasurement` reported as skipped
  where its variable is not set
- [ ] `npm run check-changes` passes after the commits

## Commit Points

- [x] `feature-0032 Write the plan`: `plan.md` and the log entry in `record.md`
- [x] `feature-0032 Add the logic of the highlight measurement`: the first three tasks, `HighlightFollowing.kt` and
  `HighlightFollowingTest.kt`
- [x] `feature-0032 Add the highlight measurement`: the script, the measurement class, `README.md` and the log entry of
  the implementation
- [x] `feature-0032 Keep the warning of the Kokoro package out of the durations`: the script and this task
- [ ] `feature-0032 Record the shares of the run on the host`: the log entry of the run on the host
