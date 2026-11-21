# Measure how well the word highlight follows the voice

## Approach

The measurement is a test class beside the other measurements, `HighlightFollowingMeasurement`, run on demand with
`ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING` set. It measures two sets: every piece of words of
`samples/markdown-features.md`, and the 11 sentences of the trial of unit 0028, each read as a file of its own. Each set
is read in Heart with the fp32 model at 1.0× and at 1.5×. The measurement has three sides, and only the first and the
last touch anything outside plain values:

1. **The plugin's side**, from the plugin's own code. The pieces of a file are `ReadingSegments(placedWhatToRead(text)
   .placedParts).segments`, as `ReadingTimeMeasurement` cuts them for a reading from the start. A `KokoroSpeechEngine`
   on the fp32 model makes the audio of each piece of words with Heart. The measurement keeps the samples of each call
   of the engine's callback as one array, and the run fails, with the piece and both numbers named, where a piece
   gives another number of arrays than it has rows. Those arrays are then the engine's audio for each row, lying end to
   end. The rows of phoneme tokens that the engine hands sherpa-onnx are made again by a `PhonemeRows` built as
   `KokoroSpeechEngine` builds it, from `tokens.txt` of the model folder and `EspeakPhonemes::sentencesOf`, after the
   engine has pointed eSpeak NG at the data of the model, with the language of Heart. The engine does not give its
   rows out, and the unit changes nothing outside the tests (R12), so the two lines that build the `PhonemeRows` are
   repeated in the measurement.
2. **The word shown**, from the plugin's estimate (R2). One `ReadingTime` and one `HeardWordEstimate` serve the whole
   reading of a file at a speed, as one `Reading` of `SpeechService` holds them. `ReadingTime` is told every piece's
   samples with `audioMade` and `segmentMade` before any moment is heard, and at each moment the measurement calls
   `readingTime.heard(segment, samplesIntoIt)` and then `estimate.heard(segment, samplesIntoIt,
   readingTime.shareOfTheSegmentHeard())`, which is what `Reading.heard` does. The estimate is called at every moment of
   a piece, also those that are not counted, because it never moves back within a piece and so depends on the moments
   before. The word shown is `estimate.places.word`, a range of the file. A piece being played whose audio is not all
   made yet gets a share from an estimated length in the plugin; the measurement has every piece made in full, which
   with 30 seconds of audio made ahead is the case for nearly every piece of a reading.
3. **The word heard**, from the durations of `model.onnx` (R3). An `OrtSession` of ONNX Runtime for Java 1.22.0, a
   dependency of the tests only, opens `model.onnx` of the fp32 model folder. For each row it is given the token ids,
   with the padding id 0 at both ends as sherpa-onnx gives them, the style of Heart for the row's length from
   `voices.bin`, and the speed `speed.forTheEngine` that the plugin hands sherpa-onnx. It gives the audio and the
   durations, `onnx::Shape_3411`, one per token with the padding at both ends, in frames of 600 samples. The engine's
   audio for the row is then found within that audio, at the offset where the two differ least, and the times of the
   durations are moved back by that offset.

Both native libraries ship a `libonnxruntime.so`, sherpa-onnx in its native jar and ONNX Runtime for Java in its own.
The measurement holds the engine and the session open in one test JVM. Where the session cannot be opened or run after
the engine is made, or the engine fails after the session is opened, the implementer stops and logs the error in
`record.md` for the planner, since the plan then does not hold.

The pure logic lives in a test source file of its own, `HighlightFollowing.kt`, and takes and returns plain values:

* **Phoneme words**: in a row, a phoneme word is a maximal run of tokens that are neither the space token nor
  punctuation, by the same Unicode categories of punctuation as `ReadingSegments` uses for the weight of a word. The
  stress and length marks of the phonemes are modifier letters and belong to the word. The phoneme words of a piece
  are those of its rows in order
* **Bare phonemes**: the tokens of a phoneme word, or of the rows of a word made into phonemes on its own, without the
  space token, punctuation and the stress marks `ˈ` and `ˌ`, which eSpeak NG moves when it joins words
* **The words' texts**: the texts of a piece's words are the runs of characters that are not white space in the
  piece's text, in order, which is how `ReadingSegments` finds them. Where the runs are not as many as the piece's
  words, as for words that stand for a whole block, the piece is left out with "<n> runs of text for <m> words"
* **Groups** (R5): the words of the plugin that weigh 0, which are punctuation only and which eSpeak NG says nothing
  for, are left out, since the estimate never shows them where any word of the piece weighs more. The measurement
  makes each remaining word into phonemes on its own, with the same `PhonemeRows` and language, and takes their bare
  phonemes. The pairing is the alignment in order of the phoneme words with the words that costs least, by dynamic
  programming, where a step is either one phoneme word with a run of one or more consecutive words, costing the edit
  distance between the phoneme word's bare phonemes and those of the run joined, or one word with a run of two or more
  consecutive phoneme words, costing the edit distance between the word's bare phonemes and those of the run joined.
  Of two alignments that cost the same, the one with fewer steps of more than one word or phoneme word is taken. Where
  the alignment taken holds a step of one word with several phoneme words, the piece is left out with "eSpeak NG reads
  <word> as <n> phoneme words", naming the first such word. Otherwise each phoneme word has its group of words, and
  where the two are as many every group is one word
* **Token ids**: the id of each token code point is the number after the last space of its line of `tokens.txt`, as
  `tokenCodePointsOf` reads the lines. The parse is repeated in the tests rather than shared, since sharing it would
  change `src/main` (R12)
* **The style**: `voices.bin` holds float32 values, little-endian, as 510 rows of 256 per speaker in the order of the
  speaker numbers. The style of a row of `n` tokens, without the padding, is row `n - 1` of the speaker, held within 0
  and 509, as the Kokoro package takes `pack[len(phonemes) - 1]`. The run fails where the file's size is not a whole
  number of speakers of 510 by 256 values
* **Placement** (R3, R6): the offset of the engine's audio of a row within the model's audio of the row is the one
  where the mean absolute difference of the samples is least. The search compares every 16th sample at every offset
  from 0 to the model's length less the engine's, and then every sample at the offsets within 32 of the best of those.
  The difference at the best offset, as a share of the level of the model's audio there, the mean of its absolute
  samples over the same samples, is the row's difference. A row whose engine audio is longer than the model's, whose
  level there is 0, or whose difference is above 20% cannot be placed, and the piece is left out with the row's number
  and the reason, as "row 2: differs by 31.4% at the best place", "row 1: the engine's audio is longer than the model's"
  or "row 1: the model's audio is silent where it is placed"
* **Spans**: a row's phoneme words lie in its model audio by the durations, the token `n` of the row having the frame
  `n + 1`. In the piece's audio, a span begins at the row's start in the engine's audio plus its start in the model's
  audio less the row's offset, and is held within the engine's audio of the row, so that a word, or the part of it,
  that falls in the silence cut off by the engine is in no span
* **Moments** (R4): a piece's moments are every 10 ms of the engine's audio, 240 samples, from 0 to before its end.
  The group heard at a moment is that of the span that holds it, and a moment that no span holds is not counted
* **Shares** (R8): a counted moment matches where the word shown is any word of the group heard. A piece's share is its
  matching moments over its counted moments, and the share of a set at a speed is the sum of those over all counted
  pieces, over the sum of their counted moments, which is weighting each piece by its time. The share in groups is the
  counted moments whose group holds more than one word, over all counted moments. A counted piece with no counted
  moments, as one whose words all weigh 0, shows its share as `-`
* **The table** (R8): for each set a heading with its name, and for each speed of it a line with its share, its share
  in groups, the seconds of words it counted, and the numbers of pieces counted and left out. Then a line per piece
  with its label, its share or `left out:` and the reason, and its text on one line and cut after 50 characters as
  `EngineSilenceMeasurement` shows it. The label is the segment number for the sample, and `<sentence>.<segment>` for
  the trial's sentences, the sentences numbered from 1

## Files

- `src/test/kotlin/com/ritenoar/aloudrite/HighlightFollowing.kt`: the pure logic of the measurement
- `src/test/kotlin/com/ritenoar/aloudrite/HighlightFollowingTest.kt`: its unit tests
- `src/test/kotlin/com/ritenoar/aloudrite/HighlightFollowingMeasurement.kt`: the measurement, which runs the engine and
  `model.onnx` and writes `build/highlight-following.txt`
- `build.gradle.kts`: ONNX Runtime for Java as a dependency of the tests
- `scripts/kokoro-durations.py` and `scripts/test_kokoro_durations.py`: removed
- `.gitignore`: the line `__pycache__/` and the blank line before it removed, as no Python is left in the project
- `README.md`: the measurement under "Testing", with what it needs, and the paragraphs on the Python environment, the
  weights and the script's tests removed
- `changes/0032-measure-how-well-the-word-highlight-follows-the-voice/record.md`: the log, and the shares of the run

Nothing under `src/main/` changes (R12).

## Tasks

- [x] In `HighlightFollowingTest.kt`, write the tests of the phoneme words, then add to `HighlightFollowing.kt` the
  function that gives the phoneme words of a piece's rows as ranges of token indices per row. Cover: words split by the
  space token; punctuation tokens, as `,`, `.`, `—` and `“`, are in no word, also where they touch one without a space;
  the stress mark `ˈ` and the length mark `ː` are in the word; the phoneme words of several rows follow one another
  (R5, R9)
- [x] Write the tests of the moments and of the word heard at a moment, then add the functions: the moments every 240
  samples before the length of the engine's audio, and the span heard at a moment, or none. Cover: a moment on the first
  sample of a span holds it and one on the sample after its last does not; a length of 480 samples gives the moments 0
  and 240, and one of 481 gives 0, 240 and 480 (R4, R9)
- [x] Write the tests of the share of a piece and of a speed, then add the function that follows one piece: it takes the
  segment number, the words heard, the spans, the engine's sample count, and the reading's `ReadingTime` and
  `HeardWordEstimate`, calls both at every moment as `Reading.heard` does, and returns the counted and the matching
  moments. Add the sum over pieces of a speed and the table. Cover, with `ReadingSegments` of a short Markdown text and
  made-up spans, without a model: spans that agree with the letters of the words give a share of 1; spans that put a
  long word first, where the estimate moves on too early, give the share that the moments make by hand; moments in
  pauses are not counted; the share of a speed weights two pieces by their counted moments and not by the piece; a
  piece with no counted moments shows `-` (R2, R4, R8, R9)
- [ ] Remove from `HighlightFollowing.kt` the check of the lengths, `LENGTH_TOLERANCE`, `lengthByTheDurations` and
  `lengthDisagreement`, with their four tests in `HighlightFollowingTest.kt`, since requirement 6 places the engine's
  audio in place of comparing the lengths (R6)
- [ ] Write the tests of the bare phonemes and of the words' texts, then add the functions: the bare phonemes of a
  phoneme word of a row and of the rows of a word; and the texts of a piece's words from its text, or the reason they
  cannot be found. Cover: the stress marks `ˈ` and `ˌ`, the space token and punctuation are left out and the length
  mark `ː` is kept; the rows of a word are joined; a text of two words with a run of spaces and a line break between
  them gives both; a word that a piece's limit cuts gives the part in the piece; three runs for two words give "3 runs
  of text for 2 words" (R5, R9)
- [ ] Write the tests of the groups, then replace `pairedWords` and `Pairing` in `HighlightFollowing.kt` by the
  function that groups the words of a piece by the phoneme words, as described under the approach, taking the phoneme
  words' bare phonemes and the words with their bare phonemes, and giving one group of `HeardWord`s per phoneme word
  or the reason. Replace the four tests of the pairing. Cover: as many phoneme words as words, with phonemes that
  differ a little, give a group of one word each in order; words that weigh 0 are in no group; one phoneme word whose
  bare phonemes are those of two words joined, as `ʌvðə` for `ʌv` and `ðə`, gives a group of those two; two joins in
  one piece give two groups; a word whose bare phonemes are those of two phoneme words joined, as a number said as
  two words, gives "eSpeak NG reads <word> as 2 phoneme words"; and where as many phoneme words as words can also be
  aligned at the same cost by one join and one split, a group of one word each is taken (R5, R9)
- [ ] Write the tests of the token ids and of the style, then add the functions: the ids of the code points of the
  lines of `tokens.txt`, and the ids of a row with 0 at both ends; and the style of a speaker for a row of `n` tokens
  from the float values of `voices.bin`, with the check of the file's size. Cover: the line of the space token, whose
  token is itself a space, gives the id after the last space; a row `ab` gives `0, id(a), id(b), 0`; the style of a row
  of 1 token is row 0 of the speaker, of 3 tokens row 2, and of 600 tokens row 509; the style of speaker 1 begins 510
  by 256 values after that of speaker 0; a size that is not a whole number of speakers fails with the size named (R3,
  R9)
- [ ] Write the tests of the placement, then add the function that gives the offset of the engine's audio of a row
  within the model's audio and its difference, or the reason it cannot be placed, as described under the approach, and
  the check of the difference against 20%. Cover, with made-up signals: an engine audio cut from a known offset of a
  model audio of varied samples is placed at that offset with a difference of 0; the same with a small added noise is
  placed at that offset with a difference under 20%; an offset that is not a multiple of 16 is found exactly; an
  engine audio unrelated to the model's has a difference above 20% and gives "row <n>: differs by <p>% at the best
  place"; an engine audio longer than the model's and a model audio of zeros give their reasons (R3, R6, R9)
- [ ] Write the tests of the spans of rows placed in the engine's audio, then change `spansOf` to take, for each row,
  its durations, its start in the engine's audio of the piece, its length there and its offset, and give the spans as
  described under the approach. Adapt its three tests. Cover: a padding token's time and a space's time hold no word;
  a row placed at offset 0 gives the spans of its durations; a row at an offset of 2 frames gives every span 2 frames
  earlier, and the word in the first 2 frames is cut to what remains; a word wholly in the cut silence is an empty span
  that holds no moment; the second row's spans begin at its start in the engine's audio and not at the end of the
  first row's model audio (R3, R4, R9)
- [ ] Write the tests of following a piece by groups, then change `followPiece` to take one group of `HeardWord`s per
  span, to count a moment as matching where the word shown is the place of any word of the group heard, and to count
  the moments in groups of more than one word. Add the share in groups of a speed, the sets and the labels to the
  table, as described under the approach. Adapt the tests of following a piece and of the table. Cover: a moment in a
  group of two matches where the estimate shows either word and not where it shows a third; the share in groups is the
  moments in groups of more than one word over the counted moments; the table gives each set's heading, each speed's
  share, share in groups, seconds counted and numbers of pieces counted and left out, and a label `3.2` for a sentence
  of the trial (R5, R7, R8, R9)
- [ ] Add `testImplementation("com.microsoft.onnxruntime:onnxruntime:1.22.0")` to `build.gradle.kts`, beside the
  other dependency of the tests, and nothing else to the build (R3, R10, R12)
- [ ] Rewrite `HighlightFollowingMeasurement.kt`. It is skipped with `assumeTrue` where
  `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING` is not set, where the platform has no speech, and where the fp32 model,
  `SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }`, is not in place, each with a message naming
  what is missing. It takes Heart as `voiceNamed("af_heart")`, the speeds `parsedSpeed("1.0")` and `parsedSpeed("1.5")`,
  and the threads of `engineThreadCount` for the engine and as the intra-op threads of the session. It holds the 11
  sentences of the trial of unit 0028 as they stand in the spec. It makes one engine and one `OrtSession` on
  `model.onnx` of the model folder, and fails, naming what it found, where the session's inputs are not `tokens`,
  `style` and `speed` or its outputs do not include `onnx::Shape_3411`. For each set, each file of it and each speed
  it runs every piece of words through the engine, keeping one array per callback, and through the session row by row
  with the `int64` ids as `[1, n + 2]`, the style as `[1, 256]` and the speed as `[1]`, and fails, naming the piece
  and the row, where a row's durations are not its tokens and two or do not add up to its model audio in frames of 600
  samples. It leaves a piece out with the reason where it gives no rows, its words' texts cannot be found, it cannot be
  grouped or a row cannot be placed, in that order, and otherwise follows it with the one `ReadingTime` and
  `HeardWordEstimate` of its file and speed. It writes the table, headed by the model file, the voice, the operating
  system and the architecture, to `build/highlight-following.txt`, prints it, and asserts that each piece of words of
  each file is counted or left out at each speed. Its comment says what it measures, for which unit, that the durations
  come from `model.onnx` by ONNX Runtime for Java, and the command, as the other measurements' comments do (R1, R2, R3,
  R5, R6, R7, R8)
- [ ] Remove `scripts/kokoro-durations.py` and `scripts/test_kokoro_durations.py`, and the line `__pycache__/` of
  `.gitignore` with the blank line before it. Keep the lines `Kokoro-82M/` and `kokoro-env/`, which were there before
  this unit for the listening of unit 0006 (R10, R12)
- [ ] Rewrite the section of `HighlightFollowingMeasurement` in `README.md` under "Testing", in the form of the others:
  that it runs only with `ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING` set, on a platform with speech and with the fp32 model
  in its folder, and is reported as skipped otherwise; what it measures, for both sets at both speeds; that the word
  heard is taken from the durations that `model.onnx` gives by ONNX Runtime for Java, moved back by where the engine's
  audio starts within the model's; that joined words count as a group, and which pieces are left out and named with the
  reason; that it writes `build/highlight-following.txt`; and its command. Remove the paragraphs on `kokoro-env`, on
  `Kokoro-82M` and on the tests of the script, with their command. It needs nothing but the model and what Gradle
  fetches, so it says nothing of the host or the sandbox (R10)
- [ ] Run the measurement with the command of `README.md` with the fp32 model in place, in the sandbox or on the host,
  and log in `record.md` the share and the share in groups of both sets at 1.0× and at 1.5×, the pieces counted and
  left out at each, with the reasons of those left out grouped by kind, and beside them the 54% and 52% of the trial of
  unit 0028, with the note that the trial made its phonemes a word at a time and counted with its own copy of the
  estimate. Where every piece of a set is left out, or more than a quarter of its pieces, log the reasons and stop for
  the planner rather than changing the rules of the plan (R11)
- [ ] Before the last commit of the implementation, check that `git diff --stat master -- src/main` is empty and that
  `git diff master -- build.gradle.kts` adds the one dependency of the tests only, so that the plugin reads, sounds and
  highlights as before (R12)

## Verification

- [ ] `./gradlew test` passes, with `HighlightFollowingTest` run and `HighlightFollowingMeasurement` reported as skipped
  where its variable is not set
- [ ] `npm run check-changes` passes after the commits

## Commit Points

- [x] `feature-0032 Write the plan`: `plan.md` and the log entry in `record.md`
- [x] `feature-0032 Add the logic of the highlight measurement`: the phoneme words, the pairing one to one, the spans,
  the lengths, the moments and the share, with their tests
- [x] `feature-0032 Add the highlight measurement`: the script, the measurement class, `README.md` and the log entry of
  the implementation
- [x] `feature-0032 Keep the warning of the Kokoro package out of the durations`: the script
- [x] `feature-0032 Revise the plan to the corrected spec`: `plan.md` and the log entry in `record.md`
- [ ] `feature-0032 Group joined words and place the engine's audio`: the tasks from the removal of the check of the
  lengths through following a piece by groups, `HighlightFollowing.kt` and `HighlightFollowingTest.kt`
- [ ] `feature-0032 Take the durations from model.onnx by ONNX Runtime for Java`: `build.gradle.kts`, the measurement,
  the removal of the script, its tests and the line of `.gitignore`, `README.md` and the log entry of the implementation
- [ ] `feature-0032 Record the shares of the run`: the log entry of the run
