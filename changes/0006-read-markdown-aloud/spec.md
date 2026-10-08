# Read the Markdown file aloud

## Context

Unit 0003 put a "Read Aloud" button on the editor tab row of a Markdown file. Pressing it showed the content of
the file in a dialog, which was a placeholder for the reading. Unit 0004 makes that button speak one fixed
sentence in place of the dialog, whatever the file holds. This unit joins the two: the button reads the file
aloud, and the fixed sentence goes.

It needs units 0003 and 0004 to be implemented. It does not need unit 0005: without it the model is put in place
by hand, as in unit 0004.

This spec was written together with the specs of units 0004 and 0005, on 2026-10-08, before unit 0004 was built.
Nothing was known then about how fast the engine is or how it behaves on long text, and several requirements
below depend on exactly that. Units 0004 and 0005 each end by revising this spec against what they found, so what
is written here is the intent and may change in its details.

## Requirements

1. Pressing the "Read Aloud" button on a Markdown file reads the content of that file aloud, from its beginning to
   its end, as the editor holds it at the moment of pressing, including changes that are not saved yet.
2. No dialog shows the content of the file: the dialog of unit 0003, which unit 0004 removes, does not return.
3. Reading starts without the whole file having been turned into audio first: for a file of 10,000 words the first
   sound comes no more than 2 seconds later than it does for a file of one sentence.
4. The IDE stays usable while a file is read: typing in an editor and opening menus are not held up.
5. While a file is read, the button on the tab of that file shows a stop icon and the tooltip "Stop Reading".
   Pressing it stops the sound within one second, and the button shows the loudspeaker and "Read Aloud" again.
6. When the end of the file is reached, the sound ends and the button shows the loudspeaker and "Read Aloud"
   again.
7. One file is read at a time. Pressing "Read Aloud" on another Markdown file while one is being read stops the
   first and starts the second.
8. Reading goes on when the user selects another tab or another tool window. It stops when the tab of the file
   that is read is closed, when its project is closed, and when the IDE exits.
9. Changes made to the file while it is read do not change what is read: it stays the text as it was when the
   button was pressed.
10. The text is spoken as it is in the file. This unit takes nothing out of it and puts nothing in.
11. Pressing the button on a file that is empty, or holds only white space, plays nothing and shows a
    notification that there is nothing to read.
12. When the model is not in place, or the platform is not one that unit 0004 supports, pressing the button does
    what unit 0004 specifies for the button in that case, or what unit 0005 specifies where that unit is
    implemented.
13. The fixed test sentence of unit 0004 is gone: the plugin speaks nothing but the content of the file the
    button was pressed on.
14. Automated tests cover requirements 5, 6, 7, 9, 11 and 13 with a stand-in for the engine, and need no model
    and no audio device. They include a file with a change that is not saved.
15. The developer has listened to a Markdown file of several pages being read from beginning to end, and has
    stopped a reading halfway, on Linux x64 and on macOS on Apple Silicon. These are the human's checks, made at
    the review.
16. `README.md` describes what the button does after this unit, and no longer describes the test sentence.

## Decisions

- **The button of unit 0003 is the one that reads.** It was placed and named for this. Its dialog and the test
  sentence of unit 0004 were both placeholders for the reading.
- **The text is read raw.** Deciding what in a Markdown file is speakable, and what to say for a code block or a
  link, is a unit of its own. Until then the engine gets the markers as well and does with them what it does,
  which may be to pronounce some of them. That is accepted here so that this unit answers one question: does
  pressing the button read the file.
- **Reading starts before the whole file is turned into audio.** A file of several pages would otherwise keep the
  user waiting in silence for as long as the engine needs for all of it. How the text is cut into pieces is for
  the plan. The 2 seconds of requirement 3 are a guess made before unit 0004 measured anything, and are the first
  thing to revise.
- **The same button stops the reading.** The user looks for the way to stop where they started. A separate stop
  button was rejected because it takes a second place in a tab row that has little room.
- **No pausing.** Stopping and starting again from the beginning is enough to make the unit usable, and pausing
  brings the question of where to resume after the text has changed. See "Beyond the request".
- **One reading at a time.** Two voices at once are of no use to anyone. Starting a second reading stops the
  first, and does not refuse, because the user has just said what they want to hear.
- **Reading goes on when the user looks elsewhere.** Listening to a document while looking at code is what the
  plugin is for. It stops when the file's tab is closed because the stop button goes with the tab, and a voice
  that cannot be stopped is worse than one that stops too early.
- **What is read is fixed at the moment of pressing.** Following changes while reading would need a way to map a
  position in the audio back to a text that has moved, which is the problem the highlighting of a later unit has
  to solve. Until then the user hears what was there when they pressed.
- **An empty file gives a notification and not silence.** Unit 0003 decided that a button that does nothing when
  pressed looks broken. With the dialog that showed the emptiness gone, a notification takes its place.
- **The tests use a stand-in for the engine.** What this unit adds is the behavior around the engine: starting,
  stopping, switching and ending. Unit 0004 tests that the engine makes audio. Running the real engine in these
  tests would make them slow and dependent on a model that is not in the repository.

## Non-goals

- Cleaning the Markdown: skipping markers, code blocks, links or images, or saying something in their place.
- Highlighting the text that is being read, and scrolling the editor along.
- Pausing and resuming.
- Reading only the selected text, or from the caret.
- Choosing a voice or a speed, and a settings page for them.
- A keyboard shortcut or a menu entry for the button.
- Reading files that are not Markdown.

## Beyond the request

- **Pausing and resuming.** A listener who is interrupted has to stop and later start from the beginning of the
  file. Pausing costs a third state of the button and a decision on what resuming means after the file changed.
  Without it, a long document is awkward to listen to in more than one sitting.
- **Reading from the caret or the selection.** It lets the listener start where they are and not at the top. It
  costs a decision on which of the two wins and how the button says which it will do. Without it, every reading
  starts at the beginning of the file.
- **A limit on the length of a file.** A file of many megabytes would be read for hours. A limit costs a choice of
  the size and a message. Without it, the reading simply goes on until it is stopped, which the stop button does
  within a second.
