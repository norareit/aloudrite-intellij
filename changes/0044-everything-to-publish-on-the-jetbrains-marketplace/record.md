---
kind: feature
status: approved
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

## Amendments
