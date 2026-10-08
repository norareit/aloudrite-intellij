# Read the Markdown file aloud

## Context

Unit 0003 put a "Read Aloud" button on the editor tab row of a Markdown file. Pressing it showed the content of
the file in a dialog, which was a placeholder for the reading. Unit 0004 makes that button speak one fixed
sentence in place of the dialog, whatever the file holds. This unit joins the two: the button reads the file
aloud, and the fixed sentence goes.

It needs units 0003 and 0004 to be implemented. It does not need unit 0005: without it the model is put in place
by hand, as in unit 0004.

This spec was first written on 2026-10-08, together with the specs of units 0004 and 0005 and before unit 0004 was
built. It was revised on 2026-10-09 against what unit 0004 established, which is this:

- The engine works inside the IDE on Linux x64 and on macOS on Apple Silicon, and the developer judged the test
  sentence on both: "It sounds great. Very clear. No stutter or anything. Sounds VERY natural."
- With the model loaded, the first sound of the test sentence came 640 and 654 ms after the press on the
  developer's Linux desktop, and 927 ms on their MacBook. The first press after the IDE started, which loads the
  model, took 1346 ms and 2082 ms.
- The engine hands over its audio in pieces while it is still working, and the plugin plays each piece as it
  comes.
- A press while the sentence is being spoken is ignored. Nothing can be stopped.
- When the engine or the audio output fails, the plugin shows a message and the next press works again.
- The engine was given one English sentence without any markup. How it speaks Markdown markers, a text of several
  pages, or a language other than English has not been tried. The lexicon the plugin gives it is the one for
  American English.

Unit 0005 ends by revising this spec once more, so what is written here may still change in its details.

## Requirements

1. Pressing the "Read Aloud" button on a Markdown file reads the content of that file aloud, from its beginning to
   its end, as the editor holds it at the moment of pressing, including changes that are not saved yet.
2. No dialog shows the content of the file: the dialog of unit 0003, which unit 0004 removes, does not return.
3. Reading starts without the whole file having been turned into audio first: for a file of 10,000 words the first
   sound comes no more than 2 seconds later than it does for a file of one sentence.
4. Reading does not fall silent in the middle of a sentence because the engine has not finished the audio that
   follows: on the developer's two machines, a file of several pages is read without such a pause.
5. The IDE stays usable while a file is read: typing in an editor and opening menus are not held up.
6. While a file is read, the button on the tab of that file shows a stop icon and the tooltip "Stop Reading".
   Pressing it stops the sound within one second, and the button shows the loudspeaker and "Read Aloud" again.
7. When the end of the file is reached, the sound ends and the button shows the loudspeaker and "Read Aloud"
   again.
8. One file is read at a time. Pressing "Read Aloud" on another Markdown file while one is being read stops the
   first and starts the second.
9. Reading goes on when the user selects another tab or another tool window. It stops when the tab of the file
   that is read is closed, when its project is closed, and when the IDE exits.
10. Changes made to the file while it is read do not change what is read: it stays the text as it was when the
    button was pressed.
11. The text is spoken as it is in the file. This unit takes nothing out of it and puts nothing in.
12. Pressing the button on a file that is empty, or holds only white space, plays nothing and shows a
    notification that there is nothing to read.
13. When the engine or the audio output fails during a reading, the reading ends, the button shows the
    loudspeaker and "Read Aloud" again, a message says that the reading failed, and the next press works.
14. When the model is not in place, or the platform is not one that unit 0004 supports, pressing the button does
    what unit 0004 specifies for the button in that case, or what unit 0005 specifies where that unit is
    implemented.
15. The fixed test sentence of unit 0004 is gone: the plugin speaks nothing but the content of the file the
    button was pressed on.
16. Automated tests cover requirements 6, 7, 8, 10, 12, 13 and 15, and that a reading stops when the tab of its
    file is closed, with a stand-in for the engine. They need no model and no audio device, and they include a
    file with a change that is not saved.
17. The developer has listened to a Markdown file of several pages being read from beginning to end, and has
    stopped a reading halfway, on Linux x64 and on macOS on Apple Silicon. The record holds what they found for
    requirement 4 and how the Markdown markers sounded. These are the human's checks, made at the review.
18. `README.md` describes what the button does after this unit, and no longer describes the test sentence.

## Decisions

- **The button of unit 0003 is the one that reads.** It was placed and named for this. Its dialog and the test
  sentence of unit 0004 were both placeholders for the reading.
- **The text is read raw.** Deciding what in a Markdown file is speakable, and what to say for a code block or a
  link, is a unit of its own. Until then the engine gets the markers as well and does with them what it does,
  which may be to pronounce some of them. That is accepted here so that this unit answers one question: does
  pressing the button read the file. What it sounds like has not been tried, and requirement 17 has the
  developer write it down, because it is what the unit that cleans the Markdown starts from.
- **Reading starts before the whole file is turned into audio.** A file of several pages would otherwise keep the
  user waiting in silence for as long as the engine needs for all of it. Unit 0004 showed that the engine hands
  over audio in pieces and that the first sound of one sentence comes in under a second with the model loaded.
  The 2 seconds of requirement 3 leave room above that. Whether the engine takes a text of 10,000 words in one go,
  or the plugin has to cut it into pieces itself, is not known and is for the plan.
- **No silence in the middle.** Starting early only helps if the engine then stays ahead of the playing. The
  times of unit 0004 suggest that it does on the developer's machines, with the Mac the slower of the two, but
  nothing longer than one sentence was measured. Requirement 4 is stated for those two machines because no
  slower one is at hand to check a wider promise against.
- **The same button stops the reading.** The user looks for the way to stop where they started. A separate stop
  button was rejected because it takes a second place in a tab row that has little room. In unit 0004 a press
  while the sentence is spoken is ignored, which was right for a sentence of four seconds and is not for a file
  of several pages.
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
- **A failure ends the reading and leaves the button usable.** Unit 0004 already reports a failure and lets the
  next press work. With a button that changes while it reads, the failure must also put the button back, or it
  would show "Stop Reading" for a reading that is no longer there.
- **The tests use a stand-in for the engine.** What this unit adds is the behavior around the engine: starting,
  stopping, switching and ending. Unit 0004 tests that the engine makes audio, and it built the plugin so that
  the engine can be replaced in a test. Running the real engine in these tests would make them slow and dependent
  on a model that is not in the repository.

## Non-goals

- Cleaning the Markdown: skipping markers, code blocks, links or images, or saying something in their place.
- Highlighting the text that is being read, and scrolling the editor along.
- Pausing and resuming.
- Reading only the selected text, or from the caret.
- Choosing a voice or a speed, and a settings page for them.
- Reading text in a language other than English as that language. Such text is read with the English voice and
  the American English lexicon, however that comes out.
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
- **Loading the model before the first press.** The first press after the IDE started took about 0.7 to 1.2
  seconds longer than later ones, because it loads the model. Loading it when a Markdown file is opened would
  hide that. It costs the memory of the loaded model and the loading time for every user who opens a Markdown
  file and never presses the button. Without it, the first reading of a session starts a second later than the
  rest.
