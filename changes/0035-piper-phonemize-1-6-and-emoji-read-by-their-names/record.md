---
kind: feature
status: draft
timestamp: 2026-11-21T11:48:00Z
supersedes: 0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes
---

# piper-phonemize 1.6 and emoji read by their names

## Log

- 2026-11-21T11:48:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `13b0427`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0035, after 0034, superseding
  unit 0030, whose requirements on the Linux library and on characters above U+FFFF it makes untrue. Read the changes
  of `piper-phonemize` from `v1.5.9` to `v1.6.1` on GitHub and tried the Java archive and the native library for Linux
  x64 of 1.6.1 outside the project, on the host and in `eclipse-temurin:21-jdk-jammy`, as the context of the spec
  says. Decided with the human: the move to the fixed release, characters above U+FFFF read by their names, and a unit
  of its own. Decided by me and open to the human: the release's own Linux library in place of the project's build,
  and characters without a name read as eSpeak NG reads them. Left beyond the request: leaving out the characters
  without a name, and a note on what this unit makes untrue in the specs of units 0028 and 0032. The status is
  `draft`

## Amendments
