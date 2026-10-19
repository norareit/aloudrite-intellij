---
kind: feature
status: in-progress
timestamp: 2026-10-19T15:47:16Z
---

# Engine threads from the machine

## Log

- 2026-10-19T11:54:32Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `dd715b7`, at the human's request, wrote `spec.md` of a new unit: the engine's number of
  threads follows from the machine, half the processors available to the JVM with a floor of 1 and a cap of 4, logged
  once each time the engine is created. It starts from two observations during the checks of unit 0006, recorded in
  `record.md` of unit 0006 on the branch `feature-0006-read-markdown-aloud`: one core kept busy by a test suite of 208
  seconds, and the fans and temperature of the machine going up while the plugin read. Read `process.md` and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0011, after 0010. The fixed number of
  2 was set in the plan of unit 0004 and never agreed in a spec; this unit is where the number is first decided.
  Decided by me and open to the human: half the logical processors as the stand-in for physical cores; the cap of 4,
  to be confirmed by the developer's measurements at 1, 2, 4, 6 and 8 threads on Linux x64 and on macOS on Apple
  Silicon; the floor of 1; the number logged and not shown. Left beyond the request: a setting for the number, a
  system property that overrides it, and reading the physical cores from the operating system. Nothing was measured
  yet. The status is `draft`
- 2026-10-19T15:39:54Z [human] approved the spec as it stands, in the main session on the Linux host, after
  asking why the cap is 4 and not 6 or 8 and being told the specifier's reasons and that the measurements of
  requirement 5 settle it. The human kept the cap at 4, and with it the other choices the specifier left open: half
  the logical processors as the stand-in for physical cores, the floor of 1, and the number logged and not shown. The
  status is `approved`. The status change and this entry were written by Claude Code, anthropic/claude-opus-5-5, on
  the human's word
- 2026-10-19T15:44:01Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux
  host, on the branch `feature-0011-engine-threads-from-the-machine`, cut from `master` at `423c8d7`, wrote
  `plan.md`. Read `process.md` and `roles/planner.md` of the installed kit. The rule of requirement 1 becomes one pure
  function, `engineThreadCount`; `KokoroSpeechEngine` takes the number as a parameter in place of its constant of 2;
  `SpeechService` creates the engine through a factory that asks the JVM for its processors at that moment and logs
  the line of requirement 3. The measurements of requirement 5 are made by a test class that runs only with
  `ALOUDRITE_MEASURE_ENGINE_THREADS` set, so that the same code measures on both machines without a build of the
  plugin; the plan fixes the sentence, the passage and how each figure is taken. Should the measurements lead the
  human to another cap than 4, the plan stops there and the unit goes back to the specifier. Nothing in the spec
  was found unclear. `./gradlew test` on the tree as cut ran 188 tests, none failed, one skipped. The status is
  `planned`

## Amendments
