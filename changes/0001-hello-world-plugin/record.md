---
kind: feature
status: draft
timestamp: 2026-10-08T11:38:32Z
---

# Hello world plugin

## Log

- 2026-10-08T11:38:32Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`. The unit is a feature and not a task, because it fixes the plugin id, the language, the
  target platform and the source of the version. Status left at `draft` for the human to approve.
- 2026-10-08T12:49:12Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, changed the vendor in `spec.md` from "Stef Klaassen" to "Ritenoar" at the human's request, and recorded the
  decision with its rationale. Status still `draft`.
- 2026-10-08T12:52:40Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, reversed the version decision in `spec.md` after the human questioned it: the version is stated in the
  Gradle build only, and `package.json` loses its `version` field. The earlier rationale claimed that npm requires
  the field, which holds only for a published package. Status still `draft`.

## Amendments
