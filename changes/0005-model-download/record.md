---
kind: feature
status: in-progress
timestamp: 2026-10-16T13:18:28Z
---

# Model download on first use

## Log

- 2026-10-08T16:49:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`, as the second of the three units the Kokoro work was split into with the human. It was
  written before unit 0004 was built, at the human's instruction, and unit 0004 requires that it is revised when
  that unit is done. At the human's instruction it requires in turn that unit 0006 is revised when this one is
  done. Decided with the human: the model is downloaded on first use and not shipped in the plugin. My own
  choices, which the human has not stated: asking the user before the download, fetching from the place
  sherpa-onnx publishes the package, the fixed checksum, and speech starting by itself after the download. Status
  left at `draft` for the human to approve.
- 2026-10-08T19:32:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, reworded one sentence of the context in `spec.md` after a change the human asked for in unit 0004:
  speech is asked for with the "Read Aloud" button and not through an action in the Tools menu. No requirement
  changed. Status still `draft`.
- 2026-10-08T22:51:21Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, revised `spec.md` against what unit 0004 established, as requirement 19 of that unit asks, reading its
  record, plan and README on its branch at `f21f947`. Changed because of what was found: the package is a packed
  archive, so the plugin unpacks it (requirement 8); a model folder the engine does not expect closed the whole
  process in one case, so an incomplete model must never count as in place, also after a kill (requirement 9);
  the checksum is checked before unpacking (7); the question replaces the missing-model message of unit 0004 and
  is not asked on a platform without speech (1, 4); the question also states the 190 MB on disk and the folder
  (2); the failure cases gain a package that cannot be unpacked, and the checksum case says what it means, since
  the publisher has replaced the package before (11); redirects are allowed over HTTPS, as the address is a
  GitHub release asset (15); and requirement 10 of unit 0004, no network request, is named as replaced (15).
  Added as my own choice: the human's checks on both platforms (17). The requirements are renumbered, from 14 to
  19. New under "Beyond the request": a copy of the package hosted by the project, a stricter check of a
  hand-placed model, and a check for free disk space. The human has not read the revision yet. Status still
  `draft`.
- 2026-10-16T12:11:56Z [human] approved the spec as it stands, in the main session on the macOS host, in these
  words: "I approve the spec for 0005". It was approved without the three optional edits the main session
  offered after checking the spec against the tree and the published package that day: the date of the revision
  in the context, the README sentence on the missing-model message in requirement 18, and a statement of what the
  checksum defends against. The status is `approved`. The status change and this entry were written by Claude
  Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-16T12:28:33Z [Claude Code, anthropic/claude-opus-5-5] as the planner, dispatched as a subagent from the main
  session on the macOS host, cut the branch `feature-0005-model-download` from the head of `master` at `545af1f` and
  wrote `plan.md` against it. `./gradlew test` passed on that tree with 46 tests in 57 seconds, on JDK 25.0.1.
  Established in a throwaway trial, in a copy of the tree outside the repository that was deleted afterwards, with
  one test under `./gradlew test`: `HttpRequests` works against a stand-in server on `127.0.0.1` in the test
  environment, follows a redirect to any scheme when left to itself, and hands a `302` back with
  `followRedirects(false)`; a cut-off body throws `Premature EOF`; `Decompressor.Tar` of the platform unpacks a
  `.tar.bz2` and sets the permissions of its entries; commons-compress 1.28.0 of the platform is on the test class
  path; cancellation reaches the work through `ProgressManager`; the question and a notification can both be caught
  in a test. In a plain JDK on APFS: a directory can be flushed, an atomic move replaces an empty directory and
  fails on one that is not empty, and a second lock in one JVM throws. Read in the bytecode of the platform: a
  queued background task always runs, and one without a project shows its progress in the frame focused last.
  Decided in the plan, as the spec leaves it to the plan: the texts of the question and of the notifications; the
  attempt directory next to the model folder, locked while it runs, and put in place by one atomic rename of a
  flushed, complete directory, which is how requirement 9 holds after a kill; that leftovers are removed only where
  their lock is free, because the sandbox IDE and an installed IDE share the model folder; the redirects followed by
  the plugin and only to HTTPS; the task without a project; the reuse of `OneAtATime`; and an opt-in test that
  installs a local copy of the real package and compares it with the hand-placed model. Added as a kind of failure
  the spec does not name, which requirement 11 allows: a model folder that exists without a complete model is the
  user's, is never deleted, and makes the attempt fail before any request with a notification that names it. The
  human may want to decide that case otherwise. Not established: anything in an IDE window, the real download, and
  that the plugin's unpacking of the real package matches `tar`, which the plan leaves to the implementer's opt-in
  test and to the human's checks. When the branch was cut, the working tree held an uncommitted edit of the human's
  to `README.md`; it was undone outside this session at 12:13:35Z, by the file's time, and nothing of it is in this
  commit. The plan still tells the implementer to check for it. The developer's Gradle daemon was
  used and left running. The real model folder on this Mac was only read. The status is `planned`
- 2026-10-16T12:32:32Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, began the work on the branch
  `feature-0005-model-download`. The status is `in-progress`
- 2026-10-16T12:45:02Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built the plan on the macOS host with seven
  commits after the start: tasks 1 to 14 of `plan.md` are done and ticked. Verified, on the tree of the commit that
  holds this entry minus this entry: `./gradlew clean buildPlugin test` succeeded, 101 tests ran, 100 passed and 1
  was skipped (`RealModelPackageTest`, without its environment variable), none failed; the archive is 20,128,248
  bytes; `grep -rn HttpRequests src/main` finds only `PackageDownload.kt`; the grep for `java.net.URL(`,
  `java.net.http`, `HttpClient`, `openConnection` and `org.apache.commons.compress` in `src/main` finds nothing;
  `git ls-files` and the archive hold no `.onnx`, `.bin` or `.tar.bz2`; `~/Library/Application Support/AloudRite`
  holds the one directory `kokoro-int8-multi-lang-v1_0` with the same 377 files and no attempt directory after all
  runs. The real package was downloaded once with `curl`, its SHA-256 matched, and
  `ALOUDRITE_MODEL_PACKAGE=<it> ./gradlew cleanTest test --tests ...RealModelPackageTest` passed and was not
  skipped: the install took 9,332 ms, the 416 entries and the SHA-256 of every file equal those of the model on this
  Mac, and no permission differs (R8, on macOS only). The downloaded copy was deleted. Not verified: anything in an
  IDE window (the question, the progress in the status bar, the notification, the speech after the download), the
  download from the real address by the plugin itself, Linux, and a power loss or a killed IDE (the killed attempt
  is covered only by a test with a leftover directory whose lock is free). Deviations from the plan, all added to
  `plan.md`: `downloadPackage` has a last parameter `resolveRedirect` with `redirectTarget` as default, because
  the stand-in server speaks HTTP and the limit of ten redirects could not otherwise be tested; progress and
  cancellation use the handed-in `ProgressIndicator` (`indicator.checkCanceled()`) and not
  `ProgressManager.checkCanceled()`; `InstallAttempt` keeps a set of its own running attempts because closing a
  second channel on a locked file can release the locks of the same process; the helpers that press the button
  moved into the new `ButtonPressSupport.kt`. I changed `ReadAloudAction` before writing `ModelDownloadFlowTest`
  and not after, so the flow test never failed to compile as the plan asks. A trial result of the plan held: the
  stand-in server, `Decompressor.Tar`, `HttpRequests` and the dialog and notification hooks all worked as the
  trial said. `isOutOfDiskSpace`, `OutOfDiskSpace` and `Unexpected` from a real full disk were not exercised
  beyond the pure function and the mapping. `README.md` had no uncommitted edit of the human when I looked.
  The status stays `in-progress`
- 2026-10-16T12:45:02Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, wrote what task 16 of the plan asks, for the
  specifier to revise the spec of unit 0006 against. Unit 0006 is `draft`, so it is not implemented and needs the
  revision, on `master`, and the human has to see it. What this unit established that unit 0006 rests on: a press
  without the model asks the question `downloadQuestion` and then downloads in the background; the speech of the
  press starts by itself after the download, through the `speak` function of the action, with the test sentence,
  `TEST_SENTENCE` in `ReadAloudAction.kt`, which is still a constant there; a press while a download runs is
  ignored without a word; the action takes a `() -> ModelDownloads` after `speak`; a failure is a notification of
  the group `AloudRite` with `Try Again`; the speech after a download comes from the thread of the download task,
  not from the press. Not done yet and open: the revision of unit 0006 and its sight by the human (R19), and the
  checks of the human (R17), for which the archive is `./gradlew buildPlugin`, steps as in task 15 of the plan, to
  be run on Linux x64 and macOS on Apple Silicon: move the model folder aside, press and decline and see nothing
  was downloaded, press and agree and cancel halfway and see neither the model folder nor a directory
  `.kokoro-int8-multi-lang-v1_0.download-*` left in its parent, press and agree, wait and hear the sentence start
  by itself, then `diff -r <moved-aside folder> <model folder>`, then delete or put back the folder moved aside
- 2026-10-16T13:04:50Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a full review of
  `0005-model-download` at commit `22a6905df2c962cf825a756bf62cdf04392da5bc`, against the merge base with
  `master`, `545af1f1708d788c1f9947f355144bc51d254288`. Returned findings S1 and P1, saved in
  `.reviews/0005-model-download/20261016T130143Z-opencode-gpt-6.1-sol.md`. Ran `./gradlew clean buildPlugin test`
  (100 passed, one skipped), `npm run check-changes`, the diff whitespace check and temporary concurrency probes.
  Could not verify the human's IDE checks on either platform (R17), the opt-in real-package comparison, the
  plugin's live download, Linux execution, a killed IDE, power loss or a full disk. The R19 revision and human
  sight of unit 0006 remain unrecorded
- 2026-10-16T13:30:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, answered the review of
  `22a6905` (findings S1 and P1). S1, accepted and fixed in `c0d0ae2`: the review is right that `start` made the
  directory visible before it was locked, so a removal in another IDE could delete it, and that the in-process set
  was filled too late. A deleted data file followed by a rename would have defeated R9, and the rename is not
  guarded by a validation. `start` now makes and locks the directory under the name `.kokoro-int8-multi-lang-v1_0.staging-<random>`,
  which `removeLeftovers` ignores, adds the final path to the in-process set, and only then renames the directory
  atomically to `.kokoro-int8-multi-lang-v1_0.download-<random>`; a directory with that name always has its lock
  held. `removeLeftovers` also treats `OverlappingFileLockException` and a directory that vanished meanwhile as
  "leave it", which the plan already said and the probes of the review showed it did not do. A removal can now
  delete only a directory whose lock was free, which no live attempt has, so the cleaner that is suspended before
  deleting a data file cannot meet a live attempt. Three tests in `InstallAttemptTest` cover the window
  deterministically through a `beforeVisible` function of `start`: a removal run there leaves the attempt and does
  not fail it, no leftover-named directory exists before the rename, and a removal after files were written under
  `espeak-ng-data` leaves them. They were written before the change was kept: against the old `InstallAttempt`
  the test source did not compile, since `start` had no such parameter, which is the failure seen; I did not
  run them against a variant of the old behavior that compiles. Residual, not fixed: a staging directory left by an
  IDE killed between creating it and the rename (a window of two file operations) is never removed, and nothing was
  tried with two real IDE processes. P1, accepted: the ticked flow-test task in `plan.md` now says that the action
  was changed first and that the suite was never seen failing to compile, as the entry of 12:45:02Z already said;
  that step cannot be redone. Verified on the tree of `c0d0ae2`, before this entry: `./gradlew clean buildPlugin test`
  succeeded, 104 tests ran, 103 passed and 1 was skipped (`RealModelPackageTest`), none failed; the archive is
  20,129,019 bytes. Not rerun: the opt-in real-package test, and nothing in an IDE window. Both findings are
  fixed as asked, so there is no proposed decline. R17 and R19 are still open. The status stays `in-progress`
- 2026-10-16T13:13:19Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of
  `0005-model-download` at commit `7a69e62ad7dea7bb31e5fa0a7580737739b30ecd`, from the previous reviewed commit
  `22a6905df2c962cf825a756bf62cdf04392da5bc`. Checked previous findings S1 and P1 and returned new finding S2;
  the complete review is `.reviews/0005-model-download/20261016T131151Z-opencode-gpt-6.1-sol-delta.md`.
  Ran `./gradlew clean buildPlugin test` (103 passed, one skipped), `npm run check-changes`, the delta whitespace
  check, an isolated abrupt-process-exit probe and a 2,000-attempt concurrency probe. Could not verify the human's
  IDE checks on either platform (R17), the opt-in real-package comparison, live IDE speech and download, Linux,
  power loss or a full disk. The process-exit probe covered initialization in a dedicated Java process, not a
  killed IDE during downloading or unpacking. R19 remains open in the record
- 2026-10-16T13:18:28Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, answered the delta review of
  `7a69e62` (finding S2). S2, accepted and fixed: the review is right that R10 covers what a killed attempt leaves
  and that my entry of 13:30:00Z called the staging directory left by a start killed before the rename "residual,
  not fixed" without a decision of the human accepting that exception to R10; the spec still requires the cleanup.
  Adding the staging prefix to the lock-based scan would bring back the race of S1, since a staging directory may
  not hold its lock file yet. `removeLeftovers` now also looks at `.kokoro-int8-multi-lang-v1_0.staging-*`
  directories and removes one only when its modification time is more than five minutes old, and then only if its
  lock, when it has a lock file, is free; `start` registers its staging path in the in-process set until the rename
  or its failure, so a start in this JVM is never removed. The five minutes are my choice: a start is a few file
  operations, so the margin is large and a killed start is still removed at the next attempt after that time, not
  immediately. Three tests were written first and the old-staging test was seen failing against the old code
  (`InstallAttemptTest` line 122, an assertion, 11 tests, 1 failed); the other two pass against the old code too,
  since they pin that nothing young or live is removed, which the old code also satisfied. `plan.md` is extended
  in the task of `InstallAttemptTest` and in the paragraph on leftovers, and its note of a residual limitation is
  replaced. Verified on the tree before this entry: `./gradlew clean buildPlugin test` succeeded and `git diff
  --check` was clean; 107 tests ran, 106 passed and 1 was skipped (`RealModelPackageTest`), none failed, and the archive is 20,129,790 bytes. Not checked: a real killed IDE, two IDE
  processes, and the isolated process-exit probe of the review, which I did not rerun. No proposed decline. R17 and
  R19 are still open. The status stays `in-progress`
- 2026-10-16T13:25:55Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of
  `0005-model-download` at commit `2abe9cbbb692b9cc43f114e14d60c8e5fdd17b58`, from the previous reviewed commit
  `7a69e62ad7dea7bb31e5fa0a7580737739b30ecd`. Checked the correction for S2 and returned S2 and new finding C1;
  the complete review is `.reviews/0005-model-download/20261016T132408Z-opencode-gpt-6.1-sol-delta.md`.
  Ran `./gradlew clean buildPlugin test` (106 passed, one skipped), `npm run check-changes`, the delta whitespace
  check, an isolated abrupt-process-exit probe, cleanup after simulated aging, and a staged two-JVM pre-lock pause
  probe. Could not verify the human's IDE checks on either platform (R17), the opt-in real-package comparison,
  live IDE speech and download, Linux, power loss or a full disk. The probes used dedicated Java processes and
  simulated elapsed aging, not a killed IDE or an actual machine sleep. R19 remains open in the record

## Amendments

- 2026-10-16T13:56:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the specifier, amended `spec.md` while the unit is
  `in-progress` and not frozen, with the human's approval, given in this session by "yes" to the proposal. Two reviews
  (S1, S2) showed that the lock per attempt directory keeps producing races. Requirement 10: after a failure or a
  cancellation nothing is left except one empty lock file in the folder that holds the model folder, kept also after a
  success because deleting it while held reopens a POSIX race; a killed attempt's leftovers are removed by the next
  attempt. Requirement 13 extended, without a new number: one lock on that file is taken before anything is touched
  and before the question, leftovers are removed only while holding it, and when another IDE holds it the plugin
  starts no download, asks no question and says in one sentence that a download is already running in another IDE
  window. Requirement 16 gains the case of a press while another IDE holds the lock. A decision with its rationale was
  added. The plan and the code still describe the old scheme and are for the implementer to bring in line
