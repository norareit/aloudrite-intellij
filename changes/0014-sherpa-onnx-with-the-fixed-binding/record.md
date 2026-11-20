---
kind: feature
status: cancelled
timestamp: 2026-10-20T20:07:28Z
---

# sherpa-onnx with the fixed binding

## Log

- 2026-10-19T14:09:41Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `37e4b25`, at the human's request, wrote `spec.md` of a new unit: the plugin moves to the
  first release of sherpa-onnx that holds the fix of its Java binding for characters above U+FFFF, and the workaround
  of unit 0006 goes. It is the reminder of the entry of 2026-10-19T09:33:36Z in the record of unit 0006, written once
  that unit was implemented. Read `process.md` and `roles/specifier.md` of the installed kit. Made it a feature,
  since it changes what is heard, and numbered it 0014, after 0013. On this date pull request 4062 of sherpa-onnx is
  open and not merged and the latest release is 1.13.8, so the unit cannot be planned yet. Asked before writing, the
  human chose to take the workaround out entirely, so that such characters reach the engine and may be read by their
  names, where during unit 0006 the human had rejected names that the plugin would provide. Decided by me and open to
  the human: the first release with the fix or a later one; the threshold of 4 seconds for the test with the real
  engine; the cut into pieces of 400 bytes and the playing of samples that are not a number as silence stay; the
  license inventory is made again for the new release. Not changed by me and to be told to the human: the draft spec
  of unit 0010 names as a non-goal that characters above U+FFFF stay left out, which this unit undoes once it is
  built. The status is `draft`
- 2026-10-19T19:30:37Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, narrowed requirement 2 of `spec.md` to
  characters above U+FFFF. It said that the plugin takes nothing out of the text of the file, which would undo the
  leaving out of Markdown markers by unit 0010 where that unit is built first. On the human's word that these
  characters are left out until this unit is done, and read after it. The status stays `draft`
- 2026-10-20T20:07:28Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on the human's word, made requirement 8 of `spec.md` ask that the test of requirement 4 with the real engine has
  run, with the model in place, and not been skipped, on both platforms, with the length of audio of each sentence in
  the record, and added a decision that says why: an implementer in the sandbox, where a new home volume has no model,
  could otherwise finish with that test skipped. The status stays `draft`
- 2026-11-20T00:34:55Z [human] cancelled the unit: Unit 0030 does its work. The fix of the Java binding for characters
  above U+FFFF, pull request 4062 of sherpa-onnx, was merged after the removal of eSpeak NG and is only in sherpa-onnx
  2.0. Moving to 2.0 needs the plugin's own fallback to eSpeak NG, which unit 0030 builds, and unit 0030 takes the
  requirements of this unit in. The unit `0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` did the work or made it
  moot. The status is `cancelled`

## Amendments
- 2026-11-20T00:54:07Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, at the human's request, records that
  unit 0030 takes in the move to a release of sherpa-onnx with the fix of pull request 4062, but not the reading of
  characters above U+FFFF, as the cancellation said. The Java binding of `piper-phonemize`, which unit 0030 uses to
  reach eSpeak NG, has the same fault as the binding of sherpa-onnx 1.13.8 had, so the workaround of unit 0006 stays,
  and the reading of such characters is left to a later unit, built on a release of `piper-phonemize` with the fix
- 2026-11-20T02:02:33Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, at the human's request, corrects the amendment
  before: the reading of characters above U+FFFF is not left to a later unit that is promised. Whether they are read
  by their names, in a unit of its own, the human decides once a release of `piper-phonemize` hands them intact.
  Until then, they stay left out as unit 0006 leaves them out
