# piper-phonemize 1.6 and emoji read by their names

## Context

Unit 0030 moved the plugin to sherpa-onnx 2.0.2, which takes phonemes in place of text, and has the plugin make the
phonemes itself with eSpeak NG through `piper-phonemize` 1.5.9, the maintainer's package. Two faults of that release
shaped unit 0030:

* Its Java binding read the text with `GetStringUTFChars`, which gives modified UTF-8, and eSpeak NG left every
  character above U+FFFF out, most emoji among them. The plugin therefore kept the workaround of unit 0006, which
  gives such characters to the front end as spaces, and a test that writes whether the release in use reads 😀 as
  "grinning face" without failing on either outcome. Whether such characters are read by their names once a release
  hands them intact to eSpeak NG was left to the human to decide then.
* Its native library for Linux x64 needed glibc 2.38 and did not load on Debian 12, Ubuntu 22.04 or RHEL 9. The
  project therefore builds that library itself, with a script, in the container image
  `quay.io/pypa/manylinux2014_x86_64`, and commits it with its SHA-256, which the build checks.

The human had both fixed upstream. `piper-phonemize` 1.6.0 and 1.6.1, both of 2026-11-21, hold pull request 43, which
reads Java strings as UTF-16 and hands eSpeak NG standard UTF-8, and pull request 45, which builds the Linux JNI
libraries in the manylinux2014 images, so that they need glibc 2.17 at most. 1.6.1 adds no change of its own beyond
the release. The specifier tried 1.6.1 outside the project on 2026-11-21, with its Java archive and its native library
for Linux x64 and the `espeak-ng-data` of the int8 model folder:

* The native library needs no symbol version of glibc newer than 2.14
* "I am 😀 happy." gave `aɪɐm ɡɹˈɪnɪŋ fˈeɪs hˈæpi.` in the language `en-us`, and "I am happy." gave `aɪɐm hˈæpi.`,
  on the host and in `eclipse-temurin:21-jdk-jammy`, Ubuntu 22.04 with glibc 2.35, where the library of 1.5.9 does
  not load

Unit 0030 found, through the C library of 1.5.9, that eSpeak NG reads U+20000, U+10330 and U+1D400, which have no
name in its data, as "letter" followed by the digits of their code point, such as "letter 2 0 0 0 0". Characters of
three bytes such as ❤ and ⚓ already reach eSpeak NG intact and are read by their names, "red heart" and "anchor".

The human decided to move to the fixed release and to have characters above U+FFFF read as eSpeak NG reads them, in
a unit of its own.

## Requirements

1. The plugin uses `piper-phonemize` 1.6.1 or a later 1.x release, for its Java binding and for its native libraries
   for Linux x64 and for macOS on Apple Silicon, all as that release publishes them. It keeps sherpa-onnx 2.0.2 or a
   later 2.x release.
2. The project builds no native library of `piper-phonemize`: the script that built it, the committed archive and
   the check of its checksum are gone.
3. The record holds the newest symbol version of glibc that the native library for Linux x64 in use needs, and it is
   2.17 or older. The tests with the real binding and the real engine run in the sandbox, Debian 12 with glibc 2.36,
   and are not skipped there.
4. A character above U+FFFF reaches eSpeak NG as it stands in the file. The plugin no longer gives such characters to
   the front end as spaces, and the words it hands on keep them.
5. A test with the real binding gives "Before the sign, 😀 and after the sign." in the language `en-us` and fails
   unless the phonemes hold those of "grinning face", `ɡɹˈɪnɪŋ fˈeɪs`. It takes the place of the test of unit 0030
   that only reported it.
6. With the real engine and the voice of the plugin, "Before the sign, 😀 and after the sign." gives an audio more
   than 10% longer than that of "Before the sign, and after the sign.". The record holds both lengths.
7. A file of nothing but characters above U+FFFF and white space is read, by the names eSpeak NG gives them, and is
   no longer a file with nothing to read. A file of nothing but white space still is one.
8. Characters above U+FFFF change nothing of how the Markdown is read: four emoji at the start of a line are still a
   paragraph and not an indented code block, and the highlights of the words before and after such a character fall
   on those words in the editor and in the preview, as they do now.
9. The phonemes that the plugin hands the engine for the six sentences of requirement 3 of unit 0030 and for
   "Before the sign, ❤ and after the sign." are the same under this unit as under 1.5.9, as found on the unit's branch
   before the move and written in the record. The test of requirement 17 of unit 0030, that the `espeak-ng-data` of
   the model folder gives the same phonemes as the data of the Java archive as published, still passes with the
   release in use.
10. Everything else the earlier units established still holds, as requirement 8 of unit 0030 lists it, and the plugin
    archive stays within the limit of 30 MB of unit 0004.
11. `README.md` and `THIRD-PARTY.md` name the release of `piper-phonemize` in use and the software its archives hold,
    with the license of each, as taken from its build files at the tag of that release. They no longer say that the
    project builds the native library for Linux x64, and `README.md` says which glibc the plugin needs on Linux.
12. `README.md` and the section on emoji and symbols of `docs/reading-markdown.md` say that characters above U+FFFF
    are read as eSpeak NG reads them: by their names where its data has one, such as "grinning face" for 😀, and as
    "letter" with the digits of their code point where it has none. The "Open" line of that section, on whether they
    are read once the binding is fixed, is answered.
13. Automated tests on plain values cover the words the plugin hands on for a text with characters above U+FFFF:
    alone, at the start and the end of a block, inside a word, next to a character of three bytes, and four at the
    start of a line; and the offsets of the words around them.
14. The tests with the real engine of this unit and of the earlier units have run with the model in place, and not
    been skipped, on Linux x64 and on macOS on Apple Silicon.
15. The developer has listened to `samples/markdown-features.md` on Linux x64 and on macOS on Apple Silicon, and the
    record holds what sounded different from before this unit.

## Decisions

- **The release's own native library for Linux x64.** The project built its own only because the release needed
  glibc 2.38. The release is now built in the same container image the project's script used, from the source of its
  tag, so building it again would keep a script, a committed archive and a checksum for no gain. Rejected: keeping the
  project's own build as a safeguard against a later release that needs a newer glibc, since requirement 3 finds that
  out when the release is next moved.
- **1.6.1 and not 1.6.0.** Both hold the two fixes. 1.6.1 is the latest release and the one the specifier tried.
- **Characters above U+FFFF are read as eSpeak NG reads them, also where it has no name.** The human decided that they
  are read. The specifier decided, open to the human, that the plugin does not filter what eSpeak NG makes of them:
  "letter 2 0 0 0 0" for a rare character is heard and understood as a character the voice cannot name, and the
  plugin cannot tell from the phonemes which characters have a name. Rejected: leaving out every character above
  U+FFFF that has no name in the data of eSpeak NG, which needs a list of the names of that data kept in step with each
  release, see item 1 of "Beyond the request".
- **The phonemes as the test that 😀 is read, and the length as the test that the engine speaks it.** The phonemes
  are the same on every run and say exactly what is read. The length checks that the engine does not drop them, with
  the margin of 10% that unit 0030 used for "the same length", the other way round.
- **The unit supersedes unit 0030.** It makes requirements 5 to 7, 11 and 16 of unit 0030 untrue and its decisions on
  the Linux library and on characters above U+FFFF obsolete, which would make that record misleading if they were
  only amended there.

## Non-goals

- Names of emoji or symbols chosen by the plugin, in place of those of eSpeak NG, and a setting to leave emoji out.
- Any change to how ❤, ✓ and other characters of three bytes are read, which reach eSpeak NG as before.
- A new way to time the highlight of an emoji read by its name. It is highlighted as any other word is.
- Moving sherpa-onnx to another release.
- The other platforms for which `piper-phonemize` 1.6.1 publishes native libraries.

## Beyond the request

1. Leave out the characters above U+FFFF that eSpeak NG reads as "letter" and digits, by a list taken from its data.
   It costs a list of the characters that data names, kept in step with each release, and a test that it matches the
   data in use. Without it, a rare character is read as "letter" and its digits, which the listener hears and can
   live with.
2. A note in the record of the specs of units 0028 and 0032 that this unit has made untrue, as unit 0030 had for units
   0028 and 0029. It costs one entry. Without it, a planner of those units finds the change from the tree, since both
   are planned against the tree they are built on.
