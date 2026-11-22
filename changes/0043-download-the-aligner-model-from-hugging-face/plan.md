# Download the aligner model from Hugging Face

## Approach

The plan was written against `feature-0043-download-the-aligner-model-from-hugging-face` at `1b2ad31`, which holds unit
0041 as merged into `master`.

Today `ALIGNER_MODEL` in `AlignerModel.kt` carries an `ArchivePackage` whose address is the asset of the GitHub release
`fa-models`, and `ModelInstaller` downloads, checks and unpacks it on its archive path. That path follows redirects
only to HTTPS, which the address on Hugging Face needs. It answers with a 302 to `us.aws.cdn.hf.co`. The check of the
sources, `ModelSourcesCheck.kt` in the source set `sourcesCheck` of `core`, reads `loadSpeechModelFiles()`. For each model
it compares two answers with a `RepositoryFiles`: the revision of the commit, and the headers of the largest file at
that commit.

* **One description of where the archive is.** `AlignerModel.kt` gets `ALIGNER_MODEL_SOURCE`, a `RepositoryFiles` of
  `csukuangfj/fa-models` at `39914ed8514a568e2d24a5e785608e1abb429133` with one `RepositoryFile`,
  `english-mfa-v3.1.0.tar.bz2`, of 47,567,475 bytes and SHA-256
  `86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da`. The `ArchivePackage` of `ALIGNER_MODEL` takes its
  address from `ALIGNER_MODEL_SOURCE.fileAddress(...)` of that file, and its size and SHA-256 from the same file. It
  keeps its unpacked size of 51,887,069 bytes. So the download and the check read the commit, the size and the SHA-256
  from one place and cannot disagree (R1, R2). The archive is still downloaded as an `ArchivePackage` and unpacked as
  today. `ALIGNER_MODEL_SOURCE` is only what the check compares with, and nothing installs it. A type of its own for "one
  file at a commit" would repeat `revisionAddress`, `fileAddress` and the comparison that `RepositoryFiles` already
  serves, so the plan does not add one. The path of the archive needs no encoding, so the address is exactly the one in
  R1, and the test of R3 asserts it as a literal string
* **The check takes the aligner model as one more model.** `ModelSourcesCheck.kt` gets a function `modelSources()` that
  returns `loadSpeechModelFiles()` followed by `ALIGNER_MODEL.packageName` to `ALIGNER_MODEL_SOURCE`. `main` iterates
  over it in place of `loadSpeechModelFiles()`. `problemsOf` does not change: for the aligner model the largest file is
  the archive, its revision is that of `csukuangfj/fa-models`, and a problem starts with `english-mfa-v3.1.0, <address>:`
  as one of a speech model starts with its package name (R2). The closing line becomes "Hugging Face still serves the
  commits of the 3 models the plugin downloads.", with the number from `modelSources()`. The description of the task in
  `core/build.gradle.kts` and the comment above the source set say "the models" in place of "the speech models".
  `.github/workflows/model-sources.yml` does not change, as R2 says
* On 2026-11-22 the planner saw both answers the check reads for the aligner model. The revision
  `https://huggingface.co/api/models/csukuangfj/fa-models/revision/39914ed8514a568e2d24a5e785608e1abb429133` answered 200
  with that commit as its `sha`. The HEAD of the address of R1 answered 302 with `x-repo-commit` that commit,
  `x-linked-size: 47567475` and `x-linked-etag` the SHA-256 above in quotes. `problemsOf` accepts a status of 200 to 399
  for the HEAD, so the check holds for the aligner model without a change to the comparison
* What the user sees changes only where the address is shown: the questions before a download name the new address,
  as they name the address of the package today. The question, the sizes, the failure and "Try Again" stay as they are
  (R1)

The unit touches the model download, so `AGENTS.md` asks for the developer's check on Linux x64 and on macOS on Apple
Silicon. It does not change the engine, sherpa-onnx or the text handed to the engine. The archive is the same, byte for
byte, so the tests with the real engine need not run with a new model.

## Files

- `core/src/main/kotlin/com/ritenoar/aloudrite/core/AlignerModel.kt`: `ALIGNER_MODEL_SOURCE`, and the package of
  `ALIGNER_MODEL` built from it
- `core/src/test/kotlin/com/ritenoar/aloudrite/core/AlignerModelTest.kt`: the new address and the source
- `core/src/sourcesCheck/kotlin/com/ritenoar/aloudrite/sources/ModelSourcesCheck.kt`: `modelSources()`, `main` over it,
  and the closing line
- `core/src/test/kotlin/com/ritenoar/aloudrite/core/ModelSourcesCheckTest.kt`: the tests of the aligner model in the check
- `core/build.gradle.kts`: the description of `checkModelSources` and the comment above `sourcesCheck`
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadMessagesTest.kt`: the new address in the expected texts of the
  questions
- `README.md`: "The speech model", the paragraph on `checkModelSources`; "The aligner model", its source and the
  instructions to put it in place by hand; and the paragraph of the measurement of the highlight that names the address
- `THIRD-PARTY.md`: the row of the aligner model and the paragraph on it

## Tasks

### The address of the download

- [x] In `AlignerModelTest`, make `theModelIsTheEnglishPackageOfTheFaModelsRelease` assert the `ArchivePackage` with
  the address
  `https://huggingface.co/csukuangfj/fa-models/resolve/39914ed8514a568e2d24a5e785608e1abb429133/english-mfa-v3.1.0.tar.bz2`,
  written out as a literal, 47,567,475 bytes, 51,887,069 bytes unpacked and SHA-256
  `86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da`. Rename the test to say that the model is the
  English archive of `csukuangfj/fa-models` at the commit. Add a test that `ALIGNER_MODEL_SOURCE` is `csukuangfj/fa-models` at
  `39914ed8514a568e2d24a5e785608e1abb429133` with the single file `english-mfa-v3.1.0.tar.bz2` of that size and SHA-256,
  and that its host is `https://huggingface.co` (R1, R3)
- [x] In `ModelDownloadMessagesTest`, replace the address of the release in the expected texts of
  `theQuestionsBeforeADownloadStateTheSizeOfTheAlignerModelWhereItIsNotOnTheMachine` and
  `theQuestionOfTheAlignerModelAloneStatesItsSizesAndItsFolder` with the address of R1. Change nothing else of the texts
  (R1)
- [x] Add `ALIGNER_MODEL_SOURCE` to `AlignerModel.kt`, with a comment that the download and the check of the sources
  both read it, and build the `ArchivePackage` of `ALIGNER_MODEL` from its one file as the approach says, until both
  tests pass (R1)
- [x] `./gradlew test` passes. Commit point 1

### The check of the sources

- [x] In `ModelSourcesCheckTest`, add tests for the aligner model with stand-in answers built for
  `ALIGNER_MODEL_SOURCE`. The first gives a revision of 200 whose `sha` is its commit, and a HEAD of 302 with that commit,
  `x-linked-size: 47567475` and the SHA-256 in quotes as `x-linked-etag`; `problemsOf("english-mfa-v3.1.0", ...)` then
  gives no problem. In the second, a revision of 404 gives one problem, exactly `english-mfa-v3.1.0,
  https://huggingface.co/api/models/csukuangfj/fa-models/revision/39914ed8514a568e2d24a5e785608e1abb429133: the commit
  39914ed8514a568e2d24a5e785608e1abb429133 is missing from csukuangfj/fa-models`. The third shows that the largest file of
  `ALIGNER_MODEL_SOURCE` is the archive, so that the HEAD goes to the address of R1. The fourth shows that `modelSources()` holds
  the two speech models of `loadSpeechModelFiles()`, in their order, and then `english-mfa-v3.1.0` with
  `ALIGNER_MODEL_SOURCE` (R2, R3)
- [x] Write `modelSources()` in `ModelSourcesCheck.kt` and make `main` check every model of it, with the closing line of
  the approach. Update the description of `checkModelSources` and the comment above `sourcesCheck` in
  `core/build.gradle.kts`, until the tests pass (R2)
- [x] Run `./gradlew checkModelSources` in the sandbox and see it succeed with the closing line naming 3 models. Then
  change the commit of `ALIGNER_MODEL_SOURCE` with a temporary edit that is not committed, run it again, and see it
  fail with a line that names `english-mfa-v3.1.0`, the address and what did not match. Undo the edit. Record both runs
  in `record.md` (R2, R5)
- [x] `./gradlew test` passes. Commit point 2

### The documents

- [x] In `README.md` under "The aligner model", make the address of the download the one of R1. Say that it is the
  archive `english-mfa-v3.1.0.tar.bz2` of the repository `csukuangfj/fa-models` on Hugging Face at commit
  `39914ed8514a568e2d24a5e785608e1abb429133`, which the workflow `upload-fa-models.yaml` of sherpa-onnx fills with the
  archives it also uploads to the release `fa-models`. Say that the plugin downloads it from the commit because an asset
  of a release can be replaced under the same name. Keep the size, the SHA-256, the folder and every other sentence of
  the section (R4)
- [x] In `README.md`, replace the address in the `curl -fLO` line of the Linux and of the macOS instructions for putting
  the aligner model in place by hand with the address of R1. Keep the SHA-256 and every other line. Run the Linux commands
  in the sandbox, with `~/.local/share/aloudrite` replaced by a new temporary folder, and see `sha256sum -c` print `OK`
  and the six files that `AlignerModel.isInPlace` looks for in `english-mfa-v3.1.0` in that folder. Record it in
  `record.md` (R4)
- [x] In `README.md`, make the paragraph of the measurement of the highlight, which names the address it unpacks the
  aligner model from, name the address of R1 (R4)
- [x] In `README.md` under "The speech model", make the paragraph on `./gradlew checkModelSources` say that it checks the
  commit of the aligner model in `csukuangfj/fa-models` as well, and the headers of its archive, as it does for the
  largest file of a speech model. Keep every other sentence of the paragraph (R2, R4)
- [x] In `THIRD-PARTY.md`, make the row of the aligner model name
  `https://huggingface.co/csukuangfj/fa-models` at commit `39914ed8514a568e2d24a5e785608e1abb429133`, as the rows of the
  speech models name theirs, and keep its license column `CC BY 4.0`. In the paragraph below, say that sherpa-onnx
  publishes the re-packaged model in the repository `csukuangfj/fa-models` on Hugging Face, in place of its release
  `fa-models`. Keep the attribution, the sentences on how the license was read and every other sentence (R4)
- [x] Check that `grep -rn "releases/download/fa-models" README.md THIRD-PARTY.md core src` finds nothing, and that the
  frozen records in `changes/` are left as they are (R4)
- [x] Commit point 3

### The developer's check

- [ ] The developer, on Linux x64 and on macOS on Apple Silicon, deletes the folder `english-mfa-v3.1.0` with the
  commands of `README.md`, keeping a speech model in place. The developer downloads the aligner model through the plugin,
  with "Download" in the row "Word timing (english-mfa-v3.1.0)" of the page "AloudRite" under Settings › Tools, and sees
  the word highlighted as it is heard in a reading. The developer's result is recorded in `record.md` (R5)

## Verification

- [x] `./gradlew test` passes, without the network
- [x] `./gradlew buildPlugin` passes, and the plugin archive still holds no class of `com.ritenoar.aloudrite.sources`
- [x] `./gradlew checkModelSources` succeeds in the sandbox (R5)
- [x] `npm run check-changes` passes on the committed work

## Commit Points

- [x] 1: `feature-0043 Download the aligner model from its commit on Hugging Face`
- [x] 2: `feature-0043 Check the commit of the aligner model every week`, with the entries of `record.md`
- [x] 3: `feature-0043 Describe the download of the aligner model from Hugging Face`, with the entry of `record.md`
