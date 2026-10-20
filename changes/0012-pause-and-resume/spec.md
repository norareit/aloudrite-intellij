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

Unit 0010 puts two seconds of silence after the words "code example" in place of a code block, long enough for the
listener to pause the reading by hand. This unit gives them the way to do that.

Unit 0015, a draft, adds the "AloudRite" tool window, which shows the file that is read and has a "Stop" button. The
human chose it as the place for the controls of a reading, so that the speed and later controls do not each need a
button on the tab. Stopping is the exception: the human wants it one click away on the tab as soon as a reading
starts, as pausing is. This unit is written to be built after unit 0015.

## Requirements

1. While a file is read, the button on the tab of that file shows a pause icon and the tooltip "Pause Reading".
   Pressing it silences the sound within one second, and the reading is then paused.
2. While a reading is paused, the button shows a play icon and the tooltip "Resume Reading". Pressing it continues
   the reading from the beginning of the piece that was being played when it was paused, as the plugin cut the text,
   and the button shows the pause icon and "Pause Reading" again.
3. The first sound after a resume comes within one second of the press, on the developer's two machines.
4. From the moment a file starts to be read, and for as long as it is read or its reading is paused, a second button
   shows on the tab of that file, beside the first, with the stop icon of unit 0006 and the tooltip "Stop Reading".
   Pressing it ends the reading: the sound stops within one second where it is playing, the second button goes, and
   the first shows the loudspeaker and "Read Aloud" again. At any other time the second button is not shown, on any
   tab.
5. The tool window of unit 0015 shows, while a file is read, a "Pause" button beside its "Stop", and, while the
   reading is paused, that it is paused and a "Resume" button in place of "Pause". "Pause" and "Resume" do what the
   tab button does in requirements 1 and 2. "Stop" ends a paused reading as it ends one that plays, as the stop button
   on the tab does in requirement 4.
6. A resumed reading reads the text it was given at the press, even when the file was changed while it was paused.
7. A paused reading stays paused for as long as the IDE runs, until it is resumed or ends. It ends, as a reading does
   in unit 0006, when the tab of its file is closed, when its project is closed and when the IDE exits, and also when
   "Read Aloud" is pressed on another Markdown file, which then is read.
8. A reading can be paused and resumed any number of times, and the end of the file is reached as in unit 0006: the
   sound ends, the second button goes, the first shows the loudspeaker and "Read Aloud" again, and the tool window
   shows that nothing is read.
9. A pause and a resume change nothing else of what unit 0006 established: one file is read at a time, the reading
   goes on when another tab or tool window is selected, and a failure ends the reading, shows its message and puts
   the tab buttons and the tool window back as they are when nothing is read.
10. Automated tests cover requirements 1, 2 and 4 to 8 with the stand-ins of unit 0006 for the engine and the
    player: that a pause plays no further audio, that a resume starts again at the beginning of the piece that was
    playing, the state and tooltip of both tab buttons and what the tool window shows in each state, that the stop
    button on the tab shows from the start of a reading, and the end of a reading that plays and of one that is paused
    by the stop button on the tab and by "Stop" in the tool window, and of a paused reading by closing the tab, by
    closing the project and by a press on another file. They need no model, no audio device and no network.
11. The developer has paused and resumed a reading of `samples/markdown-features.md` several times, in the middle of a
    sentence and between two, and stopped a reading that plays and one that is paused from the tab, on Linux x64 and
    on macOS on Apple Silicon. The record holds what they found for requirements 1 to 5, and whether the sentence
    heard again after a resume was the one expected.
12. `README.md` describes the pause, the resume, the stop button on the tab and the controls in the tool window.

## Decisions

- **The tab button pauses, and a second tab button stops, from the start of a reading.** The human chose this. A
  listener who is interrupted may want to pause or to stop, and either should be one click on the tab they started
  from. Pausing takes the button they already press. The stop takes a second button, which is there from the moment
  the reading starts, because a stop that shows only once the reading is paused makes stopping two clicks, and a stop
  that is only in the tool window, which is closed until the user opens it, makes it more. The tool window of unit
  0015 has room for the controls of a reading, and the reason against a second tab button, that the speed and later
  controls would each want one too, still holds for them: they go into the tool window. Stopping is the exception
  because it is what unit 0006 already offered on the tab, and the button takes room only while there is a reading.
  The tool window also pauses, resumes and stops, so a reading of a file that is not in view can be controlled there.
  Rejected: the stop only in the tool window, which this spec had before; keeping stop on the tab button and pausing
  only in the tool window, which makes the more common wish of an interrupted listener the harder one; keeping stop
  on the tab button and offering to resume at the next press, which makes every press after a stop a choice.
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
- The speed of the reading, which is unit 0016.

## Beyond the request

- **Pausing on a keyboard shortcut.** A listener with their hands on the keyboard could pause without the mouse. It
  costs a shortcut that does not clash with those of the IDE, and the question of which reading it pauses when the
  focus is on another file. Without it, the pause is one click on the tab of the file that is read.
- **Resuming a little before the piece.** Starting again one piece earlier would give the listener the sentence before
  as context. It costs keeping the audio of one more piece. Without it, the listener hears the sentence they were
  interrupted in, which is what the human chose.
