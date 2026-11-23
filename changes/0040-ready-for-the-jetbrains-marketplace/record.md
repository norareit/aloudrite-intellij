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
- 2026-11-23T00:11:21Z [Claude Code, anthropic/claude-opus-5-5] as the planner, wrote `plan.md` against `9b2eff0`. Ran
  the Plugin Verifier on a throwaway copy of the tree: both recommended IDEs answer `Compatible`, and the task fails on
  the internal API `ActionToolbarImpl`, which the plan removes, as it does the one use of API scheduled for removal. A
  trial of both replacements passed the verifier with `--offline` and their tests. `plugins.jetbrains.com` is not
  reachable from the sandbox, so the plain `./gradlew verifyPlugin` that R5 needs has not passed there, and the sandbox
  has no display for the look that R8 needs. The status stays `approved`
- 2026-11-23T00:11:34Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, began the work on `65d36f2`. The status is
  `in-progress`
- 2026-11-23T00:17:45Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built the plan's tasks. Of the new
  tests, `ReadingButtonsTest` was seen to fail on the old code before the change to `ReadingButtonsPanel`; the test of the
  empty text of the lists passes on the old code, as the plan says, and `MarketplaceListingTest` was written after the
  icons, the vendor and the description, so it was not seen to fail first. The test of the look checks the button the
  toolbar gets added later, and not the buttons present when the panel is built, as `ActionButton` has no getter for its
  look. `ReadingSpeedConfigurable` got the generic function `textRenderer` in place of a subclass, with the same
  behaviour. The description says that the aligner model is downloaded with the first speech model, as `README.md`
  says of the download. The status stays `in-progress`
- 2026-11-23T00:17:45Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew test buildPlugin
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
- 2026-11-23T00:23:07Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0040 at
  `eb144ac48b6348f84c615912d26193b75548096e`, against merge base `9b2eff0` with `origin/master`. Returned findings S1
  and P1 in `.reviews/0040-ready-for-the-jetbrains-marketplace/review-20261123T002307Z.md`. Independently reran the tests
  successfully and checked the build and listing; the forced offline verifier reports both recommended IDEs Compatible.
  Could not verify the online verdict because the forced online run fails on TLS, or the plugin-manager appearance and
  toolbar hover because no display is available. The status stays `in-progress`
- 2026-11-23T00:24:58Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, accepted the finding S1 of the review
  `review-20261123T002307Z.md` by the definition of the orchestrator: the description said that the "Read Aloud" button
  also stops the reading, and `README.md` gives the stop to a second button. The description now says that the button
  pauses and resumes, and that a second button, "Stop Reading", ends the reading, and `MarketplaceListingTest` has a test
  for the two statements, which was written after the text and not seen to fail first. `./gradlew test buildPlugin
  --offline` succeeded after the change, with 7 tests in `MarketplaceListingTest` and none failed or skipped
- 2026-11-23T00:24:58Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, accepted the part of the finding P1
  that asks for an accurate plan: the task of `MarketplaceListingTest` in `plan.md` now says that it was written after the
  icons, the vendor and the description and was not seen to fail first. Proposed, not decided: that the human accepts a
  narrow exception for the listing tests written after their code, whose assertions pass and were rerun by the review,
  since a failure cannot be shown now without a claim that is not true. Until the human decides, P1 stays open
- 2026-11-23T00:26:56Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0040 at
  `c36fff7388a4eadeec5d5bf7339f179760d77a1a` against the previous review's commit `eb144ac`. Verified the S1 correction
  and returned the remaining P1 in `.reviews/0040-ready-for-the-jetbrains-marketplace/review-20261123T002656Z.md`, with
  no new findings. Independently reran both modules' tests successfully, including all 7 listing tests, checked the build
  and corrected built description, and ran the change check successfully. Did not rerun the verifier for this delta;
  the online verdict and IDE visual checks remain unverified here. The status stays `in-progress`
- 2026-11-23T00:27:53Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0040 at
  `33d29403951a35f6894e10d4e91ab480b671a999` against the previous review's commit `c36fff7`. The delta holds only the
  previous review entry. Returned the remaining P1, with no new findings, in
  `.reviews/0040-ready-for-the-jetbrains-marketplace/review-20261123T002753Z.md`. The change check and diff check pass.
  Did not rerun tests or the build for unchanged implementation content; the online verifier verdict and IDE visual
  checks remain unverified here. The status stays `in-progress`
- 2026-11-23T01:03:26Z [human] decided the finding P1: accepted a narrow exception to writing the test first, for
  `MarketplaceListingTest` with its assertion for S1, which were written after what they check. Asked for the sandbox to
  admit the JetBrains Marketplace, as a deviation within this unit, and agreed to the host it redirects to
- 2026-11-23T01:03:26Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux host,
  corrected six timestamps of this log, of the planner and the implementer, which did not match the commits and the
  run's own log, and the commit points of `plan.md`, which named commits that were not made. Added requirement 9:
  the allowlist of the sandbox admits `plugins.jetbrains.com` and `downloads.marketplace.jetbrains.com`, to which the
  first redirects its files. `./gradlew verifyPlugin --rerun` succeeded on the host and in the sandbox, with
  `IU-262.12573.28` and `IU-263.8311.30` both `Compatible`, 6 usages of deprecated API and 8 of experimental API, as
  named above. In the sandbox the proxy still refused `jb.gg` once, which the run did not need. Not checked: the look
  of the icon and the description in the plugin manager, in a light and a dark theme, and the hover of the buttons,
  which are left to the developer. The status stays `in-progress`

## Amendments
