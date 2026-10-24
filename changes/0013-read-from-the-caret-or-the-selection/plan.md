# Read from the caret or the selection

## Approach

The spec asks for three things that do not exist yet: knowing where in the file each thing that is read came from,
choosing a part of the file by those positions, and saying which part a press will read. They are built in that
order, from pure computation outwards to the action, the reading and the tool window.

**Every part that is read knows where it came from.** Today `SpokenMarkdown` walks the parsed file and makes
`Words` and `Silence` directly, joining words as it goes, and `spokenPartsOf` then replaces the characters above
U+FFFF and tidies the silences. The walk changes to make placed parts, a type internal to the reading of Markdown:

* Placed words carry their text, the offset in the whole file, front matter included, of the source character each
  of their characters was read from, and the number of the block they belong to. A block is what the plugin already
  treats as one: a paragraph, a heading, an HTML block, a code block, a table, and the first paragraph of an item of
  a list together with the item's number
* The offsets come from the source spans that the parser keeps with `IncludeSourceSpans.BLOCKS_AND_INLINES`, shifted by
  the length of the front matter that `withoutFrontMatter` took off. Within a `Text` or a `Code` node, whose literal
  can be shorter than its source because of escapes, entities or backticks, each character of the literal is matched
  to the next equal character of the source span, searching forwards. The HTML of a block or an inline element keeps
  the offsets of the characters that `withoutComments` keeps
* What the plugin adds itself takes the offset of the source next to it: the full stop that ends an item takes that
  of the last character of the item's words, the `"\n"` of a line break that of the break, and the number of an item
  that of its marker. The number is also marked as the number of its item, with the item's source range
* "code example" with its silence and "see table" are marked as standing for their whole block, with the block's
  source range
* Placed silences are the silences there are today, between blocks, between items and around inline code

`PartsInARow` joins placed words of the same block as it joins words now, with their offsets. Turning placed parts into
spoken parts drops the positions and then does what `spokenPartsOf` does now, the characters above U+FFFF and the
tidying, so that `spokenPartsOf(markdown)` gives exactly what it gives today. `SpokenMarkdownTest` stays as it is and
is the proof (R4, R9).

**A sentence is cut once.** `TextPieces.kt` cuts the words at the end of a sentence, at a long enough clause and at a
blank line. The cut at a clause becomes a parameter of the private cutting function, and a new `sentencesOf(text)`
gives the cut without it, so that the rule of where a sentence ends is written once and both the engine and the start
of a reading from the caret follow it (R2).

**The part is chosen in the placed parts of the whole file.** A new file `PartOfTheFile.kt` holds the pure choice:

* `PlaceInTheText`, a sealed type: `Selection(start, end)` with `start < end`, `Caret(offset)`, and `NoText` for a
  preview without its text
* `PartOfTheFile`, an enum of `WHOLE_FILE`, `SELECTION` and `FROM_THE_CARET`
* `WhatToRead(parts: List<SpokenPart>, partOfTheFile: PartOfTheFile)`
* `whatToRead(markdown: String, place: PlaceInTheText): WhatToRead`, which parses the whole file once into placed
  parts and then:
    * For `NoText`, gives the whole file as `WHOLE_FILE` (R5)
    * For a selection, keeps the characters of placed words whose offset lies in the selection, and widens each end
      that falls on a character that is not white space to the white space around its word, within the joined words
      of its block, so that a cut word is read whole. A part marked as standing for a whole block is kept when its
      block's source range overlaps the selection at all. A silence is kept when it stands between two kept parts.
      Where the first kept character lies in an item of a list after the item's number, the number of the innermost
      such item is put before it. The result is `SELECTION`, also when it reads nothing (R1, R4)
    * For a caret, finds the first part read at or after the caret: a part standing for a whole block whose range
      holds the caret or begins after it, or else the first character of placed words that is not white space and
      whose offset is at or after the caret. Where there is none, the whole file is read as `WHOLE_FILE` (R3).
      Otherwise the reading starts at the beginning of that character's sentence, by `sentencesOf` over the joined
      words of its block, or at the start of the whole block, and runs to the end of the file, with the number of the
      item put before it as for a selection. Where nothing read lies before that start, it is the whole file as
      `WHOLE_FILE`, and otherwise `FROM_THE_CARET` (R2, R4)
  Turning the kept placed parts into spoken parts then drops the silence before the first words, as the tidying does
  for a whole file (R4)

A caret in the address of a link that ends its paragraph starts at the next sentence read after it, which is what
R2 says of text that is not read; the decision's example of a caret in the address assumes a link followed by more of
its sentence, and the tests use such a link.

**The place comes from the editor of the tab, read at the IDE's edge.** A new file `EditorPlace.kt` holds
`placeShownBy(fileEditor: FileEditor?): PlaceInTheText`, called under a read action:

* A `TextEditorWithPreview`, the editor that the Markdown plugin opens, whose `layout` is `SHOW_PREVIEW` gives `NoText`
  (R5)
* Any other `TextEditor`, also a `TextEditorWithPreview` that shows its text, gives the primary caret of its editor:
  a `Selection` of the primary caret's selection where it has one that is not empty, and otherwise a `Caret` at its
  offset. No other caret is looked at, and nothing is changed (R6, R11)
* Anything else, or none, gives `NoText`

`TextEditorWithPreview` is a class of the platform, not of the Markdown plugin, so the plugin still needs no class of
it, as unit 0003 decided. On the tab, the editor is `PlatformCoreDataKeys.FILE_EDITOR` of the event: the tab row's
`EditorTabs` gives it, as the selected editor of the same window whose file it gives as `VIRTUAL_FILE`. For the key,
the collaborator `selectedFile: (Project) -> VirtualFile?` of `ReadAloudAction` becomes `selectedEditor: (Project) ->
FileEditor?`, in the IDE `FileEditorManager.getInstance(project).selectedEditor`, whose `file` is the file read (R7).

**The action.** A press that starts a reading reads the text at the press, as now, and the place from the editor in
the same read action, and hands both to `whatToRead`. Its parts go where the parts of the whole file go now: to the
check for nothing to read, which shows the notification and asks nothing, and to the reading or the question about the
download, which after the download reads the same `WhatToRead` (R1, R9). On the tab, `update` computes `whatToRead`
in the same way while the file of the tab is not read, and sets the text and the tooltip to "Read Selection Aloud",
"Read Aloud from Caret" or "Read Aloud" by its `partOfTheFile`, with the shortcut named where it is named now and the
icon the person speaking in all three. Since the toolbar updates its buttons at every update, a tooltip shown after the
caret moved says what a press then reads. The parse of a file of ten thousand words takes milliseconds, so the update
parses each time and keeps no cache; the measurement in the tasks checks this (R8).

**The reading knows its part.** `Readings.read` and `Readings.readOnceTheModelIsInPlace` take a `WhatToRead` in place
of the parts, and `SpeechService` keeps its `partOfTheFile` with the reading for as long as it lasts, also while it is
paused. `CurrentReading` gets `partOfTheFile(target): PartOfTheFile?`, as it has `isPaused(target)`. Pausing,
resuming and the speed need nothing more, since they work on the parts the reading was given (R9, R10).

**The tool window.** `ReadingPanel` gets a label under the name of the file that shows "Selection" for `SELECTION`
and "From the caret" for `FROM_THE_CARET`, also while paused, and is hidden for `WHOLE_FILE` and while nothing is read.
It is set in `showTheCurrentReading`, which runs only when the reading changes, so a caret moved during the reading
does not change it (R10).

**Tests.** As in the units before, the tests start only the application, not a project. An editor is made with
`EditorFactory.getInstance().createEditor(document)` on the document of a `LightVirtualFile`, and released after the
test with `releaseEditor`, and the `TextEditor` of the tab with `TextEditorProvider.getInstance().getTextEditor(editor)`.
A preview is a `TextEditorWithPreview` made of two such text editors with the layout `SHOW_PREVIEW`. Where one of these
cannot be made in the application-only test environment, the test makes a stand-in that implements `TextEditor` with
the made editor, or extends `TextEditorWithPreview` with stand-ins, and the plan's tasks stay the same. The pure choice
is tested through the stand-ins of unit 0006 for the engine and the player, as `SpokenMarkdownTest` does, so that what
is asserted is what reaches the engine (R12).

There is no end-to-end suite that drives an IDE window, as unit 0003 decided, so the look in the sandbox IDE and the
developer's checks of R13 are the end-to-end coverage. The unit changes the text handed to the engine, so the tests
with the real engine are run with the model in place, as `AGENTS.md` asks.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/PlacedPart.kt`: new, placed words and silences, with the marks of a whole
  block and of the number of an item
- `src/main/kotlin/com/ritenoar/aloudrite/SpokenMarkdown.kt`: the walk makes placed parts, and the turning of placed
  parts into spoken parts; `spokenPartsOf(markdown)` keeps its result
- `src/main/kotlin/com/ritenoar/aloudrite/TextPieces.kt`: the cut at a clause as a parameter, and `sentencesOf`
- `src/main/kotlin/com/ritenoar/aloudrite/PartOfTheFile.kt`: new, `PlaceInTheText`, `PartOfTheFile`, `WhatToRead` and
  `whatToRead`
- `src/main/kotlin/com/ritenoar/aloudrite/EditorPlace.kt`: new, `placeShownBy`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: the place at the press and in the update, the three
  texts, `selectedEditor`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: `WhatToRead` in `Readings`, the part kept with the
  reading, `partOfTheFile` in `CurrentReading`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: the label of the part
- `src/test/kotlin/com/ritenoar/aloudrite/PlacedPartsTest.kt`: new, the offsets of placed parts
- `src/test/kotlin/com/ritenoar/aloudrite/TextPiecesTest.kt`: `sentencesOf`
- `src/test/kotlin/com/ritenoar/aloudrite/PartOfTheFileTest.kt`: new, what reaches the engine for each selection and
  caret of R12
- `src/test/kotlin/com/ritenoar/aloudrite/ButtonPressSupport.kt`: the file editor in the event of a press, editors
  with a caret, a selection, several carets and a block selection, a preview, and `selectedEditor` in
  `readAloudActionOn`
- `src/test/kotlin/com/ritenoar/aloudrite/EditorSupport.kt`: new, the text editors, previews, carets and selections of a
  test and their release, which `ButtonPressSupport.kt` would otherwise hold; added while building
- `src/test/kotlin/com/ritenoar/aloudrite/ReadFromTheCaretTest.kt`: new, presses, the key, the tooltips, several
  carets and the unchanged caret
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingShortcutsTest.kt`: `selectedEditor` in place of `selectedFile`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`, `SpokenMarkdownTest.kt`, `ReadingPanelTest.kt`,
  `ModelDownloadFlowTest.kt` and any other test that calls `read` or `readOnceTheModelIsInPlace`: `WhatToRead` in
  place of the parts
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: the label of the part
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadFlowTest.kt`: a part read after a download
- `README.md`: what a press and the key read, the tooltips and the tool window
- `changes/0013-read-from-the-caret-or-the-selection/developer-checks.md`: new, the checks of R13

## Tasks

### Placed parts

- [x] Write `PlacedPartsTest.kt`, failing first: for each character of the words of a placed part, the offset is that
  of the source character it was read from, in a file with front matter, an escaped character and an entity, bold and
  italic, inline code, a link with text, an HTML block with a comment, and a numbered list; the full stop added to an
  item takes the offset of the item's last character, a line break that of the break, and the number of an item that
  of its marker, marked with the item's range; "code example" with its silence and "see table" are marked with the
  range of their block; every placed part of one paragraph has the same block number, and the parts of two
  paragraphs have two (R4)
- [x] Add `PlacedPart.kt` and make the walk of `SpokenMarkdown` produce placed parts as the approach says, with
  `PartsInARow` joining placed words of a block with their offsets, and `endedAsASentence`, `joined` and
  `givesNothing` working on placed parts. `spokenPartsOf(markdown)` turns them into spoken parts as the approach says,
  and `SpokenMarkdownTest`, `ReadAloudActionTest` and `PlacedPartsTest` pass with `SpokenMarkdownTest` unchanged (R4,
  R9)

### Sentences

- [x] Add to `TextPiecesTest.kt`, failing first: `sentencesOf` cuts after `.`, `!` or `?` with the closers and white
  space, and at a blank line, and not at a comma, colon or semicolon also in a sentence longer than 60 bytes; its
  pieces joined give the text (R2)
- [x] Make the cut at a clause a parameter of the cutting in `TextPieces.kt`, and add `sentencesOf(text)` without it.
  `piecesOf` keeps its result, which `TextPiecesTest` shows unchanged (R2)

### Choosing the part

- [x] Write `PartOfTheFileTest.kt`, failing first, which reads `whatToRead(markdown, place).parts` through the
  stand-ins and asserts what is heard and the `partOfTheFile`. For a selection: within a paragraph, one that cuts a
  word at each end, which is heard whole, one across two paragraphs with the silence between them, one that touches a
  fenced code block in part and gives "code example" with its silence, one that touches a table in part and gives "see
  table", one that begins in the third item of a numbered list after its number and is heard with "3. " first, one of
  front matter only, and one of white space only, the last two hearing nothing as `SELECTION`. For a caret: in the
  middle of a sentence, at its first character, in the white space between two sentences, which starts at the second,
  in the `#` of a heading, which starts at the heading, in the address of a link followed by more of its sentence,
  which starts at that sentence, in front matter before a first sentence, which is the whole file as `WHOLE_FILE`,
  inside a fenced code block, which starts at "code example", inside a table, which starts at "see table", after the
  last word, which is the whole file as `WHOLE_FILE`, and at the start of the file, which is the whole file as
  `WHOLE_FILE`; in a sentence of a long paragraph after a comma that `piecesOf` cuts at, which starts at the sentence,
  not at the clause; and in the second sentence of a numbered item, heard with its number first. For both: a
  reference link whose definition lies outside the part is heard by its text, a comment opened before the part and
  closed in it hides what it covers, no part is heard with a silence first, and characters above U+FFFF in a part are
  heard as spaces. `NoText` gives the whole file as `WHOLE_FILE`. For "# Title\n\nFirst one. Second two.\n\nNext." a
  caret in "Second" is heard as "Second two.", the silence between blocks, and "Next." (R1, R2, R3, R4, R5, R9, R12)
- [x] Add `PartOfTheFile.kt` with `PlaceInTheText`, `PartOfTheFile`, `WhatToRead` and `whatToRead` as the approach
  says, until `PartOfTheFileTest` passes (R1, R2, R3, R4, R5)

### The reading knows its part, and the tool window shows it

- [x] Change `Readings.read` and `Readings.readOnceTheModelIsInPlace` to take a `WhatToRead`, keep its
  `partOfTheFile` in the `Reading` of `SpeechService`, add `partOfTheFile(target)` to `CurrentReading` and
  `SpeechService`, and pass `WhatToRead(parts, PartOfTheFile.WHOLE_FILE)` in `ReadAloudAction` and in every test that
  reads parts, so that the suite passes as before (R10)
- [x] Add to `ReadingPanelTest.kt`, failing first: the panel shows "Selection" in a line of its own under the name of
  the file while a selection is read and while it is paused, "From the caret" while a reading from the caret is read
  and while it is paused, neither for a whole file nor while nothing is read, and the same line after a press of
  "Pause" and "Resume"; the tool windows of two projects show the same line (R10, R12)
- [x] Add the label of the part to `ReadingPanel`, set in `showTheCurrentReading`, until the tests pass (R10)

### The action reads the part

- [x] Add to `ButtonPressSupport.kt`: an optional file editor in `eventFor`, put in the data context as
  `PlatformCoreDataKeys.FILE_EDITOR`; helpers that make a text editor of a file with a caret at an offset, with a
  selection, with several carets with and without selections, and with a block selection over several lines, and a
  preview, as the approach says, with the release of the editors after each test; and `selectedEditor` in place of
  `selectedFile` in `readAloudActionOn`. Change `ReadingShortcutsTest.kt` to give a file editor of the selected file,
  with no text editor where the test needs only the file, so that it passes as before (R7, R12)
- [x] Write `ReadFromTheCaretTest.kt`, failing first: a press on the tab with a selection reads the selection, with a
  caret in the middle of a paragraph reads from its sentence, with a caret at the start reads the whole file, with an
  empty selection reads from the caret, and in a preview reads the whole file; the key with nothing read reads the selection, and from the caret, of the selected
  editor; a selection that reads nothing shows the notification that there is nothing to read, reads nothing and asks
  nothing without the model; a selection read after a document change while paused is heard as at the press; the
  caret and the selection are the same after the press, a pause, a resume and the end of the reading; with several
  carets with and without selections, and with a block selection over several lines, a press and the key read by the
  primary caret, the reading is paused, resumed and stopped, no failure is reported, and the next press reads; the
  tooltip on the tab is "Read Selection Aloud", "Read Aloud from Caret" and "Read Aloud" in the three cases of R8,
  each with the shortcut named while nothing is read and with no shortcut while another file is read, with the person
  speaking as the icon, and changes from "Read Aloud from Caret" to "Read Aloud" after the caret moves to the start;
  "Pause Reading" and "Resume Reading" on the tab, and the names of the actions by key and in Find Action, stay as
  they are; the tool window shows "Selection" and "From the caret" for the readings a press starts (R1, R2, R3, R5,
  R6, R7, R8, R9, R10, R11, R12)
- [x] Add `EditorPlace.kt` with `placeShownBy`, and change `ReadAloudAction` to read the place with the text at the
  press, to read `whatToRead` of them, to set the three texts in `update`, and to take `selectedEditor`, as the
  approach says, until the tests pass (R1, R5, R6, R7, R8, R9, R11)
- [x] Add to `ModelDownloadFlowTest.kt`, failing first if it fails: a press with a selection and without the model
  asks about the download, and once the download ends reads the selection by itself, as `SELECTION` (R9, R12)
- [x] Measure in a test of `ReadFromTheCaretTest.kt` the time of an update of the button on the tab for
  `samples/ten-thousand-words.md` with a caret in its middle, and assert that it stays under 50 milliseconds after a
  first update that warms up, so that no cache is needed (R8)

### Documentation and checks

- [x] Describe in `README.md` what a press and the key read with a selection, with a caret and in a preview, with a
  code block, a table and the number of an item, the three tooltips, and the line the tool window shows (R14)
- [ ] Look at the change in the sandbox IDE as `AGENTS.md` says: with `./gradlew runIde`, capture only the window of
  the IDE, and look at the three tooltips on the tab and at "Selection" and "From the caret" in the tool window (R8,
  R10)
- [x] Write `developer-checks.md` with the checks of R13: on Linux x64, read a selection, read from a caret in the
  middle of a paragraph, read from a caret with the key, read a file shown only as a preview, look at the tool window
  during each, and read with several carets and with a block selection over several lines, pausing, resuming and
  stopping each and pressing again; on macOS on Apple Silicon, read one selection and read from one caret (R13)

## Verification

- [ ] `./gradlew test` passes
- [ ] The tests with the real engine pass with the model in place, and are not skipped
- [ ] `./gradlew buildPlugin` builds the archive and passes its size check
- [ ] `npm run check-changes` passes on the committed branch
- [ ] The record holds the developer's findings for R13, on both machines, and for the several carets and the block
  selection what was read and whether anything failed

## Trace

- R1: choosing the part, the action reads the part
- R2: sentences, choosing the part, the action reads the part
- R3: choosing the part, the action reads the part
- R4: placed parts, choosing the part
- R5: choosing the part, the action reads the part
- R6: the action reads the part
- R7: the action reads the part
- R8: the action reads the part, the look in the sandbox IDE
- R9: placed parts, choosing the part, the action reads the part
- R10: the reading knows its part, the tool window, the action reads the part, the look in the sandbox IDE
- R11: the action reads the part
- R12: the tests of every section but the documentation and the checks
- R13: the developer's checks
- R14: the documentation

## Commit Points

- [ ] 1. `feature-0013 Plan reading from the caret or the selection`: this plan and its log entry
- [ ] 2. `feature-0013 Keep the place in the file of what is read`: `PlacedPart.kt`, `SpokenMarkdown.kt` and
  `PlacedPartsTest.kt`, with the result of `spokenPartsOf` unchanged
- [ ] 3. `feature-0013 Choose the selection or the sentence of the caret`: `TextPieces.kt`, `PartOfTheFile.kt` and
  their tests
- [ ] 4. `feature-0013 Show in the tool window which part is read`: `SpeechService.kt`, `ReadingPanel.kt` and the tests
- [ ] 5. `feature-0013 Read the selection or from the caret of the tab and the key`: `EditorPlace.kt`,
  `ReadAloudAction.kt` and the tests
- [ ] 6. `feature-0013 Describe reading from the caret or the selection`: `README.md` and `developer-checks.md`
- [ ] 7. `feature-0013 Record the developer's checks of reading from the caret or the selection`: `record.md`
