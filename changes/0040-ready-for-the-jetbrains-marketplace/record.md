---
kind: feature
status: in-progress
timestamp: 2026-11-22T11:37:58Z
---

# 0040 Ready for the JetBrains Marketplace

## Log

- 2026-11-22T11:37:58Z [human] asked for the plugin to be made ready for the JetBrains Marketplace: a plugin icon
  drafted by the agent, a full description, the vendor's address, the version 1.0.0 and the Plugin Verifier, and no
  change notes
- 2026-11-22T11:37:58Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `1bfdf79`, read `process.md`, the standards and `roles/specifier.md` of the installed kit. Made
  the unit a feature, as the description, the icon, the vendor's address and the range of builds each need a decision.
  Numbered it 0040, after 0039. Found that the built `plugin.xml` has `since-build` 262 and no `until-build`, that the
  archive of 0.0.1 is 21,945,574 bytes, and that `META-INF/` holds no plugin icon. Drew the draft of the two icons from
  `assets/icon.svg` with the colours of `assets/README.md` and showed it to the human at 40, 80 and 160 pixels. The
  status is `draft`
- 2026-11-22T12:07:54Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, changed item 7 of requirement 3 to
  take the source of the downloads from `README.md`, as unit 0041, to be built first, moves the speech models from the
  GitHub release of sherpa-onnx to Hugging Face. The status stays `draft`
- 2026-11-22T12:13:04Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, took item 4 of "Beyond the
  request" into the unit at the human's choice, as requirement 7: the table of `assets/README.md` lists the two plugin
  icons. The build's requirement is now 8. The status stays `draft`
- 2026-11-22T12:17:21Z [human] approved the spec. The status is `approved`
- 2026-11-23T00:12:00Z [Claude Code, anthropic/claude-opus-5-5] as the planner, wrote `plan.md` against `9b2eff0`. Ran
  the Plugin Verifier on a throwaway copy of the tree: both recommended IDEs answer `Compatible`, and the task fails on
  the internal API `ActionToolbarImpl`, which the plan removes, as it does the one use of API scheduled for removal. A
  trial of both replacements passed the verifier with `--offline` and their tests. `plugins.jetbrains.com` is not
  reachable from the sandbox, so the plain `./gradlew verifyPlugin` that R5 needs has not passed there, and the sandbox
  has no display for the look that R8 needs. The status stays `approved`
- 2026-11-23T01:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, began the work on `65d36f2`. The status is
  `in-progress`
- 2026-11-23T01:30:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built the plan's tasks. Of the new
  tests, `ReadingButtonsTest` was seen to fail on the old code before the change to `ReadingButtonsPanel`; the test of the
  empty text of the lists passes on the old code, as the plan says, and `MarketplaceListingTest` was written after the
  icons, the vendor and the description, so it was not seen to fail first. The test of the look checks the button the
  toolbar gets added later, and not the buttons present when the panel is built, as `ActionButton` has no getter for its
  look. `ReadingSpeedConfigurable` got the generic function `textRenderer` in place of a subclass, with the same
  behaviour. The description says that the aligner model is downloaded with the first speech model, as `README.md`
  says of the download. The status stays `in-progress`
- 2026-11-23T01:31:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew test buildPlugin
  --offline` on the tree before the commits of these entries: it succeeded, the archive is 22,034,103 bytes and
  `META-INF/plugin.xml` in its jar holds the version 1.0.0 and the vendor's `url`, and both icons are there. The plain
  `./gradlew verifyPlugin` fails here on an `SSLHandshakeException` against `plugins.jetbrains.com`, which the sandbox
  cannot reach, so R5 is not yet met by the plain run, which is left to the developer. `./gradlew verifyPlugin --offline`
  checked against `IU-263.8311.30` and `IU-262.12573.28`: both `Compatible`, no internal API and none scheduled for
  removal, with 6 usages of deprecated API and 8 of experimental API. These stay, as the plan names them: the
  experimental `MarkdownHtmlPanel.getBrowserPipe` and `getVirtualFile` (the highlights of the preview are built on
  them, and the Markdown plugin offers no other way); the experimental and deprecated usages on `ToolWindowFactory`
  (`getAnchor`, `getIcon`, `manage`, `isApplicable`, `isDoNotActivateOnStart`), which the report places in
  `ReadingToolWindowFactory`, including two "overridden" for the two deprecated ones, as the Kotlin compiler generates
  the forwarders of the interface's default methods, not named in the source; `runReadAction` in `ReadAloudAction` and
  `ComponentPanelBuilder` in `ReadingPanel`, which work in both IDEs and whose replacement this unit has no other
  reason to make. Not checked: the look of the icon and the description in the plugin manager, and the hover of the
  buttons, as the sandbox has no display; both are left to the developer on Linux x64

## Amendments
