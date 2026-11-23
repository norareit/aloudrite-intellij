---
kind: feature
status: in-progress
timestamp: 2026-11-23T11:53:27Z
---

# 0044 Everything to publish on the JetBrains Marketplace

## Log

- 2026-11-23T11:53:27Z [human] asked for a unit that adds everything that is or might be necessary to publish the plugin on the
  JetBrains Marketplace
- 2026-11-23T11:53:27Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `master` at `fbfd50e`, read `process.md`, the standards and `roles/specifier.md`. Made the unit a feature, as it
  needs decisions on signing, the legal documents and the vendor, and numbered it 0044, after 0043. Read JetBrains'
  Approval Guidelines v1.3 and its pages on publishing, signing, EULAs, the configuration file and the listing. The
  text of the Developer Agreement could not be retrieved. Found that the first 40 characters of the description end
  within a word, that the repository has no tag, and that no plugin on the Marketplace is found by the name AloudRite.
  Two values are for the human to give before approving: the public e-mail address of the vendor (R3) and the licensor
  (R8). The status is `draft`
- 2026-11-23T12:23:15Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, added to the context of the spec the
  JetBrains pages it relies on, and why the Developer Agreement was not read: its page builds its text in the browser.
  The status stays `draft`
- 2026-11-23T12:27:01Z [human] supplied the text of the Developer Agreement, version 3.2
- 2026-11-23T12:27:01Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, checked the spec against it and read the
  Marketplace Agreement and the Content Moderation Policy; the page of the brand guidelines gave only its title. Found
  that sections 9 and 20.1 cannot strictly be met for the GPL code of others in the archive, and added a message to
  JetBrains before the agreement is accepted. Sharpened the EULA and the privacy notice to sections 10 and 16.5, and
  took the source of all bundled libraries into requirement 12, which was item 4 of "Beyond the request", so that
  list is renumbered. The status stays `draft`
- 2026-11-23T12:47:05Z [human] decided that Ritenoar CommV publishes the plugin, that its public and account address is
  nora+aloud@ritenoar.com, and that accepting the Developer Agreement and the upload wait for JetBrains' answer to the
  message about the GPL code
- 2026-11-23T12:47:05Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, worked these decisions into requirements 3, 8, 9
  and 14 and the decisions of the spec. The choice among the items of "Beyond the request" and the approval are still
  the human's. The status stays `draft`
- 2026-11-23T13:47:11Z [human] decided that the message to JetBrains goes to marketplace@jetbrains.com alone
- 2026-11-23T13:47:11Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, worked this into requirement 14. The
  status stays `draft`
- 2026-11-23T17:45:57Z [human] decided that the signing key is an encrypted key file kept in their password manager,
  and not a key in AWS KMS
- 2026-11-23T17:45:57Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, worked this into requirement 14 and the
  non-goals of the spec. The status stays `draft`
- 2026-11-23T17:59:10Z [human] approved the spec. The status is `approved`
- 2026-11-23T18:04:21Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on the unit's branch at `65abbfa`, wrote no
  plan, because the spec needs the human and the specifier first. The status stays `approved`.
  R12 no longer fits what can be fetched: it needs the source of ONNX Runtime 1.28.2, and
  https://github.com/microsoft/onnxruntime, which `THIRD-PARTY.md` names, answered 404 from the sandbox on 2026-11-23,
  to the page, the tag and the archive, while other repositories of that account answered 200. Git asks for credentials
  there. The fork `csukuangfj/onnxruntime` has no tag `v1.28.2`, and `csukuangfj/onnxruntime-libs` holds binaries and
  no source. Whether the repository is gone or only out of reach from the sandbox was not established.
  Found for the plan that follows, in a scratch copy with a throwaway key:
  the `signing` block of the IntelliJ Platform Gradle Plugin 2.19.0 with the three variables makes the signed archive,
  and `signPlugin` is skipped without them. With `--info`, Gradle logs the command of the signer with the key and its
  password, which R7 forbids. A `doFirst` on `signPlugin` that fails when `logger.isInfoEnabled` stopped this for
  `--info` and `--debug`, and a run with a wrong password logged neither. `verifyPluginSignature` needs
  `dependsOn(signPlugin)`, as Gradle otherwise refuses to run the two together.
  For R11, the upstream model card of `english_mfa` v3.1.0, at commit `9cbc4c9` of `MontrealCorpusTools/mfa-models`,
  states CC BY 4.0. For R12, `cmake/` of sherpa-onnx at `v2.0.2` gives the address and SHA-256 of the source archive of
  the libraries it fetches, and `codeload.github.com` is outside the sandbox's allowlist. For R16, https://ritenoar.com
  is outside it too. For R13, the sandbox has no display, `Xvfb`, `xwd` or `xwininfo`
- 2026-11-23T19:00:08Z [human] decided that the source of ONNX Runtime comes from its own repository with no other
  source named, confirmed that Ritenoar CommV may license the plugin, allowed the sandbox to reach
  `codeload.github.com`, and took none of the items of "Beyond the request"
- 2026-11-23T19:00:08Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, on the unit's branch, worked these
  decisions into requirements 8, 12, 14 and 16 and the decisions of the spec, and added the host to the allowlist. The
  status stays `approved`
- 2026-11-23T19:04:41Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on the unit's branch at `84cfa0e`, wrote the
  plan. Downloaded the 16 source archives the unit prepares and recorded their SHA-256 in it. The repository of ONNX
  Runtime still answered 404. The screenshots and the links to `ritenoar.com` and `www.gnu.org` are for a session on the
  host, and the links to `EULA.md` and `PRIVACY.md` answer once the unit is merged. The status stays `approved`
- 2026-11-23T19:05:37Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch at `e8fcfb5`, began the work. No review findings exist yet for this round. The status is `in-progress`
- 2026-11-23T19:12:53Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, built the
  plan except the screenshots, the links of the host and the last commits. Verified on this tree: `./gradlew test` and
  `./gradlew buildPlugin` pass (the archive is 22,037,030 bytes, under 30,000,000, and holds `EULA.md`, `PRIVACY.md`,
  `LICENSE` and `THIRD-PARTY.md`); `./gradlew verifyPlugin` reports `Compatible` for IU-262.12573.28 and IU-263.8311.30,
  with 6 usages of deprecated API and 8 of experimental API, and no internal API or API scheduled for removal was in
  its output lines that were read; the `plugin.xml` in the jar holds version 1.0.0, the vendor and the first sentence;
  `git grep` finds no key or certificate and `sandbox/compose.project.yml` names none of the three variables. The trial
  of signing with a throwaway key (since deleted) made the signed archive and `verifyPluginSignature` passed with
  `signPlugin`; `--info` and `--debug` were refused; a wrong password failed; the signed archive holds no `.pem`,
  `.crt` or `.key`; no password or key was in `build/`, `.gradle/` or the log of the daemon of that run. Found:
  `verifyPluginSignature` writes the public certificate chain to `build/tmp/verifyPluginSignature/`, which is ignored
  and holds no secret. `signPlugin` without the variables is `SKIPPED`. The model card of the aligner, read at commit
  `9cbc4c9` of its repository, states CC BY 4.0, so R11 holds. The script downloaded all 16 archives, which passed
  their checksums, 53 MB in 17 s. Links: all answer except `https://ritenoar.com` and `https://agentrite.dev`, which the
  sandbox does not reach (000), the repository of ONNX Runtime (404), and `EULA.md` and `PRIVACY.md` on `master`, which
  answer after the merge. Not done: the
  screenshots of R13 (no display in the sandbox, and the developer's say is needed first) and the links to be checked
  on the host. The status stays `in-progress`
- 2026-11-23T19:19:37Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a full review of unit 0044 at
  `23878b546fd63f795e5d7985bcdb13186e2c4f98`, against merge base `65abbfa7c34b9c1ff6bf0d51ce180294901a9ca7`
  with `origin/master`. Returned S1 and S2, saved in
  `.reviews/0044-everything-to-publish-on-the-jetbrains-marketplace/2026-11-23-round-1.md`. Independently reran the tests
  and Plugin Verifier, checked the built archive, exercised signing with disposable in-memory material and its logging
  guard, and downloaded and checked all 16 source archives. The change check passed on the reviewed commit. Could not
  verify the host screenshots, the links outside the sandbox's allowlist, the public EULA and privacy links before
  merge, the unavailable ONNX Runtime source, a clean-IDE installation, or the implementer's earlier scan of build and
  daemon logs for its disposable key. The status stays `in-progress`
- 2026-11-23T19:20:43Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, took the
  findings of `.reviews/0044-everything-to-publish-on-the-jetbrains-marketplace/2026-11-23-round-1.md`. S2 is accepted
  by the definition of the orchestrator, and not as a decision of the human: part 1 of `docs/publishing.md` no longer
  changes directory, and every command and path is relative to the root of the checkout. Verified by running the
  commands of parts 1.1 to 1.5 from the root with a disposable 2048-bit key, which was then deleted: `signPlugin
  verifyPluginSignature` made the signed archive with no error, and `PublishingDocumentsTest` passes. S1 is accepted as
  true and stays open: the screenshots of R13 need the developer's say on starting the sandbox IDE on their display,
  which the sandbox cannot give. No decline is proposed. The status stays `in-progress`
- 2026-11-23T19:22:58Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of unit 0044 at
  `d3229639d0edc684051137f3c20cdd4c8d1205f3`, against `23878b546fd63f795e5d7985bcdb13186e2c4f98`, the commit
  named by the first review. Returned the still-open S1 and no new findings; the verification of S1 and S2 is in
  `.reviews/0044-everything-to-publish-on-the-jetbrains-marketplace/2026-11-23-round-2.md`. Independently ran
  `./gradlew test --rerun`, `git diff --check 23878b5..HEAD` and the change check, which passed. Inspected the complete
  corrected signing command sequence but did not replay the implementer's interactive disposable-key run. Could not
  verify the host screenshots or the host/post-merge links; the unchanged human publication checks remain outstanding.
  The status stays `in-progress`
- 2026-11-23T19:23:31Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, read
  `.reviews/0044-everything-to-publish-on-the-jetbrains-marketplace/2026-11-23-round-2.md`. It returns S2 as corrected
  and only S1 as open, with no new finding. Nothing was changed in this round. S1 is accepted as true, by the definition
  of the orchestrator and not as a decision of the human, and stays open until the screenshots of R13 are made with the
  developer's say on their display. No decline is proposed. The status stays `in-progress`
- 2026-11-23T19:23:52Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, performed a delta review of unit 0044 at
  `115b0ed35131d6525f9d6401a625395a2e3019e6`, against `d3229639d0edc684051137f3c20cdd4c8d1205f3`, the commit
  named by the second review. Returned the still-open S1 and no new findings, saved in
  `.reviews/0044-everything-to-publish-on-the-jetbrains-marketplace/2026-11-23-round-3.md`. Independently inspected the
  record-only diff and the assets directory; the diff whitespace check and change check passed. Did not rerun the
  unchanged implementation's suites. Could not verify the host screenshots or the host/post-merge links; the human
  publication checks remain outstanding. The status stays `in-progress`
- 2026-11-23T19:41:13Z [human] decided that the links to `EULA.md` and `PRIVACY.md` on `master` are checked right after
  the merge and push and do not hold up `implemented`, and that the screenshots are made on the host
- 2026-11-23T19:41:13Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, on the host, worked this into R16 and
  the plan. Requested every external link of the description, `EULA.md`, `PRIVACY.md` and `docs/publishing.md`: all
  answer except the source archive of ONNX Runtime and `EULA.md` and `PRIVACY.md` on `master` (404), as R16 allows.
  Corrected the timestamps of the implementer's four entries to the times of their commits, which changed the hashes
  this record names. S1 stays open. The status stays `in-progress`
- 2026-11-23T19:49:12Z [human] said that the sandbox IDE may start on their display and its window may come to the front
- 2026-11-23T19:49:12Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, on the host, made the three
  screenshots of R13 in `2264763`, of the sandbox IDE on a copy of `samples/` outside the repository, so that no branch
  shows. Each is 1280 by 800 and shows no home directory, recent project or personal data. `./gradlew test` passes. This
  answers S1, which no review has confirmed yet. The status stays `in-progress`
- 2026-11-23T22:40:13Z [OpenCode, openai/gpt-6.2-sol] as the reviewer, performed a delta review of unit 0044 at
  `ca6a9084f0bc18215194171dbe9545dfd456b1bf` against `115b0ed35131d6525f9d6401a625395a2e3019e6`. Verified the
  screenshots for S1 and returned C1 in
  `.reviews/0044-everything-to-publish-on-the-jetbrains-marketplace/2026-11-23-round-4.md`. Reran `./gradlew test --rerun`,
  the change check, the diff checks and the host link checks. Could not verify the deferred public EULA and privacy
  links or the unavailable ONNX Runtime archive. Did not replay screenshot capture or the release installation.

## Amendments
