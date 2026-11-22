# The tests of the model management leave install locks behind

## Context

`InstallAttempt` marks a lock on the folder of a model that this IDE holds by a system property of the JVM, named
`com.ritenoar.aloudrite.heldInstallLock:<path of the lock file>`, and removes it when the attempt closes.
`InstallAttemptTest.noPropertyOfThePluginRemainsAfterTheAttemptClosesOrAClaimFails` checks that no property whose name
starts with `com.ritenoar.aloudrite` remains after an attempt closes. It looks at every such property of the JVM, not
only at the one of its own attempt.

During unit 0041 that test failed once in a full run of `./gradlew test` and passed in the runs after it. On
2026-11-22, a separate agent session ran it again on the branch of unit 0041 at `a0aacc9`. Its scratch files are in
the session's scratchpad and are not committed.

## Observed behavior

- The class alone, `./gradlew :test --tests 'com.ritenoar.aloudrite.InstallAttemptTest' --rerun`, passed in 50 of 50
  runs
- In 34 full runs of the root module, `./gradlew :test --rerun`, it failed once, at the first assertion, line 192:
  `expected:<[]> but was:<[com.ritenoar.aloudrite.heldInstallLock:/tmp/junit…/junit…/aloudrite/.kokoro-int8-multi-lang-v1_0.install.lock, …]>`,
  five entries, none of them the test's own folder, which has no `aloudrite` level

## Expected behavior

`InstallAttemptTest` passes whichever test classes ran before it in the same JVM, because no test class leaves a
property of the plugin behind.

## Steps to reproduce

Not reproduced on demand. What was observed: the failing run came right after a run in which
`SpeechModelManagementTest` had failed, for a reason outside the project (the Gradle cache lost the unpacked IDE while
the tests ran). Gradle runs the classes that failed in the previous execution first, so in the failing run
`SpeechModelManagementTest` ran before `InstallAttemptTest`. In the 15 recorded full runs with the usual order,
`InstallAttemptTest` ran fourth and `SpeechModelManagementTest` about 32nd, and all of them passed.

## Root cause

Evidence: a test class added for the measurement only, run after one class at a time, found exactly five
`heldInstallLock` properties of the shape `/tmp/junit*/junit*/aloudrite/…lock` after `SpeechModelManagementTest`, which
is the number and the shape of the failure, and none after `ModelDownloadFlowTest`, `ModelDownloadServiceTest` and
`ModelInstallerTest`. `SpeechModelManagementTest` builds `ModelDownloadService` with
`runInBackground = { work -> downloadWorks += work }`, and some of its tests start a download and never run the stored
work, such as `aDownloadIsRefusedWithoutAQuestionWhileOneRunsInThisIdeAndNothingStarts`, which ends after asserting
that one work is stored. The attempt of such a download is never closed, and its property stays in the JVM.

Not confirmed: which five tests leak, and that the order of the classes is what made the failing run fail, which was
inferred from the order Gradle uses and not forced.

## Fix

Every test of `SpeechModelManagementTest` ends with no attempt open: the download works it stored and did not run are
run or ended after each test, so that each attempt closes and its property is removed.

## Requirements

1. After any test of `SpeechModelManagementTest`, the JVM holds no system property whose name starts with
   `com.ritenoar.aloudrite.heldInstallLock`.
2. A regression test fails before the fix and passes after it: a check after each test of `SpeechModelManagementTest`
   that no such property remains.
3. `InstallAttemptTest` passes in a run of the root module's tests in which `SpeechModelManagementTest` runs before it.
4. The tests of the root module pass, and no test of them is removed or weakened to make them pass.

## Decisions

- **The leak is mended where it is made, and `InstallAttemptTest` keeps looking at every property of the plugin.**
  Narrowing that test to its own folder would hide a leak of any other test, and of the plugin itself, which is what it
  is there to find.
- **Only the tests are changed.** In the IDE, `runInBackground` always runs the work it is given, so an attempt that
  is never closed comes from the way the test stores the work. That the plugin itself never leaves an attempt open was
  not checked beyond this.

## Non-goals

- The loss of the unpacked IDE from the Gradle cache during a test run, which made `SpeechModelManagementTest` fail in
  the first place
- The timing of `RepositoryDownloadTest`, which unit 0041 deals with

## Beyond the request

1. A check after every test of the root module, through a shared rule, that no property of the plugin remains. It
   costs a rule added to each test class, or a listener of JUnit set up in the build. Without it, a new leak in another
   class is found as this one was, by a failure of `InstallAttemptTest` in an unlucky order.
