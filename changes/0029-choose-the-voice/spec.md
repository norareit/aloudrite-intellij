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
binding the plugin uses, and the human listened to all of them. The human rated them from 5 to 9 out of 10, found
none of them bad, and decided to offer all 13. `af_nicole` whispers, which the human keeps as a curiosity.

The rendering showed what a British voice needs to sound British:

- Each package holds a British lexicon, `lexicon-gb-en.txt`, beside the American `lexicon-us-en.txt` that the plugin
  gives the engine. A British voice with the American lexicon pronounces the words the American way.
- A word that is in neither lexicon, such as "AloudRite", is made into sounds by eSpeak NG, in the language the
  configuration names. With the language `en-gb`, eSpeak NG has no such voice, the engine fails to make any sound for
  the text, and gives an empty audio without an error to the plugin. With `en`, British English in eSpeak NG, each
  British voice gave its audio.
- The plugin today names no language, and the American voices gave their audio that way.

The lexicon and the language are part of the configuration of the engine, which the plugin creates once per model,
so a voice of the other accent needs an engine of its own. The speaker number is given with each piece, so a voice of
the same accent does not.

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
4. A reading in an American voice gives the engine `lexicon-us-en.txt` and names no language, as the plugin does now.
   A reading in a British voice gives the engine `lexicon-gb-en.txt` and the language `en`.
5. A voice chosen during a reading takes effect at the next reading, and the reading goes on in the voice it began
   with, as unit 0020 decided for the model.
6. The plugin keeps at most one engine in memory, as unit 0020 decided. A reading in a voice of the same accent as the
   engine in memory uses that engine. A reading in a voice of the other accent releases it and creates one with the
   lexicon and the language of requirement 4.
7. A model counts as in place only when its folder also holds `lexicon-gb-en.txt`, so that no engine is ever created
   with a lexicon that is missing. A folder without it is treated as a model that is not in place, as unit 0005 and
   unit 0020 treat any other missing file.
8. The tool window shows the voice at all times, also while nothing is read, and in every project, in a read-only row
   "Voice:" with the name of requirement 1, under the row of the model under the separator "Speech model". While a
   file is read or paused, it shows the voice of that reading. Otherwise it shows the voice chosen, within a second of
   the choice being applied on the settings page. The voice is chosen only on the settings page, and the link
   "Settings…" of the row of the model opens it.
9. `README.md` and the comments in the code no longer say that the plugin reads in `af_heart` only. `README.md` names
   the 13 voices, says that they come with either model without a download, that a British voice uses the British
   pronunciation, that a change of the accent creates the engine again, which may take a few seconds at the next
   reading, that a voice chosen during a reading is heard from the next reading on, and the row of the voice in the
   tool window.
10. Automated tests cover requirements 1 to 8 with the stand-ins of unit 0006 for the engine and the player, and need
    no model, no audio device and no network: the list and its order, the stored choice and a stored voice that is
    not in the list, the speaker number and the configuration given for each voice, a choice during a reading, the
    engine kept or created again, and the row of the tool window while nothing is read, during a reading and after a
    choice during a reading.
11. A test with the real engine, which runs where the model is in place and is reported as skipped otherwise, as the
    other tests with the real engine are, makes one sentence with a word in neither lexicon in each of the 13 voices,
    with the configuration of requirement 4, and finds audio that is not empty and holds no sample that is not a
    number. The implementer runs it on Linux x64 with both models in place, and the record holds that it ran and was
    not skipped.
12. The implementer looks at the settings page and at the tool window in the sandbox IDE as `AGENTS.md` says, in a
    light and a dark theme, with the list closed and open, and the record holds what the captures showed.
13. The developer reads `samples/markdown-features.md` on Linux x64 in Heart, in Emma and again in Heart, chosen on the
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
- **The pronunciation follows the voice.** The human asked that a British voice also pronounce the British way. Both
  models hold both lexicons, so the voice alone chooses the lexicon, whichever model is in use.
- **The language `en` for a British voice, and none for an American one.** The rendering showed that `en-gb` makes
  the engine give no sound at all, and that `en` works. Naming no language for an American voice keeps what the plugin
  says today exactly as it is, which the readings of every earlier unit rest on.
- **A change of the voice is heard at the next reading.** A voice of the other accent needs a new engine, which takes
  seconds and, during a reading, would mean a second engine in memory or a silence while it is created. Taking the
  choice at the next reading, as unit 0020 does for the model, avoids both and keeps the two choices alike. A change
  between two voices of the same accent could be heard at once, as a change of the speed is, but a choice that is
  heard at once for some voices and later for others would be hard to explain. Decided by the specifier and open to
  the human. Rejected: the voice changing during a reading.
- **Chosen on the settings page, shown in the tool window.** The human chose both. The tool window keeps the speed,
  which is changed while listening, and the voice is chosen once, so it is shown there read-only, as the model is.
- **The tool window shows the voice of the reading while there is one.** A voice chosen during a reading is heard
  only from the next reading, so a row that showed the choice at once would name a voice that is not the one heard.
  Decided by the specifier and open to the human.
- **The numbers are fixed in the plugin.** The Java binding gives no lookup by name, and the numbers are those of the
  `id2speaker` metadata of both models, which the specifier read from the fp32 model. The real engine test of
  requirement 11 would find a number that gave no sound, and a later model with other numbers is a new unit.
- **Checked on Linux only.** The voice is a number and a lexicon handed to the same engine on both platforms. Unit
  0016 found that the int8 model loses pieces on macOS on Apple Silicon whatever the voice, and the default model on
  macOS is the fp32 one, as unit 0020 decided.

## Non-goals

- Voices of other languages, and the English voices graded below C.
- Reading text in another language than English.
- Mixing voices, or a voice for each file or each project.
- A button that plays a sample of a voice.
- Changing the speed, the estimate of the reading time of unit 0022, or the highlights of units 0025 to 0028 for a
  voice.

## Beyond the request

The human took in item 2, the voice in the tool window, which is requirement 8. Item 1 is left out:

1. A button "Play Sample" beside the list on the settings page that reads one sentence in the voice chosen in the
   list, before it is applied. It costs a button, a short reading outside the reading of a file that has to stop a
   reading or wait for it, and its tests. Without it, a voice is heard only by applying it and reading a file.
