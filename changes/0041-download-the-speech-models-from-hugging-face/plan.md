# Download the speech models from Hugging Face

## Approach

The plan was written against `feature-0041-download-the-speech-models-from-hugging-face` at `edecf6d`.

Today every `InstallablePackage` carries one `ModelPackage`, an archive with an address, two sizes and a SHA-256, and
`ModelInstaller` downloads it with `downloadPackage`, checks it, unpacks it into the attempt directory, flushes the
tree and renames it into place. The unit keeps that frame and adds a second kind of package:

* `ModelPackage` becomes a sealed interface with `address`, `sizeInBytes` and `unpackedSizeInBytes`, which the
  messages and the settings page read as today. The archive becomes `ArchivePackage`, which the aligner model keeps
  (R11). The new `RepositoryFiles` holds a repository, a commit and the list of `RepositoryFile`s (path, size,
  SHA-256). Its `sizeInBytes` and `unpackedSizeInBytes` are both the sum of the sizes (R9), and its `address`, which
  the question before a download names as where the model comes from, is the page of the repository at the commit,
  `https://huggingface.co/<repository>/tree/<commit>`. `fileAddress` gives
  `<host>/<repository>/resolve/<commit>/<path>` with each segment of the path percent-encoded, every byte outside the
  unreserved characters of RFC 3986 encoded as UTF-8, so a space becomes `%20` and `!` becomes `%21` (R4). The host
  is a property with the default `https://huggingface.co`, so that the tests point it at the stand-in server, as they
  set the address of an archive today.
* The lists are one resource file of `core`, `speech-model-files.tsv`, read by one parser in `core` that both the
  plugin and the check of R15 call (R2). Each line is tab-separated, the path last so that a path may hold a space:
  `model<TAB><package name><TAB><repository><TAB><commit>` opens a model, and
  `file<TAB><size><TAB><sha256><TAB><path>` follows for each of its files, sorted by path in byte order. Lines that
  start with `#` are comments. The parser is the boundary: it rejects, naming the line number, a line of another kind,
  a size that is not a positive number, a SHA-256 that is not 64 lowercase hexadecimal digits, a commit that is not 40,
  a path that is absolute, holds a backslash, an empty segment, `.` or `..`, or that repeats, and a `file` line before
  any `model` line. `SpeechModel.kt` builds `KOKORO_INT8` and `KOKORO_FP32` from it by package name.
* `ModelInstaller.installInAttempt` branches on the kind of the package. An archive goes the way it goes today. A
  `RepositoryFiles` is downloaded by the new `downloadRepositoryFiles` into `<attempt>/files/<package name>`, which is
  then checked with `isInPlace`, flushed and renamed into place by the code that does this for an unpacked archive
  today. Nothing is unpacked and nothing of a failed attempt stays, as the attempt directory is removed when the
  attempt closes (R3).
* `downloadRepositoryFiles` is a coordinator on the thread of the task and up to eight workers of a fixed pool (R6).
  Only the coordinator touches the indicator and the state: the files still missing, the requests open, the end of a
  pause and whether the next request is the first after a pause. Each worker fetches one file through the same
  per-address fetch the archive uses, which follows redirects only to HTTPS and goes through the proxy settings of
  the IDE, counts the bytes it receives into one shared counter, checks the size and the SHA-256 of the file as it
  arrives, and hands its outcome back on a queue. A file larger than its size is broken off as soon as it passes it,
  as an archive is today, and a size or a SHA-256 that does not match fails the download with
  `InstallFailure.ChecksumMismatch` naming the address of the file (R3). The coordinator sets the fraction to the
  bytes received of the sum, as one bar (R8).
* An answer of 429, at any hop of a file, is an outcome of its own that carries the `Retry-After` and `ratelimit`
  headers. The pure function `rateLimitPause` gives the pause: the whole seconds of `Retry-After`, or else the `t` of
  `ratelimit`, or else 60 seconds, at most 300 (R7.2). The coordinator then submits nothing until the pause ends, puts
  the refused file back among the missing ones, and keeps what has arrived (R7.1). A 429 to another request that was
  open before the pause began also puts its file back, and moves the end of the pause to the later of the two. During
  the pause the text reads "Hugging Face limits the downloads for the moment" and the second line "Continuing in
  m:ss", counting down, and the coordinator checks the cancellation as at any other moment (R7.3). When the pause
  ends, it submits one file alone, the first request after the pause, and fills up to eight again once that answers
  with anything but 429 (R7.4). A 429 to that one request fails the download with the new
  `InstallFailure.RateLimited`, whose text says that Hugging Face limits the downloads from this address for the moment
  and that "Try Again" can be pressed later (R7.5).
* A cancellation, or a failure of any file, makes the coordinator raise the stop flag that every worker checks before
  each read, shut the pool down and wait for every worker to end before it throws, so that no worker still writes
  when the attempt removes its directory (R8). As today, a read that the network holds ends before the check that
  follows it.
* The coordinator takes its time and its waiting from a small `DownloadClock`, whose real one is `System.nanoTime` and
  `poll` on the queue with a timeout, so that the tests run pauses of up to five minutes on a clock of their own in a
  moment.
* The check of R15 is a source set of its own in `core`, `sourcesCheck`, which is in neither the plugin archive nor
  `./gradlew test` and `./gradlew build`. Its pure function compares, for one model, the answers to two requests: a GET
  of `https://huggingface.co/api/models/<repository>/revision/<commit>`, which answers 200 with the full commit as its
  `sha` while the commit exists and 404 otherwise, and a HEAD, without following redirects, of the address of the
  largest file at that commit, whose answer carries `x-repo-commit`, `x-linked-size` and `x-linked-etag`, the SHA-256
  in quotes. Both were seen so on 2026-11-22 for both repositories. Its `main` makes the requests with
  `java.net.http.HttpClient`, prints each problem with the model, the address and what did not match, and ends with a
  non-zero status when there is one. The Gradle task `checkModelSources` is a `JavaExec` of `core` that runs it.

Two things were found against the tree while planning:

* The address of a file at Hugging Face answers with a redirect: a file in Git LFS goes to the CDN, `cdn.hf.co`, and
  any other file to a relative address under `/api/resolve-cache/`. Its answer carried no `ratelimit` header, while the
  first answer carried that of the bucket `resolvers`. Following the redirects a file's address answers with is taken
  as requesting that file, which R5 allows, and no other request to `/api/` is made. It is also what the specifier's
  trial did, which counted about 450 requests of the bucket `resolvers` for 377 files
* The tree listings of both repositories at their commits hold 378 files each, `.gitattributes` and 377 others, whose
  sizes add up to 189,881,734 and 401,239,297 bytes, as the spec says. Only `!` and the space occur in the paths
  outside the characters an address allows as they are

`RealModelPackageTest` installs a copy of the archive that the plugin pins, which no longer exists for the speech
models. It becomes `RealModelDownloadTest`, which runs only with `ALOUDRITE_REAL_DOWNLOAD_FOLDER` set and downloads both
speech models from Hugging Face into that folder through the real `ModelInstaller`. It is how the implementer shows the
download against the real server and gets the fixed int8 model for the tests with the real engine, which `AGENTS.md`
asks for, since the unit changes the int8 model handed to the engine.

## Files

- `core/src/main/kotlin/com/ritenoar/aloudrite/core/ModelPackage.kt`: the sealed interface `ModelPackage`,
  `ArchivePackage`, `RepositoryFiles`, `RepositoryFile` and the encoding of a path
- `core/src/main/kotlin/com/ritenoar/aloudrite/core/SpeechModelFiles.kt`: new, the parser of the resource file and
  the loading of it from the class path
- `core/src/main/resources/com/ritenoar/aloudrite/core/speech-model-files.tsv`: new, the two lists
- `core/src/main/kotlin/com/ritenoar/aloudrite/core/SpeechModel.kt`: the two models take their package from the lists;
  `releaseAddress` goes
- `core/src/main/kotlin/com/ritenoar/aloudrite/core/AlignerModel.kt`: `ArchivePackage` in place of `ModelPackage`
- `core/src/sourcesCheck/kotlin/com/ritenoar/aloudrite/sources/ModelSourcesCheck.kt`: new, the comparison and `main`
- `core/build.gradle.kts`: the source set `sourcesCheck`, its dependencies, `testImplementation` of its output, and the
  task `checkModelSources`
- `src/main/kotlin/com/ritenoar/aloudrite/PackageDownload.kt`: the fetch of one address split from the archive's
  progress, with an outcome for 429
- `src/main/kotlin/com/ritenoar/aloudrite/RepositoryDownload.kt`: new, `downloadRepositoryFiles`, its coordinator and
  `DownloadClock`
- `src/main/kotlin/com/ritenoar/aloudrite/RateLimitPause.kt`: new, `rateLimitPause` and the countdown text
- `src/main/kotlin/com/ritenoar/aloudrite/ModelInstaller.kt`: the branch on the kind of package
- `src/main/kotlin/com/ritenoar/aloudrite/InstallAttempt.kt`: the directory the files of a repository are written to
- `src/main/kotlin/com/ritenoar/aloudrite/InstallFailure.kt`: `RateLimited`
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadMessages.kt`: the text of `RateLimited`, "Once downloaded"
  for the files of a repository, and the space a download needs by the kind of package
- `src/main/kotlin/com/ritenoar/aloudrite/PackageUnpacking.kt`: `ArchivePackage` in place of `ModelPackage`
- `src/test/kotlin/com/ritenoar/aloudrite/PackageServerStandIn.kt`: answers in sequence, headers on an answer, a delay,
  the raw paths of the requests with the time of the stand-in clock, and the most requests open at once
- `src/test/kotlin/com/ritenoar/aloudrite/RepositoryFilesStandIn.kt`: new, a small repository of files with the shape of
  a model, one of them `espeak-ng-data/voices/!v/Mr serious`, served by the stand-in under its encoded addresses
- `src/test/kotlin/com/ritenoar/aloudrite/RepositoryDownloadTest.kt`, `RateLimitPauseTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/RealModelDownloadTest.kt`: new, in place of `RealModelPackageTest.kt`, which
  goes
- `src/test/kotlin/com/ritenoar/aloudrite/ModelInstallerTest.kt`, `ModelDownloadFlowTest.kt`,
  `ModelDownloadMessagesTest.kt`, `ReadingSpeedConfigurableTest.kt`, `SpeechModelManagementTest.kt`,
  `PackageDownloadTest.kt`, `ModelPackageStandIn.kt`: the new kind of package, the new sizes and addresses
- `core/src/test/kotlin/com/ritenoar/aloudrite/core/ModelPackageTest.kt`, `SpeechModelTest.kt`,
  `SpeechModelFilesTest.kt` (new), `ModelSourcesCheckTest.kt` (new)
- `.github/workflows/model-sources.yml`: new, the weekly check
- `README.md`: "The speech model" and "Testing"
- `THIRD-PARTY.md`: "Downloaded at the request of the user"

## Tasks

### The archive as one kind of package

- [x] Make `ModelPackage` a sealed interface with `address`, `sizeInBytes` and `unpackedSizeInBytes`, and rename the
  data class to `ArchivePackage` implementing it, with `sha256`. `downloadPackage`, `unpackPackage`,
  `ModelInstaller`'s archive path and `ALIGNER_MODEL` take `ArchivePackage`, and the speech models keep their archives
  for now. Update every test that builds a `ModelPackage` to build an `ArchivePackage`. No behavior changes, and
  `./gradlew test` passes as before (R11)
- [x] In `PackageDownload.kt`, split from `downloadPackage` a fetch of one address into one file that takes a check of
  the cancellation, a size the file must not pass and a callback for the bytes received, follows the redirects as today
  and returns the SHA-256 and the bytes received. `downloadPackage` keeps its signature and sets the indicator from the
  callback as it does now. No behavior changes, and `PackageDownloadTest` passes as before

### The lists of the files

- [x] Write `SpeechModelFilesTest`: a list of two models is read into their repositories, commits and files, in order;
  a path with a space is read whole; each malformed line listed under the approach fails with its line number; and the
  resource file of the plugin holds `kokoro-int8-multi-lang-v1_0` at
  `csukuangfj/kokoro-int8-multi-lang-v1_0` and `57adb37fc0e8d4bd430cfc1b8f8c2294945b9af2` and
  `kokoro-multi-lang-v1_0` at `csukuangfj/kokoro-multi-lang-v1_0` and `f7b96bb6bef5c5da4d3aa4f4e0498fbbf62dc78b`,
  377 files each, with the sums 189,881,734 and 401,239,297 bytes, `model.int8.onnx` of 114,202,993 bytes with
  SHA-256 `75635ea3b85a32d00094dcdd61a258aba87c160f7c264c304a7b3ae9d734195e`, `model.onnx` of 325,560,556 bytes
  with `b40f62b166ac8164b0627ef48a0b358eda0985e272fb03ef5252e7206305da11`, no `.gitattributes`, and every file that
  `SpeechModel.isInPlace` looks for (R1, R2, R9)
- [x] Write in `ModelPackageTest` that `fileAddress` of `espeak-ng-data/voices/!v/Mr serious` ends in
  `/resolve/<commit>/espeak-ng-data/voices/%21v/Mr%20serious`, that a path of letters, digits, `.`, `-`, `_` and `/`
  is left as it is, that a character outside ASCII is encoded as its UTF-8 bytes, and that `sizeInBytes` and
  `unpackedSizeInBytes` of a `RepositoryFiles` are the sum of its sizes (R4, R9)
- [x] Add `RepositoryFiles`, `RepositoryFile` and the encoding to `ModelPackage.kt`, and write `SpeechModelFiles.kt`,
  the parser and the loading of `speech-model-files.tsv` from the class path through the class loader of `core`
- [x] Make the resource file. In a new empty directory outside the repository, for each of the two repositories at its
  commit: fetch the listing `https://huggingface.co/api/models/<repository>/tree/<commit>?recursive=true`, take the
  paths of its files but `.gitattributes`, download each from its address with the path encoded as R4 says, eight at a
  time at most, and write one `file` line per file with its size and its SHA-256 as `sha256sum` gives them, sorted with
  `LC_ALL=C sort`. Before it is committed, check that each list has 377 files with the sums above; that every file in
  Git LFS has the SHA-256 that the listing gives as its `lfs.oid`; that every file of the fp32 list has the SHA-256 of
  the same file in `~/.local/share/aloudrite/kokoro-multi-lang-v1_0`; and that every file of the int8 list but
  `model.int8.onnx` has that of the same file in `~/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0`. Record the
  commands and the outcome of the checks in `record.md` (R2)
- [x] The tests of the lists pass. Commit point 1

### The download of the files

- [x] Extend `PackageServerStandIn`: a path may answer with a sequence of answers, each a status, headers, a body and a
  delay, the last repeating; it records the raw path of every request with the time of a clock the test hands in, and
  the most requests open at the same moment. Write `RepositoryFilesStandIn`, a repository of about 20 files of a few
  kilobytes with the files `SpeechModel.isInPlace` looks for and `espeak-ng-data/voices/!v/Mr serious`, served under
  `/<repository>/resolve/<commit>/` by the raw, encoded path only, so that an address that is not encoded gets 404
- [x] Write `RateLimitPauseTest`: `Retry-After: 30` gives 30 seconds whatever `ratelimit` says; only
  `ratelimit: "resolvers";r=0;t=120` gives 120; neither gives 60; `Retry-After: 900` and `t=900` give 300; a
  `Retry-After` that is not a number of seconds falls to `ratelimit`, and a `ratelimit` without `t` to 60; and the
  countdown text of 277 seconds is "Continuing in 4:37" (R7.2, R7.3)
- [x] Write `RepositoryDownloadTest`, with the stand-in and a clock of the test, covering what R12 lists for the
  download: the repository arrives into its folder, the file with the space among it, with the content of each file;
  a file one byte longer than its size and a file whose SHA-256 differs each fail with `ChecksumMismatch` naming the
  address of that file; a 429 with `Retry-After`, with only `ratelimit` and with neither each pauses for 30, 120 and 60
  seconds of the clock, and one of 900 for 300; no request arrives between the 429 and the end of the pause, and no
  file that arrived before the pause is requested again; the second line of the indicator counts down during the pause
  and the first line names the limit of Hugging Face; the download completes after the pause with every file, the
  refused one among them; a 429 to the first request after the pause fails with `RateLimited`; a cancellation during
  the pause ends the download with `ProcessCanceledException` and no worker running; no more than eight requests are
  open at once, with an answer delayed so that they pile up, and more than one is; a cancellation while files arrive
  stops every worker before the call returns; the stand-in receives requests for the addresses of the files of the
  list and for no other path; and the fraction ends at 1.0 and never goes down (R3, R4, R5, R6, R7, R8, R12)
- [x] Add the outcome of a 429 to the fetch of one address, with the two headers. `downloadPackage` turns it into
  `ServerAnswer(429)` as today, so the aligner model behaves as before (R11)
- [x] Write `RateLimitPause.kt` and `RepositoryDownload.kt` as the approach describes, until `RateLimitPauseTest` and
  `RepositoryDownloadTest` pass (R3 to R8)
- [x] Add `InstallFailure.RateLimited` and its text in `failureText`: "Hugging Face limits the downloads from this
  address for the moment. Press "Try Again" later to download the model." Cover the text in
  `ModelDownloadMessagesTest` (R7.5)
- [x] Write in `ModelInstallerTest`, for a `RepositoryFiles` on the stand-in: the model is in place in its folder with
  exactly the files of the list and nothing else; after a file that does not match, the model folder does not exist
  and the attempt directory is gone; a model folder that is in place makes no request, by the request counts of the
  stand-in (R2, R3, R10)
- [x] Give `InstallAttempt` the directory the files of a repository are written to, and make `ModelInstaller` branch on
  the kind of package, as the approach describes, until those tests pass. The archive tests pass unchanged
- [x] Write in `ModelDownloadFlowTest` a press on "Read Aloud" that downloads a speech model of `RepositoryFiles` and the
  aligner model as an archive in one attempt and then reads, and one where the download meets a 429 after a pause and
  the notification shows the text of `RateLimited` with "Try Again" (R7.5, R11)
- [x] Make `SpeechModel.kt` take the packages of `KOKORO_INT8` and `KOKORO_FP32` from the lists, and remove
  `releaseAddress`. Update `SpeechModelTest`, `ModelPackageTest` and the tests of the messages, the settings page and the
  management to the new addresses and sizes: 190 MB to download and 190 MB on disk for the int8 model, 401 MB and
  401 MB for the fp32 model, 449 MB with the aligner model for the fp32 model, from
  `https://huggingface.co/csukuangfj/<package name>/tree/<commit>` (R1, R9)
- [x] In `ModelDownloadMessages.kt`, say "Once downloaded" in place of "Once unpacked" for the files of a repository,
  and in the text of `OutOfDiskSpace` give the space a download needs by its kind: the download and the unpacking
  together for an archive, as today, and the size of the files for a repository, which the model then takes. Cover both
  in `ModelDownloadMessagesTest` (R9)
- [x] `./gradlew test` passes. Commit point 2

### The check of the sources

- [x] Write `ModelSourcesCheckTest` in `core`, with stand-in answers to the comparison: answers that match give no
  problem; a 404 of the revision, and a 200 whose `sha` is another commit, each give a problem that names the model, the
  address and that the commit is missing; an `x-linked-size` and an `x-linked-etag` that differ from the list each give
  a problem with both values; an `x-repo-commit` that differs, a missing header and a status of 4xx or 5xx of the HEAD
  each give one; and the largest file of the list is the one asked for (R15)
- [x] Add the source set `sourcesCheck` to `core/build.gradle.kts`, with the output and the resources of `main` on its
  class paths and the Kotlin standard library, and its output on the class path of the tests of `core`. Write
  `ModelSourcesCheck.kt` until the test passes, and register `checkModelSources` as a `JavaExec` of its `main` in the
  group `verification`, depending on no other task of `test` or `build`. Check that `./gradlew build --dry-run` and
  `./gradlew test --dry-run` do not list it, and that the plugin archive holds no class of the package
  `com.ritenoar.aloudrite.sources` (R15)
- [x] Run `./gradlew checkModelSources` in the sandbox and see it succeed. Change the commit of one model in a copy of
  the resource file on the class path, by a temporary edit that is not committed, and see it fail naming that model.
  Record both in `record.md` (R15)
- [x] Write `.github/workflows/model-sources.yml`: on `schedule` once a week, at `17 6 * * 1`, and on
  `workflow_dispatch`; `permissions: contents: read`; one job on `ubuntu-latest` with `actions/checkout@v7`,
  `actions/setup-java@v6` with `distribution: temurin` and `java-version: 25`, as in `dependency-submission.yml`, and
  `./gradlew checkModelSources` (R16)
- [x] Commit point 3

### The documents

- [x] In `README.md` under "The speech model": the table gives 190 MB and 401 MB to download, and the comparison of the
  defaults "401 MB against 190 MB"; the list of what a download does says that the files come one by one, eight at a
  time, over HTTPS from the repository at the commit, each checked against its size and SHA-256, that the space the
  model takes is all it needs, and what a limit of Hugging Face does, with the pause, its countdown and the message after
  a second refusal; the list of the packages becomes one of the two repositories with their commits, their 377 files,
  their sizes and the resource file that lists the files, and says that the int8 model is the one with the fix of issue
  4086 of sherpa-onnx and that the old one is at commit `2a36069`; the paragraph on the publisher replacing the package
  says instead why a commit does not change and what still can, the repository deleted or its history rewritten. Keep
  every other sentence of the section (R13)
- [x] In `README.md`, replace the instructions for putting a speech model in place by hand, for Linux and macOS, with
  commands that read the list of the model from `core/src/main/resources/com/ritenoar/aloudrite/core/speech-model-files.tsv`
  of a clone of this repository, download each file with `curl` from its address at the commit, the space and `!` of
  the path encoded with `sed`, into a temporary folder named after the model, check every file with `sha256sum -c` on
  Linux and `shasum -a 256 -c` on macOS, and move the folder into the models folder. Run the Linux commands in the
  sandbox, with `XDG_DATA_HOME` set to a temporary folder and the path adjusted to it, and compare the folder they make
  with the list. Record it in `record.md` (R13)
- [x] In `README.md`, add to "The speech model" or under it how `./gradlew checkModelSources` is run and what it checks,
  that the workflow `model-sources.yml` runs it every week and can be started by hand, that it is run before a release
  is published, and that GitHub turns off a scheduled workflow of a public repository after 60 days without activity in
  the repository, which the Actions tab turns on again. Add under "Testing" what `RealModelDownloadTest` does and how it
  is run (R17)
- [x] In `THIRD-PARTY.md`, replace the row of the Kokoro package with one row per speech model, naming
  `https://huggingface.co/csukuangfj/kokoro-int8-multi-lang-v1_0` at `57adb37fc0e8d4bd430cfc1b8f8c2294945b9af2` and
  `https://huggingface.co/csukuangfj/kokoro-multi-lang-v1_0` at `f7b96bb6bef5c5da4d3aa4f4e0498fbbf62dc78b`, and make the
  paragraph below speak of the files of the model in place of the package that is unpacked. Read the `LICENSE` of each
  repository at its commit and keep the license column as it reads there. The row of the aligner model stays (R13)
- [x] Commit point 4

### The real download

- [x] Replace `RealModelPackageTest` with `RealModelDownloadTest`: it runs only with `ALOUDRITE_REAL_DOWNLOAD_FOLDER`
  set to a folder, and is skipped otherwise; it downloads both speech models from Hugging Face through `ModelInstaller`
  into `<folder>/aloudrite/<package name>`, prints how long each took, and asserts that each is in place and holds
  exactly the files of its list (R1, R2, R4, R5)
- [x] Run it in the sandbox with `ALOUDRITE_REAL_DOWNLOAD_FOLDER` set to a new temporary folder, copy
  `~/.local/share/aloudrite/english-mfa-v3.1.0` into `<folder>/aloudrite`, and run `./gradlew test` with
  `XDG_DATA_HOME` set to that folder, so that the tests with the real engine and `RealAlignerTest` run with the fixed
  int8 model and do not skip. Record the times, the requests left in the window if a 429 came, and the outcome in
  `record.md` (R1, AGENTS.md)
- [x] Commit point 5

## Verification

- [x] `./gradlew test` passes, without the network
- [x] `./gradlew test` with `XDG_DATA_HOME` set to the folder of `RealModelDownloadTest` passes with no test of the real
  engine skipped
- [x] `./gradlew buildPlugin` passes, within the limit of the archive size, and the archive holds
  `speech-model-files.tsv` and no class of `com.ritenoar.aloudrite.sources`
- [x] `./gradlew checkModelSources` succeeds in the sandbox
- [x] `npm run check-changes` passes on the committed work
- [ ] The developer downloads each speech model through the plugin, on Linux x64 and on macOS on Apple Silicon, from a
  machine on which it is not in place, and hears it read, and the result is recorded in `record.md` (R14). The unit
  touches the model download, so `AGENTS.md` asks for the check on both platforms

## Commit Points

- [x] 1: `feature-0041 Make the archive one kind of model package` for the first two tasks, as a refactoring on its
  own, then `feature-0041 Carry the lists of the files of the speech models` for the lists
- [x] 2: `feature-0041 Download the speech models file by file from Hugging Face`
- [x] 3: `feature-0041 Check every week that Hugging Face still serves the commits`
- [x] 4: `feature-0041 Describe the download from Hugging Face`
- [x] 5: `feature-0041 Download the real models in an opt-in test`, with the entries of `record.md`
