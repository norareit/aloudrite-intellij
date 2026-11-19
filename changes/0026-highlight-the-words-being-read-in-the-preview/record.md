---
kind: feature
status: in-progress
timestamp: 2026-11-19T13:16:49Z
---

# Highlight the words being read in the preview

## Log

- 2026-11-19T13:16:49Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `b4d5317`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0026, after 0025, on which it
  builds. Checked in the Markdown plugin of IntelliJ IDEA 2026.2.3 that the elements of the preview carry the ranges of
  the file they were made from, and that the plugin has an extension point for scripts and a pipe for messages to the
  page. Decided with the human: the preview in a unit of its own, after unit 0025, and scrolling by itself. Decided by
  me and open to the human: drawing with the highlights of the browser without changing the page, an optional
  dependency on the Markdown plugin that fails quietly, and each view following by itself. Left beyond the request: a
  notification when the preview cannot show the highlights. The status is `draft`
- 2026-11-19T13:30:22Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `375cc6a`, added to requirement 7 that the checkbox unit 0025 took in as its requirement 15
  turns the highlights off and on in the preview as well. The status is `draft`
- 2026-11-19T13:40:11Z [human] approved the spec. The status is `approved`
- 2026-11-19T21:48:07Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by AgentRite's orchestrator on the Linux
  host, cut `feature-0026-highlight-the-words-being-read-in-the-preview` from `master` at `3cd6e83` and wrote `plan.md`.
  Read `process.md`, the standards and `roles/planner.md` of the installed kit, 8.4.0. Read the classes and scripts of
  the Markdown plugin of IntelliJ IDEA 2026.2.3 for the extension point, the pipe, the attribute of the ranges, the
  patching of the page and the scroll sync, and wrote what they do into the plan. Decided in the plan: the page
  describes its text to the IDE and the words are found in Kotlin, so that the tests need no browser; the provider
  registered in an optional descriptor, since the panel reads its extensions only when it is made; and a check in the
  sandbox IDE of whether the platform reports an error for a missing extension point, logged as a finding for the
  human where it does. The status is `approved`
- 2026-11-19T21:55:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator on
  the Linux host, on `feature-0026-highlight-the-words-being-read-in-the-preview` at `22e351e`, began the work. Read
  `process.md`, the standards and `roles/implementer.md` of the installed kit. No review of this unit exists yet. The
  status is `in-progress`
- 2026-11-19T22:06:03Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, built tasks 1 to 9 of the plan on
  the branch, committed in the commit points of the plan: `PreviewText`, `PreviewPlaces`, `PreviewMessage`,
  `PreviewPages`, the sending from `ReadAlongHighlighter`, the optional dependency with `aloudrite-markdown.xml`, the
  provider and extension in `com.ritenoar.aloudrite.preview`, and the script of the page. Two deviations from the plan,
  neither a change of what the unit makes true: `PreviewHtml.kt` also gives `descriptionOfPage`, the JSON a page would
  send, which `ReadAlongPreviewTest` hands to its pages, and `ReadAlongPreviewExtensionTest` reads the registered
  extensions of the extension point from the application, since the descriptor classes that the plan's wording would
  need are internal to the platform. jsoup and the Markdown library come with the platform, so `build.gradle.kts` gained
  no `testImplementation`. `./gradlew test` on `f973fcd` ran 823 tests with 0 failures and 4 skipped, the skipped ones
  being those that need the real engine, which this unit does not touch, and `./gradlew buildPlugin` built an archive of
  20.7 MB, within the limit of 30 MB
- 2026-11-19T22:06:04Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, looked at the preview in the sandbox
  IDE, which it ran with `./gradlew runIde` on a private X display (Xvfb) with a copy of `samples/` as the project, so
  that no window of the developer was in reach, and captured only the window of that IDE with `xwd -id`. Reading
  `markdown-features.md` with the audio of the machine, in Islands Dark with the editor and the preview side by side,
  the captures showed: the light highlight of the sentence and the strong highlight of the word moving through the
  paragraphs; a sentence over `struck-out` text highlighted through the strikethrough element; the sentence highlighted
  across inline code, where the code element's own background keeps the highlight faint; "code example" on the text of
  the code block; "see table" on the text of every cell; an image outlined, in the dark and in the light theme, the
  outline being faint in the light one; and the preview scrolling by itself to keep the word in view. With the preview
  alone, the word was highlighted as well, in the file with no text editor in view. In Islands Light, both split and
  preview alone, the sentence is a pale blue and the word a stronger blue, and the text stays readable. With the wheel
  turned in the preview during a reading, the preview stayed where the user left it while the reading went on, and after
  a pause and a resume it scrolled back to the word. The reading of the editor beside it showed no scroll back and
  forth. Not checked: a selection copied from the preview, since a copy cannot be seen in a capture, though the page
  adds no element and no text by construction, and the scroll bar being dragged
- 2026-11-19T22:06:05Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, checked the cases of requirement 10
  in the sandbox IDE. With the Markdown plugin disabled, as `org.intellij.plugins.markdown` in the
  `disabled_plugins.txt` of the sandbox's config directory, the plugin loaded (the log lists it among the loaded
  plugins), the tool window and the toolbar were there, no error was shown and `idea.log` held none from the plugin. A
  reading could not be started there, since the file is no Markdown file without the plugin, so that the editor's
  highlights and the log line were not checked in that configuration. With the name of the extension point in
  `aloudrite-markdown.xml` changed by hand for the check and reverted afterwards, the platform showed no error and
  logged no warning for the unknown extension point, the reading highlighted the editor, and the preview showed none.
  That change leaves the real extension point of the Markdown plugin in place, so the check of the highlighter passed
  and no line was written, which the plan had expected; a missing extension point, where the line is written, is covered
  by `ReadAlongPreviewTest` only. The log lines for a panel without a pipe and for a page without ranges are covered by
  `ReadAlongPreviewExtensionTest` only
- 2026-11-19T22:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran the suites on `3846773`:
  `./gradlew test` reported 823 tests, 0 failures and 4 skipped, those that need the real engine, which this unit does
  not touch, and `npm run check-changes` passed. Requirement 13, the developer's reading of `samples/markdown-features.md`
  on Linux x64 with the preview alone in view, is left open in `plan.md` for the human, and so is the review. The status
  stays `in-progress`
- 2026-11-19T22:10:36Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0026 at `8f81216` in a full review
  against the merge base `3cd6e83`. Returned S1, P1 and C1, saved in
  `.reviews/0026-highlight-the-words-being-read-in-the-preview/review-20261119T2212.md`. Independently ran
  `./gradlew cleanTest test`, which passed, including the real engine tests; the opt-in measurements and real download
  test were skipped. `./gradlew test buildPlugin`, `npm run check-changes` and the diff whitespace check passed.
  Reproduced S1 with compiled production classes and the platform Markdown HTML generator. Did not independently
  verify the sandbox captures, copying or the developer's listening check. The status stays `in-progress`
- 2026-11-19T22:50:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator,
  took the three findings of `.reviews/0026-highlight-the-words-being-read-in-the-preview/review-20261119T2212.md`,
  which the definition of the orchestrator accepts as findings the implementer agrees with, and not as decisions of the
  human. The work is added to `plan.md` as three ticked tasks. S1, accepted by the definition of the orchestrator: the
  alignment of `PreviewPlaces` now takes an entity of the file, such as `&#32;`, `&nbsp;` or `&copy;`, as one character
  of the text node that starts where the entity starts, so that the words after it keep their positions. Regressions
  through `describedPage` cover `&#32;`, `&#x20;`, `&amp;`, `&nbsp;`, `&#169;`, `&copy;` and the letters of `&amp;` not
  being taken for the characters after it, and failed before the fix. A literal "&" followed by letters and a ";" that
  is no entity in the file, such as in `AT&T;`, would be taken for one, and is not handled. C1, accepted by the
  definition of the orchestrator: `src/test/javascript/read-along-script.test.mjs` runs the real script against a
  stand-in for the browser and the pipe, with 16 tests of the description, the version filter, the clearing at a patch,
  the no-ranges message, the colours, the outline, the unchanged page, the following and its stops by wheel, keys and a
  pointer-held scroll, and the missing pipe or highlights, and `ReadAlongScriptTest` runs it with Node from
  `./gradlew test`, so the suite needs Node, which the change check needs as well. One behaviour the tests show: a
  key or wheel before the first message of a reading does not stop the following, since a new reading starts it again.
  P1, accepted by the definition of the orchestrator: in the sandbox IDE on a private X display, with a reading going on
  and with the stop button shown, the preview's whole text was selected and copied, and the clipboard, read with `xclip`,
  was byte for byte the text copied with no reading, 5589 bytes in both (`cmp`). The earlier entry that left the copying
  unchecked is thereby answered. `./gradlew test` reported 827 tests, 0 failures and 4 skipped on the tree of this entry
  before its commit, which is the tree of the commit that follows it
