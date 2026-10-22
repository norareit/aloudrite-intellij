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

## Requirements

1. While a file is read, the tool window shows the estimated time of the whole reading and the time that is left, in
   minutes and seconds, below the name of the file.
2. The time left goes down while the audio plays, stands still while the reading is paused, and is updated at least
   once a second.
3. Both times are for the speed in use: within one second of a change of the speed, they show the time at the new
   speed.
4. For the part of the file whose audio is made, the time left counts that audio's length. For the rest, it counts an
   estimate from the length of its text at the speed in use.
5. When nothing is read, the tool window shows no time, as it does now.
6. Automated tests cover requirements 1 to 5 with the stand-ins of unit 0006 for the engine and the player: the times
   at the start of a reading, while it plays, while it is paused, after a change of the speed, and when it ends. They
   need no model, no audio device and no network.
7. The developer has read `samples/markdown-features.md` to the end on Linux x64 and the record holds how far the
   first estimate of the whole reading was from the time the reading took.
8. `README.md` describes the times the tool window shows.

## Decisions

- **An estimate from the text, made better by the audio.** The plugin could make all the audio first and know the
  time exactly, but that costs the time of the whole reading in work before the first word, and the memory of its
  audio. Counting the length of the text that is left at a rate learnt from the audio made so far gives a time from
  the first piece on, which gets more exact as the reading goes on. Decided by the specifier and open to the human.
- **In the tool window only.** It is where the reading's controls are. The tab button has no room, as unit 0015 says.
- **Checked on Linux only.** The times are user interface of the IntelliJ Platform and arithmetic, with nothing that
  differs between the platforms, as unit 0015 decided for the tool window.

## Non-goals

- A bar of progress, or showing or highlighting the text being read.
- Jumping to another place in the reading.
- The time of a reading of a selection or from the caret, beyond what requirement 1 says for any reading.

## Beyond the request

1. **A bar of progress beside the times.** It shows at a glance how far the reading has come. It costs a control and
   its tests. Without it, the two times say the same in numbers.
