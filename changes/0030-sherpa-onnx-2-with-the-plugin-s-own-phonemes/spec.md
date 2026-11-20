# sherpa-onnx 2 with the plugin's own phonemes

## Context

The plugin speaks through sherpa-onnx 1.13.8. As the plugin configures it, the Kokoro front end of 1.13.8 hands the
text of each piece whole to eSpeak NG, which turns any text into phonemes, in the language `en-us`, the voice that
the metadata of the model names. It does not look in the lexicon the plugin gives it, `lexicon-us-en.txt`, which it
reads only where no language is set at all: `offline-tts-kokoro-impl.h` of `v1.13.8` takes the voice of the metadata
when the configuration names no language, and `ConvertNonChineseToTokenIDs` of `kokoro-multi-lang-lexicon.cc` hands
the text to eSpeak NG whenever a language is set. Numbers, abbreviations, names, words of code and symbols all reach
the voice that way. This spec first said that 1.13.8 took each word from the lexicon and gave eSpeak NG only the
words it lacks. The implementer found that false, from the source and from the debug output of the engine, which
gave "bᵻfˌoːɹ" for "before" where the lexicon holds "bɪfˈɔɹ".

sherpa-onnx 2.0 removed eSpeak NG. The maintainer announced it in issue 3731 of 2026-07-08 as a breaking change and
merged it in pull request 4055 on 2026-10-20: eSpeak NG is licensed under the GPL, which does not go with the
Apache-2.0 license of sherpa-onnx. Since then the front end looks words up in the lexicon only and drops every word
the lexicon lacks. In issue 4139 the maintainer wrote that sherpa-onnx ignores such words and will not hold a
converter of its own for them. Instead, sherpa-onnx 2.0 can be handed the phonemes of each sentence in place of its
text, through `phonemeCodepoints` or `tokens` of the `GenerationConfig` of its Java binding. The maintainer publishes
the removed part as a package of its own, `piper-phonemize`, under GPL-3.0, which wraps eSpeak NG and has a Java
binding with native libraries for Linux x64 and macOS on Apple Silicon, released like those of sherpa-onnx.

The planner of unit 0028 found the cost of the move in a trial: under 2.0.2, sherpa-onnx skipped numbers ("3",
"2026", "4.2", "5%"), abbreviations ("API", "README", "npm"), words of code ("x86_64", "v1.13.8") and symbols ("&").
"I have 3 cats in 2026." made 60,016 samples under 1.13.8 and 25,762 under 2.0.2, against 23,498 for "I have cats
in.". Unit 0028 needs sherpa-onnx 2.0 for its forced aligner, which 1.13.8 does not have.

The plugin is licensed under GPL-3.0-or-later already, because the native library of sherpa-onnx 1.13.8 contains
eSpeak NG, as `README.md` and `THIRD-PARTY.md` say. The plugin may therefore use eSpeak NG itself.

Unit 0014 was written to move to the first release with the fix of the Java binding for characters above U+FFFF,
pull request 4062 of sherpa-onnx, and to take the workaround of unit 0006 out. That pull request was merged on
2026-10-20, after pull request 4055, so sherpa-onnx 2.0 holds it. The human cancelled unit 0014 for this unit.

Pull request 4062 fixed the binding of sherpa-onnx only. On 2026-11-20 the specifier found that the Java binding of
`piper-phonemize` 1.5.9 has the same fault: its `nativeText` in `jni/jni.cc` reads the text with `GetStringUTFChars`,
which gives modified UTF-8. The sentence "Before the sign, X and after the sign." gave these phonemes for X, through
the Java binding and through the C library of the same release with the text in standard UTF-8:

| X | Through the Java binding | Through the C library |
|---|---|---|
| none | `bᵻfˌoːɹ ðə sˈaɪn, ænd ˈæftɚ ðə sˈaɪn.` | the same |
| ❤ U+2764 | `... sˈaɪn, ɹˈɛd hˈɑːɹt ænd ...`, "red heart" | the same |
| 😀 U+1F600 | the same as with none | `... sˈaɪn, ɡɹˈɪnɪŋ fˈeɪs ænd ...`, "grinning face" |
| U+20000 | the same as with none | "letter 2 0 0 0 0" |
| U+10330 | the same as with none | "letter 1 0 3 3 0" |
| U+1D400 | the same as with none | "letter 1 D 4 0 0" |

Through the Java binding, eSpeak NG leaves such a character out, with no babble and nothing in its place, which is
what the workaround of unit 0006 does now. The human reports the fault to `piper-phonemize` and fixes it there, and
does not want this unit to wait for a release with the fix.

The Java archive of `piper-phonemize` 1.5.9 holds the data of eSpeak NG for every language itself, `espeak-ng-data`,
and is 9,042,593 bytes, of which the Russian dictionary alone is 8.5 MB unpacked and the English one 167 KB. The
planner of this unit found that the plugin archive would then reach about 30.9 MB and fail the limit of 30 MB that
unit 0004 set. Both model packages hold `espeak-ng-data` as well. Given that copy, the C library of
`piper-phonemize` 1.5.9 gave the same phonemes as its Java binding gave with its own data, for the sentences the
specifier tried.

The planner also found that the native library of `piper-phonemize` 1.5.9 for Linux x64 needs glibc 2.38, for
`__isoc23_strtoul`, and could not be loaded in the sandbox, Debian 12 with glibc 2.36. Those of sherpa-onnx 1.13.8
and 2.0.2 need 2.16. The plugin would no longer speak on Debian 12, Ubuntu 22.04 or RHEL 9. The workflow
`jar.yaml` of `piper-phonemize` builds that library on the runner `ubuntu-latest` itself, where sherpa-onnx builds
its own in the container image `quay.io/pypa/manylinux2014_x86_64`, whose glibc is 2.17. The C library of the same
release, in `piper-phonemize-v1.5.9-linux-x64-shared-lib.tar.bz2`, needs glibc 2.14 only. The human asks the
maintainer to build the Java library the way sherpa-onnx does, and does not want this unit to wait for it. The
library for macOS needs macOS 11.0, as sherpa-onnx does.

Issue 4178 of sherpa-onnx reports that phonemes handed in are cut every 401 tokens of a sentence, wherever that falls,
also in the middle of a word.

This unit is built before units 0028 and 0029. The human chose that order, so that both build on the front end of
this unit rather than on that of 1.13.8. The human set unit 0029 back to `draft`, to be corrected once this unit is
implemented and what it brought is known.

## Requirements

1. The plugin uses sherpa-onnx 2.0.2 or a later 2.x release, for the Java binding and for the native libraries of
   both platforms, and `piper-phonemize` 1.5.9 or a later 1.x release, for its Java binding and its native library for
   macOS on Apple Silicon. Its native library for Linux x64 is that of requirement 16.
2. The plugin hands the engine the phonemes of each piece and not its text. It finds them itself, as sherpa-onnx
   1.13.8 found them for the plugin: it makes the replacements of punctuation that the Kokoro front end of 1.13.8
   makes before eSpeak NG, gives the text to eSpeak NG whole through `piper-phonemize`, in the language `en-us` and
   with the data of eSpeak NG of requirement 17, takes one row of phonemes for each sentence eSpeak NG returns,
   leaves out each code point that is not a token of the model, and joins a short row to the row before it as 1.13.8
   does. The plugin does not use the lexicon of the model.
3. Every word of the text handed to the engine is spoken, numbers, abbreviations, words of code and symbols among
   them. A test with the real
   engine makes each of these sentences under this unit and finds that each gives an audio whose length lies within
   10% of the length 1.13.8 gives for it, as measured on the unit's branch before the move and written in the record:
    * "I have 3 cats in 2026."
    * "Version 4.2 is 5% faster & smaller."
    * "Read the README of the API with npm."
    * "It builds for x86_64 and ARM64 with v1.13.8."
    * "AloudRite reads Markdown aloud."
    * "Before the sign, and after the sign."
4. No piece hands the engine more than 400 phoneme tokens in one sentence. A piece whose phonemes would exceed it is
   cut by the plugin between two words before it is handed over, and is read whole, with no word left out or cut.
5. Characters above U+FFFF are given to the front end as a space, as the workaround of unit 0006 does now, and a file
   of nothing but such characters and white space counts as empty, as it does now.
6. With the real engine and the voice of the plugin, the sentence "Before the sign, 😀 and after the sign." gives an
   audio whose length lies within 10% of that of "Before the sign, and after the sign.", and so does the same sentence
   with U+20000, U+10330 or U+1D400 in place of the emoji.
7. A test with the real Java binding of `piper-phonemize` in use gives it "Before the sign, 😀 and after the sign." in
   the language `en-us`, writes to its output whether the phonemes hold those of "grinning face", `ɡɹˈɪnɪŋ fˈeɪs`, and
   does not fail on either outcome. The record holds what it wrote.
8. Everything else the earlier units established still holds: the model packages, their checksums and their folders,
   with no new download; the voice and the speed; the first sound before the whole file is made; the stop within a
   second; pause, resume and the change of the speed; the reading time and the highlights; the playing of samples that
   are not a number as silence; the number of threads; and the reading of a file of several pages without a silence
   in the middle of a sentence.
9. Starting a reading, from the press to the first audio, takes no more than 50 ms longer than before this unit, on
   the machine of `README.md` with the fp32 model, measured on the unit's branch before and after the move and written
   in the record.
10. `THIRD-PARTY.md` names the releases of sherpa-onnx and `piper-phonemize` in use, and the software their native
    libraries and archives hold, with the license of each, as taken from their build files at the tags of those
    releases. `README.md` names them where it names 1.13.8, says why the plugin is licensed under GPL-3.0-or-later in
    the terms of the new libraries, and states the growth of the plugin archive. It says that the native library of
    `piper-phonemize` for Linux x64 is built by the project, why, and how it is built again.
11. `README.md` and `docs/reading-markdown.md` say why characters above U+FFFF are left out in the terms of the new
    libraries: the Java binding of `piper-phonemize` does not hand them intact to eSpeak NG, as the binding of
    sherpa-onnx 1.13.8 did not hand them to the engine.
12. Automated tests cover the front end of requirement 2 on plain values, with a stand-in for eSpeak NG that gives
    set rows of phonemes: each replacement of punctuation, the full-width marks among them; a code point that is not a
    token, which is left out; the reading of the tokens of the model, the space among them; a piece of white space
    only, which gives no row; the joining of a short row to the row before it, at and on both sides of each of its
    bounds, and the first row, which stands alone; and a character above U+FFFF, which reaches the front end as a
    space. They cover the cut of requirement 4 at, below and above the limit, a word whose phonemes alone exceed it,
    and a long text whose rows hold every word in order.
13. The tests with the real engine of requirements 3 and 6, the test of requirement 7, and the tests with the real
    engine of the earlier units, have run with the model in place, and not been skipped, on Linux x64 and on macOS on
    Apple Silicon, and the record holds the lengths of audio found for the sentences of requirements 3 and 6.
14. The developer has listened to `samples/markdown-features.md` on Linux x64 and on macOS on Apple Silicon, and the
    record holds anything that sounded different from 1.13.8.
15. Before the status is set to `implemented`, the record holds an entry that names, for unit 0028 and for unit 0029,
    each requirement and decision of their specs that this unit has made untrue, unneeded or open to a simpler way,
    with what this unit brought that causes it, so that the human can have those specs corrected.
16. The native library of `piper-phonemize` for Linux x64 is built by this project from the source of the release in
    use, at its tag, with the options of the release's own build of that library in `jar.yaml`, in the container
    image `quay.io/pypa/manylinux2014_x86_64`. A script in the project makes it with Docker, given the tag, and
    packs it as the release packs its native library for Linux x64. The script fails when the library it made needs
    a symbol version of glibc newer than 2.17. The archive it makes is committed with the tag it was made from and its
    SHA-256, and the build of the plugin fails when the archive does not match that checksum.
17. The Java archive of `piper-phonemize` goes into the plugin without `espeak-ng-data`, and eSpeak NG reads the
    `espeak-ng-data` of the model folder in use. A test with the real binding finds that the phonemes of the
    sentences of requirement 3 and of "Before the sign, ❤ and after the sign." are the same with that data as with
    the data of the Java archive as published.
18. The plugin archive stays within the limit of 30 MB of unit 0004.

## Decisions

- **The plugin finds the phonemes itself, with eSpeak NG for the whole text.** That is what 1.13.8 did inside the
  engine as the plugin configured it, so the voice says what it said, and it is the way the maintainer pointed to in issue 3731. The plugin
  is under the GPL already, so the license of eSpeak NG that made sherpa-onnx drop it does not bind the plugin.
  Rejected: staying on 1.13.8, which keeps unit 0028 from its aligner; staying on 1.13.8 for the speech and running
  the aligner of 2.0 in a process of its own, since two releases of sherpa-onnx cannot share the process of the IDE,
  which costs a helper process, its messages and a second set of native libraries; spelling out numbers,
  abbreviations, symbols and words of code in the plugin and leaving the rest out, which is a large job and still
  drops names; and waiting for sherpa-onnx to bring a fallback back, which its maintainer declined.
- **No lexicon.** The implementer built the front end that this spec first asked for, the lexicon first and eSpeak NG
  for the words it lacks, and found its phonemes differ from those of 1.13.8, and its lengths by up to 10.4%, with
  "It builds for x86_64 and ARM64 with v1.13.8." shorter than the limit of requirement 3 allows. Handing each
  sentence whole to `piper-phonemize` gave lengths within 6.6% of 1.13.8 for all six sentences of requirement 3. It
  keeps the voice as it was and needs no lookup and no cutting into words, so it is less code. The human decided it.
  Rejected: the lexicon first, for the reasons above, and widening the 10% of requirement 3 to let it pass.
- **Through `piper-phonemize`, the maintainer's own package.** It is the part sherpa-onnx 1.13.8 linked, published
  the same way and for the same platforms. Rejected: building eSpeak NG for two platforms in this project.
- **The data of eSpeak NG from the model folder, and the Java archive without its own.** The human decided it after the
  planner found the archive over its limit, which reverses the choice of the data that `piper-phonemize` holds
  itself. That choice rested on the specifier's claim that the copy in the model folder would save nothing, which
  was wrong: the build can leave the data out of the Java archive. The data of all languages but English would be
  carried for nothing, and the model folder already holds it, so the plugin needs no new download. Requirement 17
  checks that the two copies give the same phonemes, since a data file of eSpeak NG is made for the release it
  belongs to. Rejected: raising the limit of unit 0004, which would carry 9 MB that is never read.
- **The project builds the Linux library of `piper-phonemize` itself, for now.** The human chose it, so that the
  plugin keeps working where it works now without waiting for the maintainer. The library is built the way
  sherpa-onnx builds its own, in the same container image, from the same source and with the same options as the
  release, so it differs from the release only in the glibc it needs. It is committed with its checksum, rather than
  built at each build of the plugin, since a build of the plugin should not need Docker, and the native library is
  small. The script runs where Docker runs: when the sandbox has none, the implementer stops for the developer to
  run it on the host. The library for macOS is that of the release, which needs no more than sherpa-onnx does.
  Rejected: waiting for a release built on an older glibc, which may take long; and accepting that the plugin stops
  speaking on Debian 12, Ubuntu 22.04 and RHEL 9.
- **Within 10% of 1.13.8 as the test that every word is spoken.** A dropped word shortens a short sentence far more
  than that, as "I have 3 cats in 2026." showed, while the same phonemes made by another release of the engine give
  nearly the same length. The developer's listening of requirement 14 is the check of how it sounds. Decided by the
  specifier and open to the human.
- **The cut of requirement 4 between two words.** A cut by the engine in the middle of a word, as issue 4178 reports,
  would be heard. The plugin's pieces of at most 400 bytes mostly stay below the limit, so the cut is the rare case.
- **Characters above U+FFFF stay left out, and the unit does not wait for a fix.** The human chose it. The Java binding
  of `piper-phonemize` 1.5.9 drops them, so taking the workaround of unit 0006 out would change nothing that is heard,
  while it would make a file of nothing but emoji read as silence in place of the notification that there is nothing
  to read. A release with the fix may take days, weeks or months. The workaround stays at least until then. Whether
  such characters are then read by their names, as a unit of its own built on a fixed release, the human decides once
  that release exists. Requirement 6 checks that they leave no babble and
  nothing in their place. Requirement 7 tells, from the release in use, whether the fix has arrived, and fails on
  neither outcome, so that it does not stand in the way of this unit. Rejected: calling the C library of
  `piper-phonemize` through the foreign function interface of Java, which bypasses the binding but needs native
  access to be enabled for the plugin, which later releases of Java restrict further; and waiting for the fix.
- **This unit names what it changes for units 0028 and 0029.** The human asked to be reminded, once this unit is
  done, to correct those specs, when what it brought is known rather than guessed. The implementer is the one who
  knows it then, so the record holds it before the unit ends.
- **The check on macOS is required.** The unit changes the native libraries, which `AGENTS.md` names as a reason for
  it.
- **The cut into pieces of at most 400 bytes and the playing of samples that are not a number as silence stay**, as
  unit 0014 decided, since neither comes from the binding.
- **The move of unit 0014 is part of this unit, its reading of emoji is not.** Its fix comes with the move to 2.0,
  but the fault it fixed sits in `piper-phonemize` as well.

## Non-goals

- The forced aligner, which is unit 0028.
- The choice of the voice and the language of eSpeak NG for a British voice, which is unit 0029.
- Going back to the native library of the release for Linux x64 once one needs glibc 2.17 or older, which is a unit
  of its own.
- Correcting the specs of units 0028 and 0029. Requirement 15 only names what is to be corrected.
- Changing how a reading is cut into pieces, beyond the cut of requirement 4.
- Spelling out numbers or symbols in the plugin beyond what eSpeak NG does.
- Reading characters above U+FFFF, emoji among them. It may be a later unit, as the human decides once a release of
  `piper-phonemize` hands them intact. Such a unit's test would check that "Before the sign, 😀 and after the sign."
  is read with "grinning face".
- Making all emoji and symbols sound alike beyond what the engine does.
- Using anything else that sherpa-onnx 2.0 adds.

## Beyond the request

1. A one-line contribution to the documentation of sherpa-onnx, or a comment on issue 4139, that names the way of this
   unit for a GPL project in Java. It costs a short text. Without it, other users of the Java binding find the way
   themselves.
