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

