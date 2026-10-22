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

## Amendments
