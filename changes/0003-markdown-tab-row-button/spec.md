# Markdown tab row button

## Context

Unit 0001 left a plugin that builds, loads and adds one placeholder: "Say Hello" in the Tools menu, which shows a
dialog reading "Hello, world!". Its spec says that the first unit that adds real behavior removes that action.

This unit takes the next two steps toward reading Markdown aloud, and still makes no sound. The button moves to
where a reader will look for it, on the editor tab of a Markdown file, and what it shows is the content of that
file instead of a fixed greeting. That proves the two things every later unit stands on: the plugin knows which
file the user is looking at and can get its text.

The developer asked for the button in the small bar that switches between "Editor", "Editor and Preview" and
"Preview". In IntelliJ IDEA 2026.2.3 with the new UI, those three icons sit at the right end of the editor tab row,
next to the `⋮` menu. The three icons themselves are fixed in platform code and no plugin can add to them. The tab
row around them does take contributions from plugins, and a throwaway trial on 2026-10-08, built outside the
repository and looked at by the developer in a sandbox IDE, showed a contributed button directly beside the layout
icons, between them and the `⋮` menu.

The record of unit 0001 holds a concern that bears on this unit: in the test environment of IntelliJ IDEA 2026.2.3,
a test that opens a project fails on the IDE's own licensing startup activity, whatever the plugin does. The tests
of this unit have to pass regardless, and how they do is for the plan.

## Requirements

1. When the selected editor tab holds a Markdown file, the right end of the editor tab row shows a button of the
   plugin, next to the icons that switch between "Editor", "Editor and Preview" and "Preview".
2. The button is there in each of those three layouts.
3. When the selected editor tab holds a file that is not a Markdown file, the button is not shown.
4. The button's tooltip is "Read Aloud".
5. The button's icon suggests speech or sound, is not the triangle the IDE uses for Run, and is legible in both a
   light and a dark theme.
6. Pressing the button shows a dialog whose text is the content of the file in the selected tab, as the editor
   holds it at that moment: the raw Markdown, character for character, including changes that are not saved yet.
7. The title of the dialog is the name of the file.
8. For a file whose content does not fit on the screen, the dialog stays within the screen, its text scrolls, and
   all of the content can be reached by scrolling. The dialog can be closed whatever the length of the content.
9. For an empty file the dialog opens with no text.
10. The "Say Hello" action is gone: it is not in the Tools menu and the plugin registers no action of that name.
11. The plugin still requires nothing beyond the platform itself. In an IDE where the Markdown plugin is absent or
    disabled, the plugin loads without an error and shows no button.
12. Automated tests cover requirements 3, 6, 9 and 10, and that the button is offered for a Markdown file. They
    include a file with a change that is not saved. They run with the documented test command and pass.
13. `README.md` describes what the plugin does after this unit, and no longer describes the "Say Hello" action.

## Decisions

- **The button is in the editor tab row, beside the layout icons.** It is the place the developer asked for, as
  near as the platform allows. The tab row belongs to the whole tab and not to the text editor inside it, so the
  button is also there when only the preview is shown. Putting it among the three layout icons was rejected
  because the platform builds that set in code and offers no supported way in, and reaching into the Markdown
  plugin's internals to force it would break with IDE updates. The corner of the text editor, where the problems
  indicator sits, was rejected because it disappears in the "Preview" layout. A floating toolbar of the plugin's
  own was rejected for the same reason, and because it shows only while the mouse is over the editor. The main
  toolbar, next to Run and Debug, was rejected because it is not tied to a file and a play-like button there reads
  as running the program.
- **Which side of the layout icons the button lands on is not a requirement.** The trial showed it between the
  layout icons and the `⋮` menu. That order is the platform's, and a requirement that fixed it would fail when the
  platform changes it, with nothing the plugin could do.
- **The icon is not a play triangle.** The developer first asked for a play-like icon, then saw the trial, which
  borrowed the IDE's Run icon, and judged that it would be confused with running the program. An icon that suggests
  speech or sound says what the button is for. Which icon, and whether it is one the platform ships or one drawn
  for the plugin, is for the plan.
- **The tooltip says "Read Aloud" although nothing is read aloud yet.** This is the button that will read the file
  aloud, and it is placed and named now so that the next unit changes what it does and not what it is called. The
  dialog is the placeholder in this unit, as the greeting was in unit 0001. A tooltip that describes the dialog was
  rejected because it would be renamed in the very next unit.
- **The content is what the editor holds, not what is on disk.** A reader presses the button about the text in
  front of them, and that includes what they just typed. Reading the file from disk was rejected because it would
  show stale text for a file with changes that are not saved, with nothing to tell the user why.
- **The content is shown raw.** Deciding what in a Markdown file is speakable is a unit of its own. Showing the
  text unaltered keeps this unit to one question, which is whether the plugin has the right file's text.
- **A Markdown file is a file the IDE treats as Markdown.** That follows the file type associations of the IDE,
  so a file the user has associated with Markdown counts and a `.md` file in an IDE without Markdown support does
  not. Going by the file name extension alone was rejected because it disagrees with the IDE in both of those
  cases, and the layout icons the button sits beside exist only where the IDE treats the file as Markdown.
- **The plugin does not come to require the Markdown plugin.** Unit 0001 requires that the plugin depends on
  nothing beyond the platform, so that it loads in any IDE built on it, and this unit keeps that. A required
  dependency was rejected because the plugin would then refuse to load where Markdown support is disabled, which
  is a harsher answer than showing no button.
- **The dialog shows the content of the selected tab only.** With several files open, or the editor split, the
  button in a tab row acts on the tab selected in that row.
- **An empty file still opens the dialog.** A button that does nothing when pressed looks broken. An empty dialog
  says truthfully that the file has no content.

## Non-goals

- Any sound: no speech, no audio, no model, no voices.
- Any processing of the Markdown: no stripping of markers, no skipping of code blocks, no use of the rendered
  preview.
- Highlighting or scrolling in the editor.
- Showing only the selected text.
- A button for files that are not Markdown.
- A keyboard shortcut, a menu entry or a context menu entry for the button.
- A settings page, or a way to hide or move the button.
- Translations of the tooltip.

## Beyond the request

- **A limit on how much text the dialog shows.** A Markdown file of many megabytes would make the dialog slow to
  open and to scroll. A limit costs a choice of the size, a notice in the dialog that the text is cut, and a test
  for it. Without it, a very large file gives a sluggish dialog that still closes normally, in a placeholder that
  the next unit replaces.
- **A menu entry or keyboard shortcut.** The button can only be pressed with the mouse. An entry costs a decision
  on where it goes and which keys it takes. Without it, the action is still reachable from the keyboard through
  the IDE's own action search, under the name "Read Aloud".
- **A test that drives a real IDE window.** As in unit 0001, it would show that the button is drawn in the tab row,
  at the cost of an additional test framework, a display and a run time of a minute or more. Without it, where the
  button is drawn is checked by hand in the sandbox IDE at the review, and the trial has already shown the place
  once.
