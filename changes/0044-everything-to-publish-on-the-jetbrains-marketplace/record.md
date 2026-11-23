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

## Amendments
