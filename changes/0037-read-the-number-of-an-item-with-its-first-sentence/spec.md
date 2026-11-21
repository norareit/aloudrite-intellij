# Read the number of an item with its first sentence

## Context

Unit 0006 has the number of an item of a numbered list read, and its successors count it from the first number of the
list and read it with its delimiter, `.` or `)`, as `docs/reading-markdown.md` says under "Numbered lists". The number
is handed to the engine as the number, its delimiter and a space, in front of the words of the item.

Listening to the voices of unit 0029, the human heard the British voices stumble after the number of an item, and
then heard the same with Heart, when reading the spec of unit 0032. The specifier found the cause in the plugin, on
`master` at `1aefa27`, by handing a numbered list through the plugin's own code and eSpeak NG:

- With the delimiter `.`, the plugin cuts the number off as a piece of its own, since `1. ` ends as a sentence does.
  The engine is handed `1. ` alone and says "one" as a whole sentence, with the fall of the voice at its end, and is
  then handed the words of the item as the next piece. Every number is so said on its own, apart from what follows.
- With the delimiter `)`, the number stays in the piece of the first sentence of the item, and eSpeak NG reads it
  straight into the first word, with no pause at all: `3) A heading like item` is "three a heading like item".

The human asked that the number be read as part of the first sentence of its item, as in "One, climb the stairs".

## Requirements

1. The number of an item of a numbered list is handed to the engine followed by a comma and a space, in place of its
   delimiter and the space, as `1, ` for `1. ` and for `1) `, in the same piece as the first sentence of the item, so
   that it is read as the start of that sentence, with the short pause of a comma after it.
2. The number is counted as it is now, from the first number of the list, at every level of nesting.
3. An item that has no words to read but its number, as one whose words are all struck out, reads its number alone,
   and it ends as a sentence does, with no comma before its end.
4. The highlights of the sentence and of the word, in the text editor and in the preview, show the number as they do
   now: the number is part of the first sentence of its item, and is highlighted where its digits and delimiter stand
   in the file.
5. Everything else about a numbered list is read as now: the end of each item as a sentence, and the silences between
   the items and around the list.
6. Automated tests cover requirements 1 to 4 for both delimiters: the text and the pieces handed to the engine, a list
   that starts at another number than 1, a nested numbered list, an item whose words are all struck out, and the
   places of the number for the highlights.
7. A test with the real data of eSpeak NG, skipped where the model is not in its folder as the other tests with the
   real engine are, finds that the piece of the first item of the numbered list of `samples/markdown-features.md` is
   made into one sentence of phonemes, in the language of an American and of a British voice.
8. `docs/reading-markdown.md`, under "Numbered lists", says what was heard and what is read now, in the form of its
   other entries.
9. The developer reads the numbered lists of `samples/markdown-features.md` on Linux x64 in Heart and in Emma, and the
   record holds whether the number is now heard as the start of its sentence and whether the stumble after it is gone.

## Decisions

- **A comma after the number.** A comma keeps the number in the sentence that follows, as it is said aloud, and gives
  it the short pause that a list item is heard with. Rejected: no mark at all, which runs the number into the first
  word as the delimiter `)` does now; a colon, which eSpeak NG also keeps in one sentence but which announces what
  follows rather than counting it; the number written as a word, which eSpeak NG already does for digits. Checked:
  eSpeak NG makes `1, Climb the stairs and unlock the lamp room.` one sentence, "wˈʌn, klˈaɪm…" in `en-us` and
  "wˈɒn, klˈaɪm…" in `en`. Decided by the specifier and open to the human.
- **The comma for both delimiters.** The delimiter is a mark of Markdown and is not heard, and the two cases now sound
  different, both wrongly, for the same thing. Decided by the specifier.
- **Kept as long as the delimiter.** The comma takes the place of the delimiter, one character for one, so the places
  that tie the text handed to the engine to the file stay as they are for the highlights. Decided by the specifier.
- **Checked by ear on Linux only.** The text handed to the engine is the same on both platforms, and the unit changes
  no native code, audio or download, as `AGENTS.md` says.
- **A feature.** Unit 0006 heard the pause at each number and judged it fine, so this changes a reading that was
  decided rather than fixing a fault of the code.

## Non-goals

- Reading the number as an ordinal, as "first", or leaving it out.
- The markers of a bulleted list, which are not read, and the silences between items.
- Numbers in the text of an item, which eSpeak NG reads as it does now.
- The stumbles of the British voices elsewhere, as before a short sentence such as "by hand", which unit 0029
  recorded.

## Beyond the request

None.
