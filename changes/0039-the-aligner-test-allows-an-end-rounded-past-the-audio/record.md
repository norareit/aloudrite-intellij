---
kind: bug
status: approved
timestamp: 2026-11-22T02:10:18Z
---

# The aligner test allows an end rounded past the audio

## Log

- 2026-11-22T02:10:18Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `53b0463`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit. Made it a bug and numbered it 0039, after 0038. The failure was reproduced
  on the host while unit 0036 was being closed, with the counts its record gives, and the decision of unit 0028 to keep
  the check on the ends in `RealAlignerTest` was read in its record. Decided by me and open to the human: the test is
  fixed and not the aligner's results, the bound on the ends is loosened by one step of the aligner's times rather than
  dropped, and the step is to be found in sherpa-onnx or its model rather than assumed to be 10 ms. Left beyond the
  request: bounding the ends in `wordStartsOf`, and repeated runs of the full suite as verification. The status is
  `draft`
- 2026-11-22T11:31:37Z [human] approved the spec. The status is `approved`

## Amendments
