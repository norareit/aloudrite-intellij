---
kind: feature
status: approved
timestamp: 2026-11-21T11:30:54Z
---

# Measure how well the word highlight follows the voice

## Log

- 2026-11-21T11:30:54Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `6f83f0c`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0032, after 0031. Found that `model.onnx`
  of the fp32 package has the audio as its one output and no durations, and that the developer's `kokoro-env` holds
  `kokoro` 0.9.4 and PyTorch 2.14.0 and `Kokoro-82M` the weights of Kokoro v1.0 and the voice `af_heart`. Decided with
  the human: a measurement that is kept and that unit 0028 can run again rather than a run whose result is only
  recorded, the durations of the model by the Kokoro Python package as the truth, and Heart as the only voice. Decided
  by me and open to the human: every piece of the sample rather than 11 sentences, 1.0× and 1.5×, only the time within
  words counted, the 2% for the lengths, and the run on the host. Left beyond the request: counting the pauses, and
  repeating the forced alignment of the trial. The status is `draft`
- 2026-11-21T11:43:08Z [human] approved the spec. The status is `approved`

## Amendments
