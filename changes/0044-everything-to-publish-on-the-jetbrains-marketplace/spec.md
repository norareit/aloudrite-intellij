# Everything to publish on the JetBrains Marketplace

## Context

Unit 0040 made the archive ready for the Marketplace as far as its icon, description, version and the Plugin Verifier
go. It left out the vendor's e-mail address and signing, and took the repository as the vendor's address. A reading of
JetBrains' pages on 2026-11-23, the Approval Guidelines v1.3 among them, shows that more is or may be asked of a first
publication:

- Guideline 1.3 asks for a valid, functional vendor website and e-mail address. The `<vendor>` of `plugin.xml` has no
  e-mail address, and its `url` is the repository and not a website of the vendor
- Guideline 1.4 asks that the first 40 characters of the description are an English summary. They are now
  `Reads Markdown files aloud inside the ID`, cut off within a word
- JetBrains' guide on publishing says to make sure the plugin is signed. Its page on signing does not say that an
  unsigned upload is refused, and says that an IDE warns when it installs a plugin its author did not sign
- Guideline 3.2 asks for a Developer EULA with any plugin, and the page on EULAs says what it must state. The Marketplace
  Agreement names "the Developer EULA or an Open-Source License" as what governs a developer's plugin
- Guideline 3.4 asks for a privacy policy where personal data is collected. The plugin collects none, but a model
  download shows the user's IP address to Hugging Face
- Guideline 3.5 asks the developer to declare whether the account is a trader under the consumer law of the European
  Economic Area, which only the human can answer
- Guideline 1.7 asks that external links are reachable, and guideline 1.8 that all content may be distributed
- Screenshots are recommended, at 1200 by 760 pixels or more in one aspect ratio
- The archive ships GPL binaries of piper-phonemize and espeak-ng, so whoever distributes it has to keep their
  corresponding source available. The repository has no tag or release that says which source is that of version 1.0.0

The Developer Agreement, version 3.2 of 2026-08-24, was read from the text the human supplied, as its page builds its
text in the browser. Accepting it, or uploading a first plugin, is a binding contract. For this plugin it adds:

- Section 1: accepting with an e-mail address of a legal entity counts as acting for that entity
- Section 3.1 makes the declaration of trader or non-trader a condition of the upload, and section 3.2 the choice of a
  free plugin
- Section 10: the developer provides a Developer EULA that names the developer as the licensor and says that JetBrains
  is no party to it and not liable to users. The Standard EULA of its appendix may not be used unmodified, and its
  ban on modifying and decompiling contradicts the GPL. The appendix also allows a similar license agreement
- Section 16.5: the developer tells users who it is, which data is processed and why, what goes to third parties and
  how to object
- Section 20.1: the developer identifies third-party content, includes its attributions and makes its corresponding
  source available, and warrants that JetBrains' use of the content will not subject JetBrains to any open-source terms
- Sections 9 and 21: the developer grants JetBrains broad, sublicensable rights to the plugin and indemnifies JetBrains
  against claims about it, with no stated limit

These last two points are the one real obstacle found. The archive holds GPL code of others, piper-phonemize and
espeak-ng, for which the developer can grant no more than the GPL does and which forbids sublicensing, and JetBrains
distributing it is itself bound by the GPL. Read strictly, the warranty and the license of the agreement cannot be
given for that code. JetBrains' pages do not address this.

The pages this unit relies on, as read on 2026-11-23:

- [Approval Guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html),
  v1.3
- [Uploading a new plugin](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html)
- [Publishing a plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [Plugin signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [Best practices for the listing](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html)
- [Developer EULA](https://plugins.jetbrains.com/docs/marketplace/eula.html)
- [Legal agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html)
- [Hidden plugins](https://plugins.jetbrains.com/docs/marketplace/hidden-plugin.html)
- [Plugin updates](https://plugins.jetbrains.com/docs/marketplace/plugin-updates.html)
- [Plugin configuration file](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)
- [Developer Agreement](https://plugins.jetbrains.com/legal/developer-agreement), version 3.2
- [Marketplace Agreement](https://www.jetbrains.com/legal/docs/plugins_site/plugin_marketplace/), version 2.0
- [Content Moderation Policy](https://www.jetbrains.com/legal/docs/terms/marketplace-content-moderation/), version 2.0
- [IntelliJ Platform Gradle Plugin, extension](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html)

Not read: the [brand guidelines](https://www.jetbrains.com/company/brand/), whose page builds its text in the browser.

The request is for everything that is or might be necessary, so that the first upload is not turned down for something
that could have been prepared. Where JetBrains' pages leave it open, this unit takes the safer reading.

## Requirements

### The plugin's metadata

1. The text of the `<description>`, without its HTML, starts with a complete English sentence of at most 40 characters
   that summarizes the plugin: `Reads Markdown files aloud in the IDE.`
2. The sentence on the second button of the description reads correctly in English and still says that "Read Aloud"
   pauses and resumes and that a second button, "Stop Reading", ends the reading.
3. The `<vendor>` of `plugin.xml` is "Ritenoar", with its `url` set to https://ritenoar.com and its `email` set to
   nora+aloud@ritenoar.com.
4. The description links to the issue tracker, https://github.com/norareit/aloudrite-intellij/issues, to the terms of
   requirement 8 and to the privacy notice of requirement 9. It keeps the link to the source code.
5. The version stays `1.0.0`, as no version has been uploaded.

### Signing

6. `./gradlew signPlugin` makes `build/distributions/aloudrite-intellij-1.0.0-signed.zip` when the certificate chain,
   the private key and its password are given through the environment variables `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and
   `PRIVATE_KEY_PASSWORD`, and `./gradlew verifyPluginSignature` succeeds on that archive. Without the variables,
   `./gradlew buildPlugin` and the tests work as before.
7. No private key, password or certificate is in the repository, in the archive, in the sandbox's configuration or in
   a log the build writes. `.gitignore` ignores the folder `docs/publishing.md` names for them, should the human keep
   them in the project directory.

### Legal documents

8. `EULA.md` at the root of the repository is the Developer EULA. It states that the plugin is licensed under
   GPL-3.0-or-later, that Ritenoar CommV, a company in Belgium, publishes it and is the licensor, that third-party software stays under the licenses `THIRD-PARTY.md` names, that JetBrains and its affiliates and
   resellers are no party to the license and are not responsible or liable to users, and that the plugin comes without
   warranty as the GPL says. It adds no restriction to the GPL, and says that the GPL decides a user's rights in the
   software should the two differ.
9. `PRIVACY.md` at the root of the repository says, in agreement with the code and `README.md`, that the plugin
   collects no personal, statistical or telemetric data and sends nothing to its developer, that its only network
   requests are the model downloads the user agrees to, that Hugging Face and the content network it redirects to
   receive what any download shows, such as the IP address, under their own privacy policy, which it links, and what is
   stored on the machine. As section 16.5 of the Developer Agreement asks, it names Ritenoar CommV as the publisher, gives
   the address of requirement 3, the purpose of the download, and how a user avoids it: by declining, or by putting the model in place by
   hand as `README.md` describes.
10. The plugin archive holds `EULA.md` and `PRIVACY.md` beside `LICENSE` and `THIRD-PARTY.md`.

### Third-party content

11. `THIRD-PARTY.md` states the license of the aligner model as read in the upstream model card of the Montreal Forced
    Aligner project, which it says was not read so far. If that card does not allow the use, the implementer stops and
    reports it to the human.
12. `THIRD-PARTY.md` names the release `v1.0.0` of the plugin's repository as the place that holds the source of every
    third-party library in the archive, at the version or commit `THIRD-PARTY.md` lists for it, among them piper-phonemize
    at the tag `v1.6.1` and espeak-ng at commit `ed530aa` of its fork.

### The listing

13. `assets/marketplace/` holds at least three screenshots of 1280 by 800 pixels, of the IDE's window and nothing else,
    showing: a reading with the sentence and the word highlighted in the editor and the preview, the tool window, and
    the settings under Tools › AloudRite. They show only files of `samples/` and no personal data. They are made as
    `AGENTS.md` says about the developer's display.

### The publication

14. `docs/publishing.md` is a checklist that the human can follow from start to finish for the first publication, and
    holds:
    1. Making the key and the self-signed certificate with the commands of JetBrains' page on signing, with a validity
       of 3650 days, that the encrypted key, its password and the certificate are kept in the developer's password
       manager and never in the repository, that a backup is needed, and how to pass them to `signPlugin`
    2. Making and pushing the tag `v1.0.0` at the commit the archive is built from, and the release `v1.0.0` with the
       source archives of requirement 12 and their SHA-256 checksums, before the upload
    3. The final checks: the tests, `./gradlew verifyPlugin`, `./gradlew signPlugin verifyPluginSignature`, and an
       installation of the signed archive from disk in a clean IDE
    4. A message, ready to send to marketplace@jetbrains.com before the agreement is accepted, that says the plugin bundles third-party GPL-3.0 code, lists it, and asks whether sections 9, 20.1(c) and 20.1(g)
       of the Developer Agreement stand in the way of publishing it, and whether `EULA.md` with the GPL meets section 10
    5. A check that mail to nora+aloud@ritenoar.com arrives, then the account with that address, on behalf of Ritenoar
       CommV as section 1 of the agreement takes it, and, only once JetBrains has answered that message, reading and
       accepting the Developer Agreement, the vendor profile of Ritenoar, and the declaration of trader status, which it
       says is the human's own to determine
    6. The value of each field of the upload form that is known: the free plugin, the file, the license, the Developer
       EULA, the source code, the issue tracker, the website, the tags and the screenshots, and that the hidden flag can
       be set on the first upload only and that unhiding cannot be undone
    7. What to do if JetBrains refuses the upload, that a version cannot be uploaded twice, when to write to
       marketplace@jetbrains.com, and what publishing keeps asking: that the plugin follows changes to the guidelines
       within 30 days, that notices arrive at the account's address, and that the developer does not solicit reviews
15. `README.md` links to `docs/publishing.md`, `EULA.md` and `PRIVACY.md`, and says how to sign the archive.
16. Every external link in the description, `EULA.md`, `PRIVACY.md` and `docs/publishing.md` answers, apart from the
    release `v1.0.0`, which the checklist has the human make first.
17. `./gradlew test`, `./gradlew buildPlugin` and `./gradlew verifyPlugin` succeed, the archive stays within the size
    limit of the build, and both IDEs answer `Compatible` with no internal API and no API scheduled for removal.

## Decisions

- **The unit takes the safer reading wherever JetBrains' pages leave a question open.** A refusal costs days of review
  each time, and the cost of the extra documents and of signing is small.
- **The vendor's address becomes https://ritenoar.com, which changes a decision of unit 0040.** That unit chose the
  repository because the plugin manager shows the vendor's address as the place to learn more. Guideline 1.3 asks for
  the vendor's website, and the configuration reference calls it the vendor's homepage. The description and the upload
  form keep the links to the repository, so a reader still finds it.
- **The plugin is signed, which changes a non-goal of unit 0040.** That unit relied on the Marketplace's own signature.
  JetBrains' guide says to sign, the Marketplace signs a file a second time after its author, and an IDE warns about a
  plugin its author did not sign.
- **The certificate is self-signed and valid for 3650 days.** JetBrains' page names a self-signed certificate as an
  option, under custom repositories, and does not say whether the Marketplace accepts or refuses one. A certificate of
  a certificate authority costs money and identity checks for no stated benefit. The long validity spares a yearly
  renewal, and the page sets no limit.
- **The signing secrets are guarded against accidental disclosure only.** The adversary is a mistake: a commit, a
  log, an archive or a sandbox that gets the key. A compromised machine of the developer is not defended against. The
  sandbox never needs the key, since signing is run on the host.
- **The EULA is a notice beside the GPL, and adds no terms that restrict it.** GPL-3.0 forbids further restrictions, so
  the Standard EULA, which bans modifying and decompiling, cannot be used. The notice gives what section 10 of the
  Developer Agreement asks for. Whether it meets that section is put to JetBrains in the message of requirement 14.
- **JetBrains is asked about the GPL code before the agreement is accepted.** Accepting is binding and carries an
  indemnity with no stated limit, and the warranty of section 20.1(g) cannot strictly be given for the GPL code of
  others. Asking costs an e-mail and some days. The alternatives were to accept and rely on the fact that nothing in
  JetBrains' pages forbids GPL plugins, which leaves the risk with the developer, or to replace piper-phonemize and
  espeak-ng, which is a change of the speech engine for a problem that may not exist.
- **The source of all bundled libraries is kept in a release of the plugin's own repository.** Section 20.1(g) makes
  the source part of the developer's warranty, `README.md` says the plugin as a whole is offered under the GPL, and the
  GPL's corresponding source of a whole may reach the libraries it needs. The upstream repositories are outside the
  vendor's control. This costs some hundreds of MB of release assets.
- **Third-party names stay in the listing only where they state a fact.** Section 1(a) of the Content Moderation
  Policy restricts references to third-party marks without authorization. The description names Kokoro, Hugging Face,
  Linux and macOS because the disclosures of what the plugin runs on and where it downloads from need them. It shows no
  third-party logo.
- **Accepting the agreement and the upload wait for JetBrains' answer to the message of requirement 14.** The rest of
  this unit is built meanwhile. Should the answer refuse the GPL code or ask for something else, the human decides what
  follows, and a change it requires is made in this unit or a new one.
- **Change notes stay out, as the human chose in unit 0040.** The guidelines do not require them.
- **The first upload stays manual.** JetBrains requires it, so the unit prepares it and does not automate it.

## Non-goals

- Doing what only the human can do: creating the account, accepting the agreement, determining and declaring trader
  status, generating and holding the real key, pushing the tag, creating the release, and the upload itself
- Legal advice, or an audit of the third-party licenses beyond requirements 11 and 12
- A certificate from a certificate authority
- Signing through a key management service such as AWS KMS, which needs a signing tool of its own
- Speech on other platforms, and any change to how the plugin reads
- A change of the plugin's name, its id or its icons

## Beyond the request

1. Configure `publishPlugin` with a Marketplace token from the environment. It costs a few lines of the build and a
   section in the checklist. Without it, each later release is uploaded by hand with "Upload Update".
2. Change notes for 1.0.0. It costs one element in `plugin.xml`. Without it, the "What's New" tab of the plugin stays
   empty, which the guidelines allow.
3. A demo video of under 5 minutes. It costs a recording and a place to host it. Without it, the listing has its
   screenshots only, and JetBrains names the video as optional.
4. A review of the Developer Agreement, `EULA.md`, `PRIVACY.md` and the trader status by a lawyer. It costs a fee.
   Without it, these rest on the reading in this spec and on JetBrains' answer, while the indemnity is the developer's.
5. A workflow of GitHub Actions that runs the Plugin Verifier on each push. It costs several GB of downloads per run.
   Without it, the verifier runs when the developer runs it and on each upload.
