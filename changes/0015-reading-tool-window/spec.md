# Reading tool window

## Context

Unit 0006 gave the plugin one control, the "Read Aloud" button on the editor tab of a Markdown file. It reads the
file, and while the file is read the same button stops it. Unit 0012, a draft, adds pausing and resuming to that
button, and a second button beside it that stops. The human also wants to make the speech faster and slower, which
would be two more buttons, and maybe to choose the voice later. The tab row has little room, which unit 0006 already
gave as its reason for one button only.

A reading also does not belong to one tab. One file is read at a time in the whole IDE, and the reading goes on when
another tab or tool window is selected. A control on the tab of the file that is read is out of sight exactly when
the user has moved to another file and wants to pause or stop.

The IntelliJ Platform lets a plugin add a tool window: a pane of its own, like Terminal or Git, that the user opens
from View › Tool Windows and that can be docked on a side of the IDE window, floated over it, or moved into a window
of its own. Asked where the controls of a reading should go, the human chose a tool window, with the tab button kept
to start a reading. The human also chose to make it a unit of its own, built before the speed: this unit makes the
place, unit 0012 and a unit for the speed fill it.

## Requirements

1. Every open project has a tool window named "AloudRite", which can be opened from View › Tool Windows. Its icon is
   the loudspeaker of the "Read Aloud" button. When a project is opened for the first time with the plugin, the tool
   window is docked on the right side of the IDE window and is closed.
2. The tool window does not open by itself, also not when a reading starts. Once the user has docked, floated or
   moved it, the IDE keeps it where the user put it, as for any other tool window.
3. While nothing is read, the tool window says that nothing is being read, and that a reading is started with "Read
   Aloud" on the tab of a Markdown file.
4. While a file is read, the tool window shows the name of the file and a "Stop" button. When the file belongs to
   another project than the one of the tool window, it shows the name of that project too. Pressing "Stop" ends the
   reading as the stop of unit 0006 does: the sound stops within one second and the tab button shows the loudspeaker
   and "Read Aloud" again.
5. The tool window of every open project shows the same reading, and its "Stop" works in each of them.
6. Within one second of a reading starting or ending, by any of the ends of unit 0006 — the end of the file, a stop
   on the tab or in the tool window, a press on another file, the closing of the file's tab or project, or a failure
   — the tool window shows the new state.
7. The tab button works as it does in unit 0006, including its stop.
8. Automated tests cover requirements 3 to 6 with the stand-ins of unit 0006 for the engine and the player: what the
   tool window shows while nothing is read, while a file of its own project is read and while a file of another
   project is read, that "Stop" ends the reading, and that the tool window follows a start, an end of the file, a stop
   on the tab and a failure. They need no model, no audio device and no network.
9. The developer has opened the tool window, docked it on another side, floated it and moved it into a window of its
   own, and stopped a reading from it while another file was selected, on Linux x64 and on macOS on Apple Silicon,
   and the record holds what they found.
10. `README.md` describes the tool window and what it shows.

## Decisions

- **A tool window holds the controls of a reading.** The human chose this. It is visible whichever file is selected,
  so it fits a reading that belongs to the IDE and not to a tab, and it has room for the controls of unit 0012, the
  speed, and what may come later, such as the voice. The user can put it where it suits them, including in a window
  of its own. Rejected: more buttons on the tab, for the room; a widget on the status bar with a popup, which takes
  almost no room but makes repeated presses, such as "faster" three times, a click to open the popup each time; and a
  banner at the top of the file that is read, which is out of sight once another file is selected.
- **The tab button stays.** Starting a reading belongs to the file, and the button is where unit 0006 put it, so a
  user who never opens the tool window loses nothing.
- **It does not open by itself.** A reading is started from the editor, and a pane that opens unasked takes room from
  it and moves the text the user is looking at. Decided by the specifier and open to the human: the alternative is to
  open it at the first reading of each project, which shows where the controls are once.
- **Docked on the right.** The tool window holds a few lines and buttons, which suit a narrow, tall pane better than
  the wide one at the bottom, where Run and Terminal are. Decided by the specifier and open to the human.
- **Only a stop in this unit.** The tool window needs a control of its own to have a purpose before unit 0012 and the
  speed are built, and a stop is what unit 0006 already has, for the case this unit is about: a reading of a file that
  is not in view. Pausing, resuming and the speed are for their own units.
- **Every project shows the reading.** One file is read in the whole IDE, so a project that is not the one of the file
  still has a reading to stop, and its tool window would be wrong to say that nothing is read. The name of the
  project tells the user where the file is.

## Non-goals

- Pausing and resuming, which unit 0012 adds, and the speed, which unit 0016 adds.
- Choosing the voice, and a settings page.
- Showing in the tool window the text that is read, or how far the reading has come.
- Opening the tab of the file that is read from the tool window.
- Starting a reading from the tool window, of the selected file or of another.
- A keyboard shortcut for the tool window or its buttons, beyond what the IDE gives any tool window.
- Showing a press that waits for the download of the model as a reading. Until the model is in place, the tool window
  shows that nothing is read, and the download shows its progress as unit 0005 made it.

## Beyond the request

- **Opening the tab of the file from its name.** A click on the name of the file in the tool window could bring its
  tab to the front, and open it in its project. It costs a link and a test. Without it, the user finds the tab as they
  would any other.
- **Opening the tool window at the first reading.** It would show the user once where the controls are. It costs
  remembering per project that it was shown. Without it, the user finds the tool window under View › Tool Windows, as
  `README.md` says.
