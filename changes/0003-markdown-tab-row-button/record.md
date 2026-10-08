---
kind: feature
status: planned
timestamp: 2026-10-08T15:33:05Z
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

## Amendments
