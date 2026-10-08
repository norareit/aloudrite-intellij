---
kind: feature
status: in-progress
timestamp: 2026-10-08T16:03:30Z
---

# Markdown tab row button

## Log

- 2026-10-08T15:33:05Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`. The unit is a feature, because it fixes where the button goes, what counts as a Markdown
  file and which text is shown. The place of the button was settled with the human before writing: a throwaway
  trial, built in a scratch directory outside the repository and deleted afterwards, added an action to the
  platform's group `EditorTabsToolbarActions`, and the human saw its button in the editor tab row of a sandbox
  IntelliJ IDEA 2026.2.3, between the layout icons and the `⋮` menu, and rejected the Run icon the trial used.
  Not established: that the button is absent for a file that is not Markdown and that pressing it works, which the
  trial was built to show and the human did not report on. Status left at `draft` for the human to approve.
- 2026-10-08T15:34:52Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, fixed the icon in `spec.md` at the human's request: requirement 5 now asks for a loudspeaker, where it
  asked for an icon that suggests speech or sound. Status still `draft`.
- 2026-10-08T15:52:54Z [human] approved `spec.md` as it stands. Status set to `approved`.
- 2026-10-08T15:52:54Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, noted a remark of the human for whoever plans and builds this unit: the human wants no one-letter names,
  and read `e`, the event parameter of unit 0001's `SayHelloAction`, as breaking the naming rule in `process.md`.
  The action this unit adds names that parameter in full. This is not a requirement of the spec. It is the
  existing rule, which unit 0001 missed.
- 2026-10-08T15:58:42Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux
  host, cut the branch `feature-0003-markdown-tab-row-button` from the head of `master` and wrote `plan.md`. Read
  from the classes of IntelliJ IDEA 2026.2.3: the tab row hands an action of `EditorTabsToolbarActions` the file of
  its selected tab, and the platform ships no loudspeaker icon, so the plan draws one. Established in a throwaway
  trial, built in a scratch directory outside the repository and deleted afterwards: with the Markdown plugin as a
  test-only dependency and no project opened, six tests of the planned action and dialog passed under
  `./gradlew test`, among them one with a change that is not saved, and the packaged descriptor kept its one
  `<depends>`. That answers the concern in the record of unit 0001 for this unit: nothing here needs a project.
  Not established, and left to the checks in a sandbox IDE: that pressing the button in the real tab row works,
  how the dialog looks, and whether the drawn icon reads as a loudspeaker, which nobody has seen. Status set to
  `planned`.
- 2026-10-08T16:03:30Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, read the spec, the plan and this record and began. Status set to `in-progress`.
- 2026-10-08T16:12:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the Linux
  host, built the unit in two commits. The new test failed to compile before `FileContentDialog` existed, then all
  eight tests of `ReadAloudActionTest` passed under `./gradlew test` (reported `tests="8" failures="0" errors="0"`),
  and `./gradlew clean buildPlugin` succeeded followed by a second passing `./gradlew test`, on the tree of the
  README commit. In the jar, `META-INF/plugin.xml` holds `com.intellij.modules.platform` as its only `<depends>`,
  both icon files are there, and the classes are only `FileContentDialog` and `ReadAloudAction`. Two deviations: the
  plan's check that `grep -rn -i 'hello' src README.md` finds nothing cannot hold, because the plan's own test for
  requirement 10 names `Say Hello` and `SayHello` to assert their absence. Only those lines in
  `ReadAloudActionTest.kt` match, and that box is left unticked for the review to decide. The test for a Markdown
  type in `notes.md` uses `Runnable { ... }` for `runWriteCommandAction`, because the Kotlin lambda overload is
  ambiguous. Not checked: every box of the plan's Verification that needs the sandbox IDE (button placement in
  the three layouts, its tooltip, absence for non-Markdown, the icon drawing, the dialog by eye, the split editor,
  the Markdown plugin disabled). A display is set in this shell, but no IDE window was started or looked at, so
  those checks are left to the review and the human, as the spec assigns them. Status stays `in-progress` until the
  review's findings are decided.

## Amendments
