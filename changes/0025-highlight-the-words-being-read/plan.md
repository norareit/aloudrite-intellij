# Highlight the words being read

## Approach

The work falls into four layers. Each is tested at its own level, and only the last knows about editors.

1. **The places of the words.** These are pure and built once per reading. Today a `WhatToRead` holds only
   `SpokenPart`s, so by the time the service cuts the reading into segments, the offsets of unit 0013 are gone.
   `WhatToRead` is changed to hold the tidied `PlacedPart`s. Its `parts` are derived from them, so the text handed to
   the engine stays exactly what it is today. A new class `ReadingSegments` then cuts these placed parts into the
   segments the service numbers, which is the one place the cut is made. For each segment it gives the words, each with
   the range of the file it was read from, the range of its sentence and its weight for the estimate.
2. **The estimate.** Also pure. `HeardWordEstimate` is told the positions heard: the segment, the samples heard into
   it, and the share of the segment's audio that `ReadingTime` already computes for the bar. From these it keeps the
   word and the sentence being heard.
3. **The service.** It feeds the estimate wherever it feeds `ReadingTime`, and also when asked. It reads the
   position of the player at the moment of the question rather than at the last slice, so that the highlights follow
   the audio at the rate the editor asks for (R6), during the play-out at the end included. It exposes one snapshot
   through a narrow interface, `HeardReading`.
4. **The editor.** `ReadAlongHighlighter` is an application service on the EDT. While a reading exists it asks for the
   snapshot every 50 milliseconds, and also at every change of the reading. It maps the two ranges through the edits
   made to the document since the reading started, and puts two range highlighters on the markup model of every
   editor of the document. It also scrolls an editor that still follows the reading.

How the places are counted, decided here so that the implementer does not re-derive it:

* **The text handed to the engine.** A new `PlacedWords.forTheEngine()` replaces each character above U+FFFF with a
  space. It gives the space the offset of the first half of the pair and an end of `endOf` of the second half, so that
  the text it gives is exactly `withCharactersAboveFfffAsSpaces(text)` and every character still has its place.
  `tidied` works on `PlacedPart`s instead of `SpokenPart`s, with the same rules: empty words dropped, neighbouring
  silences merged into the longest, and silences at either end dropped. `spokenPartsFrom` becomes these two steps
  followed by a plain mapping to `SpokenPart`, so `spokenPartsOf` gives the same parts as now
* **The segments.** A new `pieceRangesOf(text)` gives the start and end of each piece that `piecesOf(text)` cuts,
  leaving out the pieces of white space only. The segments of a `PlacedWords` are its slices at those ranges, and a
  silence is one segment. `ReadingSegments.spoken` maps the segments to `SpokenPart`, and `SpeechService.Reading`
  takes its `segments` from there. The numbering is therefore the one the service has today
* **The place of a character.** It is `offsets[i]` up to `endOf(i)`. For a character of a `PlacedWords` whose
  `standsForBlock` is set and whose offset lies within that range, it is the whole range. The place of anything
  covering several characters runs from the smallest start of their places to the largest end
* **A word (R3, R4).** A word is a run of characters that are not white space, within one segment. Within a
  segment whose part stands for a block, every character whose place is the block belongs to one single word, white
  space included, so that "code example" and "see table" are one word over the whole block. The number of an item and
  the full stop that ends a list item keep their own places. "link" needs no rule of its own, since its characters
  are spread over the address and the place of the word is the whole address. A word that a selection cuts is read
  whole by `PartOfTheFile` already, so it gets its whole place
* **The weight of a word (R5)** is the number of its code points that are neither white space nor punctuation.
  Punctuation is the Unicode categories `Pc`, `Pd`, `Ps`, `Pe`, `Pi`, `Pf` and `Po`
* **A sentence (R2).** The parts of a block are grouped as `PartOfTheFile.partsOfTheBlock` groups them: words of the
  same `block`, with a silence between them that has words of the block on both sides. Their texts are joined and cut
  with `sentencesOf`. A sentence start that falls after the start of a number mark and no later than its end is moved
  to the mark's start, which is the rule `positionIn` applies today. That rule and the grouping move into a new file
  `Sentences.kt`, which `PartOfTheFile` then uses, so that both readings of "a sentence" are one piece of code. A word
  belongs to the sentence of its first character. The place of a sentence runs from the place of its first word to
  that of its last
* **The estimate (R5).** Let the weights of the words of the segment be `w₀ … wₙ` and their sum `W`. For a share
  `s` of the segment heard, the word is the first one whose running sum through itself is greater than `s × W`. Where
  there is none, which is at `s = 1`, it is the last word whose weight is above 0. Where `W` is 0 it is the first word.
  A word of weight 0, such as a lone dash, is so never chosen within a segment, except as the first word of a segment
  of weight 0. `s` is `ReadingTime`'s share of the segment heard: the seconds heard into it divided by the segment's
  seconds at the speed in use, at most 1. These seconds are the length of the audio once the segment is made in full,
  and before that the larger of the part made and the estimate from the text. This is the share the bar already uses,
  taken out of `shareRead` into a function of its own
* **Forward only (R5, R8).** The estimate keeps the segment, the samples heard into it and the word it last gave.
  A position in the same segment with at least as many samples gives the later of the new word and the last one, so
  that audio of the segment arriving late and lengthening it does not move the word back. A position in another
  segment, or with fewer samples, which is the piece played again after a resume or a change of the speed, starts
  afresh from the word the share gives
* **Silence (R7).** A position in a silence segment changes nothing, so the highlights stay as at the end of the
  piece before it. After a resume that skips the silence, the first position is in the next piece at 0 samples, which
  gives its first word (R8)
* **Before anything is heard** there are no places, and no highlight is shown. The first word is highlighted with
  the first position the playing reports, which is when its audio begins (R5)
* **The pause (R8)** stops the positions, so the places stay as they were. No rule of the estimate is needed for it

The service:

* **The reading's identity.** Each `Reading` gets a number, counted up for each reading the service starts, and a
  count of its resumes. The highlighter uses them to tell a new reading and a resume, at which an editor follows the
  reading again (R12)
* **The live position (R6).** When a playing work makes its player, it hands the player and its `PlayedAudio` to
  the `Reading`. The `Reading` drops them in `interruptedOn` before the discard, and after the play-out. A question
  for the places takes `playedAudio.positionAt(player.samplesHeard())` under the monitor of the `Reading` and passes it
  through the same `heard` that the slices use. That `heard` ignores it while stopped, or while a pause or a change has
  not been seen yet, and so does a position that arrives after a change. `PlayedAudio` becomes safe to call from two
  threads, with `@Synchronized` on its methods. `AudioPlayer.samplesHeard()` gives the position it had at the moment
  of `close` or `discard` once the line is closed, so that a question that races the end never reads the position of
  a closed line
* **The document's stamp.** `WhatToRead` gets `documentStamp: Long?`, which `ReadAloudAction` sets to the document's
  `modificationStamp` in the read action that takes its text. It is null for a file read from disk. The highlighter
  starts recording edits when it first sees the reading, which on a press is synchronously inside `read`, through
  `READING_CHANGED`. It highlights only when the stamp then still matches. A reading that starts after a download,
  during which the file was changed, therefore shows no highlights rather than wrong ones

The editor:

* **The editors (R1, R10).** At each refresh, the editors are `EditorFactory.getEditors(document)` for the cached
  document of the target's file, which is every text editor of the file in any project. The text editor of a split
  `TextEditorWithPreview` is among them. An editor opened during the reading is so found at the next refresh. Entries
  of disposed editors are dropped
* **The edits (R11).** A pure `TextEdits` records every `DocumentEvent` of the document from the first refresh of the
  reading on. It maps a range of the text of the press to the text now, as a non-greedy range marker moves:
    * A start before an edit stays, one at or after its old end shifts by the change in length, and one within the
      replaced text goes to the end of the replacement
    * An end at or before an edit stays, one at or after its old end shifts, and one within the replaced text goes to
      the edit's offset
    * A range that comes out empty, because all its characters were deleted, gives null and is not highlighted
* **The highlighters (R13, R14).** Two `RangeHighlighter`s per editor are added with `editor.markupModel`,
  `HighlighterTargetArea.EXACT_RANGE`, and the layers `HighlighterLayer.SELECTION - 2` for the sentence and
  `HighlighterLayer.SELECTION - 1` for the word, so that the selection is drawn over both and the word over the
  sentence. Each gets `TextAttributes` with only a background colour, taken from
  `editor.colorsScheme.getAttributes(key).backgroundColor`. This keeps the text in its own colours and puts no mark
  on the error stripe. The two keys are `ALOUDRITE_HEARD_SENTENCE` and `ALOUDRITE_HEARD_WORD`, made with
  `TextAttributesKey.createTextAttributesKey(name, fallback)`. The fallbacks to begin with are
  `EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES` for the sentence and `EditorColors.SEARCH_RESULT_ATTRIBUTES` for the
  word. The implementer keeps or changes them by the captures of R17, and names the chosen two in `record.md`, as the
  spec's decision asks. A highlighter is replaced only when its mapped range differs from the one it covers now.
  Nothing is put on the document's markup model, and nothing changes the document, so the file and its undo history
  are untouched (R14)
* **The end (R9).** A refresh that finds no reading, another reading, or the box cleared (R15), removes every
  highlighter of the plugin from every editor it put one on, disposes the document listener and the editor listeners,
  and forgets them. `READING_CHANGED` refreshes with `invokeLater` in `ModalityState.any()`, or at once where it is
  published on the EDT, so a stop, an end or a failure clears the highlights within one event of the EDT. The timer is
  a `javax.swing.Timer` of 50 milliseconds, started by a refresh that finds a reading and stopped by one that finds
  none
* **The scrolling (R12).** A pure `Following` per editor holds whether the editor follows the reading. It is true
  at the start of a reading and at every resume. A user's scroll or caret move makes it false. While it is true and the
  word lies outside `editor.scrollingModel.visibleArea`, the highlighter scrolls to the word's start with
  `ScrollType.CENTER`, with the animation disabled around the call and a flag set during it. The visible area listener
  of the editor counts a change of the origin of the visible area outside that flag as the user's scroll, and a change
  of its size alone as no scroll. Its caret listener counts every caret move. A move of the caret that an edit in
  another editor of the same file causes therefore also stops the following in this editor. That case is rare and
  errs on the side of leaving the user's view alone. The caret and the selection are never touched
* **The checkbox (R15).** A new application service `HighlightSetting`, stored in `aloudrite.xml` under the name
  `AloudRiteHighlights`, holds `highlights: Boolean`, which is true until it is cleared. The page "AloudRite" gets a
  checkbox "Highlight the words being read" in a row under the speed and above the group of the models, wired to
  `isModified`, `apply` and `reset` like the speed. The highlighter reads the setting at every refresh, so a change
  applied during a reading shows within 50 milliseconds, and the reading goes on untouched
* **Registration.** An application listener of `READING_CHANGED` in `plugin.xml`, `ReadAlongListener`, calls the
  highlighter's `readingChanged()`. The highlighter takes `HeardReading`, the setting and nothing else from its
  constructor, with defaults from the services, so that a test builds it on the stand-ins and calls `refresh()` itself

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/Sentences.kt`: new. Holds `partsOfTheBlock` and the sentence starts of a
  block with the rule of the number of an item, both moved out of `PartOfTheFile.kt`
- `src/main/kotlin/com/ritenoar/aloudrite/PartOfTheFile.kt`: uses `Sentences.kt`. `WhatToRead` holds
  `placedParts`, `partOfTheFile` and `documentStamp`, and gives `parts` derived from them. `whatToRead` gains
  `documentStamp: Long? = null`
- `src/main/kotlin/com/ritenoar/aloudrite/PlacedPart.kt`: `PlacedWords.forTheEngine()`
- `src/main/kotlin/com/ritenoar/aloudrite/SpokenMarkdown.kt`: `tidied` on `PlacedPart`, plus `spokenPartOf`, and
  `spokenPartsFrom` built from them
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSegments.kt`: `pieceRangesOf`, the class `ReadingSegments` and
  `HeardWord`. Replaces `segmentsOf(SpokenPart)`
- `src/main/kotlin/com/ritenoar/aloudrite/HeardWordEstimate.kt`: new. Holds `HeardPlaces`, `HeardWordEstimate` and
  `wordAtShare`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingTime.kt`: `shareOfTheSegmentHeard()`, taken out of `shareRead`
- `src/main/kotlin/com/ritenoar/aloudrite/TextEdits.kt`: new
- `src/main/kotlin/com/ritenoar/aloudrite/Following.kt`: new
- `src/main/kotlin/com/ritenoar/aloudrite/PlayedAudio.kt`: `@Synchronized` methods
- `src/main/kotlin/com/ritenoar/aloudrite/AudioPlayer.kt`: the position kept at `close` and `discard`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: `HeardReading`, `HeardState`, the reading's number and
  resumes, the live position, the estimate fed from `heard`, and the segments from `ReadingSegments`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: passes the document's stamp to `whatToRead`
- `src/main/kotlin/com/ritenoar/aloudrite/HighlightSetting.kt`: new
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurable.kt`: the checkbox
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighter.kt`: new. Holds the service, the two
  `TextAttributesKey`s and `ReadAlongListener`
- `src/main/resources/META-INF/plugin.xml`: the application listener
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: `wholeFile(parts)` builds placed words from spoken
  parts, with each character at its own index, plus a helper `placedWhatToRead(markdown)`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: builds its `WhatToRead` with the helper
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTimeMeasurement.kt`: cuts with `ReadingSegments`
- `src/test/kotlin/com/ritenoar/aloudrite/EditorSupport.kt`: `editorWithPreviewOf(file)`, a split
  `TextEditorWithPreview`
- `src/test/kotlin/com/ritenoar/aloudrite/PlacedPartsTest.kt`: tests of `forTheEngine`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSegmentsTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/HeardWordEstimateTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTimeTest.kt`: tests of `shareOfTheSegmentHeard`
- `src/test/kotlin/com/ritenoar/aloudrite/TextEditsTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/FollowingTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/AudioPlayerTest.kt`: the position after `close` and `discard`
- `src/test/kotlin/com/ritenoar/aloudrite/HeardReadingServiceTest.kt`: new. Covers the places through `SpeechService`
  with the stand-ins
- `src/test/kotlin/com/ritenoar/aloudrite/HighlightSettingTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurableTest.kt`: the checkbox
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighterTest.kt`: new. Covers the highlights in editors with the
  stand-ins
- `README.md`: the highlights, the estimate, the scrolling and the checkbox
- `changes/0025-highlight-the-words-being-read/record.md`: the captures, the colour keys chosen, and the developer's
  check

## Tasks

- [x] Move `partsOfTheBlock` and the sentence starts of a block into `Sentences.kt`. The sentence starts are
  `sentencesOf` of the joined texts, with a start after a number mark's start and no later than its end moved to the
  mark's start. Make `startOfTheSentence` take the last of those starts at or before the hit, and `positionIn` drop
  its own number rule. This changes no behavior, so run `./gradlew test` and see `PartOfTheFileTest` and
  `ReadFromTheCaretTest` pass unchanged (R2)
- [x] Commit: `feature-0025 Move the sentences of a block out of the part of the file`
- [x] Add tests to `PlacedPartsTest` of `PlacedWords.forTheEngine()`:
    * its text equals `withCharactersAboveFfffAsSpaces` of the text
    * the space for an emoji has the offset of the emoji's first half and reaches past its second half
    * words without such characters come back unchanged

  Then write `forTheEngine`. Make `tidied` work on `PlacedPart`, and add `spokenPartOf`. Change `WhatToRead` to hold
  `placedParts`, tidied and made for the engine, with `parts` derived. Add
  `pieceRangesOf` and build the service's segments from the slices of the placed parts at those ranges, replacing
  `segmentsOf(SpokenPart)`. Update `wholeFile` in `SpeechStandIns`, `ReadingPanelTest` and `ReadingTimeMeasurement` to
  the new types. The text handed to the engine does not change, so `SpokenMarkdownTest`, `PartOfTheFileTest`,
  `TextPiecesTest`, `SpeechServiceTest` and the rest pass unchanged, and so do the tests with the real engine, run with
  the model in place (R2, R3)
- [x] Commit: `feature-0025 Keep the place in the file of every character of a reading`
- [ ] Write `ReadingSegmentsTest` on the parts of `whatToRead` of short Markdown texts. The places are given as
  offsets found in the text, so that each check reads as the characters it means. It covers:
    * the segments equal what the service cut before, for `samples/markdown-features.md` read whole
    * a word of bold or italic text covers its letters and not the asterisks around it
    * a word of a link with text covers its letters
    * a selection that cuts a word gives the whole word
    * an escaped character and an entity have a place
    * an emoji splits two words and is in neither
    * "code example" is one word covering the whole fenced code block, and its sentence is that block
    * "see table" is one word covering the whole table
    * "link" is one word covering the address, and its sentence is that of the paragraph around it
    * a sentence of two pieces cut at a comma is one sentence covering both
    * two sentences of one paragraph are two
    * a sentence with inline code in it is one sentence across the silences
    * the number of an item of an ordered list belongs to the item's first sentence, also in a reading from the caret
      and a selection
    * the full stop added to a list item belongs to its last word
    * the weights leave out white space and punctuation, and count digits and symbols such as ✓

  Then write `ReadingSegments` and `HeardWord` until it passes (R2, R3, R4)
- [ ] Write `HeardWordEstimateTest` on `ReadingSegments` of short texts. It covers:
    * no places before any position
    * the first word at a share of 0, and the last word of weight above 0 at a share of 1
    * a share between two running sums gives the word after the lower one
    * a comma or a dash does not shift the word
    * a segment of weight 0 gives its first word
    * within one segment, a later position with a lower share, as when more audio of the segment arrives, keeps the
      word
    * a position with fewer samples in the same segment, and a position in an earlier segment, start afresh at the word
      of their share
    * a position in a silence keeps the word and the sentence of the piece before it
    * the first position after the silence gives the first word of the next piece
    * the places are the word's and the sentence's

  Then write `HeardWordEstimate` and `wordAtShare` until it passes (R5, R7, R8)
- [ ] Add tests to `ReadingTimeTest` of `shareOfTheSegmentHeard()`:
    * 0 before anything is heard
    * the share of a segment made in full
    * the larger of the part made and the estimate for one made in part
    * at most 1
    * unchanged by a change of the speed, as the seconds heard are kept by their share

  Then take it out of `shareRead`, which uses it, and see the rest of `ReadingTimeTest` pass unchanged (R5)
- [ ] Commit: `feature-0025 Estimate the word being heard from the share of its piece`
- [ ] Write `TextEditsTest`. It covers:
    * no edit leaves a range as it is
    * an insertion before a range shifts it
    * one after leaves it
    * one inside widens it
    * one at its start shifts it and one at its end leaves it
    * a deletion of part of it shrinks it
    * a deletion of all of it, and a replacement of all of it, give null
    * edits in a row are applied in their order

  Then write `TextEdits` (R11)
- [ ] Write `FollowingTest`. It covers:
    * following at the start
    * not following after a user's scroll and after a caret move
    * a scroll made by the highlighter itself, and a change of size alone, are no user's scroll
    * following again after a resume and after a new reading

  Then write `Following` (R12)
- [ ] Commit: `feature-0025 Map the places through the edits and decide when an editor follows`
- [ ] Add tests to `AudioPlayerTest`: after `close`, and after `discard`, `samplesHeard()` gives the frame position
  the line had at that moment, also when the line's own position changes afterwards. Then keep it in `AudioPlayer`
  (R6, R9)
- [ ] Write `HeardReadingServiceTest` with `SpeechStandIns`, where a text makes 50 samples in two runs of 25 and a slice
  is 10 samples, and with `whilePlaying` to ask `heardReading()` during the playing. It covers:
    * null while nothing reads
    * the target, the number and the document's stamp of the reading
    * no places before the first slice
    * the first word of the first piece at the first slice
    * the word moving forward slice by slice to the last word of the piece
    * the places staying through the silence between two paragraphs
    * the first word of the next piece once its audio is heard
    * `heardReading()` reading the player's position at the call, by changing `playerLag` inside `whilePlaying` and
      asking twice
    * the places unchanged while paused, also when the making work runs on
    * at a resume, the count of resumes rising and, with the first slice played again, the first word of the piece
    * a resume during the silence giving the first word of the next piece
    * after `changeSpeed` during a piece, the old places until the new making plays, and then the first word of that
      piece
    * null once the reading ended, was stopped, was replaced by another reading, and failed
    * a new number for the next reading (R5, R6, R7, R8, R9)
- [ ] Make `PlayedAudio`'s methods `@Synchronized`. Number the readings and count their resumes. Give each `Reading`
  its `ReadingSegments` and a `HeardWordEstimate`. Feed the estimate from `heard` with the share of
  `shareOfTheSegmentHeard()`. Hand the player and its `PlayedAudio` to the `Reading` in `playUntilAnInterruption`, and
  drop them in `interruptedOn` before the discard and after the play-out. Add `HeardReading` with
  `heardReading(): HeardState?` to `SpeechService`, taking the live position under the monitor of the `Reading`. Run
  until `HeardReadingServiceTest`, `SpeechServiceTest` and `ReadingTimeServiceTest` pass (R5, R6, R7, R8, R9)
- [ ] Add a test to `ReadAloudActionTest` that a press reads with the `modificationStamp` of the file's document,
  then pass the stamp through `whatToRead` in `ReadAloudAction` (R11)
- [ ] Commit: `feature-0025 Give the places being heard from the reading`
- [ ] Write `HighlightSettingTest`: true until set, false after it is cleared, kept through `getState` and
  `loadState`, and true for a state stored before the setting existed. Then write `HighlightSetting`, registered as an
  application service through `@Service` and stored in `aloudrite.xml` (R15)
- [ ] Add tests to `ReadingSpeedConfigurableTest`:
    * the page shows a checkbox "Highlight the words being read" under the speed and above the models
    * it is checked with the setting at its default
    * clearing it makes the page modified
    * `apply` stores the cleared box
    * `reset` shows the stored value

  Then add the checkbox and its constructor parameter, and update the construction of the page in the existing tests
  (R15)
- [ ] Commit: `feature-0025 Add the checkbox that turns the highlights off`
- [ ] Write `ReadAlongHighlighterTest` with `ApplicationRule`, `EditorsOfATest` and `SpeechStandIns`. It reads a
  Markdown file of two paragraphs, one with a comma clause, a code block and a link, pressed through
  `readAloudActionOn`, and calls `refresh()` on the EDT from `whilePlaying`. It covers:
    * two editors of the file, and the text editor of a split `TextEditorWithPreview`, each showing one sentence
      highlighter and one word highlighter on the expected ranges
    * the word highlighter moving on while the sentence one stays within a sentence
    * "code example" highlighted on the whole block in both highlighters
    * both layers below `HighlighterLayer.SELECTION`, the word's above the sentence's
    * attributes with a background only
    * keys with a fallback
    * nothing before the first slice
    * an editor made during the reading showing the highlights after the next refresh and not before
    * text inserted before the sentence during the reading, with the highlights on the same characters at their new
      offsets
    * the word heard next deleted during the reading, with no word highlighted while it is heard and the reading going
      on to its end
    * after the end of the reading, a stop, a reading of another file and a failure of the engine, no highlighter of
      the plugin in any editor once the events of the EDT are dispatched after `readingChanged()`
    * the document's text and modification stamp unchanged, apart from the edits the test made
    * a reading after a download, whose document changed between the press and the start, showing no highlights
    * with the setting cleared during the reading, no highlights at the next refresh and the reading going on, and
      with it checked again, highlights at the next refresh
    * the timer's delay at most 100 milliseconds, the timer running while a reading exists and stopped after it
      (R1, R4, R6, R9, R10, R11, R13, R14, R15)
- [ ] Write `ReadAlongHighlighter`, the two keys and `ReadAlongListener`, and register the listener in `plugin.xml`.
  Add `editorWithPreviewOf` to `EditorSupport.kt`. Run until `ReadAlongHighlighterTest` passes, then run the whole
  `./gradlew test` (R1, R6, R9, R10, R11, R12, R13, R14, R15)
- [ ] Commit: `feature-0025 Highlight the sentence and the word being heard in the editors of the file`
- [ ] Start the sandbox IDE with `./gradlew runIde` and open `samples/markdown-features.md` in the text editor. Read
  it, and capture only the window whose title ends in `[aloudrite-intellij]` with `xwd -id <window id>`, as `AGENTS.md`
  says, in the light theme and in Darcula. Take these captures:
    * a paragraph being read
    * the code block being read
    * a selection over a highlighted sentence
    * the editor after it scrolled by itself
    * the page Settings › Tools › AloudRite with the checkbox

  Check that the word stands out from the sentence, that the text stays readable, that the selection and the caret
  show over both, and that the editor stops following after a scroll of the user. Change the fallback keys where the
  captures show the need, and write in `record.md` what the captures showed and which two keys were chosen. Where the
  host has no display, log that the captures could not be made (R12, R13, R15, R17)
- [ ] Commit: `feature-0025 Record the look of the highlights`
- [ ] Describe in `README.md`, after the paragraph on what is read and before the tool window, the two highlights in
  every text editor of the file. Say:
    * that the sentence is the whole sentence and the word an estimate from the share of the audio of the piece heard,
      over the characters that are neither white space nor punctuation
    * what "code example", "see table" and "link" highlight
    * that the highlights stay through a silence and a pause, and go back to the first word of the piece at a resume
      and a change of the speed
    * that they follow edits and vanish at the end
    * that the editor scrolls by itself without moving the caret, stops at a scroll or caret move of the user in that
      editor, and follows again from the next resume or reading
    * that a reading started after a download during which the file changed shows none
    * that the colours follow the colour scheme
    * that the preview shows none in this version
    * the checkbox "Highlight the words being read" under Settings › Tools › AloudRite

  Mention the checkbox also where the README describes the page Settings › Tools › AloudRite (R19)
- [ ] Commit: `feature-0025 Describe the highlights in the README`

## Verification

- [ ] `./gradlew test` passes, with the tests with the real engine run and not skipped, since the unit changes how
  the text handed to the engine is made, even though it leaves the text the same
- [ ] `./gradlew buildPlugin` builds the archive within its size limit
- [ ] `npm run check-changes` passes on the committed branch
- [ ] Every requirement has a task:
    * R1 to R15: the tests and code above
    * R16: the plain-value tests in `ReadingSegmentsTest`, `HeardWordEstimateTest`, `TextEditsTest` and
      `HeardReadingServiceTest`, and the editor tests in `ReadAlongHighlighterTest`, `HighlightSettingTest` and
      `ReadingSpeedConfigurableTest`, none of which needs the model, an audio device or the network
    * R17: the captures
    * R18: the developer's check
    * R19: the README
- [ ] Requirement 18 is left to the developer: reading `samples/markdown-features.md` on Linux x64 with the text
  editor in view, at 1.0× and at 1.5×, and recording in `record.md` how well the strong highlight kept up with the
  voice

## Commit Points

- [x] `feature-0025 Move the sentences of a block out of the part of the file`
- [x] `feature-0025 Keep the place in the file of every character of a reading`
- [ ] `feature-0025 Estimate the word being heard from the share of its piece`
- [ ] `feature-0025 Map the places through the edits and decide when an editor follows`
- [ ] `feature-0025 Give the places being heard from the reading`
- [ ] `feature-0025 Add the checkbox that turns the highlights off`
- [ ] `feature-0025 Highlight the sentence and the word being heard in the editors of the file`
- [ ] `feature-0025 Record the look of the highlights`
- [ ] `feature-0025 Describe the highlights in the README`
