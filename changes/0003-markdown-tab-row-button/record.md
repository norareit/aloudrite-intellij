---
kind: feature
status: draft
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

## Amendments
