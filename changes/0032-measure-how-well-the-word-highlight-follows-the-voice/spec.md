# Measure how well the word highlight follows the voice

## Context

Unit 0025 highlights the word being heard. The engine gives the times of whole pieces only, so the plugin estimates
the word from the share of the piece heard, weighing each word by its letters. Unit 0028 would replace that estimate
by forced alignment, with a 45 MB aligner model downloaded beside the speech models.

The spec of unit 0028 rests on a trial outside the project, which ran under sherpa-onnx 1.13.8. On 11 sentences of
`samples/markdown-features.md`, sampled every 10 ms of speech, the word shown was the word heard 54% of the time at
1.0× and 52% at 1.5× by the estimate, and 96% and 93% by forced alignment shown 70 ms later. The script of the trial is
not in the project, and which 11 sentences it used is not recorded.

Since unit 0030, the plugin reads with sherpa-onnx 2.0.2 and makes the phonemes of each piece itself. The human has
the impression, on Linux x64 and on macOS, that the word highlight follows the voice much better since then, which unit
0030 recorded as not measured. If the estimate is now close to what forced alignment reached, unit 0028 may not be
worth its download and its code. The human wants that measured before unit 0028 is built, in a way that unit 0028 can
measure again after it is built, so that the two can be compared and unit 0028 dropped if it brings too little.

The true times of the words come from the Kokoro model itself, which predicts how long each of its phoneme tokens
lasts. The model file that the plugin uses, `model.onnx` of the sherpa-onnx package, has one output, the audio, so
the durations cannot be read from it. The Kokoro Python package, `kokoro` 0.9.4 on PyTorch, runs the same model,
Kokoro v1.0, from its original weights, and returns the durations in frames of 600 samples, which add up to its
audio. The trial of unit 0028 used it. A Python environment with it, `kokoro-env`, and the weights, `Kokoro-82M`, lie
in the developer's working copy, ignored by git.

## Requirements

1. A measurement, run on demand as the other measurements of `README.md` are, finds for every piece of words of
   `samples/markdown-features.md`, as the plugin cuts the file into pieces for a reading from its start, read in Heart
   with the fp32 model at 1.0× and at 1.5×, the share of the time the word heard is the word that the highlight shows.
2. The word that the highlight shows at a moment is the one that the plugin's own estimate gives for that moment of
   the piece, from the audio that sherpa-onnx makes for the piece, taken from the code of the plugin and not written
   again for the measurement, so that a change of the estimate, as unit 0028 would make, is measured by running the
   measurement again.
3. The word heard at a moment is the word whose phoneme tokens the model is saying then, by the durations the Kokoro
   Python package gives for the same phonemes, the same voice and the same speed as the plugin hands sherpa-onnx.
4. The moments are taken every 10 ms of each piece, and only those within the tokens of a word count: the pauses
   between words, the punctuation and the silence at the start and the end of a piece are not counted.
5. A piece is left out of the shares, and named in the output with the reason, where the length of its audio by the
   durations differs from that of the audio sherpa-onnx makes for it by more than 2%, or where its phoneme words cannot
   be paired one to one with the words of the plugin, as where eSpeak NG reads one written word as several.
6. The measurement writes to a file under `build/`, as the other measurements do: for each speed, the share over all
   pieces counted, weighted by their time, the number of pieces counted and left out, and for each piece its share or
   the reason it was left out.
7. Automated tests cover the logic of the measurement that is new, the pairing of phoneme words with the words of the
   plugin, the word heard at a moment by the durations, the moments that count and the share, with plain values and
   without a model, Python or an audio device.
8. `README.md` names the measurement under "Testing", with its command, and says how to set up what it needs: the
   Python environment with the versions of `kokoro` and of PyTorch it was run with, where the weights of Kokoro v1.0
   and the voice `af_heart` come from, and that the measurement is skipped where they or the speech model are missing.
9. The record holds the shares at 1.0× and at 1.5× of the run on the developer's Linux x64 machine, the pieces counted
   and left out, and beside them the 54% and 52% of the trial of unit 0028 under 1.13.8, with the note that the trial
   used other sentences and its own way of counting.
10. The plugin reads, sounds and highlights as before: the unit changes nothing outside the tests, the measurement and
    the documents.

## Decisions

- **The durations of the model as the truth.** They are what the model itself decided each sound lasts, so they
  hold for every piece and every speed without listening, as in the trial of unit 0028. Timing words by hand would be
  nearer to what is heard, but slow and on a few sentences only. Decided with the human.
- **Heart, the fp32 model, 1.0× and 1.5×.** Heart is the voice in use until another is chosen, and the trial used it,
  so the result can be put beside the trial's. The British voices of unit 0029 are left out. 1.5× is where the human
  heard the estimate skip the most. Decided with the human for the voice, and by the specifier for the rest.
- **The plugin's own pieces and estimate.** The measurement runs the plugin's code from the text to the estimate, so
  it measures what a listener sees, and unit 0028 measures its own way by running it again. A copy of the estimate in
  the measurement would measure the copy. Decided by the specifier.
- **Every piece of the sample, not 11 sentences.** The trial's sentences are not known, and the whole sample holds
  headings, lists, links and code as well as prose, so a share over all of it says more about real reading. Decided by
  the specifier and open to the human.
- **Only the time of the words counts.** In a pause no word is being said, so the word shown then can be neither
  right nor wrong. The trial counted speech only as well. Decided by the specifier.
- **2% for the lengths.** The Kokoro Python package runs the original weights on PyTorch and sherpa-onnx a converted
  model on ONNX Runtime, so their audio may differ slightly. A larger difference means that the durations do not
  describe the audio the plugin plays, and the piece cannot be judged. Decided by the specifier and open to the human.
- **Run on the developer's machine.** The Python environment and the weights are on the host, and the sandbox of
  AgentRite reaches neither the Python Package Index nor Hugging Face. The code and the tests of item 7 can be made in
  the sandbox, and the run of item 9 is made on the host. Decided by the specifier.

## Non-goals

- Making the estimate better. The unit measures it only.
- The delay between the position the plugin reads from the audio line and what comes out of the speakers, which may
  differ between Linux x64 and macOS. The measurement works on the audio itself, as unit 0028 would.
- Other voices, other speeds and the int8 model.
- Running the measurement in the sandbox or on macOS.
- Changing the spec of unit 0028, such as adding what it must improve to be kept. That is decided once this unit has
  measured.
- Shipping the Kokoro Python package or its weights with the plugin, or naming them in `THIRD-PARTY.md`, since they are
  used by the measurement only.

## Beyond the request

1. Also counting the pauses, with the word before as the right one, as the plugin shows it. It costs a second share
   in the output and its tests. Without it, a highlight that moves on too early within a pause goes unnoticed, which is
   rare since a pause is short.
2. Repeating the trial's forced alignment in this measurement, so that both are known on the same pieces before unit
   0028 is built. It costs the aligner model in the measurement and a part of the work of unit 0028 done twice. Without
   it, the comparison waits until unit 0028 is built.
