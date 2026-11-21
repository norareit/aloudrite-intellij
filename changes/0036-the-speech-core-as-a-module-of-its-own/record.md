---
kind: feature
status: draft
timestamp: 2026-11-21T11:56:00Z
---

# The speech core as a module of its own

## Log

- 2026-11-21T11:56:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `27d1d17`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0036, after 0035. Went through
  the imports of every source file of the plugin and the constructor of `SpeechService`, as the context of the spec
  says. Decided with the human: a module with no dependency on the IntelliJ Platform, now, and no port to TypeScript.
  Decided by me and open to the human: a module of the build rather than a library, the thing read as a type of the
  caller's choosing, the download of the model and the highlights kept in the plugin, and the package
  `com.ritenoar.aloudrite.core`. Left beyond the request: publishing `core` as a library, and moving the download of
  the model to `core`. The status is `draft`

## Amendments
