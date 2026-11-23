---
kind: feature
status: approved
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

## Amendments
