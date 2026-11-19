---
kind: feature
status: approved
timestamp: 2026-11-19T21:56:57Z
---

# Choose the voice

## Log

- 2026-11-19T21:56:57Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `3cd6e83`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0029, after 0028. Read the
  numbers of the voices from the `id2speaker` metadata of the fp32 model, and rendered one passage in each of the 13
  voices through the Java binding of sherpa-onnx 1.13.8, which showed that a British voice needs `lexicon-gb-en.txt`
  and the language `en`, and that `en-gb` gives no sound. The human listened to all 13 and kept them all. Decided with
  the human: the 13 voices, British pronunciation for a British voice, and the choice on the settings page only.
  Decided by me and open to the human: the order and the names in the list, and a choice during a reading heard at
  the next reading. Left beyond the request: a button that plays a sample, and the voice in the tool window. The
  status is `draft`
- 2026-11-19T22:38:49Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `2d19269`, at the human's request, took in item 2 of "Beyond the request", a read-only row of
  the voice in the tool window, as requirement 8, and renumbered the requirements after it. Item 1, a button that
  plays a sample, stays out. Decided by me and open to the human: the row shows the voice of the reading while there
  is one. The status is `draft`
- 2026-11-19T22:41:07Z [human] approved the spec. The status is `approved`
