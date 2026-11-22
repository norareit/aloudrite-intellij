---
kind: bug
status: draft
timestamp: 2026-11-22T13:36:05Z
---

# 0042 The tests of the model management leave install locks behind

## Log

- 2026-11-22T13:36:05Z [human] asked for a bug unit for the failure of `InstallAttemptTest` that depends on the
  order of the test classes, after a separate agent session traced it to `SpeechModelManagementTest`
- 2026-11-22T13:36:05Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, in a worktree of `master` at `edecf6d`, read `process.md`, the standards and `roles/specifier.md` of the
  installed kit. Made the unit a bug, numbered 0042 after 0041. Wrote the observed behavior and the root cause from the
  report of that session, and marked what it did not confirm. The status is `draft`

## Amendments
