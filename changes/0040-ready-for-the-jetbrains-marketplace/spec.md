# Ready for the JetBrains Marketplace

## Context

The plugin is to be published on the JetBrains Marketplace, where a new plugin is uploaded by hand once and then
reviewed by JetBrains against its approval guidelines, and where the Plugin Verifier checks it against the IDEs it
claims to support. The archive that `./gradlew buildPlugin` makes installs and works, but what the Marketplace and the
plugin manager of the IDE show of it was written for a plugin under development:

- Its version is `0.0.1`
- It has no plugin icon, `META-INF/pluginIcon.svg`, so the Marketplace and the plugin manager show a generic one
- Its `<description>` in `plugin.xml`, which the Marketplace shows as the page of the plugin, is two sentences: that it
  reads Markdown and agentic coding documents aloud, and that it is made with AgentRite (unit 0034). It says nothing of
  how it is used, of the two platforms speech works on, nor of the downloads it asks for
- Its `<vendor>` is the name "Ritenoar" alone
- The build has no configuration of the Plugin Verifier, so the project cannot run the check the Marketplace runs

The brand of the plugin is in `assets/`: the speaker mark in `icon.svg`, drawn with thicker lines for small sizes, and
its colours in `assets/README.md`.

The human asked for the archive to be made ready for the Marketplace: a plugin icon, a full description, the vendor's
address, the version 1.0.0 and the Plugin Verifier.

## Requirements

1. The version of the plugin is `1.0.0`, in the archive's name, `aloudrite-intellij-1.0.0.zip`, and in the
   `<version>` of the `plugin.xml` the archive holds.
2. The plugin has an icon for light themes, `META-INF/pluginIcon.svg`, and one for dark themes,
   `META-INF/pluginIcon_dark.svg`, each 40 by 40 pixels. Each is the speaker mark of `assets/icon.svg`, with its shapes
   and line widths, the speaker drawn in `#4F6680` in the light icon and in `#8FA6BF` in the dark one, and the sound
   waves in `#E8B84A` in both, as `assets/README.md` gives them for light and dark backgrounds.
3. The `<description>` of `plugin.xml` is HTML within a `CDATA` section, in English, and states, in agreement with
   `README.md`:
   1. What the plugin does: it reads Markdown files aloud in the IDE, with speech made on the machine itself by the
      Kokoro model, without the Markdown markers
   2. How a reading is started and controlled: the "Read Aloud" button in the editor tab row of a Markdown file, which
      reads from the start, from the caret or the selection, with pause, resume and stop
   3. That the sentence and the word being read are highlighted, in the editor and in the Markdown preview
   4. The tool window, and the settings under Settings › Tools › AloudRite: the reading speed, the speech model and
      the 13 voices
   5. The four shortcuts, with their keys on Linux and on macOS
   6. That speech works on Linux x64 and on macOS on Apple Silicon only, and that on another platform the plugin loads
      and says that speech is not supported there
   7. That a reading needs a speech model that is not in the archive: the first press asks whether to download it,
      names the two models with their sizes, says that they are downloaded from the releases of sherpa-onnx on GitHub
      together with the aligner model of 48 MB, and that these downloads, which the user is asked about first, are the
      only network requests the plugin makes
   8. That the plugin is made with AgentRite, with "AgentRite" linked to https://agentrite.dev, as unit 0034 made it
   9. A link to the source code, https://github.com/norareit/aloudrite-intellij, and its license, GPL-3.0-or-later
4. The `<vendor>` of `plugin.xml` is "Ritenoar" with its `url` set to https://github.com/norareit/aloudrite-intellij.
5. The build configures the Plugin Verifier with the IDEs the IntelliJ Platform Gradle Plugin recommends for the
   plugin's range of builds, and `./gradlew verifyPlugin` succeeds with no compatibility problem reported. Where it
   reports a problem that is not a compatibility problem, such as the use of an experimental or internal API, it is
   either removed or named in `record.md` with the reason it stays.
6. `README.md` agrees with the above: "Installing in an IDE" names `aloudrite-intellij-1.0.0.zip`, and "Testing" or
   "Building" says how the Plugin Verifier is run and that it downloads the IDEs it checks against.
7. `./gradlew buildPlugin` succeeds, its archive stays within the size limit of the build, and the `plugin.xml` of the
   archive holds the version, the vendor's address and the description of requirements 1, 3 and 4, and the archive
   holds both icons. A look at the plugin in the plugin manager of the sandbox IDE, under Installed, shows the icon and
   the description.

## Decisions

- **No change notes.** The human chose to publish the first version without them. A later release may add them.
- **The compatibility range stays open: `since-build` 262 and no `until-build`.** The plugin then installs in every
  later IDE without a new release. The alternative, an `until-build` of `262.*`, would make every new IDE version
  wait for a release of the plugin, which a plugin of one developer cannot promise to follow. The risk of the open range,
  that a later IDE breaks the plugin, is what the Plugin Verifier and the Marketplace's own runs of it are there to
  catch.
- **The Plugin Verifier belongs to this unit.** Its run is the check that the archive is fit to upload, the same check
  the Marketplace makes after the upload, so it is part of being ready and not a unit of its own.
- **The vendor's address is the repository on GitHub, not ritenoar.com.** The site of Ritenoar does not mention the
  plugin, and the plugin manager of the IDE shows the vendor's address as the place to learn more about the plugin.
  The repository has the README, the source code and the issues.
- **The plugin icon is the existing speaker mark, not a new design.** It is the mark of the README's lockup and of the
  brand, and its version for small sizes, `assets/icon.svg`, has the thicker lines a 40-pixel icon needs. The icon in
  the tool window and on the button stays the green speaker in the IDE's own icon style.
- **The description is written for a user of the IDE, not for a developer of the plugin.** It is shorter than
  `README.md` and leaves out how the plugin is built, tested and measured. Where the two describe the same thing, they
  agree.

## Non-goals

- Uploading the plugin, the account and vendor profile on the Marketplace, its token and the fields of the upload
  form, such as the category, the license and the screenshots. Only the human can do these, on the Marketplace's site
- Change notes, for this release or as a mechanism for later ones
- Signing the plugin with a certificate of the vendor. The Marketplace signs every plugin it hosts
- Speech on other platforms than Linux x64 and macOS on Apple Silicon
- A change to the plugin's name, its id, or the icons of the button and the tool window

## Beyond the request

1. Configure `publishPlugin` with the Marketplace token read from an environment variable, so that a later release is
   uploaded with `./gradlew publishPlugin`. It costs a few lines of the build and a sentence in `README.md`. Without
   it each release is uploaded by hand on the Marketplace's site, as the first one has to be in any case.
2. Add the vendor's e-mail address to `<vendor>`. It costs one attribute. Without it, the plugin manager shows no
   address to write to, and users reach the developer through the issues of the repository.
3. A workflow of GitHub Actions that runs `./gradlew verifyPlugin` on every push to `master`. It costs a workflow file
   and the time on the runner to download the IDEs, several GB per run. Without it the verifier runs when the
   developer runs it, and the Marketplace runs it on each upload.
4. Add the two plugin icons to the table of `assets/README.md`. It costs two rows. Without it the icons are found in
   `src/main/resources/META-INF/` only.
