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
- 2026-11-19T22:36:38Z [human] approved the spec. The status is `approved`
- 2026-11-20T00:47:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `f2b4f6e`, at the human's request, set the status back to `draft`. The planner, in a run of the
  orchestrator on the unit's branch at `537afd2`, had stopped before writing `plan.md`: under sherpa-onnx 2.0.2 every
  word the lexicon lacks is skipped, numbers, abbreviations, words of code and symbols among them, because 2.0
  dropped eSpeak NG, so requirement 5 could not be met as written. The human had the branch deleted, since the work
  on the unit had not started, and with it the planner's entry in this record. Unit 0030 moves the plugin to
  sherpa-onnx 2 with the plugin's own fallback to eSpeak NG, and the human wants this spec corrected once unit 0030 is
  implemented, as its requirement 15 names what is to change, and approved again only then. Requirement 5, the
  context on the risk of moving to 2.0.2, and the reason of requirement 11 for the check on macOS are among what is
  to be looked at again. The planner's facts for the plan, kept here: the aligner model is published at
  `https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2`, 47,567,475 bytes,
  SHA-256 `86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da`, and unpacks to the folder
  `english-mfa-v3.1.0` of 51,887,069 bytes, holding `final.alimdl`, `lda.mat`, `tree`, `phones.txt`, `lexicon.txt`,
  `meta.json`, a `README.md` and a test file `en.wav`. Its Java API is `ForcedAligner`, configured by the model folder
  and its `lexicon.txt`, whose `compute` takes a stream given the samples and the text and returns tiers of intervals
  with a start, an end and a text. The native library of 2.0.2 is about 0.6 MB larger per platform than that of
  1.13.8. The status is `draft`
- 2026-11-20T14:49:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `8e716ff`, at the human's request once the review of unit 0030 at `f905ca7` returned no blocking
  finding, and before unit 0030 is set to `implemented`, corrected `spec.md` from the entry of requirement 15 in the
  record of unit 0030. Read `process.md`, the standards and `roles/specifier.md` of the installed kit. The context now
  says that the plugin uses sherpa-onnx 2.0.2 since unit 0030 and makes its own phonemes, so that every word is spoken,
  and that the trial ran under 1.13.8 and is taken to hold, since unit 0030 measured the audio within 0.5% of its length
  under 1.13.8; the paragraph on the risk of moving to 2.0.2 is gone. Requirement 5, that every word is still spoken
  under 2.0, and its test with the real engine in requirement 7 are removed as done by unit 0030, and the requirements
  after it are renumbered, from 6 to 11 now 5 to 10. Requirement 9 names `THIRD-PARTY.md` for the license of the aligner
  model, and requirement 10 asks for the check on macOS because the aligner is a part of the native library not used
  before. The decision on forced alignment now weighs the cost after unit 0030, which no longer includes the conversion
  of text to phonemes, keeps forced alignment and is open to the human. The sizes of the plugin archive are those after
  unit 0030, and a non-goal names the move of release. Requirement 8 is unchanged: its "before this unit" is now the
  state after unit 0030. The spec is to be approved again. The status is `draft`

## Amendments
