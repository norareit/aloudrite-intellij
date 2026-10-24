---
kind: feature
status: cancelled
timestamp: 2026-10-22T12:10:47Z
---

# Compare speech models

## Log

- 2026-10-22T12:10:47Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `e5f7a03`, at the human's request, wrote `spec.md` of a new unit: a measurement and a listen
  that compare Kokoro 1.0 in fp16, Piper voices and Kitten TTS with the two Kokoro packages of unit 0020, on both
  machines, ending with a recommendation. The human asked for the ideas to be kept as a draft unit rather than only in
  "Beyond the request" of unit 0020. Read `process.md`, the standards and `roles/specifier.md` of the installed kit,
  8.0.0. Made it a feature, not the task first suggested to the human, because it adds test code and leaves choices
  open. Numbered it 0021, after 0020. Checked in the release `tts-models` of sherpa-onnx which packages exist and their
  sizes. Decided by me and open to the human: the comparison before a feature, fp16 from a conversion of the project's
  own, one voice per model. Left beyond the request: the int8 and fp16 packages of Piper and Kitten, and more Piper
  voices. The status is `draft`
- 2026-10-24T12:47:43Z [human] cancelled the unit, because the fp32 model became the default on both platforms in
  `changes/0017-reading-shortcut`, which made what the comparison was for moot: the fp32 model loses no pieces of a
  reading on either machine and makes its audio faster than the int8 model on both, so neither a model that loses less
  nor one that is faster is still sought. What remained, a smaller download than the 350 MB of the fp32 model, was not
  judged worth a unit of its own. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word.
  The status is `cancelled`

## Amendments
