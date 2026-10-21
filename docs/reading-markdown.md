# Reading Markdown aloud

How AloudRite reads each kind of Markdown, how it sounded before the plugin handled Markdown, and what was heard
since. It was the starting point for unit 0010, which makes the plugin handle Markdown, and it is kept up to date as
more is heard.

## Where it stands

Since unit 0010, the "Read Aloud" button reads the file as Markdown. The text is parsed into its blocks, such as
headings, paragraphs, lists and code blocks, and their inline elements, such as bold words, links and code, and the
speech engine is handed what a listener should hear: the text without its markers, words of the plugin's own in place
of what cannot be read aloud well, and silence where the structure asks for a pause. The file itself is not changed.

The pauses are silence that the plugin puts into the audio, not punctuation, so that they do not change the melody of
the sentence around them. The lengths below are those at the speed of 1.0×, and each is divided by the speed of the
reading, so that at 2.0× it is half as long and at 0.75×, the slowest speed, a third longer. Their lengths at 1.0×
are:

| Where                                      | Silence                                                      |
|--------------------------------------------|--------------------------------------------------------------|
| Between two blocks, such as two paragraphs | half a second, beyond the pause at the end of the sentence   |
| In place of a horizontal rule              | half a second, the same as between two blocks                |
| Before and after inline code               | a quarter of a second                                        |
| After "code example"                       | two seconds in all, long enough to pause the reading by hand |
| Between two items of a list                | a quarter of a second, beyond the end of the sentence        |

Where two pauses meet, as a code block followed by the next block, the longest is heard, once. Nothing silent is added
before the first words of a file or after its last.

Before unit 0010, since unit 0006, the button handed the text of the file to the speech engine nearly as it is, the
Markdown markers included, which the engine read out or passed over. What is written under "Heard in unit 0006" below
was listened to by the developer on Linux x64, in October 2026, with the file `samples/markdown-features.md`, which
holds every kind below in a section of its own. Listen to that file again to check a change. What was heard after unit
0010 is under "Heard after unit 0010" at the end.

## Each kind of Markdown

### Headings

- **Heard in unit 0006:** the `#` is read as "hashtag", at every level.
- **Wanted:** the markers left out.
- **Now:** the text of the heading is read, without its `#` at any level, and without the line of `=` or `-` under a
  heading written that way.

### Bold and italic

- **Heard in unit 0006:** with asterisks, the markers are read out: "asterisk asterisk bold asterisk asterisk". Three
  asterisks around a word, for bold and italic together, are worse still. With underscores, `__bold__` and `_italic_`,
  nothing is read out.
- **Wanted:** the asterisks left out. The underscores are fine as they are.
- **Now:** the words are read, without their asterisks or underscores. The underscores were not read out before
  either, so they are meant to sound as they did.

### Strikethrough

- **Heard in unit 0006:** read as "tilde tilde struck out tilde tilde".
- **Wanted:** the markers left out.
- **Decided:** the struck out words are not read either. They are words the author no longer means, often written next
  to the words that replace them, and a listener cannot hear which words were struck out.
- **Now:** text between two tildes on each side, `~~words~~`, is left out with its tildes. A single tilde is handed to
  the engine as it is. A file whose only words are struck out reads nothing. The number of an item of a numbered list
  is read even when all of its words are struck out, so that the numbers are heard without a gap, and a file of
  nothing but such a list reads its numbers.

### Bulleted, nested and task lists

- **Heard in unit 0006:** the items run together as one sentence, nested or not. In a task list, the `x` of a ticked
  box is read, and an empty box says nothing.
- **Wanted:** a pause between the items.
- **Decided:** the state of a task is not said, since saying "done" or "not done" adds a word to every line of a list
  that the eye takes in at once.
- **Now:** the marker of an item, `-`, `*` or `+`, is not read, at any level of nesting, and neither is the box of a
  task, `[ ]`, `[x]` or `[X]`. Each item ends as a sentence does, with its pause and the fall of the voice: an item that
  does not end with `.`, `!` or `?` is handed to the engine with a full stop added, and one that does is handed over as
  it is, so that no item gets the end of a sentence twice. A full stop before a closing quote, bracket or asterisk
  counts. Beyond that end, a quarter of a second of silence follows every item but the last, the same after each, and
  between the paragraphs of one item as well, so that the items do not sound like the sentences of one paragraph.

### Numbered lists

- **Heard in unit 0006:** the voice pauses at each new number, as at the end of a sentence.
- **Wanted:** nothing; fine as it is.
- **Heard later:** the pause came from the full stop at the end of each item of the sample, all of which have one. In a
  list whose items had no full stops, the items ran together, which was "very awkward".
- **Now:** the numbers are read, counted from the first number of the list, with their `.` or `)`. Each item ends as a
  sentence does, as in a bulleted list.

### Links

- **Heard in unit 0006:** the whole address is read out. For a reference link, `[text][label]`, the label is read
  after the text, and the line that defines the label is read as well.
- **Proposed then:** read only the text of a link and leave the address out, since the text is part of the sentence.
  For an address without text of its own, bare or in angle brackets, a short word such as "link", or only the domain.
  For a reference link, the label and the line of its definition left out.
- **Now:** of a link, `[text](address)`, only the text is read. Of a reference link, `[text][label]`, `[text][]` or
  `[text]`, only the text is read, and the line that defines the label, `[label]: address`, is not read. An address
  without text of its own, bare as `https://example.com/page` or `www.example.com`, in angle brackets, or an e-mail
  address, is read as the word "link", not as its domain, which is still a string of words not meant to be heard.

### Images

- **Heard in unit 0006:** the `!` and the name of the file are read.
- **Wanted:** only the alternative text.
- **Now:** only the alternative text is read. An image without alternative text is not read at all, and leaves no
  pause.

### Inline code

- **Heard in unit 0006:** the backticks are not read. Words in backticks sound fine. Real code in backticks is read
  nearly whole, apart from braces, and runs on as part of the sentence.
- **Wanted:** a pause before and after inline code.
- **Decided:** the pause is silence the plugin adds, not a comma, which would give the melody of an aside. The symbols
  of code are read as the engine reads them, and not named, since naming them is a dictionary of its own for every
  language.
- **Now:** a quarter of a second of silence before and after the code, which is handed to the engine as it is.

### Code blocks

- **Heard in unit 0006:** the language of the block, such as `kotlin`, is read, and then the code as one sentence.
- **Wanted:** the code left out, with something like "code example" in its place, and a pause after it long enough
  to pause the reading by hand. That last part touches the feature of pausing and resuming, which is to be a unit of
  its own.
- **Now:** a code block, fenced with backticks or tildes or indented, is read as "code example", followed by two
  seconds of silence. Neither its fence, nor its language, nor its code is read. A file of nothing but a code block
  reads "code example", and is not a file with nothing to read.

### Quotes

- **Heard in unit 0006:** read aloud as text; the `>` is not read.
- **Wanted:** nothing; fine as it is.
- **Now:** the blocks of a quote are read as any others, without the `>`, with half a second between them.

### Tables

- **Heard in unit 0006:** the pipes are not read; the words of the cells are read one after the other, as one
  sentence.
- **Wanted:** a word such as "see table" in place of the table, since a table cannot be read aloud well.
- **Now:** a table is read as "see table". Neither its cells nor its separator line is read.

### Horizontal rules

- **Heard in unit 0006:** the voice stutters where the `---` is. The rule, between its blank lines, reaches the engine
  as a piece of its own, and the engine gives a piece with nothing to say either as a short noise, which is the
  stutter, or as half a second of samples that are not a number, which AloudRite plays as silence.
- **Wanted:** the rule left out.
- **Now:** a rule, `---`, `***` or `___`, is not read. Half a second of silence takes its place, the same as between two
  blocks, and between two paragraphs it gives that half second once.

### Emoji and symbols

- **Heard in unit 0006:** characters above U+FFFF, among them most emoji, are left out, with no pause. Emoji and
  symbols of three bytes do reach the engine: ⚓ is read as "anchor", ✓ is silent, ❤ is read as "red heart".
- **Open:** whether all emoji and symbols are made alike, and how. Unit 0014 is to have the characters above U+FFFF
  read once a release of sherpa-onnx can hand them over intact.
- **Now:** as in unit 0006. A character above U+FFFF is left out as a space after the Markdown is parsed, so that four
  emoji at the start of a line do not make an indented code block.

### Escaped characters

- **Heard in unit 0006:** `\*` and `\#` are read with the backslash, as "backslash star" and "backslash hash".
- **Wanted:** at least the backslash left out.
- **Decided:** the escaped character itself is part of the text, which the author escaped to have it shown as itself.
- **Now:** the backslash is not read, and the character after it is handed to the engine as it is. An entity, such as
  `&amp;`, is handed over as the character it stands for.

### Front matter

- **Now:** a block that begins with a line of `---` as the first line of the file and ends with the next line of `---`
  or `...` is not read, both lines included, whatever stands between them, and leaves no pause. A `---` on the first
  line without such an end is not front matter, and is read as the rest of this document says, as a rule or as the
  line under a heading. A file of nothing but front matter reads nothing. Front matter of another form, such as TOML
  between two lines of `+++`, is handed to the engine as it is.

### HTML comments

- **Now:** a comment, `<!-- ... -->`, is not read, neither its markers nor what is in it, whether it stands on lines of
  its own or within a sentence, and it leaves no pause. What counts as a comment is what CommonMark says: a comment
  that is not closed, at the start of a line or in a block of HTML such as `<div>`, leaves out what follows it, up to
  the `-->` of a later piece of HTML or else the rest of the file, and one that is not closed within a sentence is
  read as text, as a rendered file shows them. `<!--` and `-->` within a quoted attribute of other HTML are taken for
  a comment as well. In inline code and in a code block, a comment is code. A
  file of nothing but comments reads nothing. HTML other than comments is handed to the engine as it is.

## Beyond Markdown

### Paragraphs

- **Heard in unit 0006:** no longer pause between two paragraphs. The next paragraph starts straight after the last
  sentence of the one before, which feels unnatural.
- **Wanted:** a slightly longer pause between paragraphs. The plugin already cuts the text at every blank line, so it
  knows where a paragraph ends.
- **Now:** half a second of silence between two paragraphs, beyond the pause at the end of the sentence, and the same
  between any two blocks that follow each other: headings, paragraphs, lists, quotes, tables, rules and code blocks. The
  items of a list, and the blocks within an item, have a quarter of a second between them instead. A line break within a
  paragraph is handed to the engine as a line break, as before.

### Pauses at punctuation

- **Heard in unit 0006:** a long sentence is made in parts, cut after a comma, a colon or a semicolon, so that a
  reading never waits for the engine. The pauses at those cuts sound okay.

## Heard after unit 0010

What the developer hears in `samples/markdown-features.md` on Linux x64 and on macOS on Apple Silicon is written
here.

- **Linux x64, October 2026, in the sandbox IDE:** it "sounded very good". The one thing that annoyed slightly was the
  pause between the items of a list, then only the end of a sentence: too short, "it still feels a bit like a
  sentence". A quarter of a second of silence was added between two items after this. Listened to again with that
  quarter of a second, the whole file was "perfect".
- **macOS on Apple Silicon, October 2026:** "the text on macOS was good. No comments."
