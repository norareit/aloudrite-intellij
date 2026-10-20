# Reading speed

## Context

The plugin reads at the normal speed of the Kokoro model, which unit 0004 chose and every unit since has kept. Testing
the plugin, the human found that they miss a way to make the speech faster, and slower.

Unit 0015, a draft, adds the "AloudRite" tool window, which holds the controls of a reading, so that new controls do
not each need a button on the editor tab. The human chose it for the speed, and chose to build it before this unit.
Unit 0012, also a draft, makes the tab button pause and resume, puts a stop button beside it while a file is read, and
gives the tool window a pause, a resume and a stop. This unit is written to be built after both.

What units 0006 and 0012 established, and this unit builds on:

- The plugin cuts the text into pieces: a sentence, the part of a long sentence up to a comma, colon or semicolon,
  and 400 bytes at the most. The engine works ahead of the playing, by up to 30 seconds of audio.
- A stop and a pause silence the sound within one second.
- A resume starts at the beginning of the piece that was playing, and comes within one second because the audio made
  ahead is kept.

The engine of sherpa-onnx takes a speed with each piece it is given, and makes the audio of that piece at that speed.
A speed of 2 makes the words take half the time, without raising the pitch of the voice. Audio that was made cannot be
made faster or slower afterwards without processing of the sound that the plugin does not have. So the audio made
ahead holds the speed of the moment it was made.

## Requirements

1. The speed is a number from 0.5 to 2.0, in steps of 0.1, where 1.0 is the speed of the plugin before this unit. It
   is shown as the number with one decimal and a multiplication sign, as "1.0×".
2. The tool window of unit 0015 always shows the speed, with a "Slower" and a "Faster" button beside it, whether a
   file is read or not. A press on "Slower" lowers the speed by 0.1, and a press on "Faster" raises it by 0.1. At 0.5
   "Slower" is disabled, and at 2.0 "Faster" is.
3. The settings of the IDE have a page "AloudRite" under Tools, on which the speed can be chosen from the same values.
   The page and the tool window of every open project show the same speed, and a change in one shows in the others.
4. The speed is the same for the whole IDE, and is kept when the IDE is closed and opened again. Before the user has
   chosen one, it is 1.0.
5. A reading reads at the speed set when it starts.
6. When the speed changes while a file is read, the audio made ahead is dropped, the sound stops within one second,
   and the reading goes on from the beginning of the piece that was playing, at the new speed. What was already heard
   of that piece is heard again. The pieces after it are made at the new speed.
7. After several changes of the speed in quick succession during a reading, the piece is read once, at the last speed
   set. The audio of the speeds in between is not played.
8. When the speed changes while a reading is paused, nothing is heard until the reading is resumed, and the resume
   starts at the beginning of the piece that was playing at the pause, at the new speed. Such a resume may take longer
   than the one second of unit 0012, since the piece has to be made again.
9. The voice, its pitch, the sound and what is read are the same at every speed. Only the time that the words take
   changes.
10. At every speed, a reading of `samples/markdown-features.md` to its end has no silence in the middle of a sentence
    on the developer's two machines: the engine stays ahead of the playing as it does at 1.0.
11. Automated tests cover requirements 1 to 8 with the stand-ins of unit 0006 for the engine and the player: the
    values and their display, the limits of the buttons, the speed handed to the engine with each piece, a speed kept
    and given back after the plugin's settings are saved and loaded, a change during a reading, several changes in a
    row, and a change while paused. They need no model, no audio device and no network.
12. The developer has listened to a reading at 0.5, 0.8, 1.0, 1.5 and 2.0, and changed the speed during a reading,
    several times in a row and while paused, on Linux x64 and on macOS on Apple Silicon. The record holds how
    understandable each speed was, the time from a change during a reading to the sound at the new speed, and what
    they found for requirement 10. Where a speed is not understandable, the unit stops and the human sets the range.
13. `README.md` describes the speed, its range, where it is set and what a change during a reading does.

## Decisions

- **The speed is set in the tool window and on a settings page.** The human chose the tool window for the controls of
  a reading, and to keep the speed. A control that changes what is heard belongs where the reading is controlled, and
  a kept value belongs in the settings, where the human expects the voice to go later as well. Both show one value, so
  that there is no default speed beside a current one to tell apart.
- **A change during a reading starts the piece again.** The human chose this. The audio made ahead holds the old
  speed, up to 30 seconds of it, so a change heard only after it would come far too late. Starting the piece that is
  playing again at the new speed is the same rule as the resume of unit 0012, and a listener who changes the speed
  hears at once what the new speed sounds like on words they just heard. Rejected: going on at the new speed from the
  next piece, which still has to make the audio ahead again and so leaves a gap in the reading; and a change that
  only the next reading hears, which cannot speed up what is being listened to.
- **No limit on the time to the new speed.** The engine has to make the piece again before it is heard, and a piece
  can be up to 400 bytes, which takes several seconds. Requirement 12 has the developer measure it instead, so that
  the human can decide whether it needs a unit of its own. Decided by the specifier and open to the human.
- **From 0.5 to 2.0, in steps of 0.1.** The engine takes any speed, but its voice is made for 1.0, and how it sounds
  far from it is not known. The range is the one most players of spoken audio offer, and steps of 0.1 let a listener
  find a speed without passing it. Requirement 12 checks that the ends are understandable. Decided by the specifier
  and open to the human.
- **One speed for the whole IDE.** One file is read at a time in the whole IDE, and a speed is a matter of the
  listener, not of the project. Rejected: a speed per project or per file, which the listener would have to set again
  wherever they read.
- **The speed comes from the engine, not from the playing.** Playing the audio faster raises the pitch, and keeping
  the pitch while playing faster needs processing of the sound that the plugin does not have. The engine makes the
  words take less time with the same voice. Rejected: playing faster, for the pitch.
- **Written for after units 0015 and 0012.** The tool window must exist for the controls, and requirement 8 needs the
  pause. Building this before unit 0012 would leave requirement 8 without a pause to change the speed in.

## Non-goals

- Choosing the voice. The settings page of this unit is where it can go later.
- A speed per project or per file.
- Keyboard shortcuts for "Slower" and "Faster".
- Making a change during a reading heard sooner than the engine makes the piece again.
- Changing the speed of the pauses that the plugin adds, such as the two seconds after "code example". They stay as
  long as unit 0010 made them, at every speed.
- Speeds below 0.5 or above 2.0.

## Beyond the request

- **Pauses that follow the speed.** At 2.0 the pauses of unit 0010 would be half as long, which keeps the rhythm of
  the reading the same at every speed. It costs dividing each pause by the speed, and a decision on the two seconds
  after "code example", which are there to give the listener time to pause by hand. Without it, the pauses are as
  long at every speed, which at 2.0 makes them stand out more.
- **Keyboard shortcuts for the speed.** A listener with their hands on the keyboard could change the speed without
  the mouse. It costs two shortcuts that do not clash with those of the IDE. Without it, the speed is one click in the
  tool window.
