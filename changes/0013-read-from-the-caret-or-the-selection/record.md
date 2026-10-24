---
kind: feature
status: in-progress
timestamp: 2026-10-19T14:09:41Z
---

# Read from the caret or the selection

## Log

- 2026-10-19T14:09:41Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `37e4b25`, at the human's request, wrote `spec.md` of a new unit: a press reads the
  selection, or else from the sentence the caret is in to the end of the file. It is the second of the two features
  the human asked for when approving unit 0006, from its "Beyond the request", and was written once unit 0006 was
  implemented, as its record asked. Read `process.md` and `roles/specifier.md` of the installed kit. Made it a
  feature and numbered it 0013, after 0012. Asked before writing, the human chose: the selection wins, and without
  one the reading starts at the beginning of the sentence the caret is in. Decided by me and open to the human: a
  sentence is the one of unit 0006, without the cut at a comma, colon or semicolon; a caret with nothing but white
  space after it reads the whole file; a preview without the text reads the whole file; only the primary caret
  counts; the tooltip says which of the three a press will read, with the same icon. Left beyond the request: reading
  from the paragraph, and reading from a click in a preview. The status is `draft`
- 2026-10-24T13:11:55Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `7b3e128`, at the human's request, checked the spec against the units implemented since it was
  written and rewrote it. It chose the part in the text of the file and read it as a file of its own, which unit 0010
  breaks: a piece of Markdown read alone loses what it means in the whole file, as a caret in a fenced code block or a
  selection of rows of a table shows. Its tooltips predated the icon of unit 0012 and the shortcut in the tooltips of
  unit 0017, its notification of nothing to read predated unit 0010, its non-goal of a shortcut was overtaken by
  unit 0017, and its check on both machines predated the rule in `AGENTS.md`. The human chose: the part is chosen in
  the Markdown of the whole file; a code block or a table that the selection touches is read whole; the number of an
  item is read when the part begins after it; the shortcut reads what the button would read; the tooltips name the
  shortcut as "Read Aloud" does; the check is on Linux x64, with one selection and one caret read on macOS on Apple
  Silicon. Decided by me and open to the human: a caret in text that is not read starts at the sentence of the next
  word read; a word that the selection cuts is read whole. Added beyond the request: the tool window saying what is
  read. The status is `draft`
- 2026-10-24T13:24:10Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `00a2a84`, at the human's request, added to the spec that a press and the shortcut with several
  carets or a block selection read by the primary caret and leave the plugin working, and that the tests and the
  developer's check on Linux x64 try both. Reading them as they mean stays a non-goal. The status is `draft`
- 2026-10-24T13:31:40Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `00a2a84`, at the human's request, took item 3 of "Beyond the request" into the spec as
  requirement 10: the tool window shows "Selection" or "From the caret" under the name of the file for the whole of
  such a reading, and nothing new for a whole file. Renumbered the requirements after it. Decided by me and open to
  the human: the two words. The status is `draft`
- 2026-10-24T13:22:35Z [human] approved the spec. The status is `approved`
- 2026-10-24T13:33:15Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of AgentRite's
  orchestrator in the sandbox, on `feature-0013-read-from-the-caret-or-the-selection` at `24a0d4a`, wrote `plan.md`:
  the walk of the Markdown keeps the place in the file of each thing read, the selection or the sentence of the caret
  is chosen in those places for the whole file, the place comes from the primary caret of the tab's text editor or
  the selected editor, and the reading keeps which part it reads for the tooltip and the tool window. Read
  `process.md`, the standards and `roles/planner.md` of the installed kit. The status is `approved`
- 2026-10-24T14:02:10Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a unit run of AgentRite's
  orchestrator in the sandbox, on `feature-0013-read-from-the-caret-or-the-selection` at `c6bd3dc`, began the work.
  Read `spec.md`, `plan.md`, this record, `process.md`, the standards and `roles/implementer.md` of the installed kit.
  There is no review of the unit in `.reviews/` yet, so there are no findings to take. The status is `in-progress`
- 2026-10-24T15:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on
  `feature-0013-read-from-the-caret-or-the-selection`, built the plan in the order of its tasks, tests first, in five
  commits after the plan (`feature-0013 Keep the place in the file of what is read`, `... Choose the selection or the
  sentence of the caret`, `... Show in the tool window which part is read`, `... Read the selection or from the caret of
  the tab and the key`, `... Describe reading from the caret or the selection`). On `ecede7c`, `./gradlew test
  buildPlugin` ran with 582 tests, 3 skipped and 0 failed, and the build passed. The three skipped are the two opt-in
  measurements and `RealModelPackageTest`, which needs the `ALOUDRITE_MODEL_PACKAGE` file. The tests with the real
  engine, `KokoroSpeechEngineTest`, ran with the int8 model in place, 5 of 5 and none skipped. `spokenPartsOf(markdown)`
  gives what it gave, as `SpokenMarkdownTest`, unchanged, shows
- 2026-10-24T15:20:30Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, added to the plan, as necessary
  work it did not name, the test support file `EditorSupport.kt`, which holds the text editors, the preview, the carets
  and the block selection of the tests, and the stand-in file editor without text for `ReadingShortcutsTest.kt`.
  `ButtonPressSupport.kt` gets only the file editor of an event. The placed parts keep the offsets per character in a
  `PlacedWords`, with `numbers` marks for the number of an item, and `sentencesOf` is public in `TextPieces.kt`, both as
  the approach says. Details that the plan left to the work: the platform makes the last caret that is set the primary
  one, so the tests of several carets put the primary caret last; a word that the selection cuts is widened within one run
  of words, which inline code ends, so a word cut that has inline code inside it is widened only up to the code; the
  characters of an entity that has no character in its source take the offset of the place the search is at, which is
  wrong only for a selection that begins or ends inside an entity; the characters of "link" for an address are spread over
  the address, so that a selection of any part of it reaches them; a caret in the number of an item, and a sentence that
  begins right after the number, start at the number, so that a caret in the first sentence of the first item reads the
  whole file; the number put before a part is that of the innermost numbered item, also for a bullet item inside it;
  `NumberedItem` and the offsets are in the coordinates of the whole file, front matter included
- 2026-10-24T15:21:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, did not do the task of looking at
  the change in the sandbox IDE, which stays unticked in `plan.md`: the sandbox has no display and neither `xwd`, `xwininfo`
  nor `Xvfb`, so `./gradlew runIde` cannot open a window there, as `AGENTS.md` and `README.md` say. The three tooltips and the
  two lines of the tool window are therefore checked only by the tests, which run without the look and feel of the IDE.
  Not checked in a running IDE: that the tab row gives the file editor of the tab as `PlatformCoreDataKeys.FILE_EDITOR`,
  which the classes `EditorWindow` and `EditorTabbedContainer` of the platform name in their code but no test with a
  window shows, and that a toolbar update after a caret move shows the new tooltip. `developer-checks.md` holds the checks
  for requirement 13, which the developer has not made yet. The status stays `in-progress`
- 2026-10-24T13:51:23Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a full review of unit 0013 at
  `05977c46473f9bbe747ca9dce31e12c24961d624`, against merge base `24a0d4a` with `origin/master`. Returned S1, S2,
  S3, S4 and S5 in `.reviews/0013-read-from-the-caret-or-the-selection/2026-10-24T135123Z-round-1.md`.
  `./gradlew cleanTest test buildPlugin` and `npm run check-changes` passed; the five real-engine tests ran with
  the int8 model and none skipped. Could not verify the running-IDE visual and toolbar checks or the developer's
  Linux x64 and macOS Apple Silicon listening checks. The status stays `in-progress`
- 2026-10-24T16:10:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on
  `feature-0013-read-from-the-caret-or-the-selection`, took the findings of round 1 of the review in
  `.reviews/0013-read-from-the-caret-or-the-selection/2026-10-24T135123Z-round-1.md`. Accepted by the definition of the
  orchestrator, and not decided by the human: S1, S2, S3 and S4, each reproduced or confirmed in the code and corrected
  in the commit after `05977c4`. S1: the characters of "link" for an address now each reach up to the next, so a
  selection of any part of the address, and a caret near its end, reach them. S2: the offsets of an HTML block or inline
  are found for the whole literal and the characters that survive the comments are chosen by index, so repeated text
  is no longer matched into a comment. S3: a caret at the end of a code block or table is after it, not in it. S4: the
  choice tests now also go through the stand-in engine and player and assert the text that reaches it, the block
  selection test asserts the primary caret's exact selection and pauses, resumes and stops, and the key is tested with
  several carets, with selections and with a block selection. Regression tests were added for S1 to S3. Not accepted
  or declined: S5 is the developer's checks of requirement 13 and the look in the sandbox IDE, which stay open for the
  human. No decline is proposed. On `988ca82`, `./gradlew test buildPlugin` ran 590 tests, 0 failed, 3 skipped as before,
  and the build passed. The status stays `in-progress`
- 2026-10-24T13:56:39Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a delta review of unit 0013 at
  `92bacd9`, compared with `05977c46473f9bbe747ca9dce31e12c24961d624`, the commit reviewed in round 1.
  Verified the corrections of S1 to S4 and returned the still-unverified S5 in
  `.reviews/0013-read-from-the-caret-or-the-selection/2026-10-24T135639Z-round-2.md`, with no new findings.
  `./gradlew cleanTest test buildPlugin` and `npm run check-changes` passed; the five real-engine tests ran
  with the int8 model and none skipped. Could not verify the running-IDE visual and toolbar checks or the
  developer's Linux x64 and macOS Apple Silicon listening checks. The status stays `in-progress`
- 2026-10-24T13:58:44Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, completed a delta review of unit 0013 at
  `aeca9d8`, compared with `92bacd9`, the commit reviewed in round 2. The delta contains only that review's
  record entry, with no implementation changes. Returned the still-unverified S5 and no new findings in
  `.reviews/0013-read-from-the-caret-or-the-selection/2026-10-24T135844Z-round-3.md`.
  `./gradlew cleanTest test buildPlugin` and `npm run check-changes` passed; the five real-engine tests ran
  with the int8 model and none skipped. Could not verify the running-IDE visual and toolbar checks or the
  developer's Linux x64 and macOS Apple Silicon listening checks. The status stays `in-progress`
- 2026-10-24T22:52:24Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `feature-0013-read-from-the-caret-or-the-selection` at `5c40525`, at the human's request, corrected the
  spec after the developer's check on Linux x64. In the sandbox IDE the tooltip of the button did not follow the
  selection or the caret in any of its three cases: it changed only once a reading had started, so requirement 8 was
  false. Checks 1 to 3 of `developer-checks.md` read what they should, and check 4 read the whole file with no line
  in the tool window. The human chose to keep the tooltip "Read Aloud" in every case rather than to make it follow.
  Requirement 8 now keeps the button as units 0012 and 0017 made it, requirement 12 tests "Read Aloud" in place of
  the three tooltips, requirement 14 no longer asks the README for them, the decision on the tooltip gives the reason,
  and a non-goal names the tooltip that says what will be read. The status stays `in-progress`

## Amendments
