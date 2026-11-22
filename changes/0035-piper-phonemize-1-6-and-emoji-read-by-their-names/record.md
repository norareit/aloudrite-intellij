---
kind: feature
status: implemented
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
- 2026-11-22T01:25:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran the suites on the final code
  tree `94d1f9d` (the commit after it changes only change documents): `./gradlew test` gave 1060 tests, 0 failed, 8
  skipped (the measurements and `RealModelPackageTest`), `./gradlew buildPlugin` passed with an archive of 21,944,710
  bytes, and `npm run check-changes` passed on the commit before this entry. The unit waits for the developer's checks
  of `developer-checks.md` (R14 on macOS on Apple Silicon, R15 on both platforms) and for the review. Not checked: the
  editor and the preview of the sandbox IDE were not looked at with an emoji read, so that the highlight of the words
  around it is checked only by the tests on plain values and the real aligner, and by the developer's listening. The
  status stays `in-progress`
- 2026-11-21T23:40:28Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a full review of unit 0035 at
  `255793236ab645a59079bd2094faf9106add2a9e` against merge base `e725b7b14cfc3f9390f39f274a57e2c16246700a` with
  `origin/master`. Returned S1 and C1 in `.reviews/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/review-round-1-sol.md`.
  Independently ran `./gradlew test --rerun-tasks` (1060 tests, 0 failures, 8 skipped, none of the real binding, engine
  or aligner tests skipped), `./gradlew buildPlugin` (21,944,802 bytes), `npm run check-changes` and the native glibc
  inspection. Could not independently verify the earlier 1.5.9 runtime capture, upstream license/build-file provenance,
  macOS execution, listening or the live IDE highlights. The status stays `in-progress`
- 2026-11-22T02:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took the findings of
  `.reviews/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/review-round-1-sol.md`. C1 (the code-point loop of
  `wordsOf` no longer counted U+00A0, U+2007 and U+202F as white space): accepted by the definition of the
  orchestrator, not by a decision of the human, and fixed by adding `Character.isSpaceChar` to the test of white space
  in `ReadingSegments.kt`, with `noBreakSpacesSeparateWordsAsOtherWhiteSpaceDoes` in `ReadingSegmentsTest`, which
  checks the words and their places for those three separators and a space. The test was written with the fix and was
  not run against the code before it. `./gradlew test` on the tree of this entry's commit: 0 failed. S1 (the
  developer's checks of R14 and R15 are outstanding): agreed, and nothing to apply, since only the human can make
  them; they stay open in `developer-checks.md` and the plan. The status stays `in-progress`
- 2026-11-21T23:45:07Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a delta review of unit 0035 at
  `2a099f4abc86bbe46b07f452393d78e6c0860ead`, compared with `255793236ab645a59079bd2094faf9106add2a9e` from the
  first review. Verified C1 corrected; returned outstanding S1 and new non-blocking C2 in
  `.reviews/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/review-round-2-sol.md`. Independently ran
  `./gradlew test --rerun-tasks` (1061 tests, 0 failures, 8 skipped, none of the real binding, engine or aligner tests
  skipped), `./gradlew buildPlugin` (21,944,816 bytes), `npm run check-changes` and the delta whitespace check.
  Could not verify macOS execution, developer listening, live IDE highlights or execution of the new test with the
  pre-fix predicate. The status stays `in-progress`
- 2026-11-22T02:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took the findings of
  `.reviews/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/review-round-2-sol.md`. C2 (the regression test was
  not shown failing before the fix): accepted by the definition of the orchestrator, not by a decision of the human.
  The test-first order of that fix did not happen and is not claimed. As evidence after the fact, the fix was taken
  out of `ReadingSegments.kt` for one run, leaving `Character.isWhitespace(codePoint)` alone: `ReadingSegmentsTest`
  ran 27 tests and `noBreakSpacesSeparateWordsAsOtherWhiteSpaceDoes` failed, the only failure. With the fix back, the
  27 tests pass, and the file is as committed. S1 (the developer's checks of R14 and R15): unchanged, only the human
  can make them. The status stays `in-progress`
- 2026-11-21T23:48:13Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a delta review of unit 0035 at
  `05adc695163c7f52816a6c3195f0c69b9c6c12c2`, compared with `2a099f4abc86bbe46b07f452393d78e6c0860ead` from the
  second review. Reviewed the record-only delta and C2's recorded follow-up; returned outstanding S1 and no new
  findings in `.reviews/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/review-round-3-sol.md`. Independently
  ran `./gradlew test --rerun-tasks` (1061 tests, 0 failures, 8 skipped, none of the real binding, engine or aligner
  tests skipped), `npm run check-changes` and the delta whitespace check. Confirmed code, tests and build configuration
  unchanged from the second review. Could not independently verify the historical failing-test run, macOS execution,
  developer listening or live IDE highlights. The status stays `in-progress`
- 2026-11-22T00:24:07Z [human] accepted C1 of
  `.reviews/0035-piper-phonemize-1-6-and-emoji-read-by-their-names/review-round-1-sol.md` with the fix as committed in
  `2a099f4`. The status stays `in-progress`
- 2026-11-22T00:43:04Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, ran the checks of
  `developer-checks.md` on macOS on Apple Silicon (`arm64`, Darwin 24.1.0) on `115d3a9`, with the int8 model, the fp32
  model and the aligner model in place. `./gradlew test`: 1061 tests, 0 failures, 0 errors, 8 skipped, which are the
  seven measurements and `RealModelPackageTest`, so `EspeakPhonemesTest` (6), `EspeakDataTest` (1),
  `KokoroSpeechEngineTest` (6) and `RealAlignerTest` (2) ran, none skipped, and no test failed (R14).
  `EngineLengthsMeasurement` with the int8 model, `Mac OS X`, `aarch64`, gave for "Before the sign, and after the
  sign." 45,188 samples, 1.883 s, and for "Before the sign, 😀 and after the sign." 65,189 samples, 2.716 s, which is
  1.443 times as many (R6). The other sentences: "I have 3 cats in 2026." 60,305 samples, 2.513 s; "Version 4.2 is 5%
  faster & smaller." 79,443 samples, 3.310 s; "Read the README of the API with npm." 65,916 samples, 2.747 s; "It
  builds for x86_64 and ARM64 with v1.13.8." 138,140 samples, 5.756 s; "AloudRite reads Markdown aloud." 47,194
  samples, 1.966 s; ❤ 56,325 samples, 2.347 s; U+20000 45,033 samples, 1.876 s; U+10330 44,918 samples, 1.872 s;
  U+1D400 77,324 samples, 3.222 s. Concern: on Linux x64 U+20000 and U+10330 gave 84,857 and 84,130 samples, read as
  "letter" with the digits of their code point, and on macOS they give no more audio than the sentence without a sign,
  which suggests they are not read there at all, against what R12 has `README.md` and `docs/reading-markdown.md` say.
  The emoji and U+1D400 give the same lengths on both platforms. The cause was not looked into, and the phonemes of
  those two characters on macOS were not checked. The developer listened to `samples/markdown-features.md` on macOS
  on Apple Silicon with the fp32 model: the three emoji of the section "Emoji of four bytes" were read by their names,
  the highlight fell on the words around them in the editor and in the preview, and nothing else sounded different
  from before this unit or broken (R15 on macOS). The sample has no character above U+FFFF without a name, so the
  listening did not cover the concern above. The listening on Linux x64 is not recorded yet. The status stays
  `in-progress`
- 2026-11-22T00:49:14Z [human] accepted as a limitation of this unit that on macOS on Apple Silicon U+20000 and U+10330
  give no audio of their own, where on Linux x64 they are read as "letter" with the digits of their code point. The
  cause is not looked into, and `README.md` and `docs/reading-markdown.md` stay as they are. Decided as well that ✓,
  which is silent as it was before this unit, stays so, with no unit of its own. The status stays `in-progress`
- 2026-11-22T00:53:54Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, recorded the developer's listening
  on Linux x64 with the fp32 model: the three emoji of the section "Emoji of four bytes" were read by their names, each
  emoji was highlighted as the other words are while its name was heard, the highlight was right in the editor and in
  the preview, and nothing else sounded different from before this unit. ✓ stayed silent, as it was before this unit
  (R15 on Linux x64). The developer's checks on macOS are in the entry of 2026-11-22T00:43:04Z, and no test failed
  there, so none needs to be traced to this unit. Ticked the last task, the last verification and the last commit point
  of `plan.md`. No blocking finding is left: S1 is answered by the developer's checks, C1 was accepted by the human, C2
  does not block, and the difference of U+20000 and U+10330 on macOS was accepted by the human as a limitation. The code,
  tests and build configuration are unchanged since `05adc69`; `./gradlew cleanTest test` on `ba889ba`, in the sandbox
  with both models and the aligner model in place, gave 1061 tests, 0 failed, 8 skipped, which are the measurements and
  `RealModelPackageTest`, and `EspeakPhonemesTest` (6), `EspeakDataTest` (1), `KokoroSpeechEngineTest` (6) and
  `RealAlignerTest` (2) ran with none skipped. The status is `implemented`

## Amendments
