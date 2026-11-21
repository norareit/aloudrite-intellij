---
kind: feature
status: in-progress
timestamp: 2026-11-21T23:40:00Z
supersedes: 0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes
---

# piper-phonemize 1.6 and emoji read by their names

## Log

- 2026-11-21T11:48:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `13b0427`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0035, after 0034, superseding
  unit 0030, whose requirements on the Linux library and on characters above U+FFFF it makes untrue. Read the changes
  of `piper-phonemize` from `v1.5.9` to `v1.6.1` on GitHub and tried the Java archive and the native library for Linux
  x64 of 1.6.1 outside the project, on the host and in `eclipse-temurin:21-jdk-jammy`, as the context of the spec
  says. Decided with the human: the move to the fixed release, characters above U+FFFF read by their names, and a unit
  of its own. Decided by me and open to the human: the release's own Linux library in place of the project's build,
  and characters without a name read as eSpeak NG reads them. Left beyond the request: leaving out the characters
  without a name, and a note on what this unit makes untrue in the specs of units 0028 and 0032. The status is
  `draft`
- 2026-11-21T12:52:05Z [human] approved the spec. The status is `approved`
- 2026-11-21T23:21:02Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of the orchestrator in
  the sandbox, on the branch `feature-0035-piper-phonemize-1-6-and-emoji-read-by-their-names` at `e725b7b`, wrote
  `plan.md`. Read `process.md`, the standards and `roles/planner.md` of the installed kit. Fetched the three jars of
  `piper-phonemize` 1.6.1 outside the project: their names and layout are those of 1.5.9, and the Linux library needs
  no glibc symbol version newer than 2.14. Tried the forced aligner of unit 0028 on the audio of a sentence with the
  name of an emoji spoken: it gives no interval for an emoji, so a piece that holds one would lose its alignment, and
  a word its lexicon lacks, given in its place, is placed where the name is heard. The plan therefore makes each
  character above U+FFFF a word of its own and gives it to the aligner as the word `emoji`, so that the highlights of
  the words around it stay where they are heard, as requirement 8 asks. It found that a piece holding ❤ already has no
  alignment before this unit, which stays as it is, since how ❤ is read is a non-goal. The status stays `approved`
- 2026-11-21T23:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a unit run of the orchestrator
  in the sandbox, on the branch `feature-0035-piper-phonemize-1-6-and-emoji-read-by-their-names`, began the work. Read
  `process.md`, the standards, `roles/implementer.md` and the three documents of the unit. The status is `in-progress`
- 2026-11-21T23:41:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, added
  `thePhonemesHandedToTheEngineAreThoseOf1_5_9` to `EspeakPhonemesTest` and made the companion of `EspeakDataTest`
  internal so that it shares `SENTENCES`. It failed with no constants and printed what was found, which were written
  into the test, and it then passed. Found under `piper-phonemize` 1.5.9 at `a80af4a` (the tree before any change of
  this unit), with the int8 model, with the voice `DEFAULT_VOICE` (`en-us`), on Linux x64 in the sandbox; the rows of
  a sentence are joined by ` | `, and each of these sentences is one row. Each ends with a space after the full stop:
    * `I have 3 cats in 2026.`: `aɪ hæv θɹˈiː kˈæts ɪn tˈuː θˈaʊzənd twˈɛnti sˈɪks. `
    * `Version 4.2 is 5% faster & smaller.`: `vˈɜːʒən fˈoːɹ pɔɪnt tˈuː ɪz fˈaɪv pɚsˈɛnt fˈæstɚ ænd smˈɔːlɚ. `
    * `Read the README of the API with npm.`: `ɹˈiːd ðə ɹˈiːdmiː ʌvðɪ ˌeɪpˌiːˈaɪ wɪð ˌɛnpˌiːˈɛm. `
    * `It builds for x86_64 and ARM64 with v1.13.8.`: `ɪt bˈɪldz fɔːɹ ˈɛks ˈeɪɾi sˈɪks sˈɪksti fˈoːɹ ænd ˌeɪˌɑːɹɹˈɛm sˈɪksti fˈoːɹ wɪð vˈiː wˈʌn pɔɪnt wˈʌn θɹˈiːpɔɪnt ˈeɪt. `
    * `AloudRite reads Markdown aloud.`: `ɐlˈaʊd ɹˈaɪt ɹˈiːdz mˈɑːɹkdaʊn ɐlˈaʊd. `
    * `Before the sign, and after the sign.`: `bᵻfˌoːɹ ðə sˈaɪn, ænd ˈæftɚ ðə sˈaɪn. `
    * `Before the sign, ❤ and after the sign.`: `bᵻfˌoːɹ ðə sˈaɪn, ɹˈɛd hˈɑːɹt ænd ˈæftɚ ðə sˈaɪn. `
- 2026-11-21T23:55:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, moved the build to
  `piper-phonemize` 1.6.1 for the Java binding and both native libraries, and removed the project's own Linux build: the
  script, the committed jar, its checksum and the task that checked it, with the import of `MessageDigest`. `git grep`
  finds the removed names outside `changes/` only in `README.md` and `THIRD-PARTY.md`, which the documents task
  corrects. The native library for Linux x64 is `piper-phonemize/native/linux-x64/libpiper-phonemize-jni.so` of
  `piper-phonemize-native-lib-linux-x64-1.6.1.jar`, as the Gradle cache resolved it, SHA-256
  `788d4da5729c0396120266cfa15ea765a6f2ebe2359729292f6d4b90c3cfb91c`. By `objdump -T` the newest symbol version of
  glibc it names is `GLIBC_2.14`, which is 2.17 or older (R3). `EspeakPhonemesTest` (6 tests) and `EspeakDataTest`
  (1 test) ran in the sandbox, Debian 12 with glibc 2.36, with none skipped, and the phonemes of
  `thePhonemesHandedToTheEngineAreThoseOf1_5_9` are the same under 1.6.1 for all seven sentences (R9), and
  `anEmojiIsReportedAsReadOrLeftOut` wrote `true`. `./gradlew test` on the tree of this commit: 1060 tests, 0 failed,
  8 skipped, which are the measurements and `RealModelPackageTest`, and none of the tests with the real binding or
  engine. The status stays `in-progress`
- 2026-11-22T00:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, made characters above U+FFFF reach
  eSpeak NG as they stand in the file. Removed `withCharactersAboveFfffAsSpaces`, `EngineTextTest`,
  `PlacedWords.forTheEngine` and the spaces in `placedPartsForTheEngine`, which now only tidies the parts. Each such
  character is a word of its own in `wordsOf` of `ReadingSegments.kt`, outside the words that stand for a block, and
  `partsOf` of `WordAlignment.kt` gives it to the aligner as `emoji`. Necessary work that the plan did not name, added
  to its list of files: `PlacedWords.substituted` and `textForTheSentences` served only `forTheEngine`, so they went
  with it, and `Sentences.kt` cuts the sentences from `text`; two tests of `HeardWordEstimateTest` took an emoji in a
  code span as words with nothing to hear, which it now is not, so they take a code span of spaces, and the test of
  `PartOfTheFileTest` on a selection hears the emoji as it stands. For `ReadAloudActionTest`, a file of nothing but
  emoji and white space is read, one of white space is still a file with nothing to read, and without the model the
  press asks to download it, as it does for a text file. `KokoroSpeechEngineTest` has `anEmojiIsSpokenAndLengthensTheSentence`
  in place of the test of the sentences with nothing added, and `EngineLengthsMeasurement` gives the sentences as they
  are. `./gradlew test` on the tree of this commit: 1059 tests, 0 failed, 8 skipped, the same measurements and
  `RealModelPackageTest` as before. The tests of the real aligner and of requirement 5 follow
- 2026-11-22T00:45:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, added the tests with the real
  binding, engine and aligner. `anEmojiIsReadAsGrinningFace` fails unless the phonemes of "Before the sign, 😀 and
  after the sign." in `en-us` hold `ɡɹˈɪnɪŋ fˈeɪs` (R5), and passes. `RealAlignerTest` aligns that sentence with the
  trial sentences at 1.0× and 1.5× and checks that `lexicon.txt` of `english-mfa-v3.1.0` has no line for the word
  `emoji`, and both pass (R8). `./gradlew test` on the tree of this commit, in the sandbox with the int8 model, the fp32
  model and the aligner model in place: 1060 tests, 0 failed, 8 skipped, which are the measurements and
  `RealModelPackageTest`, so `EspeakPhonemesTest` (6), `EspeakDataTest` (1), `KokoroSpeechEngineTest` (6) and
  `RealAlignerTest` (2) ran, none skipped. `EngineLengthsMeasurement` with the int8 model, `Linux amd64`, at the normal
  speed, gave for "Before the sign, and after the sign." 45,044 samples, 1.877 s, and for "Before the sign, 😀 and after
  the sign." 65,192 samples, 2.716 s, which is 1.447 times as many (R6). The other sentences of the table: "Before the
  sign, ❤ and after the sign." 56,117 samples, 2.338 s; U+20000 84,857 samples, 3.536 s; U+10330 84,130 samples, 3.505
  s; U+1D400 77,325 samples, 3.222 s, which are read as "letter" with the digits of their code point and take longer
  than the emoji with a name. `./gradlew buildPlugin` passed, and the archive `aloudrite-intellij-0.0.1.zip` is
  21,944,710 bytes, against the 21,892,913 bytes of unit 0030 and the limit of 30,000,000 (R10)
- 2026-11-22T01:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, wrote the documents. Read
  `CMakeLists.txt`, the lists of `src/` and `licenses/` and `LICENSE.md` of `piper-phonemize` at the tag `v1.6.1` from
  its pages on github.com, since the raw files and the archive of the tag could not be reached from the sandbox. The
  `CMakeLists.txt` holds `PIPER_PHONEMIZE_VERSION 1.6.1`, fetches eSpeak NG as the archive of commit `ed530aa1...` of
  the fork, links it with its `ucd-tools` and names no other software, `src/uni_algo.h` and `licenses/uni-algo` are in
  the tree, and `LICENSE.md` is the GPL, version 3 of 29 June 2007, so the licenses of `THIRD-PARTY.md` stand. The
  commit `5f92f0b` of the tag was taken from the plan and not read again. `THIRD-PARTY.md` and `README.md` name 1.6.1
  for all three jars and no longer the project's own build. `README.md` says that the plugin needs glibc 2.16 or newer
  on Linux: `objdump -T` gave 2.14 for the library of piper-phonemize 1.6.1 and 2.16 for `libsherpa-onnx-jni.so` and
  `libonnxruntime.so` of sherpa-onnx 2.0.2, and that Debian 12, Ubuntu 22.04 and RHEL 9 have glibc 2.36, 2.35 and 2.34
  is from their releases and was not checked here beyond Debian 12 and the Ubuntu 22.04 image the specifier used. The
  size of the archive in `README.md` is the 21,944,710 bytes found above. `README.md` and `docs/reading-markdown.md`
  say how characters above U+FFFF are read, and the open question of that section is answered. `git grep` for
  `U+FFFF`, `1.5.9`, `build-piper` and `glibc 2.38` finds, outside `changes/` and the tests, only the lines that
  describe the history. `developer-checks.md` is written

## Amendments
