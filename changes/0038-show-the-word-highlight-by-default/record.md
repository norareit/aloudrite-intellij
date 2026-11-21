---
kind: task
status: implemented
timestamp: 2026-11-21T22:22:08Z
---

# 0038 Show the word highlight by default

## Description

Unit 0027 made "Highlight the word being read" under Settings › Tools › AloudRite start cleared, because the word was
then estimated from the share of the piece heard and skipped words. Unit 0028 finds the word by aligning the audio of
each piece with its text, and in its measurement and in the developer's checks on Linux x64 and on macOS on Apple
Silicon the highlight followed the voice well at 1.0× and at 1.5×. The word highlight now starts checked, as the other
three choices of the highlights do:

* The default of the word in the choices of the highlights is checked, so that a reading shows the word as well as the
  sentence until the user clears it
* The comment of `HighlightSetting.kt` and the passage of `README.md` on the four checkboxes say that all four are
  checked until they are cleared, and no longer that the word is cleared because it is an estimate. The passage may say
  that the word is estimated where the aligner model is not on the machine, as unit 0028 describes
* The tests that hold the defaults, of the settings, of the page "AloudRite", of the highlighter and of the preview,
  expect the word to be shown with the defaults

No stored choice is converted. The IDE keeps a choice only where it differs from the default, so a cleared word, which
was the default, was never kept, and after this task the word is checked for everyone, also for those who had cleared
it, until they clear it again. No build of the plugin has been given to anyone but the developer, so this reaches no
one else.

The task is built after unit 0028 is merged into `master`, as its reason rests on that unit. Afterwards `./gradlew test`
passes.

Not part of this task: the defaults of the other three choices, the way the word is found, and the aligner model and
its download.

## Log

- 2026-11-21T22:22:08Z [human] decided, after the checks of unit 0028 on Linux x64 and on macOS, that the highlight of
  the word being read should start checked, as requirement 13 of unit 0028 says is a unit of its own
- 2026-11-21T22:22:08Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `41daa51`, read `process.md`, the standards and `roles/specifier.md` of the installed kit,
  8.4.0. Made the unit a task: one default, its comment, a passage of `README.md` and the tests that hold the defaults.
  Numbered it 0038, after 0037. Found that `HighlightSetting` stores its choices through the IDE, which writes a value
  only where it differs from the default, so that no stored choice needs converting and a cleared word becomes checked.
  The status is `draft`
- 2026-11-21T22:35:04Z [human] approved the record. The status is `approved`
- 2026-11-21T22:36:58Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on `task-0038-show-the-word-highlight-by-default`
  cut from `master` at `7b40778`, which holds unit 0028 as `implemented`, wrote `plan.md`. It numbers the record's
  requirements R1 to R5, changes the four tests of the defaults first, and sets the default of the word in both
  `HighlightChoices` and `StoredHighlights`, since the IDE writes a stored value only where it differs from the latter.
  The status stays `approved`
- 2026-11-21T23:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, began the work on `task-0038-show-the-word-highlight-by-default` and found no review files for the unit in `.reviews/`. The status is `in-progress`
- 2026-11-21T23:12:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, changed the four tests of the defaults first and saw exactly those four fail with `./gradlew test` on the four classes, then set the default of the word to checked in `HighlightChoices` and `StoredHighlights`, rewrote the comment of `HighlightSetting.kt` and the passage of `README.md`. `./gradlew test` (all tests, the tests with the real engine not asked for) and `./gradlew buildPlugin` pass, and the grep of the plan finds nothing, on the tree of `ec93bab`. Not checked: the capture of the settings page in the sandbox IDE, since `./gradlew runIde` opens a window and does not run in the sandbox
- 2026-11-21T22:41:48Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0038 in full at
  `1a5dea27b0b32817e051c6f4d18e9031046c61c9`, against merge base `7b40778` with `origin/master`. Returned
  non-blocking finding P1 in `.reviews/0038-show-the-word-highlight-by-default/review-round-1.md`. Independently ran
  `./gradlew test buildPlugin`, a fresh `./gradlew cleanTest test`, and `npm run check-changes`, which passed;
  the real engine and aligner tests ran, while opt-in measurements and `RealModelPackageTest` were skipped.
  Could not verify the settings-page capture without a display, or the historical test-first failing run.
- 2026-11-21T23:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took the finding P1 of `review-round-1.md`, not blocking, as accepted by the definition of the orchestrator and not as a decision of the human: ticked the two commit points of `plan.md`, which `c87f06c` and `ec93bab` made. The review has no other finding. The status is `in-progress`
- 2026-11-21T22:43:02Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0038 at `db2e67a` as a
  delta from `1a5dea27b0b32817e051c6f4d18e9031046c61c9`, the commit of the first review. Verified the correction
  of P1 and returned no new findings. Saved `.reviews/0038-show-the-word-highlight-by-default/review-round-2.md`.
  `git diff --check`, `npm run check-changes` and `./gradlew test buildPlugin` passed; Gradle reported tests and
  build outputs up to date. The settings-page capture and historical test-first failing run remain unverified.
- 2026-11-21T22:50:30Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on `task-0038-show-the-word-highlight-by-default` at `5c1b4e5`, did the last task of `plan.md`: started the
  sandbox IDE with `./gradlew runIde`, whose `aloudrite.xml` holds the reading speed, the speech model and the voice and
  no highlight choice, opened Settings › Tools › AloudRite and captured the Settings window alone with `xwd`. All four
  boxes of the highlights, the word among them, are checked. Ticked the task
- 2026-11-21T22:52:40Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, ran `./gradlew test buildPlugin`
  on the tree of `5c1b4e5`, the tree of this commit but for its documents, and it passed. Which tests with the real
  engine were skipped was not checked. Every task of `plan.md` is ticked, and the reviews of both rounds
  returned no blocking finding: P1 of the first round was applied and the second round found nothing. The human asked
  for the unit to be set implemented once the check of the settings page passed, and left the log timestamps of the
  implementer's earlier entries as they are. The status is `implemented`

## Amendments
