# Everything to publish on the JetBrains Marketplace

## Approach

The plan was written against `feature-0044-everything-to-publish-on-the-jetbrains-marketplace` at `84cfa0e`. What it
says about signing, the source archives, the links and the Marketplace's tags was tried or read in the sandbox on
2026-11-23.

* **The metadata** is edited in `src/main/resources/META-INF/plugin.xml`, and `MarketplaceListingTest` holds what can
  drift. The description opens with `Reads Markdown files aloud in the IDE.` and goes on with the sentence on the
  Kokoro model. The sentence on the second button becomes: `While a reading runs or is paused, a second button beside
  the first, "Stop Reading", ends it.` The links to the two documents are
  `https://github.com/norareit/aloudrite-intellij/blob/master/EULA.md` and `.../blob/master/PRIVACY.md`, and they answer
  only once the unit is merged and pushed (R1 to R4).
* **Signing.** The `intellijPlatform` block gets the block below, and `tasks` gets the guard and the dependency. The
  trial of 2026-11-23 showed that Gradle logs the signer's command, with the key and its password, at `--info` and
  `--debug`. The signer takes the password only as an argument, so the guard refuses these two levels. `doFirst` runs
  only when the task is not skipped, so a build without the variables is untouched. Without the dependency, Gradle
  refuses to run `signPlugin verifyPluginSignature` together (R6, R7).

  ```kotlin
  signing {
      certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
      privateKey = providers.environmentVariable("PRIVATE_KEY")
      password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
  }
  ```

  ```kotlin
  signPlugin {
      // The signer takes the key and its password as arguments, which Gradle logs at these two levels
      doFirst {
          check(!logger.isInfoEnabled) { "signPlugin refuses --info and --debug, which would log the key and its password" }
      }
  }

  verifyPluginSignature {
      dependsOn(signPlugin)
  }
  ```

* **Where the secrets may lie.** `docs/publishing.md` names `.signing/` as the folder for them. `.gitignore` and
  `.dockerignore` both exclude it, since the image build takes the project directory as its context. The sandbox mounts
  the project directory, so the checklist has the human remove the folder after signing and not start the sandbox while
  it exists. JetBrains' commands also write a decrypted `private.pem`, needed for the certificate. The checklist deletes
  it right after, and `PRIVATE_KEY` is the encrypted key, which the trial signed with.
* **The source archives.** `scripts/third-party-sources.tsv` lists them, one per line as file name, address and SHA-256,
  separated by tabs, with no header. `scripts/prepare-third-party-sources.sh <list> <folder>` empties the folder,
  downloads each file with `curl -fsSL`, checks its SHA-256 with `sha256sum`, or `shasum -a 256` where that is missing,
  and writes `SHA256SUMS`. On a mismatch it removes the file, names it with both checksums and exits with an error. The
  checksums of the libraries that sherpa-onnx fetches agree with its `cmake/` at `v2.0.2`. The others were computed from
  the archives on 2026-11-23:

  | File | Address, under `https://github.com/` | SHA-256 |
  |---|---|---|
  | `sherpa-onnx-2.0.2.tar.gz` | `k2-fsa/sherpa-onnx/archive/refs/tags/v2.0.2.tar.gz` | `187627fa17a2a6d7024cd32f5bbcf8ff414151c14f739532940c24aa7cdc7d69` |
  | `piper-phonemize-1.6.1.tar.gz` | `csukuangfj/piper-phonemize/archive/refs/tags/v1.6.1.tar.gz` | `109101070ccefcb81a5193377be239c4d2d63f7b1aa716cd4aa52a00ce7bc958` |
  | `espeak-ng-ed530aa.zip` | `csukuangfj/espeak-ng/archive/ed530aa113046142eb5115cf2fc9157854d0ffe1.zip` | `e4e262cbe34f7fe21f91f1ba3397f2728e1f30eafbae7853f2b753a9ed13f0dd` |
  | `kaldi-native-fbank-1.23.1.tar.gz` | `csukuangfj/kaldi-native-fbank/archive/refs/tags/v1.23.1.tar.gz` | `c9722d55bf791d591baf6f79c8db8c66b7eeacf3bec59a4ac37864eacf2e8f37` |
  | `kaldi-native-io-1.23.2.tar.gz` | `csukuangfj/kaldi_native_io/archive/refs/tags/v1.23.2.tar.gz` | `8c0eadd3aa2d9cafe62e82c2e3075a57bc41cac7e83f9553671413f07b070426` |
  | `kaldi-decoder-0.4.0.tar.gz` | `k2-fsa/kaldi-decoder/archive/refs/tags/v0.4.0.tar.gz` | `078f2ebb9927c19445096549f880b4ac6db14b7eeb263062f50697873901c955` |
  | `kaldi-hmm-gmm-1.2.1.tar.gz` | `csukuangfj/kaldi-hmm-gmm/archive/refs/tags/v1.2.1.tar.gz` | `8344681ef464373bab54d548877ffa1cc82c16097e4f0975ce7b497c2e5b6e33` |
  | `kaldifst-1.9.0.tar.gz` | `k2-fsa/kaldifst/archive/refs/tags/v1.9.0.tar.gz` | `225d8377d1d262e3aedab23558fead106b4cbc83462fb29f1c5e7d9e3b0c14dd` |
  | `openfst-1.8.5-2026-07-09.tar.gz` | `csukuangfj/openfst/archive/refs/tags/v1.8.5-2026-07-09.tar.gz` | `2ff712a32952fcb01d351121a6bc8ccf4fdc6b2aa06ce8df2b3095dedd518c0e` |
  | `kissfft-febd4ca.zip` | `mborgerding/kissfft/archive/febd4caeed32e33ad8b2e0bb5ea77542c40f18ec.zip` | `497103e664168ebe39580b757adbe616f6cf85a16572af581ca7bc42d0ab13fd` |
  | `utf8proc-2.12.0.tar.gz` | `JuliaStrings/utf8proc/archive/refs/tags/v2.12.0.tar.gz` | `f564011d38b2888d583d510b08e69ffa15aa117155db1b9b49ef1dfe1fa25111` |
  | `simple-sentencepiece-0.7.tar.gz` | `pkufool/simple-sentencepiece/archive/refs/tags/v0.7.tar.gz` | `1748a822060a35baa9f6609f84efc8eb54dc0e74b9ece3d82367b7119fdc75af` |
  | `hclust-cpp-2026-02-25.tar.gz` | `csukuangfj/hclust-cpp/archive/refs/tags/2026-02-25.tar.gz` | `8f14e024c709d73afb40ae69cb22de4b73dba67cbce40f2e518813da8139ab56` |
  | `nlohmann-json-3.12.0.tar.gz` | `nlohmann/json/archive/refs/tags/v3.12.0.tar.gz` | `4b92eb0c06d10683f7447ce9406cb97cd4b453be18d7279320f7b2f025c10187` |
  | `commonmark-java-0.30.1.tar.gz` | `commonmark/commonmark-java/archive/refs/tags/commonmark-parent-0.30.1.tar.gz` | `b5fb067af22ae871102b1442c33ba29a2d40f2280bfe71202c5ac50ad363af40` |
  | `autolink-java-0.12.0.tar.gz` | `robinst/autolink-java/archive/refs/tags/autolink-0.12.0.tar.gz` | `0c7c85680ffc23bbbb55167385424c6157a55e766e1321566c46de7a5be56da3` |

  These 16 archives cover every library of the table in `THIRD-PARTY.md` but ONNX Runtime. The Java bindings and native
  libraries of sherpa-onnx and piper-phonemize are in their own archives, uni-algo is in that of piper-phonemize, and
  the extensions of commonmark-java are in its archive. None of the archives holds a `.gitmodules`.
* **ONNX Runtime** is not in the list, as R12 has the human add it. Its repository still answered 404 on 2026-11-23. The
  checklist downloads `https://github.com/microsoft/onnxruntime/archive/refs/tags/v1.28.2.tar.gz` to
  `build/third-party-sources/onnxruntime-1.28.2.tar.gz`, checks that `gzip -dc <file> | git get-tar-commit-id` starts
  with `33ca962`, and writes `SHA256SUMS` again over all the archives. It says that the release waits until this works.
* **The upload form's tag** is `Editor`, which `https://plugins.jetbrains.com/api/tags?family=intellij` lists. `Markdown`
  is on the Marketplace's whole list and not on that of the IntelliJ family, so the checklist does not use it.
* **What the sandbox cannot do.** It has no display, `Xvfb`, `xwd` or `xwininfo`, and does not reach `ritenoar.com` or
  `www.gnu.org`. The screenshots (R13) are made in a session on the host, after the developer has said that the window
  may come to the front. The links to these two hosts are checked there too, and the links to `EULA.md` and `PRIVACY.md`
  once the unit is merged and pushed (R16). The unit changes no native code, audio output, model download or text
  handed to the engine, so `AGENTS.md` asks for no check on macOS and no run with the real engine.

## Files

- `src/main/resources/META-INF/plugin.xml`: the description and the vendor
- `src/test/kotlin/com/ritenoar/aloudrite/MarketplaceListingTest.kt`: the new vendor, summary, button sentence and links
- `src/test/kotlin/com/ritenoar/aloudrite/PublishingDocumentsTest.kt`: new, the facts of the documents and the list
- `src/test/kotlin/com/ritenoar/aloudrite/ThirdPartySourcesScriptTest.kt`: new, the script on local files
- `build.gradle.kts`: signing, the guard, the dependency and the two files in the archive
- `.gitignore`, `.dockerignore`: `.signing/`
- `EULA.md`, `PRIVACY.md`: new
- `THIRD-PARTY.md`: the model card and the release that holds the source
- `scripts/third-party-sources.tsv`, `scripts/prepare-third-party-sources.sh`: new
- `docs/publishing.md`: new, the checklist
- `README.md`: the links, signing, and the four files in the archive
- `assets/marketplace/`: new, the screenshots
- `changes/0044-everything-to-publish-on-the-jetbrains-marketplace/record.md`: the outcomes

## Tasks

### The metadata

- [x] In `MarketplaceListingTest`, replace the vendor test by one for `<vendor url="https://ritenoar.com"
  email="nora+aloud@ritenoar.com">Ritenoar</vendor>`. Add a test that the description without its tags and with its
  white space collapsed starts with `Reads Markdown files aloud in the IDE.`, of at most 40 characters, and a test for
  the links to the issue tracker, `EULA.md` and `PRIVACY.md` beside that to the source. Change the test of the second
  button to the sentence of the approach and keep its check on pausing and resuming. See them fail (R1, R2, R3, R4)
- [x] Edit the `<vendor>` and the `<description>` in `plugin.xml` as the approach says, and see the tests pass
  (R1, R2, R3, R4)
- [x] Confirm that `version` in `build.gradle.kts` is still `1.0.0` (R5)

### Signing

- [x] Add the `signing` block, the guard and the dependency of the approach to `build.gradle.kts` (R6, R7)
- [x] Add `.signing/` to `.gitignore` and to `.dockerignore` (R7)
- [x] With a throwaway key and certificate made in a new folder under `/tmp`, confirm that `./gradlew signPlugin
  verifyPluginSignature --rerun` makes `build/distributions/aloudrite-intellij-1.0.0-signed.zip` and succeeds, that
  `signPlugin --rerun` fails with `--info` and with `--debug`, that a wrong password fails, that none of these outputs,
  `build/`, `.gradle/` or the Gradle daemon's log holds the password or the key, and that the signed archive holds no
  `.pem`, `.crt` or `.key`. Then delete the folder (R6, R7)
- [x] Without the variables, confirm that `./gradlew buildPlugin` succeeds with `signPlugin` skipped (R6)

### The legal documents

- [x] Add `PublishingDocumentsTest` with a check that `EULA.md` holds `GPL-3.0-or-later`, `Ritenoar CommV`, `Belgium`,
  `licensor`, `THIRD-PARTY.md`, `JetBrains` and `warranty`, and that `PRIVACY.md` holds `Ritenoar CommV`,
  `nora+aloud@ritenoar.com`, `Hugging Face`, `https://huggingface.co/privacy` and `IP address`. See it fail (R8, R9)
- [x] Write `EULA.md` with each statement of R8, adding no term that restricts the GPL, and with the sentence that the
  GPL decides a user's rights should the two differ (R8)
- [x] Write `PRIVACY.md` with each statement of R9, comparing it with `README.md` and the download code: what is stored
  is the models in the folder per user and the settings of the IDE, and a user avoids the download by declining or by
  putting the model in place by hand. See the test pass (R9)
- [x] Change `from(layout.projectDirectory.files("LICENSE", "THIRD-PARTY.md"))` in `prepareSandbox` to hold `EULA.md`
  and `PRIVACY.md` as well (R10)

### The third-party content

- [x] Read the model card at https://github.com/MontrealCorpusTools/mfa-models/blob/9cbc4c96af41b8a0acbd98eb75888839af9f1e17/acoustic/english/mfa/v3.1.0/README.md.
  If it does not allow the use, stop and report it to the human. Otherwise replace in `THIRD-PARTY.md` the sentence that
  the card was not read by what it states, with that commit (R11)
- [x] Add `ThirdPartySourcesScriptTest`, which runs the script on a temporary list with `file://` addresses: a matching
  file arrives and is in `SHA256SUMS`, and a file with another checksum makes it exit with an error, names the file and
  leaves it out. See it fail (R12)
- [x] Write `scripts/prepare-third-party-sources.sh`, executable, as the approach says, and see the test pass (R12)
- [x] Write `scripts/third-party-sources.tsv` with the 16 rows of the approach. Add to `PublishingDocumentsTest` that
  each row has a file name, an `https://github.com/` address and a SHA-256 of 64 hexadecimal digits, that the file names
  are unique, that no row is of ONNX Runtime, and that `THIRD-PARTY.md` names each file (R12)
- [x] Add a section to `THIRD-PARTY.md` that names the release `v1.0.0` of the plugin's repository as the place of the
  source of every library in the archive, lists each archive with the libraries it holds, and says that the source of
  ONNX Runtime 1.28.2 comes from https://github.com/microsoft/onnxruntime at the tag `v1.28.2`, commit `33ca962`, that
  the human adds it when making the release, and that the repository did not answer on 2026-11-23 (R12)
- [x] Run `scripts/prepare-third-party-sources.sh scripts/third-party-sources.tsv build/third-party-sources` and confirm
  that all 16 archives arrive and pass (R12)

### The publication

- [x] Add to `PublishingDocumentsTest` that `docs/publishing.md` holds `-days 3650`, `.signing/`, `CERTIFICATE_CHAIN`,
  `git tag`, `33ca962`, `verifyPluginSignature`, `marketplace@jetbrains.com`, `20.1(g)`, `nora+aloud@ritenoar.com`, the
  word `trader` and the word `hidden`, and that `README.md` links to the three documents. See it fail (R14, R15)
- [x] Write parts 1 to 3 of `docs/publishing.md`. Part 1: JetBrains' three `openssl` commands with `-days 3650`, the
  deletion of `private.pem`, the encrypted key, its password and the certificate in the password manager and a backup,
  `.signing/` as the approach says, and passing them with `read -rs` for the password and `"$(cat ...)"` for the two
  files, with no `--info` or `--debug`. Part 2: the tag `v1.0.0` at the commit the archive is built from, pushing it,
  the script, ONNX Runtime as the approach says, `SHA256SUMS`, `gh release create v1.0.0 --verify-tag` with the archives
  and the sums, before the upload, and that the release waits until the source of ONNX Runtime can be added. Part 3:
  `./gradlew test`, `./gradlew verifyPlugin`, `./gradlew signPlugin verifyPluginSignature`, and an installation of the
  signed archive from disk in a clean IDE (R14.1, R14.2, R14.3)
- [x] Write part 4 of `docs/publishing.md`: the message to marketplace@jetbrains.com alone, ready to send before the
  agreement is accepted, that lists piper-phonemize under GPL-3.0 and espeak-ng under GPL-3.0-or-later, and asks whether
  sections 9, 20.1(c) and 20.1(g) stand in the way of publishing and whether `EULA.md` with the GPL meets section 10
  (R14.4)
- [x] Write part 5: the check that mail to nora+aloud@ritenoar.com arrives, the account with that address on behalf of
  Ritenoar CommV as section 1 takes it, and, only once JetBrains has answered, reading and accepting the agreement, the
  vendor profile and the declaration of trader status, which is the human's own to determine (R14.5)
- [x] Write part 6: the upload form's values, namely free, `build/distributions/aloudrite-intellij-1.0.0-signed.zip`,
  GPL-3.0-or-later with the link to `LICENSE`, the link to `EULA.md`, the repository, its issue tracker,
  https://ritenoar.com, the tag `Editor` and the screenshots of `assets/marketplace/`, and that the hidden flag can be
  set on the first upload only and that unhiding cannot be undone (R14.6)
- [x] Write part 7: what to do on a refusal, that a version cannot be uploaded twice, when to write to
  marketplace@jetbrains.com, that the plugin follows changes to the guidelines within 30 days, that notices arrive at
  the account's address, and that the developer does not solicit reviews (R14.7)
- [x] In `README.md`, link the three documents, add how to sign the archive with a pointer to the checklist, and change
  the sentence under "License" that the archive holds both files to name all four. See the test pass (R10, R15)

### The listing

- [ ] In a session on the host, after the developer has said that the sandbox IDE may start and its window may come to
  the front, start `./gradlew runIde`, open the folder `samples/` as the project, and set the window to 1280 by 800 with
  `xdotool windowsize <id> 1280 800`, or `wmctrl -i -r <id> -e 0,-1,-1,1280,800` where `xdotool` is missing, with the id
  that `AGENTS.md` says how to find. Capture with `xwd -id <id>` and convert to PNG, into `assets/marketplace/`:
  `reading.png`, a paused reading of `markdown-features.md` with the sentence and the word highlighted in the editor
  and the preview side by side; `tool-window.png`, the tool window open during that reading; and `settings.png`, the page
  under Tools › AloudRite. Where the developer declines, the task stays open and the look is handed to them (R13)
- [ ] Confirm with `identify` that each image is 1280 by 800, and look at each for anything outside `samples/` and for
  personal data, such as a home directory or a recent project (R13)

## Verification

- [x] `./gradlew test` passes (R1, R2, R3, R4, R8, R9, R12, R14, R15, R17)
- [x] `./gradlew buildPlugin` succeeds within the limit of 30,000,000 bytes, and the archive holds `EULA.md`,
  `PRIVACY.md`, `LICENSE` and `THIRD-PARTY.md` in `aloudrite-intellij/` (R10, R17)
- [x] In `aloudrite-intellij/lib/aloudrite-intellij-1.0.0.jar` of the archive, `META-INF/plugin.xml` holds the
  `<version>` 1.0.0, the vendor and the first sentence (R1, R3, R5)
- [x] `./gradlew verifyPlugin` succeeds, both IDEs answer `Compatible`, and the report has no internal API and no API
  scheduled for removal (R17)
- [x] `git grep` finds no private key or certificate in the tree, and `sandbox/compose.project.yml` names none of the
  three variables (R7)
- [x] Every external link in the description, `EULA.md`, `PRIVACY.md` and `docs/publishing.md` is requested, and
  `record.md` gives those that do not answer with the reason. Only the release `v1.0.0`, the repository of ONNX Runtime
  and, until the merge and push, `EULA.md` and `PRIVACY.md` on `master` may be among them (R16)
- [x] On the host, the links to `ritenoar.com` and `agentrite.dev` answer. The links to `EULA.md` and `PRIVACY.md` are
  checked right after the merge and push, with the outcome as an amendment in `record.md` (R16)
- [ ] `npm run check-changes` passes after the last commit

## Commit Points

- [x] `feature-0044 Plan everything to publish on the Marketplace`: this plan and its log entry
- [x] `feature-0044 Write the summary, the vendor and the links of the description`: the metadata
- [x] `feature-0044 Sign the plugin from the environment`: the build, the two ignore files
- [x] `feature-0044 Add the EULA and the privacy notice`: the two documents, their checks and the archive
- [x] `feature-0044 Prepare the source archives of the third-party libraries`: the script, the list, the tests and
  `THIRD-PARTY.md`
- [x] `feature-0044 Write the checklist for the first publication`: `docs/publishing.md`, `README.md` and their checks
- [ ] `feature-0044 Add the screenshots for the listing`: `assets/marketplace/`
- [ ] `feature-0044 Record the checks`: `record.md`, with the verifier's verdict and the links
