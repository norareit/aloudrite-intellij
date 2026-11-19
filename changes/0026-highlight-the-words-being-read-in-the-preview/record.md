---
kind: feature
status: in-progress
timestamp: 2026-11-19T13:16:49Z
---

# Highlight the words being read in the preview

## Log

- 2026-11-19T13:16:49Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `b4d5317`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0026, after 0025, on which it
  builds. Checked in the Markdown plugin of IntelliJ IDEA 2026.2.3 that the elements of the preview carry the ranges of
  the file they were made from, and that the plugin has an extension point for scripts and a pipe for messages to the
  page. Decided with the human: the preview in a unit of its own, after unit 0025, and scrolling by itself. Decided by
  me and open to the human: drawing with the highlights of the browser without changing the page, an optional
  dependency on the Markdown plugin that fails quietly, and each view following by itself. Left beyond the request: a
  notification when the preview cannot show the highlights. The status is `draft`
- 2026-11-19T13:30:22Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `375cc6a`, added to requirement 7 that the checkbox unit 0025 took in as its requirement 15
  turns the highlights off and on in the preview as well. The status is `draft`
- 2026-11-19T13:40:11Z [human] approved the spec. The status is `approved`
- 2026-11-19T21:48:07Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by AgentRite's orchestrator on the Linux
  host, cut `feature-0026-highlight-the-words-being-read-in-the-preview` from `master` at `3cd6e83` and wrote `plan.md`.
  Read `process.md`, the standards and `roles/planner.md` of the installed kit, 8.4.0. Read the classes and scripts of
  the Markdown plugin of IntelliJ IDEA 2026.2.3 for the extension point, the pipe, the attribute of the ranges, the
  patching of the page and the scroll sync, and wrote what they do into the plan. Decided in the plan: the page
  describes its text to the IDE and the words are found in Kotlin, so that the tests need no browser; the provider
  registered in an optional descriptor, since the panel reads its extensions only when it is made; and a check in the
  sandbox IDE of whether the platform reports an error for a missing extension point, logged as a finding for the
  human where it does. The status is `approved`
- 2026-11-19T22:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator on
  the Linux host, on `feature-0026-highlight-the-words-being-read-in-the-preview` at `22e351e`, began the work. Read
  `process.md`, the standards and `roles/implementer.md` of the installed kit. No review of this unit exists yet. The
  status is `in-progress`
