# Developer checks of unit 0013

The checks of requirement 13 of the spec. Build the plugin with `./gradlew buildPlugin` and install the archive, or start
the sandbox IDE with `./gradlew runIde`, on the machine named in each part. Open `samples/markdown-features.md`, whose
text is long enough to start from the middle of. Write what was found in `record.md`.

## Linux x64

1. Select one sentence in the middle of a paragraph. The tooltip of "Read Aloud" on the tab says "Read Selection Aloud".
   Press it. Only the selected sentence is read, and the tool window, opened beforehand, shows "Selection" under the name
   of the file. Pause and resume. The line stays. Stop
2. Select part of a word at each end of a selection. The words that are cut are read whole
3. Put the caret in the middle of a paragraph, without a selection. The tooltip says "Read Aloud from Caret". Press it.
   The reading begins at the start of the sentence the caret is in and runs to the end of the file, and the tool window
   shows "From the caret". Move the caret during the reading. The line does not change
4. Put the caret at the start of the file. The tooltip says "Read Aloud", and a press reads the whole file, with
   neither line in the tool window
5. Put the caret in a code block, then in a table. The reading begins at "code example", then at "see table"
6. With nothing read, put the caret in the middle of a paragraph, click the editor so that it has the focus, and press
   Ctrl+Alt+Shift+Space. It reads what the button would read. Do the same with a selection
7. Show the file only as a preview, with the button on the tab that shows the preview alone. The tooltip says "Read Aloud"
   and a press reads the whole file
8. Make several carets with Alt+Shift+click or Ctrl+Alt+Shift+Down, with and without selections, and press the button.
   Note what is read and whether anything fails. Pause, resume and stop it, and press again
9. Make a block selection over several lines with Alt+Shift+drag, and do the same. Note what is read and whether
   anything fails
10. Look at the tool window during each of 1 to 9

## macOS on Apple Silicon

1. Read one selection
2. Read from one caret
