---
kind: bug
status: in-progress
timestamp: 2026-11-20T14:33:41Z
---

# The tests of the shortcuts in tooltips pass on macOS

## Log

- 2026-11-20T14:33:41Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, on the Mac, on `master` at `8e716ff`,
  at the human's request, wrote `spec.md` for the 8 tooltip tests that the checks of unit 0030 found failing on macOS on
  Apple Silicon, for which the human chose a bug unit of its own. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a bug and numbered it 0031, after 0030. Reproduced the
  failures on `8e716ff`, on `b4d5317` before it and on `f1aedc3`, and found from `git log -S` that the tests come from
  units 0013, 0017 and 0024. Left beyond the request: running the whole suite whenever a unit is checked on macOS. The
  status is `draft`
- 2026-11-20T14:40:16Z [human] approved the spec. The status is `approved`
- 2026-11-20T14:41:16Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on the Mac, on
  `bugfix-0031-shortcut-tooltip-tests-on-macos` cut from `master` at `50428c6`, wrote `plan.md`. The fix needs a plan:
  the keystrokes the tests hold move to a file of their own as a refactoring of its own, before the 8 tests change
  what they expect, which is two commits, and the run on Linux x64 is the developer's. Read `roles/planner.md` of the
  installed kit, 8.4.0. The status is `approved`
- 2026-11-20T14:42:13Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, on the Mac, began the work at `bc1f04f`. Ran the
  four test classes on the branch on macOS: of 104 tests, the 8 of the spec failed with the messages it gives, and the
  other 96 passed. The status is `in-progress`
- 2026-11-20T14:44:05Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, on the Mac, moved the keystrokes of the plugin's
  shortcuts and the rule of Cmd for Ctrl from `ReadingShortcutsTest` to `PluginShortcuts.kt` in `b666407`, after which
  the four test classes gave the same 8 failures and the keymap tests passed, and in `09b4989` had the 8 tests expect the
  shortcut of the keymap in use, from those keystrokes and the names of the keymap in use and its parents. Verified on
  macOS on Apple Silicon, on an Apple M3 Max with macOS 15.1.1, at `09b4989`: the four classes ran 104 tests with no
  failure; `./gradlew test` with both models in place ran 846 tests with no failure and 4 skipped, the opt-in
  `EngineSilenceMeasurement`, `EngineThreadsMeasurement` and `ReadingTimeMeasurement`, and `RealModelPackageTest`.
  With the keystroke of "Read Aloud" in the table changed to `ctrl alt shift R` for one run, not committed, 6 tests
  failed, the 5 tooltip tests that name the shortcut of "Read Aloud" or "Pause Reading" and the keymap test, so a
  tooltip naming another shortcut still fails them. `git diff` from `master` names no file under `src/main`. Not
  verified: the run on Linux x64 of requirement 2, which is not made on this machine and waits for the developer. The
  status is `in-progress`
- 2026-11-20T14:57:04Z [human] ran `./gradlew test` on Linux x64 on the unit's branch at `042fcd3`: 846 tests, no failures and 4
  skipped. Which tests were skipped was not reported. With it, requirement 2 holds. The entry was written by Claude
  Code, anthropic/claude-opus-5-5, on the human's word
- 2026-11-20T14:57:04Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, ticked the run on Linux x64 in `plan.md`. Every
  task of the plan is ticked. The unit waits for the review. The status is `in-progress`
- 2026-11-20T15:04:20Z [OpenCode, openai/gpt-6.2-sol] as the reviewer, reviewed unit 0031 in full at `0fed813`.
  Returned C1 in `.reviews/0031-shortcut-tooltip-tests-on-macos/20261120T150351Z-opencode-gpt-6.2-sol-full.md`.
  Reproduced the eight baseline failures and ran the full suite on macOS. Did not independently rerun Linux x64 or
  repeat the temporary keystroke change.

