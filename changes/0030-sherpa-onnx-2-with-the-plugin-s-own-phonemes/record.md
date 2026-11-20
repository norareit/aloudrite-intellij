---
kind: feature
status: approved
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
- 2026-11-20T00:43:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `13d6a1f`, at the human's request, added requirement 15: before the unit is implemented, its
  record names what of the specs of units 0028 and 0029 it has made untrue, unneeded or open to a simpler way, so that
  the human is reminded to have them corrected. Also wrote in the context that the human set unit 0029 back to
  `draft`, and that correcting the two specs is no part of this unit. The status is `draft`
- 2026-11-20T00:54:07Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `4ccf5c1`, at the human's request, tested the Java binding of `piper-phonemize` 1.5.9 for
  characters above U+FFFF and found the fault of pull request 4062 of sherpa-onnx in it: its `nativeText` reads the
  text with `GetStringUTFChars`, and eSpeak NG leaves such a character out, where the C library of the same release,
  given standard UTF-8, reads 😀 as "grinning face". Wrote the finding into the context. Decided with the human: the
  unit does not wait for a fixed release, which the human pursues upstream; the workaround of unit 0006 stays, so
  that requirements 5 to 7, 11 and the parts of 12 to 14 on emoji that came from unit 0014 are replaced by a check
  that such characters leave nothing in their place and a test that tells whether the release in use has the fix;
  reading them is left to a later unit, whose test checks that 😀 is read with "grinning face"; and eSpeak NG reads
  the data that `piper-phonemize` holds itself, in place of the copy in the model folder. The status is `draft`
- 2026-11-20T01:03:27Z [human] approved the spec. The status is `approved`
