# Publishing AloudRite on the JetBrains Marketplace

This is the checklist for the first publication of the plugin, written for the developer. It holds what only the
developer can do. What the repository can prepare was prepared in unit 0044 under `changes/`, and the reasons are in
its `spec.md`. The pages of JetBrains it follows were read on 2026-11-23, and they change, so read the page of a step
again where it matters.

The order is: make the key (part 1), prepare the release of the source (part 2), check the archive (part 3), write to
JetBrains and wait for the answer (part 4), make the account and accept the agreement (part 5), upload (part 6). Part
7 is for after the upload.

Nothing in this file is done by the build. The first upload is manual, as JetBrains requires.

## 1. The key and the certificate

The plugin is signed with a self-signed certificate that is valid for 3650 days, so that no yearly renewal is needed.
The signing is run on the host, never in the sandbox.

1. Stay in the root of the checkout for every command of this part, and make a folder for the files there. `.signing/`
   is ignored by Git and by the image build, and the sandbox mounts the project directory, so remove the folder after
   signing and do not start the sandbox while it exists. Every path below is relative to the root of the checkout. To
   keep the files elsewhere, replace `.signing` by that folder in each command:

   ```sh
   mkdir -p .signing
   ```

2. Make the encrypted key, then the decrypted one that the certificate needs, then the certificate, with the three
   commands of the page [Plugin signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html), at a
   validity of 3650 days. Each of the first two asks for a password. Choose a long one and keep it:

   ```sh
   openssl genpkey -aes-256-cbc -algorithm RSA -out .signing/private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
   openssl rsa -in .signing/private_encrypted.pem -out .signing/private.pem
   openssl req -key .signing/private.pem -new -x509 -days 3650 -out .signing/chain.crt
   ```

3. Delete the decrypted key at once. It is not used again, as `PRIVATE_KEY` is the encrypted one:

   ```sh
   rm .signing/private.pem
   ```

4. Keep `private_encrypted.pem`, its password and `chain.crt` in your password manager, and nowhere in the repository.
   Make a backup of the three. A lost key means that a later version cannot be signed with the same certificate, which
   needs a decision of JetBrains and may need a new plugin.

5. Pass them to Gradle through the environment, in the shell you sign from, with no `--info` or `--debug`. The build
   refuses these two levels, because Gradle would write the key and its password to the log:

   ```sh
   export CERTIFICATE_CHAIN="$(cat .signing/chain.crt)"
   export PRIVATE_KEY="$(cat .signing/private_encrypted.pem)"
   read -rs PRIVATE_KEY_PASSWORD && export PRIVATE_KEY_PASSWORD
   ./gradlew signPlugin
   ```

   `build/distributions/aloudrite-intellij-1.0.0-signed.zip` is the signed archive. Close the shell afterwards, or
   `unset CERTIFICATE_CHAIN PRIVATE_KEY PRIVATE_KEY_PASSWORD`, and remove the `.signing/` folder.

## 2. The tag and the release of the source

The archive ships GPL code of others, so the source of every library in it has to be available. `THIRD-PARTY.md` names
the release `v1.0.0` of the repository as the place. It is made before the upload.

1. Make the tag at the commit the archive is built from, and push it:

   ```sh
   git tag -a v1.0.0 -m "AloudRite 1.0.0" <commit>
   git push origin v1.0.0
   ```

2. Download the 16 source archives and check them against the list. The script empties its folder first, and stops
   with an error naming a file whose SHA-256 differs from `scripts/third-party-sources.tsv`:

   ```sh
   scripts/prepare-third-party-sources.sh scripts/third-party-sources.tsv build/third-party-sources
   ```

3. Add the source of ONNX Runtime 1.28.2 from its own repository. The repository did not answer on 2026-11-23, so
   **the release waits until this works**, and if it does not return, you decide what follows:

   ```sh
   curl -fsSL -o build/third-party-sources/onnxruntime-1.28.2.tar.gz \
     https://github.com/microsoft/onnxruntime/archive/refs/tags/v1.28.2.tar.gz
   gzip -dc build/third-party-sources/onnxruntime-1.28.2.tar.gz | git get-tar-commit-id
   ```

   The commit id that the second command prints starts with `33ca962`. If it does not, do not use the file.

4. Write the checksums again over all the archives, so that `SHA256SUMS` holds ONNX Runtime too, with
   `shasum -a 256` where `sha256sum` is missing:

   ```sh
   (cd build/third-party-sources && rm -f SHA256SUMS && sha256sum *.tar.gz *.zip > SHA256SUMS)
   ```

5. Make the release with the archives and the sums, and no upload before it exists:

   ```sh
   gh release create v1.0.0 --verify-tag --title "AloudRite 1.0.0" \
     --notes "AloudRite 1.0.0. The assets are the source of the third-party libraries in the plugin archive, see THIRD-PARTY.md." \
     build/third-party-sources/*
   ```

   The size is some hundreds of MB, which GitHub allows for a release.

## 3. The final checks

On the host, on the tagged commit:

1. `./gradlew test`
2. `./gradlew verifyPlugin`: both IDEs of the verifier answer `Compatible`, with no internal API and no API scheduled
   for removal
3. `./gradlew signPlugin verifyPluginSignature`, with the variables of part 1 and the default level of logging
4. Install `aloudrite-intellij-1.0.0-signed.zip` from disk in a clean IDE under Settings › Plugins › the gear ›
   Install Plugin from Disk. The IDE must not warn that the plugin is unsigned. Read a Markdown file with it
5. Look at the screenshots of `assets/marketplace/`: each is 1280 by 800 pixels, shows only the IDE's window, and shows
   no personal data

## 4. The message to JetBrains

The agreement is binding and carries an indemnity with no stated limit, and the archive holds GPL code that the agreement
may not strictly allow. Send this to marketplace@jetbrains.com **alone** and wait for the answer before part 5:

> Subject: GPL-3.0 third-party code in a plugin, before accepting the Developer Agreement
>
> Hello,
>
> I am about to publish a free plugin, AloudRite, which reads Markdown files aloud in IntelliJ Platform IDEs. The plugin
> is licensed under GPL-3.0-or-later and is published by Ritenoar CommV, a company in Belgium. The source is at
> https://github.com/norareit/aloudrite-intellij.
>
> The plugin archive bundles third-party code that is under the GPL: piper-phonemize 1.6.1 (GPL-3.0), and espeak-ng,
> built into the native library of piper-phonemize (GPL-3.0-or-later). The other bundled libraries are under Apache-2.0,
> MIT or BSD licenses. `THIRD-PARTY.md` in the repository lists all of them, and the source of every one is kept in the
> release `v1.0.0` of the repository.
>
> Before I accept the Developer Agreement, version 3.2, I would like to know:
>
> 1. Do sections 9, 20.1(c) and 20.1(g) of the Developer Agreement stand in the way of publishing a plugin that
>    bundles GPL-3.0 code of others? I can grant no more in that code than the GPL does, and the GPL forbids
>    sublicensing it.
> 2. Does `EULA.md` of the repository, a notice beside the GPL that names Ritenoar CommV as the licensor and says that
>    JetBrains is no party to it, meet section 10, given that the Standard EULA's ban on modifying and decompiling
>    contradicts the GPL?
>
> Kind regards,
> Ritenoar CommV, nora+aloud@ritenoar.com

If JetBrains refuses the GPL code or asks for something else, stop and decide what follows. A change that it requires is
made in a unit of its own.

## 5. The account and the agreement

1. Check that mail to nora+aloud@ritenoar.com arrives: send one to it, and see it in the inbox that you read.
2. Make the account on https://plugins.jetbrains.com with that address, on behalf of Ritenoar CommV. Section 1 of the
   agreement takes an address of a company as acting for it.
3. Only once JetBrains has answered the message of part 4: read the Developer Agreement and accept it.
4. Make the vendor profile: the name Ritenoar, the website https://ritenoar.com and the address of step 1.
5. Declare whether the account is a trader under the consumer law of the European Economic Area. Section 3.1 makes the
   declaration a condition of the upload. Whether Ritenoar CommV is a trader is for you to determine, and this
   checklist does not say.

## 6. The upload

Upload at https://plugins.jetbrains.com/plugin/add with these values:

| Field | Value |
|---|---|
| Plugin pricing | Free, as section 3.2 of the agreement chooses it |
| File | `build/distributions/aloudrite-intellij-1.0.0-signed.zip` |
| License | GPL-3.0-or-later, with the link https://github.com/norareit/aloudrite-intellij/blob/master/LICENSE |
| Developer EULA | https://github.com/norareit/aloudrite-intellij/blob/master/EULA.md |
| Source code | https://github.com/norareit/aloudrite-intellij |
| Issue tracker | https://github.com/norareit/aloudrite-intellij/issues |
| Website | https://ritenoar.com |
| Tags | `Editor` |
| Screenshots | the files of `assets/marketplace/` |

The form may ask for the privacy policy as well: it is https://github.com/norareit/aloudrite-intellij/blob/master/PRIVACY.md.

The form's hidden flag keeps the plugin out of the listing until you show it. It can be set on the first upload only,
and unhiding cannot be undone.

## 7. After the upload

* JetBrains reviews the first upload, and it takes days. A version cannot be uploaded twice, so a change to the archive
  after a refusal needs a new version number in `build.gradle.kts`
* If the upload is refused, read the reason in the mail to the account's address. Fix what it names in a unit of its
  own, and upload the next version
* Write to marketplace@jetbrains.com when the reason is unclear, or the review has taken longer than the page on
  [plugin updates](https://plugins.jetbrains.com/docs/marketplace/plugin-updates.html) says
* Publishing asks that the plugin follows changes to the Approval Guidelines within 30 days, that the notices that
  JetBrains sends to the account's address are read, and that you do not solicit reviews
