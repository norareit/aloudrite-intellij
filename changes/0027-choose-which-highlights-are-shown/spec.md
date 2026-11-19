# Choose which highlights are shown

## Context

Unit 0025 highlights the sentence being heard lightly and the estimated word being heard strongly in the text editor,
and unit 0026 brings the same two highlights to the preview of a Markdown file. Both are switched off and on together,
in every view, by the one checkbox "Highlight the words being read" of requirement 15 of unit 0025.

While watching a reading in the sandbox IDE during the work on unit 0025, the human saw that the strong highlight was
not always on the word being spoken, and that it sometimes skipped a word. That follows from how the word is found:
the engine gives no times for words, so the word is estimated from the share of the audio of the piece heard, and
the voice does not keep the even pace the estimate assumes. The sentence, in contrast, is known exactly. A better
estimate would narrow the error but not remove it, as unit 0025 records.

The human therefore asked to choose the highlights one by one: the light highlight of the sentence and the strong
highlight of the word, each on or off, and, separately, whether the text editor and the preview show highlights at
all. This unit replaces the single checkbox with those four choices. It is built after unit 0026, since the choice of
the preview has nothing to act on before that unit.

## Requirements

1. Settings › Tools › AloudRite holds four checkboxes in place of the checkbox "Highlight the words being read" of
   unit 0025, in this order:
    * "Highlight in the text editor"
    * "Highlight in the preview"
    * "Highlight the sentence being read"
    * "Highlight the word being read"

   The page no longer holds the checkbox "Highlight the words being read".
2. "Highlight in the text editor", "Highlight in the preview" and "Highlight the sentence being read" are checked
   until the user clears them. "Highlight the word being read" is cleared until the user checks it. One choice of each
   holds for the whole IDE, in every project, and each is kept when the IDE is closed and opened again.
3. With "Highlight in the text editor" cleared, no text editor shows the highlights of requirement 1 of unit 0025, and
   none scrolls by itself by requirement 12 of unit 0025. With "Highlight in the preview" cleared, no preview shows the
   highlights of requirement 1 of unit 0026, and none scrolls by itself by requirement 8 of unit 0026. Each of the two
   leaves the other view as it is.
4. With "Highlight the sentence being read" cleared, no view shows the light highlight of the sentence. With
   "Highlight the word being read" cleared, no view shows the strong highlight of the word. Each of the two leaves the
   other highlight as it is, in every view that "Highlight in the text editor" and "Highlight in the preview" let show
   highlights.
5. A view that shows the sentence and not the word scrolls by itself to keep the highlighted sentence in view, and
   stops and starts doing so at the moments requirement 12 of unit 0025 and requirement 8 of unit 0026 give. A view
   that shows neither the sentence nor the word does not scroll by itself.
6. While both "Highlight in the text editor" and "Highlight in the preview" are cleared, "Highlight the sentence being
   read" and "Highlight the word being read" are shown greyed out, with their check marks as they were, and cannot be
   changed. When either of the first two is checked again, the two can be changed again, and their check marks hold as
   they were before they were greyed out.
7. A change of any of the four during a reading takes effect within half a second: a highlight that a cleared box
   turns off is gone from every view it no longer belongs in, and a highlight that a checked box turns on is shown from
   the next update on. The reading itself goes on as it was.
8. Automated tests cover requirements 2 to 4, 6 and 7, with the stand-ins of unit 0006 for the engine and the player,
   in the text editor and in the messages the IDE sends to the page of the preview. They need no model, no audio
   device and no network. The settings page is tested for the four boxes, their order, and their greying out of
   requirement 6.
9. The implementer looks at the settings page in the sandbox IDE as `AGENTS.md` says, in a light and a dark theme,
   with all four boxes checked and with the first two cleared, and at a reading with only the sentence highlighted in
   the text editor. The record holds what the captures showed.
10. `README.md` describes the four checkboxes, what each turns off, the scrolling by the sentence when the word is not
    shown, and the greying out, in place of its description of the checkbox "Highlight the words being read".

## Decisions

- **Four checkboxes, as two pairs.** The human asked for one choice of the view and one of the highlight, rather than
  a sentence and a word for each view. Four boxes cover what a reader wants: the sentence alone where the estimated
  word distracts, and the highlights in only the view they read in. A sentence and a word for each view separately
  would take four boxes for the highlights alone, for a combination nobody asked for, and was rejected.
- **The views first.** The human suggested putting the choices of the view above those of the highlight, since the
  latter apply only within the views the former let show highlights.
- **Greyed out, not hidden, and kept.** The human asked for the choices of the highlight to be greyed out while no
  view shows highlights. Greying out rather than hiding keeps the page from changing shape. Keeping their check marks
  means that checking a view again brings back the highlights the user chose, rather than resetting them. The keeping
  was decided by the specifier and is open to the human.
- **The word off until checked, the rest on.** The human decided that both views and the sentence start checked and
  the word starts cleared. The sentence is known exactly and is worth showing to every user. The word is an estimate
  that, in the human's check of unit 0025 at 1.0×, skipped short or quickly spoken words such as "and", "the" and
  "once", and a highlight that is often wrong is better offered than imposed. A user who changes nothing therefore
  sees the sentence in both views and no word, and follows the reading by the sentence as requirement 5 says. Should
  a later unit make the estimate of the word good enough, that unit may change its default.
- **The sentence is followed when the word is not shown.** Unit 0025 scrolls to keep the word in view. With the word
  turned off, the reader still follows the sentence, and a view that stopped scrolling would lose the reading at the
  bottom of the screen. Decided by the specifier and open to the human.
- **Both highlights cleared is allowed.** With both "Highlight the sentence being read" and "Highlight the word being
  read" cleared, the views show nothing, as with both views cleared. Preventing it would need a rule between the boxes
  that the human did not ask for, for a state that does no harm.
- **The wording of the boxes.** "Highlight in the text editor" and "Highlight in the preview" name the views as the
  IDE's Markdown editor names its modes. "Highlight the sentence being read" and "Highlight the word being read" follow
  the wording of the checkbox of unit 0025. Decided by the specifier and open to the human.
- **Checked on Linux only.** The page and the highlights are user interface of the IntelliJ Platform, with nothing
  that differs between the platforms, as `AGENTS.md` says.

## Non-goals

- A better estimate of the word, by vowels, by syllables or by times of the engine.
- A choice of the highlights for each project, or for each file.
- A key or an action that turns the highlights off and on during a reading.
- A page under Settings › Editor › Color Scheme for the colours of the highlights, as unit 0025 leaves out.

## Beyond the request

1. Carrying over a cleared checkbox "Highlight the words being read" of unit 0025: a user who cleared it would find all
   four of the new boxes cleared, rather than the views and the sentence checked as requirement 2 starts them. It
   costs reading the old setting once and a test. Without it, such a user sees the sentence again after the update
   and clears the boxes they want off, once.
