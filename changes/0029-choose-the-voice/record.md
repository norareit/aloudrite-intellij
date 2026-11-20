---
kind: feature
status: draft
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
- 2026-11-20T00:43:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `1f49a54`, at the human's request, set the status back to `draft` before any work on the unit
  had started and before its branch was cut. Unit 0030 moves the plugin to sherpa-onnx 2 and has the plugin find the
  phonemes itself, and the human wants this spec corrected once unit 0030 is implemented, when what it brought is
  known, and approved again only then. What is to be looked at again: requirement 4, since the lexicon and the
  language of eSpeak NG would be chosen in the plugin's own front end rather than in the configuration of the
  engine; requirement 6, since a change of the accent may then need no new engine; requirement 5 and its decision,
  since a change of the voice may then be heard at once, as a change of the speed is; requirement 7, on what a model
  in place holds; and requirement 11, the test with the real engine. The status is `draft`
- 2026-11-20T14:49:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `master` at `8e716ff`, at the human's request once the review of unit 0030 at `f905ca7` returned no blocking
  finding, and before unit 0030 is set to `implemented`, corrected `spec.md` from the entry of requirement 15 in the
  record of unit 0030. Read `process.md`, the standards and `roles/specifier.md` of the installed kit. Tried on Linux
  x64 with `piper-phonemize` 1.5.9 from the branch of unit 0030 and the data of eSpeak NG of the fp32 model: `en` gives
  British phonemes, such as "ʃˈɛdjuːl" for "schedule" and "təmˈɑːtəʊ" for "tomato", each a token of the model; `en-gb`
  gives no phonemes; `en-us` gives the American ones. The context now says that the pronunciation is the language given
  to eSpeak NG with each piece, that 1.13.8 never read the lexicon for English, and that no voice needs an engine of its
  own. Requirement 4 names the language of eSpeak NG for each accent in place of a lexicon and a language of the engine.
  Requirement 6 keeps one engine for every voice. Requirement 7 on `lexicon-gb-en.txt` is removed as unneeded, and the
  requirements after it are renumbered, from 8 to 13 now 7 to 12. Requirement 8 no longer says that a change of the
  accent creates the engine again, and requirements 9 and 10 test the language of the phonemes in place of the
  configuration of the engine. The decision that a change of the voice is heard at the next reading is kept with a new
  reason, since no new engine is needed any more, and stays open to the human. The spec is to be approved again. The
  status is `draft`
