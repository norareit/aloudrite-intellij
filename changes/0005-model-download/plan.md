# Model download on first use

## Approach

A press of "Read Aloud" keeps the order of unit 0004: the platform first, then the model. Where the model is not in
place, the action no longer shows the missing-model message. It asks the user whether to download the model, and on
"Download" it hands the download to a new application service, which runs it as a cancellable background task of the
IDE with a progress indication in the status bar. When the model is in place, the service calls back into the action,
which hands the test sentence to the `SpeechService` of unit 0004 as a press would have.

The download is one installation attempt, done by a new class `ModelInstaller` in this order:

1. If the model is in place already, stop: there is nothing to do and no request is made.
2. If the model folder exists, is not empty and does not hold a model, fail before any request: something the
   plugin did not make is in the way, and the plugin never deletes it.
3. Create the parent of the model folder where it is missing, remembering which directories it created.
4. Remove what earlier attempts left behind in that parent, except an attempt that another IDE is still running.
5. Create a new attempt directory in that parent and lock it for as long as the attempt runs.
6. Download the package into the attempt directory over HTTPS, computing its SHA-256 while it arrives.
7. Compare the SHA-256 with the one fixed in the plugin. Nothing is unpacked before this holds.
8. Unpack the package inside the attempt directory and check that it gave one directory holding a model in place.
9. Flush every file and directory of the unpacked model to the disk.
10. Rename the unpacked directory to the model folder in one atomic step, and flush the parent.
11. Delete the attempt directory. On a failure or a cancellation at any step before 10, delete it too, and the
    directories of step 3 where they are empty.

The model folder therefore comes into being by one rename of a complete, flushed directory, and at no other moment.
Before the rename it does not exist, and what an attempt writes lies in a directory that `isModelInPlace` never
looks at. That is what requirement 9 asks, including when the IDE is killed or the machine loses power: a killed
attempt leaves only its attempt directory, which the next attempt removes (requirement 10).

This plan is written against the branch `feature-0005-model-download`, cut from `master` at `545af1f`. The tree holds
unit 0004 as implemented: `ReadAloudAction` checks the platform and then `isModelInPlace` and shows
`modelMissingMessage` before it reaches `SpeechService`, `KokoroModel.kt` holds the package name and the five
entries, and `ReadAloudActionTest` has nine tests. `./gradlew test` passed on that tree on 2026-10-16 on the macOS
host with 46 tests, `KokoroSpeechEngineTest` included, in 57 seconds.

### What a trial established

Made on 2026-10-16 on the macOS host (Apple Silicon, JDK 25.0.1 Corretto), in a copy of the tree at `545af1f` outside
the repository with one throwaway test, under `./gradlew test` with IntelliJ IDEA 2026.2.3. The copy was deleted
afterwards. Use these as established, and log it in `record.md` where one of them turns out not to hold:

* `com.intellij.util.io.HttpRequests` works in the test environment of `ApplicationRule` against a
  `com.sun.net.httpserver.HttpServer` on `127.0.0.1`. With `followRedirects(false)` a `302` comes back to the caller
  with its `Location` header and is not followed. Left to itself, `HttpRequests` follows a `Location` of any scheme,
  `http` included, which is why the plugin follows redirects itself. A body cut off before its `Content-Length`
  throws `java.io.IOException: Premature EOF` out of the input stream.
* `com.intellij.util.io.Decompressor.Tar(InputStream)` unpacks a `.tar.bz2` by itself, since it detects the
  compression through commons-compress, and sets the POSIX permissions of the tar entries. The platform ships
  commons-compress 1.28.0 as `lib/intellij.libraries.commons.compress.jar`, and it is on the compile class path of
  the tests, so a test can build a `.tar.bz2` with `TarArchiveOutputStream` and `BZip2CompressorOutputStream`.
* Inside `ProgressManager.getInstance().runProcess(work, indicator)`, `ProgressManager.getInstance().progressIndicator`
  is that indicator, and `ProgressManager.checkCanceled()` after `indicator.cancel()` throws
  `ProcessCanceledException`, also when called from the entry filter of a `Decompressor`.
* `Messages.showYesNoDialog(null, message, title, yesText, noText, icon)` reaches the `TestDialog` of
  `TestDialogManager` with the message, and returns what it answers.
* A `Notification` sent with `notify(null)` reaches a subscriber of `Notifications.TOPIC` on the application's
  message bus. The test environment reports unit test mode and headless.

And outside the IDE, in a plain JDK 25.0.1 on APFS:

* `FileChannel.open(directory, READ).force(true)` works on a directory.
* `Files.move(source, target, ATOMIC_MOVE)` of a directory replaces a target that is an empty directory, and throws
  a `FileSystemException` with the reason `Directory not empty` for a target that is not empty.
* A second `tryLock` on a file the same JVM has locked throws `OverlappingFileLockException`.

Read in the bytecode of IntelliJ IDEA 2026.2.3, not run:

* `CoreProgressManager.startTask` calls `Task.run(indicator)` without looking at the cancellation first, so the work
  of a queued `Task.Backgroundable` always runs and the guard it holds is always released.
* `BackgroundableProcessIndicator` of a task without a project shows its progress in the status bar of the frame
  focused last, through `WindowManagerImpl.findFrameHelper(null)`, which is the frame where the button was pressed.

Checked by the main session on 2026-10-16 and not again here: the GitHub API reports the release asset of the
package as 132,303,094 bytes with the SHA-256 of the README, and its address answers with one `302` to an HTTPS
address on `release-assets.githubusercontent.com`, then `200`. Unit 0004 measured the unpacked directory as
189,882,497 bytes. The hand-placed model on this Mac holds 377 files in 40 directories and no symbolic link.

### Decisions the spec leaves to the plan

* **The question** is `Messages.showYesNoDialog` with the project of the event, the title `AloudRite`, the buttons
  `Download` and `Not Now`, and the question icon. Its text is, with the sizes taken from the package constants and
  the folder as an absolute path:
  `AloudRite needs its speech model to speak, and the model is not on this machine yet. Download it now?` then an
  empty line, then `The download is 132 MB, from <address>. Once unpacked, the model takes 190 MB in <model folder>.`
  "MB" is 1,000,000 bytes, rounded to the nearest whole number. Any answer but `Download`, closing the dialog
  included, declines.
* **The package constants** are one value, `KOKORO_MODEL_PACKAGE`, of a class `ModelPackage` with the address, the
  size of 132,303,094 bytes, the unpacked size of 189,882,497 bytes and the SHA-256
  `4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e`. The tests give the installer a `ModelPackage`
  of their own, for the stand-in server and the stand-in package. Only the address of the production value has to
  be HTTPS; a test address is `http://127.0.0.1`, because the stand-in server speaks plain HTTP.
* **Redirects** are followed by the plugin: `HttpRequests` with `followRedirects(false)`, `gzip(false)` and
  `throwStatusCodeException(false)`, for each address in turn. A `301`, `302`, `303`, `307` or `308` leads to the
  address of its `Location` header, resolved against the address that sent it, and only when that is `https`. At
  most 10 redirects are followed. `HttpRequests` applies the proxy settings of the IDE to every request it opens,
  which is requirement 15.
* **The progress indication** is a `Task.Backgroundable` without a project, cancellable, titled
  `Downloading the AloudRite speech model`. The download shows the text `Downloading the speech model`, the second
  text `<received> of 132 MB` and the fraction received of the package size. The unpacking shows the text
  `Unpacking the speech model` and the fraction of the package read so far. The installer looks at the cancellation
  at every read of the network or of the package, including inside the one large entry `model.int8.onnx`, so a
  cancel takes effect within one buffer. From the rename of step 10 on, the model is in place and a cancel no longer
  changes anything.
* **The task has no project**, because the model belongs to the user and not to a project, and its progress then
  appears in the frame where the button was pressed (see the trial).
* **The work holds the guard of unit 0004.** The download service uses `OneAtATime` as `SpeechService` does, with a
  background function that queues the task and runs the work in its `run`. The work takes its indicator from
  `ProgressManager.getInstance().progressIndicator`, which inside a task is the task's own. A test runs the work it
  was handed inside `ProgressManager.getInstance().runProcess(work, indicator)` with an indicator of its own.
* **What decides the outcome** is what the work does, and not the `onSuccess` or `onCancel` of the task, because the
  platform calls `onCancel` for a task whose indicator was cancelled after the work ended. The work itself starts the
  speech after the model is in place, and itself reports a failure.
* **Speech after the download** is the test sentence through the `speak` function the action was given, with the
  model folder and the time of the original press. The line `First sound <n> ms after the button was pressed` in the
  log then includes the time of the download, which is what the line says.
* **The attempt directory** is `.<package name>.download-<random>` in the parent of the model folder, made with
  `Files.createTempDirectory(parent, ".kokoro-int8-multi-lang-v1_0.download-")`. It holds a file `lock`, locked
  with `FileChannel.tryLock` for the whole attempt, the package as `kokoro-int8-multi-lang-v1_0.tar.bz2`, and the
  directory `unpacked` that the package is unpacked into. It lies next to the model folder so that the rename of
  step 10 stays on one file system and is atomic; the temporary folder of the machine may be another file system.
* **Leftovers are removed at the start of the next attempt** and not at the press, as requirement 10 says. A
  directory of the parent whose name starts with `.kokoro-int8-multi-lang-v1_0.download-` is a leftover when its
  `lock` can be locked; when `tryLock` returns null or throws `OverlappingFileLockException`, an attempt is running
  in another IDE, or in this JVM, and the directory is left alone. The developer runs the sandbox IDE and an
  installed IDE against the same model folder, as the README says, and two attempts must not delete each other's
  files: an attempt that loses files it has written could rename an incomplete directory into place. The operating
  system releases the lock of a killed IDE.
* **Two IDEs that download at once** both download and unpack, each in its own attempt directory. The first rename
  wins. The second finds the model folder in place when its rename fails, takes that as success, deletes its own
  attempt directory and speaks.
* **Something in the way.** When the model folder exists, is not empty, and does not hold a model, the plugin made
  none of it, since it only ever creates that folder by renaming a complete one: it is the user's, for example a
  hand-placed model cut off halfway. The plugin does not delete or merge it. The attempt fails before any request,
  with a notification that names the folder and asks the user to remove it, and that offers to try again. An empty
  directory there is replaced by the rename. This is a failure the spec does not name; requirement 11 asks for "at
  the least" five kinds, and this adds one.
* **The directories made for the parent** are removed after a failure or a cancellation where they are empty,
  deepest first, so that a failed first attempt leaves no `AloudRite` or `aloudrite` directory, and on Linux no
  `.local/share` it had to create. After a success they stay, since they hold the model.
* **Flushing.** Every regular file and every directory of the unpacked model is opened with `FileChannel.open` and
  `force(true)`, then renamed, then the parent is forced. This is the most the JDK offers against a power loss. It
  does not make the drive empty its own cache on macOS, where `fsync` does not ask it to, which is recorded as a
  limit in the README.
* **The unpacking is checked** after it ends: the directory `unpacked` holds exactly one entry,
  `kokoro-int8-multi-lang-v1_0`, and `isModelInPlace` is true for it. Otherwise the package "could not be
  unpacked". Symbolic links that point out of the target are refused with
  `Decompressor.EscapingSymlinkPolicy.DISALLOW`, and `Decompressor` itself refuses entry names that leave the target.
* **The package is capped at its size.** A body longer than the size of the package fails as a checksum that does
  not match as soon as it passes that size, so that a wrong answer of the server cannot fill the disk.
* **The kinds of failure** are a sealed class `InstallFailure`, carried by a `ModelInstallException`:
    * `NoConnection(host)`: an `IOException` before the status of a response arrived
    * `ServerAnswer(status)`, `RedirectRefused(location)` and `TooManyRedirects`: an answer that is neither `200` nor
      a redirect to follow
    * `BrokeOff(receivedBytes, totalBytes)`: an `IOException` while reading the body, or a body shorter than its
      `Content-Length`
    * `OutOfDiskSpace(folder)`: an `IOException` while writing, during the download or the unpacking, whose message
      contains `No space left on device`, or after which the file store of the parent has less than 10,000,000 bytes
      usable
    * `CannotUnpack(detail)`: any other `IOException` of the unpacking, or an unpacking that gave no model
    * `ChecksumMismatch(address)`: a SHA-256 that is not the one fixed in the plugin, or a body longer than the package
    * `FolderInTheWay(folder)`: see above
    * `Unexpected(detail)`: any other `IOException`, such as a folder the user may not write to
* **The notification** is of a group `AloudRite` registered in `plugin.xml` as a balloon, of the type `WARNING`, with
  the title `AloudRite could not download the speech model` and one action, `Try Again`, which expires the
  notification and starts the same download again, without a second question, followed by speech as after the
  first. Its content, one per kind:
    * `There is no connection to <host>. Check the network and the proxy settings of the IDE.`
    * `The download address answered with HTTP <status>.`
    * `The download address sent AloudRite on to <location>, which is not an HTTPS address, so AloudRite did not
      follow it.`
    * `The download address redirected AloudRite more than 10 times.`
    * `The download broke off after <received> of <total> MB.`
    * `There is not enough free disk space in <folder>. The download and the unpacking need about 322 MB for a
      moment, and the model takes 190 MB after.`, with the two figures from the package constants
    * `The package could not be unpacked: <detail>.`
    * `The package at <address> is not the one this version of AloudRite was built for: its SHA-256 checksum does
      not match. The publisher has replaced the package at that address before. The README of AloudRite says how to
      put a model in place by hand.`
    * `The folder <folder> exists and does not hold a complete model. Remove it, then try again.`
    * `<detail>`, the message of the exception
* **A cancellation reports nothing.** The user asked for it. It is logged at level INFO.
* **A failure is logged** with `warn`, with the exception, as `SpeechService` does, and never with `error`, which
  would be an error report of the IDE. `ProcessCanceledException` is rethrown by the work and never logged.
* **The action is handed the download service** as a function that gives a `ModelDownloads`, the narrow contract of
  the service (`isRunning`, `start(modelFolder, onModelInPlace)`), which the constructor without arguments fills
  with `service<ModelDownloadService>()`, looked up when it is called, as `speak` is.
* **A press while a download runs** is ignored without a word, as a press while a sentence is spoken is in unit
  0004: the progress in the status bar already says what is happening.
* **Platform code.** The plugin uses `HttpRequests`, `Decompressor`, `Task.Backgroundable`, `Messages` and the
  notification API of the platform. It names no class of commons-compress in its main code, adds no dependency, and
  `THIRD-PARTY.md` lists nothing new, since what it uses is part of the IDE as every platform API is.
* **An opt-in test with the real package.** `RealModelPackageTest` runs only where the environment variable
  `ALOUDRITE_MODEL_PACKAGE` names a copy of the real package on disk, and is reported as skipped otherwise. It
  serves that copy from the stand-in server with the production size and checksum, installs it into a temporary
  folder, and compares the result with the hand-placed model folder of the machine where that is in place. It is how
  requirement 8 is shown without touching the real model folder, which on this Mac must not be changed.

### Where the work is done

* **On the host.** The build fetches the jars of sherpa-onnx from `github.com`, as in unit 0004. The tests need no
  network.
* **The real model folder is never written by a test or a check of the implementer.** On this Mac
  `~/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0` holds the human's model, and it is not to be
  removed or changed. Every test uses a folder under a `TemporaryFolder`. The checks that need the model folder
  removed are the human's (requirement 17).
* **The human's uncommitted edit of `README.md`.** When the branch was cut, the working tree held an edit of the
  human's to the Requirements section of `README.md`, a stray `tha=t`, which is not part of this unit. It must not be
  committed, reverted or stashed by an agent. Before changing `README.md`, look at `git diff README.md`. If the edit
  is still there, ask the human what to do with it, and commit only this unit's own changes to the file.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/ModelPackage.kt`: new, the class `ModelPackage` and the value
  `KOKORO_MODEL_PACKAGE`
- `src/main/kotlin/com/ritenoar/aloudrite/InstallFailure.kt`: new, the sealed class `InstallFailure` and
  `ModelInstallException`
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadMessages.kt`: new, the text of the question and of each
  failure, and the rounding to MB
- `src/main/kotlin/com/ritenoar/aloudrite/PackageDownload.kt`: new, the only file that names `HttpRequests`: the
  download of the package with its redirects, its SHA-256 and its progress, the pure function that decides a
  redirect, and the pure function that decides whether a write failed for lack of disk space
- `src/main/kotlin/com/ritenoar/aloudrite/PackageUnpacking.kt`: new, the unpacking of the package with `Decompressor`
  and its progress
- `src/main/kotlin/com/ritenoar/aloudrite/InstallAttempt.kt`: new, the attempt directory with its lock, its removal,
  and the removal of leftovers
- `src/main/kotlin/com/ritenoar/aloudrite/ModelInstaller.kt`: new, the eleven steps of the Approach
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadService.kt`: new, the contract `ModelDownloads` and the
  application service that runs the installer as a background task, reports a failure and starts the speech
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: changed, the question in place of the missing-model
  message
- `src/main/resources/META-INF/plugin.xml`: changed, the notification group `AloudRite`
- `src/test/kotlin/com/ritenoar/aloudrite/ModelPackageStandIn.kt`: new, a small `.tar.bz2` built in the test, with
  its SHA-256, its expected tree, and variants that are cut off or lack an entry
- `src/test/kotlin/com/ritenoar/aloudrite/PackageServerStandIn.kt`: new, the stand-in server on `127.0.0.1` that
  serves, cuts off, redirects or answers with a status, and counts the requests
- `src/test/kotlin/com/ritenoar/aloudrite/ModelPackageTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadMessagesTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PackageDownloadTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PackageUnpackingTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/InstallAttemptTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ModelInstallerTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadServiceTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadFlowTest.kt`: new, the press of the button through the
  question, the download from the stand-in server and the speech
- `src/test/kotlin/com/ritenoar/aloudrite/RealModelPackageTest.kt`: new, opt-in
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: changed, the test of the missing-model message and
  the test of the unsupported platform move to `ModelDownloadFlowTest`, and the helper that makes the action passes a
  download service
- `README.md`: the download, and the model by hand as an alternative
- `THIRD-PARTY.md`: the note that the plugin makes no network request, and the section on the model package
- `changes/0005-model-download/record.md`: the log

Not changed, and named because the plan depends on it: `KokoroModel.kt`, whose `isModelInPlace` stays the check of
five entries, `SpeechService.kt`, `OneAtATime.kt` and `build.gradle.kts`.

## Tasks

- [x] Write `ModelPackageTest`, a plain JUnit 4 test: `KOKORO_MODEL_PACKAGE` has the address
      `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2`, the
      size 132,303,094, the unpacked size 189,882,497 and the SHA-256 of the Approach, all written out in the test,
      and its address starts with `https://`. Then write `ModelPackage.kt` and see the test pass (R7, R15)
- [x] Write `ModelDownloadMessagesTest`, a plain JUnit 4 test of `downloadQuestion(modelPackage, modelFolder)`, which
      for `KOKORO_MODEL_PACKAGE` and a folder gives exactly the text of the Approach with `132 MB`, the address,
      `190 MB` and the absolute folder; of the rounding, where 132,303,094 bytes is `132`, 189,882,497 is `190` and
      499,999 is `0`; and of `failureText(failure)` for each kind of `InstallFailure`, each asserted as the whole
      text of the Approach. Then write `InstallFailure.kt` and `ModelDownloadMessages.kt`, and see the test pass
      (R2, R11)
- [x] Write `ModelPackageStandIn.kt` in the test sources. It builds, in memory, a `.tar.bz2` whose one top directory
      is `kokoro-int8-multi-lang-v1_0/`, holding the four files and the directory `espeak-ng-data` of
      `KokoroModel.kt`, one file `espeak-ng-data/en_dict` and one `LICENSE`, each with content from a
      `java.util.Random` of a fixed seed so that it does not compress, about 300,000 bytes in all. It gives the bytes,
      their SHA-256, the expected tree as relative paths with their contents, and a `ModelPackage` for a given
      address. Variants: the package cut off at half its length, and a package without `lexicon-us-en.txt`. Write
      `PackageServerStandIn.kt`: an `AutoCloseable` around `com.sun.net.httpserver.HttpServer` on `127.0.0.1` and a
      free port, which per path serves bytes with their `Content-Length`, serves the full `Content-Length` and closes
      after a given number of bytes, answers with a redirect to a given `Location`, or answers with a given status,
      and counts the requests per path (R16)
- [x] Write `PackageDownloadTest`, a JUnit 4 test with the `ApplicationRule` class rule of `ReadAloudActionTest`.
      For the pure function `redirectTarget(current, location)`: an absolute `https` location is taken as it is, a
      relative one is resolved against the current address, and an `http` location, an `ftp` one and a missing one
      fail with `RedirectRefused`. For the pure function `isOutOfDiskSpace(message, usableBytes)`: true for a message
      containing `No space left on device`, true for 9,999,999 usable bytes, false for an other message and
      10,000,000 bytes. Against the stand-in server, with an `EmptyProgressIndicator`: the package arrives whole in the
      target file and the function returns its SHA-256; a `302` to an `http` address fails with `RedirectRefused` and
      the address it names is never requested; a `404` fails with `ServerAnswer(404)`; eleven redirects in a row fail
      with `TooManyRedirects`; a body cut off halfway fails with `BrokeOff` with the bytes received; a body longer
      than the package size fails with `ChecksumMismatch`; an address on a port nobody listens on fails with
      `NoConnection`; and an indicator that is cancelled when half the package has arrived makes the function throw
      `ProcessCanceledException`. Then write `PackageDownload.kt`: the loop over redirects of the Approach, a buffer
      of 65,536 bytes, the SHA-256 computed while the bytes arrive, `checkCanceled` and the progress at every buffer,
      reading and writing in separate `try` blocks so that a failed read is `BrokeOff` and a failed write is
      `OutOfDiskSpace` or `Unexpected`. See the test pass (R5, R6, R11, R15)
- [x] Write `PackageUnpackingTest`, a JUnit 4 test with the `ApplicationRule`: the stand-in package written to a
      file unpacks into a target directory to exactly the expected tree; the cut-off package fails with
      `CannotUnpack`; and an indicator that cancels itself at the first fraction above zero while its text is
      `Unpacking the speech model` makes the function throw `ProcessCanceledException`. Then write
      `PackageUnpacking.kt`: `Decompressor.Tar` over an input stream of the package that counts the bytes read,
      calls `checkCanceled` at every read and sets the fraction of the package size, with
      `EscapingSymlinkPolicy.DISALLOW`. See the test pass (R5, R6, R8, R11)
- [x] Write `InstallAttemptTest`, a plain JUnit 4 test under a `TemporaryFolder`: a new attempt is a directory whose
      name starts with `.kokoro-int8-multi-lang-v1_0.download-`, holding a locked `lock`; closing it removes the
      directory with everything in it; removing leftovers deletes such a directory with files in it whose lock is
      free, leaves one alone whose lock the test holds, and leaves alone every other entry of the parent, a model
      folder included. Then write `InstallAttempt.kt` and see the test pass (R9, R10)
- [x] Write `ModelInstallerTest`, a JUnit 4 test with the `ApplicationRule`, the stand-in server and a model folder
      `<temporary folder>/data/aloudrite/kokoro-int8-multi-lang-v1_0` whose parent does not exist yet. Each case ends
      by asserting what is on disk. Its indicator asserts, at every fraction it is given, that the model folder does
      not exist (R9):
    - success: `isModelInPlace` is true, the model folder holds exactly the expected tree, and the parent holds the
      model folder and nothing else
    - a model already in place: nothing is requested and nothing changes
    - a non-empty folder in the way: `FolderInTheWay`, nothing is requested, the folder is as it was
    - an empty folder in the way: success, as above
    - cancel during the download, cancel during the unpacking, a body cut off, a checksum that does not match, the
      cut-off package served with its own checksum, and the package without `lexicon-us-en.txt` served with its own
      checksum: `ProcessCanceledException` for the two cancellations and the kinds `BrokeOff`, `ChecksumMismatch`
      and `CannotUnpack` for the others, then `isModelInPlace` is false and `<temporary folder>/data` is empty, since
      the attempt created `aloudrite` and removed it
    - a failure where the parent existed before: the parent stays, and is empty
    - a leftover attempt directory with a half package in it, its lock free: a successful attempt removes it
    - a model folder that appears during the unpacking, filled by the stand-in function of `KokoroModelStandIn.kt`
      from the indicator of this case, as another IDE would leave it: the rename fails, the attempt counts as
      success, the folder the test made is the one in place, and no attempt directory is left
      Then write `ModelInstaller.kt` with the eleven steps of the Approach, and see the test pass
      (R7, R8, R9, R10, R11, R14)
- [x] Write `ModelDownloadServiceTest`, a JUnit 4 test with the `ApplicationRule`. The service is made with a
      background function that keeps the work, an `install` function of the test and a `reportFailure` that keeps
      what it is given; the test runs a kept work inside `ProgressManager.getInstance().runProcess` with an
      `EmptyProgressIndicator`. `start` hands one work over and returns true, `isRunning` is true until it has run,
      a second `start` meanwhile returns false and hands nothing over. After an `install` that returns, the callback
      ran once and nothing is reported. After an `install` that throws a `ModelInstallException`, the failure is
      reported once with a retry, the callback did not run, `isRunning` is false, and calling the retry hands a new
      work over that runs the callback after a successful `install`. After an `install` that throws
      `ProcessCanceledException`, the work rethrows it, nothing is reported, the callback did not run and `isRunning`
      is false. After an `install` that throws an `IOException`, it is reported as `Unexpected`. Then write
      `ModelDownloadService.kt`: the contract `ModelDownloads`, the `@Service(Service.Level.APP)` class with a
      primary constructor that takes the three collaborators, and a constructor without arguments that queues a
      `Task.Backgroundable(null, "Downloading the AloudRite speech model", true)`, installs with a `ModelInstaller`
      of `KOKORO_MODEL_PACKAGE`, and reports through the notification of the Approach. Register
      `<notificationGroup id="AloudRite" displayType="BALLOON"/>` in `plugin.xml`. See the test pass
      (R5, R6, R11, R12, R13)
- [x] Write `ModelDownloadFlowTest`, a JUnit 4 test with the `ApplicationRule`, which presses a `ReadAloudAction`
      made with `Linux`, `amd64`, a model folder under a `TemporaryFolder`, a `speak` that keeps what it is given, and
      a `ModelDownloadService` with a background function that keeps the work, an installer of the stand-in package
      on the stand-in server, and the production `reportFailure`. The press goes through `ActionUtil.performAction`
      on the event dispatch thread as in `ReadAloudActionTest`; the answer to the question comes from a `TestDialog`
      that keeps every message; the kept work is then run on the test thread inside `runProcess`; a notification is
      caught through a subscriber of `Notifications.TOPIC`. Every case that ends without a model asserts that
      `isModelInPlace` is false for the folder. The cases (R16):
    - a download that succeeds: one question, with the text of `downloadQuestion`; after the work, the model is in
      place and the sentence was spoken once, with that folder, and the press itself spoke nothing
    - a declined question: nothing is handed over, nothing is requested and nothing is spoken; a second press asks
      again
    - a cancellation during the download and one during the unpacking, through the indicator the test runs the work
      with: nothing is spoken and no notification comes
    - a download that breaks off, a checksum that does not match, and a package that cannot be unpacked: one
      notification each, of the group `AloudRite`, whose content is the `failureText` of the kind and which has the
      action `Try Again`; nothing is spoken
    - a model already in place: no question, no request, and the sentence spoken at the press
    - a press while a download is running: no second question, and no second work handed over
    - a platform that is not supported, `Windows 11` and `amd64` with an empty folder: the message of unit 0004 for
      that case, no question, nothing handed over, nothing requested
      Run `./gradlew test` and see it fail to compile, because the action has no such constructor (R1, R3, R4, R12,
      R13, R14, R16)
- [x] Change `ReadAloudAction.kt`: the primary constructor takes, after `speak`, a function that gives the
      `ModelDownloads`, and the constructor without arguments passes `{ service<ModelDownloadService>() }`.
      `actionPerformed` takes the time, shows the message for an unsupported platform where `isSpeechSupported` is
      false, speaks where `isModelInPlace` is true, returns where a download is running, asks the question, and on
      `Download` starts the download with a callback that speaks the test sentence with the folder and the time of the
      press. `modelMissingMessage` goes. In `ReadAloudActionTest`, move
      `pressingTheButtonOnAnUnsupportedPlatformSaysSoAndSpeaksNothing` into `ModelDownloadFlowTest` as its last case
      and delete `pressingTheButtonWithoutTheModelShowsWhereItIsMissingAndSpeaksNothing`, which the declined question
      replaces, and let its helper make the action with a download service whose background function keeps the work.
      The seven other tests stay as they are. Run `./gradlew test` and see every test pass (R1, R3, R4, R12, R13,
      R14)
- [x] Write `RealModelPackageTest`, a JUnit 4 test with the `ApplicationRule`, which opens with `assumeTrue` on the
      environment variable `ALOUDRITE_MODEL_PACKAGE` naming a file, so that it is reported as skipped otherwise. It
      serves that file from the stand-in server, installs it with a `ModelPackage` of the production size, unpacked
      size and checksum and the stand-in address into a folder under a `TemporaryFolder`, and asserts that the model
      is in place. Where the model folder of the machine is in place, it compares the two trees: the same relative
      paths, the same kinds of entry, and the same SHA-256 for every file. It reads that folder and writes nothing
      to it. It prints the time the installation took (R7, R8)
- [x] Download the real package once into a new temporary directory outside the repository with `curl -fLO` from
      its address, check it with `shasum -a 256` against the SHA-256 of the Approach, and run
      `ALOUDRITE_MODEL_PACKAGE=<that file> ./gradlew test --tests com.ritenoar.aloudrite.RealModelPackageTest`. See it
      pass and not be skipped. Where the trees differ, or the POSIX permissions of the files differ from those of the
      hand-placed model, write what differs into `record.md`, and correct the unpacking where the contents differ.
      Delete the temporary directory afterwards (R7, R8)
- [x] Rewrite the parts of `README.md` that this unit makes untrue, and add what it brings. The introduction no
      longer says that the plugin makes no network request: it says that the first press without the model asks
      whether to download it, and that this download is the only network request the plugin makes. The paragraph on
      the two messages names the question in place of the missing-model message. The section on the model says: the
      plugin downloads it at the first press after asking, 132 MB from the address, checks it against the SHA-256,
      and unpacks it into the model folder, 190 MB; the download runs in the background with its progress in the
      status bar, where it can be cancelled; it uses the proxy settings of the IDE; it needs about 322 MB free for a
      moment; nothing of a failed or cancelled download is left behind, and what a killed IDE left is removed at the
      next download; a failure shows a notification that offers to try again; a model folder that exists without a
      complete model is not touched and has to be removed by hand; and a hand-placed model is used as it is. It says
      how to remove the model: delete the model folder, with the command for each platform. The package, its
      address, its sizes and its SHA-256 stay, with the note that the publisher has replaced it before and that a
      different checksum then makes the download fail until a new version of the plugin is out. The commands that put
      the model in place by hand stay as they are, introduced as an alternative, for a machine without network
      access or for when the download fails, and no longer as a step. It records the limit of the flush: on macOS the
      drive may hold the last writes in its own cache at a power loss. Everything else in the README stays as it is,
      and the human's uncommitted edit is handled as "Where the work is done" says (R18)
- [x] In `THIRD-PARTY.md`, replace the note `The plugin makes no network request. The libraries are not fetched at
      run time.` with one saying that the libraries are not fetched at run time and that the only thing the plugin
      fetches is the model package, after the user agreed. Rename the section "Fetched by the user" to say that the
      plugin downloads the package at the user's request, or the user fetches it, and change its last sentence, which
      says that the README says how to get it, to say that the README describes the download and how to put it in
      place by hand. Nothing else in the file changes (R15, R18)
- [ ] Hand the human what their checks of requirement 17 need, and write their answers into `record.md` as they give
      them: the archive of `./gradlew buildPlugin` to install on Linux x64 and on macOS on Apple Silicon; the steps,
      which are to move the model folder aside, press and decline and see that nothing was downloaded, press and
      agree and cancel halfway and see that neither the model folder nor an attempt directory
      `.kokoro-int8-multi-lang-v1_0.download-*` is left in its parent, and press and agree and wait and hear the
      sentence start by itself; the command that compares the downloaded model with the one moved aside,
      `diff -r <moved-aside folder> <model folder>`; and that the folder moved aside is then deleted or put back, as
      the human chooses. No agent moves or deletes the model folder on the human's Mac (R17)
- [ ] Before the status is set to `implemented`, write into `record.md` a list of what this unit established that the
      spec of unit 0006 rests on, at the least: that a press without the model asks the question and then downloads
      in the background; that the speech of the press starts by itself after the download, through the `speak`
      function of the action, with the test sentence; that a press while a download runs is ignored; and that the test
      sentence is still a constant of `ReadAloudAction.kt`. Ask the human to have the specifier revise the spec of
      unit 0006 against that list, on `master` as the process says, where unit 0006 is not implemented yet. Log in
      `record.md` whether it changed and that the human has seen the revision, or that unit 0006 needed no change, or
      was implemented already (R19)

## Verification

- [ ] `./gradlew clean buildPlugin` succeeds, the archive is still no larger than 30,000,000 bytes, and `./gradlew test`
      passes with every test of the tasks above and the seven of `ReadAloudActionTest` that stay. `RealModelPackageTest`
      is reported as skipped without the variable (R16)
- [ ] `grep -rn 'HttpRequests' src/main` finds only `PackageDownload.kt`, and `grep -rn -E
      'java\.net\.(URL\(|http)|HttpClient|openConnection|org\.apache\.commons\.compress' src/main` finds nothing: the
      download is the only network request and goes through the platform (R15)
- [ ] `git ls-files` shows no file ending in `.onnx`, `.bin` or `.tar.bz2`, and the archive holds no model
- [ ] After all tests and checks of the implementer, `~/Library/Application Support/AloudRite` on this Mac holds the
      one directory `kokoro-int8-multi-lang-v1_0` it held, with the same 377 files, and nothing else: no attempt
      directory. On Linux the same holds for its model folder
- [ ] The press in an IDE, which needs the model folder moved aside, a display and the network. On this Mac the
      model folder is not to be touched, so here it is the human's check of requirement 17. Where the implementer
      works on Linux and the human agrees to the model folder being moved aside there: in the sandbox IDE of
      `./gradlew runIde`, press the button, decline, press and agree, cancel, press and agree again, see the
      progress in the status bar, see that typing in an editor goes on meanwhile and that the sentence starts by
      itself, and put the folder back. Log what was checked and where, or that it was left to the human
      (R1, R3, R5, R6, R12)
- [ ] `record.md` holds the human's answers of requirement 17 for both platforms (R17)
- [ ] `npm run check-changes` passes on the committed branch

## Commit Points

- [ ] `feature-0005 Add the model package and the texts of the download`: `ModelPackage.kt`, `InstallFailure.kt`,
      `ModelDownloadMessages.kt` and their tests
- [ ] `feature-0005 Download and unpack the model package`: `PackageDownload.kt`, `PackageUnpacking.kt`, the two
      stand-ins and their tests
- [ ] `feature-0005 Put the model in place as a whole or not at all`: `InstallAttempt.kt`, `ModelInstaller.kt` and
      their tests
- [ ] `feature-0005 Run the download as a background task that reports its failures`: `ModelDownloadService.kt`,
      `plugin.xml` and `ModelDownloadServiceTest.kt`
- [ ] `feature-0005 Ask to download the model when the button is pressed without it`: `ReadAloudAction.kt`,
      `ReadAloudActionTest.kt`, `ModelDownloadFlowTest.kt` and `RealModelPackageTest.kt`
- [ ] `feature-0005 Describe the download in the README and the third-party list`: `README.md` and `THIRD-PARTY.md`,
      without the human's uncommitted edit
- [ ] `feature-0005 Record the implementation`: `record.md` and the ticked boxes of this plan
