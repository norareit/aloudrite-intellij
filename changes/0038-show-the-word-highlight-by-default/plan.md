# Show the word highlight by default

## Approach

The record of this task states its requirements as a list and two paragraphs. The plan numbers them so that each task
can name what it delivers:

* R1: the default of the word in the choices of the highlights is checked
* R2: the comment of `HighlightSetting.kt` and the passage of `README.md` on the four checkboxes say that all four are
  checked until they are cleared, and no longer that the word is cleared because it is an estimate. The passage may say
  that the word is estimated where the aligner model is not on the machine
* R3: the tests that hold the defaults, of the settings, of the page "AloudRite", of the highlighter and of the preview,
  expect the word to be shown with the defaults
* R4: no stored choice is converted
* R5: the task is built after unit 0028 is merged into `master`, and afterwards `./gradlew test` passes

R5's precondition holds: the branch is cut from `master` at `7b40778`, which holds unit 0028 with the status
`implemented`.

The default lives in two places in `HighlightSetting.kt`: the parameter `word` of the data class `HighlightChoices`, read
by every `HighlightChoices()` and so by `HighlightSetting().choices` and by `reset()` of the page, and the field `word` of
`StoredHighlights`, which is what the IDE compares a stored value against to decide whether to write it. Both become
`true`. Were only the first changed, a state stored by the IDE would keep a cleared word as the default of the stored
class, and a checked word would load as cleared. `loadState` and `getState` stay as they are, and no code reads or
rewrites an older stored state (R4).

The tests change first, so that each fails against the old default, and then the default and its comment follow. The
test names that state the old defaults are renamed to say the new ones.

## Files

- `src/test/kotlin/com/ritenoar/aloudrite/HighlightSettingTest.kt`: the defaults test expects the word checked, and is
  renamed; the earlier-unit test sets a choice that differs from the defaults in all four
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurableTest.kt`: after `reset()` with the defaults all four
  boxes are checked, and the test is renamed
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighterTest.kt`: with the defaults each editor of the file shows
  the sentence and the word, and the test is renamed
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongPreviewTest.kt`: with the defaults each message holds the sentence and
  the word, and the test is renamed
- `src/main/kotlin/com/ritenoar/aloudrite/HighlightSetting.kt`: the two defaults of `word` and the comment above the
  class
- `README.md`: the sentences on the defaults in the passage on the four checkboxes, under the highlights of the reading

## Tasks

- [x] In `HighlightSettingTest.kt`, rename `theViewsAndTheSentenceAreOnAndTheWordIsOffUntilTheUserChangesThem` to
  `allFourAreOnUntilTheUserChangesThem` and make it expect `HighlightChoices(inTextEditor = true, inPreview = true,
  sentence = true, word = true)`. In `aStateStoredByTheCheckboxOfTheEarlierUnitLoadsAsTheDefaults`, set the choice
  before the load to `HighlightChoices(false, false, false, false)`, so that the word as well differs from the defaults
  and the test still proves the load resets it. Leave `eachChoiceChangedOnItsOwnIsGivenBackOnceItsStateIsLoadedIntoANewSetting`
  as it is: it flips each default, and with the new default it covers a cleared word that is stored and given back,
  which needs the default of `StoredHighlights` to change as well (R3, R4)
- [x] In `ReadingSpeedConfigurableTest.kt`, rename
  `afterResetWithTheDefaultsTheFirstThreeAreCheckedTheWordIsClearedAllAreEnabledAndThePageIsNotModified` to
  `afterResetWithTheDefaultsAllFourAreCheckedAndEnabledAndThePageIsNotModified` and make it expect
  `listOf(true, true, true, true)` from `shownChoices()`. The other tests of the boxes set their choices explicitly or
  derive them from `HighlightChoices()` and stay as they are (R3)
- [x] In `ReadAlongHighlighterTest.kt`, rename `withTheDefaultsEachSliceShowsTheSentenceAndNoWordInEveryEditorOfTheFile`
  to `withTheDefaultsEachSliceShowsTheSentenceAndTheWordInEveryEditorOfTheFile` and make it expect
  `listOf(listOf("One two three.", "One", "One two three.", "One"))`, the word that slice 1 shows in
  `withTheSentenceClearedAndTheWordChosenEachSliceShowsTheWordAndNoSentence` (R3)
- [x] In `ReadAlongPreviewTest.kt`, rename `withTheDefaultsEachMessageHoldsTheSentenceAndNoWord` to
  `withTheDefaultsEachMessageHoldsTheSentenceAndTheWord`, and make it collect `page.drawn("sentence")` and
  `page.drawn("word")` at each slice and assert that the list is not empty and that both are not empty in every entry,
  as `withTheSentenceClearedAndTheWordChosenEachMessageHoldsTheWordAndNoSentence` asserts for the word. Leave the
  field `setting` of this file and of `ReadAlongHighlighterTest.kt`, which sets `HighlightChoices(word = true)`, as it
  is (R3)
- [x] Run `./gradlew test --tests '*HighlightSettingTest' --tests '*ReadingSpeedConfigurableTest' --tests
  '*ReadAlongHighlighterTest' --tests '*ReadAlongPreviewTest'` and see the four changed defaults tests fail, and no
  other test of these files (R3)
- [x] In `HighlightSetting.kt`, set `val word: Boolean = true` in `HighlightChoices` and `var word: Boolean = true` in
  `StoredHighlights`, and leave `getState` and `loadState` as they are (R1, R4)
- [x] Rewrite the comment above `HighlightSetting` to say that the choices are for the whole IDE and kept across
  restarts, that all four are on until they are cleared, and that a state stored by an earlier version loads as the
  defaults, with nothing about the word being an estimate (R2)
- [x] In `README.md`, replace the sentence that begins "The first three are checked until they are cleared, and the word
  is cleared until it is checked" with one that says all four are checked until they are cleared, so that a reading
  shows the sentence and the word until the user clears one, and that the word is estimated while the aligner model is
  not on the machine, as the section on the aligner model describes. Leave the rest of the passage, from "A view that
  shows the sentence and not the word" on, as it is (R2)
- [x] Run the same four test classes again and see them all pass (R1, R3)

## Verification

- [x] `./gradlew test` passes. The tests with the real engine need not run for this task, since it changes neither the
  speech engine, sherpa-onnx nor the text handed to the engine (R5)
- [x] `./gradlew buildPlugin` succeeds
- [x] `npm run check-changes` passes once the work is committed
- [x] `grep -rn -i "cleared until it is checked\|since it is an estimate" README.md src` finds nothing (R2)
- [x] In the sandbox IDE started with `./gradlew runIde`, with no `aloudrite.xml` holding a highlight choice, the page
  Settings › Tools › AloudRite shows all four boxes checked, seen in a capture of the IDE's own window as `AGENTS.md`
  says (R1)

## Commit Points

- [x] `task-0038 Plan the task`: `plan.md` and the log entry in `record.md`
- [x] `task-0038 Show the word highlight by default`: the four tests, `HighlightSetting.kt` and `README.md`, which
  form one change of behaviour with its tests and its documentation
