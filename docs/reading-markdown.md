# Reading Markdown aloud

How AloudRite reads each kind of Markdown, what it should do instead, and what is still to be decided. It is the
starting point for the unit that makes the plugin handle Markdown, and it is kept up to date as more is heard.

## Where it stands

Since unit 0006, the "Read Aloud" button hands the text of the file to the speech engine nearly as it is. The one
exception is characters above U+FFFF, among them most emoji, which are left out as a space, because the engine's Java
binding cannot hand them over intact. Everything else, the Markdown markers included, reaches the engine, which reads
some of them out and passes over others.

What is written under "Heard" was listened to by the developer on Linux x64, in October 2026, with the file
`samples/markdown-features.md`, which holds every kind below in a section of its own. Listen to that file again to
check a change.

## Each kind of Markdown

### Headings

- **Heard:** the `#` is read as "hashtag", at every level.
- **Wanted:** the markers left out.

### Bold and italic

- **Heard:** with asterisks, the markers are read out: "asterisk asterisk bold asterisk asterisk". Three asterisks
  around a word, for bold and italic together, are worse still. With underscores, `__bold__` and `_italic_`, nothing
  is read out.
- **Wanted:** the asterisks left out. The underscores are fine as they are.

### Strikethrough

- **Heard:** read as "tilde tilde struck out tilde tilde".
- **Wanted:** the markers left out.
- **Open:** whether the struck out words are read at all.

### Bulleted, nested and task lists

- **Heard:** the items run together as one sentence, nested or not. In a task list, the `x` of a ticked box is read,
  and an empty box says nothing.
- **Wanted:** a pause between the items.
- **Open:** whether the state of a task is said, as "done" and "not done", or left out.

### Numbered lists

- **Heard:** the voice pauses at each new number, as at the end of a sentence.
- **Wanted:** nothing; fine as it is.

### Links

- **Heard:** the whole address is read out. For a reference link, `[text][label]`, the label is read after the text,
  and the line that defines the label is read as well.
- **Proposed, not yet decided:** read only the text of a link and leave the address out, since the text is part of the
  sentence. For an address without text of its own, bare or in angle brackets, a short word such as "link", or only the
  domain. For a reference link, the label and the line of its definition left out.

### Images

- **Heard:** the `!` and the name of the file are read.
- **Wanted:** only the alternative text.

### Inline code

- **Heard:** the backticks are not read. Words in backticks sound fine. Real code in backticks is read nearly whole,
  apart from braces, and runs on as part of the sentence.
- **Wanted:** a pause before and after inline code.
- **Open:** how the pause is made: a comma, which gives the melody of an aside, or silence that the plugin adds
  around a piece of its own. And whether the symbols of code are read as they are or named, as "greater than or
  equal" for `>=`.

### Code blocks

- **Heard:** the language of the block, such as `kotlin`, is read, and then the code as one sentence.
- **Wanted:** the code left out, with something like "code example" in its place, and a pause after it long enough
  to pause the reading by hand. That last part touches the feature of pausing and resuming, which is to be a unit of
  its own.

### Quotes

- **Heard:** read aloud as text; the `>` is not read.
- **Wanted:** nothing; fine as it is.

### Tables

- **Heard:** the pipes are not read; the words of the cells are read one after the other, as one sentence.
- **Wanted:** a word such as "see table" in place of the table, since a table cannot be read aloud well.

### Horizontal rules

- **Heard:** the voice stutters where the `---` is. Possibly because the rule, between its blank lines, reaches the
  engine as a piece of its own; that was not checked.
- **Wanted:** the rule left out.
- **Open:** whether a pause takes its place.

### Emoji and symbols

- **Heard:** characters above U+FFFF, among them most emoji, are left out, with no pause. Emoji and symbols of three
  bytes do reach the engine: ⚓ is read as "anchor", ✓ is silent, ❤ is read as "red heart".
- **Open:** whether all emoji and symbols are made alike, and how.

### Escaped characters

- **Heard:** `\*` and `\#` are read with the backslash, as "backslash star" and "backslash hash".
- **Wanted:** at least the backslash left out.
- **Open:** whether the escaped character itself is read.

## Beyond Markdown

### Paragraphs

- **Heard:** no longer pause between two paragraphs. The next paragraph starts straight after the last sentence of the
  one before, which feels unnatural.
- **Wanted:** a slightly longer pause between paragraphs. The plugin already cuts the text at every blank line, so it
  knows where a paragraph ends.

### Pauses at punctuation

- **Heard:** a long sentence is made in parts, cut after a comma, a colon or a semicolon, so that a reading never waits
  for the engine. The pauses at those cuts sound okay.
