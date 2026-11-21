# 0033 Shorten the pause after a code block

## Approach

The task has no numbered requirements, so the plan numbers the items of its description in `record.md` in their
order, and traces each task to them:

* R1: `PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS` is 1,000, still divided by the speed of the reading
* R2: where two pauses meet, the longest is heard once, so a code block followed by the next block gives one second
* R3: the tests that assert the parts of a code block expect 1,000 ms after "code example", and the tests of
  `SpeechServiceTest` and `ReadingSpeedTest` that use 2,000 as an arbitrary length of a silence are left as they are
* R4: `README.md` and the table of pauses in `docs/reading-markdown.md` say one second, the table drops "long enough
  to pause the reading by hand", the "Wanted" line of the code block stays and its "Now" line says one second

The change is one constant. The division by the speed and the merging of pauses that meet are done elsewhere and are
left untouched: `ReadingSpeed.silenceMilliseconds` divides every silence, and the silences next to each other are
made one when the placed parts are made into spoken ones, so R1's division and R2 follow from the constant alone. The
tests that encode the length change with it, ahead of the constant, so that they fail first and pass once it is
changed.

The tests that hold the length of the pause after a code block, found on the tree at `8d6b931`:

* `SpokenMarkdownTest`: the constant `CODE_BLOCK = "<silence 2000 ms>"`, the silence the stand-in engine plays after
  "code example". The stand-in plays 100 samples a second, and prints a silence as its milliseconds, so it becomes
  `"<silence 1000 ms>"`
* `PlacedPartsTest.theCodeExampleIsFollowedByItsSilence`: expects `"code example", 2_000, 500, "After."` before the
  silences are merged, which becomes `1_000`
* `ReadAlongHighlighterTest.codeExampleIsHighlightedOnTheWholeBlockInBothHighlighters`: picks the slice in
  "code example" as `6 + 20 + 1`, after the six slices of "Before." and the twenty of the two seconds of silence. A
  slice is a tenth of a second, so one second is ten slices, and the slice becomes `6 + 10 + 1`, with the comment
  saying one second and ten

Left as they are, under R3: `Silence(2000)` in `SpeechServiceTest` at its three readings of `Words("code example")`,
and `silenceMilliseconds(2000)` in `ReadingSpeedTest`. They build their silences by hand and do not read the
constant. The other tests that read a code block, in `ReadAlongPreviewTest`, `ReadingSegmentsTest`,
`PartOfTheFileTest` and `ReadAloudActionTest`, assert words, places or highlights that do not depend on the length of
the silence.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/SpokenMarkdown.kt`: `PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS` from `2_000` to
  `1_000`
- `src/test/kotlin/com/ritenoar/aloudrite/SpokenMarkdownTest.kt`: `CODE_BLOCK` expects a silence of 1,000 ms
- `src/test/kotlin/com/ritenoar/aloudrite/PlacedPartsTest.kt`: the silence after "code example" is `1_000`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighterTest.kt`: the slice in "code example" is `6 + 10 + 1`,
  and its comment says one second, which is ten slices
- `README.md`: one second after "code example" in its three places
- `docs/reading-markdown.md`: the row of the table of pauses, and the "Now" line of the code block

## Tasks

- [ ] In `SpokenMarkdownTest.kt`, change `const val CODE_BLOCK = "<silence 2000 ms>"` to `"<silence 1000 ms>"`
  (R2, R3)
- [ ] In `PlacedPartsTest.kt`, in `theCodeExampleIsFollowedByItsSilence`, change the expected `2_000` to `1_000`
  (R3)
- [ ] In `ReadAlongHighlighterTest.kt`, in `codeExampleIsHighlightedOnTheWholeBlockInBothHighlighters`, change the
  slice `6 + 20 + 1` to `6 + 10 + 1`, and the comment to say that the silence of one second, which "code example"
  follows, is ten slices (R3)
- [ ] Run `./gradlew test` and see the three tests above fail, and only for the length of the silence (R3)
- [ ] In `SpokenMarkdown.kt`, change `PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS = 2_000` to `1_000`, leaving the comment
  above the constants as it is (R1)
- [ ] Run `./gradlew test` and see it pass, with the `Silence(2000)` of `SpeechServiceTest` and the
  `silenceMilliseconds(2000)` of `ReadingSpeedTest` unchanged (R1, R2, R3)
- [ ] In `README.md`, where it says a code block is read as "code example" followed by two seconds of silence, say one
  second of silence (R4)
- [ ] In `README.md`, where a reading paused during a silence the plugin adds is described, change the example "the
  two seconds after "code example"" to "the second after "code example"" (R4)
- [ ] In `README.md`, in the paragraph on the silences following the speed, say that at 2.0× the second after "code
  example" is half a second, and that at 0.75× it is one and a third seconds, in place of one second and two and two
  thirds seconds. The half second between two paragraphs and its two thirds of a second at 0.75× stay (R1, R4)
- [ ] In `docs/reading-markdown.md`, change the row `After "code example"` of the table of pauses to "one second in
  all", dropping "long enough to pause the reading by hand", and realign the column of the table to the longest cell
  left in it (R4)
- [ ] In `docs/reading-markdown.md`, in the "Now" line of the code block, say "followed by one second of silence" in
  place of "followed by two seconds of silence". Leave its "Wanted" line and the sentence under the table on pauses
  that meet as they are (R2, R4)
- [ ] Search `README.md`, `docs/` and `src/` for "two seconds", "2_000" and "2000" and confirm that none left refers
  to the pause after a code block, beyond the "Wanted" line and the two kinds of test kept under R3 (R3, R4)

## Verification

- [ ] `./gradlew test` passes. The tests with the real engine may be skipped, as the unit changes neither the engine
  nor the text handed to it, only the length of a silence the plugin adds
- [ ] `npm run check-changes` passes on the committed branch

## Commit Points

- [ ] `task-0033 Plan the shorter pause after a code block`: `plan.md` and the log entry in `record.md`
- [ ] `task-0033 Shorten the pause after a code block to one second`: the three tests and the constant, after the
  full suite passes
- [ ] `task-0033 Say one second after a code block in the documentation`: `README.md` and `docs/reading-markdown.md`
