---
kind: feature
status: in-progress
timestamp: 2026-10-19T20:01:17Z
---

# Read Markdown without its markers

## Log

- 2026-10-19T11:50:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `67e2777`, at the human's request, wrote `spec.md` of a new unit: the plugin reads Markdown
  without its markers, with words in place of code blocks and tables and with pauses where the structure asks for
  them. It starts from what the human heard on Linux during the checks of unit 0006, gathered in `record.md` of unit
  0006 at 2026-10-19T11:41:58Z and in `docs/reading-markdown.md` on the branch `feature-0006-read-markdown-aloud`,
  which is not on `master` yet. Read `process.md` and `roles/specifier.md` of the installed kit. Made it a feature
  and numbered it 0010, after the 0009 of the human. It needs unit 0006 implemented. Decided by me and open to the
  human: that the plugin reads the structure of the Markdown and does not only take out characters; that a pause is
  silence the plugin adds and not punctuation; the lengths of the pauses, half a second between paragraphs and for a
  horizontal rule, a quarter of a second around inline code, two seconds after "code example"; a bare address read as
  "link" and not as its domain; the struck out words read; the state of a task not said; an escaped character handed
  to the engine; a file of nothing but a code block not counted as empty. Left beyond the request: naming the
  symbols of code, front matter and HTML comments. Nothing of it was heard yet. The status is `draft`
- 2026-10-19T14:16:12Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `28bbe26`, corrected `spec.md` at the human's request. The human listened to a spec of their own
  with AloudRite and found its numbered items, which had no full stops, running together, "very awkward"; the items
  of the sample's numbered list all end with a full stop, which gave the pause that sounded good there, so "numbered
  lists sound fine" holds only for items with their own punctuation. The human asked for a pause before the next item
  in numbered and bulleted lists alike, without a double pause for items that do end with punctuation. Changed: the
  context says so; requirement 4 gives every item of a list, numbered ones too, the pause and fall of the end of a
  sentence, once, and keeps the numbers read; requirement 16 keeps only quotes as in unit 0006; the decision on the
  lengths of the pauses no longer says that numbered lists have the pause already; a new decision: an item without
  its own end is handed to the engine as if it ended with a full stop, so that all items end alike and none gets a
  second pause, the one place where a pause is made with punctuation, with the reasons and the alternatives
  rejected. Decided by me and open to the human: that way of making the pause. No requirement is renumbered. Not
  corrected here, since this commit holds only this unit's documents: the sentence on numbered lists in
  `docs/reading-markdown.md` and the record of unit 0006, which say the voice pauses at each new number; the
  first is for requirement 21 of this unit, the second for an amendment of unit 0006 if the human wants one. The
  status stays `draft`
- 2026-10-19T19:30:37Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, reworded the non-goal on emoji in
  `spec.md`, which said that characters above U+FFFF stay left out and so read as for good: this unit leaves them out
  as unit 0006 does, and unit 0014 has them read once a release of sherpa-onnx can hand them over intact. On the
  human's word that they are left out until unit 0014 is done, and read after it. The status stays `draft`
- 2026-10-19T19:41:17Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, changed requirement 3 of `spec.md` and
  its decision so that struck out text is not read, neither its tildes nor its words, on the human's word that
  reading struck out words next to the words that replace them makes a sentence the author never meant. A file whose
  only words are struck out reads nothing, under requirement 17. The status stays `draft`
- 2026-10-19T19:45:25Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, took front matter in from "Beyond the
  request" of `spec.md`, on the human's word: requirement 15 leaves out a block of YAML between a `---` on the first
  line of the file and the next `---` or `...`, with no pause in its place, and a decision gives the reasons. Front
  matter of another form, such as TOML between `+++` lines, stays a non-goal. Requirements 15 to 21 became 16 to 22,
  so the requirement on `docs/reading-markdown.md` that earlier entries call 21 is now 22. The status stays `draft`
- 2026-10-19T19:46:52Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, took HTML comments in from "Beyond the
  request" of `spec.md`, on the human's word: requirement 16 leaves out an HTML comment, on lines of its own or within
  a sentence, with no pause in its place and with CommonMark deciding what is a comment, and a decision gives the
  reasons. Other HTML stays a non-goal. Requirements 16 to 22 became 17 to 23, so the requirement on
  `docs/reading-markdown.md` that earlier entries call 21 or 22 is now 23. The status stays `draft`
- 2026-10-19T19:49:18Z [human] approved the spec as it stands, in the main session on the Linux host, after taking front
  matter and HTML comments in from "Beyond the request" and leaving struck out text unread, and leaving naming the
  symbols of code out. The status is `approved`. The entry was written by Claude Code, anthropic/claude-opus-5-5, on
  the human's word
- 2026-10-19T19:57:17Z [human] settled, when the planner asked, what requirement 17 leaves open: every two blocks
  that follow each other get the half second of silence between them, heading, paragraph, list, quote, table, rule
  and code block alike, except the items of one list, which have only the end of a sentence between them, and the
  silence after "code example", which is two seconds in all and not two and a half. `spec.md` is not changed; the
  plan follows this. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-19T19:59:00Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  cut the branch `feature-0010-read-markdown-without-its-markers` from `master` at `a938e17` and wrote `plan.md`
  against it. Read `process.md` and `roles/planner.md` of the installed kit. The plan bundles commonmark-java 0.30.1
  with four of its extensions and autolink-java 0.12.0, hands the reading a list of words and silences in place of a
  string, and works the words and silences out of the parsed Markdown in one pure function, tested through the
  stand-in engine. `./gradlew test` on that tree ran 201 tests: none failed and two were skipped. The status is
  `planned`
- 2026-10-19T20:01:17Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on the branch `feature-0010-read-markdown-without-its-markers` at `b91cabe`, began the plan. Read
  `process.md` and `roles/implementer.md` of the installed kit, and `spec.md`, `plan.md` and `record.md` of this unit.
  The status is `in-progress`
- 2026-10-19T20:12:40Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, did task 4 the other way round
  from how the plan words it: the plan has `endsASentence(text, whiteSpaceStart)` call `endsWithASentenceEnd` on the
  text before the white space, which copies that text at every run of white space and makes cutting a long file
  quadratic; instead `endsWithASentenceEnd(text)` calls `endsASentence(text, text.length)`. The rule of what ends a
  sentence is still held once, and the tests of `piecesOf` pass unchanged. `./gradlew buildPlugin` gave an archive
  of 20,415,528 bytes that holds the five commonmark-java jars and `autolink-0.12.0.jar`, within its size check
- 2026-10-19T20:16:30Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, added to task 10 and to the files
  of `plan.md` a change the plan had not foreseen: three tests of `ModelDownloadFlowTest` press on `# Title` and
  expected the engine to be given `# Title`, which this unit makes `Title`; their expectation is now `Title`. In
  task 9, the case of a file of nothing but markers that are not read covers front matter, two comments on lines of
  their own, and two struck out paragraphs. In task 7, a code block as the last block of a list item is heard as
  `code example.`, since the plan ends the last words of every item as a sentence. Concern, not worked around: in one
  of seven runs of `./gradlew test` on this tree, `InstallAttemptTest.noPropertyOfThePluginRemainsAfterTheAttemptClosesOrAClaimFails`
  failed because a system property of an install lock under a temporary `aloudrite` folder was still set, which only
  `ModelDownloadFlowTest` and `ModelInstallerTest` make; the other six runs passed. This unit changes neither of
  those tests beyond the expected text, and I did not find out whether the same happens on `master`

## Amendments
