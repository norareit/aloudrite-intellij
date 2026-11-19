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
