# Choose which highlights are shown

## Approach

Unit 0025 reads one Boolean, `HighlightSetting.highlights`, at every refresh of `ReadAlongHighlighter`, and removes
every highlight of the editors and clears every page of the preview while it is false. The refresh runs every 50 ms
while a reading exists, so a change of the setting is seen within 50 ms. This unit replaces the Boolean with four
choices and makes the refresh decide, per view and per highlight, what is drawn. Nothing about the estimate, the
pages or the following changes, except the one rule of what a view scrolls to.

1. **The setting.** `HighlightSetting` holds one immutable `HighlightChoices`, a data class of four Booleans:
   `inTextEditor`, `inPreview`, `sentence` and `word`, with the defaults `true`, `true`, `true` and `false`. The
   highlighter reads the whole value once per refresh, so that the four are always seen together and never half of
   an apply. It is kept in a `@Volatile` property, as the Boolean is today. `HighlightChoices` also gives
   `highlightsCanBeChosen`, true when `inTextEditor` or `inPreview` is, which the settings page greys out by. The
   stored state `StoredHighlights` gets the same four fields with the same defaults, under the same `@State` name
   `AloudRiteHighlights` and the same file `aloudrite.xml`. Its field `highlights` is dropped. The serializer of the
   platform ignores an option it does not know, so a state stored by unit 0025 loads as the defaults, which is what
   the spec asks for, since carrying over the old box is left beyond the request (R2)
2. **The highlighter.** `ReadAlongHighlighter.refresh` passes the choices to `ShownReading.show`. There the mapped
   sentence is kept only where `sentence` is chosen and the mapped word only where `word` is. The editors are drawn
   with these where `inTextEditor` is chosen, and have their highlights removed where it is not. The pages are sent
   the places with these where `inPreview` is chosen, and the clearing message where it is not. The existing rule
   that a page is sent the clearing message when neither the sentence nor the word is left holds as it is, and so
   does `PreviewPage.send`, which sends nothing that equals the last message (R3, R4, R7)
3. **What is followed.** A view scrolls to the word where the word is drawn, and to the sentence where only the
   sentence is drawn, and not at all where neither is. In the editor, `HighlightsOfAnEditor.show` keeps in view the
   range given by a pure `rangeToFollow(sentence, word)`, which is `word ?: sentence`. In the page, the script keeps
   in view, in this order, the first range of the word, the outlined element of the word, and the sentence. The
   moments the following stops and starts again do not change. Keeping a range in view means: nothing is done while
   the start and the end of the range are both in view, and otherwise the start of the range is put in the middle of
   the view. For a sentence taller than the window of the preview, its top is put at the top of the window rather
   than in the middle, so that the sentence does not begin above the window. The check of the end as well as the
   start applies to the word too, which changes nothing for a word on one line (R5)
4. **The settings page.** `ReadingSpeedConfigurable` holds four `JBCheckBox`es in place of `highlightCheckBox`, each
   in a row of its own, in the order of requirement 1, where the one box is now: under the speed and above the
   group of the models. An item listener on the two boxes of the views sets `isEnabled` of the two boxes of the
   highlights to `highlightsCanBeChosen` of the choices the page shows, and `reset` calls the same update after
   setting the boxes. Disabling a `JBCheckBox` leaves its `isSelected` as it is, so the check marks are kept while
   greyed out, and an apply while they are greyed out stores them as they are shown (R1, R6)

How the parts are decided, so that the implementer does not re-derive it:

* **The names of the boxes.** `textEditorCheckBox`, `previewCheckBox`, `sentenceCheckBox` and `wordCheckBox`, with
  the texts in the companion as `TEXT_EDITOR_TEXT`, `PREVIEW_TEXT`, `SENTENCE_TEXT` and `WORD_TEXT`. `HIGHLIGHT_TEXT`
  is removed
* **The page reads and writes the choices whole.** A private `chosenHighlights(): HighlightChoices` builds the value
  from the four boxes. `isModified` compares it with `highlightSetting().choices`, `apply` stores it, and `reset`
  sets the four boxes from the stored value. No box is compared or stored on its own
* **The indent of the boxes of the highlights.** None. They stay at the left margin, as the spec asks only for the
  order and the greying, and an indent would be a look the human did not choose
* **The scrolling of a sentence in the editor.** `keepInView(range)` takes the range rather than an offset, checks
  `editor.offsetToXY(range.start)` and `editor.offsetToXY(range.end)` against the visible area, and scrolls to the
  start with `ScrollType.CENTER` as now. A sentence longer than half the editor keeps its start in the middle, and a
  repeated scroll to the same place moves nothing and so is no scroll of the user
* **The scrolling of a sentence in the page.** The rectangle kept in view runs from the top of the first range of the
  sentence to the bottom of its last. `keepInView(rect)` scrolls to `scrollY + rect.top` where `rect.height` is
  greater than `innerHeight`, and to the middle as now otherwise. The IDE sends no flag for it: an empty `word` and a
  null `wordElement` are what tell the page to follow the sentence. Where both highlights are on and a word happens
  to have neither pieces nor an element, the page now follows the sentence instead of standing still, which is the
  same rule and harms nothing
* **The tests of unit 0025 and 0026 that draw the word.** The word starts cleared, so `ReadAlongHighlighterTest` and
  `ReadAlongPreviewTest` set `setting.choices = HighlightChoices(word = true)` where they build the setting, so that
  they go on testing what they tested. The tests of the defaults are in `HighlightSettingTest` and in the new tests
  below, which build the setting with its defaults on purpose

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/HighlightSetting.kt`: `HighlightChoices`, its `highlightsCanBeChosen`, the
  four stored fields, and `choices` in place of `highlights`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighter.kt`: the choices passed to `ShownReading.show`, the
  views and the highlights chosen there, `rangeToFollow`, and `keepInView` of a range
- `src/main/resources/com/ritenoar/aloudrite/preview/aloudrite-read-along.js`: following the sentence where there is
  no word, the end of the range checked, and the top of a tall sentence at the top of the window
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurable.kt`: the four boxes, their greying, and the choices
  read and written whole
- `src/test/kotlin/com/ritenoar/aloudrite/HighlightSettingTest.kt`: the defaults, the four kept, the state of unit
  0025 loaded, and `highlightsCanBeChosen`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighterTest.kt`: the word chosen in the setup, the test of the
  one box replaced by tests of the four, and `rangeToFollow`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongPreviewTest.kt`: the word chosen in the setup, and the test of the
  one box replaced by tests of the four
- `src/test/javascript/read-along-script.test.mjs`: the following of the sentence
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurableTest.kt`: the tests of the one box replaced by tests
  of the four boxes, their order and their greying
- `README.md`: the four boxes in place of the one, in the paragraph of the editor, the paragraph of the preview and
  the paragraph of the settings page
- `changes/0027-choose-which-highlights-are-shown/record.md`: what the captures showed

## Tasks

- [x] Write the new tests of `HighlightSettingTest`, replacing the four that name `highlights`. They cover:
    * a new setting has `choices` equal to `HighlightChoices(inTextEditor = true, inPreview = true, sentence = true,
      word = false)`
    * each of the four, changed on its own, is given back by `loadState(first.state)` in a new setting, with the other
      three as they were
    * a `StoredHighlights` deserialized with `XmlSerializer.deserialize` from `<StoredHighlights><option
      name="highlights" value="false" /></StoredHighlights>`, the state unit 0025 wrote with its box cleared, and
      loaded, gives the defaults
    * `highlightsCanBeChosen` is false only with `inTextEditor` and `inPreview` both false
    * the test that the setting is one application service named `AloudRiteHighlights` is kept as it is

  Then write `HighlightChoices` and the new `HighlightSetting` until they pass. The tree does not compile until the
  next task is done (R2, R8)
- [x] In `ReadAlongHighlighterTest` and `ReadAlongPreviewTest`, set `choices` with `word = true` in the setup, and
  make `ReadAlongHighlighter` and `ReadingSpeedConfigurable` read `choices` instead of `highlights`, the page still
  with its one box, which reads and writes `inTextEditor` for this step only. Run both test classes and see them pass
  as before, apart from the two tests of the one box, which are rewritten in the tasks below and are marked
  `@Ignore` until then with the reason "rewritten for the four boxes in this unit"
- [x] Write in `ReadAlongHighlighterTest`, on the stand-ins of unit 0006 and with the slices of
  `readAndCheckAtEachSlice`, replacing `withTheSettingClearedDuringTheReadingThereAreNoHighlights...`:
    * with the setting at its defaults, each slice that draws shows the sentence and no word in every editor of the
      file
    * with `sentence` cleared and `word` chosen, each slice shows the word and no sentence
    * with both cleared, no highlighter is in any editor, and the reading plays all its slices
    * with `inTextEditor` cleared, no highlighter is in any editor, and the pages of a `PreviewPages` that records
      what it is sent are still sent the places, so that the editor's choice leaves the preview as it is
    * clearing each of the four at slice 2 and checking it again at slice 4: the highlight it turns off is gone at
      the refresh of slice 2, is back at the refresh of slice 4, and the reading plays all 12 slices, which is within
      half a second since the refresh is on the event of the slice
    * `rangeToFollow` gives the word where both are given, the sentence where only the sentence is, and null where
      neither is

  Then make `ShownReading.show` take the choices, draw and clear by them, and add `rangeToFollow` and `keepInView` of
  a range until they pass (R3, R4, R5, R7, R8)
- [x] Write in `ReadAlongPreviewTest`, replacing `withTheCheckboxClearedDuringTheReadingThePagesAreCleared...`:
    * with the setting at its defaults, each message of places holds the sentence's pieces and an empty `word` with a
      null `wordElement`
    * with `sentence` cleared and `word` chosen, each holds the word's pieces and an empty `sentence`
    * with both cleared, every page of the file is sent the clearing message and no places
    * with `inPreview` cleared, every page of the file is sent the clearing message and no places, and the editor
      still shows its highlights
    * clearing `inPreview`, `sentence` and `word` in turn at slice 2 and checking it again at slice 4 changes the
      messages at the refresh of slice 2 and of slice 4, and the reading plays all its slices

  Then make them pass with the drawing of the previous task, changing it only where they fail (R3, R4, R7, R8)
- [x] Write in `read-along-script.test.mjs`:
    * a message with sentence pieces, an empty `word` and a null `wordElement`, with the sentence out of view, scrolls
      once to the middle of the window
    * the same with the sentence in view does not scroll
    * the same with a sentence taller than the window scrolls its top to the top of the window
    * a message with a word and a sentence still scrolls to the word, as the existing test says
    * a wheel turn stops the following of the sentence as it stops that of the word

  The stand-in gives every range the one rectangle `page.rect`, so the sentence's rectangle is that one. Then change
  `places` and `keepInView` of the script until they pass, and run `ReadAlongScriptTest` (R5)
- [x] Write in `ReadingSpeedConfigurableTest`, replacing the four tests of `highlightCheckBox`:
    * the page shows four boxes with the texts of requirement 1, in that order from top to bottom, all under the speed
      and above the first radio button of the models, and no component of the page has the text "Highlight the words
      being read"
    * after `reset` with the defaults, the first three are checked, the word is cleared, all four are enabled, and the
      page is not modified
    * each box changed on its own makes the page modified, and `apply` stores the choices with that box changed and
      the other three as they were, after which the page is not modified
    * `reset` shows each of the four stored values
    * clearing both boxes of the views greys out the boxes of the highlights with their check marks as they were, a
      sentence checked and a word checked as well as cleared; checking either box of the views enables them again with
      the same check marks; and `apply` while they are greyed out stores their check marks as shown
    * `reset` with both views stored as cleared shows the boxes of the highlights greyed out

  Then write the four boxes, their rows, the item listener, `chosenHighlights` and the greying until they pass, and
  remove the `@Ignore`d tests (R1, R6, R8)
- [x] Look at the settings page in the sandbox IDE as `AGENTS.md` says, in a light theme and in a dark one, with all
  four boxes checked and with the two of the views cleared. Then read a Markdown file of `samples/` with only "Highlight
  in the text editor" and "Highlight the sentence being read" checked, and look at the text editor during the reading,
  including a moment when the editor has scrolled by itself to the next sentence. Record in `record.md` what the
  captures showed: the order and texts of the boxes, the greyed out boxes with their marks, the sentence highlighted
  without a word, and the scrolling (R9)
- [x] Rewrite in `README.md` the sentences on the checkbox "Highlight the words being read", in the paragraph of the
  highlights in the editor, in that of the preview and in that of the settings page, to describe the four boxes and
  their defaults, what each turns off, the scrolling by the sentence where the word is not shown, the greying out of
  the boxes of the highlights while neither view is checked with their marks kept, and that one choice of each holds
  for the whole IDE and is kept across restarts. Leave no mention of "Highlight the words being read" (R10)

## Verification

- [x] `./gradlew test` passes. The unit does not change the speech engine, sherpa-onnx or the text handed to the
  engine, so the tests with the real engine need not run
- [x] `./gradlew buildPlugin` builds the archive within its size limit
- [x] `npm run check-changes` passes on the committed branch
- [x] Every requirement has a task:
    * R1: the four boxes in `ReadingSpeedConfigurable` and their tests
    * R2: `HighlightChoices`, `HighlightSetting` and `HighlightSettingTest`
    * R3: the views chosen in `ShownReading.show`, and the tests of `inTextEditor` and `inPreview`
    * R4: the highlights chosen in `ShownReading.show`, and the tests of `sentence` and `word`
    * R5: `rangeToFollow`, `keepInView` of a range, and the following of the sentence in the script
    * R6: the greying of the settings page and its tests
    * R7: the choices read at every refresh, and the tests that change them at slice 2 and slice 4
    * R8: the tests above, none of which needs a model, an audio device or the network
    * R9: the captures in the sandbox IDE
    * R10: the README

## Commit Points

The work went in two commits rather than three: the second planned commit, the settings page, went in with the first.

- [x] `feature-0027 Choose the views and the highlights that a reading draws`
- [x] `feature-0027 Describe the four highlight checkboxes in the README and record the work`
