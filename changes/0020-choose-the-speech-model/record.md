---
kind: feature
status: in-progress
timestamp: 2026-10-22T13:20:00Z
---

# Choose the speech model

## Log

- 2026-10-22T11:16:38Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `8d1ef87`, at the human's request, wrote `spec.md` of a new unit: the speech model is chosen,
  downloaded and removed on the plugin's settings page, between the int8 and the fp32 package of Kokoro 1.0. The human
  asked for it after unit 0016 found that the int8 model loses pieces of a reading on macOS on Apple Silicon and the
  fp32 model does not. Read `process.md`, the standards and `roles/specifier.md` of the installed kit, 7.3.0. Made it
  a feature and numbered it 0020, after the task 0019. Checked in the documentation of sherpa-onnx which Kokoro
  packages exist and which have the voice `af_heart`. Decided by me and open to the human: the two packages of 1.0 as
  the choice; the fp32 model by default on macOS and the int8 model on Linux x64; a choice during a reading waits for
  the next reading; the model in use cannot be removed; one download at a time across both models. Left beyond the
  request: Kokoro 1.1, Kokoro 0.19, a measurement of speed on the page, and the debug output of sherpa-onnx turned
  off. The status is `draft`
- 2026-10-22T12:10:47Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `e5f7a03`, at the human's request, added Kokoro 1.0 in fp16 as item 5 of "Beyond the request",
  after a check of the releases of sherpa-onnx found no fp16 package of Kokoro there. Read `process.md`, the standards
  and `roles/specifier.md` of the installed kit, 8.0.0. The status is `draft`
- 2026-10-22T12:15:04Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, at the human's request that the default and the models be easy to change, extended requirement 1 so that the
  models are one list that the rest of the code iterates, and requirement 3 so that the default per platform is stated
  in one place and is not saved as a choice; added both to the tests of requirement 11 and recorded the decision. The
  status is `draft`
- 2026-10-22T12:21:16Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on the human's taking in of item 4 of "Beyond the request" with the output kept for the sandbox IDE, added
  requirement 15 and its decision. Checked in the binding of sherpa-onnx 1.13.8 that `OfflineTtsModelConfig` has
  `setDebug` and that its builder starts with `debug` on. The status is `draft`
- 2026-10-22T12:23:22Z [human] approved the spec. The status is `approved`
- 2026-10-22T12:32:03Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a run of AgentRite's orchestrator,
  on `feature-0020-choose-the-speech-model` at `cbfd02b`, wrote `plan.md`. Read `process.md`, the standards and
  `roles/planner.md` of the installed kit. The plan makes the model a value of a list in a refactoring first, then
  adds the debug output of sherpa-onnx only in the sandbox IDE, the fp32 entry and the default per platform, the
  model of a reading with one engine at a time, the download, choice and removal from the page, the part of the page
  and the README, in seven commits. Checked again that the fp32 package is 349,906,910 bytes with the SHA-256 of unit
  0016's record, and read from a listing of the package that its files take 401,239,297 bytes. That is 401 MB in the
  plugin's megabytes, as 190 MB is for the int8 model, where the context of the spec says 384 MB, which is what the
  folder takes on the disk. The page and the README will say 401 MB; the spec's requirements are not affected. The
  status stays `approved`
- 2026-10-22T12:35:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a run of AgentRite's
  orchestrator, on `feature-0020-choose-the-speech-model`, began the work. Read `process.md`, the standards and
  `roles/implementer.md` of the installed kit, the spec, the plan and this record. There is no review of this unit in
  `.reviews/` yet. The status is `in-progress`
- 2026-10-22T13:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built steps 1 to 7 of the plan in
  seven commits, each with the tests before or with the code. Put the int8 model in place by hand in
  `~/.local/share/aloudrite`, with the commands and the checksum of the README, so that the tests of the real engine
  ran. On `a2f811c`, `./gradlew cleanTest test buildPlugin` reported 475 tests, 0 failed and 3 skipped, which are
  `EngineThreadsMeasurement`, `EngineSilenceMeasurement` and `RealModelPackageTest`, each gated by an environment
  variable, and the 5 tests of `KokoroSpeechEngineTest` ran. The archive is 20,549,373 bytes, under the limit. The real
  engine wrote nothing to the output of the tests with the debug output off
- 2026-10-22T13:20:01Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, deviated from the plan in five
  places, each recorded under "Additions found while building" in `plan.md`: `defaultSpeechModel` takes the operating
  system name only, since the architecture would not change its answer; `OneAtATime` and `ModelDownloadService` got an
  `onChange` and an `onRunningChanged`; the page depends on the narrow interface `ManagedSpeechModels`, which also holds
  `downloadingModel`; `ModelPackage` is a data class; `ModelDownloadOffer.offer` returns whether the download started.
  The question of unit 0005 now names the model in parentheses, as step 1 and requirement 8 ask, and its test changed
  with it. The claim of the lock now takes the folder that holds the model folders, and the old lock file name is kept
- 2026-10-22T13:20:02Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, did not do two tasks, and left
  their boxes unticked: the look at the page in the sandbox IDE (step 6) and the look at the output of `runIde` for the
  lines of sherpa-onnx (step 2). The sandbox has no display and no `xwd`, so `./gradlew runIde` cannot open a window
  here. The layout of the page is covered by tests of the components, including the height of the radio buttons on a
  tall page, and says nothing about how it looks under the look and feel of the IDE. They are for the developer on the
  host. Not checked either: the fp32 model with the real engine, which was not downloaded here, and the downloads from
  the page with the real network, which the tests replace with a stand-in server and stand-in installs
- 2026-10-22T13:20:03Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, left requirements 12 and 13 to the
  developer, as the plan does. R12 is the check on macOS on Apple Silicon, with the fp32 model downloaded from the page,
  at 1.0, 1.5 and 2.0 on `samples/markdown-features.md`. R13 is the check on Linux x64 of the download of the other
  model from the page, the switch between the two, a reading with each and a removal. Neither is done, so the last box
  of "Commit Points" and the two boxes of "Verification" for them stay open
- 2026-10-22T14:02:49Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0020 at
  `e078b4d2fd16acc54bbc46b54fafca0e7e594aad`, against the merge base with `origin/master`,
  `cbfd02bfa96f259f530f89ea5230c29b3c6c23ec`. Returned S1-S4, saved in
  `.reviews/0020-choose-the-speech-model/2026-10-22T140054Z-review.md`. Ran
  `./gradlew cleanTest test buildPlugin` and `npm run check-changes`, both successfully; all five real int8 engine
  tests ran, and the three environment-gated suites were skipped. Could not verify the developer checks of R12 and
  R13, the settings-page appearance in the sandbox IDE, its debug-enabled output, or the real fp32 engine. The status
  stays `in-progress`
- 2026-10-22T14:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a run of AgentRite's
  orchestrator, on `feature-0020-choose-the-speech-model`, took up S1 to S4 of
  `.reviews/0020-choose-the-speech-model/2026-10-22T140054Z-review.md`. S3: `ModelDownloads` has `downloadingModel`,
  which `ModelDownloadService` sets in `start`, so the page shows "downloading" for a download that a press of Read
  Aloud started too, and `SpeechModelManagement` no longer keeps its own copy. S4: the page subscribes to
  `READING_CHANGED` and shows the models again, which keeps a selection not applied yet. A test of each was added
  (`ModelDownloadServiceTest`, `ReadingSpeedConfigurableTest`) and written after the code, so neither was seen to
  fail first. On `59362d0`, `./gradlew cleanTest test buildPlugin` reported 477 tests, 0 failed and 3 skipped, the same
  three gated suites
- 2026-10-22T14:40:01Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, proposes to decline S2 as a
  change of the code, and asks for a decision on R15. The log entry of 13:20:00Z that the real engine wrote nothing was
  wrong, and the review is right: with `debug` off, the run of `KokoroSpeechEngineTest` still prints one block, the
  line `jni/offline-tts.cc:operator():481 config:` and the `OfflineTtsConfig(...)` with `debug=False`, once when the
  engine is created. The per-piece lines are gone. The block is written by the native code of sherpa-onnx 1.13.8 with no
  setting of the binding to turn it off, as far as the binding's classes show, and only a redirect of the process's
  error output would remove it, which I would not put into an IDE plugin. Proposed: R15 is read as the per-piece lines,
  which is what the spec's context is about, and the one block per engine is left. Open until the human decides. S1 is
  the developer's checks R12 and R13 and stays open. The status stays `in-progress`
- 2026-10-22T14:55:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, recorded the human's decisions on
  the review of 14:00:54Z. S2 is declined as a change of the code: R15 is read as the per-piece lines of debug output,
  and the one configuration block that the native code writes when the engine is created is accepted. S1 stays open on
  purpose: the human does the checks R12 and R13 and the look at the page after every other finding is done, and no
  finding but S1 is open now. The status stays `in-progress`
- 2026-10-22T14:12:23Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0020 at
  `cab2db9a81351aab19de62de86191ef1d97bd3fa`, from the previous reviewed commit
  `e078b4d2fd16acc54bbc46b54fafca0e7e594aad`. Reported verification of S3 and S4, the recorded disposition of S2,
  the outstanding S1, and new nonblocking P1, in
  `.reviews/0020-choose-the-speech-model/2026-10-22T141106Z-delta-review.md`. Ran
  `./gradlew cleanTest test buildPlugin`: 477 tests, 0 failures, 3 environment-gated skips, with all five real int8
  engine tests running. `npm run check-changes` passed. Could not verify the developer checks of R12 and R13, the
  IDE visual inspection, its debug-enabled sandbox output, or the real fp32 engine. The status stays `in-progress`
- 2026-10-22T16:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, at the developer's request after the
  Linux check of the settings page, changed the layout of the model rows in `47c7698`. In the check, a row was one
  long line, the Remove button was cut off at the default window width, and the buttons of the two rows did not line up.
  Now each row holds the radio button with the Download and Remove buttons to its right, and the description is a row
  of its own below it, indented to the label of the radio button. The radio buttons share a width group and so do the
  two kinds of button, so that the buttons of both models have the same x position and size. No behavior changed. A new
  test checks this in the laid out page at 800 by 700: the same x and size of the buttons of the rows, the buttons level
  with their radio button, and the description below and to the right of it. `./gradlew test` reported 478 tests, 0
  failures, 3 skips on the final tree. Not checked by eye in the sandbox IDE: the display is the developer's own, where
  `./gradlew runIde` returned at once because their sandbox IDE is already open with the earlier code. A render of the
  page with the Swing default look and feel, which is not the IDE's, showed the intended arrangement. The developer
  re-checks the look at the default window width. The status stays `in-progress`
- 2026-10-22T17:10:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, corrected `spec.md` at the developer's
  request after the manual check on Linux: the tool window should also show the speech model in use, read-only. Added
  requirements 16 to 18 (the row, its tests, the README) after requirement 15 and one decision, and renumbered nothing.
  This is a new requirement and not a clarification of requirement 9: the tool window shows no model today, only the
  reading, its controls and the speed, and requirement 9 asks that what is in use agree between the page and a reading,
  where it names the tool windows without asking them to show the model. Requirement 16 uses the existing notice of a
  change of the model in use and adds no mechanism. The unit is not implemented, so `spec.md` is not frozen. The plan
  needs a task for it. The status stays `in-progress`
- 2026-10-22T17:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the planner, wrote step 8 of `plan.md` for requirements 16 to 18 against the tree as it is: a row of the model in use in `ReadingPanel`, taken as `modelInUse: () -> SpeechModel` and followed through `SPEECH_MODEL_CHANGED`, below the speed block, with a longer name wrapping rather than being cut, tests first, the README, and a look in the sandbox IDE at the first docked width. Added a commit point for it. The check items R12 and R13 and the macOS item are as they were. The status stays `in-progress`

## Amendments
