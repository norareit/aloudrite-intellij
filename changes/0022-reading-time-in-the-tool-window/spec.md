# Reading time in the tool window

## Context

The tool window "AloudRite" of unit 0015 shows which file is read, and holds the pause, the stop and the speed of
units 0012 and 0016. It does not show how long a reading takes or how much of it is left: unit 0015 lists how far
the reading has come as a non-goal. A listener who starts a long file has no way to tell whether it lasts two minutes
or twenty, or how long is left after a pause.

The human asked for an estimated reading time and a remaining reading time in the tool window, as an idea for a later
unit once the tool window and its controls were built.

The engine makes the audio a few pieces ahead of the playing, so the length of the audio is known only for the pieces
made so far. The time of the whole file before its end is therefore an estimate from the text that is left.

Having read the spec, the human took in the bar of progress that was item 1 of "Beyond the request", and asked to
know how accurate the estimate is before the unit is merged, measured without the developer's help.

## Requirements

1. While a file is read, the tool window shows the estimated time of the whole reading and the time that is left, in
   minutes and seconds, below the name of the file.
2. The time left goes down while the audio plays, stands still while the reading is paused, and is updated at least
   once a second.
3. Both times are for the speed in use: within one second of a change of the speed, they show the time at the new
   speed.
4. For the part of the file whose audio is made, the time left counts that audio's length. For the rest, it counts an
   estimate from the length of its text at the speed in use.
5. While a file is read, the tool window shows a bar of progress beside the times. It shows the share of the file's
   text that has been read: it is empty at the start of a reading, moves forward at least once a second while the
   audio plays, stands still while the reading is paused, and never moves back, also not after a change of the speed
   or of the estimate.
6. When nothing is read, the tool window shows no time and no bar, as it does now.
7. Automated tests cover requirements 1 to 6 with the stand-ins of unit 0006 for the engine and the player: the times
   and the bar at the start of a reading, while it plays, while it is paused, after a change of the speed, and when it
   ends. They need no model, no audio device and no network.
8. A measurement in the project's tests reads `samples/markdown-features.md` and `samples/ten-thousand-words.md` with
   the real engine, with each of the two models of unit 0020, at 1.0, 1.5 and 2.0, and writes a table to `build/`. For
   each file, model and speed, it holds the length of the whole audio, and the estimate of the whole reading the tool
   window would show after the first piece and when a quarter, a half and three quarters of the audio have played,
   each with its error in seconds and in percent of the length of the whole audio. It runs only when asked for and with
   the model in place, and is reported as skipped otherwise, as `EngineSilenceMeasurement` of unit 0016 is.
9. The record holds the table of requirement 8 on Linux x64, measured by the implementer, before the unit is set to
   `implemented`.
10. The developer has read `samples/markdown-features.md` to the end on Linux x64 and the record holds how far the
    first estimate of the whole reading was from the time the reading took.
11. `README.md` describes the times and the bar the tool window shows, and how the measurement of requirement 8 is
    run.

## Decisions

- **An estimate from the text, made better by the audio.** The plugin could make all the audio first and know the
  time exactly, but that costs the time of the whole reading in work before the first word, and the memory of its
  audio. Counting the length of the text that is left at a rate learnt from the audio made so far gives a time from
  the first piece on, which gets more exact as the reading goes on. Decided by the specifier and open to the human.
- **In the tool window only.** It is where the reading's controls are. The tab button has no room, as unit 0015 says.
- **The bar shows the text that has been read, not the time.** A bar of the time that has played against the estimate
  of the whole would move back whenever the estimate grows, which looks like a fault, and would jump at a change of
  the speed. The share of the text only grows, and does not depend on the estimate or the speed. Within the piece that
  plays, the bar moves with its audio, so that it moves at least once a second and not only at the end of a sentence.
  Decided by the specifier and open to the human. Rejected: the share of the time, for the reasons above.
- **The accuracy is measured against the length of the audio, by the implementer.** The human asked for it without
  their help. The real engine makes the audio without an audio device, so the length of each file's whole audio is
  known exactly and can be compared with what the estimate said at each point, for both models, three speeds and a
  short and a long file, in minutes rather than in the hours that listening would take. The length of the audio is
  the time of the reading as long as the engine stays ahead of the playing, which it does on Linux x64 with either
  model, as `README.md` says for the speed of each. The developer's reading of requirement 10 is the one check of the
  time the playing really takes, with its pauses between pieces. The unit sets no bound the error must stay within:
  nobody knows yet what a text estimate achieves, and the table is what lets the human judge it before the merge.
  Decided by the specifier and open to the human. Rejected: a bound in the spec, which would be a guess.
- **Checked on Linux only.** The times are user interface of the IntelliJ Platform and arithmetic, with nothing that
  differs between the platforms, as unit 0015 decided for the tool window.

## Non-goals

- Showing or highlighting the text being read.
- Jumping to another place in the reading.
- The time of a reading of a selection or from the caret, beyond what requirement 1 says for any reading.

## Beyond the request

The human took in the first and only item, a bar of progress beside the times, which is requirement 5.
