---
kind: feature
status: draft
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

## Amendments
