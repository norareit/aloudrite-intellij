---
kind: bug
status: planned
timestamp: 2026-10-19T14:50:21Z
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

## Amendments
