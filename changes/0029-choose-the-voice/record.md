---
kind: feature
status: in-progress
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
- 2026-11-20T15:13:07Z [human] approved the spec. The status is `approved`
- 2026-11-20T15:26:44Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by the orchestrator of AgentRite on the branch
  `feature-0029-choose-the-voice` at `b33c93e`, wrote `plan.md` against that tree, which holds unit 0030 implemented.
  Read `process.md`, the standards and `roles/planner.md` of the installed kit. The voices are a registry that the
  settings page, the tool window and the engine iterate, the voice is handed to the engine with each piece and taken by
  a reading when it starts, and the language of eSpeak NG follows the accent of the voice. Every requirement, 1 to 12,
  has a task. The status stays `approved`
- 2026-11-20T15:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by the orchestrator of AgentRite on
  the branch `feature-0029-choose-the-voice` at `d5dfc34`, began the work. Read `process.md`, the standards and
  `roles/implementer.md` of the installed kit, and `spec.md`, `plan.md` and `record.md`. The unit has no review yet. The
  status is `in-progress`
- 2026-11-20T16:05:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the branch `feature-0029-choose-the-voice`,
  built the voices, the setting, the engine, the reading, the settings page, the row of the tool window and the README,
  with the tests beside each. The tasks of the engine and of the reading were committed together as `feature-0029 Hand the
  voice and its language to the engine`, so the commit point `Read in the voice chosen when the reading starts` has no
  commit of its own and its box stays open. The constructor of `ReadingSpeedConfigurable` takes the `VoiceSetting` as its
  last parameter and that of `ReadingPanel` takes `voiceInUse` before `openTheSettings`, which the plan left open. The
  tests of the panel assert no state before the notice of a change is dispatched, since `runInEdtAndGet` dispatches the
  pending events itself, and the row of the voice is tested to hold only labels, since a label takes the focus by default in
  Swing and the plan's wording on the focus cannot be tested as such
- 2026-11-20T16:06:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew buildPlugin test` on Linux x64
  on the tree after the commit `feature-0029 Describe the voices in the README`, with both models in place under
  `~/.local/share/aloudrite`. The build succeeded and 905 tests ran with 0 failures and 6 skipped, which are the five
  measurements and `RealModelPackageTest`, skipped by their own design. `KokoroVoicesTest` ran both of its tests, one for each
  model, with none skipped, and every voice of the 13 gave audio with no sample that is not a number. `EspeakPhonemesTest`
  ran 4 tests and `KokoroSpeechEngineTest` 6, none skipped, so the language `en` gave "ʃˈɛdjuːl" and "təmˈɑːtəʊ" and `en-us`
  "skˈɛdʒuːl" and "təmˈeɪɾoʊ". `grep -rn "af_heart" README.md src/main` finds only the table of the voices in `Voice.kt`
- 2026-11-20T16:07:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, did not do the captures of requirement 11:
  the shell of the sandbox has no display and no `xwd` or `xwininfo`, so the sandbox IDE cannot be started and looked at
  here. The two tasks of the captures and the reading of requirement 12 stay open, and are for the developer. Not checked:
  how the row of the voice looks on the settings page and in the tool window, in either theme
- 2026-11-20T15:36:00Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0029 in a full review of
  `fe7ba441d0b488a586158a42ba5b791e1628bca7` against merge base `b33c93efd26c22d160a5ee99391aa9ce23eb8ac9`
  with `origin/master`. Returned S1, S2 and P1, saved in `.reviews/0029-choose-the-voice/review-20261120T153600Z.md`.
  Ran `./gradlew buildPlugin test`, then `./gradlew cleanTest test` to force execution, and `npm run check-changes`;
  all succeeded. Both real-engine voice tests passed without skips with both models in place; the four phoneme tests
  and six speech-engine tests passed without skips. Could not verify the visual checks of requirement 11 or the
  developer's listening check of requirement 12. The status stays `in-progress`
- 2026-11-21T09:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took in the review of `fe7ba44`, accepted by the
  definition of the orchestrator and not as a decision of the human. Finding P1 is accepted: ticked the commit points
  that exist, and merged the two commit points of the engine and of the reading into one, since their tasks were committed
  together, as the earlier entry records. Findings S1 and S2 are accepted as true and cannot be resolved by the
  implementer: the captures of requirement 11 and the reading of requirement 12 need a display and a listener, and stay
  open for the developer. No decline is proposed. The status stays `in-progress`
