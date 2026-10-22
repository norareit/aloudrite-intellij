---
kind: feature
status: approved
timestamp: 2026-10-22T11:16:38Z
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

## Amendments
