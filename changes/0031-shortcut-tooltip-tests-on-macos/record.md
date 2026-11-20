---
kind: bug
status: approved
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
