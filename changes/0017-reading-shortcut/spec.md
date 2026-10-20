# Reading shortcut

## Context

Unit 0012, a draft, makes the "Read Aloud" button on the editor tab of a Markdown file pause and resume a reading,
with a second button beside it that stops. Each of them takes the mouse. Unit 0012 named pausing on a keyboard
shortcut under "Beyond the request": a listener with their hands on the keyboard could pause without the mouse, at
the cost of a shortcut that does not clash with those of the IDE, and of a decision on which reading it pauses when
the focus is on another file. The human asked for it as a unit of its own, and chose that the shortcut does what the
tab button does: it starts a reading, pauses it and resumes it.

What units 0006 and 0012 established, and this unit builds on:

- One file is read at a time, in the whole IDE. A reading goes on when another tab or tool window is selected.
- A press on "Read Aloud" reads the Markdown file of its tab. While the file is read, the button pauses the reading,
  and while the reading is paused, it resumes it. A resume starts at the beginning of the piece that was playing.
- Pressing "Read Aloud" on another Markdown file ends the reading of the first and reads the second.
- The first press downloads the speech model when it is not there yet, after asking, as unit 0005 made it.

The IDE lets a plugin give an action a default shortcut in each of its keymaps. The user can change it, or remove
it, in Settings › Keymap, and finds the action by its name in Find Action as well.

This unit is written to be built after unit 0012, since it needs the pause and the resume.

## Requirements

1. The plugin has an action named "Read Aloud, Pause or Resume", which is listed in Settings › Keymap and found by
   Find Action.
2. While no file is read, the action reads the Markdown file of the selected editor in the IDE window that has the
   focus, as a press on its "Read Aloud" button does, including the question about the download when the model is
   not there yet. When the selected editor is not one of a Markdown file, or no editor is open, the action does
   nothing.
3. While a file is read, the action pauses that reading, as a press on "Pause Reading" does, whichever editor, tool
   window or project window has the focus, including the editor of another Markdown file.
4. While a reading is paused, the action resumes it, as a press on "Resume Reading" does, wherever the focus is as in
   requirement 3.
5. After each use of the action, the buttons on the tab of the file and the tool window of unit 0015 show the state
   of the reading as they do after a press on the tab button.
6. In the IDE's default keymap on Linux, the action has the shortcut Ctrl+Alt+Shift+Space. In its default keymap on
   macOS, it has ⌘⌥⇧Space.
7. In the keymaps of requirement 6, as IntelliJ IDEA 2026.2.3 ships them with the plugins it bundles, no other action
   has the shortcut of requirement 6.
8. A shortcut the user gives the action in Settings › Keymap does what requirements 2 to 4 say, and a shortcut the
   user removes does nothing any more.
9. Automated tests cover requirements 1 to 5 and 7 with the stand-ins of unit 0006 for the engine and the player:
   that the action starts a reading of the selected Markdown file, does nothing when the selected file is not
   Markdown, pauses and resumes a reading with the focus on the file that is read, on another Markdown file and in
   another project, and that the buttons follow. They need no model, no audio device and no network.
10. The developer has started, paused and resumed a reading with the shortcut, with the focus on the file that is
    read, on another file, and in a tool window, on Linux x64 and on macOS on Apple Silicon. The record holds what
    they found, and whether the desktop or another application took the shortcut before the IDE did.
11. `README.md` describes the shortcut and where to change it.

## Decisions

- **The shortcut does what the tab button does.** The human chose this. One key starts, pauses and resumes, so the
  user learns one key for listening. Rejected: a shortcut that only pauses and resumes, which leaves the start to the
  mouse; and a second shortcut that stops, which is one more key that has to be free in every keymap.
- **The shortcut pauses the reading wherever the focus is.** The human chose this. One file is read in the whole IDE,
  so there is never a doubt which reading a pause means, and a listener who has moved on to another file is the one
  who most needs a pause without finding the tab first. Rejected: a shortcut that works only in the editor of the
  file that is read, which fails exactly when the user has moved on.
- **While a file is read, the shortcut pauses it, also from another Markdown file.** On the tab of another Markdown
  file, the button starts a reading of that file, because the button belongs to its tab. The shortcut belongs to no
  tab, so it acts on the reading that exists. Starting another file stays a press on its button. Decided by the
  specifier and open to the human.
- **The shortcut does nothing outside a Markdown file.** With nothing to read and nothing playing, there is nothing
  for it to do, and a message for a key pressed by mistake would be noise. Decided by the specifier and open to the
  human: the alternative is a short notification that only Markdown files are read.
- **Ctrl+Alt+Shift+Space, and ⌘⌥⇧Space on macOS.** The human chose to have a default key. The space bar is the key
  that pauses and plays in media players. Every shorter combination with Space is taken in the IDE's default keymaps:
  Ctrl+Space, Ctrl+Shift+Space and Ctrl+Alt+Space among them. The same holds for Ctrl+Alt+Shift with the letters P,
  R, S and A that would name the action. Checked against the keymaps and the plugins of IntelliJ IDEA 2026.2.3.
  Rejected: Alt+Shift+P, which is free in the IDE but on macOS types a character in the editor; and no default,
  which leaves every user to assign a key before they can use it. Whether the desktop takes the key first is not in
  the keymaps, so requirement 10 asks the developer. Decided by the specifier and open to the human.
- **The action has a name of its own.** The name in Settings › Keymap says what the key does in every state, where
  "Read Aloud" alone would hide the pause. Decided by the specifier and open to the human.

## Non-goals

- A shortcut that stops a reading.
- Shortcuts for the speed of unit 0016.
- A menu entry for the action, beyond Find Action.
- Keymaps other than the IDE's default ones on Linux and macOS, such as Emacs or Sublime Text. The action has no key
  there until the user gives it one.
- Windows, which the plugin does not support.
- The shortcut where a component of the IDE takes the key for itself before the IDE's keymap does, such as the
  terminal can.

## Beyond the request

- **The shortcut in the tooltips of the tab buttons.** "Read Aloud (Ctrl+Alt+Shift+Space)" would teach the key to a
  user who never opens the keymap. It costs tying the shortcut to the tab buttons' tooltips, which the IDE does by
  itself only when the buttons and the shortcut are the same action. Without it, the user finds the key in
  `README.md` or in Settings › Keymap.
- **A shortcut for the selected file even during a reading.** A second key could start the selected Markdown file
  while another is read, as its tab button does. It costs another free key. Without it, the user presses the button
  on the tab of the other file.
