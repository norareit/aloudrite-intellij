---
kind: feature
status: in-progress
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
- 2026-11-21T22:35:01Z [human] approved the spec. The status is `approved`
- 2026-11-22T01:04:19Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of the sandbox, on the
  branch at `433363f`, wrote `plan.md`. Read `process.md`, the standards and `roles/planner.md` of the installed kit.
  The tree has moved on since the spec was written at `27d1d17`: unit 0028 added the word alignment and the common type
  of the downloaded packages, and `SpeechService` now also takes the aligning of the pieces and the delay of the word.
  The alignment finds which word is heard and the reading uses it, so it moves to `core` under requirement 2, which the
  plan says, and nothing in the spec conflicts with the tree. Decided in the plan: the class of requirement 3 is
  `ReadingAloud<Target>`, with the interfaces of the reading made generic and the service of the plugin holding it as
  `reading`; the shared stand-ins as test fixtures of `core`; the Kotlin standard library left to the IDE as now; the
  commits that relocate code to `core` change only the package, the imports and the references the package forces. The
  status stays `approved`

## Amendments
