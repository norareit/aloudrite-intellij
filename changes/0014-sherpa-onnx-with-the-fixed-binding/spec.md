# sherpa-onnx with the fixed binding

## Context

The plugin speaks through sherpa-onnx 1.13.8. During unit 0006 the implementation found that its Java binding never
hands a character above U+FFFF intact to the native code, a character of four bytes in UTF-8, among them most emoji.
The binding reads the text with `GetStringUTFChars`, which gives modified UTF-8, and in modified UTF-8 such a
character is the two halves of its surrogate pair, three bytes each. The engine then makes about 5.7 seconds of
babble for each such character, whichever one it is.

Unit 0006 worked around it in the plugin:

- Each character above U+FFFF is given to the engine as a space, which requirement 11 of unit 0006 makes the one
  exception to reading the text as it is in the file.
- A file of nothing but such characters and white space counts as empty, by requirement 12 of unit 0006: it shows the
  notification that there is nothing to read, and asks no question about the download.
- `README.md` and `docs/reading-markdown.md` say that these characters are left out, and why.

The human reported the fault to sherpa-onnx as https://github.com/k2-fsa/sherpa-onnx/issues/4060 and submitted a fix
as https://github.com/k2-fsa/sherpa-onnx/pull/4062, which reads the text of the TTS functions of the Java binding as
standard UTF-8. On 2026-10-19, when this spec was written, the pull request was open and not merged, and the latest
release was 1.13.8 of 2026-09-10. Before the report, the human listened to the same sentence with U+1F600 through the
Python API of sherpa-onnx 1.13.8, which hands the text over as standard UTF-8, and heard no babble. Through it, the
sentence "Before the sign, and after the sign." gave 1.88 seconds of audio, and 2.7 to 3.5 seconds with U+1F600,
U+20000, U+10330 or U+1D400 in it, against 7.4 to 7.6 seconds through the Java binding.

This unit moves the plugin to a release of sherpa-onnx with that fix and takes the workaround out. It cannot be
planned before such a release exists. Where the fix is merged in another form, the release that holds it is the one
meant.

Emoji and symbols of three bytes already reach the engine, which reads some of them by their names: during unit 0006
the developer heard ⚓ read as "anchor" and ❤ as "red heart", while ✓ was silent.

## Requirements

1. The plugin uses the first release of sherpa-onnx whose Java binding hands a character above U+FFFF intact to the
   native code, or a later release, for the Java binding and for the native libraries of both platforms.
2. The text of the file is given to the engine as it is in the file, characters above U+FFFF included. The plugin
   takes nothing out of it and puts nothing in.
3. A file that holds nothing but characters above U+FFFF and white space is read like any file with text. Only a file
   that is empty or holds only white space shows the notification that there is nothing to read.
4. With the real engine and the voice of the plugin, the sentence "Before the sign, 😀 and after the sign." gives
   less than 4 seconds of audio, and so does the same sentence with U+20000, U+10330 or U+1D400 in place of the emoji.
5. Everything else that units 0004 to 0006 established still holds with the new release: the model package and its
   checksum, the voice and the speed, the first sound before the whole file is made, the stop within a second, and
   the reading of a file of several pages without a silence in the middle of a sentence. Where the new release
   needs another model package, the unit stops there and the human decides.
6. `THIRD-PARTY.md` names the new release, and the software its native libraries hold, with the license of each, as
   taken from the build files of sherpa-onnx at the tag of that release. `README.md` names the new release where it
   names 1.13.8.
7. `README.md` and `docs/reading-markdown.md` no longer say that characters above U+FFFF are left out. They say that
   emoji and symbols are handed to the engine as they are, and read as the engine reads them, which may be by their
   names. Nothing in the plugin's code or tests says any longer that such a character cannot reach the engine.
8. Automated tests cover requirements 2 and 3 with a stand-in engine: a character above U+FFFF alone, several in a
   row, one between two words, and a file of nothing but them, each reaching the engine as it is in the file. A test
   with the real engine covers requirement 4, and runs where the model is in place.
9. The developer has listened to the sections "Emoji of four bytes" and "Symbols of three bytes" of
   `samples/markdown-features.md`, and to the whole file, on Linux x64 and on macOS on Apple Silicon. The record
   holds how each emoji and symbol of those sections sounded, and anything else that sounded different from unit
   0006.

## Decisions

- **The workaround goes, and emoji are read as the engine reads them.** The human decided this. During unit 0006 the
  human rejected reading such a character by its name, when the plugin would have had to provide the name itself.
  Now the engine provides it, as it already does for the emoji of three bytes, so all emoji are handled alike, which
  was a non-goal of unit 0006. Rejected: moving to the fixed release and still leaving such characters out, which
  keeps the difference between emoji of three and of four bytes as a choice of the plugin; and leaving every emoji
  out, those of three bytes included, which takes out what the engine reads well, as ⚓, and overlaps with unit 0010,
  which decides what of a Markdown file is read.
- **The first release with the fix, or a later one.** The workaround cannot go before the fix is released, and
  nothing in this unit needs a version later than that. A later one is allowed so that the unit does not have to
  move to a release that has since been replaced, which the plan checks when it is written.
- **Less than 4 seconds** in requirement 4 lies above the 2.7 to 3.5 seconds that the Python API gave with the same
  model and text, and well below the 7.4 to 7.6 seconds of the babble, so it tells the two apart without depending on
  how the engine says each name.
- **The cut into pieces of at most 400 bytes stays.** Unit 0006 found that one call of the engine with more than about
  1,500 characters ends the process, and has the plugin cut the text far below that. The fix of the binding says
  nothing about that limit, and the short pieces also keep the stop within a second, so this unit does not touch
  them.
- **The playing of samples that are not a number as silence stays.** Unit 0006 added it after the engine gave such
  samples for a horizontal rule. It does not come from the binding.
- **The license inventory is made again.** A new release may link other versions of ONNX Runtime, espeak-ng and the
  other libraries in its native libraries, and the plugin ships them, so `THIRD-PARTY.md` follows the release, as it
  was made for 1.13.8 in unit 0004.

## Non-goals

- Making all emoji and symbols sound alike beyond what the engine does, such as naming ✓, which the engine leaves
  silent.
- Leaving emoji out, or saying a word of the plugin's own in their place.
- Using anything else that the new release adds, such as new voices, models or options.
- A half of a surrogate pair without its other half, which unit 0006 left as it is and which a file read from disk
  does not hold.
- Changing the cut of the text into pieces, or the number of threads of the engine, which is unit 0011.

## Beyond the request

- None
