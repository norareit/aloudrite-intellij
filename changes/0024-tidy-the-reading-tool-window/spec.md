# Tidy the reading tool window

## Context

The tool window "AloudRite" of unit 0015 has grown with each unit since: the part of the file of unit 0013, the
speed of unit 0016, the model of unit 0020 and the times and the bar of unit 0022. Each was added as one more row
under the last, so the pane is now a single column of up to eleven lines, every one 8 pixels from the next. The human
finds it cramped and clumsy since the times were added, and not professional, without knowing why.

Asked by the human, the specifier looked at a capture of the pane and found these causes:

* There is no hierarchy. The labels "Reading speed" and "Speech model" are headings, but they have the font, size and
  colour of the values under them, and every row has the same gap, so the eye sees one list rather than three groups:
  the reading, the speed and the model.
* The bar of progress has its own preferred width and follows text of a varying width, so its left edge moves as the
  times change and it lines up with nothing.
* The line `~4:02 left of ~4:11` carries two marks and two numbers in a short line, which reads as code rather than as
  a sentence.
* The grey text under the speed is two lines high, always shown, and the largest block of the pane, for a remark about
  an edge case.
* The "Pause" and "Stop" buttons are two large text buttons whose borders start a few pixels to the right of the
  labels, as does the icon of the minus button, so the left edge is ragged. The pane mixes three kinds of control.

The human took all of the specifier's suggestions: groups with titled separators, the bar across the pane with the
times on a line under it, "Pause" and "Stop" as icons in the title bar of the tool window, the remark on the speed as
the tooltip of its field, the model as a quieter line with a link to its settings, and one left edge.

## Requirements

1. The pane holds three groups, in this order: the reading, the speed and the model. The speed is under a titled
   separator of the IDE that reads "Reading speed", and the model under one that reads "Speech model": the IDE's
   title of a group, followed by a line to the right edge of the pane. The reading has no title. The rows within a
   group are closer to each other than the last row of a group is to the separator of the next.
2. "Pause", "Resume" and "Stop" are icon buttons in the title bar of the tool window, beside the IDE's own buttons
   of the tool window, and the pane holds no text buttons for them. The first shows the IDE's pause icon while a file
   is read and its resume icon while the reading is paused, and the second shows the IDE's stop icon. Their tooltips
   are "Pause Reading", "Resume Reading" and "Stop Reading", each with the shortcut of the action of the keymap in use,
   as the buttons' tooltips have now. A press does what a press on the text button of the same name does now, in the
   tool window of every project.
3. While nothing is read, both icon buttons are shown greyed out and do nothing on a press, and the first shows the
   pause icon.
4. While a file is read, the reading group shows, from the top: the name of the file, as `Reading <name>` or
   `Paused reading <name>` as now, the part of the file and the project when they are shown now, the bar of progress,
   and a line of the times. The bar spans the width of the pane, from its left edge to its right edge less the border
   of the pane, at any width of the pane.
5. The line of the times shows the time left at its left, as `About 4:02 left`, and the time of the whole reading at
   its right edge, as `4:11 in all`, in the formats of unit 0022 and without `~`. Both are in the smaller font the IDE
   uses for the comments under a control, in its grey of a comment. Both have the tooltip of the times of unit 0022.
6. The row of the speed under its separator holds the minus button, the field and the plus button, as now. The pane
   no longer shows the grey text under the row, and the field's tooltip is that text, "A change starts the sentence
   that is playing again at the new speed. This can take a few seconds."
7. Under its separator, the model group shows the name of the model in use on the left of its row, wrapping at the
   width of the pane as now, and a link "Settings…" at the right edge of the row, which opens the page "AloudRite"
   under Settings › Tools.
8. The left edge of the text of every label, of the title of each separator, of the bar and of the icon of the minus
   button are at the same distance from the left edge of the pane, within one pixel at the IDE's default scale.
9. While nothing is read, the reading group shows only the text that says so, which is the text of now.
10. Everything the tool window does that requirements 1 to 9 do not change stays as `README.md` describes it: what is
    shown when, what a press does, how the times and the bar move, the speed field and its error mark, and that every
    project's tool window shows the one reading of the IDE.
11. The automated tests of the pane cover requirements 2, 3, 5, 6 and 7: the state, icon and tooltip of each title
    bar button while nothing is read, while a file is read and while it is paused, and that a press pauses, resumes and
    stops; the text of the line of the times; the tooltip of the speed field; and that the link opens the settings
    page. They need no model, no audio device and no network.
12. Before the unit is handed to the developer, the implementer has looked at captures of the sandbox IDE's window,
    taken as `AGENTS.md` says, while nothing is read, while a file is read and while it is paused, at the width the
    tool window opens with and at a narrow one where the model's name wraps, in the IDE's light and dark themes, and
    the record says what they showed of requirements 1, 4, 5, 7 and 8.
13. The developer has looked at the tool window on Linux x64 while nothing is read, while a file is read and while it
    is paused, and the record holds what they found.
14. `README.md` describes the tool window as it is after the unit.

## Decisions

- **Titled separators, the IDE's own.** A separator with a title is how the settings pages and tool windows of the
  IDE group their controls, so the pane looks like the rest of the IDE rather than like a form of its own. Decided by
  the specifier and taken by the human. Rejected: bold labels as headings without a line, which give the groups a
  title but still let them run into each other; boxes with a border, which the IDE's new interface no longer uses.
- **No title over the reading.** It is the first thing in the pane and the reason it exists, and its first line, the
  name of the file, already says what it is. Decided by the specifier and open to the human.
- **Pause and stop in the title bar.** It is where the Run and Debug tool windows keep the controls of what runs, and
  it frees a row of two large buttons, the heaviest controls of the pane. The buttons are there also while nothing is
  read, greyed out, so that the title bar does not change its layout when a reading starts and the user learns where
  they are. The human chose icons. Rejected: an icon toolbar beside the name of the file, which still takes room from
  the name in a narrow pane and is gone while nothing is read; the text buttons, aligned, which keeps the weight.
- **The bar across the pane, the times under it.** A media player shows its bar this way, with the time on either
  side under it. A bar of the full width lines up with the pane's edges, keeps its place while the times change, and
  is easy to read at a glance. Decided by the specifier and taken by the human.
- **One `About` instead of two `~`.** Unit 0022 marked both times as estimates with `~`, which the developer asked for
  after their reading. The line `About 4:02 left` and `4:11 in all` keeps them marked, with the word at the start of
  the line and the tooltip on both, and reads as words rather than as code. The time of the whole is the one that
  moves most as the estimate gets better, and it carries no mark of its own: the line reads as one statement, and a
  second `about` beside the first adds noise. This changes the text that unit 0022 decided on, which is frozen, so it
  is decided here. Decided by the specifier and open to the human. Rejected: `About 4:02 left` and `of about 4:11`,
  for the repeated word; keeping the `~`, which is the noise the human found.
- **The times in the grey of a comment.** They are information about the reading and not a control, and a smaller,
  grey line under the bar lets the name of the file and the bar lead. Decided by the specifier and open to the human.
- **The remark on the speed as the field's tooltip.** The human chose it. It explains why a change can take a few
  seconds, which matters at the moment of a change and not the rest of the time. Rejected: a help icon beside the
  field, which adds a fourth control to the row; showing the text for a few seconds after a change, which moves the
  pane while the user looks at it.
- **A link to the settings of the model.** The model is chosen only on the settings page, and the tool window said
  so only in `README.md`. The link makes the line of the model lead somewhere and says where it is changed. Decided by
  the specifier and taken by the human.
- **Checked on Linux only.** The pane is user interface of the IntelliJ Platform, which behaves the same on macOS, as
  `AGENTS.md` says. The two themes are checked because the separators and the grey text depend on the theme's
  colours.

## Non-goals

- A time already played, as `1:09 / 4:11`.
- A button that starts a reading from the tool window.
- Any change to the button on the tab of the file, to the shortcuts, or to the settings page.
- Choosing, downloading or removing the model from the tool window.
- Any change to how the times are estimated or how the bar moves.

## Beyond the request

1. The first button of the title bar starts a reading while nothing is read, of the Markdown file whose editor is
   selected, as the key "Read Aloud, Pause or Resume" does, rather than being greyed out. It costs an action of the
   title bar that follows the selected editor and a test of it. Without it, a reading is started from the tab of the
   file or with the key, as now, and the greyed buttons only show where pause and stop will be.
