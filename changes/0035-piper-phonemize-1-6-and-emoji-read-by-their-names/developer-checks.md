# Developer checks of unit 0035

These are the checks of requirements 14 and 15 that only the developer can make. Both speech models and the aligner
model have to be in place, as `README.md` says under "The speech model". Send back what each step asks for, and the
implementer records it.

## On macOS on Apple Silicon

1. Check out the branch `feature-0035-piper-phonemize-1-6-and-emoji-read-by-their-names` and run `./gradlew test`. The
   tests with the real binding, engine and aligner, `EspeakPhonemesTest`, `EspeakDataTest`, `KokoroSpeechEngineTest` and
   `RealAlignerTest`, must not be skipped. Send back the counts of tests, failures and skipped tests from the report,
   and the names of any test that was skipped or failed
2. Run the lengths:

   ```sh
   ALOUDRITE_MEASURE_ENGINE_LENGTHS=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineLengthsMeasurement
   ```

   Send back `build/engine-lengths.txt`

## Listening, on Linux x64 and on macOS on Apple Silicon

Start the sandbox IDE with `./gradlew runIde`, with the fp32 model in use, open `samples/markdown-features.md` and read
it aloud from the beginning to the end. The three emoji in the section "Emoji of four bytes" are read by their names
now, and were left out before. Note anything else that sounds different from before this unit, such as a number, an
abbreviation, a word of code or a symbol said another way, a word left out, a pause or a cut in the middle of a word, or
a voice that is no longer the same. Look at the highlight while the sentence with the emoji is read, in the editor and
in the preview, and note whether it falls on the words before and after each emoji. Fill in:

| Platform | What sounded different from before this unit, the emoji among it | Did the highlight fall on the words around the emoji, in the editor and in the preview |
|----------|------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| Linux x64 | | |
| macOS on Apple Silicon | | |
