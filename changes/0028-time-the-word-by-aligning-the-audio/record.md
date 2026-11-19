---
kind: feature
status: draft
timestamp: 2026-11-19T21:25:28Z
---

# Time the highlighted word by aligning the audio

## Log

- 2026-11-19T21:25:28Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `master` at `4e900a6`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0028, after 0027. Before writing it,
  ran a trial outside the project: confirmed that the Kokoro model returns the durations of its tokens and that
  sherpa-onnx 1.13.8 and 2.0.2 discard them, and measured the forced aligner of sherpa-onnx 2.0.2 against those
  durations, with the results the spec's context gives. Found that sherpa-onnx 2.0.2 skips words not in the lexicon
  where 1.13.8 speaks them. Decided with the human: the unit itself, after the trial, and that its record holds the
  human's decision on the default of the word highlight of unit 0027. Decided by me and open to the human: forced
  alignment over the durations of the model, the delay of 70 ms set by ear, and the aligner model downloaded with a
  speech model rather than shipped in the plugin archive. Left beyond the request: keeping the times of a piece played
  again. The status is `draft`

## Amendments
