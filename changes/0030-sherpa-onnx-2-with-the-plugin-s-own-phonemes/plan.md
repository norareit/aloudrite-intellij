# sherpa-onnx 2 with the plugin's own phonemes

## Approach

Under sherpa-onnx 1.13.8 the Kokoro front end inside the native library turns the text of a piece into rows of
phoneme tokens, one row per sentence, and the model makes the audio of each row. Under 2.0.2 the plugin makes those
rows itself and hands them over as `GenerationConfig.phonemeCodepoints`, one `int[]` of code points per row. The work
falls into four layers:

1. **The rows** (`PhonemeRows`, pure). It does what the Kokoro front end of 1.13.8 did for English text as the plugin
   configures the engine, which is to give the text to eSpeak NG whole, so that the voice says what it said (decision
   1). Under 1.13.8 the language was the `voice` metadata of the model, `en-us`, so the lexicon was never read. It
   takes the set of code points of `tokens.txt` and a function that gives the phonemes of each sentence of a text, and
   is tested with a stand-in for that function (R2, R4, R12).
2. **eSpeak NG** (`EspeakPhonemes`, the only class that names a class of `piper-phonemize`). It gives the phonemes of
   each sentence of a text through `PiperPhonemize.text(text, "en-us")`, with the data of the model folder in use (R2,
   R17).
3. **The engine** (`KokoroSpeechEngine`). It reads the tokens of the model folder, makes the rows of each piece with
   `PhonemeRows` and hands them to `OfflineTts.generateWithConfigAndCallback` (R2).
4. **The build.** sherpa-onnx 2.0.2; the Java archive of `piper-phonemize` 1.5.9 without `espeak-ng-data`; its native
   library for macOS on Apple Silicon from the release; and its native library for Linux x64 built by a script of the
   project and committed with its checksum (R1, R16, R17, R18).

The measurements under 1.13.8 that requirements 3 and 9 compare with are made first, on this branch, before anything
moves.

What the libraries do, as read for this plan from their sources at the tags `v1.13.8` and `v2.0.2` of sherpa-onnx and
`v1.5.9` of `piper-phonemize` (commit `dae4d2d`), and tried on the Linux host with the release's own Linux library:

* `PiperPhonemize.initializeWithDataDir(dir)` calls `espeak_Initialize` with `dir`, which is the folder that holds
  `espeak-ng-data`, not `espeak-ng-data` itself. It is guarded by `std::call_once`: only the first call in a process
  takes effect, and every later one returns 0 whatever folder it names. It returns -1 where the data is missing
* `PiperPhonemize.text` selects the voice at every call, and eSpeak NG reads `en_dict` and the phoneme tables from
  that folder again each time. Tried on the host: with the data folder deleted after the initialization, every call
  gave the phonemes `.`, with `Can't read dictionary file` on the error output. So once the folder that eSpeak NG was
  initialized with is gone, every word is dropped, with no error the plugin can see. The plugin
  lets a listener remove a model that is neither in use nor that of the reading, so a reading with the fp32 model, a
  choice of the int8 model and the removal of the fp32 model in one session would leave eSpeak NG reading a folder
  that no longer exists
* `PiperPhonemize.text` returns null where the native call failed, and a `Result` that has to be closed otherwise.
  An empty text gives no sentences, a text of white space one empty sentence. The binding loads its native library
  from the resource `piper-phonemize/native/<os>-<arch>/libpiper-phonemize-jni.<so|dylib>` of the class loader of
  `LibraryUtils`, as sherpa-onnx loads its own, and `System.load` prints the warning of restricted methods, as
  sherpa-onnx does now
* Tried on the host with the data of the fp32 model folder: `api` and `API` both give `ˌeɪpˌiːˈaɪ`, `npm` gives
  `ˌɛnpˌiːˈɛm`, `3` `θɹˈiː`, `2026` `tˈuː θˈaʊzənd twˈɛnti sˈɪks`, `x86` `ˈɛks ˈeɪɾi sˈɪks`, `%` `pɚsˈɛnt`, `&`
  `ˈænd`, `_` nothing, `aloudrite` `ɐlˈaʊdɹaɪt`
* In 2.0.2, `phonemeCodepoints` takes one row per sentence. Each row is mapped through `tokens.txt`, a code point that
  is not a token is skipped, and a row is cut after 401 tokens wherever that falls (issue 4178), then wrapped in the
  padding token 0. Each row is one run of the model and one call of the callback, as each sentence was under 1.13.8,
  so the stop after the sentence being made holds as before. The text handed beside the rows is not read in that
  path. `GenerationConfig` has the same default silence scale, 0.2, as the call the plugin makes now
* All 114 tokens of `tokens.txt` are one code point each. A line of `tokens.txt` is the token, a space and its number,
  and the token of the word gap is itself a space, so the number is what follows the last space of the line
* 2.0.2 warns that `data_dir` of Kokoro is ignored, and still reads the lexicon it is given
* Found by the implementer, under 1.13.8 with the debug output on: `offline-tts-kokoro-impl.h` sets the language to the
  `voice` metadata, `en-us`, where `lang` is empty, and the Kokoro front end then gives the whole text to eSpeak NG
  before it looks at the lexicon. The sentence "It builds for x86_64 and ARM64 with v1.13.8." was one row, with
  "v1.13.8" read with "point" for each dot. Handing the engine the phonemes of `PiperPhonemize.text(sentence, "en-us")`
  for the six sentences of requirement 3 gave lengths within 6.6% of 1.13.8

How the parts are decided, so that the implementer does not re-derive them:

* **The rows (R2, R4).** `PhonemeRows.rowsOf(text)`, as the Kokoro front end of 1.13.8 did for the text of a piece with
  the language `en-us`, and the lexicon not used:
    1. A text of white space only gives no row, and eSpeak NG is not asked
    2. Replace `，` and `:` with `,`, `、` with `,`, `；` with `;`, `：` with `:`, `。` with `.`, `？` with `?` and `！` with
       `!`, in that order, and every run of white space with one space, as 1.13.8 did before it asked eSpeak NG
    3. Give the text to `sentencesOf`, which gives the phonemes of each sentence eSpeak NG returns as code points, and
       take one row for each, with every code point that is not a token left out. A row of no tokens is dropped
    4. A row of more than 400 tokens is cut after the last space that keeps a row within 400, which falls between two
       words that eSpeak NG reads, and only where its first 400 tokens hold no such space, at 400 (R4)
    5. Join short rows as 1.13.8 does, in tokens without the padding: a row of more than 10 tokens stands alone; a row
       of 10 or fewer is appended to the row before it where the two together have fewer than 46 tokens or it has 2
       or fewer, and where the two together have no more than 400 (R4); otherwise it stands alone. The first row
       always stands alone
    6. A piece that gives no row is not handed to the engine at all
  The joining rule is that of the front end of 1.13.8 for the lexicon path, read from its source for the plan. The
  implementer could not read the path that 1.13.8 took for eSpeak NG, so the rows are checked against the lengths of
  1.13.8 of requirement 3
* **The data of eSpeak NG (R17).** Since eSpeak NG can be initialized only once in a process and reads its data
  folder at each call, `EspeakPhonemes` initializes it once with a folder of its own, made with
  `Files.createTempDirectory("aloudrite-espeak-ng")` and deleted when the process ends, which holds one symbolic link
  `espeak-ng-data` to the `espeak-ng-data` of the model folder in use. Each creation of a `KokoroSpeechEngine` points
  the link at the folder of its own model before the engine speaks: it makes the new link under a temporary name in
  the same folder and moves it over the old one with `ATOMIC_MOVE`, so the link never points nowhere. The service has
  one engine at a time, and a model may be removed only where it is neither in use nor that of the reading, so the
  link points at a folder that exists whenever a piece is phonemized. eSpeak NG reads the data of the model folder in
  use, and the removal of another model takes nothing from it. Rejected: initializing with the model folder of the
  first reading, which the case above breaks; and copying the data out of the model folder, which is a second copy
  of 19 MB
* **The engine (R2).** The tokens are read from `tokens.txt` of the model folder at each creation of the engine, and the
  lexicon is not read. `speak` makes the rows of the text, and where there are any, sets `sid` to
  `AF_HEART_SPEAKER_ID`, `speed` to the speed given and `phonemeCodepoints` to the rows, and calls
  `generateWithConfigAndCallback(text, config, callback)` with the callback of now. `setDataDir` is no longer called,
  and the lexicon is still given, since the native code may end the process without one
* **The archive without the data (R17).** The published `piper-phonemize-jvm-1.5.9.jar` is resolved into a
  configuration of its own, `publishedPiperPhonemizeJvm`, not transitive, and a `Zip` task with the extension `jar`
  copies its entries without `piper-phonemize/espeak-ng-data/**` into
  `build/piper-phonemize/piper-phonemize-jvm-1.5.9-without-espeak-ng-data.jar`, keeping the manifest of the release as
  it is. That file is the `implementation` dependency. The test of requirement 17 gets the path of the published jar
  through a system property that a `CommandLineArgumentProvider` of the `test` task adds, so that it is resolved only
  when the tests run
* **The Linux library (R16).** `scripts/build-piper-phonemize-linux-x64.sh <tag>` runs, in
  `quay.io/pypa/manylinux2014_x86_64`: `yum install -y java-11-openjdk-devel`, with `JAVA_HOME` set to the folder of
  that JDK under `/usr/lib/jvm`; a clone of `https://github.com/csukuangfj/piper-phonemize` at the tag; the CMake call
  of the step "Build JNI" of `jar.yaml` at that tag, `-DBUILD_SHARED_LIBS=OFF -DBUILD_PIPER_PHONEMIZE_PYTHON=OFF
  -DBUILD_PIPER_PHONEMIZE_CORE_TESTS=OFF -DBUILD_PIPER_PHONEMIZE_JNI=ON -DCMAKE_INSTALL_PREFIX=build/install`, and
  `cmake --build build --config Release --target install`, whose `CMakeLists.txt` takes `Release` where no build type
  is given; and the packing of the step "Create native JAR", `piper-phonemize/native/linux-x64/libpiper-phonemize-jni.so`
  in `piper-phonemize-native-lib-linux-x64-<version>.jar` with `jar cfv ... -C . piper-phonemize/`, the version read
  from `set(PIPER_PHONEMIZE_VERSION ...)` as the workflow reads it. The source and the build stay inside the container;
  only the jar is copied to a mounted output folder, and the container gives it to the user and group of the host,
  passed in as `$(id -u)` and `$(id -g)`, so that no file of root is left behind. The script fails, and copies
  nothing, where `objdump -T` of the library names a symbol version `GLIBC_` newer than 2.17, and where Docker is
  missing. It writes the jar to `native/` of the project and prints the tag, the file and its SHA-256
* **The checksum (R16).** `build.gradle.kts` holds the SHA-256 of the committed jar beside the version of
  `piper-phonemize`. A task `verifyPiperPhonemizeLinuxX64` computes the SHA-256 of the file and fails, naming the file,
  the checksum expected and the one found, where they differ. The dependency is `files(<the jar>).builtBy(<the task>)`,
  so every task that uses the jar, the compilation, the tests, `prepareSandbox` and `buildPlugin`, runs the check first
* **The test of requirement 17.** eSpeak NG can be initialized once per process, so the two sets of data are compared
  in two processes: a `main` in `src/test`, `PhonemesProbeProcess.kt`, initializes `PiperPhonemize` with the folder it
  is given and prints the phonemes of each sentence it is given, and the test starts it as `LockProbe` starts its
  process, with the classpath of the tests in an argument file. Once with the int8 model folder, once with a folder
  into which the test copied `piper-phonemize/espeak-ng-data/` of the published jar. The two outputs are equal
* **The measurements (R3, R9).** Made with the int8 model, the model of the existing tests with the real engine, for
  the lengths, and with the fp32 model, the one of `README.md`'s figures, for the first audio. Both go through
  `SpeechEngine` only, so they compile and run unchanged before and after the move

## Files

- `build.gradle.kts`: sherpa-onnx 2.0.2; the repository of `piper-phonemize`; the configuration, the stripping task, the
  checksum and its task; the system property of the tests
- `scripts/build-piper-phonemize-linux-x64.sh`: new, the build of the Linux library
- `native/piper-phonemize-native-lib-linux-x64-1.5.9.jar`: new, made by the script and committed
- `src/main/kotlin/com/ritenoar/aloudrite/PhonemeRows.kt`: new, the rows of a piece, the reading of `tokens.txt` from
  lines, and the limit of 400 tokens
- `src/main/kotlin/com/ritenoar/aloudrite/EspeakPhonemes.kt`: new, eSpeak NG through `piper-phonemize` and the link to
  the data
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngine.kt`: the rows of `PhonemeRows` through `GenerationConfig`,
  no `setDataDir`, the reading of the tokens
- `src/main/kotlin/com/ritenoar/aloudrite/EngineText.kt`: the comment, which names the binding of sherpa-onnx as the
  reason
- `src/main/kotlin/com/ritenoar/aloudrite/PlacedPart.kt`: only where its comment names the binding of sherpa-onnx as the
  reason
- `src/test/kotlin/com/ritenoar/aloudrite/PhonemeRowsTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/EspeakPhonemesTest.kt`: new, the real binding: requirement 7, the link
- `src/test/kotlin/com/ritenoar/aloudrite/PhonemesProbeProcess.kt`: new, the second process of requirement 17
- `src/test/kotlin/com/ritenoar/aloudrite/EspeakDataTest.kt`: new, requirement 17
- `src/test/kotlin/com/ritenoar/aloudrite/EngineLengthsMeasurement.kt`: new, the lengths of requirements 3 and 6
- `src/test/kotlin/com/ritenoar/aloudrite/FirstAudioMeasurement.kt`: new, the first audio of requirement 9
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`: requirements 3 and 6 in place of the test of the
  emoji
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroTtsConfigTest.kt`: only where it reads the data folder
- `README.md`, `THIRD-PARTY.md`, `docs/reading-markdown.md`: requirements 10 and 11, and the two new measurements under
  "Testing"
- `changes/0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes/developer-checks.md`: new, the checks on macOS and the
  listening
- `changes/0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes/record.md`: the measurements, the outcomes and the entry
  of requirement 15

## Tasks

### Measured under 1.13.8, before the move

- [x] Add `EngineLengthsMeasurement`, run only where `ALOUDRITE_MEASURE_ENGINE_LENGTHS` is set and the int8 model is
  in place, as the other measurements are. With one engine of the int8 model, at the normal speed, it makes each of
  the six sentences of requirement 3, "Before the sign, ❤ and after the sign.", and the sentence of requirement 6 with
  each of 😀, U+20000, U+10330 and U+1D400, each through `withCharactersAboveFfffAsSpaces` as a reading gives it, and
  writes the number of samples and the seconds of each to `build/engine-lengths.txt` and to the output (R3, R6, R13)
- [x] Add `FirstAudioMeasurement`, run only where `ALOUDRITE_MEASURE_FIRST_AUDIO` is set and the fp32 model is in
  place. For the first piece of words of `samples/markdown-features.md`, as `spokenPartsOf` and `piecesOf` give it, it
  measures five times the milliseconds from the creation of a `KokoroSpeechEngine` of the fp32 model, with the threads
  the plugin uses, to the first call of the callback, and five times, with one engine made beforehand, from the call
  of `speak` to the first call of the callback. It writes each time and the median of each set to
  `build/first-audio.txt` and to the output. The first creation in the process is reported apart, as the first reading
  after the start of the IDE (R9)
- [x] Run both on the Linux host, with the models in place, and record in `record.md` the samples of each sentence and
  the medians, with the machine, the model and the commit. Build the plugin and record the size of the archive (R3, R9,
  R10, R18)

### The build

- [x] Write `scripts/build-piper-phonemize-linux-x64.sh` as decided above, and run it with `v1.5.9`. Where Docker does
  not run where the implementer works, stop and ask the developer to run it on the host. Check that the library in the
  jar needs no symbol version of glibc newer than 2.17 and links only `libm`, `libpthread`, `libc` and
  `ld-linux-x86-64`, and record the tag, the size and the SHA-256 (R16)
- [x] In `build.gradle.kts`, set sherpa-onnx to 2.0.2; add an `exclusiveContent` Ivy repository for the group
  `com.github.csukuangfj.piper.phonemize` at `https://github.com/csukuangfj/piper-phonemize/releases/download`, with
  the same pattern as that of sherpa-onnx; add `piper-phonemize-native-lib-osx-aarch64` 1.5.9; and add the
  configuration `publishedPiperPhonemizeJvm`, the stripping task and the stripped jar as `implementation` (R1, R17)
- [x] Add the checksum and `verifyPiperPhonemizeLinuxX64`, and the committed jar as `files(...).builtBy(...)`. Check
  that `./gradlew buildPlugin` fails, with the message, where one byte of a copy of the jar in its place differs, and
  passes with the jar as made (R16)
- [x] Add the system property of the published jar to the `test` task (R17)
- [x] Commit: the script, the jar and the build

### The rows

The tasks below were first written for a front end with a lexicon, and were done as such at `305c815`. The spec was then
corrected, and `KokoroFrontEnd` and its test were removed and replaced by `PhonemeRows` and its test.

- [x] Write `PhonemeRowsTest` first, with a set of tokens and a stand-in for eSpeak NG that gives set sentences and
  records the text it was asked. It covers: the text is given to eSpeak NG whole and each sentence it returns is a
  row; each replacement of punctuation, the full-width marks among them, and the full-width colon that stays a colon;
  a run of white space as one space; a code point that is not a token is left out; a sentence of such code points
  only gives no row; a piece of white space only gives no row and eSpeak NG is not asked; short rows joined and a row
  of more than 10 tokens standing alone, at 10 and 11 tokens and at 45 and 46 together, and a row of 2 and of 3
  tokens; the first row, which stands alone; a space in place of a character above U+FFFF, as
  `withCharactersAboveFfffAsSpaces` gives it (R2, R5, R12)
- [x] In the same test, the cut of requirement 4: a row of exactly 400 tokens is one row; a short row is joined to a
  row before it up to 400 and not above; a sentence of more than 400 tokens is cut at the last space that keeps a row
  within the limit, and at 400 where there is none; a sentence of several times the limit; no row of a long text holds
  more than 400 tokens, and the rows hold every word in order (R4, R12)
- [x] Cover the reading of `tokens.txt`, with the line of the space, on lines given as values (R2, R12)
- [x] Write `PhonemeRows.kt` until the tests pass (R2, R4)
- [x] Remove `KokoroFrontEnd.kt` and `KokoroFrontEndTest.kt`, which the corrected spec makes unneeded
- [x] Commit: the rows and their tests

### eSpeak NG and the engine

- [x] Write `EspeakPhonemesTest`, run only where the int8 model is in place: the phonemes of `api` are `ˌeɪpˌiːˈaɪ`;
  after pointing the data at a copy of the model's `espeak-ng-data` in a temporary folder and then at the model folder
  itself and deleting the copy, `2026` still gives `tˈuː θˈaʊzənd twˈɛnti sˈɪks`; and the test of requirement 7, which
  gives `PiperPhonemize` "Before the sign, 😀 and after the sign." in `en-us` through `EspeakPhonemes`, writes to the
  output whether the phonemes hold `ɡɹˈɪnɪŋ fˈeɪs`, with the phonemes, and fails on neither outcome (R2, R7, R17)
- [x] Write `EspeakPhonemes.kt` as decided above. A failed initialization throws with the folder named, and a null
  result gives no phonemes (R2, R17)
- [x] Write `PhonemesProbeProcess.kt` and `EspeakDataTest`, run only where the int8 model is in place: the phonemes of
  the six sentences of requirement 3 and of "Before the sign, ❤ and after the sign." are the same with the data of the
  model folder as with the data of the published jar (R17)
- [x] Change `KokoroSpeechEngine` as decided above, and `KokoroTtsConfigTest` where it no longer holds (R2)
- [x] In `KokoroSpeechEngineTest`, replace `anEmojiInASentenceAddsNoBabble` with: each of the six sentences of
  requirement 3 gives a number of samples within 10% of that recorded under 1.13.8, written in the test as constants
  with the commit they were measured at; and "Before the sign, X and after the sign." with each of 😀, U+20000,
  U+10330 and U+1D400, through `withCharactersAboveFfffAsSpaces`, gives a length within 10% of "Before the sign, and
  after the sign." Keep the other cases, which now run under 2.0.2 (R3, R5, R6, R8)
- [ ] Correct the comments of `EngineText.kt` and `PlacedPart.kt` that give the binding of sherpa-onnx as the reason
  for the spaces: the binding of `piper-phonemize` leaves the characters out, as that of sherpa-onnx 1.13.8 did not
  hand them over intact (R11)
- [ ] Run `./gradlew test` with both models in place, and check from its report that the tests with the real engine,
  those of requirements 3, 6, 7 and 17 and those of the earlier units, ran and were not skipped. Record what the test
  of requirement 7 wrote (R3, R6, R7, R8, R13, R17)
- [ ] Commit: eSpeak NG, the engine and their tests

### Measured under 2.0.2

- [ ] Run `EngineLengthsMeasurement` and `FirstAudioMeasurement` again on the same machine, and record the lengths and
  the medians beside those of 1.13.8, and whether every median of the first audio is within 50 ms of its earlier one.
  Where one is not, find from the times which part grew, the reading of the tokens, the creation of eSpeak NG or the
  phonemes of the text, and make that part faster until all are within 50 ms. Record what
  was done and the medians after it (R3, R6, R9)
- [ ] Build the plugin, check that `./gradlew buildPlugin` passes within 30,000,000 bytes, and record the size of the
  archive and its growth (R10, R18)
- [ ] Commit, where a change was made for the first audio

### The documents

- [ ] `THIRD-PARTY.md`: sherpa-onnx 2.0.2 and `piper-phonemize` 1.5.9, the Java archive without `espeak-ng-data` and the
  Linux library built by the project, and what the native libraries hold with the license of each, taken from
  `CMakeLists.txt`, `cmake/` and the CMake files of `sherpa-onnx/csrc` and `sherpa-onnx/jni` of sherpa-onnx at `v2.0.2`,
  and from `CMakeLists.txt` and `cmake/` of `piper-phonemize` at `v1.5.9`, among them the commit of eSpeak NG it
  builds and uni-algo where it still does. The data of eSpeak NG that the plugin reads comes from the model package,
  under the entry of the package. Remove what 2.0.2 no longer links (R10)
- [ ] `README.md`: 2.0.2 and 1.5.9 where 1.13.8 is named, the jars fetched from GitHub under "Requirements", the size
  of the archive measured above, the reason of the GPL in the terms of `piper-phonemize`, which holds eSpeak NG, and a
  section on the Linux library: built by the project because the release's needs glibc 2.38, how to build it again
  with the script and update the checksum. The reason for leaving out characters above U+FFFF, and the two new
  measurements under "Testing" with their commands (R10, R11)
- [ ] `docs/reading-markdown.md`, "Emoji and symbols": the reason in the terms of `piper-phonemize`, and the "Open" item
  naming the later unit rather than unit 0014 (R11)
- [ ] Commit: the documents

### The checks and the handing over

- [ ] Write `developer-checks.md`: on macOS on Apple Silicon, `./gradlew test` with both models in place, with the
  tests with the real engine not skipped, and `EngineLengthsMeasurement`, with what to send back: the report's counts
  and `build/engine-lengths.txt`; on Linux x64 and on macOS, the listening to `samples/markdown-features.md` in the
  sandbox IDE with the fp32 model, noting anything that sounds different from 1.13.8, with a table to fill in (R13,
  R14)
- [ ] Read the specs of units 0028 and 0029 and add to `record.md` the entry of requirement 15: for each, every
  requirement and decision that this unit made untrue, unneeded or open to a simpler way, with what of this unit
  causes it (R15)
- [ ] Run `npm run check-changes`, and log in `record.md` that the unit waits for the developer's checks of
  `developer-checks.md` and for the review
- [ ] Commit: the developer checks and the record
- [ ] Once the developer sent the results back, record them in `record.md`: the results on macOS with the lengths, and
  what sounded different on each platform. Where a test failed on macOS, fix it before going on (R13, R14)

## Verification

- [ ] `./gradlew test` passes on Linux x64 with both models in place and no test with the real engine skipped
- [ ] `./gradlew buildPlugin` passes, and the archive is within 30,000,000 bytes
- [ ] The build fails where the committed Linux jar does not match its checksum
- [ ] The lengths of requirements 3 and 6, the medians of requirement 9 and the outcome of requirement 7 are in
  `record.md`, before and after the move where the requirement compares
- [ ] The developer's results on macOS and the listening on both platforms are in `record.md`
- [ ] `record.md` holds the entry of requirement 15
- [ ] `npm run check-changes` passes

## Commit Points

- [x] `feature-0030` The plan
- [x] `feature-0030` The measurements under 1.13.8, with their results in the record
- [x] `feature-0030` The Linux library of `piper-phonemize`, its script and the build on sherpa-onnx 2.0.2
- [x] `feature-0030` The rows of the plugin
- [ ] `feature-0030` eSpeak NG through `piper-phonemize` and the engine on the plugin's phonemes
- [ ] `feature-0030` The measurements under 2.0.2, and what was made faster where needed
- [ ] `feature-0030` The documents
- [ ] `feature-0030` The developer checks and the record
- [ ] `feature-0030` The developer's results
