# Speech engine proof of concept

## Context

AloudRite is to read Markdown aloud with the Kokoro text-to-speech model, running on the user's own machine. So far
the plugin makes no sound. `idea.md` proposed embedding ONNX Runtime and calling the model directly, and called
that a footprint of about 100 MB. Looked at on 2026-10-08, that plan has two holes.

The first is size. The published Kokoro v1.0 model is 326 MB at full precision, 163 MB at 16 bits and 86 to 92 MB
quantized to 8 bits. The 55 voices are 29 MB together, about 0.5 MB each. The ONNX Runtime Java library is 56 MB
for all its platforms. The 100 MB of `idea.md` is the quantized model alone.

The second is that the model does not take text. It takes phoneme tokens, and something has to turn English text
into them first. The developer's Python experiment does that with the `misaki` library, falling back on espeak-ng.
Neither exists for the JVM, and `idea.md` does not mention the step.

sherpa-onnx closes the second hole. It is a library with a Java API that takes text and returns audio for Kokoro,
and it publishes native libraries of about 10 MB per platform, among them Linux x64 and macOS on Apple Silicon. It
publishes Kokoro model packages as well: 132 MB for the 8-bit multi-language v1.0 model with its voices. Its build
compiles espeak-ng into its native library, and espeak-ng is licensed under GPL-3.0 or later. A plugin that ships
sherpa-onnx therefore ships GPL code, and has to be offered under the GPL as a whole.

None of this has been run inside an IDE. This unit is the proof of concept: it makes the plugin speak one fixed
sentence on the two platforms the developer uses, and it measures what the answer costs. The work is split over
three units. This is the first, unit 0005 lets the plugin download the model itself, and unit 0006 connects the
"Read Aloud" button of unit 0003 to the speech. Their specs were written together with this one, before anything
was known from running it, which is why this unit ends by revising them.

## Requirements

1. The plugin archive holds everything needed to turn text into speech on Linux x64 and on macOS on Apple
   Silicon, apart from the model. Nothing else has to be installed on the machine: no Python, no espeak-ng, no
   audio library.
2. The plugin archive is no larger than 30 MB.
3. The model is not in the plugin archive and is not tracked by git. `README.md` says which model package to get,
   where to get it, and in which folder to put it.
4. That folder is one per user, outside any IDE's own directories, so that the sandbox IDE, an installed IDE and
   the automated tests all find the same copy of the model.
5. With the model in place, pressing the "Read Aloud" button of unit 0003 on a Markdown file plays the sentence
   "AloudRite can speak. This sentence was made on your own machine." through the default audio output of the
   machine, in the voice `af_heart` at normal speed. The sentence is the same whatever the file holds. The dialog
   of unit 0003 that shows the content of the file is gone, and the plugin adds no other action.
6. The IDE stays usable while the sentence is made and played: typing in an editor and opening menus are not held
   up.
7. Pressing the button while the sentence is still being made or played does not produce overlapping speech.
8. When the model is not in its folder, pressing the button shows a message that says the model is missing, names
   the folder, and points to the README. No error report of the IDE appears.
9. On a platform other than the two of requirement 1, the plugin loads without an error, the button is shown as
   on the other platforms, and pressing it shows a message that speech is not supported on this platform.
10. The machine needs no network connection for any of this, and the plugin makes no network request.
11. An automated test asserts requirement 8 and another asserts requirement 9, and both run without the model and
    without an audio device. The automated tests of unit 0003 that assert when the button is shown still pass,
    and those that assert the dialog are replaced or removed.
12. An automated test makes the audio of the test sentence without playing it and asserts that it lasts between 2
    and 15 seconds. Where the model is not in its folder, that test is reported as skipped and not as passed.
13. The record states, as measured on the developer's Linux machine: the size of the plugin archive, the size of
    the model folder, and the time from pressing the button to the first sound, for the first press after the
    IDE started and for the one after it.
14. The developer has heard the test sentence on Linux x64 and on macOS on Apple Silicon, from the plugin
    installed in an IDE, and the record holds their judgment of how it sounds and the times of requirement 13 as
    they found them on macOS. These are the human's checks, made at the review.
15. The repository holds a `LICENSE` file with the text of the GNU General Public License version 3, and
    `README.md` states that the plugin is licensed under GPL-3.0-or-later and why.
16. A file in the repository lists the third-party software the plugin ships or uses, with the license of each: at
    the least sherpa-onnx, ONNX Runtime, espeak-ng and the Kokoro model. The plugin archive holds the license
    text and that list.
17. The source of the plugin is public in a GitHub repository, in addition to the Bitbucket repository it is
    pushed to today. This is the human's step, and the record says when it was done and where the repository is.
18. `README.md` describes what the button does after this unit and the platforms speech works on, and no longer
    describes the dialog.
19. Before this unit is set to `implemented`, the specs of units 0005 and 0006 have been revised by the specifier
    against what this unit established, and the human has seen the revisions. Where one of them needed no change,
    the record of this unit says so.

## Decisions

- **The model runs inside the IDE process, through an existing Java binding.** The alternative of starting a
  Python program was rejected: the developer's own Python environment for Kokoro is 6.1 GB, and it would have to
  be set up on every user's machine. The worry that this means writing JNI code does not apply, because the
  binding is published as a Java library with its native code inside.
- **sherpa-onnx, not ONNX Runtime used directly.** sherpa-onnx turns text into phonemes itself, and its native
  libraries for the two platforms are about 20 MB together. Using ONNX Runtime directly was rejected because the
  plugin would then need a converter from text to phonemes of its own, which is a project in itself and is what
  the quality of the speech depends on most.
- **The plugin is licensed under GPL-3.0-or-later.** sherpa-onnx contains espeak-ng, which is GPL-3.0-or-later,
  so a plugin that ships it has to be offered under the GPL as a whole. The developer accepts that: the plugin was
  meant to be open source from the start, and they do not want a closed or paid version. "Or later" follows
  espeak-ng's own terms. Downloading the native library at first use to keep the plugin under another license was
  rejected as a grey area and not a way out. Not verified: the Marketplace's own rules for GPL plugins, and how
  the GPL sits with a plugin that runs inside a proprietary IDE. Both have to be looked at before the plugin is
  published there, which is not in this unit.
- **The license enters in this unit.** This is the unit that puts GPL code into the plugin, so the license file
  and the notices arrive with it and not when the plugin is published.
- **The source goes to GitHub as well.** The developer wants the source in the place where people look for open
  source, next to the Bitbucket repository. Creating the repository and pushing to it needs the developer's
  account, so it is their step.
- **The model is the 8-bit multi-language Kokoro v1.0 package of sherpa-onnx.** It is the Kokoro version
  `idea.md` names, in the smallest form sherpa-onnx publishes, at 132 MB packed. The full-precision package of 350
  MB was rejected for its size, before anyone has heard whether the difference is audible. The English-only 8-bit
  package of 103 MB was rejected because it is the older version 0.19. How the 8-bit model sounds is exactly what
  requirement 14 is there to find out, and a poor result changes this decision.
- **The voice is `af_heart`.** `idea.md` calls it the primary recommendation. Choosing a voice is not part of
  this unit.
- **The model is put in place by hand.** Getting it there automatically has many ways to fail that have nothing
  to do with whether the engine works, and it is unit 0005. Doing it by hand first keeps the two questions apart.
- **The model lives in one folder per user, outside the IDE's directories.** The IDE's own directories are per
  IDE version and per sandbox, so a model kept there would be fetched again for each of them, 132 MB at a time.
  Which folder exactly is for the plan, following what each operating system expects.
- **Linux x64 and macOS on Apple Silicon, and nothing else.** They are the two platforms the developer uses, and
  the developer asked for both from the beginning. Intel Macs were left out by the developer as a platform few
  still use. Windows and Linux on ARM have nobody to check them.
- **The button of unit 0003 speaks the test sentence, in place of its dialog.** The developer asked for it, once
  unit 0003 was merged. The button is where reading will be started, so the proof of concept goes through the
  place the real thing will, and nothing temporary is added that a later unit has to take out. An earlier version
  of this spec had a temporary "Speak Test Sentence" action in the Tools menu, chosen so that this unit did not
  depend on unit 0003, and the developer rejected it. The dialog goes because a button that both opens a dialog
  and speaks does two unrelated things, and the dialog was a placeholder for the speech from the start. The cost
  is that between this unit and unit 0006 the button says a sentence that has nothing to do with the file, and
  the plugin no longer shows that it can get the file's text. Unit 0006 connects the two.
- **The limit of 30 MB on the archive.** The two native libraries and the Java binding come to about 20 MB as
  published. The limit leaves room for what was not foreseen and still fails loudly if the model or a third
  platform's library ends up inside.
- **The sound itself is judged by the developer.** An automated test can show that audio of a plausible length
  comes out. Whether it is intelligible and pleasant can only be heard, and the agent that builds this has no
  ears and no Mac.

## Non-goals

- Downloading the model from the plugin. That is unit 0005.
- Reading a file, or anything else than the one fixed sentence. That is unit 0006.
- Stopping or pausing speech that has started.
- Choosing a voice, a speed or a language, and a settings page for them.
- Windows, Intel Macs and Linux on ARM.
- Use of a graphics card.
- Publishing the plugin on the Marketplace.
- Any processing of Markdown.

## Beyond the request

- **Checking the hand-placed model against a checksum.** A model folder that is incomplete or holds another
  package gives an error from the engine that may be hard to read. A check costs a list of expected files or
  checksums in the plugin and a message of its own. Without it, a wrong folder fails when the button is pressed,
  with whatever the engine reports, on the machine of a developer who put the files there minutes before. Unit
  0005 needs the checksum anyway and is where it belongs.
- **Native libraries for Intel Macs and Windows.** sherpa-onnx publishes both, at about 11 MB and 8 MB. Including
  them costs archive size and two platforms nobody here can check. Without them, those users get the message of
  requirement 9, and adding a platform later is a change to the build and to that message.
- **A way to stop the test sentence.** It costs a second action or a changing button. Without it, a sentence of a
  few seconds plays to its end. Unit 0006 needs stopping for whole files and specifies it there.
