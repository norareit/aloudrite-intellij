# The tests of the download lock start their second process on Linux

## Context

Unit 0005 takes a lock before a download, so that one download runs at a time across IDE windows (its requirement
13). Four of its tests check that lock against a second Java process, which stands in for another IDE. That process is
started by a helper of the tests, the lock probe, which hands `java` the whole classpath of the test run as one
`-cp` argument.

The review of unit 0006 ran the full suite on Linux x64 and found these four tests failing. They fail before any of
their assertions runs, and unit 0006 did not cause it: every run of the suite that unit 0005 records was made on
macOS, and its record lists Linux among what was not run. The four tests have never passed on Linux.

Only the tests are affected. The plugin starts no process, and the lock itself works on Linux: the human's checks of
requirement 17 of unit 0005 passed there.

## Observed behavior

On Linux x64, `./gradlew test` reports these four tests as failed, each with `java.io.IOException: Cannot run
program ".../jbr/bin/java": Exec failed, error: 7 (Argument list too long)` at the line that starts the probe:

* `InstallAttemptTest.anAttemptWhileAnotherProcessHoldsTheLockStartsNothingAndTheLockIsFreeOnceThatProcessIsGone`
* `InstallAttemptTest.theLockOfAnAttemptKeepsAnotherProcessOut`
* `InstallAttemptTest.aRejectedClaimThroughAnotherClassLoaderLeavesTheLockOfTheOwnerInForce`
* `ModelDownloadFlowTest.aPressWhileAnotherProcessHoldsTheLockAsksNothingStartsNothingAndSaysSo`

## Expected behavior

The probe starts on Linux as it does on macOS, and the four tests run to their assertions and pass.

## Steps to reproduce

On Linux x64, on `master` at `1f8ed00`:

```sh
./gradlew test --rerun --tests 'com.ritenoar.aloudrite.InstallAttemptTest' --tests 'com.ritenoar.aloudrite.ModelDownloadFlowTest'
```

This reports 27 tests completed and the four above failed, with the message above in each of their results.

## Root cause

Reproduced on this Linux x64 host (kernel 7.0.0), with the JetBrains Runtime 25.0.4 that the tests run on.

The classpath of the test task has 1,696 entries and is 268,911 bytes long when joined, as an init script that
prints the classpath of the task `test` measured. The probe passes it to `java` as one argument. Linux refuses any
single argument or environment string of 131,072 bytes or more: `/bin/true` started with one argument of 131,071
bytes runs, and with one of 131,072 bytes the shell reports `argument list too long`. The limit on all arguments
together, `getconf ARG_MAX`, is 2,097,152 bytes and is not what is reached. So `exec` of the probe fails with error 7
and the process never starts.

macOS has no limit on a single argument, which is why the suite of unit 0005 passed there. That rests on the record of
unit 0005 and was not measured on a Mac for this unit.

## Fix

The probe hands `java` its classpath in an argument file, `java @<file>`, rather than on the command line. An argument
file is read by the launcher itself and does not pass through `exec`, so the limit does not apply to it.

The classpath is written into the file in double quotes. The launcher splits an argument file at whitespace, and four
entries of the classpath hold a space, the directory `plugins/JPA Model` of the IDE among them. Tried on this host with
the classpath above: the probe in mode `try` started from an argument file with the classpath unquoted fails with
`Could not find or load main class` and a fragment of a path after `JPA`, and with the classpath in double quotes it
prints `free` and exits 0. The same classpath given as `-cp` on the command line is refused with `argument list too
long`.

## Requirements

1. On Linux x64, `./gradlew test` runs the four tests named under "Observed behavior" to their assertions, and they
   pass
2. On macOS on Apple Silicon, the four tests pass as before
3. The four tests are the regression test: they fail on `master` at `1f8ed00` on Linux x64 with `error: 7 (Argument
   list too long)` and pass with the fix
4. The four tests assert what they asserted before, and the probe locks and tests the lock file as before. Only the way
   the probe process is started changes
5. The plugin's own code does not change

## Decisions

- **An argument file, quoted.** The `@file` syntax has been in the `java` launcher since JDK 9 and has no limit on
  length. The alternatives were rejected: the environment variable `CLASSPATH` meets the same limit, since Linux
  applies it to each environment string as well (an environment string of 131,072 bytes was refused on this host); a
  jar with nothing but a manifest whose `Class-Path` lists the entries works, but each entry would have to be written
  as a URL, which is more code for the same result; handing the probe only the entries it needs couples the helper to
  what the probe happens to load, and breaks when that changes; and checking the lock within the same process tests
  something else, since the lock is held per process
- **The argument file is temporary.** It is written where temporary files go and is removed when the probe is
  closed, so a test run leaves nothing behind
- **A bug of its own, not part of unit 0006.** The cause is in a helper of unit 0005, and unit 0006 changes nothing in
  it. Unit 0005 is implemented and frozen, and the fix changes no requirement of it, so this unit corrects its tests
  without superseding it
- **No plan.** The fix is one change to one helper of the tests, in one commit

## Non-goals

- Any change to the lock, to the download, or to anything else the plugin does
- Any test or check beyond the four tests, such as a check of the length of the classpath
- Windows, which the plugin does not support. Its own limit on the length of a command line, and the way the
  launcher reads a backslash inside quotes, are not addressed
- The other failures or findings of the review of unit 0006

## Beyond the request

- **Escaping a double quote and a backslash in the classpath.** Inside quotes, the launcher reads a backslash as an
  escape. No entry of the classpath on this host holds either character. It would cost a line that replaces each of
  them with its escaped form. Without it, an entry holding one would make the probe fail to start with a clear
  `Could not find or load main class`, in a test run only, and it would be fixed then
