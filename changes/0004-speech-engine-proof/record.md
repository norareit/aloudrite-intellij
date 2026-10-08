---
kind: feature
status: planned
timestamp: 2026-10-08T19:41:45Z
---

# Speech engine proof of concept

## Log

- 2026-10-08T16:49:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`. The human's request, to get Kokoro working in the plugin, was split with the human into
  three units: this one, 0005 for the download of the model and 0006 for reading a file. At the human's
  instruction all three specs were written now, and this one requires that 0005 and 0006 are revised when it is
  done. Decided with the human: sherpa-onnx as the engine, GPL-3.0 as the license and part of this unit, the
  source on GitHub next to Bitbucket as the human's own step, and Linux x64 and macOS on Apple Silicon as the
  platforms. Looked up on the day of writing and not run: the sizes of the model, of the ONNX Runtime Java library
  and of sherpa-onnx 1.13.8 with its Kokoro packages, and that the build of sherpa-onnx compiles espeak-ng in as
  a static library. Not established: that sherpa-onnx loads inside an IDE plugin, how the 8-bit model sounds, how
  fast it is, and that its v1.0 package holds the voice `af_heart`. My own choices, which the human has not
  stated: "or later" on the license, the 8-bit multi-language v1.0 package, the voice, the test sentence, the
  30 MB limit on the archive, and the model folder being one per user. Status left at `draft` for the human to
  approve.
- 2026-10-08T19:14:26Z [human] approved `spec.md` as it stands. Status set to `approved`.
- 2026-10-08T19:24:34Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a planner session on the Linux
  host, cut the branch `feature-0004-speech-engine-proof` from the head of `master` at `9299d4e` and wrote
  `plan.md`.
  Established in a throwaway trial, made in a scratch directory outside the repository and deleted afterwards,
  with the three jars of sherpa-onnx 1.13.8 and the model package fetched from GitHub: the jars are not on Maven
  Central and resolve as an Ivy repository over the release assets; a copy of this repository built an archive of
  20,036,743 bytes that holds them with the two license files; the package unpacks to one directory of
  189,882,497 bytes; `af_heart` is speaker 3 in the metadata of the model, which answers what the specifier left
  open; the test sentence came out as 3.67 seconds of audio at 24,000 Hz, in a plain JDK 25 and inside
  `./gradlew test`, with the first piece 0.64 s after asking and the engine created in 0.67 s; and Java Sound
  opened an output line of that rate without anything being played. Found and planned around: a configuration
  without a lexicon makes the native code end the whole process with status 255, where an absent or a cut-off
  model file only throws, so the plan never creates the engine before the five entries it names exist. The spec's
  "Beyond the request" assumed that a wrong model folder fails with an error of the engine, and for some mistakes
  that failure is the IDE closing, which bears on unit 0005. Also found: the binding copies its native libraries
  to the temporary folder at each start and leaves an empty directory there, or 31 MB after a kill. Requirement 9
  names the button of unit 0003, which is implemented on its own branch and is not in the tree this was planned
  against, so the plan leaves "Say Hello" in place and keeps whatever tests are there passing. Decided in the
  plan, as the spec leaves it to the plan: the model folder, `~/.local/share/aloudrite/` or
  `$XDG_DATA_HOME/aloudrite/` on Linux and `~/Library/Application Support/AloudRite/` on macOS, with the unpacked
  package directory inside.
  Not established, and left to the checks in an IDE: that the binding loads its native libraries through the
  class loader of a plugin, that anything is audible, how it sounds, and everything on macOS. The planner stopped
  the developer's running Gradle daemon with `./gradlew --stop` after the trial, which it need not have done; the
  next build starts a new one. Status set to `planned`.
- 2026-10-08T19:31:42Z [human] asked for one change to `spec.md`, after unit 0003 was merged into `master`: the test
  sentence is started with the "Read Aloud" button of unit 0003 and not with an action in the Tools menu.
- 2026-10-08T19:31:42Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  made that change in `spec.md` on the unit's branch. Requirements 5, 7, 8, 9, 11, 13 and 18 and the decision on
  how the sentence is started are reworded, and no requirement is renumbered. My own choice within the request,
  which the human has not stated: the dialog of unit 0003 goes in this unit, so that the button speaks in place
  of showing the content and does not do both. `plan.md` was written for the Tools menu action and no longer
  matches the spec, so the status is set back from `planned` to `approved` for the planner to revise it.
- 2026-10-08T19:41:45Z [Claude Code, anthropic/claude-opus-5-5] as the planner, dispatched as a subagent from the
  main session on the Linux host, rewrote `plan.md` for the corrected spec, against the branch at `3b10a03`, which
  holds unit 0003. The "Speak Test Sentence" action, its class, its test and its entry in `plugin.xml` are out of
  the plan. In their place `ReadAloudAction` is changed so that a press checks the platform, then the model, then
  speaks, `FileContentDialog.kt` is deleted, and in `ReadAloudActionTest` the three tests that assert the dialog
  go, the five others of unit 0003 stay unchanged, and four come: the plugin registers no action but the button,
  the message for a missing model, the message and the button on an unsupported platform, and the same sentence
  whatever the file holds. `plugin.xml` is not changed. The findings of the first trial that do not depend on
  how the sentence is started were kept and not run again: the jars as release assets, the archive size, the
  model folder, the five entries, speaker 3, the lexicon and the timings. Established in a second throwaway
  trial, made in a scratch directory outside the repository and deleted afterwards, in a copy of the current
  tree without sherpa-onnx: under `./gradlew test`, an action made by the test with a platform, a folder and a
  function that speaks in place of the service can be pressed like the registered one, its messages reach the
  test dialog, and `ActionManager.getActionIdList` gives the one id. Changed in the design beyond the trigger:
  the action is handed the function that speaks, so that a test covers the press that speaks without an engine;
  the guard `OneAtATime` is given the function that runs work in the background, so that its test needs no
  thread; and the service leaves the engine alone at disposal while a sentence is being made. Corrected: the log
  of the sandbox IDE is in `log_runIde`. Found on this machine: the model is not in its folder and
  `XDG_DATA_HOME` is not set. Not established, as before: that the binding loads its native libraries through
  the class loader of a plugin, that anything is audible, and everything on macOS. The developer's Gradle daemon
  was used and left running. The branch was not rebased on the two commits `master` gained for units 0005 and
  0006. Nothing in the corrected spec was found to contradict the tree. Status set to `planned`.

## Amendments
