# Pause and resume

## Context

Unit 0006 made the "Read Aloud" button on the editor tab of a Markdown file read the file aloud. While it reads, the
same button shows a stop icon and "Stop Reading", and a press stops the reading. A listener who is interrupted can
only stop, and start again later from the beginning of the file. Unit 0006 left pausing out on purpose and named it
under "Beyond the request": it costs a third state of the button and a decision on what resuming means. The human
asked for it as a unit of its own once unit 0006 was implemented.

What unit 0006 established, and this unit builds on:

- The text that is read is fixed at the moment of the press. A change made to the file afterwards does not reach the
  reading.
- The plugin cuts the text into pieces before it hands them to the engine: a piece ends after a sentence, after a
  blank line, after a comma, colon or semicolon once the piece is long enough, and at 400 bytes at the most. The
  engine works ahead of the playing, by up to 30 seconds of audio.
- A stop silences the sound within one second.
- One file is read at a time. A press on another file stops the first reading and starts the second.
- A reading stops when the tab of its file is closed, when its project is closed and when the IDE exits.

Unit 0010, which is a draft, puts two seconds of silence after the words "code example" in place of a code block, long
enough for the listener to pause the reading by hand. This unit gives them the way to do that.

## Requirements

1. While a file is read, the button on the tab of that file shows a pause icon and the tooltip "Pause Reading".
   Pressing it silences the sound within one second, and the reading is then paused.
2. While a reading is paused, the button shows a play icon and the tooltip "Resume Reading". Pressing it continues
   the reading from the beginning of the piece that was being played when it was paused, as the plugin cut the text,
   and the button shows the pause icon and "Pause Reading" again.
3. The first sound after a resume comes within one second of the press, on the developer's two machines.
4. While a file is read and while its reading is paused, a second button shows on the tab of that file, beside the
   first, with the stop icon of unit 0006 and the tooltip "Stop Reading". Pressing it ends the reading: the sound
   stops within one second where it is playing, the second button goes, and the first shows the loudspeaker and
   "Read Aloud" again. At any other time the second button is not shown, on any tab.
5. A resumed reading reads the text it was given at the press, even when the file was changed while it was paused.
6. A paused reading stays paused for as long as the IDE runs, until it is resumed or ends. It ends, as a reading does
   in unit 0006, when the tab of its file is closed, when its project is closed and when the IDE exits, and also when
   "Read Aloud" is pressed on another Markdown file, which then is read.
7. A reading can be paused and resumed any number of times, and the end of the file is reached as in unit 0006: the
   sound ends, the second button goes, and the first shows the loudspeaker and "Read Aloud" again.
8. A pause and a resume change nothing else of what unit 0006 established: one file is read at a time, the reading
   goes on when another tab or tool window is selected, and a failure ends the reading, shows its message and puts
   the buttons back.
9. Automated tests cover requirements 1, 2, 4, 5, 6 and 7 with the stand-ins of unit 0006 for the engine and the
   player: that a pause plays no further audio, that a resume starts again at the beginning of the piece that was
   playing, the state and tooltip of both buttons in each state, and the end of a paused reading by the stop button,
   by closing the tab, by closing the project and by a press on another file. They need no model, no audio device and
   no network.
10. The developer has paused and resumed a reading of `samples/markdown-features.md` several times, in the middle of a
    sentence and between two, and stopped a paused reading, on Linux x64 and on macOS on Apple Silicon. The record
    holds what they found for requirements 1 to 4, and whether the sentence heard again after a resume was the one
    expected.
11. `README.md` describes the pause, the resume and the stop button.

## Decisions

- **The button pauses, and a second button stops.** The human chose this. Pausing is what a listener who is
  interrupted reaches for, so it takes the button they already press. Unit 0006 rejected a second button because the
  tab row has little room. This one takes room only while there is a reading to stop, and stands beside the button
  that started it, which keeps the reason unit 0006 gave for stopping with the same button: the user looks for the
  way to stop where they started. Rejected: keeping stop on the button and offering to resume at the next press,
  which makes every press after a stop a choice, and a button that only pauses, which leaves no way to end a reading
  except by closing its tab.
- **A resume starts at the beginning of the piece that was playing.** The human chose this. A listener who was
  interrupted in the middle of a sentence has lost its beginning, and hearing it whole again costs a few seconds at
  the most. The piece is the unit the plugin already makes audio in, so it is the sentence, or the part of a long
  sentence that the plugin cut at a comma, colon or semicolon. Rejected: continuing exactly where the sound stopped,
  which resumes in the middle of a word.
- **A resume is quick.** The engine needs up to several seconds for a long piece, and a listener who presses
  "Resume" expects to hear it at once, as they did when they pressed "Pause". So requirement 3 asks for the first
  sound within one second, which means that a resume does not wait for the engine to make the piece again. How is for
  the plan.
- **What is read stays fixed at the press.** Unit 0006 fixed the text at the press so that it never has to map a
  position in the audio back to a text that has moved. A resume keeps that rule, so the question of where to resume
  after the file changed does not come up. Rejected: resuming in the changed file, which needs that mapping.
- **A pause has no time limit.** A listener can come back after lunch. Holding a paused reading costs the audio that
  was made ahead of it, at most about 3 MB, and the engine, which stays loaded as it does between readings anyway.
  Decided by the specifier and open to the human.
- **Another file ends a paused reading.** One file is read at a time, as unit 0006 decided, and a press on another
  file says what the user wants to hear now. Rejected: keeping the paused reading of the first file aside to resume
  later, which needs more than one reading to exist.

## Non-goals

- Resuming a reading after its tab, its project or the IDE was closed.
- A keyboard shortcut or a menu entry for pausing, resuming or stopping.
- Skipping forward or back by a sentence or a paragraph.
- Pausing by itself, such as when the audio output changes or the screen locks.
- Showing in the editor where the reading is, or where it will resume.
- Reading from the caret or the selection, which the human wants as a unit of its own, unit 0013.

## Beyond the request

- **Pausing on a keyboard shortcut.** A listener with their hands on the keyboard could pause without the mouse. It
  costs a shortcut that does not clash with those of the IDE, and the question of which reading it pauses when the
  focus is on another file. Without it, the pause is one click on the tab of the file that is read.
- **Resuming a little before the piece.** Starting again one piece earlier would give the listener the sentence before
  as context. It costs keeping the audio of one more piece. Without it, the listener hears the sentence they were
  interrupted in, which is what the human chose.
