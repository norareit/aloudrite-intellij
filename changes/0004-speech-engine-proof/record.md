---
kind: feature
status: in-progress
timestamp: 2026-10-08T21:10:00Z
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
- 2026-10-08T20:09:15Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of
  `0004-speech-engine-proof` at commit `d20162e3b6280d44bde57eb03f9a745c541e2ee7`, against the merge base with
  `master`, `0026d5bc4dcbd15fc986475a83a234f25e336864`. Returned findings S1, P1, P2, C1 and C2, saved in
  `.reviews/0004-speech-engine-proof/review-20261008T200746Z-d20162e.md`. Ran `./gradlew clean buildPlugin test`
  with the model in place, a second test run with an absent model selected through `XDG_DATA_HOME`, and
  `npm run check-changes`. Could not verify live-IDE playback, native loading through the installed plugin's class
  loader, responsiveness, repeated-press behavior in a live IDE, the live missing-model error indicator, operation
  with the machine's network disabled, any macOS execution or listening, the first-sound timings, GitHub
  publication, or the outstanding post-implementation revisions of units 0005 and 0006. Historical temporary
  trials and test-first failures were not independently reconstructed, and the complete provenance and static
  dependency inventory of the published native binaries were not independently audited.
- 2026-10-08T20:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, recorded what the human reported after installing the archive by hand in their real IntelliJ on Linux
  (not the sandbox IDE, and the build of the archive is the one of `d20162e`'s tree as far as the code goes; which
  archive file exactly was installed was not stated). Reported by the human: the sentence was audible, so the
  binding loads its native libraries through the class loader of the plugin on Linux. They could still click on
  other things while it spoke. Pressing again while it spoke gave no second voice, and they pressed continuously
  during the speech and only the logged times appeared, so each `First sound` line is an actual start of speech and
  ignored presses log nothing. The `First sound` lines of `idea.log` read 1773 ms for the first press, which
  includes loading the model, and 665 ms and 662 ms for later presses, with the model loaded. Together with the
  archive of 20,069,169 bytes and the model folder of 189,882,497 bytes logged above, that completes the three
  measurements of requirement 13 on Linux (a 16-core AMD Ryzen 9 9950X3D), and the measurement task of the plan
  is ticked for Linux only. Not reported and so not claimed: the human's judgment of how it sounds, that menus
  open and typing goes on without a pause, that no dialog opens, that a second Markdown file gives the same
  sentence, that Tools holds no entry, and anything in the sandbox IDE. The Verification item for the sandbox IDE
  on Linux is therefore left unticked. Still open: the message for a missing model without an error indicator,
  the button with the network off, macOS install, hearing and times, the GitHub repository, and the revision of
  the specs of units 0005 and 0006.
- 2026-10-08T20:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, recorded the human's check of the missing model, made in their real IntelliJ on Linux with the installed
  plugin. Reported by the human: they renamed `~/.local/share/aloudrite` to `~/.local/share/aloudrite2` and pressed
  Read Aloud. A dialog with an OK button appeared that read "The speech model is missing. AloudRite looks for it
  in /home/stef/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0. The README of AloudRite says which model
  package to get and how to put it there." They saw no error indicator ("no error lights") in the IDE. They were
  told to rename the folder back and had not confirmed it when this was written. When this entry was written,
  `ls ~/.local/share` on the machine showed `aloudrite` and no `aloudrite2`, which is what I saw and not the
  human's confirmation. The Verification item for the missing model is left unticked: it says "in the same IDE",
  which is the sandbox IDE started with `./gradlew runIde`, and the check was made in the installed plugin of
  another IDE; and it names the status bar, where the human reported "no error lights" without saying where they
  looked. What is established is the message text, its folder and its README, as the human saw them, and that no
  error indicator was seen. Nothing else is claimed.
- 2026-10-08T20:30:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, recorded two more results the human reported from their real IntelliJ on Linux with the installed plugin.
  First, they confirmed that they moved the model folder back, so `~/.local/share/aloudrite` is in place again, which
  the main session also saw as `aloudrite/kokoro-int8-multi-lang-v1_0` with no `aloudrite2`. This closes the loose
  end of the entry on the missing model. Second, with the network off, pressing Read Aloud kept working fine and it
  spoke. They did not say how they turned the network off, nor whether the plugin was loaded or the model was
  already loaded when they did. The Verification item for the network off is left unticked: it says "in the same
  IDE", which is the sandbox IDE started with `./gradlew runIde`, and the check was made in the installed plugin of
  another IDE. What is established is that, as the human reported, the button spoke with the network off. Nothing
  else is claimed.
- 2026-10-08T20:45:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, applied the review `review-20261008T200746Z-d20162e.md` (ignored by git, in `.reviews/`) at the human's
  request, in place in this unit, which is not frozen. Outcomes by finding:
  C1 (blocking, accepted): `SpeechService` had no unit coverage. The plan is extended (two tasks at the end of
  its list, and the Files list) and it was extended in the same commit as the work and not before it, which the
  process asks for the other way round. `SpeechService` now takes the way it runs work, makes the engine and the
  player, and reports the first sound and a failure through its constructor, with a constructor without arguments
  for the platform. New contracts `SpeechEngine` and `SamplePlayer` are what it uses, implemented by
  `KokoroSpeechEngine` and `AudioPlayer`, which now takes an open `SourceDataLine` and closes the line when
  `drain` throws. `SpeechServiceTest` has 11 tests: nothing but a hand-over happens at the press, the work plays
  each piece at the rate of the engine, the first sound is reported once per press, the engine is made once and
  reused, a press while one runs is ignored through speaking and through the closing of the player, a failure of
  the engine, of the speech and of the player is reported and the next press works, and `dispose` closes the
  engine only where nothing is running. `AudioPlayerTest` has 5, with a stand-in line: the line is started, the
  samples are written as PCM, the first write is announced once and before the write, and drain comes before close
  also when drain fails. As a check that the tests can fail, the caching of the engine was removed once, and four
  tests of the service failed, and the first-write announcement was disabled once, and the player test failed;
  both were put back. Not covered by these tests, and left to a live IDE: the real thread pool, the real error
  dialog on the event dispatch thread, and the real audio device.
  C2 (accepted): the time to the first sound is now taken by `AudioPlayer` after the first piece is converted and
  just before it is written to the line, and not when the piece reaches the service. The plan says so. The Linux
  times recorded above (1773, 665 and 662 ms), taken by the human with the archive of `d20162e`, used the earlier
  point, when the first samples arrived, so they are before the conversion and the write. They are kept as they
  stand and are not counted for requirement 13: the human has to install the rebuilt archive and press again to
  get the times that count. For that reason the measurement task in the plan is unticked again, with its note.
  The sizes in that task are unaffected. The rebuilt archive is 20,073,758 bytes.
  P1 (accepted): the tasks for `SpeechPlatform`, `KokoroModel`, `PcmAudio` and `OneAtATime` were ticked, and they
  were not done test first, as the entry above says. That cannot be repaired afterwards and is acknowledged here.
  For the engine test and the tests of the press I said they were seen failing to compile, and that is my own
  account: the history holds no trace of it. The plan is kept as written.
  P2 (accepted): the six commit points are ticked against the commits `ba2c2b1`, `75e1add`, `845ebae`, `eb1549f`,
  `beaeb37` and `d20162e`, the last one with a description that no longer promises the measurements, and a
  seventh point is added for the commit of this change.
  S1 (not mine to close): the macOS install and times, the GitHub repository, and the specs of units 0005 and
  0006 stay open, together with the human's judgment of the sound and the sandbox-IDE verification items.
  Verified on the tree of this change: `./gradlew clean buildPlugin test` passed with 46 tests, 0 failures and 0
  skipped: `KokoroModelTest` 7, `AudioPlayerTest` 5, `PcmAudioTest` 6, `OneAtATimeTest` 5, `KokoroSpeechEngineTest`
  1 (the model in place), `ReadAloudActionTest` 9, `SpeechServiceTest` 11, `SpeechPlatformTest` 2. `npm run check-changes` passed
  on the commit of this change.
- 2026-10-08T20:50:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, recorded the Linux times of requirement 13 that the human measured again, from their real IntelliJ on
  Linux, after uninstalling the plugin and installing it again from the rebuilt archive of 20,073,758 bytes, built
  from `129622b`, where the time is taken at the first write to the audio line. The human pasted these lines of
  `idea.log`: `2026-10-08 22:22:05,089 [18561] First sound 1346 ms after the button was pressed`, then
  `2026-10-08 22:23:08,397 [81869] First sound 640 ms` and `2026-10-08 22:23:13,817 [87289] First sound 654
  ms`. The number in brackets is the uptime of the IDE in milliseconds, so the first press came about 18.5 seconds
  after the process started and the IDE was a fresh process. The human did not say whether they restarted it by
  hand or accepted a restart prompt. The first press includes loading the model and the later two have it loaded.
  The file cache of the operating system was warm from earlier runs and no cache was dropped, so the first-press
  figure is not a cold start. These times replace the earlier 1773, 665 and 662 ms for requirement 13 on Linux,
  which used the earlier point of time and stay in the record above as they were. With the archive size and the
  model folder size logged before, which the rebuilt archive does not change for the model (the archive size of the
  rebuild is the 20,073,758 bytes above), the three Linux measurements are complete, on the 16-core AMD Ryzen 9
  9950X3D, and the measurement task of the plan is ticked. The task names the log of the sandbox IDE, and the log
  here is that of the human's real IDE. The Verification item that lists the measurements together with the
  judgment of the sound on both platforms, the macOS times, the GitHub repository and the fate of the specs of
  units 0005 and 0006 stays unticked, as most of it is not done. Nothing else is claimed.
- 2026-10-08T20:29:36Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of
  `0004-speech-engine-proof` at commit `2ed4c1892d3ef5017911bfbc9b0d51434eec0d67`, compared against the prior
  reviewed commit `d20162e3b6280d44bde57eb03f9a745c541e2ee7`. Verified the previously accepted findings and
  reviewed the intervening diff on all three axes. Returned findings S1, P3, C3 and C4, saved in
  `.reviews/0004-speech-engine-proof/delta-20261008T202814Z-2ed4c18.md`. Ran `./gradlew clean buildPlugin test`,
  `npm run check-changes` and `git diff --check`, and inspected the rebuilt archive. Could not independently
  observe the human's installed-IDE Linux checks or inspect their personal log, hear speech or measure latency
  there, verify how the network was disabled, perform any macOS check, verify GitHub publication or the
  outstanding specifier revisions, or reconstruct the historical mutation trials from the final committed tree.
- 2026-10-08T21:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, applied the findings P3 and C4 of the delta review `delta-20261008T202814Z-2ed4c18.md` (ignored by git,
  in `.reviews/`), in place, and committed the reviewer's own entry above in a commit of its own first. P3
  (accepted): the Linux measurement task of `plan.md` is reworded to what was done, with the installed plugin in
  the human's own IntelliJ and their `idea.log` as pasted, and not the sandbox IDE. The numbers and the claims of
  the entries above are unchanged. The wording says "restarted" about the IDE because the uptime of the first press
  shows a fresh process, and the entries above say the human did not tell how. C4 (accepted, the test fix
  preferred): `aFailureOfThePlayerIsReportedAndTheNextPressWorks` now makes the stand-in player factory fail once,
  runs the next work item with the factory working, and asserts one failure in all, playback of both pieces,
  that the player was closed, and the order of the events. The plan task and the record entry above that call it
  covered are therefore accurate as they stand. The test was not run against a factory that always fails, so that
  it would fail there is argued from its assertions and not observed. C3 (the mix of refactoring and behavior in
  `129622b`) is the human's decision and was not acted on, and history was not rewritten. Verified on the tree of
  the test commit: `./gradlew clean buildPlugin test` passed with 46 tests, 0 failures, 0 skipped, `SpeechServiceTest`
  11 among them. Still open as before: macOS install, hearing and times, the human's judgment of the sound, the
  GitHub repository, the specs of units 0005 and 0006, C3, and the sandbox-IDE verification items.
- 2026-10-08T21:10:00Z [human] decided finding C3 of the delta review `delta-20261008T202814Z-2ed4c18.md`, in the
  main session, with the words "C3: I accept it." That is, the human accepts that commit `129622b` mixes refactoring
  (the constructor injection and the interfaces `SpeechEngine` and `SamplePlayer`) with behavior changes (the time
  to the first sound moved to the first write, and the audio line closed when `drain` throws), as an exception to
  the rule of `process.md` to keep refactoring and behavior in separate commits. No history is rewritten, and
  nothing in the code or the plan changed because of it.

## Amendments
