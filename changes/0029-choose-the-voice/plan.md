# Choose the voice

## Approach

The voice becomes data, as the model is: a registry `VOICES` of the 13 voices of requirement 1, each with its Kokoro
name, its name in the list, its speaker number and its accent, and the accent carries the language that eSpeak NG is
given. The code that consumes the voices iterates the registry and names none of them, as `SPEECH_MODELS` is iterated.

The work falls into five layers, built from the bottom up:

1. **The voices** (`Voice.kt`, pure). `Voice(kokoroName, displayName, speakerNumber, accent)`, an `enum class Accent`
   with `AMERICAN("en-us")` and `BRITISH("en")`, the list `VOICES` in the order of requirement 1, `DEFAULT_VOICE` as
   Heart, and `voiceNamed(kokoroName): Voice?` for the stored choice (R1, R2, R4).
2. **The setting** (`VoiceSetting.kt`). An application service stored in `aloudrite.xml` under the name
   `AloudRiteVoice`, shaped as `ReadingSpeedSetting`: a `@Volatile` voice, `choose(voice)` that is no change for the
   voice already chosen, a collaborator `onChange` handed in so that a test needs no bus, and a topic `VOICE_CHANGED`
   that the default constructor publishes. It stores the Kokoro name, and a stored name of no voice loads as Heart. It
   tells the speech service nothing, since choosing a voice creates no engine and frees none (R2, R6).
3. **The engine** (`SpeechEngine`, `KokoroSpeechEngine`, `PhonemeRows`, `EspeakPhonemes`). `SpeechEngine.speak` takes
   the voice, as `speak(text, speed, voice, onSamples)`. `KokoroSpeechEngine` hands `voice.accent.espeakLanguage` to
   `PhonemeRows.rowsOf`, which hands it to its `sentencesOf`, which is `EspeakPhonemes.sentencesOf(text, language)` in
   place of the fixed `en-us`. The `GenerationConfig` of a piece is built by a new pure function
   `kokoroGenerationConfig(voice, speed, rows)`, beside `kokoroTtsConfig`, so that the speaker number is tested without
   the model. One engine serves every voice, since the voice is given per piece (R3, R4, R6).
4. **The reading** (`SpeechService`). The service takes a collaborator `currentVoice: () -> Voice`, as it takes
   `currentSpeed`, and a reading takes the voice when it starts, in `read`, and keeps it in `Reading.voice` for every
   making of it, also the makings after a pause and a change of the speed. `CurrentReading` gains
   `voiceOfTheReading(): Voice?`, the voice of the current reading, also while paused, and null where nothing reads.
   `Readings.read` and `ReadAloudAction` do not change, since the model is handed in for the download flow and the voice
   needs none (R3, R5, R6).
5. **The user interface** (`ReadingSpeedConfigurable`, `ReadingPanel`, `ReadingToolWindowFactory`). The settings page
   gets a row "Voice:" with a `ComboBox` of `VOICES`, directly under the row "Reading speed:" and above the four
   checkboxes of the highlights, which follows `VOICE_CHANGED` as the speed follows its topic. The tool window gets a
   read-only row "Voice:" in the group of the model, under the row of the model, which shows
   `voiceOfTheReading() ?: voiceInUse()` and is shown again on `READING_CHANGED` and on `VOICE_CHANGED`, on the EDT in
   any modality as the model is. Its name is HTML and wraps in a narrow pane, as the name of the model does, so the
   preferred height that the row of the model computes for its wrapped name is first extracted, in a commit of its
   own that changes no behavior, into one helper that both rows use (R1, R7).

The language of the phonemes for each voice is tested at two levels without the model: `Voice` maps each voice to its
language, and `PhonemeRows` hands the language it is given to `sentencesOf`. The stand-in engine of `SpeechStandIns`
records the voice of each text it is given, which tests that a reading hands the engine the voice it began with. With
the model in place, `EspeakPhonemesTest` checks that `en` gives the British phonemes of the spec and `en-us` the
American ones, and a new real engine test makes a sentence in each voice with each model.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/Voice.kt` (new): `Voice`, `Accent`, `VOICES`, `DEFAULT_VOICE`, `voiceNamed`
- `src/main/kotlin/com/ritenoar/aloudrite/VoiceSetting.kt` (new): the application service, `VoiceChangeListener`
  and `VOICE_CHANGED`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechEngine.kt`: `speak` takes the voice
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngine.kt`: removes `AF_HEART_SPEAKER_ID` and its comment, adds
  `kokoroGenerationConfig`, hands the language to the rows
- `src/main/kotlin/com/ritenoar/aloudrite/PhonemeRows.kt`: `sentencesOf: (text: String, language: String) ->
  List<IntArray>`, and `rowsOf(text, language)`
- `src/main/kotlin/com/ritenoar/aloudrite/EspeakPhonemes.kt`: removes `ESPEAK_VOICE`, `sentencesOf(text, language)`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: `currentVoice`, `Reading.voice`, `voiceOfTheReading`,
  the voice handed to `speak`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurable.kt`: the row "Voice:"
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: the extracted wrapping height, the row "Voice:"
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingToolWindowFactory.kt`: hands the voice in use to the panel
- `src/test/kotlin/com/ritenoar/aloudrite/VoiceTest.kt` (new), `VoiceSettingTest.kt` (new),
  `KokoroVoicesTest.kt` (new, with the real engine)
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: `voice` of the setting, `voicesMade`, the new `speak`
- `src/test/kotlin/com/ritenoar/aloudrite/PhonemeRowsTest.kt`, `KokoroTtsConfigTest.kt`, `EspeakPhonemesTest.kt`,
  `SpeechServiceTest.kt`, `ReadingSpeedConfigurableTest.kt`, `ReadingPanelTest.kt`, `ReadingShortcutsTest.kt`,
  `KokoroSpeechEngineTest.kt`: the new tests and the new signatures
- `src/test/kotlin/com/ritenoar/aloudrite/EngineSilenceMeasurement.kt`, `EngineLengthsMeasurement.kt`,
  `FirstAudioMeasurement.kt`, `ReadingTimeMeasurement.kt`, `EngineThreadsMeasurement.kt`: `speak` with
  `DEFAULT_VOICE`, so that what they measure stays as it was
- `README.md`: the voices
- `changes/0029-choose-the-voice/record.md`: the log of the work, the runs and the captures

## Tasks

### The voices

- [x] Write `VoiceTest`: `VOICES` holds the 13 voices of requirement 1 in its order, each with its Kokoro name, its name
  in the list and its speaker number exactly as the table gives them; `DEFAULT_VOICE` is Heart; every voice whose
  Kokoro name begins with `af_` or `am_` has the language `en-us`, and every one that begins with `bf_` or `bm_` the
  language `en`; `voiceNamed` gives the voice of each Kokoro name and null for `af_alloy` and for an empty name
  (R1, R4, R9)
- [x] Add `Voice.kt` with `Voice`, `Accent`, `VOICES`, `DEFAULT_VOICE` and `voiceNamed`, with a comment that the
  numbers are those of the `id2speaker` metadata of both models and that the binding has no lookup by name (R1, R4)

### The setting

- [x] Write `VoiceSettingTest`, as `SpeechModelSettingTest` and `ReadingSpeedSettingTest` test theirs: a new setting
  holds Heart; `choose` of another voice holds it and calls `onChange` once with it; `choose` of the voice held calls
  nothing; `getState` then `loadState` into a new setting gives the voice chosen; a state with `af_alloy`, with an
  empty name and with no name loads as Heart (R2, R9)
- [x] Add `VoiceSetting.kt` with `VoiceChangeListener`, `VOICE_CHANGED` and the service `VoiceSetting`, whose default
  constructor publishes `VOICE_CHANGED` and nothing else, registered by its `@Service` annotation alone, as the other
  settings are (R2, R6)

### The engine

- [x] In `PhonemeRowsTest`, have the stand-in of `sentencesOf` record the language it is given, and add a test that
  `rowsOf(text, "en")` and `rowsOf(text, "en-us")` hand that language to `sentencesOf`; update the other tests to the
  new signature (R4, R9)
- [x] In `KokoroTtsConfigTest`, add a test that `kokoroGenerationConfig` of each voice of `VOICES` holds its speaker
  number in `sid`, the speed it was given and the rows it was given in `phonemeCodepoints`. It runs without the model;
  confirm that building a `GenerationConfig` of 2.0.2 loads no native code, as the test of `kokoroTtsConfig` relies on
  for its configuration (R3, R9)
- [x] Change `PhonemeRows` to take the language and hand it on, `EspeakPhonemes.sentencesOf` to take the language and
  drop `ESPEAK_VOICE`, `SpeechEngine.speak` to take the voice, and `KokoroSpeechEngine` to build the configuration with
  `kokoroGenerationConfig` and to hand `voice.accent.espeakLanguage` to the rows; remove `AF_HEART_SPEAKER_ID` and its
  comment, and say in the comment of the class that one engine serves every voice, since the voice is given per piece
  (R3, R4, R6, R8)
- [x] Update `StandInEngine` of `SpeechStandIns` to the new `speak` and have it record the voice of each text in
  `voicesMade`, beside `textsMade` and `speedsMade`; update `KokoroSpeechEngineTest` and the five measurements to call
  `speak` with `DEFAULT_VOICE` (R3, R9)
- [x] In `EspeakPhonemesTest`, which runs only with the model in place, add a test that "schedule" and "tomato" give
  "ʃˈɛdjuːl" and "təmˈɑːtəʊ" in the language `en` and "skˈɛdʒuːl" and "təmˈeɪɾoʊ" in `en-us`, and change its helper
  to name the language (R4)
- [x] Add `KokoroVoicesTest`, a test with the real engine shaped as `KokoroSpeechEngineTest`: one test for each model
  of `SPEECH_MODELS`, each skipped with `assumeTrue` where the platform has no speech or that model is not in place
  under `userModelsFolder()`, which creates a `KokoroSpeechEngine` of that model, makes "AloudRite reads this schedule
  aloud." in each of the 13 voices of `VOICES`, asserts for each voice that the audio holds at least one sample and no
  sample that is `NaN` or infinite, naming the voice and the model in the message, and closes the engine at the end
  (R10)

### The reading

- [x] Add `var voice: Voice = DEFAULT_VOICE` to `SpeechStandIns` and hand `currentVoice = { voice }` to the service,
  then write the tests in `SpeechServiceTest`: a reading started with each voice of `VOICES` hands the engine that
  voice for every text; a voice chosen during a reading leaves the rest of that reading, also after a pause and a
  resume and after a change of the speed, in the voice it began with, and the next reading takes the new voice;
  `voiceOfTheReading` is null while nothing reads, the voice of the reading while it plays and while it is paused, and
  null again after its end; a reading in one voice followed by a reading in another with the same model makes one
  engine, as `modelsMade` shows, and a choice of the voice while nothing reads makes none and closes none (R3, R5, R6,
  R9)
- [x] Add `currentVoice` to the `SpeechService`, `voice` to `Reading`, taken in `read`, `voiceOfTheReading` to
  `CurrentReading` and the service, and hand `reading.voice` to `speak` in `makeTheAudioOf`; the default constructor
  takes `{ service<VoiceSetting>().voice }` (R3, R5, R6)

### The settings page

- [x] Write the tests in `ReadingSpeedConfigurableTest`, with a `VoiceSetting` of the test handed to the page: the page
  has a label "Voice:" for a list of the 13 names of requirement 1 in its order; the list shows the voice of the
  setting at a reset; choosing another voice makes the page modified, and an apply chooses it in the setting and leaves
  the page unmodified; a voice chosen in the setting while the page is open is shown once the events are dispatched,
  without touching a speed chosen on the page and not applied; the row of the voice is directly under the row of the
  speed and above the checkboxes of the highlights (R1, R9)
- [x] Add the row to `ReadingSpeedConfigurable`: the constructor takes `voiceSetting: () -> VoiceSetting`, with
  `{ service<VoiceSetting>() }` in the default one; `voiceLabel` with `VOICE_LABEL_TEXT = "Voice:"` and `voiceChoice`, a
  `ComboBox` of `VOICES` whose renderer shows `displayName`; `isModified`, `apply` and `reset` include it; the page
  subscribes `VOICE_CHANGED` to a `resetVoice` that resets only the voice; update the comment of the class (R1)

### The tool window

- [x] Extract from `modelRow` of `ReadingPanel` the preferred height of a row whose HTML label wraps at the width the
  pane leaves it beside another component, into one private helper, with no change of behavior; the tests of the row of
  the model pass unchanged. This is a refactoring commit of its own (R7)
- [x] Write the tests in `ReadingPanelTest`, with a voice in use handed to the panel as the model in use is: while
  nothing is read the group of the model holds, under the row of the model, a row with "Voice:" and the name of the
  voice in use, in every project; while a file is read and while it is paused, the row shows the voice of the reading,
  also after a voice was chosen during it and `VOICE_CHANGED` was dispatched; once that reading ends, the row shows the
  voice chosen; a choice while nothing is read shows once `VOICE_CHANGED` is dispatched; the row holds no control that
  takes the focus or a click; the longest name, Nicole's, wraps and fits in full in a narrow pane; and update
  `thePanelHoldsTheReadingThenTheSpeedThenTheModelGroupAndOnlyTheLastTwoHaveATitledSeparator`,
  `theLeftEdgeOfEveryElementOfThePaneIsTheSame` and `whileAFileIsReadThePanelHoldsNoButtonButTheLinkOfTheModel` to
  count the row of the voice, and `ReadingShortcutsTest` to the new constructor (R7, R9)
- [x] Add the row to `ReadingPanel`: the constructor takes `voiceInUse: () -> Voice`; `voiceLabel` with
  `VOICE_LABEL_TEXT = "Voice:"` at the left edge and `voiceNameLabel` in HTML beside it, in a `voiceRow` under
  `modelRow` in `modelGroup`; `showTheVoice()` shows `reading().voiceOfTheReading() ?: voiceInUse()` and is called at
  creation, from `showTheCurrentReading` and on `VOICE_CHANGED` through `invokeLater` in any modality; hand
  `{ service<VoiceSetting>().voice }` in `ReadingToolWindowFactory`; update the comment of the class (R7)

### The documentation

- [x] Change `README.md`: in place of "in the voice `af_heart`", say that the speech is in the voice chosen; add a
  paragraph, beside that of the model under "The speech model" or after the paragraph of the speed, that names the 13
  voices as the list shows them, says that they come with either model without a download, that one voice holds for
  the whole IDE and is kept when the IDE is closed, that Heart is the voice until one is chosen, that it is chosen
  under Settings › Tools › AloudRite in the row "Voice:", that a British voice uses the British pronunciation, and
  that a voice chosen during a reading is heard from the next reading on; describe the row "Voice:" of the tool window
  under the row of the model, which shows the voice of the reading while a file is read or paused and the voice chosen
  otherwise, and that the voice is chosen only on the settings page; and say that the page holds the row of the voice
  under the speed. Check with `grep -rn "af_heart" README.md src` that no text or comment says the plugin reads in
  `af_heart` only (R8)

### The checks

- [x] Put both models in place under `~/.local/share/aloudrite`, as README.md says under "The speech model", run
  `./gradlew test`, and confirm in its report that `KokoroVoicesTest`, `EspeakPhonemesTest` and
  `KokoroSpeechEngineTest` ran with both tests of `KokoroVoicesTest` passed and none skipped; log in `record.md` that it
  ran on Linux x64 with both models, was not skipped, and what it found (R10)
- [x] Start the sandbox IDE with `./gradlew runIde` and capture only its window, as `AGENTS.md` says, of the settings
  page with the list of the voice closed and open, and of the tool window while nothing is read and while a file is
  read, each in a light and a dark theme; log in `record.md` what the captures showed (R11)
- [ ] Log in `record.md` that the reading of `samples/markdown-features.md` on Linux x64 in Heart, Emma and Heart
  again, chosen on the settings page between the readings, is left to the developer, and record the outcome once the
  developer has reported it (R12)

## Verification

- [x] `./gradlew buildPlugin` succeeds
- [x] `./gradlew test` passes, with `KokoroVoicesTest` run and not skipped for both models
- [x] `grep -rn "af_heart" README.md src/main` finds nothing that says the plugin reads in that voice only
- [x] `npm run check-changes` passes on the committed branch

## Commit Points

- [x] `feature-0029 Plan choosing the voice`: `plan.md` and the entry of `record.md`
- [x] `feature-0029 Add the voices and the setting of the voice`: the tasks of the voices and of the setting
- [x] `feature-0029 Hand the voice and its language to the engine`: the tasks of the engine and of the reading, which were committed together
- [x] `feature-0029 Choose the voice on the settings page`: the tasks of the settings page
- [x] `feature-0029 Extract the height of a wrapped row of the tool window`: the refactoring alone
- [x] `feature-0029 Show the voice in the tool window`: the rest of the tool window
- [x] `feature-0029 Describe the voices in the README`: the documentation
- [x] `feature-0029 Record the runs and the captures`: the entries of the checks in `record.md`
