# The developer's checks of the reading shortcuts

These are the checks of requirement 14 of [spec.md](spec.md), which the automated tests cannot make, with the check of
requirement 10 in the IDE itself. The look of the tooltips is checked on Linux x64 only, and the keys on Linux x64 and
on macOS on Apple Silicon: whether a key reaches the IDE at all is up to the desktop of each machine, which no test
sees.

The look of the tooltips is also the last task of the section "The tooltips" of [plan.md](plan.md), which the
implementer could not do in the sandbox IDE on the developer's screen, as [record.md](record.md) says.

The implementer writes what the developer finds into [record.md](record.md), on the developer's word. The tables below
can be filled in on each machine and handed over as they are.

## Setup, on each machine

1. Check out `feature-0017-reading-shortcut` and run `./gradlew runIde`. A separate sandbox IDE opens with the plugin.
2. In that IDE, open the project, and open `samples/ten-thousand-words.md` and `samples/markdown-features.md` in two
   tabs. Open the tool window "AloudRite" with its button on the tool window bar on the right, or with View › Tool
   Windows › AloudRite.
3. Under Settings › Keymap, check that the keymap in use is "Default for XWin", "Default for GNOME" or "Default for
   KDE" on Linux and "macOS" on macOS, and type "Read" in the search field of the page: "Read Aloud, Pause or Resume",
   "Stop Reading", "Read Slower" and "Read Faster" are listed, each once, with these keys.

| Action                      | Linux                 | macOS    |
|-----------------------------|-----------------------|----------|
| Read Aloud, Pause or Resume | Ctrl+Alt+Shift+Space  | ⌃⌥⇧Space |
| Stop Reading                | Ctrl+Alt+Shift+O      | ⌃⌥⇧O     |
| Read Slower                 | Ctrl+Alt+Shift+Comma  | ⌃⌥⇧,     |
| Read Faster                 | Ctrl+Alt+Shift+Period | ⌃⌥⇧.     |

## The look of the tooltips, on Linux x64 only

4. Hover each button below, in the light and in the dark theme, and note whether it shows its text with the key after
   it in the IDE's grey, as the tooltips of the IDE's own toolbar buttons do, such as the buttons beside it on the tab,
   and whether any button shows a second tooltip as well.
    - On the tab of `ten-thousand-words.md`, while nothing is read: "Read Aloud" with Ctrl+Alt+Shift+Space.
    - After pressing "Read Aloud" there: "Pause Reading" with Ctrl+Alt+Shift+Space, and "Stop Reading" with
      Ctrl+Alt+Shift+O.
    - After pressing "Pause Reading": "Resume Reading" with Ctrl+Alt+Shift+Space, and "Stop Reading" with its key.
    - On the tab of `markdown-features.md`, while the other file is read and while it is paused: "Read Aloud" with no
      key.
    - In the tool window while a file is read: "Pause" with "Pause Reading" and Ctrl+Alt+Shift+Space, "Stop" with "Stop
      Reading" and Ctrl+Alt+Shift+O, the minus button with "Slower" and Ctrl+Alt+Shift+Comma, and the plus button with
      "Faster" and Ctrl+Alt+Shift+Period. While paused, "Resume" with "Resume Reading" and Ctrl+Alt+Shift+Space.

| Tooltips           | Text with the key in grey | A second tooltip | Notes |
|--------------------|---------------------------|------------------|-------|
| Light, tab         |                           |                  |       |
| Light, tool window |                           |                  |       |
| Dark, tab          |                           |                  |       |
| Dark, tool window  |                           |                  |       |

## The keys, on Linux x64 and on macOS on Apple Silicon

Start each of these with nothing read and the speed at 1.0.

5. **With the focus on the file that is read.** Click in the editor of `ten-thousand-words.md` and press the key of
   "Read Aloud, Pause or Resume": the file is read. Press it again: the reading pauses, and the tab button shows
   "Resume Reading". Press it again: the reading goes on. Press the key of "Read Faster" and then that of "Read
   Slower": the sentence begins again, a little faster, and then at 1.0, and the field in the tool window follows.
   Press the key of "Stop Reading": the reading ends, and the stop button on the tab goes.
6. **With the focus on another file.** Start the reading of `ten-thousand-words.md` with its button, then click in the
   editor of `markdown-features.md`. Press each of the four keys as in step 5: they act on the reading of
   `ten-thousand-words.md`, and the key of "Read Aloud, Pause or Resume" pauses and resumes it rather than reading
   `markdown-features.md`.
7. **With the focus in a tool window.** Start the reading again, click in the tool window "Project", and press each
   of the four keys as in step 5.
8. **The speed with no file read.** With nothing read, press the key of "Read Faster" three times and that of "Read
   Slower" once, with the tool window open and once with it closed: the field shows 1.1 after the first round and
   1.2 after the second. At 2.0 the key of "Read Faster" does nothing, and at 0.75 that of "Read Slower".
9. For each key, note whether the IDE got it, or whether the desktop or another application took it before the IDE
   did, and what that did.

| Machine                | Key                         | Focus on the file read | On another file | In a tool window | No file read | Taken before the IDE by |
|------------------------|-----------------------------|------------------------|-----------------|------------------|--------------|-------------------------|
| Linux x64              | Read Aloud, Pause or Resume |                        |                 |                  | not asked    |                         |
| Linux x64              | Stop Reading                |                        |                 |                  | not asked    |                         |
| Linux x64              | Read Slower                 |                        |                 |                  |              |                         |
| Linux x64              | Read Faster                 |                        |                 |                  |              |                         |
| macOS on Apple Silicon | Read Aloud, Pause or Resume |                        |                 |                  | not asked    |                         |
| macOS on Apple Silicon | Stop Reading                |                        |                 |                  | not asked    |                         |
| macOS on Apple Silicon | Read Slower                 |                        |                 |                  |              |                         |
| macOS on Apple Silicon | Read Faster                 |                        |                 |                  |              |                         |

## A key of the user's, on Linux x64 and on macOS on Apple Silicon

10. Under Settings › Keymap, give "Stop Reading" a second key of your choice, such as Ctrl+Alt+Shift+F11, and remove
    the key of "Read Faster". Press OK.
11. Start a reading: the new key stops it, as Ctrl+Alt+Shift+O still does. Ctrl+Alt+Shift+Period no longer changes
    the speed. The tooltip of "Stop Reading" still names Ctrl+Alt+Shift+O, the first of its keys, and the plus button
    in the tool window shows "Faster" alone.
12. Remove the key of "Stop Reading" that the keymap gave it, so that only yours is left: its tooltips on the tab and
    in the tool window now name yours. Put the keymap back with "Reset" on the page afterwards.

| Machine                | A key given acts | A removed key does nothing | The tooltips follow | Notes |
|------------------------|------------------|----------------------------|---------------------|-------|
| Linux x64              |                  |                            |                     |       |
| macOS on Apple Silicon |                  |                            |                     |       |
