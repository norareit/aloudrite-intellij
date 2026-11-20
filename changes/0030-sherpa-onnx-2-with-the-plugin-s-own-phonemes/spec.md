# sherpa-onnx 2 with the plugin's own phonemes

## Context

The plugin speaks through sherpa-onnx 1.13.8. For an English text, its Kokoro front end looks each word up in the
lexicon the plugin gives it, `lexicon-us-en.txt`, and hands a word that the lexicon lacks to eSpeak NG, which turns
any text into phonemes. Numbers, abbreviations, names, words of code and symbols all reach the voice that way.

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
2026-10-20, after pull request 4055, so sherpa-onnx 2.0 holds it. This unit takes the work of unit 0014 in, and the
human cancelled that unit for it.

Pull request 4062 fixed the binding of sherpa-onnx only. Whether the Java binding of `piper-phonemize` hands a
character above U+FFFF intact to its native code is not known.

Issue 4178 of sherpa-onnx reports that phonemes handed in are cut every 401 tokens of a sentence, wherever that falls,
also in the middle of a word.

This unit is built before units 0028 and 0029. The human chose that order, so that both build on the front end of
this unit rather than on that of 1.13.8. The human set unit 0029 back to `draft`, to be corrected once this unit is
implemented and what it brought is known.

## Requirements

1. The plugin uses sherpa-onnx 2.0.2 or a later 2.x release, for the Java binding and for the native libraries of
   both platforms, and `piper-phonemize` 1.5.9 or a later 1.x release, for its Java binding and its native libraries
   of both platforms.
2. The plugin hands the engine the phonemes of each piece and not its text. It finds them itself: a word in
   `lexicon-us-en.txt` of the model in use takes the phonemes of the lexicon, and a word not in it takes the phonemes
   eSpeak NG gives for it through `piper-phonemize`, in the language `en-us`.
3. Every word of the text handed to the engine is spoken, those not in the lexicon among them. A test with the real
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
5. Characters above U+FFFF reach the front end of requirement 2 as they are in the file. The plugin no longer gives
   them as a space, or takes anything else out or puts anything in for them. What the plugin does with the rest of
   the text, such as leaving out the Markdown markers of unit 0010, stays as it is.
6. A file that holds nothing but characters above U+FFFF and white space is read like any file with text. Only a file
   that is empty or holds only white space shows the notification that there is nothing to read.
7. With the real engine and the voice of the plugin, the sentence "Before the sign, 😀 and after the sign." gives
   less than 4 seconds of audio, and so does the same sentence with U+20000, U+10330 or U+1D400 in place of the
   emoji.
8. Everything else the earlier units established still holds: the model packages, their checksums and their folders,
   with no new download; the voice and the speed; the first sound before the whole file is made; the stop within a
   second; pause, resume and the change of the speed; the reading time and the highlights; the playing of samples that
   are not a number as silence; the number of threads; and the reading of a file of several pages without a silence
   in the middle of a sentence.
9. Starting a reading, from the press to the first audio, takes no more than 50 ms longer than before this unit, on
   the machine of `README.md` with the fp32 model, measured on the unit's branch before and after the move and written
   in the record.
10. `THIRD-PARTY.md` names the releases of sherpa-onnx and `piper-phonemize` in use, and the software their native
    libraries hold, with the license of each, as taken from their build files at the tags of those releases.
    `README.md` names them where it names 1.13.8, says why the plugin is licensed under GPL-3.0-or-later in the terms
    of the new libraries, and states the growth of the plugin archive.
11. `README.md` and `docs/reading-markdown.md` no longer say that characters above U+FFFF are left out. They say that
    emoji and symbols are handed to the engine as they are, and read as the engine reads them, which may be by their
    names. Nothing in the plugin's code or tests says any longer that such a character cannot reach the engine.
12. Automated tests cover the front end of requirement 2 on plain values, with a stand-in for eSpeak NG and a small
    lexicon: a word in the lexicon, a word not in it, punctuation, a number, and a character above U+FFFF alone,
    several in a row, between two words and in a file of nothing but them, each reaching the front end as it is in
    the file. They cover the cut of requirement 4 at, below and above the limit.
13. The tests with the real engine of requirements 3 and 7, and those with the real engine of the earlier units, have
    run with the model in place, and not been skipped, on Linux x64 and on macOS on Apple Silicon, and the record holds
    the lengths of audio found for the sentences of requirements 3 and 7.
14. The developer has listened to `samples/markdown-features.md` on Linux x64 and on macOS on Apple Silicon, and the
    record holds anything that sounded different from 1.13.8, and how each emoji and symbol of its sections "Emoji of
    four bytes" and "Symbols of three bytes" sounded.
15. Before the status is set to `implemented`, the record holds an entry that names, for unit 0028 and for unit 0029,
    each requirement and decision of their specs that this unit has made untrue, unneeded or open to a simpler way,
    with what this unit brought that causes it, so that the human can have those specs corrected.

## Decisions

- **The plugin finds the phonemes itself, with eSpeak NG for what the lexicon lacks.** That is what 1.13.8 did inside
  the engine, so the voice says what it said, and it is the way the maintainer pointed to in issue 3731. The plugin
  is under the GPL already, so the license of eSpeak NG that made sherpa-onnx drop it does not bind the plugin.
  Rejected: staying on 1.13.8, which keeps unit 0028 from its aligner; staying on 1.13.8 for the speech and running
  the aligner of 2.0 in a process of its own, since two releases of sherpa-onnx cannot share the process of the IDE,
  which costs a helper process, its messages and a second set of native libraries; spelling out numbers,
  abbreviations, symbols and words of code in the plugin and leaving the rest out, which is a large job and still
  drops names; and waiting for sherpa-onnx to bring a fallback back, which its maintainer declined.
- **Through `piper-phonemize`, the maintainer's own package.** It is the part sherpa-onnx 1.13.8 linked, published
  the same way and for the same platforms. Rejected: building eSpeak NG for two platforms in this project.
- **The data of eSpeak NG from the model folder.** Both model packages hold `espeak-ng-data`, which sherpa-onnx 1.13.8
  read and 2.0 ignores, so the plugin needs no new download. Decided by the specifier and open to the human.
- **Within 10% of 1.13.8 as the test that every word is spoken.** A dropped word shortens a short sentence far more
  than that, as "I have 3 cats in 2026." showed, while the same phonemes made by another release of the engine give
  nearly the same length. The developer's listening of requirement 14 is the check of how it sounds. Decided by the
  specifier and open to the human.
- **The cut of requirement 4 between two words.** A cut by the engine in the middle of a word, as issue 4178 reports,
  would be heard. The plugin's pieces of at most 400 bytes mostly stay below the limit, so the cut is the rare case.
- **The work of unit 0014 is part of this unit.** Its fix comes with the move to 2.0, and its test of the emoji is
  where an unknown in `piper-phonemize` would show, so the two are checked together. Requirements 5 to 7 and 11, and
  the emoji part of requirements 12 to 14, are those of unit 0014, taken in as they were written.
- **This unit names what it changes for units 0028 and 0029.** The human asked to be reminded, once this unit is
  done, to correct those specs, when what it brought is known rather than guessed. The implementer is the one who
  knows it then, so the record holds it before the unit ends.
- **The check on macOS is required.** The unit changes the native libraries, which `AGENTS.md` names as a reason for
  it.
- **The cut into pieces of at most 400 bytes and the playing of samples that are not a number as silence stay**, as
  unit 0014 decided, since neither comes from the binding.

## Non-goals

- The forced aligner, which is unit 0028.
- The choice of the voice, the British lexicon and the language of eSpeak NG for a British voice, which is unit 0029.
- Correcting the specs of units 0028 and 0029. Requirement 15 only names what is to be corrected.
- Changing how a reading is cut into pieces, beyond the cut of requirement 4.
- Spelling out numbers or symbols in the plugin beyond what eSpeak NG does.
- Making all emoji and symbols sound alike beyond what the engine does, or leaving them out.
- Using anything else that sherpa-onnx 2.0 adds.

## Beyond the request

1. A one-line contribution to the documentation of sherpa-onnx, or a comment on issue 4139, that names the way of this
   unit for a GPL project in Java. It costs a short text. Without it, other users of the Java binding find the way
   themselves.
