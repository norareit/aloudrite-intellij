---
kind: feature
status: draft
timestamp: 2026-11-20T00:33:58Z
supersedes: 0014-sherpa-onnx-with-the-fixed-binding
---

# sherpa-onnx 2 with the plugin's own phonemes

## Log

- 2026-11-20T00:33:58Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `537afd2`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0030, after 0029. Read on GitHub
  why sherpa-onnx 2.0 dropped eSpeak NG (issue 3731 and pull request 4055: its GPL license), the maintainer's answer in
  issue 4139, the phonemes that `GenerationConfig` of 2.0 takes, and the Java binding and releases of
  `piper-phonemize`. Found that pull request 4062, the fix of unit 0014, was merged after 4055 and is in 2.0. Decided
  with the human: the plugin's own fallback to eSpeak NG, this unit before units 0028 and 0029, and the work of unit
  0014 taken in, which the human cancelled for it. Decided by me and open to the human: the data of eSpeak NG from the
  model folder, and 10% of the length under 1.13.8 as the test that every word is spoken. Left beyond the request: a
  note for other users of sherpa-onnx. The status is `draft`
