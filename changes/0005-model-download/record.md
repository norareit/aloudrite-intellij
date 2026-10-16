---
kind: feature
status: in-progress
timestamp: 2026-10-16T12:32:32Z
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

## Amendments
