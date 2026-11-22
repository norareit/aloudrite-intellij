---
kind: bug
status: in-progress
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
- 2026-11-22T22:44:16Z [human] approved the spec. The status is `approved`
- 2026-11-22T23:39:36Z [Claude Code, anthropic/claude-opus-5-5] as the planner, wrote `plan.md` against `d5ab50b`. A
  temporary suite that ran `SpeechModelManagementTest` before `InstallAttemptTest`, in a worktree removed afterwards,
  reproduced the failure on demand and settled what the spec left unconfirmed: five tests leak, three through a stored
  download work and two through a stored removal work. The section "Fix" of the spec names the download works only, and
  requirement 1 covers both, so the plan runs the works of both lists. The status stays `approved`
- 2026-11-22T23:59:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, began the work. The status is
  `in-progress`
- 2026-11-22T23:59:10Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, added the `@After` check to
  `SpeechModelManagementTest` alone and ran the class: 17 of 26 tests failed, the five tests of the plan's table among
  them and the rest through the property that a leaking test left in the JVM. Then added the running of `downloadWorks`
  and `removalWorks` before the check: the class passes, 26 of 26
- 2026-11-22T23:59:20Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, found necessary work outside
  the plan. With the check in place, `./gradlew test --rerun` failed 26 tests of `SpeechModelManagementTest`, because the
  check looks at every property of the JVM and `ModelDownloadFlowTest` ran before it and leaves one `heldInstallLock`
  property behind, from a stored `keptWorks` work that never runs. A temporary suite of `ModelDownloadFlowTest` then a
  check confirmed it, and the planner's measurement had not found it. Added the running of `keptWorks` at the start of
  `cleanUp` in `ModelDownloadFlowTest`, listed that file in `plan.md`. Only tests changed, as the spec decides. Of
  the other test classes that touch the model download, 11 were probed the same way, one at a time before a check,
  and none leaves a property: `ModelDownloadServiceTest`, `ModelInstallerTest`, `ModelDownloadMessagesTest`,
  `RealModelDownloadTest`, `ReadingShortcutsTest`, `ReadAlongPreviewTest`, `ReadAlongHighlighterTest`,
  `ReadFromTheCaretTest`, `ReadAloudActionTest`, `ReadingTitleActionsTest`, `ReadingPanelTest`, and
  `PackageDownloadTest`. The other classes were not probed
- 2026-11-23T00:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, verified on the final tree of the
  two test files: a temporary suite of `SpeechModelManagementTest` then `InstallAttemptTest` passed (R3), the
  temporary files were deleted, and `./gradlew test --rerun` passed (R4, `BUILD SUCCESSFUL`, with the tests
  that need the speech model counted as the build counts them). `git diff origin/master -- src/` shows the two test
  files only and no test removed. Not checked: macOS, and a forced run in which Gradle starts the failed classes first
- 2026-11-22T23:46:15Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a full review of unit 0042 at
  `8c715fedae8337e2e0e03a4d55862b821c7f4845`, against merge base `d5ab50b12455a09199806bfea40acda184bb5d7e`.
  Returned nonblocking finding C1, with no spec-conformance or plan-fidelity findings, saved in
  `.reviews/0042-model-management-tests-leave-install-locks-behind/review-20261122T234615Z.md`. Ran
  `./gradlew test --rerun`, `npm run check-changes` and `git diff --check`, all passing. Could not independently
  verify the historical failing regression run or the temporary ordered-suite proof for R3; the temporary files are
  absent and were not recreated under the reviewer's no-change rule. The status stays `in-progress`

## Amendments
