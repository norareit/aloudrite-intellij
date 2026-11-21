# piper-phonemize 1.6 and emoji read by their names

## Approach

Unit 0030 left two workarounds for faults of `piper-phonemize` 1.5.9, and 1.6.1 fixes both. This unit takes both out
and makes the rest of the plugin fit characters above U+FFFF that are now read. The work falls into four layers:

1. **The phonemes under 1.5.9, first.** Requirement 9 compares the phonemes of seven sentences under this unit with
   those under 1.5.9, so a test with the real binding that holds them as constants is written and run before anything
   moves (R9).
2. **The build.** `piper-phonemize` 1.6.1 for the Java binding and for both native libraries, all from the release.
   The script, the committed jar of the project's own Linux build, its checksum and the task that checks it go (R1, R2,
   R3).
3. **The text.** The words of the Markdown reach eSpeak NG with their characters above U+FFFF as they stand in the
   file: `withCharactersAboveFfffAsSpaces` and `PlacedWords.forTheEngine` go, and the places of the words and the
   aligner are made to fit a character that is now heard (R4, R7, R8, R13).
4. **The tests with the real binding and engine, the documents and the developer's checks** (R5, R6, R10 to R12, R14,
   R15).

What was found for this plan, on 2026-11-21, on the branch at `e725b7b`, in the sandbox (Debian 12, glibc 2.36):

* The release `v1.6.1` (commit `5f92f0b`) publishes `piper-phonemize-jvm-1.6.1.jar` (9,042,589 bytes, with 392 entries
  under `piper-phonemize/espeak-ng-data/` as 1.5.9 had), `piper-phonemize-native-lib-linux-x64-1.6.1.jar` (344,698
  bytes) and `piper-phonemize-native-lib-osx-aarch64-1.6.1.jar` (228,785 bytes), under the names and the layout of
  1.5.9, so the Ivy repository and the stripping task of `build.gradle.kts` take them unchanged. The Linux library,
  `piper-phonemize/native/linux-x64/libpiper-phonemize-jni.so`, needs no symbol version of glibc newer than
  `GLIBC_2.14`, by `objdump -T`
* `CMakeLists.txt` of `piper-phonemize` from `v1.5.9` to `v1.6.1` changes the version and adds a patch of two headers
  of eSpeak NG for MSVC. eSpeak NG is still the commit `ed530aa113046142eb5115cf2fc9157854d0ffe1` of the fork
* The forced aligner of unit 0028, with `english-mfa-v3.1.0`, was given the audio of "Before the sign, grinning face
  and after the sign." from the fp32 model, as 1.6.1 will make it for "Before the sign, 😀 and after the sign.":
    * With the words `before the sign 😀 and after the sign`, it gave no interval for 😀, seven in place of eight, so
      `wordStartsOf` finds no alignment and the whole piece falls back to the estimate
    * With `before the sign and after the sign`, it gave every word, but put "and" at 1.06 s, where it is heard at
      1.74 s, so the words after the emoji would be shown early
    * With a word that is not in its lexicon in place of 😀, such as `emoji`, which `lexicon.txt` does not hold, it gave
      that word from 1.02 s, "and" at 1.74 s and every word after it where it is heard, at 1.0× and at 1.5×. Two such
      words for "grinning face red heart", and four at the start of a sentence, were placed one after the other in the
      same way
    * With ❤ in the words, it gave no interval for ❤ either. That is the case now, before this unit, for every piece
      that holds ❤, and stays so, since how ❤ is read is a non-goal

How the parts are decided, so that the implementer does not re-derive them:

* **The words of the engine (R4, R7).** `placedPartsForTheEngine(placed)` becomes `tidied(placed)`, with no change of
  the words, and keeps its name, since what it gives is still what the engine is given. `PlacedWords.forTheEngine`,
  `EngineText.kt` with `withCharactersAboveFfffAsSpaces`, and `EngineTextTest.kt` are removed. The parse of the
  Markdown is unchanged, so four emoji at the start of a line stay a paragraph, as they are characters and not spaces.
  The check of `ReadAloudAction` for a file with nothing to read, words that are all blank, then finds a file of
  emoji to read, and a file of white space still empty, with no change of its code
* **The words of the highlight (R8).** Each character above U+FFFF is a word of its own in `wordsOf` of
  `ReadingSegments.kt`, outside the words that stand for a block, so that "one😀two" gives `one`, `😀` and `two`, as
  the emoji splits them now, and the voice says "one grinning face two". Its place is the range of its two halves in
  the file, as `placeOf` gives it from the offsets, and its weight in the estimate is 1, as `weightOf` gives it. A
  sequence joined by U+200D keeps the joiner as a word of its own, which the aligner gives no interval, as it does now
  with the emoji as spaces, so such a piece is shown by the estimate as now
* **The words of the aligner (R8).** `partsOf` of `WordAlignment.kt` gives a part that holds a character above U+FFFF
  as the word `emoji`, a constant beside `partsOf` with a comment on why: the aligner gives no interval for an emoji,
  and given a word its lexicon lacks it places the speech of the name there, as found above. The text of the interval
  is the word as given, so `wordStartsOf` matches it unchanged. A test with the real aligner checks that `lexicon.txt`
  holds no line for the word, so that a later model that holds it is noticed
* **The test of the phonemes (R9).** In `EspeakPhonemesTest`, `thePhonemesHandedToTheEngineAreThoseOf1_5_9` makes
  `PhonemeRows(tokenCodePointsOf(<lines of tokens.txt of the int8 model>), EspeakPhonemes::sentencesOf)` and gives
  `rowsOf(sentence, DEFAULT_VOICE.accent.espeakLanguage)` for each of the seven sentences of `EspeakDataTest.SENTENCES`,
  the rows of a sentence as strings joined by ` | `, and compares them with constants, a map from sentence to string
  with the commit they were found at in a comment. The constants are taken from a run of the test under 1.5.9 that
  fails and prints what it found, and are then written into the test
* **The build (R1, R2).** `piperPhonemizeVersion` becomes `1.6.1`. `piper-phonemize-native-lib-linux-x64` is a
  dependency of the release as that of macOS is. `piperPhonemizeLinuxX64Jar`, `piperPhonemizeLinuxX64Sha256`,
  `verifyPiperPhonemizeLinuxX64`, `files(...).builtBy(...)`, the import of `MessageDigest` and the comment that names
  the script go, and so do `scripts/build-piper-phonemize-linux-x64.sh` and
  `native/piper-phonemize-native-lib-linux-x64-1.5.9.jar`, which leaves `scripts/` and `native/` empty. sherpa-onnx
  stays 2.0.2
* **The glibc (R3).** The newest symbol version is read with `objdump -T` from the library in the jar of the Gradle
  cache that the build resolved, and recorded with the file and its SHA-256

## Files

- `build.gradle.kts`: 1.6.1, the Linux library from the release, the checksum and its task removed
- `scripts/build-piper-phonemize-linux-x64.sh`: removed
- `native/piper-phonemize-native-lib-linux-x64-1.5.9.jar`: removed
- `src/main/kotlin/com/ritenoar/aloudrite/EngineText.kt`: removed
- `src/main/kotlin/com/ritenoar/aloudrite/PlacedPart.kt`: `forTheEngine` removed, and with it `substituted` and `textForTheSentences`, which only it used
- `src/main/kotlin/com/ritenoar/aloudrite/Sentences.kt`: the sentences are cut from `text`, as `textForTheSentences` is gone
- `src/test/kotlin/com/ritenoar/aloudrite/HeardWordEstimateTest.kt`, `PartOfTheFileTest.kt`: the two tests that used an emoji in a code span as words with nothing to hear use a code span of spaces, and the test of a selection hears the emoji as it stands
- `src/main/kotlin/com/ritenoar/aloudrite/SpokenMarkdown.kt`: `placedPartsForTheEngine` without `forTheEngine`, and the
  comments of `spokenPartsFrom` and `tidied` that name the space in place of an emoji
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: the comment that names characters above U+FFFF as spaces
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSegments.kt`: a character above U+FFFF as a word of its own in `wordsOf`
- `src/main/kotlin/com/ritenoar/aloudrite/WordAlignment.kt`: the word `emoji` for such a character in `partsOf`
- `src/test/kotlin/com/ritenoar/aloudrite/EngineTextTest.kt`: removed
- `src/test/kotlin/com/ritenoar/aloudrite/EspeakPhonemesTest.kt`: the test of requirement 9, and that of requirement 5 in
  place of `anEmojiIsReportedAsReadOrLeftOut`
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`: the test of requirement 6 in place of
  `aCharacterAboveFfffAddsNothingToTheLengthOfASentence`
- `src/test/kotlin/com/ritenoar/aloudrite/EngineLengthsMeasurement.kt`: the sentences as they are, without
  `withCharactersAboveFfffAsSpaces`
- `src/test/kotlin/com/ritenoar/aloudrite/RealAlignerTest.kt`: the sentence with 😀, and the word `emoji` not in the
  lexicon
- `src/test/kotlin/com/ritenoar/aloudrite/PhonemeRowsTest.kt`, `PlacedPartsTest.kt`, `SpokenMarkdownTest.kt`,
  `ReadAloudActionTest.kt`, `ReadingSegmentsTest.kt`, `WordAlignmentTest.kt`, `PreviewPlacesTest.kt`: the tests on plain
  values
- `README.md`, `THIRD-PARTY.md`, `docs/reading-markdown.md`: requirements 11 and 12
- `changes/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/developer-checks.md`: new, the checks of requirements
  14 and 15
- `changes/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/record.md`: the phonemes, the glibc, the lengths, the
  size of the archive and the developer's results

## Tasks

### The phonemes under 1.5.9, before the move

- [x] Add `thePhonemesHandedToTheEngineAreThoseOf1_5_9` to `EspeakPhonemesTest` as decided above, run it with the int8
  model in place under 1.5.9, write the rows it printed into the test as constants, with `e725b7b` or the commit it ran
  at, and run it again until it passes. Record the seven strings in `record.md`, with the commit, the model and the
  machine (R9)
- [x] Commit: the test of the phonemes under 1.5.9, with its record entry

### The build on 1.6.1

- [x] In `build.gradle.kts`, set `piperPhonemizeVersion` to `1.6.1`, add
  `com.github.csukuangfj.piper.phonemize:piper-phonemize-native-lib-linux-x64:$piperPhonemizeVersion` as
  `implementation`, and remove the jar, the checksum, `verifyPiperPhonemizeLinuxX64`, its dependency, the import of
  `MessageDigest` and the comment on the project's build (R1, R2)
- [x] Remove `scripts/build-piper-phonemize-linux-x64.sh` and `native/piper-phonemize-native-lib-linux-x64-1.5.9.jar`,
  and check with `git grep` that nothing outside `changes/` names them, `piperPhonemizeLinuxX64` or
  `verifyPiperPhonemizeLinuxX64`, apart from `README.md` and `THIRD-PARTY.md`, which the documents task corrects (R2)
- [x] Run `objdump -T` on `libpiper-phonemize-jni.so` of the resolved `piper-phonemize-native-lib-linux-x64-1.6.1.jar`
  and record in `record.md` the newest `GLIBC_` version it names, which must be 2.17 or older, with the file and the
  SHA-256 of the jar (R3)
- [x] Run `./gradlew test` with both models in place. The tests that hold now are those of the phonemes, of the data
  (`EspeakDataTest`) and of the lengths of requirement 3 of unit 0030. `anEmojiIsReportedAsReadOrLeftOut` should now
  write `true`. The tests on the emoji as spaces still pass, since the plugin still gives the engine spaces. Where the
  test of the phonemes fails under 1.6.1, record the rows that differ in `record.md` and stop for the human, since
  requirement 9 is then false and the constants are not to be changed (R1, R3, R9)
- [x] Commit: the build on 1.6.1 without the project's own Linux library

### The characters above U+FFFF as they stand

- [x] Change the tests on plain values first, so that they fail until the code below is done (R4, R7, R8, R13):
    * `SpokenMarkdownTest`: `emojiAtTheStartOfALineAreSpacesAndMakeNoCodeBlock` becomes one that hears four emoji at
      the start of a line as `"😀😀😀😀 not code."`, a paragraph and not "code example", and `Before 😀 after.` as it
      stands. Add an emoji alone in a file, at the start and at the end of a paragraph, inside a word (`one😀two`),
      and next to ❤ (`❤😀`), each heard as it stands
    * `PlacedPartsTest`: the tests of `forTheEngine` are replaced by ones on `placedPartsForTheEngine`: the text of
      `Before 😀 and😀after.` is kept with its emoji, every character has an offset, and the two halves of an emoji
      have the offsets of the two halves in the file; a number mark of `😀 1. x` keeps its place
    * `ReadingSegmentsTest`: `anEmojiSplitsTwoWordsAndIsInNeitherOfThem` becomes one in which `one😀two` gives the
      words `one`, `😀` and `two` with their places in the file; add an emoji at the start and the end of a block,
      next to ❤, as `❤😀` giving `❤` and `😀`, and four at the start of a line giving four words with their places;
      `theSentenceAroundAnEmojiIsTheOneOfAReadingFromTheCaret` keeps its sentences
    * `WordAlignmentTest`: `partsOf` gives the word `emoji` for a word of a character above U+FFFF, also with
      punctuation after it, gives ❤ as it is, and `wordStartsOf` finds the starts of `before`, `emoji` and `after`
      from intervals that hold `emoji`
    * `PreviewPlacesTest`: for `Before 😀 after.`, the places that `ReadingSegments` gives to `Before`, `😀` and
      `after` give the pieces of the preview that show them
    * `ReadAloudActionTest`: the three tests that the engine is given spaces become ones that it is given the text as
      it stands; a press on a file of nothing but emoji and white space reads it, and without the model offers the
      download as for any other file to read; a file of nothing but white space still says that there is nothing to
      read, as an existing test does, or a new one where none does
    * `PhonemeRowsTest`: the case that gives `withCharactersAboveFfffAsSpaces("one😀two")` becomes one in which the
      stand-in for eSpeak NG is asked `one😀two` as it stands
- [x] Make `placedPartsForTheEngine` give the words unchanged, remove `PlacedWords.forTheEngine`, `EngineText.kt` and
  `EngineTextTest.kt`, and correct the comments of `SpokenMarkdown.kt` and `ReadAloudAction.kt` that name the spaces
  (R4, R7)
- [x] Make each character above U+FFFF a word of its own in `wordsOf` of `ReadingSegments.kt`, and give it to the
  aligner as `emoji` in `partsOf`, as decided above (R8)
- [x] Remove the remaining uses of `withCharactersAboveFfffAsSpaces` in `KokoroSpeechEngineTest` and
  `EngineLengthsMeasurement`, which give the sentences as they are, and run the tests on plain values until they pass
  (R4, R13)
- [x] Commit: the characters above U+FFFF as they stand in the file, and the words of the highlight around them

### The tests with the real binding, engine and aligner

- [x] In `EspeakPhonemesTest`, replace `anEmojiIsReportedAsReadOrLeftOut` with
  `anEmojiIsReadAsGrinningFace`, which gives "Before the sign, 😀 and after the sign." in `en-us` and fails unless the
  phonemes hold `ɡɹˈɪnɪŋ fˈeɪs` (R5)
- [x] In `KokoroSpeechEngineTest`, replace `aCharacterAboveFfffAddsNothingToTheLengthOfASentence` with
  `anEmojiIsSpokenAndLengthensTheSentence`: "Before the sign, 😀 and after the sign." gives more than 1.1 times the
  samples of "Before the sign, and after the sign.", with both counts in the message (R6)
- [x] In `RealAlignerTest`, align "Before the sign, 😀 and after the sign." as the trial sentences are, at 1.0× and 1.5×,
  and check that the aligner's `lexicon.txt` holds no line for the word `emoji` (R8)
- [x] Run `./gradlew test` with both models and the aligner model in place in the sandbox, and check in the report that
  `EspeakPhonemesTest`, `EspeakDataTest`, `KokoroSpeechEngineTest` and `RealAlignerTest` ran and none was skipped (R3,
  R5, R6, R9, R10, R14)
- [x] Run `ALOUDRITE_MEASURE_ENGINE_LENGTHS=1 ./gradlew cleanTest test --tests
  com.ritenoar.aloudrite.EngineLengthsMeasurement` and record in `record.md` the samples and seconds of "Before the
  sign, 😀 and after the sign." and "Before the sign, and after the sign.", with what it gave for U+20000, U+10330 and
  U+1D400 (R6)
- [x] Build the plugin with `./gradlew buildPlugin`, check that it passes within 30,000,000 bytes, and record the size
  of the archive beside the 21,892,913 bytes of unit 0030 (R10)
- [x] Commit: the tests with the real binding, engine and aligner, and the record of what they found

### The documents

- [x] `THIRD-PARTY.md`: 1.6.1 for the Java binding and both native libraries, all from the release, the Linux library
  no longer built by the project, and what they hold with the license of each, taken from `CMakeLists.txt` and `cmake/`
  of `piper-phonemize` at `v1.6.1` (commit `5f92f0b`), among them eSpeak NG at `ed530aa` and uni-algo where it still is,
  and the license file at that tag (R11)
- [x] `README.md`: 1.6.1 where 1.5.9 is named, the three jars of `piper-phonemize` fetched from GitHub, the section on
  the Linux library replaced by one that says the release's library needs glibc 2.17 or older, the newest version
  recorded above, and so which glibc the plugin needs on Linux, without the script and the checksum, the size of the
  archive, and the paragraph on characters above U+FFFF: read as eSpeak NG reads them, by their names where its data
  has one, such as "grinning face" for 😀, and as "letter" with the digits of their code point where it has none (R11,
  R12)
- [x] `docs/reading-markdown.md`, "Emoji and symbols": the "Open" item on whether they are read answered, and "Now"
  saying that characters above U+FFFF reach eSpeak NG intact since this unit and are read as `README.md` says, with
  "grinning face" and "letter 2 0 0 0 0" as the examples (R12)
- [x] Check with `git grep -n -i 'U+FFFF\|1\.5\.9\|build-piper\|glibc 2.38'` outside `changes/` that nothing says
  they are left out or names the old build any more (R11, R12)
- [x] Commit: the documents

### The checks and the handing over

- [x] Write `developer-checks.md`, as unit 0030 did: on macOS on Apple Silicon, `./gradlew test` with both models and
  the aligner model in place, with `EspeakPhonemesTest`, `EspeakDataTest`, `KokoroSpeechEngineTest` and
  `RealAlignerTest` not skipped, sending back the report's counts and the names of any skipped or failed test, and
  `EngineLengthsMeasurement`, sending back `build/engine-lengths.txt`; on Linux x64 and on macOS, listening to
  `samples/markdown-features.md` in the sandbox IDE, with a table for what sounded different from before this unit,
  the emoji among it, and whether the highlight fell on the words around the emoji in the editor and the preview (R14,
  R15)
- [x] Run `npm run check-changes`, and log in `record.md` that the unit waits for the developer's checks of
  `developer-checks.md` and for the review
- [x] Commit: the developer checks and the record
- [ ] Once the developer sent the results back, record them in `record.md`: the counts and lengths on macOS, and what
  sounded different on each platform, and for each test that failed on macOS whether this unit caused it (R14, R15)

## Verification

- [x] `./gradlew test` passes in the sandbox with both models and the aligner model in place and no test with the real
  binding, engine or aligner skipped
- [x] `./gradlew buildPlugin` passes, and the archive is within 30,000,000 bytes
- [x] The phonemes under 1.5.9, the glibc, the two lengths of requirement 6 and the size of the archive are in
  `record.md`
- [x] Nothing of the project builds or checks a native library of `piper-phonemize`
- [ ] The developer's results on macOS and the listening on both platforms are in `record.md`
- [x] `npm run check-changes` passes

## Commit Points

- [x] `feature-0035` The plan
- [x] `feature-0035` The test of the phonemes under 1.5.9, with its record entry
- [x] `feature-0035` The build on 1.6.1 without the project's own Linux library
- [x] `feature-0035` The characters above U+FFFF as they stand, and the words of the highlight around them
- [x] `feature-0035` The tests with the real binding, engine and aligner, and what they found
- [x] `feature-0035` The documents
- [x] `feature-0035` The developer checks and the record
- [ ] `feature-0035` The developer's results
