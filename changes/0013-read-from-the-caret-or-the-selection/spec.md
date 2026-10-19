# Read from the caret or the selection

## Context

Unit 0006 made the "Read Aloud" button on the editor tab of a Markdown file read the whole file, from its beginning to
its end, as the editor holds it at the press. A listener who wants to hear one paragraph, or to go on from where they
are reading, has to listen from the top. Unit 0006 left this out on purpose and named it under "Beyond the request":
it costs a decision on whether the caret or the selection wins, and on how the button says which it will do. The
human asked for it as a unit of its own once unit 0006 was implemented.

What unit 0006 established, and this unit builds on:

- The text is taken from the document of the file at the press, unsaved changes included, and stays fixed for the
  whole reading.
- Each character above U+FFFF is given to the engine as a space. A text that is then empty or white space only reads
  nothing and shows the notification that there is nothing to read, and asks no question about the download.
- After a download that the press started, the text of the press is read by itself.
- The plugin cuts the text into pieces: a sentence ends after a `.`, `!` or `?`, optionally followed by `"`, `'`, `)`,
  `]` or `*`, and then white space, and a block ends after a blank line.

A Markdown file can be open in the IDE as text, as a preview, or as both side by side. Only the text shows a caret and
a selection. The IDE also keeps the caret of a file where it was the last time the file was open.

## Requirements

1. When the file's text editor in the tab where the button is pressed holds a selection that is not empty, a press
   on "Read Aloud" reads the selected text, and nothing else of the file.
2. When it holds no selection, a press reads from the beginning of the sentence the caret is in to the end of the
   file. A sentence begins at the start of the file, or after the end of a sentence or a block as unit 0006 cuts
   them. A caret in the white space between two sentences starts at the next one.
3. When nothing but white space follows the start of requirement 2, the whole file is read.
4. When the tab shows the file only as a preview, without its text, the whole file is read, as in unit 0006.
5. Only the primary caret and its selection count. Other carets are not looked at.
6. The tooltip of the button says what a press will read: "Read Selection Aloud" in the case of requirement 1, "Read
   Aloud from Caret" in the case of requirement 2 when the reading does not start at the beginning of the file, and
   "Read Aloud" when the whole file will be read. The loudspeaker icon stays the same in all three.
7. Everything that unit 0006 established holds for the part that is read, as it does for a whole file: it is the text
   as the editor holds it at the press, unsaved changes included, and stays fixed for the reading; characters above
   U+FFFF are given as spaces; a part that is then empty or white space only reads nothing and shows the notification
   that there is nothing to read, and asks no question about the download; and after a download that the press
   started, the part of the press is read by itself.
8. The selection and the caret are not changed by a press or by the reading.
9. Automated tests cover requirements 1 to 7: the text that reaches a stand-in engine for a selection, for a caret in
   the middle of a sentence, at its first character, between two sentences, in a heading followed by a blank line, at
   the start of the file and after the last sentence; the tooltip in each case; and an empty selection, a selection of
   white space only, and a part read after a download. They need no model, no audio device and no network.
10. The developer has read a selection, read from a caret in the middle of a paragraph, and read a file shown only as
    a preview, on Linux x64 and on macOS on Apple Silicon, and the record holds what they found.
11. `README.md` describes what a press reads with a selection, with a caret, and in a preview.

## Decisions

- **The selection wins, and otherwise the caret.** The human chose this. A selection is made on purpose and says
  exactly what to read. A caret is always somewhere, so it says less, and it starts the reading only where the user
  put it or left it. Rejected: reading the whole file when there is no selection, which leaves out reading from where
  one is; and separate actions in the context menu for the selection and the caret, which keep the button as it is
  but hide what the human wants in a menu.
- **From the beginning of the sentence, not from the caret itself.** A reading that begins in the middle of a word or
  a sentence is hard to follow, and a caret is rarely put at the exact start of a sentence. The sentence is the one of
  unit 0006, which the plugin already finds when it cuts the text, without the cut at a comma, colon or semicolon,
  which is about making audio sooner and not about where a sentence starts. Decided by the specifier and open to the
  human, who chose "the sentence the caret is in" without saying what a sentence is.
- **A caret at the end reads the whole file.** The IDE keeps the caret where it last was, and after writing a file
  that is often at its end. Reading nothing there, with the notification of an empty file, would be wrong for a file
  that is not empty. Decided by the specifier and open to the human.
- **The tooltip says what will be read.** Since the caret is always somewhere, a press may start the reading far from
  the top, where the listener expected the whole file. The tooltip tells them before they press. The icon stays the
  same, so that the button is recognised. Decided by the specifier and open to the human, as unit 0006 named this
  cost without deciding it.
- **The part is chosen from the text of the file before anything else is done with it.** Unit 0006 gives characters
  above U+FFFF as spaces, and unit 0010, a draft, takes Markdown markers out. Choosing the part first, in the text the
  user sees, means the caret and the selection mean what they show, and the rules of those units apply to the part as
  they do to a whole file.
- **The part is fixed at the press, as the text is.** A reading that is paused and resumed, which unit 0012 adds,
  resumes in the part it was given, and so needs nothing of this unit.

## Non-goals

- Reading the selection of a preview, where the IDE shows no caret.
- Several carets or selections, and a block selection read as columns: only the primary caret and its selection
  count.
- Moving the caret or the selection along with the reading, or highlighting what is read.
- Reading from the caret in a file that is not Markdown, or in an editor other than the one in the tab where the
  button is pressed.
- A keyboard shortcut or a menu entry for any of the three.
- Pausing and resuming, which is unit 0012.

## Beyond the request

- **Reading from the paragraph, not the sentence.** Starting at the beginning of the paragraph the caret is in would
  give more context. It costs a definition of a paragraph next to the sentence and the block of unit 0006. Without it,
  a listener who wants the whole paragraph puts the caret in its first sentence or selects it.
- **Reading from the caret in a preview.** A preview has no caret, but a click in it could choose where to start. It
  costs mapping a position in the rendered preview back to the text, through the IDE's Markdown plugin, which unit
  0003 decided the plugin needs no class of. Without it, a preview reads the whole file.
