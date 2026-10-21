# Reading speed

## Context

The plugin reads at the normal speed of the Kokoro model, which unit 0004 chose and every unit since has kept. Testing
the plugin, the human found that they miss a way to make the speech faster, and slower.

Unit 0015 added the "AloudRite" tool window, which holds the controls of a reading, so that new controls do not each
need a button on the editor tab. The human chose it for the speed, and chose to build it before this unit. Unit 0012
made the tab button pause and resume, put a stop button beside it while a file is read, and gave the tool window a
pause, a resume and a stop. This unit is written to be built after both.

What units 0006, 0010 and 0012 established, and this unit builds on:

- The plugin cuts the text into pieces: a sentence, the part of a long sentence up to a comma, colon or semicolon,
  and 400 bytes at the most. The engine works ahead of the playing, by up to 30 seconds of audio.
- The plugin adds silences of set lengths where the structure of the document asks for them: half a second between two
  paragraphs and in place of a horizontal rule, a quarter of a second around inline code and between two items of a
  list, and two seconds after the words "code example", long enough for the listener to pause the reading by hand.
- A stop and a pause silence the sound within one second.
- A resume starts at the beginning of the piece that was playing, and comes within one second because the audio made
  ahead is kept. A resume of a pause made during a silence starts at the piece after it.

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
3. The speed in the tool window is shown in a field in which a speed can be typed, followed by "×". The field accepts
   a number from 0.5 to 2.0 with one decimal at the most, with a point as the decimal sign, such as "2" or "1.5". A
   value it accepts sets the speed when Enter is pressed or the field loses focus, and is then shown with one decimal.
   The speed changes once, also when the typed value is far from the one before, so going from 1.0 to 2.0 this way is
   one change and not ten. A value it does not accept leaves the speed as it was, and the field shows the error mark
   of the IDE with a message that names the range, until the value is corrected or Escape puts the speed back in it.
4. The settings of the IDE have a page "AloudRite" under Tools, on which the speed can be chosen from the same values.
   The page and the tool window of every open project show the same speed, and a change in one shows in the others.
5. The speed is the same for the whole IDE, and is kept when the IDE is closed and opened again. Before the user has
   chosen one, it is 1.0.
6. A reading reads at the speed set when it starts.
7. When the speed changes while a file is read, the audio made ahead is dropped, the sound stops within one second,
   and the reading goes on from the beginning of the piece that was playing, at the new speed. What was already heard
   of that piece is heard again. The pieces after it are made at the new speed. A change during a silence that the
   plugin adds goes on at the piece after the silence, as a resume does in unit 0012.
8. After several changes of the speed in quick succession during a reading, the piece is read once, at the last speed
   set. The audio of the speeds in between is not played.
9. When the speed changes while a reading is paused, nothing is heard until the reading is resumed, and the resume
   starts where the resume of unit 0012 starts, at the new speed. Such a resume may take longer than the one second of
   unit 0012, since the piece has to be made again.
10. Each silence that the plugin adds lasts its length at 1.0 divided by the speed: at 2.0 the half second between two
    paragraphs is a quarter of a second, and at 0.5 it is a second. The two seconds after "code example" are the
    exception, and last two seconds at every speed.
11. The voice, its pitch, the sound and what is read are the same at every speed. Only the time that the words and the
    silences of requirement 10 take changes.
12. At every speed, a reading has no silence in the middle of a sentence on the developer's two machines: the engine
    stays ahead of the playing as it does at 1.0. Requirements 14 and 15 check it.
13. Automated tests cover requirements 1 to 10 with the stand-ins of unit 0006 for the engine and the player: the
    values and their display, the limits of the buttons, the values the field accepts and those it does not, a typed
    speed far from the one before as one change, the speed handed to the engine with each piece, the length of each
    silence at a speed, a speed kept and given back after the plugin's settings are saved and loaded, a change during
    a reading and during a silence, several changes in a row, and a change while paused. They need no model, no audio
    device and no network.
14. On Linux x64, the developer has listened to the first five paragraphs of `samples/ten-thousand-words.md` at every
    speed from 0.5 to 2.0, and has changed the speed during a reading with the buttons and with the field, several
    times in a row, and while paused. The record holds how understandable each speed was, whether a silence came in
    the middle of a sentence at any of them, and the time from a change during a reading to the sound at the new
    speed. Where a speed is not understandable, the unit stops and the human sets the range.
15. On Linux x64 and on macOS on Apple Silicon, the developer has listened to `samples/markdown-features.md` to its end
    at 0.5, 1.5 and 2.0. The record holds, for each machine and speed, whether a silence came in the middle of a
    sentence, and how the silences of requirement 10 sounded.
16. `README.md` describes the speed, its range, where it is set, how the silences follow it and what a change during a
    reading does.

## Decisions

- **The speed is set in the tool window and on a settings page.** The human chose the tool window for the controls of
  a reading, and to keep the speed. A control that changes what is heard belongs where the reading is controlled, and
  a kept value belongs in the settings, where the human expects the voice to go later as well. Both show one value, so
  that there is no default speed beside a current one to tell apart.
- **A field to type the speed in, beside the buttons.** The human asked for it. Every press on a button is a change of
  requirement 7, which drops the audio ahead and has the engine make the piece that is playing again, so going from
  1.0 to 2.0 with "Faster" makes that piece ten times, for nine speeds that are never heard. A typed speed is one
  change, however far it goes. The buttons stay for a small step, to find a speed by ear. A value is set on Enter or
  when the field loses focus, and not while it is typed, so that "1" on the way to "1.5" is no change of its own.
  Values with more than one decimal are not accepted rather than rounded, so that what is set is what was typed.
  Decided by the specifier and open to the human: the point as the only decimal sign, Enter and the loss of focus as
  what sets the value, and the error mark for a value out of range. Rejected: a slider, which gives no exact value
  and, set while it is dragged, makes a change at every step it passes.
- **A change during a reading starts the piece again.** The human chose this. The audio made ahead holds the old
  speed, up to 30 seconds of it, so a change heard only after it would come far too late. Starting the piece that is
  playing again at the new speed is the same rule as the resume of unit 0012, and a listener who changes the speed
  hears at once what the new speed sounds like on words they just heard. For a change during a silence, the rule of
  unit 0012 for a resume in a silence holds for the same reason it holds there. Rejected: going on at the new speed
  from the next piece, which still has to make the audio ahead again and so leaves a gap in the reading; and a change
  that only the next reading hears, which cannot speed up what is being listened to.
- **No limit on the time to the new speed.** The engine has to make the piece again before it is heard, and a piece
  can be up to 400 bytes, which takes several seconds. Requirement 14 has the developer measure it instead, so that
  the human can decide whether it needs a unit of its own. Decided by the specifier and open to the human.
- **From 0.5 to 2.0, in steps of 0.1.** The engine takes any speed, but its voice is made for 1.0, and how it sounds
  far from it is not known. The range is the one most players of spoken audio offer, and steps of 0.1 let a listener
  find a speed without passing it. Requirement 14 checks that every speed is understandable. Decided by the specifier
  and open to the human.
- **The silences follow the speed.** The human took this in from what the spec first left beyond the request. At 2.0
  the words take half the time, and silences of the same length would stand out and slow the reading down; dividing
  them by the speed keeps the rhythm of a document the same at every speed. The two seconds after "code example" stay
  two seconds, because they are not part of the rhythm: unit 0010 made them long enough for the listener to reach for
  the pause by hand, and a hand is no quicker at 2.0. Decided by the specifier and open to the human: that exception.
  Rejected: all silences the same at every speed, which was the spec before the human took this in.
- **One speed for the whole IDE.** One file is read at a time in the whole IDE, and a speed is a matter of the
  listener, not of the project. Rejected: a speed per project or per file, which the listener would have to set again
  wherever they read.
- **The speed comes from the engine, not from the playing.** Playing the audio faster raises the pitch, and keeping
  the pitch while playing faster needs processing of the sound that the plugin does not have. The engine makes the
  words take less time with the same voice. Rejected: playing faster, for the pitch.
- **Every speed is listened to on Linux, three on macOS.** The human decided it. Whether a speed is understandable is
  a matter of the voice, which is the same on both platforms, so one machine is enough for it, and prose of several
  sentences per paragraph is what it is judged on. Whether the engine stays ahead differs between the machines, since
  it is native code, and it is checked on both with the whole of `samples/markdown-features.md`, which holds every
  silence of requirement 10: at 2.0, where the engine has the least time per second of sound; at 0.5, the other end;
  and at 1.5, between them. The changes of the speed during a reading are logic of the plugin and user interface of
  the IntelliJ Platform, so they are checked on Linux only. Rejected: every speed on both machines, which the spec
  first asked for, at a cost to the developer that finds nothing the checks above do not.
- **Written for after units 0015 and 0012.** The tool window must exist for the controls, and requirement 9 needs the
  pause. Building this before unit 0012 would leave requirement 9 without a pause to change the speed in.

## Non-goals

- Choosing the voice. The settings page of this unit is where it can go later.
- A speed per project or per file.
- Keyboard shortcuts for "Slower" and "Faster".
- Making a change during a reading heard sooner than the engine makes the piece again.
- Changing the length of the two seconds after "code example".
- Speeds below 0.5 or above 2.0, and speeds between the steps of 0.1.

## Beyond the request

The first item, silences that follow the speed, was taken in by the human and is requirement 10. The other items keep
their numbers.

2. **Keyboard shortcuts for the speed.** A listener with their hands on the keyboard could change the speed without
   the mouse. It costs two shortcuts that do not clash with those of the IDE. Without it, the speed is one click in the
   tool window, or a value typed in its field.
3. **A wait after a press on a button.** The plugin could wait a moment after a press on "Slower" or "Faster" before
   it has the engine make the piece again, so that presses in a row make it once. It costs a delay before every
   change heard, also a single one, and a test with time in it. Without it, a listener who wants a large step types it
   in the field, and the presses in a row are played as requirement 8 says.
4. **A comma as the decimal sign in the field.** A listener used to a comma could type "1,5". It costs a second form
   to accept and to test. Without it, "1,5" shows the error mark with the range, and the listener types "1.5".
