# Ready for the JetBrains Marketplace

## Approach

The plan was written against `feature-0040-ready-for-the-jetbrains-marketplace` at `9b2eff0`, which is the head of
`master` and holds units 0041, 0042 and 0043. The downloads therefore come from Hugging Face, as `README.md` says.

* **The version.** `version` in the `allprojects` block of `build.gradle.kts` is the one place it is set. The IntelliJ
  Platform Gradle Plugin patches it into the `plugin.xml` of the archive and it names the archive. It also names the jar
  of `core`, so `README.md` changes in four places: lines 551, 566, 575 and 711 at `9b2eff0` (R1, R6).
* **The icons.** Both are copies of the four shapes of `assets/icon.svg`, with its `viewBox` of `0 0 120 120` and its
  stroke width of 10, and with `width` and `height` of 40. The circle and the shoulders take the colour of the speaker,
  and the two waves stay `#E8B84A` (R2). A test compares the shapes with `assets/icon.svg`, so that the mark and the
  icons cannot drift apart unnoticed. The tests of the root project run in the project directory, as
  `ReadAlongScriptTest` relies on, so the test reads `assets/icon.svg` by its relative path.
* **The description and the vendor** are edited in `src/main/resources/META-INF/plugin.xml`. The description is HTML in
  the existing `CDATA` section, and takes its facts from `README.md`. The test checks for the facts that can drift: the
  sizes, the keys, the links, the platforms and the license (R3, R4).
* **The Plugin Verifier.** `intellijPlatform { pluginVerification { ides { recommended() } } }` is the configuration
  that R5 asks for. On 2026-11-23 `./gradlew printProductsReleases` gave `IU-263.8311.30` and `IU-2026.2.5`, and the
  verifier checked against `IU-263.8311.30` and `IU-262.12573.28`, the build of 2026.2.5.
* **What the verifier reports at `9b2eff0`.** The planner ran it on a throwaway copy of the tree. Both IDEs answer
  `Compatible`, but the task fails, because its default failure levels include internal API and the plugin uses
  `ActionToolbarImpl.setCustomButtonLook` in `ReadingButtonsPanel`. It also reports one use of API scheduled for removal,
  `SimpleListCellRenderer.create(String, Function)`, of which the other overload, `create(Customizer)`, is deprecated as
  well. The plan removes both, and the same trial with the two replacements below passed with both IDEs `Compatible`,
  no internal API and no API scheduled for removal, and with `ReadingButtonsTest` and `ReadingSpeedConfigurableTest`
  passing:
    * `ReadingButtonsPanel` no longer casts the toolbar to `ActionToolbarImpl`. It gives each `ActionButton` of the
      toolbar component the `ReadingButtonLook` through the public `ActionButton.setLook`, for the children already
      there and, through a `ContainerListener`, for each child the toolbar adds later, since the toolbar makes its buttons
      after the panel is built
    * `ReadingSpeedConfigurable` gets a private `textRenderer`, a subclass of `SimpleListCellRenderer` that overrides
      `customize`, in place of both calls of `create`
* **What stays, named in `record.md` as R5 asks.** These do not fail the task:
    * The 2 usages of `MarkdownHtmlPanel.getBrowserPipe` and `getVirtualFile`, experimental API. The highlights in the
      preview are built on them, the Markdown plugin offers no other way, and `README.md` already describes how the
      plugin behaves when they are missing
    * The 6 experimental usages on `ToolWindowFactory` (`getAnchor`, `getIcon`, `manage`) and the 4 deprecated ones
      (`isApplicable`, `isDoNotActivateOnStart`). `ReadingToolWindowFactory` overrides none of them in its source. They are
      the forwarders that the Kotlin compiler generates for the default methods of the interface
    * `runReadAction` in `ReadAloudAction` and `ComponentPanelBuilder` in `ReadingPanel`, deprecated. Both work in both
      IDEs, and replacing them changes code this unit has no other reason to touch. `commentLabel` depends on the
      non-wrapping comment for its height, as its comment records
* **Where the verifier can run.** It asks `plugins.jetbrains.com` for a dependency of the IDE, and that host is not
  reachable from the sandbox, where the plain run fails with an `SSLHandshakeException` before it gives a verdict. In the
  sandbox, run `./gradlew verifyPlugin` once to fetch the IDEs, which then fails on that host, and then
  `./gradlew verifyPlugin --offline`, which gives the verdict without the Marketplace. R5 is only met by a plain
  `./gradlew verifyPlugin` that succeeds, so where the host is out of reach that run is handed to the developer.
  Requirement 9, added after the review, admits that host and the one it redirects its files to, so the plain run
  now works in the sandbox as well.
* **The look in the plugin manager.** The sandbox has no `DISPLAY`, `Xvfb`, `xwd` or `xwininfo`, so the look that R8 and
  `AGENTS.md` ask for cannot be made there. Where the implementer has no display, it hands this to the developer on
  Linux x64, with the hover of the buttons on the tab, which the change of the button look touches. The unit touches no
  native code, audio output or model download, so `AGENTS.md` asks for no check on macOS, and it does not change the
  engine or the text handed to it, so the tests with the real engine need not run with the model.

## Files

- `build.gradle.kts`: the version 1.0.0 and the configuration of the Plugin Verifier
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingButtonsAction.kt`: the button look without `ActionToolbarImpl`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurable.kt`: `textRenderer`
- `src/main/resources/META-INF/plugin.xml`: the description and the vendor's `url`
- `src/main/resources/META-INF/pluginIcon.svg`: new, the icon for light themes
- `src/main/resources/META-INF/pluginIcon_dark.svg`: new, the icon for dark themes
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingButtonsTest.kt`: the look reaches the buttons of the toolbar
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurableTest.kt`: the text of the two lists
- `src/test/kotlin/com/ritenoar/aloudrite/MarketplaceListingTest.kt`: new, the icons, the description and the vendor
- `README.md`: the version, and how the Plugin Verifier is run
- `assets/README.md`: a row for each plugin icon
- `sandbox/compose.project.yml`: the two hosts of the JetBrains Marketplace
- `AGENTS.md`: asking before using the developer's display
- `changes/0040-ready-for-the-jetbrains-marketplace/record.md`: what the verifier reports and why each usage stays

## Tasks

- [x] In `ReadingButtonsTest`, add a test that an `ActionButton` added to `panel.toolbar.component` is given a
  `ReadingButtonLook`, with a subclass of `ActionButton` that records the look passed to `setLook`, and see it fail (R5)
- [x] In `ReadingButtonsPanel`, remove the use of `ActionToolbarImpl`, and set one `ReadingButtonLook` on the
  `ActionButton` children present and on each one added, through a `ContainerListener`. See the test pass (R5)
- [x] In `ReadingSpeedConfigurableTest`, add a test that the renderers of `speedChoice` and `voiceChoice` show
  `ReadingSpeed.display` and `Voice.displayName`, and an empty text for a null value. It passes on the old code and
  guards the replacement (R5)
- [x] In `ReadingSpeedConfigurable`, replace both calls of `SimpleListCellRenderer.create` by the private `textRenderer`,
  and see the tests pass (R5)
- [x] Add `pluginVerification { ides { recommended() } }` to the `intellijPlatform` block of `build.gradle.kts`, run the
  verifier as the approach says, and confirm that both IDEs answer `Compatible` with no internal API and no API scheduled
  for removal (R5)
- [x] Set `version = "1.0.0"` in `build.gradle.kts` (R1)
- [x] Add `MarketplaceListingTest`, written after the icons, the vendor and the description and so not seen to fail first (see `record.md`), with these checks (R2, R3, R4):
    - each icon is 40 by 40, and has the `viewBox`, the shapes and the stroke widths of `assets/icon.svg`
    - the speaker is `#4F6680` in the light icon and `#8FA6BF` in the dark one, and the waves `#E8B84A` in both
    - the `<vendor>` is `Ritenoar` with the `url` https://github.com/norareit/aloudrite-intellij
    - the description holds the link to https://agentrite.dev on "AgentRite", the link to the repository,
      `GPL-3.0-or-later`, `Linux x64`, `Apple Silicon`, `Kokoro`, `Hugging Face`, `190 MB`, `401 MB`, `48 MB`, `13` voices,
      and the eight keys of the table in `README.md`
- [x] Add `pluginIcon.svg` and `pluginIcon_dark.svg` to `src/main/resources/META-INF/` (R2)
- [x] Set the `url` of `<vendor>` in `plugin.xml` (R4)
- [x] Write the `<description>` in `plugin.xml` for a user of the IDE, in English, in HTML within the `CDATA` section,
  with each of the nine items of R3 in the order of the spec, and see `MarketplaceListingTest` pass (R3)
- [x] Compare each statement of the description with `README.md`, in particular that the models and the aligner model
  come from Hugging Face and that these downloads, asked about first, are the only network requests (R3)
- [x] In `README.md`, change the four mentions of `0.0.1` to `1.0.0`, and add to "Testing" how the Plugin Verifier is run
  with `./gradlew verifyPlugin`, that it checks against the IDEs the IntelliJ Platform Gradle Plugin recommends, and that
  it downloads them (R6)
- [x] In the table of the logo in `assets/README.md`, add a row for `pluginIcon.svg` and one for `pluginIcon_dark.svg`,
  each naming `src/main/resources/META-INF/` and its use on the Marketplace and in the plugin manager of the IDE, on light
  themes and on dark themes respectively (R7)
- [x] Log in `record.md` the verdict of the verifier with the IDEs it checked, and each usage that stays with its reason,
  as the approach lists them, from the report of the final build (R5)
- [x] Add `plugins.jetbrains.com` and `downloads.marketplace.jetbrains.com` to `ALLOWLIST` in
  `sandbox/compose.project.yml`, and run `./gradlew verifyPlugin --rerun` in the sandbox (R9)
- [x] Add to `AGENTS.md` that an agent asks before it uses the developer's display (R10)

## Verification

- [x] `./gradlew test` passes (R2, R3, R4, R5)
- [x] `./gradlew buildPlugin` succeeds within the limit of 30,000,000 bytes, and makes
  `build/distributions/aloudrite-intellij-1.0.0.zip` (R1, R8)
- [x] In `aloudrite-intellij/lib/aloudrite-intellij-1.0.0.jar` of the archive, `META-INF/plugin.xml` holds the
  `<version>` 1.0.0, the vendor's `url` and the description, and `META-INF/pluginIcon.svg` and
  `META-INF/pluginIcon_dark.svg` are there (R1, R2, R3, R4, R8)
- [x] `./gradlew verifyPlugin` succeeds with no compatibility problem, run as the approach says for where it runs (R5)
- [x] The plugin manager of the sandbox IDE, under Installed, shows the icon and the description, in a light and a dark
  theme, seen by the developer (R8)
- [ ] The buttons on the tab of a Markdown file show the hover in the pressed colour, seen by the developer (R5)
- [ ] `npm run check-changes` passes after the last commit

## Commit Points

- [x] `feature-0040 Plan the readiness for the JetBrains Marketplace`: this plan and its log entry
- [x] `feature-0040 Set the look of the reading buttons without internal API`: the first two tasks
- [x] `feature-0040 Render the speed and voice lists without deprecated API`: the next two tasks
- [x] `feature-0040 Configure the Plugin Verifier and set the version to 1.0.0`: the build and its check, the version,
  and in `README.md` its four mentions and the section on the Plugin Verifier
- [x] `feature-0040 Add the plugin icons`: the icons, the checks of them and the rows in `assets/README.md`
- [x] `feature-0040 Write the description and the vendor's address`: `plugin.xml` and the rest of `MarketplaceListingTest`
- [x] `feature-0040 Record what the Plugin Verifier reports`: the entry in `record.md`
- [x] `feature-0040 Give the stop of a reading to its own button in the description`: the correction of S1
- [x] `feature-0040 Record the decision on P1 and correct the record`: these documents
- [x] `feature-0040 Admit the JetBrains Marketplace to the sandbox`: the allowlist
- [x] `feature-0040 Record the look in the plugin manager`: these documents
- [x] `feature-0040 Ask before using the developer's display`: the rule in `AGENTS.md`
