# Choose the voice

## Context

The plugin reads in one voice, `af_heart`, which it asks the engine for by its speaker number, 3. The Java binding of
sherpa-onnx has no lookup of a voice by name, so the number is fixed in the code.

Both models of unit 0020, `kokoro-int8-multi-lang-v1_0` and `kokoro-multi-lang-v1_0`, hold the same 54 voices in
their `voices.bin`, under the same numbers, which the `id2speaker` metadata of the model names. A voice is therefore
chosen without a download. 28 of the voices speak English: 20 American, whose names begin with `af_` and `am_`, and 8
British, whose names begin with `bf_` and `bm_`. The others are voices of other languages.

The model card of Kokoro grades each voice by the quality and the amount of the audio it was trained on. The human
asked to be able to choose the voice before the plugin is published, and chose the English voices graded A, B- or
C+, and the British voices graded C, so that British voices are offered as well as American ones. Some listeners
prefer British English.

The specifier rendered one passage in each of those 13 voices with the fp32 model on Linux x64, through the Java
binding the plugin used then, sherpa-onnx 1.13.8, and the human listened to all of them. The human rated them from 5
to 9 out of 10, found none of them bad, and decided to offer all 13. `af_nicole` whispers, which the human keeps as a
curiosity.

Since unit 0030, the plugin reads with sherpa-onnx 2.0.2 and makes the phonemes of each piece itself, with eSpeak NG
through `piper-phonemize`, and hands them to the engine. It gives eSpeak NG the language `en-us` with each piece, and
gives the engine no language. It still gives the engine `lexicon-us-en.txt`, because without a lexicon the native code
ends the process, but the engine does not read it for phonemes it is handed. Unit 0030 also found that 1.13.8, as the
plugin configured it, never read the lexicon for English and gave the whole text to eSpeak NG. So the pronunciation of a
voice is the language that eSpeak NG is given, and a British voice sounds British when eSpeak NG makes British phonemes:

- With the language `en`, British English in eSpeak NG, the phonemes are British: "schedule" is "ʃˈɛdjuːl" and
  "tomato" "təmˈɑːtəʊ", where `en-us` gives "skˈɛdʒuːl" and "təmˈeɪɾoʊ". Each phoneme of these is a token of the model.
- With the language `en-gb`, eSpeak NG has no such voice, and `piper-phonemize` gives no phonemes at all. Under
  1.13.8 the same language made the engine give an empty audio without an error.
- A word in no dictionary, such as "AloudRite", is spelled into phonemes by eSpeak NG in either language.

The specifier checked these with `piper-phonemize` 1.5.9 and the data of eSpeak NG of the fp32 model on Linux x64.
The speaker number is given to the engine with each piece and the language to eSpeak NG with each piece, so no voice
needs an engine of its own.

## Requirements

1. Settings › Tools › AloudRite holds a row "Voice:" with a drop-down list of these 13 voices, in this order and with
   these names:

   | Voice         | Name in the list                             | Speaker number |
   |---------------|----------------------------------------------|----------------|
   | `af_heart`    | Heart (American English, female)             | 3              |
   | `af_bella`    | Bella (American English, female)             | 2              |
   | `af_kore`     | Kore (American English, female)              | 5              |
   | `af_sarah`    | Sarah (American English, female)             | 9              |
   | `af_aoede`    | Aoede (American English, female)             | 1              |
   | `af_nicole`   | Nicole (American English, female, whispers)  | 6              |
   | `am_puck`     | Puck (American English, male)                | 18             |
   | `am_fenrir`   | Fenrir (American English, male)              | 14             |
   | `am_michael`  | Michael (American English, male)             | 16             |
   | `bf_emma`     | Emma (British English, female)               | 21             |
   | `bf_isabella` | Isabella (British English, female)           | 22             |
   | `bm_fable`    | Fable (British English, male)                | 25             |
   | `bm_george`   | George (British English, male)               | 26             |

2. The voice is Heart until the user chooses another. One voice holds for the whole IDE, in every project, and it is
   kept when the IDE is closed and opened again. A stored voice that is not in the list, as after a later version
   drops one, is read as Heart.
3. A reading reads in the voice chosen when it starts, with the speaker number of requirement 1, in either model.
4. The phonemes of a reading in an American voice are made by eSpeak NG in the language `en-us`, as the plugin does
   now, and those of a reading in a British voice in the language `en`.
5. A voice chosen during a reading takes effect at the next reading, and the reading goes on in the voice it began
   with, as unit 0020 decided for the model.
6. The plugin keeps at most one engine in memory, as unit 0020 decided, and a reading in any voice uses the engine of
   the model in use. Choosing a voice never creates an engine.
7. The tool window shows the voice at all times, also while nothing is read, and in every project, in a read-only row
   "Voice:" with the name of requirement 1, under the row of the model under the separator "Speech model". While a
   file is read or paused, it shows the voice of that reading. Otherwise it shows the voice chosen, within a second of
   the choice being applied on the settings page. The voice is chosen only on the settings page, and the link
   "Settings…" of the row of the model opens it.
8. `README.md` and the comments in the code no longer say that the plugin reads in `af_heart` only. `README.md` names
   the 13 voices, says that they come with either model without a download, that a British voice uses the British
   pronunciation, that a voice chosen during a reading is heard from the next reading on, and the row of the voice in
   the tool window.
9. Automated tests cover requirements 1 to 7 with the stand-ins of unit 0006 for the engine and the player, and need
   no model, no audio device and no network: the list and its order, the stored choice and a stored voice that is
   not in the list, the speaker number and the language of the phonemes for each voice, a choice during a reading, the
   engine kept when the voice changes, and the row of the tool window while nothing is read, during a reading and
   after a choice during a reading.
10. A test with the real engine, which runs where the model is in place and is reported as skipped otherwise, as the
    other tests with the real engine are, makes one sentence with a word in no dictionary in each of the 13 voices,
    from the phonemes that eSpeak NG makes in the language of requirement 4, and finds audio that is not empty and
    holds no sample that is not a number. The implementer runs it on Linux x64 with both models in place, and the
    record holds that it ran and was not skipped.
11. The implementer looks at the settings page and at the tool window in the sandbox IDE as `AGENTS.md` says, in a
    light and a dark theme, with the list closed and open, and the record holds what the captures showed.
12. The developer reads `samples/markdown-features.md` on Linux x64 in Heart, in Emma and again in Heart, chosen on the
    settings page between the readings, and the record holds that each reading was in the voice chosen and that Emma
    pronounced the words the British way.

## Decisions

- **All 13 voices the human listened to.** The human chose the voices graded C+ or better, and the British voices
  graded C, and kept every one after listening. Voices of other languages are left out, since they read English text
  with the accent of their own language, and the English voices graded below C because their training audio was
  short. Rejected: all 28 English voices, and a shorter list cut by the human's ratings.
- **The order follows the human's ratings within each accent and sex.** American before British, since the plugin
  has read in an American voice from the start, and female before male, since Heart is female and leads the list as
  the voice in use until one is chosen. Within each group, the voices the human rated higher come first, with Nicole
  last among the American women for its whisper. Decided by the specifier and open to the human.
- **Names with their accent and sex in the list.** The names of Kokoro, such as `bf_emma`, code the accent and the sex
  in two letters that a user cannot be expected to read. Nicole is marked as whispering so that nobody takes it for a
  fault. Decided by the specifier and open to the human.
- **The pronunciation follows the voice.** The human asked that a British voice also pronounce the British way. Since
  unit 0030 the pronunciation is the language of eSpeak NG, so the voice alone chooses the language, whichever model
  is in use. The lexicons of the model play no part in the pronunciation, since the plugin gives the engine its
  phonemes, so a British voice needs no `lexicon-gb-en.txt` and a model needs no other file than unit 0030 asks for.
- **The language `en` for a British voice, and `en-us` for an American one.** `en-gb` gives no phonemes, and `en`
  gives British ones. `en-us` for an American voice keeps what the plugin does since unit 0030 exactly as it is.
- **A change of the voice is heard at the next reading.** Since unit 0030 no voice needs a new engine, so a change
  could be heard at once, as a change of the speed is. It is taken at the next reading, as unit 0020 does for the
  model, because the voice is chosen once on the settings page and not while listening, as the speed is in the tool
  window, and hearing it at once would add the restart of the piece being heard and its tests for a choice made
  rarely. Decided by the specifier and open to the human. Rejected: the voice changing during a reading.
- **Chosen on the settings page, shown in the tool window.** The human chose both. The tool window keeps the speed,
  which is changed while listening, and the voice is chosen once, so it is shown there read-only, as the model is.
- **The tool window shows the voice of the reading while there is one.** A voice chosen during a reading is heard
  only from the next reading, so a row that showed the choice at once would name a voice that is not the one heard.
  Decided by the specifier and open to the human.
- **The numbers are fixed in the plugin.** The Java binding gives no lookup by name, and the numbers are those of the
  `id2speaker` metadata of both models, which the specifier read from the fp32 model. The real engine test of
  requirement 10 would find a number that gave no sound, and a later model with other numbers is a new unit.
- **Checked on Linux only.** The voice is a speaker number handed to the same engine and a language handed to the same
  eSpeak NG on both platforms. Unit 0016 found that the int8 model loses pieces on macOS on Apple Silicon whatever the
  voice, and the default model on macOS is the fp32 one, as unit 0020 decided.

## Non-goals

- Voices of other languages, and the English voices graded below C.
- Reading text in another language than English.
- Mixing voices, or a voice for each file or each project.
- A button that plays a sample of a voice.
- Changing the speed, the estimate of the reading time of unit 0022, or the highlights of units 0025 to 0028 for a
  voice.

## Beyond the request

The human took in item 2, the voice in the tool window, which is requirement 7. Item 1 is left out:

1. A button "Play Sample" beside the list on the settings page that reads one sentence in the voice chosen in the
   list, before it is applied. It costs a button, a short reading outside the reading of a file that has to stop a
   reading or wait for it, and its tests. Without it, a voice is heard only by applying it and reading a file.
