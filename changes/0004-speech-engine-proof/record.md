---
kind: feature
status: in-progress
timestamp: 2026-10-08T20:00:00Z
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
- 2026-10-08T20:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, dispatched as a subagent
  from the main session on the Linux host, built the plan as far as one machine without ears and without a Mac
  allows, in the commits `ba2c2b1`, `75e1add`, `845ebae`, `eb1549f` and `beaeb37` on this branch. The status is
  set to `in-progress` and stays there: the tasks that are the human's, and the measurement of the times, are
  open (see below). The human authorized, in the session, putting the model under `~/.local/share/aloudrite` and
  running the build on the host, which fetches the jars from `github.com`; nothing else outside the repository
  was changed except the temporary files named below.
  Done and verified on `beaeb37` (the code of the tree is that of `eb1549f`, `beaeb37` changed `README.md` only),
  on the developer's Linux machine, an AMD Ryzen 9 9950X3D with 16 cores and 32 threads:
  `./gradlew clean buildPlugin test` is green. The 30 tests are: `KokoroModelTest` 7, `OneAtATimeTest` 5,
  `PcmAudioTest` 6, `SpeechPlatformTest` 2, `ReadAloudActionTest` 9 (the five of unit 0003 that do not assert the
  dialog, unchanged, and the four of the press) and `KokoroSpeechEngineTest` 1, which was counted as passed with
  the model in place (0 skipped, 0 failures) and as skipped (1 skipped, 0 failures) with the model folder renamed
  for one run, where `ReadAloudActionTest` passed too. The folder was renamed back. The archive is 20,069,169
  bytes, and holds `LICENSE`, `THIRD-PARTY.md`, the plugin's jar and the three jars of sherpa-onnx under `lib/`
  and nothing else. The three jars have the checksums of the plan. `sha256sum LICENSE` is the one of the plan.
  With the limit lowered to 1,000,000 for one run, `buildPlugin` failed with `The plugin archive is 20054823 bytes,
  more than the limit of 1000000 bytes`, and the limit was put back to 30,000,000. `com.k2fsa` appears in
  `KokoroSpeechEngine.kt` only, and `java.net`, `HttpClient`, `HttpRequests` and `URL(` appear nowhere in
  `src/main`. `FileContentDialog` appears nowhere in `src`, `README.md` has no word `dialog`, `git diff master --
  src/main/resources` is empty, and `plugin.xml` has one `<action>` and `com.intellij.modules.platform` as its only
  `<depends>`. `git ls-files` lists no `.onnx`, `.bin` or `.bz2`. The commands of the README for Linux were run
  as written with `HOME` set to a scratch directory: the checksum check printed `OK` and the unpacked folder is
  189,882,497 bytes. The model was put in `~/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0` with the same
  steps, by hand, and checked against the checksum of the plan before it was unpacked. The size of the model
  folder is 189,882,497 bytes by `du -sb`. The README commands for macOS were not run.
  Measured: archive 20,069,169 bytes, model folder 189,882,497 bytes. The two times to the first sound (requirement
  13) are not measured, and the lines `First sound <n> ms after the button was pressed` do not exist in any log yet.
  Tried and abandoned: `./gradlew runIde` started on the developer's display with a scratch project. The plugin
  loaded (`Loaded custom plugins: AloudRite (0.0.1)` in `idea.log`, and no `ERROR` line in the log in the two
  minutes it ran), but pressing the button needs a click in a window on a desktop that also holds the developer's
  own IDEs and terminals, and driving it with `xdotool` there could have sent keys to the wrong window, so it was
  stopped, and the sandbox IDE was ended. So not checked by anyone: that the binding loads its native libraries
  through the class loader of the plugin, that anything is audible, that a press does not hold up the IDE, that a
  second press gives no second voice, that the message for a missing model reaches the IDE without an error
  indicator, that the button speaks with the network off, and the sound itself. These are the Verification items
  left unticked, and need a person at the screen.
  What differs from the plan: the tests for `SpeechPlatform`, `KokoroModel`, `PcmAudio` and `OneAtATime` were
  written in the same step as the code and not seen failing first, which the plan asks for. The engine test and
  the tests of the press were seen failing to compile before the code existed. The tests of the engine and of the
  press pass on the code; whether they would catch a fault in the code was not tried by breaking it. In the first
  commit of the plan's ticks the ticks were put on the wrong tasks by a script error, and corrected in the same
  commit before anything else was built on it. `THIRD-PARTY.md` lists, beside the four named libraries, what the
  `CMakeLists.txt` files and `cmake/` of sherpa-onnx `v1.13.8` link into the JNI library, each with the license
  read in its own repository at the tag or commit those files name. ONNX Runtime is 1.28.2, by the build files and
  by the string in `libonnxruntime.so` of the Linux jar. Eigen and portaudio are left out because the build files
  show no `include(eigen)` and the JNI library does not link portaudio, which was not confirmed on the binary.
  Temporary files outside the repository: the 132 MB package was moved to the scratchpad of the session, and the
  jars and the trial project are there too.
  Open for the human, none of them done: the check in a window on Linux, the two times of requirement 13, the
  install and the hearing on both machines (requirement 14), the GitHub repository (requirement 17), and the
  revision of the specs of units 0005 and 0006 against what this unit established (requirement 19). The list of
  what they rest on is for the next entry, once the human has given the times, because the times belong in it.

## Amendments
