# Read from the caret or the selection

## Context

Unit 0006 made the "Read Aloud" button on the editor tab of a Markdown file read the whole file, from its beginning to
its end, as the editor holds it at the press. A listener who wants to hear one paragraph, or to go on from where they
are reading, has to listen from the top. Unit 0006 left this out on purpose and named it under "Beyond the request":
it costs a decision on whether the caret or the selection wins, and on how the button says which it will do. The
human asked for it as a unit of its own once unit 0006 was implemented.

This spec was first written when unit 0006 was the only reading there was. It was rewritten once units 0010, 0012,
0015, 0016, 0017 and 0020 were implemented, because what a reading is had changed under it: the text is now read as
Markdown, the button pauses and resumes, and a shortcut does what the button does.

What the implemented units established, and this unit builds on:

- The text is taken from the document of the file at the press, unsaved changes included, and stays fixed for the
  whole reading, also across a pause, a resume and a change of the speed.
- The text is read as Markdown, as unit 0010 made it. Front matter at the top of the file is taken off, the rest is
  parsed as a whole, with the position in the file of each block and of each inline element kept, and what a listener
  should hear is made from it: the words without the markers, "code example" for a code block, "see table" for a
  table, "link" for a bare address, a full stop added to an item of a list that has none, and the silences between
  blocks, between items and around inline code. Only then are characters above U+FFFF given to the engine as spaces.
- The words are cut into pieces for the engine: a sentence ends after a `.`, `!` or `?`, optionally followed by `"`,
  `'`, `)`, `]` or `*`, and then white space, and every block, such as a paragraph, a heading or an item of a list,
  is a piece of its own. A long sentence is also cut at a comma, colon or semicolon.
- A text that would read nothing, because it is empty or holds only white space, or nothing but front matter, HTML
  comments or struck out text, reads nothing and shows the notification that there is nothing to read, and asks no
  question about the download. One of nothing but a code block reads "code example".
- After a download that the press started, the text of the press is read by itself.
- While no file is read, the button on the tab shows the person speaking and the tooltip "Read Aloud". While the file
  of the tab is read, it shows "Pause Reading", and while that reading is paused, "Resume Reading". The tooltip names
  the shortcut of the action "Read Aloud, Pause or Resume", Ctrl+Alt+Shift+Space on Linux and ⌘⌥⇧Space on macOS,
  except on the tab of a Markdown file while another file is read, where a press reads its own file and the shortcut
  would pause the other.
- While no file is read, that shortcut reads the Markdown file of the selected editor, as a press on its button does.

A Markdown file can be open in the IDE as text, as a preview, or as both side by side. Only the text shows a caret and
a selection. The IDE also keeps the caret of a file where it was the last time the file was open.

## Requirements

1. When the file's text editor in the tab where the button is pressed holds a selection that is not empty, a press
   on "Read Aloud" reads what the selection covers, and nothing else of the file. What it covers is decided in the
   Markdown of the whole file, as requirement 4 says.
2. When it holds no selection, a press reads from the beginning of the sentence the caret is in to the end of the
   file. A sentence is one as the plugin cuts the words that are read, as the context says, without the cut at a
   comma, colon or semicolon: it begins at the start of what is read of the file, after the end of a sentence, or at
   the start of a block. Where the caret is in text that is not read, such as the marker of a heading, the address of
   a link, an HTML comment, white space between two sentences, or front matter, the sentence is the one of the first
   word read after the caret.
3. When nothing is read after the caret, the whole file is read.
4. The part is chosen in the Markdown of the whole file, and what is read of it is what the whole file would read
   there:
    - A code block or a table that the selection touches, wholly or in part, is read as a whole, as "code example"
      followed by its silence or as "see table". A caret in a code block or a table starts the reading at those words.
    - When the part begins in an item of a list after its number, the number of the item is read before the first
      word of the part.
    - A word that the selection cuts is read whole.
    - Everything else that unit 0010 established holds within the part as it does in the whole file: a code block
      in the part is read as "code example" and not as text, a reference link is read by its
      text also when the line that defines it is outside the part, a comment that is opened before the part and
      closed within or after it is not read, and a selection of front matter reads nothing.
    - The part begins with its first words, without a silence of the plugin before them.
5. When the tab shows the file only as a preview, without its text, the whole file is read, as in unit 0006.
6. Only the primary caret and its selection count. Other carets are not looked at. With several carets, also those
   that a block selection makes, a press and the shortcut read by the primary caret and its selection as requirements
   1 to 5 say, the reading plays, pauses, resumes and stops as any other, and nothing fails: no error is shown or
   logged, and the next press works.
7. While nothing is read, the shortcut of "Read Aloud, Pause or Resume" reads from the selected editor of a Markdown
   file what a press on that editor's "Read Aloud" button would read, by requirements 1 to 6.
8. The button on the tab stays as units 0012 and 0017 made it. While the file of the tab is not read, its tooltip
   says "Read Aloud", with the shortcut where unit 0017 names it, whether a press will read the selection, from the
   caret or the whole file, and its icon is the person speaking. "Pause Reading", "Resume Reading", "Stop Reading",
   the names of the actions in Settings › Keymap and in Find Action stay as they are.
9. Everything else that a reading of a whole file does holds for the part that is read: it is the text as the editor
   holds it at the press, unsaved changes included, and stays fixed for the reading, also across a pause, a resume and
   a change of the speed; characters above U+FFFF are given as spaces; a part that would read nothing shows the
   notification that there is nothing to read and asks no question about the download; and after a download that the
   press started, the part of the press is read by itself.
10. While a selection is read, also a paused reading of one, the tool window of every project shows "Selection" in a
    line of its own under the name of the file, and while a reading from the caret is read or paused, "From the
    caret". While a whole file is read, it shows neither, as it does now. What it shows stays the same for the whole
    reading, also when the selection or the caret changes during it.
11. The selection and the caret are not changed by a press, by the shortcut or by the reading.
12. Automated tests cover requirements 1 to 10 with the stand-ins of unit 0006 for the engine and the player: the text
    that reaches the engine for a selection within a paragraph, one that cuts words, one across two paragraphs, one
    that touches a code block and one that touches a table in part, one that begins in an item of a list after its
    number, one of front matter only, and an empty selection and one of white space only; for a caret in the middle
    of a sentence, at its first character, between two sentences, in the marker of a heading, in the address of a
    link, in front matter, inside a fenced code block, inside a table, after the last word, and at the start of the
    file; for a reference link whose definition is outside the part and a comment opened before it; for several
    carets with and without selections, and a block selection over several lines, each read, paused, resumed and
    stopped, and a press after it; the shortcut with a selection and with a caret; the tooltip "Read Aloud" with a
    selection, with a caret and for a whole file, with and without the shortcut named; what the tool window shows for a
    selection, a reading from the caret and a whole file, while it plays and while it is paused, and after the caret
    moves during it; and a part read after a download.
    They need no model, no audio device and no network.
13. On Linux x64, the developer has read a selection, read from a caret in the middle of a paragraph, read from a
    caret with the shortcut, read a file shown only as a preview, looked at the tool window during each of these, and read with several carets and with a block
    selection over several lines, pausing, resuming and stopping those two readings and pressing again after them. On
    macOS on Apple Silicon, the developer has read one selection and read from one caret. The record holds what they
    found, and for the several carets and the block selection what was read and whether anything failed.
14. `README.md` describes what a press and the shortcut read with a selection, with a caret, and in a preview, and
    what the tool window shows.

## Decisions

- **The selection wins, and otherwise the caret.** The human chose this. A selection is made on purpose and says
  exactly what to read. A caret is always somewhere, so it says less, and it starts the reading only where the user
  put it or left it. Rejected: reading the whole file when there is no selection, which leaves out reading from where
  one is; and separate actions in the context menu for the selection and the caret, which keep the button as it is
  but hide what the human wants in a menu.
- **The part is chosen in the Markdown of the whole file.** The human chose this when the spec was rewritten. The
  first version chose the part from the text of the file and then read it as a file of its own, which was sound while
  the text was read nearly as it stood. Since unit 0010 the text is read as Markdown, and a piece of Markdown read
  alone loses what it means in the file: a caret inside a fenced code block makes its closing fence open a new code
  block that runs to the end of the file, rows of a table without its header are read with their pipes, a reference
  link loses its definition, and a comment opened before the part no longer hides what follows. The parser already
  keeps the position of each block and inline element in the file, so the whole file is parsed as it is for a full
  reading and the part is cut out of what it would read. Rejected: cutting the text first and parsing the part alone,
  for those cases. This makes the start of a sentence one of the words that are read, not of the Markdown in the
  file, which is where the plugin cuts sentences since unit 0010.
- **From the beginning of the sentence, not from the caret itself.** A reading that begins in the middle of a word or
  a sentence is hard to follow, and a caret is rarely put at the exact start of a sentence. The sentence is the one
  the plugin already cuts, without the cut at a comma, colon or semicolon, which is about making audio sooner and not
  about where a sentence starts. A caret in text that is not read belongs to the sentence of the next word read, so a
  caret on the `#` of a heading starts at the heading and one in the address of a link at the sentence that holds the
  link. Decided by the specifier and open to the human, who chose the sentence the caret is in without saying what a
  sentence is.
- **A code block or a table that the selection touches is read whole.** The human chose this. Neither is read word by
  word, so a part of one has no words of its own to read, and leaving out one that a selection begins or ends in
  would skip it without a sound. Rejected: reading one only when the selection covers it wholly. Whether the selection
  touches a block is a comparison of positions, so it costs little next to cutting within a paragraph, which the unit
  needs anyway.
- **The number of an item is read when the part begins after it.** The human chose this. The number says where in
  the list the listener is, and a numbered list read from its third item without "3" sounds like prose. Rejected:
  starting at the first word of the part without the number.
- **A word that the selection cuts is read whole.** A selection made with the mouse often misses a letter at either
  end, and half a word is read by the engine as a different word or as letters. Decided by the specifier and open to
  the human: the alternative is to read exactly what is selected.
- **A caret at the end reads the whole file.** The IDE keeps the caret where it last was, and after writing a file
  that is often at its end. Reading nothing there, with the notification of an empty file, would be wrong for a file
  that is not empty. Decided by the specifier and open to the human.
- **The shortcut reads what the button would read.** The human chose this. The shortcut of unit 0017 starts a reading
  as a press on the button does, and a key pressed with the focus in the editor is the most natural way to read from
  the caret. Rejected: the shortcut reading the whole file, which would make the key and the button read different
  text for the same file.
- **The tooltip stays "Read Aloud".** The human chose this in the developer's check on Linux x64, which corrected
  the spec. Its first version had the tooltip say what a press would read, as "Read Selection Aloud", "Read Aloud
  from Caret" or "Read Aloud". In the running IDE the tooltip did not follow the selection or the caret: the toolbar
  of the tab works out the tooltip again only when the IDE sees activity, which a press is and a move of the caret or
  a change of the selection is not, so it kept the text of its last update until a reading had started once. Making
  it follow would have meant waking the toolbar on every move of the caret and every change of the selection, and
  choosing the part in the Markdown of the whole file each time, for a text the human did not find worth that.
  Since the caret is always somewhere, a press may start the reading far from the top; the listener hears it at
  once, and the tool window says "Selection" or "From the caret" by requirement 10. Rejected: keeping the tooltip
  that says what will be read, and waking the toolbar on every move of the caret and change of the selection to make
  it right.
- **The part is fixed at the press, as the text is.** A reading that is paused and resumed, or whose speed is
  changed, goes on in the part it was given, so pausing, resuming and the speed need nothing of this unit.
- **Several carets and a block selection are tried, though they are not supported.** The human chose this. Reading
  them as they mean is a non-goal, and what they read may sound odd, but a user can make them at any time, so a press
  with them has to read something and leave the plugin working. Requirement 6 says what that is, and requirements 12
  and 13 try it.
- **The tool window says what is read.** The human took this in from "Beyond the request". A reading from the caret
  may start far from the top, and a listener who comes back to it in the tool window, perhaps from another file or
  project, cannot otherwise tell a part from the whole file. It shows what was pressed, not where the part lies, so it
  stays the same while the caret moves. A whole file shows nothing new, so that the tool window looks as it did for
  the reading every user has had so far. The words "Selection" and "From the caret" are decided by the specifier and
  open to the human.
- **Linux, and a short check on macOS.** The human chose this. The choice of the part is computation in Kotlin and
  the tool window is user interface of the IntelliJ Platform, neither of which differs between the platforms, so
  `AGENTS.md` would leave the check to Linux x64. The human wanted one selection and one caret read on macOS on Apple
  Silicon as well. Rejected: the full check on both machines, which the first version of the spec asked for.

## Non-goals

- Reading the selection of a preview, where the IDE shows no caret.
- Several carets or selections, and a block selection read as columns: only the primary caret and its selection
  count. That a press with them works is requirement 6.
- Moving the caret or the selection along with the reading, or highlighting what is read.
- Reading from the caret in a file that is not Markdown, or in an editor other than the one in the tab where the
  button is pressed, or the selected one for the shortcut.
- A menu entry or an action of its own for reading the selection or reading from the caret.
- The reading time of unit 0022, a draft, for a part.
- A tooltip of the button that says what a press will read.

## Beyond the request

The human took in item 3, the tool window saying what is read, which is now requirement 10. The other items keep
their numbers.

1. **Reading from the paragraph, not the sentence.** Starting at the beginning of the paragraph the caret is in would
   give more context. It costs a second rule for the start next to that of the sentence. Without it, a listener who
   wants the whole paragraph puts the caret in its first sentence or selects it.
2. **Reading from the caret in a preview.** A preview has no caret, but a click in it could choose where to start. It
   costs mapping a position in the rendered preview back to the text, through the IDE's Markdown plugin, which unit
   0003 decided the plugin needs no class of. Without it, a preview reads the whole file.
3. **The tool window says what is read.** It could show "selection" or "from the caret" beside the name of the file.
   It costs a line of text and its tests. Without it, the listener knows what they pressed, and the tooltip told them
   before.
