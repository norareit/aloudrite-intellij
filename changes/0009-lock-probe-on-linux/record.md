---
kind: bug
status: in-progress
timestamp: 2026-10-19T15:24:58Z
---

# The tests of the download lock start their second process on Linux

## Log

- 2026-10-18T23:10:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `1f8ed00`. The human passed on a finding of the review of unit 0006: the four tests of the
  lock of unit 0005 fail on Linux because the lock probe hands `java` a classpath longer than Linux takes in one
  argument, and the fix is an argument file. Asked for a bug unit of its own. Read `process.md` and
  `roles/specifier.md` of the installed kit, the lock probe, the four tests and the record of unit 0005. Made the unit
  as a bug and numbered it 0009. Wrote `spec.md`, and no plan, for the reason its decisions give. Seen on this host,
  with the JetBrains Runtime 25.0.4 the tests run on:
  * The command under "Steps to reproduce" reports 27 tests completed and the four failed, each with `error: 7
    (Argument list too long)`
  * An init script in a scratch directory printed the classpath of the task `test`: 1,696 entries, 268,911 bytes.
    The report the human passed on gave 268,909 characters, measured from inside the test process
  * `/bin/true` takes one argument of 131,071 bytes and refuses one of 131,072, and `env` refuses one environment
    string of 131,072 bytes. `getconf ARG_MAX` gives 2,097,152
  * The probe in mode `try`, started by hand on a scratch lock file: refused with `-cp` on the command line; failing
    with `Could not find or load main class` from an argument file with the classpath unquoted, because four entries
    hold a space; printing `free` and exiting 0 from an argument file with the classpath in double quotes

  Decided by the specifier and open to the human at approval: the quotes, the argument file as a temporary file
  removed when the probe is closed, the unit as a bug without `supersedes`, and leaving the escape of quotes and
  backslashes beyond the request. Not established here: anything on macOS, which rests on the record of unit 0005.
  Not tried: the fix in the helper itself, or the full suite. The status is `draft`
- 2026-10-19T14:21:51Z [human] approved the spec as it stands, in the main session on the Linux host, with the
  choices the specifier left open: the quotes, the argument file as a temporary file removed when the probe is
  closed, the unit as a bug without `supersedes`, and the escape of quotes and backslashes left beyond the request.
  The status is `approved`. The status change and this entry were written by Claude Code, anthropic/claude-opus-5-5,
  on the human's word
- 2026-10-19T14:50:21Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux
  host, on the branch `bugfix-0009-lock-probe-on-linux`, cut from `master` at `ef2bec8`. Read `process.md` and
  `roles/planner.md` of the installed kit, the spec, this record and the lock probe. The spec still fits the tree:
  the command under "Steps to reproduce", run at `ef2bec8`, reports 32 tests completed, more than at `1f8ed00`
  because later units added tests to `ModelDownloadFlowTest`, and the same four failed with `error: 7 (Argument
  list too long)`. Wrote no `plan.md`: the fix is one change to the helper `LockProbe` in
  `src/test/kotlin/com/ritenoar/aloudrite/LockProbeProcess.kt`, writing the quoted classpath to a temporary
  argument file in `start` and removing it in `close`, which the tests also reach after `kill`; the four existing
  tests are the regression test, and no documentation changes. That is a single commit, below the threshold of
  `process.md`. The status is `planned`
- 2026-10-19T14:52:33Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, on
  the branch `bugfix-0009-lock-probe-on-linux` at `679fd28`. Read `roles/implementer.md` of the installed kit, the
  spec and this record. The regression test is the four tests named in the spec, seen failing at `ef2bec8` with
  `error: 7 (Argument list too long)` in the planner's run above, and the tree has not changed outside this unit's
  documents since. The status is `in-progress`
- 2026-10-19T14:53:57Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host.
  Committed the fix as `5e64f25`: `LockProbe.start` writes `-cp` and the classpath in double quotes to a temporary
  file made with `Files.createTempFile` and starts `java @<file>`, and `close` removes that file after ending the
  process. The process itself, its modes and the four tests are unchanged, and nothing under `src/main` changed.
  Seen on this host, Linux x64, at `5e64f25`:
  * The command under "Steps to reproduce" passes all 32 tests of `InstallAttemptTest` and `ModelDownloadFlowTest`,
    the four named in the spec among them, each run to its end and none skipped (requirements 1 and 3)
  * `./gradlew test --rerun`, the full suite, reports 188 tests, no failure and no error, and one skipped:
    `RealModelPackageTest.theRealPackageInstallsToTheSameModelAsTheOneInPlaceByHand`, whose assumption needs
    `ALOUDRITE_MODEL_PACKAGE`, which is not set here
  * After both runs, no `lock-probe*.args` file is left under `build/` or `/tmp`

  The regression run that saw the four fail was at `ef2bec8`, not at `1f8ed00` as requirement 3 names; the specifier
  saw them fail at `1f8ed00`. Not checked: macOS on Apple Silicon (requirement 2), since no Mac is at hand here; the
  argument file when `start` fails before the process exists, which leaves the file in place, in a test run only.
  The status stays `in-progress` until the review
- 2026-10-19T15:10:14Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a full review of
  `0009-lock-probe-on-linux` at commit `2dc386cea8dfc9ea6b2cea85b6878e33315cdad7`, against the merge base with
  `master`, `ef2bec8092652367d9c0e5476875eae26ac17ff8`. Returned C1, nonblocking, on code quality; no findings on
  spec conformance or plan fidelity. Independently ran the targeted regression command (32 tests, none failed or
  skipped), `./gradlew test --rerun` (188 tests, no failures or errors, one real-package test skipped),
  `npm run check-changes` and the diff checks, all successfully on Linux x64. No probe argument files remained
  after either test run. Could not verify macOS Apple Silicon, the exact historical failing runs at `1f8ed00` and
  the merge base, or cleanup after exceptional startup. Complete findings and verification evidence are saved in
  `.reviews/0009-lock-probe-on-linux/20261019T150902Z-opencode-gpt-6.1-sol-full.md`
- 2026-10-19T15:15:48Z [human] accepted C1 of the review, in the main session on the Linux host: the argument file is removed
  when the probe fails to start, as well as when it is closed. This entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-19T15:17:07Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host.
  Applied C1 as `ac389d1`: `LockProbe.start` removes the argument file and rethrows the original exception when
  writing the file or starting the process throws. Seen on this host, Linux x64:
  * With the path of `java` changed by hand to one that does not exist, and changed back afterwards,
    `InstallAttemptTest.theLockOfAnAttemptKeepsAnotherProcessOut` fails with `Cannot run program` and leaves no
    `lock-probe*.args` file under `build/` or `/tmp`. The same change made to the helper as at `5e64f25` leaves one
    in `/tmp`, which was then removed
  * At `ac389d1`, the command under "Steps to reproduce" passes all 32 tests of the two classes, and `./gradlew test
    --rerun` reports 188 tests, no failure and no error, and the one skip of `RealModelPackageTest` as before. No
    `lock-probe*.args` file is left after either run

  Not checked: a failure of `Files.writeString`, which the same handler covers but which was not caused here; macOS
  on Apple Silicon (requirement 2), as before. The change after `2dc386c` has not been reviewed. The status stays
  `in-progress`
- 2026-10-19T15:22:41Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of
  `0009-lock-probe-on-linux` at commit `6f4cada39bfa532764c9fe3079ac45614c3dd855`, from the previous review's
  commit `2dc386cea8dfc9ea6b2cea85b6878e33315cdad7`. Verified the accepted C1 correction and returned no new
  findings on spec conformance, plan fidelity or code quality. Independently exercised a missing-executable launch
  through the compiled helper: the expected IOException propagated and no argument file remained. The targeted
  regression command passed all 32 tests; `./gradlew test --rerun` reported 188 tests, no failures or errors and
  one real-package test skipped. `npm run check-changes` and the diff checks passed on Linux x64. Could not verify
  macOS Apple Silicon or injected write and cleanup failures; the historical red runs were outside this delta.
  Complete findings and verification evidence are saved in
  `.reviews/0009-lock-probe-on-linux/20261019T152144Z-opencode-gpt-6.1-sol-delta.md`
- 2026-10-19T15:23:34Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, at
  `6f4cada` with the delta review recorded. The full review returned C1, nonblocking, which the human accepted and
  which is applied in `ac389d1`; the delta review returned no findings. No finding is open, and the unit has no
  `plan.md`. The suites last run, at `ac389d1`, are recorded above, and only this record changed after it. Still not
  verified: macOS on Apple Silicon (requirement 2). The status is `implemented`
- 2026-10-19T15:24:58Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host, at
  `23ac07f`. Set the unit back, on the human's word: requirement 2, the four tests on macOS on Apple Silicon, has not
  been run, and the status `implemented` above was set before it was. The branch is not merged, so `master` holds
  the unit as `approved` and nothing frozen there changes. The human runs the command under "Steps to reproduce" on
  a Mac on this branch. The status is `in-progress`

## Amendments
