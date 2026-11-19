# Highlight the words being read

## Context

The plugin reads a Markdown file aloud, but nothing on the screen shows where in the text the reading is. Someone who
listens and reads along at the same time has to find the place by ear, and finds it again after every glance away.

The human asked for highlighting in the style of karaoke: the word being spoken marked strongly, and the rest of its
sentence marked lightly.

What the plugin already has makes this possible without a new engine:

- Each character of the words that are read carries the offset in the file of the character it was read from, which
  unit 0013 introduced to read from the caret or the selection.
- Unit 0022 added the position being heard, as the piece that plays and the samples heard into it, counted from the
  position the audio line reports as played and not from what the engine has made.

The engine gives no timing of its own for words. sherpa-onnx 1.13.8, which the plugin uses, hands the plugin only the
samples of each piece. Kokoro computes the length of each sound internally, but the model file that sherpa-onnx runs
does not output it. The piece that plays is therefore known exactly, and the word within it can only be estimated.

The human reads mostly in the preview of a Markdown file. Drawing the highlight there works quite differently from
drawing it in the editor, so the human chose to split the work in two units: this unit, with the estimate, the
positions in the file and the highlight in the text editor, and unit 0026, with the highlight in the preview, built
right after it.

## Requirements

1. While a file is read, every text editor that shows the file in an open project shows two highlights. One is a
   light highlight of the sentence being heard. The other is a strong highlight of the word being heard, within it.
   The text editor of a Markdown file shown with its preview beside it counts as a text editor.
2. The sentence is a sentence as the plugin cuts the words that are read for a reading from the caret, without the cut
   at a comma, colon or semicolon. It covers the characters of the file that the words of that sentence were read
   from, from its first word to its last. The number of an item of an ordered list is part of the item's first
   sentence, as it is for a reading from the caret.
3. A word is a run of characters of the words that are read with no white space in it, and its highlight covers the
   characters of the file it was read from. A word that a selection cuts, and which is read whole, is highlighted
   whole.
4. Words that stand for something that is not read word by word are highlighted on what they stand for: "code
   example" on the whole code block, "see table" on the whole table, and "link" on the address it is read for. While
   they are heard, the strong highlight covers that whole source, and the light highlight covers the sentence they
   are part of.
5. The highlighted word is the estimate of the word being heard. Within the piece that plays, the share of the audio of
   the piece that has been heard chooses the word, as that share of the characters of the piece that are neither white
   space nor punctuation. The first word of a piece is highlighted when its audio begins, and its last word by the time
   its audio ends. Within one piece, the highlight only moves forward.
6. While the audio plays, the highlights are updated at least ten times a second.
7. During a silence the plugin adds, such as the half second between two paragraphs or the two seconds after "code
   example", the highlights stay as they were at the end of the piece before the silence.
8. While a reading is paused, the highlights stay where they were at the pause. At a resume, and at a change of the
   speed, the strong highlight goes back to the first word of the piece that plays again, with the audio. A resume that
   goes on at the piece after a silence highlights its first word.
9. When the reading ends, by itself, by a stop, by another reading, or by a failure of the speech, both highlights are
   gone from every editor of the file within half a second.
10. A text editor of the file that is opened during a reading shows the highlights from its next update on.
11. When the file is changed during a reading, the highlights stay on the characters they were made for, as the
    editor moves them with the text. A word whose characters were all deleted is not highlighted while it is heard,
    and the reading goes on as it does now.
12. The editor scrolls by itself to keep the highlighted word in view, without moving the caret or the selection. It
    stops doing so in an editor once the user scrolls that editor or moves its caret there. It does so again in that
    editor from the next resume or the next reading on.
13. The highlights do not hide the selection, the caret or the text. They are drawn below the selection, and their
    colours come from the colour scheme in use, so that they suit a light and a dark theme.
14. Nothing the plugin highlights changes the file, its undo history or the editor's own highlighting once the
    reading ends.
15. Automated tests cover requirements 2 to 5, 7 and 8 on plain values. They cover requirements 1, 9, 10 and 11 in the
    editor with the stand-ins of unit 0006 for the engine and the player, and they need no model, no audio device and
    no network.
16. The implementer looks at the highlights in the sandbox IDE as `AGENTS.md` says, in a light and a dark theme, and
    the record holds what the captures showed.
17. The developer reads `samples/markdown-features.md` on Linux x64 with the text editor in view, and the record holds
    how well the strong highlight kept up with the voice, at 1.0× and at 1.5×.
18. `README.md` describes the highlights, the estimate behind the word, the scrolling and when it stops.

## Decisions

- **The word is estimated from the share of the audio heard.** The engine gives no times for words, so only the piece
  is exact. Spreading the audio of a piece over its characters is what the bar of progress of unit 0022 already does
  for the share of the text read, and it needs nothing new from the engine. Leaving out white space and punctuation
  keeps the pauses at a comma from pushing the highlight ahead. Rejected: making each word a call of its own to the
  engine, which would know its time exactly but break the intonation of the sentence and slow the making of audio
  many times over; and a model exported to give the length of each sound, which means running the model outside
  sherpa-onnx or changing sherpa-onnx, a large job for a visual gain. Choosing by vowels or by syllables rather than by
  characters may follow the voice better. That is left for a later unit, once the developer's check of requirement 17
  shows whether the characters are good enough. Decided with the human, who chose the estimated word over the sentence
  alone and over true times for each word.
- **A light sentence and a strong word.** The human asked for it. The light highlight shows where the reading is at a
  glance, and the strong one follows the voice.
- **The sentence, not the piece.** A long sentence is cut into pieces at a comma, colon or semicolon only so that the
  engine makes its first audio sooner. That cut is not one the reader sees, so the light highlight covers the whole
  sentence, as a reading from the caret starts at the whole sentence. Decided by the specifier and open to the human.
- **The highlights stay through a silence.** If they went out for the half second between two paragraphs, they would
  flicker at every block. Decided by the specifier and open to the human.
- **The editor follows the reading until the user takes over.** The human asked for scrolling by itself. A view that
  keeps scrolling while the user scrolls away or works elsewhere in the file fights them, so the following stops at
  the first scroll or caret move of the user in that editor. It comes back at a resume or a new reading, which are the
  moments the user turns back to the reading. Decided with the human as to the scrolling. The moment it stops and
  comes back was decided by the specifier and is open to the human.
- **The colours are keys of the plugin's own, with their defaults from the colour scheme.** A fixed colour would
  suit one theme and not the other. Keys of the plugin's own, which fall back to keys of the colour scheme, follow the
  theme in use and leave room for a page to choose them later. Which keys they fall back to is chosen by the
  implementer from the captures of requirement 16, and named in the record. Decided by the specifier and open to the
  human.
- **The highlights follow the text as it is edited.** The reading reads the text of the press, as unit 0006 decided.
  The editor moves a highlight made with the IDE's own markers along with the text that is edited around it, so the
  highlights stay on the words that are heard as long as those words are still there.
- **Checked on Linux only.** The highlights are user interface of the IntelliJ Platform and arithmetic, with nothing
  that differs between the platforms, as `AGENTS.md` says.

## Non-goals

- The highlights in the preview of a Markdown file, which unit 0026 adds. In a preview shown alone, this unit shows
  nothing.
- Times of the engine for each word, or an estimate by vowels or syllables.
- Choosing to read from a place by clicking a word during a reading.
- Moving the caret or the selection with the reading.
- A page in Settings › Editor › Color Scheme for the colours of the highlights.

## Beyond the request

1. A setting under Settings › Tools › AloudRite that turns the highlights off. It costs a checkbox, a stored value and
   their tests. Without it, the highlights are always on while a file is read, and a user who does not want them has
   no way to turn them off.
2. A page under Settings › Editor › Color Scheme, as for a language, where the two colours can be chosen. It costs a
   page of the platform's kind, with a sample text, and its tests. Without it, the colours follow the colour scheme
   and cannot be changed by the user.
