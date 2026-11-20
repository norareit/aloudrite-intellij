# Time the highlighted word by aligning the audio

## Context

Unit 0025 highlights the word being heard strongly, and unit 0026 brings that highlight to the preview. The engine
gives the times of whole pieces only, so both estimate the word from the share of the piece heard, weighing each word
by its letters. In the human's checks of unit 0025 at 1.0× and at 1.5×, the highlight skipped words, mostly short or
quickly spoken ones such as "and", "the" and "once", and more noticeably at 1.5×. Unit 0027 therefore starts the word
highlight cleared.

The human asked whether sherpa-onnx can give the times of the words. The specifier looked into it:

- The Kokoro model predicts how long each of its phoneme tokens lasts and returns it as a second output, in frames of
  600 samples, which add up exactly to the audio. sherpa-onnx discards it: its `GeneratedAudio` holds the samples
  and the sample rate only, in 1.13.8 and in 2.0.2, which the plugin uses since unit 0030. Its two open requests for
  timings of words in speech, issues 3705 and 3727, have no plan behind them from the maintainer.
- From 2.0.1 on, sherpa-onnx has forced alignment, with a Java API: given audio and its text, a `ForcedAligner` returns
  the start and the end of each word. Its English model, `english-mfa-v3.1.0`, a GMM-HMM model of the Montreal Forced
  Aligner under CC BY 4.0, is a download of 45 MB and takes 50 MB on disk.

A trial outside the project measured it on 11 sentences of `samples/markdown-features.md`, made by the fp32 Kokoro
model, against the times of the words that the model's own durations give. Sampled every 10 ms of speech, the word
shown was the word heard:

| | At 1.0× | At 1.5× |
|---|---|---|
| By the share of letters, as unit 0025 does | 54% | 52% |
| By forced alignment | 86% | 82% |
| By forced alignment, shown 70 ms later | 96% | 93% |

The aligner placed the start of each word 65 to 85 ms before the durations of the model did, always in that
direction. Aligning took 0.4 seconds for 59 seconds of audio on the AMD Ryzen 9 9950X3D of `README.md`, about 14 ms a
sentence. Words not in its dictionary, such as "AloudRite" or "recordFoundBoat", did not make it fail: it gave them a
short interval and kept the words around them in place.

The trial ran under sherpa-onnx 1.13.8. Unit 0030 moved the plugin to sherpa-onnx 2.0.2, and since then the plugin
makes the phonemes of each piece itself, with eSpeak NG through `piper-phonemize`, and hands them to the engine, so
that every word is spoken, words that 2.0.2 would skip as out of vocabulary among them. The audio of the sentences
that unit 0030 measured is within 0.5% of its length under 1.13.8, so the trial is taken to hold under 2.0.2.

This unit is built after unit 0027, whose choice of the word highlight it makes worth reconsidering.

## Requirements

1. The word that the strong highlight shows, in every view that shows it, is chosen from the times at which the audio
   of the piece being heard holds each of its words, found by aligning that audio with the text of the piece by the
   forced aligner of sherpa-onnx and its English model `english-mfa-v3.1.0`.
2. The word is shown a fixed time after the start the aligner gives for it, so that it is shown with the voice. The
   developer sets that time by ear in the check of requirement 10, starting from 70 ms, and the record holds the time
   set.
3. A piece whose alignment is not ready when its audio starts to play, which failed, or which did not give a start
   for each word of the piece, shows the word by the estimate of unit 0025 for as long as it has no alignment. A
   reading never waits for an alignment, and a failed alignment is logged and ends neither the reading nor the
   highlights.
4. Through the silence between two words, the highlight stays on the word before it, as through the silence after a
   piece in unit 0025.
5. The aligner model is downloaded together with a speech model, from the release of sherpa-onnx where it is
   published, into a folder beside the speech models. The question before a download of a speech model states the size
   of the aligner model in the size of the download where the aligner model is not yet on the machine. Where a speech
   model is in place and the aligner model is not, as for a listener who downloaded a speech model before this unit,
   the page "AloudRite" under Settings › Tools offers to download the aligner model on its own, and the reading shows
   the word by the estimate of unit 0025 until it is in place. Removing the last speech model removes the aligner model
   with it.
6. Automated tests cover requirements 1 to 4 with the stand-ins of unit 0006 for the engine and the player and a
   stand-in for the aligner, without a model, an audio device or a network.
7. A test with the real engine and the real aligner aligns the sentences of the trial at 1.0× and at 1.5×, and passes
   when the aligner gives each word one interval, in the order of the text, within the audio of the piece. It is
   skipped where either model is not in its folder.
8. Starting a reading, from the press to the first audio, takes no more than 50 ms longer than before this unit, on
   the machine of `README.md` with the fp32 model.
9. `README.md` describes how the word is now found and what is shown while a piece has no alignment, and the aligner
   model with its size, its download and its folder. `THIRD-PARTY.md` names the aligner model with its license.
10. The developer reads `samples/markdown-features.md` in the text editor at 1.0× and at 1.5× on Linux x64 and on macOS
    on Apple Silicon, with the word highlight checked, and the record holds whether the highlight keeps up with the
    voice and whether it still skips words. The check on macOS is asked for because the unit uses a part of the native
    library of sherpa-onnx that the plugin has not used before.
11. When the human has decided the blocking findings of the review, and before the status is set to `implemented`,
    the record holds the human's decision on whether "Highlight the word being read" of unit 0027 should start
    checked. A decision to change it is a unit of its own.

## Decisions

- **Forced alignment over the durations of the model.** The durations of Kokoro would be exact, but sherpa-onnx gives
  no way to them, in 2.0.2 as in 1.13.8. Since unit 0030 the plugin makes the phoneme tokens of each piece itself, so
  getting the durations would no longer mean rebuilding the conversion of text to phonemes. It would still mean
  running the model through ONNX Runtime ourselves in place of the engine of sherpa-onnx, with the voices, the
  splitting of long texts and the native libraries that come with it, and mapping the tokens back to the words; or
  keeping a fork of sherpa-onnx that returns them, which would mean building its native library for two platforms in
  this project. Both were rejected for their cost. Forced alignment is a released part of sherpa-onnx, in the release
  the plugin already uses, works on the audio alone, and came within a fixed delay of the durations in the trial.
  Decided by the specifier, from the trial, and open to the human since unit 0030 lowered the cost of the first
  alternative.
- **A better estimate was rejected.** Weighing words by their syllables or vowels in place of their letters is cheap,
  but stays a guess from the share of the piece, and the human asked for the times themselves.
- **A fixed delay, set by ear.** Which of the aligner and the durations of the model is nearer to the sound cannot be
  told from the trial, only that they differ by a near constant. The ear of the developer decides, starting from the
  70 ms that brought the trial to 96% and 93%. Decided by the specifier and open to the human.
- **Downloaded with a speech model, not in the plugin archive.** The aligner model would grow the plugin archive from
  about 22 MB to about 67 MB, at every update of the plugin, while the speech models are already downloaded once and
  kept. Downloading it with them keeps the download the plugin's only network request. Decided by the specifier and
  open to the human.
- **The estimate stays as the fallback.** A piece is aligned once its audio is complete, and the first piece of a
  reading starts to play before that. A failed or partial alignment, and a machine without the aligner model, would
  otherwise show no word at all.
- **English only.** The plugin reads with an English voice, and the aligner models of sherpa-onnx are English and
  Chinese. A piece the English model cannot align falls back by requirement 3.
- **The default of the word highlight is the human's.** Unit 0027 starts the word highlight cleared because the
  estimate skipped words. Whether this unit makes it good enough to start checked is for the human to judge once it is
  built, and is recorded by requirement 11. The human asked for that question to be part of this unit.

## Non-goals

- Changing the default of "Highlight the word being read" of unit 0027. Requirement 11 records the decision only.
- The highlight of the sentence, which is known exactly already.
- Timings of words for languages other than English.
- Exposing the durations of the Kokoro model through sherpa-onnx, or asking its maintainers to.
- Changing the speech models, the voice, or how a reading is cut into pieces.
- Moving to another release of sherpa-onnx, which unit 0030 did.

## Beyond the request

1. Keeping the times of a piece played again, as after a pause in its middle, rather than aligning it anew. It costs
   a small cache and a test. Without it, a piece played again is aligned again, which takes some 14 ms.
