---
kind: feature
status: in-progress
timestamp: 2026-11-20T00:33:58Z
supersedes: 0014-sherpa-onnx-with-the-fixed-binding
---

# sherpa-onnx 2 with the plugin's own phonemes

## Log

- 2026-11-20T00:33:58Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `537afd2`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0030, after 0029. Read on GitHub
  why sherpa-onnx 2.0 dropped eSpeak NG (issue 3731 and pull request 4055: its GPL license), the maintainer's answer in
  issue 4139, the phonemes that `GenerationConfig` of 2.0 takes, and the Java binding and releases of
  `piper-phonemize`. Found that pull request 4062, the fix of unit 0014, was merged after 4055 and is in 2.0. Decided
  with the human: the plugin's own fallback to eSpeak NG, this unit before units 0028 and 0029, and the work of unit
  0014 taken in, which the human cancelled for it. Decided by me and open to the human: the data of eSpeak NG from the
  model folder, and 10% of the length under 1.13.8 as the test that every word is spoken. Left beyond the request: a
  note for other users of sherpa-onnx. The status is `draft`
- 2026-11-20T00:43:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `13d6a1f`, at the human's request, added requirement 15: before the unit is implemented, its
  record names what of the specs of units 0028 and 0029 it has made untrue, unneeded or open to a simpler way, so that
  the human is reminded to have them corrected. Also wrote in the context that the human set unit 0029 back to
  `draft`, and that correcting the two specs is no part of this unit. The status is `draft`
- 2026-11-20T00:54:07Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `4ccf5c1`, at the human's request, tested the Java binding of `piper-phonemize` 1.5.9 for
  characters above U+FFFF and found the fault of pull request 4062 of sherpa-onnx in it: its `nativeText` reads the
  text with `GetStringUTFChars`, and eSpeak NG leaves such a character out, where the C library of the same release,
  given standard UTF-8, reads 😀 as "grinning face". Wrote the finding into the context. Decided with the human: the
  unit does not wait for a fixed release, which the human pursues upstream; the workaround of unit 0006 stays, so
  that requirements 5 to 7, 11 and the parts of 12 to 14 on emoji that came from unit 0014 are replaced by a check
  that such characters leave nothing in their place and a test that tells whether the release in use has the fix;
  reading them is left to a later unit, whose test checks that 😀 is read with "grinning face"; and eSpeak NG reads
  the data that `piper-phonemize` holds itself, in place of the copy in the model folder. The status is `draft`
- 2026-11-20T01:03:27Z [human] approved the spec. The status is `approved`
- 2026-11-20T01:09:56Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of the sandbox, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `fdbc721`, read `process.md`, the standards and
  `roles/planner.md` of the installed kit, and stopped before writing `plan.md`, because the tree and the releases
  the spec names leave open two things the plan needs and may not decide. The unit goes back to the specifier and to
  the human. Found from the release assets of sherpa-onnx `v2.0.2` and of `piper-phonemize` `v1.5.9`, which are
  published under `csukuangfj/piper-phonemize` and not under `k2-fsa`. First, the archive outgrows the limit of 30 MB
  that requirement 2 of unit 0004 set and that requirement 8 keeps: the Java binding and the two native libraries of
  sherpa-onnx 2.0.2 are 20,750,113 bytes against 20,164,193 for 1.13.8, and those of `piper-phonemize` 1.5.9 are
  9,631,992 more, 9,042,593 of them the Java archive with all of `espeak-ng-data`, of which `ru_dict` alone is 8.5 MB
  unpacked and `en_dict` 167 KB. With the archive of 20.7 MB that unit 0026 built, that gives about 30.9 MB, and
  `./gradlew buildPlugin` fails at its limit of 30,000,000 bytes. The spec takes the whole Java archive as given, in
  its decision on the data of eSpeak NG, and says nothing of the limit. Second, the native library of
  `piper-phonemize` 1.5.9 for Linux x64 needs glibc 2.38, for `__isoc23_strtoul`, where those of sherpa-onnx 1.13.8
  and 2.0.2 need 2.16. Loading it in the sandbox, Debian 12 with glibc 2.36, failed with ``GLIBC_2.38' not found``,
  so the plugin would no longer speak on Debian 12, Ubuntu 22.04 or RHEL 9, which it does now, and the tests of
  requirements 3, 6, 7 and 9 cannot run in the sandbox. The library for macOS needs macOS 11.0, as sherpa-onnx does.
  Also found: sherpa-onnx 2.0.2 warns that `data_dir` of Kokoro is ignored, and `GenerationConfig` takes the
  phonemes as `int[][]`, one row per sentence, which `PiperPhonemize.Result.getPhonemes` gives in that shape. The
  status is `approved`
- 2026-11-20T01:20:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `138b3e8`, at the human's request, corrected
  `spec.md` for what the planner found. Confirmed on the host that the native library for Linux x64 in
  `piper-phonemize-native-lib-linux-x64-1.5.9.jar` needs glibc 2.38, 2.36, 2.35 and 2.34 for `__isoc23_strtoul`,
  `arc4random`, `_dl_find_object` and `pthread_once`, that `libpiper_phonemize_core.so` of the same release needs
  2.14, and that the libraries of sherpa-onnx 1.13.8 need 2.16, and read that `jar.yaml` of `piper-phonemize` builds
  the library on `ubuntu-latest`, where `linux-jni.yaml` of sherpa-onnx uses `quay.io/pypa/manylinux2014_x86_64`.
  Decided with the human: the Java archive goes in without `espeak-ng-data` and eSpeak NG reads the copy in the
  model folder, which reverses the earlier choice, and the project builds the Linux library itself in that image
  until a release needs no newer glibc, without waiting for the maintainer. Added requirements 16 to 18 and changed
  requirements 1, 2 and 10 and the decisions to match. Decided by me and open to the human: the library is committed
  with its checksum rather than built at each build of the plugin, and the developer runs the script on the host
  where the sandbox has no Docker. The status is `approved`
- 2026-11-20T01:26:14Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `6c9ed76`, at the human's request, tried the build of
  requirement 16 on the host before planning, outside the project. In `quay.io/pypa/manylinux2014_x86_64`, CentOS 7.9
  with glibc 2.17, gcc 10.2.1 and CMake 4.4.4, `yum install -y java-11-openjdk-devel` gave the headers of JNI, with
  `JAVA_HOME` at `/usr/lib/jvm/java-11-openjdk-11.0.23.0.9-2.el7_9.x86_64`, and the source of `piper-phonemize` at the
  tag `v1.5.9`, commit `dae4d2d`, built with the options of `jar.yaml` and `-DCMAKE_BUILD_TYPE=Release`. The library
  `libpiper-phonemize-jni.so` is 1,048,032 bytes, needs no symbol version of glibc newer than 2.14, and links
  `libm`, `libpthread`, `libc` and `ld-linux-x86-64` only. Packed as the release packs it, under
  `piper-phonemize/native/linux-x64/`, it is 340,718 bytes. With `piper-phonemize-jvm-1.5.9.jar`, it loaded and
  phonemized in `eclipse-temurin:21-jdk-jammy`, Ubuntu 22.04 with glibc 2.35, where the library of the release does
  not load, and gave the same phonemes with the `espeak-ng-data` of the fp32 model folder as with the data of the
  Java archive, for "I have 3 cats in 2026.", "It builds for x86_64 and ARM64 with v1.13.8." and "Before the sign, ❤
  and after the sign.". The files the container wrote belong to root, so the script is to run Docker as the user of
  the host. The status is `approved`
- 2026-11-20T01:41:16Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `7085beb`, read `process.md`, the standards and
  `roles/planner.md` of the installed kit, and wrote `plan.md`. Read the Kokoro front end of sherpa-onnx at
  `v1.13.8`, the handling of `phonemeCodepoints` at `v2.0.2`, and the Java binding and C library of
  `piper-phonemize` at `v1.5.9`, and the plan has the plugin's front end do what that of 1.13.8 did. Found that
  `piper-phonemize` initializes eSpeak NG once per process and reads its data folder again at each call: tried on
  the host with the release's Linux library, a data folder deleted after the initialization made every call give
  only `.`. A reading, a change of the model and the removal of the first model in one session would then drop
  every word not in the lexicon. To keep eSpeak NG reading the data of the model folder in use, as requirement 17
  says, the plan initializes it with a folder of the plugin's own that holds a link to the `espeak-ng-data` of the
  model in use, pointed again at each creation of the engine. Decided in the plan as well: words are lowercased before
  eSpeak NG is given them, as 1.13.8 did; characters from U+4E00 to U+9FFF, which 1.13.8 dropped, go to eSpeak NG as
  any other word not in the lexicon; and the script runs the container as root, for `yum`, and gives the one file it
  copies out to the user of the host, rather than running Docker as that user. The status is `approved`
- 2026-11-20T01:45:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a unit run on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `4d2f9be`, read `process.md`, the standards, `roles/implementer.md`,
  `spec.md`, `plan.md` and this record, and began the work. The status is `in-progress`
- 2026-11-20T01:58:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch at `4d2f9be`
  plus the two new measurement classes, measured under sherpa-onnx 1.13.8 on the Linux host (AMD Ryzen 9 9950X3D, 6
  engine threads, Linux amd64). `EngineLengthsMeasurement` with the int8 model at the normal speed, through
  `withCharactersAboveFfffAsSpaces`, in samples at 24 kHz: "I have 3 cats in 2026." 60,305; "Version 4.2 is 5% faster &
  smaller." 80,202; "Read the README of the API with npm." 65,916; "It builds for x86_64 and ARM64 with v1.13.8." 138,351;
  "AloudRite reads Markdown aloud." 47,194; "Before the sign, and after the sign." 45,037; with a heart U+2764 56,273; with
  😀 45,256; with U+20000 45,191; with U+10330 45,032; with U+1D400 45,252. `FirstAudioMeasurement` with the fp32 model,
  for the piece "Markdown features": creation of the engine to the first audio 641, 621, 649, 640 and 647 ms, median
  641 ms, and the first creation in the process 644 ms; `speak` to the first audio of an engine made beforehand 168,
  135, 131, 131 and 135 ms, median 135 ms. `./gradlew buildPlugin` gave an archive of 20,727,090 bytes. The measured
  values vary a little from run to run (the emoji sentences differ by up to 219 samples), so the 10% of requirement 3 is
  wide against that. The status is `in-progress`
- 2026-11-20T02:50:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, wrote
  `scripts/build-piper-phonemize-linux-x64.sh` as planned and ran it with `v1.5.9` on the host, where Docker runs. The
  source is at commit `dae4d2d831f9f0817355f7c0e8abd98c2fb0a49d`. The library in the jar needs glibc 2.14 at most, links
  `libm`, `libpthread`, `libc` and `ld-linux-x86-64` only (`objdump -p`), and the jar
  `native/piper-phonemize-native-lib-linux-x64-1.5.9.jar` is 342,963 bytes with the SHA-256
  `74c2c9f052836906b973d868cfdbfefe38e59681a7c6b02fa0f1acd78d771310`. The jar is owned by the user of the host. Moved
  `build.gradle.kts` to sherpa-onnx 2.0.2 and `piper-phonemize` 1.5.9, with the stripped Java archive (17 entries and
  11,529 bytes, the manifest of the release as it is), the macOS library of the release, the committed Linux jar behind
  `verifyPiperPhonemizeLinuxX64` and the system property of the published jar for the tests. Checked that
  `./gradlew buildPlugin` fails with the checksum expected and the one found when one byte is appended to the jar, and
  passes with the jar as made, with an archive of 21,879,994 bytes. The status is `in-progress`
- 2026-11-20T03:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, wrote
  `KokoroFrontEndTest` first and then `KokoroFrontEnd.kt` as the plan decides. The 26 tests of the class pass
  (`./gradlew test --tests com.ritenoar.aloudrite.KokoroFrontEndTest`, none skipped). Read the Kokoro front end of
  1.13.8 again for the replacements, where `:` becomes `,` before the full-width colon becomes `:`, as the plan says.
  Deviations from the plan: none. A lexicon token of more than one code point leaves its line out, as a token that is
  not in `tokens.txt` does. The status is `in-progress`
- 2026-11-20T04:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, wrote
  `EspeakPhonemes`, the change of `KokoroSpeechEngine`, `EspeakPhonemesTest`, `PhonemesProbeProcess`, `EspeakDataTest`
  and the new cases of `KokoroSpeechEngineTest`, and found a premise of the spec and the plan to be false, so the unit
  stops here for the specifier and the human. Verified on the Linux host with the int8 model. First, requirement 17
  holds: `EspeakDataTest` finds the same phonemes for the six sentences of requirement 3 and the heart sentence with the
  data of the model folder and with that of the published Java archive. Requirement 7: `EspeakPhonemesTest` wrote that
  piper-phonemize 1.5.9 does not read U+1F600 as "grinning face", and that the phonemes of "Before the sign, 😀 and
  after the sign." are those of the sentence without it. Second, the false premise: the spec says that 1.13.8 took a
  word of `lexicon-us-en.txt` from the lexicon and gave eSpeak NG only the words the lexicon lacks. As the plugin
  configures the engine, it did not. `offline-tts-kokoro-impl.h` of `v1.13.8` sets the language to the `voice`
  metadata of the model, `en-us`, when `lang` is empty, and `ConvertNonChineseToTokenIDs` of the Kokoro front end then
  returns `ConvertTextToTokenIDsWithEspeak` for the whole text, before it looks at the lexicon. Run with the debug
  output on, in a worktree at `adb78f4`, the engine gave one row per sentence whose phonemes are those of eSpeak NG
  for the whole sentence ("bᵻfˌoːɹ ðə sˈaɪn", where the lexicon holds "bɪfˈɔɹ"), "v1.13.8" read as "vˈiː wˈʌn pɔɪnt
  wˈʌn θɹˈiːpɔɪnt ˈeɪt" with the dots inside it, and no line "Use espeak-ng to handle the OOV". So the plan's front end,
  which splits the words and looks each up in the lexicon, does not do what 1.13.8 did. The effect on requirement 3:
  with that front end, the samples for the six sentences are 62,280, 78,514, 61,291, 124,022, 45,388 and 48,472 against
  60,305, 80,202, 65,916, 138,351, 47,194 and 45,037 under 1.13.8, so "It builds for x86_64 and ARM64 with v1.13.8."
  is 10.4% shorter and `KokoroSpeechEngineTest` fails on it, and "Before the sign, and after the sign." is 7.6% longer.
  Tried outside the project, handing the engine the phonemes that `PiperPhonemize.text(sentence, "en-us")` gives for the
  whole sentence, one row per sentence it returns, with the tokens of the model: 61,120, 79,841, 61,662, 138,920, 48,278
  and 45,998 samples, within 6.6% of 1.13.8 for each of the six, with rows of 50, 61, 49, 117, 38 and 37 tokens. That is
  simpler than the plan's front end, needs no lexicon and no word splitting, and is what 1.13.8 did, but it changes
  requirement 2 and the decisions on the front end, the lexicon and the limit of 400 tokens, and it affects units 0028
  and 0029, so it is for the specifier and the human to decide. Not done: the comments of `EngineText.kt` and
  `PlacedPart.kt`, the run of the whole suite, the measurements under 2.0.2, the documents and the developer checks.
  The branch holds `KokoroFrontEnd` and its tests as the plan wrote them, and `KokoroSpeechEngineTest` fails on the
  x86_64 sentence. The status is `in-progress`
- 2026-11-20T01:58:29Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `e0693f6`, at the human's request, corrected `spec.md`
  for the false premise the implementer found. Checked it in the source at `v1.13.8`: `offline-tts-kokoro-impl.h`,
  line 238, takes the voice of the metadata when the configuration names no language, and
  `kokoro-multi-lang-lexicon.cc`, line 303, hands the text to eSpeak NG whenever a language is set. Decided with the
  human: the plugin hands each piece whole to eSpeak NG through `piper-phonemize` and uses no lexicon, as 1.13.8 did,
  and the front end that splits words and looks them up, with the tests of it that only that front end needs, is
  removed. Changed the context, requirements 2, 3 and 12, the first decision and a non-goal, and added the decision
  on the lexicon. Requirement 12 now names what of the removed tests stays as a test of the new front end: the
  replacements of punctuation, the code points that are not tokens, the tokens of the model, white space only, the
  joining of short rows, the character above U+FFFF and the cut of requirement 4. The status is `in-progress`
- 2026-11-20T02:02:16Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `d4af30c`, at the human's request, reworded the
  decision on characters above U+FFFF and its non-goal: reading them by their names may be a later unit, which the
  human decides on once a release of `piper-phonemize` hands them intact, and is no longer promised. The human wants
  no unit for it written now. The status is `in-progress`
- 2026-11-20T05:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch at `fbf632c`,
  brought the unit in line with the spec corrected in `d4af30c` and `fbf632c`. Rewrote the approach, the decisions on
  the rows and the engine, the files and the tasks of `plan.md`, and logged that the front end was first built with a
  lexicon at `305c815`. Removed `KokoroFrontEnd.kt` and `KokoroFrontEndTest.kt`, and with them the lexicon reading and
  the word splitting. Added `PhonemeRows.kt`, which replaces the punctuation as 1.13.8 did, gives the text to eSpeak NG
  whole through `EspeakPhonemes.sentencesOf`, takes a row for each sentence without the code points that are not
  tokens, cuts a row over 400 tokens at the last space and joins short rows, and `PhonemeRowsTest`, whose 19 tests
  cover requirement 12 with a stand-in for eSpeak NG. `KokoroSpeechEngine` no longer reads the lexicon. Verified on the
  Linux host with the int8 model: `PhonemeRowsTest` (19), `EspeakPhonemesTest` (3), `EspeakDataTest` (1) and
  `KokoroSpeechEngineTest` (6) pass with none skipped, so each of the six sentences of requirement 3 is within 10% of
  its length under 1.13.8 and the sentences of requirement 6 are within 10% of the one without a sign. Not verified: the
  joining rule for rows that come from the sentences of eSpeak NG, which is that of the lexicon path of 1.13.8 as the
  plan read it, since the source of the path 1.13.8 took could not be fetched in this run; the lengths agree with
  1.13.8 within 10%. Not done: the comments of `EngineText.kt` and `PlacedPart.kt`, the run of the whole suite, the
  measurements under 2.0.2, the documents, the developer checks and the entry of requirement 15. The status is
  `in-progress`
- 2026-11-20T02:11:17Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0030 at commit
  `160b9e77049d72133335ce695cd70988e8955e89`, against merge base `fdbc721`, and returned S1, S2, S3 and S4 in
  `.reviews/0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes/review-20261120T021013Z.md`. Ran the full Linux suite
  (872 tests, no failures, six skipped opt-in measurements; the real-engine and phonemizer tests were not skipped),
  the plugin build, the lengths and first-audio measurements, and the change check. Could not independently verify
  macOS execution, developer listening on either platform, the historical measurements, a Docker rebuild of the
  native library or the checksum-failure experiment. The status remains `in-progress`
- 2026-11-20T06:30:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, for
  requirement 15, read `spec.md` of units 0028 and 0029 and names what this unit has made untrue, unneeded or open to
  a simpler way, so that the human can have them corrected. Nothing of those specs is edited. For unit 0028: (a) the
  context says 2.0.2 skips words that are not in the lexicon, and requirement 5 asks that every word 1.13.8 speaks is
  still spoken; this unit does both, since the plugin hands the engine the phonemes of eSpeak NG (requirement 3 of this
  unit), so requirement 5 and its test with the real engine are done here and are unneeded there. (b) The decision
  "Forced alignment over the durations of the model" rejects running the model through ONNX Runtime itself because it
  would mean rebuilding the conversion of text to phonemes that sherpa-onnx does with eSpeak NG and its lexicons. This
  unit has built that conversion, with `PhonemeRows` and `EspeakPhonemes`, so the plugin now holds the phoneme tokens
  of each row and the spaces between words, which leaves the mapping of the durations back to the words and the
  running of the model through ONNX Runtime as the cost, and the rejection is open to a second look. (c) The context
  says sherpa-onnx 2.0.2 is the newest release and that moving to it is a risk; the move is made. (d) Requirement 9
  measures the first audio against "before this unit", and its baseline is now the state after this unit. (e)
  Requirement 10 and the third-party software it names are listed in `THIRD-PARTY.md` as this unit rewrote it for
  2.0.2, with `piper-phonemize` added, and the context there that 1.13.8 turned "AloudRite" into phonemes through
  the lexicon is wrong, since 1.13.8 gave the whole text to eSpeak NG. For unit 0029: (a) the context says a British
  voice with the American lexicon pronounces words the American way, and requirement 4 and the decision on the lexicon
  have the lexicon follow the voice. This unit found that 1.13.8, as the plugin configured it, never read the lexicon
  for English, since the language defaulted to the `voice` metadata `en-us` and the whole text went to eSpeak NG, and
  the plugin no longer uses the lexicon either; so the British pronunciation that the specifier heard probably came
  from the language `en` alone, which is not verified here, and `lexicon-gb-en.txt` and its place in requirements 4
  and 7 are probably unneeded. (b) The language is now the one the plugin gives `PiperPhonemize.text`, in
  `EspeakPhonemes`, where it is the constant `en-us`, and is not part of the configuration of the engine, so the
  accent can change at each piece. Requirement 6, which keeps one engine and creates it again for the other accent, the
  decision that a change of the voice is heard at the next reading because a new engine takes seconds, and the
  configuration of the engine in requirement 10 are open to a simpler way: only the speaker number and the language of
  the phonemes differ between voices, and neither needs a new engine. (c) The finding that `en-gb` makes the engine give
  an empty audio concerns the engine of sherpa-onnx 1.13.8; whether `PiperPhonemize.text` takes `en-gb` or only `en` was
  not tried. (d) Requirement 11, the test with the real engine, has to run the phonemes of the plugin and name the
  language of `EspeakPhonemes` and not a configuration of the engine. (e) Requirement 9, "the comments in the code no
  longer say that the plugin reads in `af_heart` only", is unchanged. The status is `in-progress`
- 2026-11-20T06:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, recorded
  the outcome of the four findings of the review of `160b9e7`, each accepted by the definition of the orchestrator for
  round 2 and not decided by the human. S1, accepted and fixed: `PhonemeRows` now adds the space token after each full
  stop token before it counts, cuts and joins, as `PiperPhonemesToIdsKokoroOrKitten` of 1.13.8 does, which the
  implementer read from the source at `v1.13.8` after the review fetched it; the tests count rows of dotted sentences
  (36 and 9 raw tokens stand apart as 37 and 10, 35 and 8 are joined as 45) and a cut with the spaces added, and the
  plan describes the algorithm. After it, the six sentences of requirement 3 made 60,330, 80,199, 65,913, 138,250,
  47,194 and 45,252 samples against 60,305, 80,202, 65,916, 138,351, 47,194 and 45,037 under 1.13.8, each within 0.5%,
  where before the fix the largest difference was 6.6%. The sentences of requirement 6 made 56,200 (heart), 45,254,
  45,033, 45,254 and 44,949 samples against 45,252 without a sign. S2, accepted and done: `THIRD-PARTY.md` names
  sherpa-onnx 2.0.2 and `piper-phonemize` 1.5.9 and what their build files at those tags name, `README.md` names the
  releases, the GPL reason in terms of `piper-phonemize`, the Linux library and how it is built again, the archive size
  and the two new measurements, `docs/reading-markdown.md` gives the reason for the characters above U+FFFF, and the
  comment of `EngineText.kt` names the binding of `piper-phonemize` (`PlacedPart.kt` names no binding and is not
  changed). The licenses of kissfft, kaldi-native-io, kaldifst and kaldi-hmm-gmm were read from their repositories,
  and which libraries named by `cmake/` are in the binaries was not checked. S3, accepted in part: on the final tree
  `./gradlew test` ran 875 tests with no failure and 6 skipped, which are the opt-in measurements and
  `RealModelPackageTest`; `KokoroSpeechEngineTest` (6), `EspeakPhonemesTest` (3), `EspeakDataTest` (1) and
  `PhonemeRowsTest` (22) were not skipped; the plugin archive is 21,892,913 bytes against 20,727,090 before. The first
  audio with the fp32 model, six threads, after the move: creation of the engine to first audio 663, 683, 680, 680 and
  657 ms, median 680 ms against 641 ms before (+39 ms); `speak` to first audio of an engine made beforehand 149, 128,
  127, 128 and 133 ms, median 128 ms against 135 ms; the first creation in the process 711 ms against 644 ms (+67 ms),
  and 854, 853 and 848 ms in three runs with eSpeak NG made ready on a thread of its own while the model loads, which
  was worse and was reverted. The part the plugin adds is small: initializing eSpeak NG took 5 to 6 ms, a first
  phonemization 1 ms, and `OfflineTts` of 2.0.2 took 675 to 679 ms to create. So the medians of requirement 9 are within
  50 ms and the first creation in a process is not, by 17 ms in this run and more in the reviewer's, and the cause is the
  creation of `OfflineTts` in 2.0.2 and not the plugin's own work; the task of making it faster is left open, and the
  decision whether that is acceptable is the human's. The measurement starts at the creation of the engine and not at
  the press. The developer checks of requirements 13 and 14 are in `developer-checks.md` and wait for the developer. S4,
  accepted and done: the entry for requirement 15 is above. The status is `in-progress`
- 2026-11-20T02:21:28Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0030 at commit
  `f746b19cfd02c453c2adfdb803a4e5b729747c4b`, from the previously reviewed commit `160b9e7`, checking S1 to S4 and
  returning the remaining S3 in `.reviews/0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes/review-round2-20261120.md`.
  Ran the Linux suite (875 tests, no failures, six skipped opt-in or real-package cases; no real-engine or phonemizer
  case skipped), the plugin build, the length and first-audio measurements, and the change check. Could not
  independently verify macOS execution, developer listening, historical baseline runs, the transient startup
  diagnostic experiment, every dependency license or binary provenance, a Docker rebuild or the historical
  checksum-failure experiment. The status remains `in-progress`
