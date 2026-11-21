---
kind: task
status: approved
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

## Amendments
