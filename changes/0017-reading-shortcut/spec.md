# Reading shortcuts

## Context

Unit 0012 made the "Read Aloud" button on the editor tab of a Markdown file pause and resume a reading, with a second
button beside it that stops. Unit 0015 gave the "AloudRite" tool window a pause, a resume and a stop, and unit 0016 a
"Slower" and a "Faster" button for the speed of the reading. Each of them takes the mouse. Unit 0012 named pausing on
a keyboard shortcut under "Beyond the request", and unit 0016 named shortcuts for the speed there as well: a listener
with their hands on the keyboard could control the reading without the mouse, at the cost of shortcuts that do not
clash with those of the IDE, and of a decision on which reading they act on when the focus is on another file. The
human asked for them as a unit of their own, with four shortcuts: one that does what the tab button does, starting,
pausing and resuming; one that stops; one that makes the reading slower; and one that makes it faster. The human
also asked that the tooltips of the buttons name their shortcut.

What units 0006, 0012, 0015 and 0016 established, and this unit builds on:

- One file is read at a time, in the whole IDE. A reading goes on when another tab or tool window is selected.
- A press on "Read Aloud" reads the Markdown file of its tab. While the file is read, the button pauses the reading,
  and while the reading is paused, it resumes it. A resume starts at the beginning of the piece that was playing.
- Pressing "Read Aloud" on another Markdown file ends the reading of the first and reads the second.
- The first press downloads the speech model when it is not there yet, after asking, as unit 0005 made it.
- The button "Stop Reading" is on the tab of the file that is read, also while its reading is paused, and stops it.
  The tool window has a "Pause" button, which shows "Resume" while the reading is paused, and a "Stop" button.
- The speed is one for the whole IDE, set in steps from 0.75 to 2.0 with "Slower" and "Faster" in the tool window,
  whether a file is read or not. Presses in a row, each within half a second of the one before, are one change. A
  change during a reading starts the piece that is playing again at the new speed.

The IDE lets a plugin give an action a default shortcut in each of its keymaps. The user can change it, or remove
it, in Settings › Keymap, and finds the action by its name in Find Action as well. A toolbar button that is an action
shows the shortcut of that action in its tooltip, in the IDE's own style.

The keys the human first proposed, Ctrl+Alt with A, S, Q and E, are all taken in the IDE's default keymap on Linux:
Ctrl+Alt+A adds a file to version control, Ctrl+Alt+S opens the settings, Ctrl+Alt+Q toggles the rendered view of
documentation comments, and Ctrl+Alt+E browses the history of a console and toggles a search in the selection.
Ctrl+Alt+Shift with A, S and E is taken as well, and so is Ctrl+Alt+Shift+Q, which the bundled plugin Database Tools
gives to "Query File…" in the default keymaps on Linux. With Ctrl+Alt+Shift, every letter but O and Z is taken there.

## Requirements

1. The plugin has four actions, named "Read Aloud, Pause or Resume", "Stop Reading", "Read Slower" and "Read Faster",
   which are listed in Settings › Keymap and found by Find Action. Each of these names appears once among the actions
   of the plugin in Settings › Keymap.
2. While no file is read, "Read Aloud, Pause or Resume" reads the Markdown file of the selected editor in the IDE
   window that has the focus, as a press on its "Read Aloud" button does, including the question about the download
   when the model is not there yet. When the selected editor is not one of a Markdown file, or no editor is open, the
   action does nothing.
3. While a file is read, "Read Aloud, Pause or Resume" pauses that reading, as a press on "Pause Reading" does,
   whichever editor, tool window or project window has the focus, including the editor of another Markdown file.
4. While a reading is paused, "Read Aloud, Pause or Resume" resumes it, as a press on "Resume Reading" does, wherever
   the focus is as in requirement 3.
5. While a file is read, also while its reading is paused, "Stop Reading" stops that reading, as a press on the
   "Stop Reading" button of its tab does, wherever the focus is as in requirement 3. While no file is read, it does
   nothing.
6. "Read Slower" and "Read Faster" change the speed as a press on "Slower" and on "Faster" in the tool window does,
   wherever the focus is as in requirement 3, whether a file is read or not, and whether the tool window is open or
   not. This includes the half second of unit 0016 within which presses in a row are one change, also for presses of
   a shortcut and of a button mixed. At 0.75 "Read Slower" does nothing, and at 2.0 "Read Faster" does nothing.
7. After each use of one of the actions, the buttons on the tab of the file, the tool window of unit 0015 and the
   settings page of unit 0016 show the state of the reading and the speed as they do after a press on the
   corresponding button.
8. In the IDE's default keymaps on Linux, "Default for XWin", "Default for GNOME" and "Default for KDE", the actions
   have these shortcuts, and in its default keymap on macOS, "macOS", the shortcuts with the same keys, ⌃ for Ctrl and
   ⌥ for Alt:

   | Action                      | Linux                 | macOS   |
   |-----------------------------|-----------------------|---------|
   | Read Aloud, Pause or Resume | Ctrl+Alt+Shift+Space  | ⌃⌥⇧Space |
   | Stop Reading                | Ctrl+Alt+Shift+O      | ⌃⌥⇧O    |
   | Read Slower                 | Ctrl+Alt+Shift+Comma  | ⌃⌥⇧,    |
   | Read Faster                 | Ctrl+Alt+Shift+Period | ⌃⌥⇧.    |

9. In the keymaps of requirement 8, as IntelliJ IDEA 2026.2.3 ships them with the plugins it bundles, no other action
   has one of the shortcuts of requirement 8.
10. A shortcut the user gives one of the actions in Settings › Keymap does what requirements 2 to 6 say for that
    action, and a shortcut the user removes does nothing any more.
11. The tooltip of each button that does what one of the actions does names the shortcut of that action, in the
    IDE's style of a tooltip with a shortcut, and only there:
    - On the editor tab: "Read Aloud", "Pause Reading" and "Resume Reading" name the shortcut of "Read Aloud, Pause or
      Resume", and "Stop Reading" names that of "Stop Reading". On the tab of a Markdown file while another file is
      read, "Read Aloud" names no shortcut, because there the button reads its own file and the shortcut pauses the
      other.
    - In the tool window: "Pause" and "Resume" have the tooltips "Pause Reading" and "Resume Reading" and name the
      shortcut of "Read Aloud, Pause or Resume", "Stop" has the tooltip "Stop Reading" and names that of "Stop
      Reading", and "Slower" and "Faster" name those of "Read Slower" and "Read Faster".
12. The tooltips of requirement 11 name the shortcut the action has in the keymap in use, also one the user gave it in
    Settings › Keymap, and the first of them when the action has several. A tooltip of an action without a shortcut is
    its text alone, as it was before this unit.
13. Automated tests cover requirements 1 to 7, 9, 11 and 12 with the stand-ins of unit 0006 for the engine and the
    player: that "Read Aloud, Pause or Resume" starts a reading of the selected Markdown file, does nothing when the
    selected file is not Markdown, and pauses and resumes a reading with the focus on the file that is read, on another
    Markdown file and in another project; that "Stop Reading" stops a reading and a paused reading from there, and does
    nothing without one; that "Read Slower" and "Read Faster" change the speed within its limits, with and without a
    reading, and that presses of a shortcut and of a button within the half second are one change; that the buttons
    follow; that no other action has the shortcuts; and the tooltips of requirement 11, with the default shortcuts,
    with a shortcut the user gave, and without one. They need no model, no audio device and no network.
14. On Linux x64, the developer has looked at the tooltips of requirement 11 in the sandbox IDE. On Linux x64 and on
    macOS on Apple Silicon, the developer has used each of the four shortcuts, with the focus on the file that is read,
    on another file, and in a tool window, and has changed the speed with them with no file read. The record holds what
    they found, and for each shortcut and machine whether the desktop or another application took it before the IDE
    did.
15. `README.md` describes the four shortcuts, what each does wherever the focus is, and where to change them.

## Decisions

- **Four shortcuts: read, pause and resume on one, stop, slower and faster.** The human chose this, after the spec first
  had one shortcut only. A listener who controls the reading from the keyboard needs all of it there, and having to
  reach for the mouse to stop or to change the speed undoes the point of the first shortcut. The first shortcut does
  what the tab button does, so the user learns one key for listening and pausing. Rejected: a stop that is only the
  pause held longer, which no player does and which the user cannot see.
- **The shortcuts act on the reading wherever the focus is.** The human chose this for the first shortcut, and it
  holds for all four. One file is read in the whole IDE, so there is never a doubt which reading a key means, and a
  listener who has moved on to another file is the one who most needs it without finding the tab first. Rejected:
  shortcuts that work only in the editor of the file that is read, which fail exactly when the user has moved on.
- **While a file is read, the first shortcut pauses it, also from another Markdown file.** On the tab of another
  Markdown file, the button starts a reading of that file, because the button belongs to its tab. The shortcut belongs
  to no tab, so it acts on the reading that exists. Starting another file stays a press on its button. Decided by the
  specifier and open to the human.
- **The first shortcut does nothing outside a Markdown file, and "Stop Reading" nothing without a reading.** With
  nothing to read and nothing playing, there is nothing for them to do, and a message for a key pressed by mistake
  would be noise. Decided by the specifier and open to the human: the alternative is a short notification that only
  Markdown files are read.
- **The speed shortcuts work also with no file read.** The buttons in the tool window do, since the speed is one for
  the IDE and a listener may set it before a reading. The shortcuts do what the buttons do, including the half second
  of unit 0016, so that a key held down or pressed several times has the engine make the piece once. Decided by the
  specifier and open to the human.
- **Ctrl+Alt+Shift with Space, O, Comma and Period.** The human proposed Ctrl+Alt with A, S, Q and E, which are all
  taken in the IDE's default keymap on Linux, as the context says, Ctrl+Alt+S by the settings, which a user of the IDE
  reaches for often. With Shift added, A, S and E are taken as well. The keys of requirement 8 were checked against
  the default keymaps and the bundled plugins of IntelliJ IDEA 2026.2.3, and are free in all of them. That check first
  missed that Database Tools takes Ctrl+Alt+Shift+Q, which the spec first had for the stop, and the implementer found
  it in the keymaps the tests load and in every descriptor of the distribution. All four share the modifiers, so the
  user learns one hold and four keys. The space bar is the key that pauses and plays in media players, which is why
  the spec chose it for the first shortcut from the start. O, for off, is the human's choice for the stop, after Q
  turned out to be taken; it is one of the two letters free with Ctrl+Alt+Shift, and the other, Z, is too awkward to
  press with one hand together with the three modifiers. Comma and Period are the keys of "<" and ">", which video
  players such as YouTube use for the playback speed, and the slower one is on the left. Rejected:
  Ctrl+Alt+Shift+Minus and +Equals, as the zoom of the IDE, which are taken; End or Delete for the stop, which a
  laptop keyboard of a Mac lacks, and Ctrl+Alt+Shift+Delete, which logs out on KDE without asking; Alt+Shift with
  letters, which on macOS types a character in the editor; Ctrl+Alt+Shift+Q, which would share the key with "Query
  File…" of Database Tools; and Ctrl+Alt+Shift+Z, as said. Whether the desktop takes a key first is not in the
  keymaps, so requirement 14 asks the developer. The human chose O; the other keys were decided by the specifier and
  are open to the human.
- **On macOS, ⌃⌥⇧ rather than ⌘⌥⇧.** The keymap of the IDE on macOS mostly puts ⌘ where Linux has Ctrl, and the spec
  first had ⌘⌥⇧Space. But ⌘⌥⇧Q, the stop's first key in that form, is the shortcut of macOS that logs the user out at
  once, without asking, and a key derived from Linux that way can land on a shortcut of macOS. ⌃⌥⇧ with the four keys
  is free in the IDE's keymap on macOS, is not a shortcut of macOS itself, and is the same keys as on Linux, so a
  developer who works on both machines presses the same keys on each. Decided by the specifier and open to the human.
- **The actions have names of their own.** The name in Settings › Keymap says what the key does in every state, where
  "Read Aloud" alone would hide the pause, and "Slower" and "Faster" alone would not say what they make slower. Each
  name appears once, so that the user does not find two "Stop Reading" entries and wonder which one has the key.
  Decided by the specifier and open to the human.
- **"Read Slower" and "Read Faster" have the minus and plus icons.** The human asked for this after seeing the actions
  in Settings › Keymap, where "Read Aloud, Pause or Resume" and "Stop Reading" show the icons of their tab buttons and
  the two speed actions showed none. They take the icons of "Slower" and "Faster" in the tool window, the IDE's minus
  and plus, so that an action and its button look alike wherever the action is listed, as in Settings › Keymap and Find
  Action. Decided by the human.
- **The tooltips name the shortcut.** The human took this in from what the spec first left beyond the request. It
  teaches the keys to a user who never opens the keymap, at the place where they reach for the mouse instead. The
  tooltip names the shortcut only where the press does what the shortcut does, because a tooltip that names a key on
  the "Read Aloud" button of another file, where the key would pause a different reading, would teach the wrong thing.
  The text buttons of the tool window had no tooltip, and get one so that they can name their shortcut; the tooltip
  says what the button does in full, as the tab buttons do. Following the keymap in use, and not the default keys,
  keeps a tooltip true for a user who changed them. Decided by the specifier and open to the human: the tooltips of the
  tool window, the full names in them, and the first of several shortcuts.

## Non-goals

- A menu entry for the actions, beyond Find Action.
- Keymaps other than the IDE's default ones on Linux and macOS, such as Emacs or Sublime Text. The actions have no key
  there until the user gives them one.
- Windows, which the plugin does not support.
- The shortcuts where a component of the IDE takes the key for itself before the IDE's keymap does, such as the
  terminal can.
- Shortcuts for the field of the speed or for the settings page.

## Beyond the request

The human took in the first item, the shortcut in the tooltips of the tab buttons, which is now requirements 11 and
12 for every button. The other items keep their numbers.

2. **A shortcut for the selected file even during a reading.** A second key could start the selected Markdown file
   while another is read, as its tab button does. It costs another free key. Without it, the user presses the button
   on the tab of the other file.
3. **The new speed shown after a speed shortcut.** A short message, such as a balloon or a text in the status bar,
   could show the speed after "Read Slower" or "Read Faster", which matters when no file is read and the tool window is
   closed, since nothing is heard then. It costs a message of a kind the plugin has not used for a key, and a decision
   on how long it stays. Without it, the listener hears the new speed at the next reading, or opens the tool window to
   see it.
4. **The shortcut in the tool window's text when nothing is read.** The text "Start a reading with "Read Aloud" on the
   tab of a Markdown file" could name the shortcut as well. It costs keeping that text true to the keymap, as the
   tooltips are. Without it, the user finds the shortcut in the tooltip of "Read Aloud".
