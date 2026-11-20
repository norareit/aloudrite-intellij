# Developer checks of unit 0030

These are the checks of requirements 13 and 14 that only the developer can make. Both models have to be in place, as
`README.md` says under "The speech model". Send back what each step asks for, and the implementer records it.

## On macOS on Apple Silicon

1. Check out the branch `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` and run `./gradlew test`. The tests
   with the real engine, `KokoroSpeechEngineTest`, `EspeakPhonemesTest` and `EspeakDataTest`, must not be skipped. Send
   back the counts of tests, failures and skipped tests from the report, and the names of any test that was skipped
   or failed
2. Run the lengths:

   ```sh
   ALOUDRITE_MEASURE_ENGINE_LENGTHS=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineLengthsMeasurement
   ```

   Send back `build/engine-lengths.txt`
3. Run `ALOUDRITE_MEASURE_FIRST_AUDIO=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.FirstAudioMeasurement`
   and send back `build/first-audio.txt`. This is not asked for by the requirements on macOS, and tells whether the
   start of a reading there takes longer

## Listening, on Linux x64 and on macOS on Apple Silicon

Start the sandbox IDE with `./gradlew runIde`, with the fp32 model in use, open `samples/markdown-features.md` and read
it aloud from the beginning to the end. Note anything that sounds different from sherpa-onnx 1.13.8, such as a number, an
abbreviation, a word of code or a symbol said another way, a word left out, a pause or a cut in the middle of a word, or
a voice that is no longer the same. Fill in:

| Platform | Anything that sounded different from 1.13.8 | Words left out or cut |
|----------|---------------------------------------------|-----------------------|
| Linux x64 | | |
| macOS on Apple Silicon | | |
