# Read Markdown without its markers

## Approach

The press stops handing the engine a string and hands it a list of spoken parts instead: words, which the engine
makes into audio as before, and silences of a set length, which the plugin puts into the audio itself (decision
"A pause is silence that the plugin adds"). The Markdown is parsed into its blocks and inline elements, and the
parts are worked out from that tree by one pure function, so that every rule of the spec is unit tested with plain
values and, through the stand-in engine that the tests already have, as what the engine is given and what is
played (R21).

This plan is written against the branch `feature-0010-read-markdown-without-its-markers`, cut from `master` at
`a938e17`, on which unit 0006 is implemented. On that tree `ReadAloudAction.actionPerformed` takes the text of the
document, turns characters above U+FFFF into spaces with `withCharactersAboveFfffAsSpaces`, shows the notification
when the result is blank, and hands the string through `Readings.read` and `Readings.readOnceTheModelIsInPlace` to
`SpeechService`, whose `makeTheAudio` cuts it with `piecesOf` and hands each piece that is not blank to the engine.
`./gradlew test` on that tree ran 201 tests: none failed, and two, `RealModelPackageTest` and
`EngineThreadsMeasurement`, were skipped.

### The parser

The plugin bundles commonmark-java 0.30.1 (`org.commonmark:commonmark`, BSD-2-Clause) from Maven Central, with four
of its extensions of the same version: `commonmark-ext-gfm-strikethrough`, `commonmark-ext-gfm-tables`,
`commonmark-ext-task-list-items` and `commonmark-ext-autolink`, which brings `org.nibor.autolink:autolink` 0.12.0
(MIT). Together they are about 300 KB, far below the archive limit in `build.gradle.kts`. It is a CommonMark parser,
so what counts as an HTML comment, also one that is not closed, is what CommonMark says (R16). It is not a class of
the IDE's Markdown plugin (decision of unit 0003), and both licenses allow shipping it in a plugin under
GPL-3.0-or-later. The IDE 2026.2.3 does not carry `org.commonmark` classes of its own, so there is no second copy to
clash with. The strikethrough extension is built with `requireTwoTildes(true)`, since R3 is about `~~words~~` and a
single tilde is left to the engine as it is.

The parser is built once, with `includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)`, which the rule of R7
below needs.

### What each kind of Markdown becomes

The function walks the tree and gives a list of `SpokenPart`, which is `Words(text)` or `Silence(milliseconds)`:

* **Front matter (R15)**: taken off the text before it is parsed, by a pure function of its own. When the first line
  is `---`, the text up to and including the next line that is `---` or `...` is left out. Without such an end, or
  without `---` on the first line, the text is left as it is. A line counts as `---` or `...` when it is that after
  its trailing white space, a carriage return included, is taken off. The commonmark-java extension for YAML front
  matter is not used: it accepts only lines of YAML keys, and R15 leaves out whatever stands between the two lines.
* **Paragraph**: its inline content as words. A soft and a hard line break become `\n`, as the engine was given
  them in unit 0006.
* **Heading (R1)**: its inline content as words, without the `#`, at every level, ATX and setext alike.
* **Emphasis and strong emphasis (R2)**: their content, without the delimiters, asterisks and underscores alike.
  The underscores were not read out in unit 0006, so leaving them out is meant to sound the same; the listening of
  R22 checks it in the section "Bold and italic".
* **Strikethrough (R3)**: nothing, neither the tildes nor the words.
* **Text and escaped characters (R14)**: the literal that the parser gives, in which the backslash of an escape is
  gone and the character after it stays. An entity such as `&amp;` reaches the engine as the character it stands for.
* **Link (R6, R7, R8)**: a link whose source begins with `[` gives its text, written as it is or as a reference,
  `[text][label]`, `[text][]` or `[text]`. Any other link is an address that the parser recognised on its own, in
  angle brackets or bare through the autolink extension, e-mail addresses included, and gives the word `link`. Which
  of the two it is, is read from the character of the Markdown at `sourceSpans.first().inputIndex` of the link. The
  Markdown here is the text after the front matter is taken off, which is what was parsed.
* **Link reference definition (R8)**: nothing.
* **Image (R9)**: the text of its children, which is the alternative text, with the same rules for what is inside
  it. An image without alternative text gives nothing.
* **Inline code (R10)**: `Silence(250)`, its literal as words, as the engine reads it, `Silence(250)`.
* **Code block, fenced with backticks or tildes, or indented (R11)**: the words `code example`, then
  `Silence(2000)`. Neither the fence, nor the language, nor the code.
* **Table (R12)**: the words `see table`. Neither the cells nor the separator line.
* **Horizontal rule (R13)**: `Silence(500)`.
* **HTML comment (R16)**: an inline comment, an `HtmlInline` whose literal begins with `<!--`, gives nothing. Of an
  `HtmlBlock`, every comment is taken out, from `<!--` to the next `-->`, or to the end of the block where there is
  none, and what is left is handed to the engine as it is, as the non-goals say of other HTML; where nothing but
  white space is left, the block gives nothing at all, and counts as no block for the pauses below. Other inline HTML
  is handed to the engine as it is. Code and code blocks are never looked into for comments, since the parser has
  already made them code.
* **Block quote (R18)**: its blocks, each by these rules, without the `>`.
* **List (R4, R5)**: each item gives its content: the number and delimiter of the source first in a numbered list, as
  `1. ` or `1) ` from `markerStartNumber` plus the place of the item and `markerDelimiter`, so that the engine is given
  the number as in unit 0006; no marker in a bulleted list; and nothing for the box of a task list,
  `TaskListItemMarker`, nor for its state. The blocks of an item, a nested list's items among them, follow each other
  inside the list with `Silence(250)` between each two of them, and so do the items of a list, in place of the silence
  between blocks (R4, as corrected at 2026-10-19T20:26:00Z, see task 15). The last words of each item and of each
  paragraph inside one are ended as a sentence: a full stop is added at the end of those words unless they already end
  with `.`, `!` or `?`, looked at before the closing marks that `TextPieces` already skips, `"`, `'`, `)`, `]` and `*`.
  An item that gives no words gets no full stop. A code block or table inside an item gives its words and its silence as
  anywhere else.
* **Characters above U+FFFF**: the text of every `Words` goes through `withCharactersAboveFfffAsSpaces` at the end,
  after parsing, so that a space put in place of an emoji at the start of a line cannot change the structure, as
  four of them would by making an indented code block.

### The pauses

The human settled at 2026-10-19T19:57:17Z, see `record.md`, what R17 leaves open: every two blocks that follow each
other get `Silence(500)` between them, heading, paragraph, list, quote, table, rule and code block alike, with the two
exceptions that the spec sets itself: the items of one list, and the blocks inside an item, have the end of a sentence
and `Silence(250)` between them (R4, as corrected at 2026-10-19T20:26:00Z), and after `code example` the silence is two
seconds in all, not two and a half. The blocks inside a quote are blocks like any others. A block that gives nothing,
such as a link reference definition, a comment or a struck out paragraph, leaves no pause of its own (R15, R16).

The silences are tidied in one place at the end: two or more silences next to each other become one, the longest
of them, which makes the two seconds after `code example` and a rule between two paragraphs a single half second,
"as long as the pause between two paragraphs" (R13); a silence at the start or at the end of the list is left out,
since nothing is read before or after it; and `Words` whose text is empty are left out. A silence next to a `Words`
of white space only stays, since that is where an emoji or a space stood.

The engine makes its own pause at the end of a sentence, so a list item ended with a full stop and a new piece cut by
`piecesOf` keep the pauses that unit 0006 has.

### The reading

`Readings.read` and `Readings.readOnceTheModelIsInPlace` take `parts: List<SpokenPart>` in place of `text: String`,
and `SpeechService.Reading` keeps the parts. `makeTheAudio` goes through the parts in order: of `Words`, it cuts the
text with `piecesOf` and hands every piece that is not blank to the engine, as now; of `Silence`, it puts
`FloatArray(sampleRate * milliseconds / 1000)` of zeros into the `AudioAhead`, and ends the loop when the put answers
false. The stop is looked at before every part as it is before every piece now, and the player plays a silence in
slices of a tenth of a second like any audio, so the stop still works within a second during two seconds of
silence (R19).

`ReadAloudAction.actionPerformed` makes the parts with `spokenPartsOf(textAtThePress(file))` in place of
`withCharactersAboveFfffAsSpaces(...)`, and shows the notification that there is nothing to read when no part is
`Words` with text that is not blank. A file of nothing but front matter, comments or struck out text reads nothing;
a file of nothing but a code block reads `code example` (R19). The document is only read, never written (R20).

### The trace

| Requirement | Tasks |
|-------------|-------|
| R1 | 7, 8 |
| R2 | 7, 8 |
| R3 | 7, 8 |
| R4 | 4, 7, 8, 15 |
| R5 | 7, 8, 15 |
| R6 | 7, 8 |
| R7 | 7, 8 |
| R8 | 7, 8 |
| R9 | 7, 8 |
| R10 | 7, 8 |
| R11 | 7, 8 |
| R12 | 7, 8 |
| R13 | 7, 8 |
| R14 | 7, 8 |
| R15 | 5, 6, 7, 8, 10 |
| R16 | 7, 8, 10 |
| R17 | 7, 8 |
| R18 | 7, 8 |
| R19 | 1, 2, 9, 10 |
| R20 | 9, 10 |
| R21 | 1, 7, 9 |
| R22 | 11, 14 |
| R23 | 12, 13, 15 |

Task 3 is the build change that tasks 7 and 8 need, and traces to R1 to R18 through them.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/SpokenPart.kt`: new, the sealed interface `SpokenPart` with `Words` and
  `Silence`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: `Readings` and `Reading` take the parts, and
  `makeTheAudio` makes a silence as zeros
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: makes the parts with `spokenPartsOf`, and the
  notification looks at the parts
- `src/main/kotlin/com/ritenoar/aloudrite/TextPieces.kt`: the test of a sentence end becomes a function that
  `SpokenMarkdown.kt` uses as well
- `src/main/kotlin/com/ritenoar/aloudrite/FrontMatter.kt`: new, `withoutFrontMatter(markdown)`
- `src/main/kotlin/com/ritenoar/aloudrite/SpokenMarkdown.kt`: new, `spokenPartsOf(markdown)`, the parser, the pause
  lengths and the words of the plugin
- `build.gradle.kts`: the dependencies on commonmark-java and its four extensions
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: what was heard, as texts and silences
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`: reads parts, and silence is played as zeros
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: what a press on Markdown hands the engine, and
  what reads nothing
- `src/test/kotlin/com/ritenoar/aloudrite/TextPiecesTest.kt`: the sentence end as a function of its own
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadFlowTest.kt`: a press on `# Title` reads `Title`
- `src/test/kotlin/com/ritenoar/aloudrite/FrontMatterTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/SpokenMarkdownTest.kt`: new, each kind of Markdown through the stand-in
  engine
- `samples/markdown-features.md`: front matter, HTML comments and a numbered list without full stops, to listen to
- `docs/reading-markdown.md`: what the plugin does after this unit, for each kind
- `README.md`: no longer says that the markers are read; the parser in the archive; the samples
- `THIRD-PARTY.md`: commonmark-java, its extensions and autolink-java
- `changes/0010-read-markdown-without-its-markers/record.md`: the log and what the human heard

## Tasks

Each task names the requirements it delivers. The test of a task comes before its code.

### Silence in the reading

- [x] 1. In `SpeechStandIns`, add `heard(): List<String>`, which reads `samplesPlayed()` in order and gives each
      run of samples of value 0 as `"<silence N ms>"`, N being the length of the run at `STAND_IN_SAMPLE_RATE`, and
      each run of the other values as the texts the engine made them from, joined in order with nothing between, so
      that the cuts of `piecesOf` do not show. Keep `textsMade` as it is for the tests that look at the pieces. In
      `SpeechServiceTest`, change `read` to hand `listOf(Words(text))`, and add the cases: the parts
      `Words("One."), Silence(500), Words("Two.")` are heard as `"One."`, `"<silence 500 ms>"`, `"Two."`; a silence
      is played in slices of a tenth of a second like other audio; a stop during a silence of two seconds plays no
      further slice and discards the player; a silence as the only part plays it and ends the reading. In
      `ReadAloudActionTest`, nothing changes yet. The tests do not compile until task 2 (R19, R21)
- [x] 2. Add `SpokenPart.kt` with `sealed interface SpokenPart`, `data class Words(val text: String)` and
      `data class Silence(val milliseconds: Int)`. Change `Readings.read`, `Readings.readOnceTheModelIsInPlace`,
      `SpeechService.Reading` and `ReadAloudAction` from `text: String` to `parts: List<SpokenPart>`. In
      `makeTheAudio`, go through the parts as "The reading" above says, with a function of its own for one part so
      that the loop stays at one level. In `ReadAloudAction`, hand `listOf(Words(text))` for now, with the text and
      the blank check as they are, so that every test of the action passes unchanged. Run `./gradlew test`: all pass
      (R19)

### Commit point A: `feature-0010 Make a silence part of the reading`

### The sentence end

- [x] 3. In `build.gradle.kts`, add `implementation` dependencies on `org.commonmark:commonmark`,
      `commonmark-ext-gfm-strikethrough`, `commonmark-ext-gfm-tables`, `commonmark-ext-task-list-items` and
      `commonmark-ext-autolink`, all `0.30.1`, with the version in one `val` as `sherpaOnnxVersion` is. Run
      `./gradlew buildPlugin` and check that the archive holds the five jars and `autolink-0.12.0.jar`, and still
      passes its size check
- [x] 4. In `TextPiecesTest`, add cases for a public `endsWithASentenceEnd(text: String): Boolean`: true for `Done.`,
      `Done!`, `Done?`, `"Done."`, `(Done.)` and `*Done.*`, false for `Done`, `Done,`, `Done:` and the empty text.
      Then in `TextPieces.kt`, make `endsASentence(text, whiteSpaceStart)` call it on the text before the white
      space, so that the rule of what ends a sentence is held once. `piecesOf` gives the same pieces as before, and
      its tests pass unchanged (R4)

### Commit point B: `feature-0010 Add the Markdown parser and hold the sentence end once`

The dependency and the extraction change no behavior, which is why they share a commit apart from the next one.

### Front matter

- [x] 5. Write `FrontMatterTest`: front matter ended by `---` and by `...` is taken off with both its lines, and the
      text after it is kept exactly; `\r\n` line ends are handled alike; a `---` on the first line with no end leaves
      the text as it is; a `---` that is not on the first line leaves the text as it is; a file of nothing but
      front matter gives the empty text; a closing line with trailing spaces counts; the `---` inside the front
      matter is the first one after the opening line, so a second block further down stays (R15)
- [x] 6. Write `FrontMatter.kt` with `withoutFrontMatter(markdown: String): String` as "What each kind of Markdown
      becomes" says. Run `FrontMatterTest` (R15)

### Each kind of Markdown

- [x] 7. Write `SpokenMarkdownTest`, each case pressing the Markdown through the stand-in engine:
      `SpeechStandIns().service.read(target, spokenPartsOf(markdown), ...)`, `runKeptWorks()`, then `heard()`. Keep
      every case below 3,000 samples of audio, the limit of the `AudioAhead` at the stand-in's sample rate, since
      the kept works run one after the other: a text is 50 samples and two seconds of silence 200. The cases, each
      asserting the exact list heard:
      * headings of levels 1 to 6, ATX and setext, give their text only (R1)
      * `**bold**`, `*italic*`, `***both***`, `__bold__` and `_italic_` give the words only (R2)
      * `the ~~old~~ new value` gives `the  new value`; a paragraph of nothing but struck out text gives nothing,
        and leaves a single half second between the paragraphs around it (R3, R17)
      * a bulleted list with `-`, `*` and `+`, a nested list, items with and without `.`, `!` and `?`, and an item
        ending in `."` or `)`: every item ends with exactly one sentence end, with no silence between the items;
        a numbered list with `1.` and with `3)` keeps its numbers; numbered items without full stops are ended with
        one (R4)
      * a task list with `[ ]`, `[x]` and `[X]` gives the items without box or state, ended as in R4 (R5)
      * an inline link, a link with a title, a reference link `[text][label]`, `[text][]` and `[text]` with their
        definitions give the text only, and the definition lines give nothing (R6, R8)
      * a bare `https://` address, a bare `www.` address, an address in angle brackets and an e-mail address in angle
        brackets give `link`; `[https://example.com](https://example.com)` gives the address, being a link with text
        (R7)
      * an image gives its alternative text; an image without it gives nothing, and leaves no pause (R9)
      * ``Call `x` now.`` gives `Call`, 250 ms, `x`, 250 ms, `now.`, each text with the spaces around it as
        the paragraph has them; inline code at the start of a paragraph
        gives the half second between blocks and not two silences (R10)
      * a code block fenced with backticks, with tildes, with a language, and an indented one, each between two
        paragraphs: `code example`, then two seconds, then the next paragraph; a code block as the last block gives
        no silence after it; a code block in a list item (R11)
      * a table between two paragraphs gives `see table`, with half a second on each side (R12)
      * `---`, `***` and `___` between two paragraphs give one half second (R13)
      * `\*`, `\#` and `\\` give `*`, `#` and `\` (R14)
      * front matter at the top is not heard and leaves no silence at the start; a file of nothing but front matter
        hears nothing; a `---` first line without end is heard as a rule and the rest as paragraphs or a setext
        heading, as CommonMark parses it (R15)
      * a comment on lines of its own between two paragraphs leaves the half second between them and nothing more; a
        comment within a sentence gives nothing; a comment that is not closed at the start of a line leaves out the
        rest of the file, and one that is not closed within a sentence is read as text, as CommonMark has it; a
        comment in inline code and in a code block is code; a file of nothing but comments hears nothing (R16)
      * two paragraphs give half a second between them; so do a heading and a paragraph, a paragraph and a list, a
        list and a quote (R17)
      * a quote, also one of two paragraphs, gives its text without `>`, with the half second between its
        paragraphs (R18)
      * an emoji at the start of a line is a space and makes no code block, and the existing cases of emoji still
        hold through the parser
- [x] 8. Write `SpokenMarkdown.kt` with `spokenPartsOf(markdown: String): List<SpokenPart>`, the parser built once,
      the three lengths as named constants with a comment that points to the decision on the lengths of the pauses,
      and the words `code example`, `see table` and `link` as named constants. Walk the tree with a visitor or a
      `when` over the node classes, as "What each kind of Markdown becomes" and "The pauses" say, and tidy the
      silences at the end in one function. Run `SpokenMarkdownTest` until it passes, without changing what a case
      asserts unless the case contradicts the spec (R1 to R18)

### The press

- [x] 9. In `ReadAloudActionTest`, change `aPressOnAFileWithTextReadsThatText` to expect `Title` and `Some text.`
      with half a second between them, and add: a press on a file of nothing but front matter, of nothing but
      comments, and of nothing but struck out text shows the notification that there is nothing to read, also
      without the model, and asks and downloads nothing; a press on a file of nothing but a code block reads
      `code example` and shows no notification; after a press, the document's text and the file on disk are what
      they were (R19, R20, R21)
- [x] 10. In `ReadAloudAction`, make the parts with `spokenPartsOf(textAtThePress(file))`, and decide that there is
      nothing to read when no part is `Words` with text that is not blank. Update the comment that says why nothing
      is asked for such a file. In `ModelDownloadFlowTest`, which presses on `# Title`, expect `Title` to be read.
      Run `./gradlew test`: all pass (R15, R16, R19, R20)

### Commit point C: `feature-0010 Read Markdown without its markers`

### The sample, the documents and the listening

- [x] 11. In `samples/markdown-features.md`, add front matter at the very top, of a title, a date and a list of
      tags; a section "Numbered list without full stops", the three items of "Numbered list" without their full
      stops; a section "Bulleted list with full stops"; and a section "HTML comments" with a comment on lines of its
      own between two paragraphs and one within a sentence. Say in the opening paragraph that the front matter above
      it is not to be heard (R22)
- [x] 12. Rewrite `docs/reading-markdown.md` so that it describes what the plugin does after this unit, for each kind
      of Markdown, with the pauses and their lengths, the words `code example`, `see table` and `link`, front matter
      and HTML comments, and keeps what was heard in unit 0006 as the history it started from. Keep its existing text
      where it still holds and suggest, rather than make, any removal beyond what the change makes untrue, as the
      kit's rules on documentation say. Put a section "Heard after unit 0010" in it for the findings of task 14 (R23)
- [x] 13. In `README.md`, replace the paragraph that says the text is read with its Markdown markers by one that says
      the markers are left out, code blocks and tables read as a word, addresses as "link", and pauses added, and
      points to `docs/reading-markdown.md`; say that a file with nothing to read includes one of nothing but front
      matter, comments or struck out text; mention commonmark-java in the archive; and describe the new sections of
      the sample. In `THIRD-PARTY.md`, add rows for commonmark-java 0.30.1 with its four extensions (BSD-2-Clause,
      https://github.com/commonmark/commonmark-java) and autolink-java 0.12.0 (MIT,
      https://github.com/robinst/autolink-java), with the licenses read at their source as the file's other entries
      were (R23)

### Commit point D: `feature-0010 Describe reading Markdown without its markers`

- [ ] 14. Build with `./gradlew buildPlugin`, start the sandbox IDE with `./gradlew runIde`, and have the human
      listen to `samples/markdown-features.md` on Linux x64, section by section, and then on macOS on Apple Silicon
      with the archive installed. Record in `record.md`, for each platform, what the human found for each section
      against `docs/reading-markdown.md`, and add it to its section "Heard after unit 0010". Where the human sets
      other pause lengths, change the constants, the tests and the documents to them, and log the human's word.
      Where the human cannot listen on one of the platforms, log that, and leave this task unticked with the reason,
      as the process says (R22)

### The pause between list items

Added after the listening on Linux in task 14, where the human found the pause between list items too short, and
after `spec.md` was corrected to R4 with a quarter of a second of silence between two items.

- [x] 15. In `SpokenMarkdownTest`, expect `Silence(250)` between two items of a list and between two blocks inside an
      item, in place of the line break that joined them. In `SpokenMarkdown.kt`, join the items of a list and the
      blocks of an item with `Silence(250)`, a named constant beside the other lengths, the same way the blocks of a
      document are joined with `Silence(500)`. Update the pauses in `docs/reading-markdown.md` and `README.md`. Run
      `./gradlew test` (R4, R5, R23)

### Commit point F: `feature-0010 Pause between list items`

### Commit point E: `feature-0010 Record what was heard`

## Verification

- [ ] `./gradlew test` passes, with no test skipped beyond the two that are skipped on `master`
- [ ] `./gradlew buildPlugin` passes its archive size check, and the archive holds the jars of commonmark-java and
      autolink-java
- [ ] In the sandbox IDE, pressing "Read Aloud" on `samples/markdown-features.md` reads it, the stop works during
      the two seconds after "code example", and pressing it on `samples/empty.md` shows the notification
- [ ] `npm run check-changes` passes
- [ ] Every requirement in the trace has its tasks ticked

## Commit Points

- [x] A: `feature-0010 Make a silence part of the reading`, after task 2
- [x] B: `feature-0010 Add the Markdown parser and hold the sentence end once`, after task 4
- [x] C: `feature-0010 Read Markdown without its markers`, after task 10
- [x] D: `feature-0010 Describe reading Markdown without its markers`, after task 13
- [x] F: `feature-0010 Pause between list items`, after task 15
- [ ] E: `feature-0010 Record what was heard`, after task 14
