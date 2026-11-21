# Read the number of an item with its first sentence

## Approach

The number of an item is made in `SpokenMarkdown.kt`. Today `numberOf` gives the count and the delimiter of the list,
as `1. ` or `3) `, and `partsOfItem` puts it as words of its own in front of the words of the item's first block. Those
words are joined with the first block's words by `PartsInARow`, so the number is already in the same part as the first
sentence. What cuts it off is the text: `TextPieces.kt` ends a sentence, and so a piece, after `1. `, and `)` gives no
pause at all.

The change keeps everything about where the number stands and how it is marked, and changes only its text:

- `numberOf` gives the count alone, as an `Int?`, null for a bulleted list, and `partsOfItem` writes the text.
- An item whose blocks give words reads its number as the count followed by `, `, for both delimiters. The comma
  is not a sentence end, and `,` cuts a piece only after `MINIMUM_CLAUSE_BYTES`, which the count alone never reaches
  as it always starts a part (it follows a silence or starts the reading), so the number stays in the piece and the
  sentence of the item's first words.
- An item whose blocks give nothing, as one whose words are all struck out, reads its number as the count followed by
  `. `, so that `endedAsASentence` finds it ended and it reads `1.` alone, with no comma before its end. Today a `)`
  item of that kind reads `1).`, and it reads `1.` after the change as well.
- Both texts are the digits of the count, one mark and a space, which is as long as the count with its delimiter
  and space today. The `NumberMark` covers the whole number text as now, and `markerAt` still finds the marker in the
  file, so the highlights in the text editor and in the preview are unchanged. `NumberedItem.number` holds the text
  that is read, so that `withItsNumber` in `PartOfTheFile.kt` puts `3, ` in front of a selection that begins in the
  item after its number, with no change to that file.

Counting, nesting, the full stop at the end of each item, the silences, and the empty number in a comment stay as
they are.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/SpokenMarkdown.kt`: `numberOf` gives the count, `partsOfItem` writes the
  number text with `, ` or, for an item that gives nothing else, `. `, and a constant names the comma and space
- `src/main/kotlin/com/ritenoar/aloudrite/PlacedPart.kt`: the comment on `NumberedItem` gives `3, ` as the example of
  the number as it is read
- `src/test/kotlin/com/ritenoar/aloudrite/SpokenMarkdownTest.kt`: the text and pieces handed to the engine
- `src/test/kotlin/com/ritenoar/aloudrite/PlacedPartsTest.kt`: the offsets and the number marks
- `src/test/kotlin/com/ritenoar/aloudrite/PartOfTheFileTest.kt`: the number before a selection or caret in an item
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSegmentsTest.kt`: the places of the number for the highlights
- `src/test/kotlin/com/ritenoar/aloudrite/EspeakPhonemesTest.kt`: the test with the real data of eSpeak NG
- `docs/reading-markdown.md`: the entry under "Numbered lists", and the sentence on struck out items under
  "Strikethrough" if it then says less than is true

## Tasks

- [x] In `SpokenMarkdownTest.kt`, change `aNumberedListKeepsItsNumbersAndEndsItsItemsAsASentence` to expect
  `1, First.`, `2, Second.`, and for the `)` list `3, Third.`, `4, Fourth!`. Add tests that a list starting at 7 reads
  `7, ` and `8, `; that a nested numbered list under each delimiter reads `1, Outer.` and `1, Inner one.`,
  `2, Inner two.`, counting each level from its own first number; and that `piecesOf` of the words of `1. Climb the
  stairs and unlock the lamp room.` and of the same item with `)` is one piece that begins `1, Climb`. Run them and
  see them fail (R1, R2, R6)
- [x] In `SpokenMarkdownTest.kt`, add tests that an item whose words are all struck out reads `2.` alone between the
  items around it, for `2. ~~gone~~` and for `2) ~~gone~~`, with no comma, and that a file of nothing but such a list
  reads its numbers that way. Run them and see the `)` case fail (R3, R6)
- [x] In `PlacedPartsTest.kt`, change `theFullStopOfAnItemTakesTheOffsetOfItsLastCharacterAndTheNumberThatOfItsMarker`
  to expect `1, First.` and `2, Second.` with the same offsets, and `theNumberOfAnItemIsMarkedWithTheRangeOfItsItem` to
  expect `2, ` as the number. Add a case with `)` that the three characters of `1, ` take the offset of the marker and
  are covered by the number mark (R1, R4, R6)
- [x] In `PartOfTheFileTest.kt`, change the expectations of the tests that read a selection or a caret in an item to the
  comma, as `3, Third thing.`, `2, Second.|3, Third thing.`, `2, Inner two.` and `2, Dos.|3, Three.`, and add one with
  `)` (R1, R6)
- [x] In `ReadingSegmentsTest.kt`, add to the tests of the number marker one with `)` and a list that starts at
  another number than 1, showing that the first word highlighted is the marker as the file writes it, such as `10)`,
  and that the number belongs to the first sentence of its item. Run the existing tests of the marker, which must
  pass unchanged (R4, R6)
- [x] In `SpokenMarkdown.kt`, make `numberOf` give the count, and make `partsOfItem` write the number as the count and
  `, ` when its blocks give words, and as the count and `. ` when they give nothing, using `givesNothing` on the joined
  blocks; keep the empty number in a comment. Update the comment on `numberOf` and on `NumberedItem` in `PlacedPart.kt`.
  Run `./gradlew test` and see all of the above pass (R1, R2, R3, R4, R5)
- [x] Commit: `feature-0037 Read the number of an item with its first sentence`
- [x] In `EspeakPhonemesTest.kt`, add a test that reads `samples/markdown-features.md` with `spokenPartsOf`, takes the
  first piece by `piecesOf` that begins with the number of the first item of the section "Numbered list", asserts that
  it begins `1, Climb`, and asserts that `EspeakPhonemes.sentencesOf` gives one sentence for it with
  `Accent.AMERICAN.espeakLanguage` and with `Accent.BRITISH.espeakLanguage`. It is skipped as the other tests of the
  class are where the model is not in its folder. Run it with the model in place and see it pass, and do not count a
  skipped run as a pass (R7)
- [x] Commit: `feature-0037 Test that eSpeak NG reads the first item as one sentence`
- [x] In `docs/reading-markdown.md`, under "Numbered lists", add the entries of unit 0037 in the form of the others:
  what was heard (the British voices, and then Heart, stumble after the number; with `.` the number is said as a
  sentence of its own, with `)` it runs into the first word), what was wanted (the number read as the start of the
  first sentence of its item), and what is read now (the number followed by a comma, for both delimiters, counted as
  before, and an item with no words but its number reads its number alone, ended as a sentence). Change the "Now" entry
  so that it no longer says the number is read with its `.` or `)`. Check the sentence under "Strikethrough" on the
  number of an item whose words are struck out and leave it as it is if it is still true (R8)
- [x] Commit: `feature-0037 Describe how the number of an item is read`
- [ ] Ask the developer to read the two numbered lists of `samples/markdown-features.md` on Linux x64 in Heart and in
  Emma, and log in `record.md` whether the number is heard as the start of its sentence and whether the stumble after
  it is gone (R9)

## Verification

- [x] `./gradlew test` passes, with the model in its folder, so that `EspeakPhonemesTest` runs and is not skipped
- [x] `./gradlew buildPlugin` succeeds
- [ ] `npm run check-changes` passes after the last commit

## Commit Points

- [x] `feature-0037 Read the number of an item with its first sentence`: the tests of R1 to R6 and the change of
  `SpokenMarkdown.kt`
- [x] `feature-0037 Test that eSpeak NG reads the first item as one sentence`: the test of R7
- [x] `feature-0037 Describe how the number of an item is read`: `docs/reading-markdown.md`
- [ ] `feature-0037 Record the developer's check`: the log entry of R9 in `record.md`
