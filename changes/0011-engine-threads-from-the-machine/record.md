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
- 2026-10-19T17:56:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the
  Linux host (Lestat-budgie), on the branch `feature-0011-engine-threads-from-the-machine`, read `process.md`,
  `roles/implementer.md` and the three documents of the unit, and moved the status to `in-progress`. Built tasks 1 to 7
  of `plan.md` in the commits of its commit points: `engineThreadCount` with `EngineThreadsTest` (the test and the
  function were written together, so the test was not seen failing to compile), the engine's constructor parameter,
  `SpeechService.createKokoroEngine` with the log line, and `EngineThreadsMeasurement`. `./gradlew test` after the
  engine change ran 198 tests, none failed, one skipped (`RealModelPackageTest`); after the measurement class was
  added, `EngineThreadsMeasurement` is skipped as well, which was seen in its own result file only and not on a
  full run of the suite
- 2026-10-19T17:56:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, measured on the Linux host
  (Lestat-budgie, AMD Ryzen 9 9950X3D) with `ALOUDRITE_MEASURE_ENGINE_THREADS=1 ./gradlew cleanTest test --tests
  com.ritenoar.aloudrite.EngineThreadsMeasurement`, at about 17:49 to 17:55 UTC, with IntelliJ IDEA open (about 12%
  of a core), a game process of about 10% and htop running; the load average rose from 2.8 to 5.5 over the run.
  `build/engine-threads.txt`:

  ```
  processors 32, os Linux, arch amd64
  threads  first audio (ms)  audio (s)  audio per second of work
        1              1797     146.78                      1.96
        2              1443     146.78                      2.40
        4              1326     146.78                      2.71
        6              1264     146.78                      2.79
        8              1260     146.78                      2.70
  ```

  From 2 to 4 threads the first audio is 117 ms sooner and the audio per second of work 0.31 higher; from 4 to 6, 62 ms
  and 0.08; from 6 to 8, 4 ms sooner and 0.09 lower. One run per machine state, so the differences of the last
  steps are within what noise could give; not checked by repeating
- 2026-10-19T18:50:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, logged the measurement of the Mac
  that the human handed over, made by Claude Code on the Mac at the human's request, with the same command as on the Linux
  host, on the branch at `94aca96`, from 20:38:19 to 20:45:41 CEST on 2026-10-19, 7m 21s in Gradle, the test passed and
  was not skipped. The machine is an Apple M3 Max, with 16 physical and 16 logical CPUs. Running at the time: IDEA, Steam
  and Yousician were quit, `kernel_task` took about 12% before the run, Chrome and the Claude session were open, and the
  process list was not checked during the run. `build/engine-threads.txt`, as handed over:

  ```
  processors 16, os Mac OS X, arch aarch64
  threads  first audio (ms)  audio (s)  audio per second of work
        1              2144     178.46                      1.95
        2              1903     146.72                      1.87
        4              1760     146.72                      2.03
        6              1648     146.72                      2.12
        8              1648     146.72                      2.19
  ```

  The row of 1 thread made 178.46 s of audio and the others 146.72 s, so its audio per second of work is not strictly
  like for like with the others. On the Linux host every row made 146.78 s. The cause was not looked into. From 1 to 2
  threads the first audio is 241 ms sooner, from 2 to 4 143 ms, from 4 to 6 112 ms and from 6 to 8 none. The audio per
  second of work is not monotone (1.95, 1.87, 2.03, 2.12, 2.19) and rises by 0.16 from 2 to 4, 0.09 from 4 to 6 and
  0.07 from 6 to 8. With 16 processors the rule of requirement 1 gives 4 threads here
- 2026-10-19T19:00:00Z [human] after seeing both tables, set the cap of the engine's threads to 6 in place of 4, in the
  main session on the Linux host. The entry was written by Claude Code, anthropic/claude-sonnet-5-5, on the human's word
- 2026-10-19T19:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, stopped on the human's cap of 6, as
  the plan says to when the human sets another cap than 4. Requirement 1 and requirement 4 of `spec.md` name the cap of
  4 and the cases 8, 9 and 32 processors giving 4 threads, and the decision "A cap of 4" and the README text of the plan
  do too, so the unit goes back to the specifier and the human before anything more is built. Nothing of the code is
  changed for 6: `EngineThreads.kt`, `EngineThreadsTest.kt` and the log line still hold 4. The rest of the plan is not
  done: the README, the listening checks on both machines, the final suites and the change check. The status stays
  `in-progress`

## Amendments
