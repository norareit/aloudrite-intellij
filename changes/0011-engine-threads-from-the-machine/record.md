---
kind: feature
status: implemented
timestamp: 2026-10-19T19:23:00Z
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
- 2026-10-19T15:48:53Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the main session on the
  Linux host (Lestat-budgie), on the branch `feature-0011-engine-threads-from-the-machine`, read `process.md`,
  `roles/implementer.md` and the three documents of the unit, and moved the status to `in-progress`. Built tasks 1 to 7
  of `plan.md` in the commits of its commit points: `engineThreadCount` with `EngineThreadsTest` (the test and the
  function were written together, so the test was not seen failing to compile), the engine's constructor parameter,
  `SpeechService.createKokoroEngine` with the log line, and `EngineThreadsMeasurement`. `./gradlew test` after the
  engine change ran 198 tests, none failed, one skipped (`RealModelPackageTest`); after the measurement class was
  added, `EngineThreadsMeasurement` is skipped as well, which was seen in its own result file only and not on a
  full run of the suite
- 2026-10-19T15:54:49Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, measured on the Linux host
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
- 2026-10-19T18:47:23Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, logged the measurement of the Mac
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
- 2026-10-19T18:53:00Z [human] after seeing both tables, set the cap of the engine's threads to 6 in place of 4, in the
  main session on the Linux host. The entry was written by Claude Code, anthropic/claude-sonnet-5-5, on the human's word
- 2026-10-19T18:53:19Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, stopped on the human's cap of 6, as
  the plan says to when the human sets another cap than 4. Requirement 1 and requirement 4 of `spec.md` name the cap of
  4 and the cases 8, 9 and 32 processors giving 4 threads, and the decision "A cap of 4" and the README text of the plan
  do too, so the unit goes back to the specifier and the human before anything more is built. Nothing of the code is
  changed for 6: `EngineThreads.kt`, `EngineThreadsTest.kt` and the log line still hold 4. The rest of the plan is not
  done: the README, the listening checks on both machines, the final suites and the change check. The status stays
  `in-progress`
- 2026-10-19T18:58:20Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, corrected `spec.md` for the human's
  cap of 6: requirement 1 caps the threads at 6, requirement 4 adds 12 and 13 processors and gives 1, 1, 1, 4, 4, 6, 6
  and 6 threads for 1, 2, 3, 8, 9, 12, 13 and 32, the decision "A cap of 4" becomes "A cap of 6" with the reasons from
  the measurements of both machines, and the two items under "Beyond the request" that named the cap of 4 name 6. The
  plan still holds 4, in the tests, the log line it expects and the README text, and follows the corrected spec once
  the human has approved it. The status stays `in-progress`
- 2026-10-19T19:00:16Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, took up the unit again on the
  spec corrected for the cap of 6 (commit `60cb40e`), on the human's word that the spec has been changed. Added a task
  to `plan.md` to raise the cap, and changed the plan's forward-looking lines (the expected log line and the README text)
  from 4 to 6. Wrote the cases for 12 and 13 processors and changed those of 16 and 32 to expect 6, saw those four
  fail, set `MAXIMUM_THREADS` to 6 and ran `./gradlew test`: 201 tests, 0 failed, 2 skipped (`RealModelPackageTest` and
  `EngineThreadsMeasurement`). The measurements above were made at thread counts 1 to 8 and do not depend on the cap
- 2026-10-19T19:07:29Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, logged the listening check on the
  Linux host, with the sandbox IDE started by the human from `./gradlew runIde` on the branch at `HEAD` of the cap of 6.
  A first start, from the implementer's own shell, failed on the first press with "No line matching interface
  SourceDataLine supporting format PCM_SIGNED 24000.0 Hz" from `AudioPlayer.open`, which this unit does not touch. The
  human's default output had been set to the headset used for gaming; it was set back to the Focusrite 2i2 and the next
  start played. That the headset was the cause was not verified, only that the failure did not come back. The log of
  the sandbox (`idea.log`) holds, for the first press after the start:
  `2026-10-19 21:05:39,118 ... Created the speech engine with 6 threads, of 32 processors available` and
  `2026-10-19 21:05:40,401 ... First sound 1898 ms after the button was pressed`. Unit 0006 gave 1288 ms for its first
  press, on 2 threads, which also loaded the model; the two figures are of one press each, and the first sound of
  `ten-thousand-words.md` was not asked for in either. What the human found: no great change in the temperature or the
  fans; they still go up as the processor goes to 70 to 75 degrees Celsius and then holds near 70 with the fans blowing
  hard, and when the reading ends it goes back to 50. The human did not say what they found of the time to the first
  sound. Not checked: a second press with the engine loaded, and 2 threads on the same day for comparison
- 2026-10-19T19:07:58Z [human] decided to skip the listening check on the Mac, in the main session on the Linux host. The
  entry was written by Claude Code, anthropic/claude-sonnet-5-5, on the human's word
- 2026-10-19T19:07:58Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, removed the task of the Mac's
  listening check from `plan.md` on that decision, as the process says for a task that will not be done. Requirement 6 of
  `spec.md` asks for a listening on each of the two machines, so it is met for the Linux host only; the spec is not
  changed by the implementer, and the reviewer and the human should take this as a known gap and not a finding of the
  work. The Mac's measurements of requirement 5 and its table are in the record
- 2026-10-19T19:08:10Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran the suites on `fb8b845`, the tree
  of the code as it stands (the commits since the cap of 6 changed only documents after `./gradlew test` ran on the code
  commit and the README): `./gradlew test` reported 201 tests, 0 failed, 2 skipped (`RealModelPackageTest` and
  `EngineThreadsMeasurement`), of which the last run said BUILD SUCCESSFUL in 3 s, so Gradle may have reused the results
  of the run on the same code, which was 29 s. `KokoroSpeechEngineTest` is among those that ran, the model being in its
  folder. `grep -rn "setNumThreads\|THREAD_COUNT\|KokoroSpeechEngine(" src/main` shows the engine created only in
  `createKokoroEngine` and `setNumThreads` given only `threadCount`. `npm run check-changes` exited 0 on the committed
  branch. Not checked: that `idea.log` holds no second "Created the speech engine" line for a second reading, and the
  Mac's listening. The unit is still `in-progress`, until the review is done and the human has decided its blocking
  findings
- 2026-10-19T19:12:51Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0011 at commit
  `a94b4303c110861e4c43fd64ec3c70d095b70159`, a full review against merge base
  `423c8d7d6d2f690821c3c40cb704b9e878aa8ad7` with `master`. Returned S1, P1 and C1; the complete findings are in
  `.reviews/0011-engine-threads-from-the-machine/20261019T191138Z-opencode-gpt-6.1-sol-full.md`.
  Independently ran `./gradlew cleanTest test buildPlugin`: 201 tests, 0 failures, 0 errors, 2 expected skips;
  all 12 numeric tests and all 4 real-engine tests passed, and the archive task was up to date.
  `npm run check-changes` and the diff whitespace checks passed. Checked the existing Linux measurement output
  against the record and the two recorded sandbox INFO lines against `idea.log`. Not independently verified:
  historical red runs, benchmark execution and timings, subjective listening observations and sound identity,
  the Mac environment, a live change of JVM processor limits, and the second-reading sandbox log check.
- 2026-10-19T19:16:00Z [human] declined finding S1 of the review, the missing statement of the first-sound comparison
  on Linux for requirement 6: the listening on Linux was done well enough, and no more is asked for it. Finding C1, the
  `Co-Authored-By` trailer of commit `60cb40e`, is to be fixed. The entry was written by Claude Code,
  anthropic/claude-sonnet-5-5, on the human's word
- 2026-10-19T19:18:32Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, fixed finding C1 of the review on the
  human's word, given for this one rewrite. Removed the `Co-Authored-By: Claude Opus 5.5` trailer from the message of the
  specifier's commit `60cb40e`, now `8ac60ef`, with `git filter-branch --msg-filter` over `423c8d7..HEAD`, which gives
  the commits after it new hashes. Checked: no commit of that range has a `Co-Authored-By` line, and the tree of the
  rewritten head equals that of the tag `backup-0011-before-trailer-fix`, the head before the rewrite, so no file
  changed. Hashes this record names are of before the rewrite: `60cb40e` is `8ac60ef`, and `fb8b845`, named in an entry
  above, is `558f18d`. The rewritten branch is not pushed yet, so `origin` still has the old commits, which the Mac had
  fetched at `94aca96`. The commits up to `5ea478d`, which include `94aca96` and `2d4d377`, keep their hashes
- 2026-10-19T19:20:54Z [human] had the branch deleted from both remotes, `origin` pushing to GitHub and to Bitbucket,
  instead of pushing the rewritten one, since the human merges to `master` locally and pushes `master`. The entry above
  that says `origin` still has the old commits is true of the time it was written only. The entry was written by Claude
  Code, anthropic/claude-sonnet-5-5, on the human's word
- 2026-10-19T19:20:54Z [human] declined finding P1 of the review, on the plan's ticks overstating the checks made. The
  entry was written by Claude Code, anthropic/claude-sonnet-5-5, on the human's word
- 2026-10-19T19:23:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran the last open check of the plan: in a sandbox
  IDE started by the implementer's shell, the human pressed "Read Aloud" four times in one session, and `idea.log` holds
  one `Created the speech engine with 6 threads, of 32 processors available` line, at 21:21:58, and four `First sound`
  lines (1941, 1244, 1262 and 1256 ms), so the service kept its engine across the readings. The audio played this time
  from the Focusrite output. Ran `./gradlew test` on `0e55294`, before this entry: 201 tests, 0 failed, 2 skipped
  (`RealModelPackageTest` and `EngineThreadsMeasurement`), from results that Gradle reused (625 ms), since no code changed after the 29 s run of the cap of 6 and its README. The review's findings are decided: S1 declined by the human, C1
  fixed and P1 declined by the human's word, and its Mac listening gap is the human's. Every task of `plan.md` is ticked.
  The status is `implemented`

## Amendments

- 2026-10-19T19:25:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, corrected the timestamps of its own
  log entries above. Several had been written as guesses and not read from the clock, and some of them were later than
  the real time or out of order. They now follow the times of the commits that hold them. Reason: the timestamps order
  the record, and the status timestamp is the latest of them
