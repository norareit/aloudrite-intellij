---
kind: feature
status: in-progress
timestamp: 2026-11-19T13:16:49Z
---

# Highlight the words being read

## Log

- 2026-11-19T13:16:49Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `b4d5317`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0025, after 0024. Checked that the
  Java binding of sherpa-onnx 1.13.8 hands over only samples, with no times for words. Decided with the human: a light
  highlight of the sentence and a strong one of the estimated word, the editor in this unit and the preview in unit
  0026, and scrolling by itself. Decided by me and open to the human: the sentence rather than the piece, the
  highlights staying through a silence, when the scrolling stops and comes back, and colours from keys of the
  plugin's own. Left beyond the request: a setting that turns the highlights off, and a page for their colours. The
  status is `draft`
- 2026-11-19T13:30:22Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `375cc6a`, at the human's request, took in item 1 of "Beyond the request", a checkbox on the
  plugin's settings page that turns the highlights off, as requirement 15, and renumbered the requirements after it.
  The checkbox also covers the preview of unit 0026. Decided by me and open to the human: its wording, and that it is
  checked until cleared. Item 2, a page for the colours, stays out. The status is `draft`
- 2026-11-19T13:31:36Z [human] approved the spec. The status is `approved`
- 2026-11-19T13:52:18Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by AgentRite's orchestrator, on
  `feature-0025-highlight-the-words-being-read` at `05e3d29`, wrote `plan.md` against the tree of that branch. Read
  `process.md`, the standards and `roles/planner.md` of the installed kit, 8.4.0. Every task names the requirements it
  delivers, and every requirement has a task. Requirement 18 is left to the developer. Decided for the plan, as how and
  not as what is true:
    * The reading keeps its placed parts, so that one cut gives both the engine's segments and the places of the words
    * The word's share is the one the bar of unit 0022 already uses
    * The position is read from the player when the editor asks, 20 times a second
    * Edits are mapped through a log of the document's changes from the start of the reading
    * A reading whose file changed between the press and its start, as during a download, shows no highlights
    * Every caret move in an editor ends its following
  The status is `approved`
- 2026-11-19T14:05:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator, on
  `feature-0025-highlight-the-words-being-read` at `395ae8c`, began the work. Read `process.md`, the standards and
  `roles/implementer.md`. No review files exist for the unit yet. The status is `in-progress`
- 2026-11-19T14:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on
  `feature-0025-highlight-the-words-being-read`, built the unit by the tasks of `plan.md`. The tests of the segments, the estimate and the highlighter were written with the code and not strictly before it.
  Two commit points of the plan were made in one commit: the estimate and the mapping of the edits, since the second was
  small and had no dependency on the editor. `./gradlew test` was run at each commit with the model in place, so that the
  tests with the real engine (`KokoroSpeechEngineTest` and the suites that use it) ran and were not skipped. The four classes
  that skip are the measurements `EngineSilenceMeasurement`, `EngineThreadsMeasurement` and `ReadingTimeMeasurement`, which
  run only on request, and `RealModelPackageTest`, which needs `ALOUDRITE_MODEL_PACKAGE` to name a package file and has
  nothing to do with the text handed to the engine. On the final tree before the last documents commit,
  `./gradlew test` reported 743 tests, 4 skipped, 0 failed
- 2026-11-19T14:41:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, decided in the work, as how and not
  as what is true:
    * A reading whose `WhatToRead` has no document stamp, which is a file read from disk because it had no document at the
      press, shows no highlights, since the document that an editor opened later holds may differ from the disk text in its
      line separators and so in its offsets
    * `ReadAlongHighlighter` takes the interval of its timer as a third constructor parameter, so that the tests refresh at
      the moments they choose and the timer of the service itself is tested for its delay of 50 milliseconds
    * The existing expectations of `AudioPlayerTest` for the calls to the line now hold the one call that reads the
      position before a close and a discard
    * The weight of the share, the sentence of a word and the places of "code example", "see table" and "link" are as
      `plan.md` says, and a number or a full stop whose place lies within a block that is read as a whole belongs to that
      block's word, as the rule of the place of a character gives
- 2026-11-19T14:42:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, could not make the captures of
  requirement 17: the sandbox has no display and no `xwd`, `xwininfo` or X server, as `README.md` says of `runIde`. The
  look of the highlights in a light and a dark theme, the scrolling by itself, the selection and caret over the highlights
  and the checkbox on the settings page were therefore not looked at. The two keys are the fallbacks that `plan.md` named
  to begin with: `ALOUDRITE_HEARD_SENTENCE` falls back to `EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES` and
  `ALOUDRITE_HEARD_WORD` to `EditorColors.SEARCH_RESULT_ATTRIBUTES`. Whether they stand out enough from each other and keep
  the text readable in both themes is not checked, and nor is that the listener registered in `plugin.xml` reaches the
  highlighter in a running IDE, which is checked only by the descriptor of the plugin in a test. Both are left to the
  developer
- 2026-11-19T14:43:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew buildPlugin`, which built
  `build/distributions/aloudrite-intellij-0.0.1.zip` of about 20.7 MB, and the change check, which passed on the committed
  branch, and `README.md` now describes the highlights, the estimate, the scrolling and the checkbox. The status
  stays `in-progress`: requirement 18, the developer's reading of `samples/markdown-features.md` at 1.0× and 1.5×, and the
  look in a running IDE are open
- 2026-11-19T14:24:11Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0025 at
  `70f2dd9c26d960504361c613418f553a17346646`, a full review against merge base
  `05e3d290b3e43c28f5d588a87a537ae1683e0f61`. Returned S1, S2, S3, S4, S5 and P1 in
  `.reviews/0025-highlight-the-words-being-read/review-round-1-70f2dd9.md`. Ran `./gradlew test --rerun-tasks`, which
  passed with the real-engine tests run, `./gradlew buildPlugin`, `npm run check-changes` and `git diff --check`.
  Could not verify the look and interaction in a running IDE without a display, or the developer's listening check
  at 1.0× and 1.5×. The status stays `in-progress`
- 2026-11-19T15:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator, on
  `feature-0025-highlight-the-words-being-read`, took in the findings of `.reviews/0025-highlight-the-words-being-read/review-round-1-70f2dd9.md`.
  Accepted by the definition of the orchestrator, not as decisions of the human, and applied in `-` with a regression test
  for each, written and seen to fail before the fix:
    * S1: the sentences are cut from the text with each character that was made a space for the engine counted as a
      character that is no white space, as in a reading from the caret, so the highlight sentence around an emoji is the
      caret's. The words keep which characters were substituted
    * S2: the words are found in the whole of the words before they are cut into pieces. A word that a piece limit cuts has
      its whole place in each piece, with the weight of the part that the piece holds
    * S3: a number of an item carries the marker of the file, such as `10.`, and the word of the number covers it, also
      when the number is added back for a selection
    * S4: a position in the silence that follows the piece heard finishes that piece's word, with no position in its last
      word needed. A silence that does not follow the piece heard changes nothing
    * S5: `TextEdits` follows the runs of the original characters that are left, so that inserted text does not keep a range
      once all of them are gone
- 2026-11-19T15:21:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, proposes declining P1, the missing
  exception for the tests written with the code and not before it, as an action for the implementer: the order of
  work in the first round cannot be restored by a later change and the admission stays in this record, so the exception is
  the human's to grant or refuse. The corrections of this entry followed the required order. The proposal is open until the
  human decides it
- 2026-11-19T15:22:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew test` on `-` with the
  model in place: 751 tests, 4 skipped as before, 0 failed. The status stays `in-progress`
- 2026-11-19T15:49:09Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, on
  `feature-0025-highlight-the-words-being-read` at `4bebbe8`, recorded the human's decision on P1 of
  `.reviews/0025-highlight-the-words-being-read/review-round-1-70f2dd9.md`: the human granted an exception for the
  tests of the segments, the estimate and the highlighter that were written with the code in the first round and not
  before it. The exception covers that round alone, and the admission above stays as it is. P1 is decided. The status
  stays `in-progress`
- 2026-11-19T16:11:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, on
  `feature-0025-highlight-the-words-being-read` at `c8eb296`, recorded the human's check of requirement 18 at 1.0×.
  The human watched `samples/markdown-features.md` being read in the text editor of the sandbox IDE at 1.0×. The
  light highlight was on the sentence being heard. The strong highlight was not always on the word being spoken, and
  now and then it skipped a word. The human judged that the estimate leaves little room for improvement without
  times of the engine for words, and asked for the highlights to be chosen one by one, which unit 0027 specifies. The
  check at 1.5× is open. The status stays `in-progress`
- 2026-11-19T18:31:14Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, on
  `feature-0025-highlight-the-words-being-read` at `2667d92`, made the captures of requirement 17 in the sandbox IDE
  of `./gradlew runIde`, with `samples/markdown-features.md` in the text editor beside its preview, at the speed of
  1.3× that the sandbox had set. Each capture was of the sandbox IDE's own window, or of the part of it that its
  Settings dialog covered. In the dark theme Islands Dark:
    * the sentence has a faint grey band and the word a muted dark green, which stands out from the sentence, and the
      text stays readable under both
    * the code block, from its opening fence to its closing one, has the strong highlight while "code example" is
      heard, and keeps it through the silence after it
    * a selection over a highlighted sentence is drawn over the highlights, and the caret shows
    * a reading that starts below the view scrolls the editor to its first sentence, and the editor followed the
      highlight from line 11 to line 96 without a move of the user. In an earlier reading the following stopped
      after the human worked in the sandbox IDE, as requirement 12 says it does
    * a pause keeps both highlights where they were, and a stop removes them
    * the page Settings › Tools › AloudRite shows the checkbox "Highlight the words being read", checked, in a row
      under the speed

  In the light theme Islands Light, the sentence has a pale lavender band and the word a stronger blue-lavender. The
  word stands out, though less than in the dark theme, and the text stays readable; a selection and the caret show
  over the highlights, and the code block is highlighted whole as in the dark theme. The settings page was not
  captured in the light theme. The background of the code block and of the front matter, teal in the dark theme and
  green in the light one, is the IDE's own background of an embedded language and not a highlight of the plugin. The
  keys keep the fallbacks named above. Not looked at: an edit during a reading, a change of the speed during a
  reading and the checkbox cleared during a reading. During the captures, keystrokes of the human reached the sandbox
  IDE and changed line 97 of `samples/markdown-features.md`; the file was restored from git and is unchanged. The
  status stays `in-progress`
- 2026-11-19T18:32:39Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, on
  `feature-0025-highlight-the-words-being-read` at `3874ca0`, corrected the entry of the human's check: the readings
  the human watched were those of the captures, at the speed of 1.3× that the sandbox had set, and not at 1.0×. What
  the human saw stands, at 1.3×. The checks of requirement 18 at 1.0× and at 1.5× are both open. The status stays
  `in-progress`
