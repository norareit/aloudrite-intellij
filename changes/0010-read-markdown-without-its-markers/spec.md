# Read Markdown without its markers

## Context

Unit 0006 makes the "Read Aloud" button read a Markdown file aloud. It hands the text to the speech engine nearly as it
is in the file: only characters above U+FFFF, among them most emoji, are left out as a space, because the engine's Java
binding cannot hand them over intact. Everything else reaches the engine, the Markdown markers included. Unit 0006
decided so on purpose, so that it answers one question, whether pressing the button reads the file, and it had the
developer listen to how the markers sound, because that is what this unit starts from.

The developer listened on Linux x64 in October 2026, to `samples/markdown-features.md`, a file that unit 0006 adds and
that holds every kind of Markdown in a short section of its own, followed by two long chapters of prose. What was heard,
and what the developer wants instead, is in `docs/reading-markdown.md`, which unit 0006 adds as well. In short:

- Markers are read out where a listener expects silence: the `#` of a heading as "hashtag", the asterisks of bold and
  italic as "asterisk asterisk", the tildes of strikethrough, the `!` and file name of an image, the backslash of an
  escaped character, the whole address of a link, and the label and definition of a reference link.
- Some structures run together into one long sentence: the items of a bulleted, nested or task list, the cells of a
  table, the code of a code block, and code in backticks inside a sentence. The items of a numbered list do as well
  when they do not end with a full stop: in the sample every numbered item ends with one, which gave the pause that
  sounded good there, and in a spec of the developer's own, whose numbered items had no full stops, they ran together
  and the reading was "very awkward".
- A horizontal rule makes the voice stutter.
- Two paragraphs follow each other without a longer pause than between two sentences.
- Some kinds already sound right and are to stay as they are: the numbers of a numbered list, quotes, bold and
  italic written with underscores, the backticks of inline code, and the pipes of a table, none of which is read out
  as a marker.

This unit makes the plugin hand the engine what a listener should hear: the text without its markers, with words in
place of what cannot be read aloud well, and with pauses where the structure of the document asks for them.

It needs unit 0006 to be implemented. At the time of writing, unit 0006 is built on its branch and not yet on
`master`, and `samples/markdown-features.md` and `docs/reading-markdown.md` exist only there.

## Requirements

1. The `#` markers of a heading are not read, at any level. The text of the heading is read.
2. The asterisks of bold, italic, and bold and italic together, `**word**`, `*word*` and `***word***`, are not read.
   The words between them are read. Bold and italic written with underscores sound as they do in unit 0006.
3. Struck out text, `~~words~~`, is not read: neither its tildes nor its words. A file whose only words are struck
   out reads nothing, as requirement 19 says of such a file.
4. The marker of each item of a bulleted list, `-`, `*` or `+`, is not read, at any level of nesting. The number of
   an item of a numbered list is read as in unit 0006. Every item of a list, bulleted, numbered, nested or task, ends
   with the pause and the fall of the voice at the end of a sentence, so that the items do not run together. It comes
   once: an item that ends with `.`, `!` or `?` sounds as it does in unit 0006, with no pause beyond the one its own
   punctuation gives.
5. In a task list, the box of an item, `[ ]` or `[x]`, is not read, and neither is its state. Between two items there
   is the pause of requirement 4.
6. Of a link, `[text](address)`, only the text is read. The address is not read.
7. A bare address, such as `https://example.com/page`, and an address in angle brackets, such as
   `<https://example.com/page>`, are read as the word "link".
8. Of a reference link, `[text][label]`, only the text is read. The label is not read, and neither is the line that
   defines it, `[label]: address`.
9. Of an image, `![alternative text](file)`, only the alternative text is read. An image without alternative text is
   not read at all.
10. Code in backticks within a sentence is read as the engine reads it, with a pause before and after it.
11. A code block, fenced with backticks or tildes or indented, is not read: neither its fence, nor its language, nor
    its code. The words "code example" are read in its place, followed by a pause of two seconds.
12. A table is not read: neither its cells nor its separator line. The words "see table" are read in its place.
13. A horizontal rule, `---`, `***` or `___` on a line of its own, is not read. There is a pause in its place, as long
    as the pause between two paragraphs.
14. The backslash of an escaped character, such as `\*` or `\#`, is not read. The character after it is handed to the
    engine as it is.
15. Front matter is not read: a block that begins with a line of `---` as the first line of the file and ends with the
    next line of `---` or `...`, both lines included. There is no pause in its place, and the reading begins with what
    follows it. A `---` on the first line without such an end is not front matter, and is read as the rest of this
    unit says. A file of nothing but front matter reads nothing.
16. An HTML comment, `<!-- ... -->`, is not read, neither its markers nor what is in it, whether it stands on lines of
    its own or within a sentence. There is no pause in its place. What counts as a comment, also one that is not
    closed, is what CommonMark says, so that what is not read is what a rendered file does not show. Inside inline
    code or a code block, `<!--` is code and is handled as requirements 10 and 11 say. A file of nothing but comments
    reads nothing.
17. Between two paragraphs there is a pause clearly longer than the one at the end of a sentence.
18. Quotes sound as they do in unit 0006.
19. Everything that unit 0006 established still holds: what is read is the text of the file as it was at the press,
    unsaved changes included; a reading starts before the whole file is made into audio; the stop works within a
    second; a file that would read nothing shows the notification that there is nothing to read. A file that holds
    nothing but Markdown that is not read, such as a single code block, counts as one that reads "code example" and
    not as empty.
20. The file in the editor is not changed. Only what is handed to the engine is.
21. Automated tests cover requirements 1 to 18 with a stand-in for the engine: for each kind of Markdown, the text that
    reaches the engine, and where a pause is required, that it is there. They need no model, no audio device and no
    network.
22. The developer has listened to `samples/markdown-features.md` on Linux x64 and on macOS on Apple Silicon, and the
    record holds what they found for each section, against `docs/reading-markdown.md`.
23. `docs/reading-markdown.md` describes what the plugin does after this unit, for each kind of Markdown, and
    `README.md` no longer says that the Markdown markers are read.

## Decisions

- **The plugin reads the structure of the Markdown, not only its characters.** Leaving out a `#` or a `*` wherever it
  stands would also take them out of prose and code where they mean something, and the pauses of lists, code and
  paragraphs need to know where those begin and end. So the text is read as Markdown, with its blocks and inline
  elements, before it is handed to the engine. How it is parsed is for the plan, within the decision of unit 0003 that
  the plugin needs no class of the IDE's Markdown plugin, which may be absent, and within the license of the plugin.
- **A pause is silence that the plugin adds, not punctuation.** The engine has no instruction for a pause. A comma or
  a full stop would make one, but it changes the melody of the sentence around it, and a comma around inline code
  gives it the sound of an aside. Unit 0006 already makes the text in separate pieces, so a piece can be followed by
  silence of a set length. Rejected: punctuation, for the melody.
- **The lengths of the pauses.** Between two paragraphs, and in place of a horizontal rule, half a second of silence
  beyond the end of the sentence. Around inline code, a quarter of a second. After "code example", two seconds, which
  the developer asked for as long enough to pause the reading by hand. Between two items of a list, the pause at the
  end of a sentence, which the developer found good in the numbered items of the sample, all of which end with a full
  stop. Decided by the specifier and open to the human: the lengths are guesses until they are heard, and the human
  may set others.
- **An item of a list without its own end is ended as a sentence.** An item that does not end with `.`, `!` or `?`
  is handed to the engine as if it ended with a full stop, which the file does not show; an item that does end with
  one is handed over as it is. So every item ends alike, with the pause and the fall of the voice of the end of a
  sentence, and an item with its own punctuation cannot get a second pause, which the developer asked to avoid. This
  is the one place where the plugin makes a pause with punctuation, against the decision before: the pause wanted
  here is that of the end of a sentence, which only the engine makes, and silence added by the plugin would differ
  in length from it, item by item, between items with punctuation and items without. Rejected: silence after an item
  without its own end, for that difference; and silence after every item, which gives the punctuated ones the double
  pause. Decided by the specifier and open to the human.
- **Words in place of what cannot be read aloud well, as the developer asked:** "code example" for a code block and
  "see table" for a table. Rejected for a table: reading its cells row by row, which is long and hard to follow by
  ear.
- **A bare address is read as "link".** Its text is the address itself, and reading an address out is what the
  developer found ridiculous. Rejected: reading only its domain, which is shorter but still a string of words that
  are not meant to be heard. Decided by the specifier and open to the human, who asked for a short word or the domain.
- **The struck out words are not read.** The human chose this. Strikethrough marks words as no longer meant, and it
  is often written next to the words that replace them, as in "the ~~old~~ new value", where the author keeps the old
  words to show the change. Reading both makes a sentence that the author never meant, and a listener cannot hear
  which words were struck out. Rejected: reading the struck out words, which the specifier had proposed, because
  leaving them out could make a sentence read as something it does not say, which the human found the lesser
  problem; and saying "struck out" before them, which keeps the words but adds a word of the plugin's own.
- **The state of a task is not said.** Saying "done" or "not done" for each item adds a word to every line of a list
  that the eye takes in at once. Rejected: saying it. Decided by the specifier and open to the human, who left it open.
- **Code is read as the engine reads it.** Naming its symbols, as "greater than or equal" for `>=`, is a dictionary of
  its own for every language. This unit only sets inline code apart by its pauses. See "Beyond the request".
- **An escaped character is handed to the engine.** The author escaped it to have it shown as itself, so it is part
  of the text; the engine then does with it what it does with that character elsewhere. Decided by the specifier and
  open to the human, who left it open.
- **Front matter is not read.** The human took it in from "Beyond the request". It is data about the file, such as
  its title, date and tags, for the tools that build a site, and a rendered file does not show it. Read aloud, a file
  that has it starts with its keys and values. Only YAML between two `---` lines at the very top counts, the form
  that most documentation sites use; a `---` with no end is left to the other rules, so that a file that begins with
  a horizontal rule is not swallowed whole. Rejected: saying a word in its place, as
  for a code block, since the listener misses nothing by not hearing it.
- **HTML comments are not read.** The human took them in from "Beyond the request". A comment is a note for whoever
  edits the file, and a rendered file does not show it, so a listener should not hear it either. CommonMark decides
  what is a comment, as the rendered file does, so that the two agree also on a comment that is not closed. Rejected:
  saying a word in its place, since the listener misses nothing that a reader sees.
- **A file of nothing but Markdown that is not read is not empty.** A file with a single code block reads "code
  example"; the notification that there is nothing to read is for a file that would give the engine nothing.

## Non-goals

- Changing how emoji and symbols are read. This unit leaves characters above U+FFFF out as a space, as unit 0006
  decided, and hands emoji and symbols of three bytes, such as ⚓ and ❤, to the engine as they are. Unit 0014, a
  draft, has the characters above U+FFFF read once a release of sherpa-onnx can hand them over intact.
- Naming the symbols of code, or reading code in any way other than as the engine reads it.
- Reading a table's cells, or describing its size.
- Pausing and resuming a reading, which the developer wants as a unit of its own. The pause after "code example" is
  a stretch of silence, not a pause of the reading.
- Choosing the length of the pauses in a setting.
- HTML inside Markdown other than the comments of requirement 16, footnotes, math, front matter in another form
  than that of requirement 15, such as TOML between two `+++` lines, and other extensions beyond the kinds named in
  the requirements. They are handed to the engine as they are.
- Highlighting what is read, and reading from the caret or the selection.

## Beyond the request

- **Naming the symbols of code.** Inline code with operators would sound clearer, as "count greater than or equal to
  one". It costs a table of symbols and their names, and a decision for each language. Without it, code is read as
  the engine reads it, with braces silent and other symbols read out or not as the engine decides.
