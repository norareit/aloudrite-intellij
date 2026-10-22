# Choose the speech model

## Context

The plugin speaks with one model, the int8 package `kokoro-int8-multi-lang-v1_0` of sherpa-onnx, which unit 0004
chose and unit 0005 downloads the first time "Read Aloud" is pressed without it. It is 132 MB to download and 190 MB
unpacked, and it lives in a folder of its own per user, outside the directories of any IDE.

Unit 0016 found, with a measurement it keeps in the project, that this model loses pieces of a reading on macOS on
Apple Silicon. The engine makes such a piece as samples that are all not a number, which the plugin plays as silence.
Of the 136 pieces of `samples/markdown-features.md`, with 6 threads, it lost 1 at the normal speed, 30 at 1.5 and 56
at 2.0, and with 1 thread 129, 118 and 112. A second try of a lost piece gave sound only for some. On Linux x64 it
lost none. The fp32 package of the same model, `kokoro-multi-lang-v1_0`, lost none on either machine, with 6 threads
or with 1, at any of the three speeds. It is 350 MB to download and 384 MB unpacked. Whether the Mac makes its audio
fast enough to stay ahead of the playing was not measured. Issue 3754 of sherpa-onnx reports the same kind of loss
with the int8 model on ARM, and none with fp32.

The two packages have the same voices, `af_heart` among them with the same speaker number, the same tokens, lexicon
and data, and differ in the file of the model, `model.int8.onnx` against `model.onnx`. The human asked for a way to
manage the model on the plugin's settings page, the page "AloudRite" under Tools of unit 0016, so that a listener can
switch between the int8 and the fp32 model and download them from there.

What unit 0005 established, and this unit builds on:

- A download asks first, says how large it is, how much space it takes and where it goes, runs in the background with
  its progress in the status bar, can be cancelled, is checked against a SHA-256, and puts the model in place with one
  rename of a complete folder. It uses the proxy settings of the IDE
- One download runs at a time, also across IDE windows, held by a lock file next to the model folder
- A model folder that the plugin did not make and that is not complete is not touched
- A press on "Read Aloud" without the model asks whether to download it, and reads the file once it is in place

## Requirements

1. The plugin knows two models, which the settings page names "Kokoro 1.0, int8" and "Kokoro 1.0, fp32". Each has its
   own package, address, size, unpacked size, SHA-256 and folder, and the folders lie side by side, as
   `kokoro-int8-multi-lang-v1_0` and `kokoro-multi-lang-v1_0` in the folder of unit 0005, with the name of the file of
   the model each holds. The models are the entries of one list, and the settings page, the download, the removal and
   the reading take the models from that list and name none of them. Another package of Kokoro 1.0, such as one in
   fp16, is added by adding an entry to the list, and the rest of the code does not change.
2. The settings page "AloudRite" has a part "Speech model" under the speed of unit 0016. It lists both models, each
   with its size to download, the space it takes, and whether it is on the machine. One of them is marked as the model
   in use, and it can be chosen there.
3. The model in use is the same for the whole IDE and is kept when the IDE is closed and opened again. Before the user
   has chosen one, it is the int8 model on Linux x64 and the fp32 model on macOS on Apple Silicon. That default is
   stated in one place in the code, as which entry of the list of requirement 1 each platform takes, so that changing
   it changes that place and the test that pins it, and nothing else. The default is not saved as a choice: a user who
   has not chosen a model gets the default of the version of the plugin they run, so a release with another default
   moves them to it, and a user who has chosen keeps their choice.
4. A model that is not on the machine can be downloaded from the page. The download behaves as that of unit 0005: it
   says what it will download, how much and where before anything is fetched, runs in the background with its
   progress in the status bar, can be cancelled, is checked against its SHA-256, and leaves nothing of a failed or
   cancelled try but the lock file. One download runs at a time across both models and across IDE windows, and the
   page shows that a download is running.
5. Choosing a model that is not on the machine asks whether to download it. The model is in use once it is in place,
   and the one before stays in use until then. Declining leaves the model in use as it was.
6. A model that is on the machine and not in use can be removed from the page, after a question that names the space
   it frees. Its folder is then gone. The model in use cannot be removed, and a folder the plugin did not make, as
   unit 0005 says, is not removed.
7. A reading reads with the model in use when it starts. A choice made during a reading takes effect at the next
   reading, and the reading goes on with the model it began with.
8. A press on "Read Aloud" without the model in use on the machine asks whether to download that model, as unit 0005
   does for the int8 model, and reads the file once it is in place.
9. The page, the tool windows and a reading agree on the model in use: a choice on one settings page shows on another
   that is open, as the speed of unit 0016 does.
10. The plugin uses no more memory for its models than one engine of the model in use, outside the moment a reading
    with a newly chosen model begins. The engine of the model that was in use is released once no reading uses it.
11. Automated tests cover requirements 1 to 10 with stand-ins for the network, the engine and the player: the two
    models and their folders, the default per platform, a default that is not saved as a choice and so follows a
   changed default, the choice kept after the settings are saved and loaded, a
    download from the page and its refusal while another runs, a choice of a model not on the machine and its
    decline, a removal and its refusal for the model in use, a reading that keeps its model when the choice changes,
    and a press on "Read Aloud" without the model in use. They need no model, no audio device and no network.
12. On macOS on Apple Silicon, the developer has downloaded the fp32 model from the page, chosen it, and listened to
    `samples/markdown-features.md` to the end of the first chapter of prose at 1.0, 1.5 and 2.0. The record holds,
    for each speed, whether a part of the text was not heard, and whether a silence came in the middle of a sentence,
    which would say that the engine does not stay ahead of the playing. `EngineSilenceMeasurement` with the fp32 model
    on that machine is in the record of unit 0016 and need not be run again.
13. On Linux x64, the developer has downloaded the model that was not on the machine from the page, switched between
    the two, read a file with each, and removed one. The record holds that each step did what requirements 2 to 8 say.
14. `README.md` describes the two models, their sizes, the default per platform and why, where the model is chosen,
    downloaded and removed, and the commands to put either model in place by hand.

## Decisions

- **The two packages of Kokoro 1.0.** They share every file but the model, have the same voice under the same number,
  and so differ for the listener only in how they sound, how fast they are and whether they lose pieces. A third
  variant is not the same kind of choice, as "Beyond the request" says. Decided by the specifier and open to the human.
- **The fp32 model by default on macOS.** On macOS the int8 model loses pieces of a reading at every speed, and more on
  a Mac with fewer processors, where the engine gets fewer threads. A listener there would otherwise first hear a
  reading with parts missing before finding the cause on a settings page. On Linux x64 the int8 model loses nothing
  and is the smaller download. Requirement 12 checks that the Mac keeps up with fp32. Decided by the specifier and open
  to the human. Rejected: the int8 model by default everywhere, which keeps the download small and leaves the loss on
  the Mac until a listener finds the page.
- **The default and the models are easy to change.** The human asked for it. Whether the Mac keeps up with fp32 is
  not known until requirement 12 is checked, and if it does not, the default there goes back to int8, or later to a
  package in fp16 as item 5 of "Beyond the request" says. So the default per platform is one statement, and a model
  is one entry of a list that everything else iterates. Leaving the default unsaved until the user chooses means such
  a change reaches the listeners who never chose, rather than only new installations. Rejected: saving the default at
  the first start, which would keep every existing listener on a default that a later release found wrong.
- **The model is managed on the settings page.** The human asked for it there. The page holds what is kept for the
  IDE, as the speed, and the voice can go there later as well. The tool window controls a reading and does not grow a
  second purpose.
- **A choice during a reading waits for the next reading.** Changing the model during a reading would make the audio
  ahead again with another engine, which the speed of unit 0016 does with one engine and a speed, and here also loads
  a model of several hundred megabytes in the middle of a reading. A listener changes the model seldom, and not to hear
  the difference in the middle of a sentence. Decided by the specifier and open to the human.
- **A model is removed only when it is not in use.** Removing the model in use would leave the plugin without a model,
  which the first press of "Read Aloud" would then download again. Decided by the specifier and open to the human.
- **Written for after unit 0016.** The settings page this unit adds to, and the measurement and findings it builds on,
  come with unit 0016, so this unit is built on a default branch that holds it.
- **One download at a time across both models.** The lock of unit 0005 keeps two IDE windows from downloading the same
  model at once. Two downloads of two models at once would also halve the speed of each, and a listener needs one
  model to read. Decided by the specifier and open to the human.

## Non-goals

- Choosing the voice.
- Models of other kinds than Kokoro, and models from other addresses than the releases of sherpa-onnx.
- Changing the model during a reading so that it is heard at once.
- Mending the int8 model on macOS, which is a matter of sherpa-onnx and onnxruntime.

## Beyond the request

1. **Kokoro 1.1, int8 and fp32.** The packages `kokoro-int8-multi-lang-v1_1` and `kokoro-multi-lang-v1_1` have 103
   voices against 53. The documentation of sherpa-onnx does not say whether `af_heart` is among them and under which
   number, so each would need that found out, a speaker number per model, and the same check of lost pieces on both
   machines. Without it, the two models of 1.0 are the choice.
2. **Kokoro 0.19, English only.** The packages `kokoro-en-v0_19` and `kokoro-int8-en-v0_19` have 11 voices and no
   `af_heart`, so they need a choice of voice, which is a non-goal here. Without it, they are not offered.
3. **A measurement on the page of how fast the chosen model is on the machine.** It would show whether the machine
   keeps up before a reading proves it, at the cost of a short run of the engine from the page. Without it, the
   developer's checks and a reading show it.
4. **The debug output of sherpa-onnx turned off.** The plugin creates the engine with the default configuration of
   the binding, which has `debug` on, so the native code writes a line per piece to the error output of the IDE. It
   costs one setting and a check that nothing else depends on it. Without it, the lines go on to an output nobody
   reads.
5. **Kokoro 1.0, fp16.** A third package of the same network, with its weights stored as 16-bit floating point
   numbers, would be about half the size of the fp32 package, and on Apple Silicon, which computes with such numbers
   natively, it may be faster than fp32 without the loss of int8. The releases of sherpa-onnx have no such package of
   Kokoro, so it would come from another address or from a conversion of the project's own, carrying the metadata
   that sherpa-onnx reads from the model file, and it would need the same check of lost pieces on both machines. Unit
   0021 compares it with other models. Without it, the two packages of 1.0 are the choice, and fp16 is worth taking up
   if requirement 12 finds that the Mac does not stay ahead of the playing with fp32.
