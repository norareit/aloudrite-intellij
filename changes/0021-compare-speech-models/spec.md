# Compare speech models

## Context

The plugin speaks with Kokoro 1.0 through sherpa-onnx. Unit 0016 found that its int8 package loses pieces of a
reading on macOS on Apple Silicon, and unit 0020 lets the listener choose the fp32 package instead, which lost none
but is 350 MB to download and was not measured for whether the Mac makes its audio fast enough to stay ahead of the
playing.

sherpa-onnx speaks with more than Kokoro, and its releases hold packages of other models that may be smaller, faster
or both:

- Kokoro 1.0 in fp16, with the weights of the same network stored as 16-bit floating point numbers. The releases of
  sherpa-onnx have no such package of Kokoro, as item 5 of "Beyond the request" of unit 0020 says
- Piper voices, of the VITS family, such as `vits-piper-en_US-lessac-medium` at 67 MB and
  `vits-piper-en_US-lessac-high` at 115 MB, each also as an int8 and an fp16 package. They are known to be fast on a
  processor, and to sound flatter than Kokoro
- Kitten TTS, a small English model, as `kitten-nano-en-v0_8-int8` at 31 MB, `kitten-micro-en-v0_8` at 44 MB and
  `kitten-mini-en-v0_8` at 67 MB

Which of them, if any, is worth offering in the plugin depends on what nobody has measured here: whether each loses
pieces on either machine, how fast it makes its audio, and how it sounds over a long reading. The human asked for the
ideas to be kept as a unit of their own, and for the comparison to decide what comes next.

## Requirements

1. A measurement in the project's tests runs a model of each family above, Kokoro, VITS and Kitten, over every piece
   of `samples/markdown-features.md` at 1.0, 1.5 and 2.0, with the threads the plugin uses and with one thread. For
   each model and setup it writes, as `EngineSilenceMeasurement` of unit 0016 does, the pieces that came out as
   silence or as samples that are not a number, and also how long the audio took to make against how long it lasts.
2. The measurement runs only when asked for and with the model in place, and is reported as skipped otherwise, so the
   suite needs no model, no network and no audio device, as before.
3. The plugin itself is not changed. It speaks with the models of unit 0020 only.
4. The record holds the table of requirement 1 on Linux x64 and on macOS on Apple Silicon for at least these models:
   Kokoro 1.0 in int8, fp32 and fp16; `vits-piper-en_US-lessac-medium` and `vits-piper-en_US-lessac-high`; and
   `kitten-nano-en-v0_8-int8`, `kitten-micro-en-v0_8` and `kitten-mini-en-v0_8`. For a model the measurement could not
   run, it holds why.
5. The record holds, for each model of requirement 4, the developer's judgement after listening to the first chapter
   of prose of `samples/markdown-features.md` at 1.0 and 1.5: whether it is pleasant to listen to for a long reading,
   and what stood out.
6. The record ends with a recommendation of which model, if any, to offer next in the plugin, and on which platform
   by default, with the reasons from requirements 4 and 5.
7. `README.md` describes how the measurement of requirement 1 is run for each model.

## Decisions

- **A comparison before a feature.** Offering a model costs a package, an address, a checksum, a configuration in the
  engine and a voice, and the settings page of unit 0020 makes adding one cheap only once the right one is known.
  Measuring first keeps that cost for a model that earns it. Decided by the specifier and open to the human.
- **A feature rather than a task.** The unit adds test code, for the engines of the VITS and Kitten families, and
  leaves choices to make, such as the voice of each model and how fp16 is obtained. A task is an edit with no
  decision left open.
- **fp16 from a conversion of the project's own.** No release of sherpa-onnx has it. Converting the fp32 file of
  `kokoro-multi-lang-v1_0` to fp16 keeps the metadata that sherpa-onnx reads from the file, where a file from another
  project, as the exports of kokoro.js or kokoro-onnx, would lack it. If the converted file does not load or does not
  speak, the record says so under requirement 4. Decided by the specifier and open to the human.
- **One voice per model.** A voice of US English, as near to `af_heart` as the model offers, chosen by the developer
  when listening. Comparing every voice of every model would take longer than the comparison is worth.
- **The two platforms the plugin supports.** The loss of unit 0016 differs between them, so a model is judged on both.

## Non-goals

- Offering any of the models in the plugin. That is the unit the recommendation of requirement 6 leads to.
- Kokoro 1.1 and 0.19, which unit 0020 leaves out for reasons of their voices, and Matcha-TTS.
- Languages other than English.
- Mending the int8 model on macOS.

## Beyond the request

1. **Piper and Kitten in int8 and fp16 as well.** Each Piper voice and the Kitten nano model have such packages, and
   the int8 ones are a third of the size. It would triple the measurements of those models. Without it, the
   comparison takes one package of each, and a smaller one is measured later if that model is recommended.
2. **More Piper voices.** The releases hold several hundred, and some may sound better than lessac. It costs a listen
   per voice. Without it, lessac stands for the family, and other voices are compared if Piper is recommended.
