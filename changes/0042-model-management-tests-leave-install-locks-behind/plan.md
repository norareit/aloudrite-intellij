# The tests of the model management leave install locks behind

## Approach

The plan was written against `bugfix-0042-model-management-tests-leave-install-locks-behind` at `d5ab50b`.

An `InstallAttempt` holds its `heldInstallLock` property until it closes. `SpeechModelManagementTest` stores background
work in two lists, `downloadWorks` and `removalWorks`, and a stored work that never runs never closes its attempt.

A measurement on this tree, with a temporary check after each test, found exactly five leaking tests:

| Test | Work left unrun |
| --- | --- |
| `aDownloadIsRefusedWithoutAQuestionWhileOneRunsInThisIdeAndNothingStarts` | download |
| `aDownloadFromThePageLeavesTheAlignerModelOutOfTheQuestionWhereItIsInPlace` | download |
| `aRemovalWhileADownloadRunsIsRefusedAndTheFolderStays` | download |
| `removingASpeechModelThatLeavesAnotherInPlaceKeepsTheAlignerModel` | removal |
| `theModelsAreToldAsChangedWhenADownloadStartsAndEndsEitherWayAndAfterARemoval` | removal |

The section "Fix" of the spec names the download works only. Requirement 1 covers every attempt, so the plan runs the
stored works of both lists.

One `@After` method does both jobs, in this order: it runs the works still stored, then asserts that no property
remains. JUnit runs `@After` inside the `TemporaryFolder` rule, so the works still find their folders. Running a removal
work a second time is harmless, because `InstallAttempt.close` returns at once on an attempt already closed, so the
tests that already ran their removal works stay as they are.

The assertion looks at every property of the JVM with the prefix, as requirement 1 states it. Before the fix, a leaked
property therefore also fails the tests that run after the leaking one, so the number of failures is above five.

The prefix stays private in `InstallAttempt`, because only the tests change. The test spells it out, as
`InstallAttemptTest` spells out its own prefix.

## Files

- `src/test/kotlin/com/ritenoar/aloudrite/SpeechModelManagementTest.kt`: the `@After` method with its check and the
  running of the stored works
- `changes/0042-model-management-tests-leave-install-locks-behind/record.md`: the outcome of the work

## Tasks

- [ ] In `SpeechModelManagementTest`, add an `@After` method that asserts that
  `System.getProperties()` holds no key starting with `com.ritenoar.aloudrite.heldInstallLock`, with the keys found as
  the failure's value, and import `org.junit.After` (R2)
- [ ] Run `./gradlew :test --tests 'com.ritenoar.aloudrite.SpeechModelManagementTest' --rerun` and confirm that it
  fails, with the five tests of the table among the failures. Record the result in `record.md` (R2)
- [ ] At the start of the same `@After` method, before the assertion, run every work in `downloadWorks` and then every
  work in `removalWorks` (R1)
- [ ] Run the command of the second task again and confirm that every test of the class passes (R1, R2)

## Verification

- [ ] Prove the order of requirement 3 with a temporary class that is never committed. Create
  `src/test/kotlin/com/ritenoar/aloudrite/ProbeSuite0042.kt`, a JUnit `Suite` annotated with
  `@RunWith(Suite::class)` and `@Suite.SuiteClasses(SpeechModelManagementTest::class, InstallAttemptTest::class)`, run
  `./gradlew :test --tests 'com.ritenoar.aloudrite.ProbeSuite0042' --rerun`, confirm that all its tests pass, delete
  the file, and record the result in `record.md`. Without the fix, this suite fails in
  `noPropertyOfThePluginRemainsAfterTheAttemptClosesOrAClaimFails` with the five entries (R3)
- [ ] Run `./gradlew test --rerun` and confirm that it passes (R4)
- [ ] Confirm with `git diff master -- src/` that the only source file changed is `SpeechModelManagementTest.kt`, and
  that no test was removed or weakened (R4)
- [ ] Run `npm run check-changes` after the last commit

## Commit Points

- [ ] `bugfix-0042 Close the install attempts that the model management tests leave open`: the test file, with the check
  and the running of the works together, so that no commit holds a failing test
- [ ] `bugfix-0042 Record the implementation`: `record.md`
