# Reading shortcuts

## Approach

The four actions of the spec are actions of the IDE registered in `plugin.xml`, with their default shortcuts given
there in the keymaps of requirement 8. The keymap of the IDE then does all the binding, so that a shortcut the user
gives or removes in Settings › Keymap is the one that acts, and nothing in the plugin binds a key of its own (R8, R10).
What is new is in four places.

**The tab buttons are two of the four actions.** The two buttons on the editor tab are actions already,
`com.ritenoar.aloudrite.ReadAloud` and `com.ritenoar.aloudrite.StopReading`, and both are listed in Settings › Keymap
today, the second as "Stop Reading". A second action named "Stop Reading" would break requirement 1, and a separate
"Read Aloud" beside "Read Aloud, Pause or Resume" would leave the user two entries for nearly the same thing. So the
two registered actions become the keymap actions of the spec, keep their ids, so that a key a user may already have
given them stays, and act in two ways:

* Pressed on the tab, which is when the event comes from a toolbar (`ActionUiKind.Toolbar`), each does what it does
  now: "Read Aloud" acts on the file of its tab, and "Stop Reading" is shown on the tab of the file that is read. The
  texts and icons of the tab stay "Read Aloud", "Pause Reading", "Resume Reading" and "Stop Reading"
* Used in any other way, by its shortcut or from Find Action, each acts on the one reading of the IDE. "Read Aloud,
  Pause or Resume" takes the current reading as its target where there is one, also a paused one, and otherwise the
  file of the selected editor of the project of the event, which is the project of the IDE window that has the focus.
  From the target on, it does what the tab button does: a target that is read is paused or resumed, and any other is
  read, with the checks of the platform, of a file with nothing to read, and the question about the download. A
  selected file that is not Markdown, or no selected editor, ends the press with nothing done. "Stop Reading" stops
  the current reading and does nothing without one (R2, R3, R4, R5)

The template text of `ReadAloud` becomes "Read Aloud, Pause or Resume", which is the name in Settings › Keymap and in
Find Action, so the tab's "Read Aloud" becomes a constant of its own in place of the template text. Outside a
toolbar, `update` shows the template text and icon of each action, and enables "Read Aloud, Pause or Resume" wherever
the event has a project and "Stop Reading" while there is a reading. Whether the selected file is Markdown is decided
on the EDT in `actionPerformed`, so that `update` on the background thread does not ask the editor manager (R1, R2).

`Readings` gets `currentTarget()`, which `SpeechService` already has for `CurrentReading`. `ReadAloudAction` gets a
collaborator `selectedFile: (Project) -> VirtualFile?`, in the IDE `FileEditorManager.getInstance(project)
.selectedEditor?.file`, which a test replaces.

**Two new actions for the speed.** `ReadSlowerAction` and `ReadFasterAction`, ids `com.ritenoar.aloudrite.ReadSlower`
and `com.ritenoar.aloudrite.ReadFaster`, texts "Read Slower" and "Read Faster", in one file with a common base class
that takes `setting: () -> ReadingSpeedSetting`. They are enabled while a step that way is possible, as the buttons are
active, and act wherever the focus is, whether a file is read or not (R6). A press on "Slower" or "Faster" and a press
of their shortcut are one decision, a step after a wait, so `ReadingSpeedSetting` gets `slower()` and `faster()`,
which set the step below or above with `afterAWait = true`, and both the panel and the actions call them. The half
second of unit 0016 then holds for presses of a shortcut and of a button mixed, since both reach
`SpeechService.changeSpeed` the same way. At a limit the step is the speed itself, which `set` ignores (R6).

The state shown after an action follows from what exists: the reading publishes `READING_CHANGED` at a pause, a resume
and a stop, which the tool window follows; the setting publishes `READING_SPEED_CHANGED`, which the tool window and the
open settings page follow; and the toolbar of the tab updates its buttons after every action (R7).

**The shortcuts.** In `plugin.xml`, each of the four actions gets a `keyboard-shortcut` in the keymap `$default`,
from which "Default for XWin", "Default for GNOME" and "Default for KDE" derive, and one in `Mac OS X 10.5+`, the
keymap named "macOS", with the same keystroke: `ctrl alt shift SPACE`, `ctrl alt shift O`, `ctrl alt shift COMMA` and
`ctrl alt shift PERIOD`. The entries for macOS carry `replace-all="true"`, because the IDE otherwise also gives a
macOS keymap the shortcut of `$default` with Ctrl turned into ⌘, which requirement 8 does not have there (R8).

**The tooltips.** A new file `ShortcutTooltip.kt` holds the one place that makes a tooltip with a shortcut:
`shortcutTooltip(title: String, actionId: String?): HelpTooltip`, made as the IDE's toolbar buttons make theirs, with
`setPlainTextTitle(title)` and, where the action has a keyboard shortcut in the active keymap, `setShortcut` with
`KeymapUtil.getFirstKeyboardShortcutText(actionId)`. With no action or no shortcut it is the title alone (R11, R12).

* On the tab, the two actions put that tooltip into their presentation under `ActionButton.CUSTOM_HELP_TOOLTIP` at
  every update in a toolbar, which the button of the toolbar then shows in place of its own. "Read Aloud" passes no
  action id while another target is read, so that it names no shortcut there. The toolbar updates its buttons often,
  so a shortcut changed in Settings › Keymap shows at the next update (R11, R12)
* In the tool window, `ReadingPanel` installs the tooltips on its four buttons: "Pause Reading" or "Resume Reading"
  with the shortcut of `ReadAloud` on the first, "Stop Reading" with that of `StopReading` on "Stop", and "Slower" and
  "Faster" with those of `ReadSlower` and `ReadFaster`. The plain tooltips of the two icon buttons are removed, so
  that only the IDE's tooltip shows. The panel installs them again when it shows the reading, since the first button
  changes between pause and resume, and when the keymap changes, which it follows on `KeymapManagerListener.TOPIC`,
  for `activeKeymapChanged` and `shortcutsChanged`, in `followTheReading` as it follows the reading (R11, R12)

The tests read a tooltip through `HelpTooltip.getTooltipFor` on a panel's button and through the presentation's
`CUSTOM_HELP_TOOLTIP` on the tab: its title from `getTitle()`, and its shortcut from the text of the labels of
`createTipPanel()`, in a helper `titleAndShortcutOf(tooltip)` in `ButtonPressSupport.kt`. A shortcut the user gave is
made in a test by deriving a keymap from the active one, adding the shortcut, making it active through
`KeymapManagerEx`, and making the earlier one active again afterwards.

There is no end-to-end suite that drives an IDE window, as unit 0003 decided. Whether a key reaches the IDE at all is
up to the desktop, which no test sees, so the developer's checks of R14 are the end-to-end coverage. The unit does not
change the speech engine or the text handed to it, so the tests with the real engine are not required to run with the
model in place.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: `currentTarget()` in `Readings`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: the use by key and from Find Action, `selectedFile`,
  the constant "Read Aloud" and the tooltip on the tab
- `src/main/kotlin/com/ritenoar/aloudrite/StopReadingAction.kt`: the use by key and from Find Action, and the tooltip
  on the tab
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedActions.kt`: new, `ReadSlowerAction` and `ReadFasterAction`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedSetting.kt`: `slower()` and `faster()`
- `src/main/kotlin/com/ritenoar/aloudrite/ShortcutTooltip.kt`: new, `shortcutTooltip`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: `slower()` and `faster()` of the setting, the tooltips and
  the following of the keymap
- `src/main/resources/META-INF/plugin.xml`: the text of `ReadAloud`, the two new actions and the shortcuts
- `src/test/kotlin/com/ritenoar/aloudrite/ButtonPressSupport.kt`: the ids of the new actions, a press by key,
  `titleAndShortcutOf`, and keymaps with a shortcut of the user's
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingShortcutsTest.kt`: new, the actions by key, the keymaps and the
  tooltips on the tab
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: the template text of `ReadAloud`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedSettingTest.kt`: `slower()` and `faster()`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: the panel after an action, and its tooltips
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurableTest.kt`: the page after an action
- `README.md`: the shortcuts
- `changes/0017-reading-shortcut/developer-checks.md`: new, the checks of R14

## Tasks

### The actions by key

- [x] Add to `ButtonPressSupport.kt` the ids `com.ritenoar.aloudrite.ReadSlower` and `com.ritenoar.aloudrite.ReadFaster`
  and `pressTheKeyOf(action, project, file = null)`, which performs the action with an event of the place
  `ActionPlaces.KEYBOARD_SHORTCUT`, no toolbar, and a data context holding the project and, where given, a
  `VIRTUAL_FILE` that stands for a file selected in the project view; and a way to read whether an action is enabled
  for such an event (R13)
- [x] Write in `ReadingShortcutsTest`, with the stand-ins of unit 0006 and a `selectedFile` that returns the file a
  test chooses:
    - the plugin registers four actions with the texts "Read Aloud, Pause or Resume", "Stop Reading", "Read Slower"
      and "Read Faster", and the texts of the plugin's actions are all different, so that each name appears once in
      Settings › Keymap (R1, R13)
    - with nothing read, the key reads the selected Markdown file, also when the project view has another file
      selected; with no model in its folder it asks about the download as the tab button does; with a selected file
      that is not Markdown, and with no selected editor, it reads nothing and shows nothing (R2, R13)
    - while a file is read, the key pauses it with the selected file being the one read, another Markdown file, one
      that is not Markdown and none, and with the event from another project; while it is paused, the key resumes it
      from each of those (R3, R4, R13)
    - "Stop Reading" by key stops a reading and a paused reading from each of those places, and is disabled and does
      nothing with no reading (R5, R13)
    - on the tab, both buttons still do what they did: "Read Aloud" on another Markdown file during a reading reads
      that file, and "Stop Reading" is shown only on the tab of the file that is read; outside a toolbar
      "Read Aloud, Pause or Resume" and "Stop Reading" show their template texts (R1, R2)
    - after a pause, a resume and a stop by key, the tab button of the file that is read shows "Resume Reading",
      "Pause Reading" and "Read Aloud", and "Stop Reading" is shown and gone accordingly (R7, R13)
- [x] Add `currentTarget()` to `Readings` (R3, R4, R5)
- [x] Change `ReadAloudAction` and `StopReadingAction` to act as the approach says, with `selectedFile` in
  `ReadAloudAction` and the constant "Read Aloud" for the tab, and change the template text of `ReadAloud` in
  `plugin.xml` and the test of it in `ReadAloudActionTest` (R1, R2, R3, R4, R5)
- [x] Add to `ReadingPanelTest` that a pause, a resume and a stop by key show in the panel as a press on its buttons
  does (R7, R13)
- [x] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 2.

### The speed by key

- [x] Add to `ReadingSpeedSettingTest` that `slower()` and `faster()` hand the collaborator the step below and above
  with the wait, and hand it nothing at 0.75 and at 2.0 (R6, R13)
- [x] Add `slower()` and `faster()` to `ReadingSpeedSetting`, and have the "Slower" and "Faster" buttons of
  `ReadingPanel` call them in place of their own `set`, with the tests of the panel passing unchanged (R6)
- [x] Write in `ReadingShortcutsTest`:
    - "Read Slower" and "Read Faster" by key change the speed by one step with no reading, and during a reading make
      the piece again after the half second, at the new speed, with the event from any project (R6, R13)
    - at 0.75 "Read Slower" is disabled and does nothing, and at 2.0 "Read Faster" is (R6, R13)
    - a press on "Faster" in the panel and a press of the key of "Read Faster" 300 ms later make the piece once, at the
      speed of the second, and the same with key first and button second (R6, R13)
- [x] Add to `ReadingPanelTest` that the field and the limits of the buttons show the speed after each key, and to
  `ReadingSpeedConfigurableTest` that an open page shows it (R7, R13)
- [x] Write `ReadSlowerAction` and `ReadFasterAction` in `ReadingSpeedActions.kt`, and register them in `plugin.xml`
  with their ids and texts (R1, R6)
- [x] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 3.

### The shortcuts in the keymaps

- [x] Write in `ReadingShortcutsTest`: in each of "Default for XWin", "Default for GNOME", "Default for KDE" and
  "Mac OS X 10.5+", each of the four actions has exactly its shortcut of requirement 8 and no other, so that macOS
  has no ⌘ shortcut for them; and each of those shortcuts belongs to its action alone in that keymap (R8, R9, R13)
- [x] Add the shortcuts to `plugin.xml` as the approach says (R8)
- [x] Look up every keystroke of requirement 8 in the keymaps and plugin descriptors of the IntelliJ IDEA 2026.2.3
  distribution the build uses, including the bundled plugins the tests do not load, with a script in the scratchpad
  that reads every `keymaps/*.xml` and every `plugin.xml` and module descriptor in its jars and compares keystrokes
  with their modifiers in any order. Where one is taken, stop and log it for the specifier and the human (R9)
- [x] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 4.

### The tooltips

- [x] Add `titleAndShortcutOf(tooltip)` and the keymap with a shortcut of the user's to `ButtonPressSupport.kt` (R13)
- [x] Write in `ReadingShortcutsTest`, for the tooltip in the presentation of the tab buttons:
    - "Read Aloud", "Pause Reading" and "Resume Reading" name the shortcut of "Read Aloud, Pause or Resume", and
      "Stop Reading" that of "Stop Reading", as the text the keymap in use gives for the default keys (R11, R13)
    - on the tab of a Markdown file while another file is read, and while another file is paused, "Read Aloud" is the
      title alone (R11, R13)
    - with a shortcut the user gave, and with a second shortcut added after the default, the tooltip names the first;
      with the shortcuts of the action removed it is the title alone (R12, R13)
- [x] Write in `ReadingPanelTest`: "Pause" has "Pause Reading" and "Resume" has "Resume Reading", both with the
  shortcut of "Read Aloud, Pause or Resume"; "Stop" has "Stop Reading" with its shortcut; "Slower" and "Faster" have
  those of "Read Slower" and "Read Faster", and no plain tooltip of their own; a shortcut the user gave shows once the
  keymap changed, and a removed one leaves the title alone (R11, R12, R13)
- [x] Change `speedShownBy` in `ReadingPanelTest` to read the titles of the tooltips of "Slower" and "Faster" (R13)
- [x] Write `ShortcutTooltip.kt`, put the tooltip into the presentation of the two tab actions, and install the
  tooltips in `ReadingPanel` with the following of the keymap, as the approach says (R11, R12)
- [x] Run `./gradlew test` and see the whole suite pass (R13)
- [ ] Start the sandbox IDE with `./gradlew runIde`, capture only its window as `AGENTS.md` says, and look at the
  tooltips of the tab buttons while nothing is read, while a file is read and paused, and on the tab of another file;
  and at those of the four buttons of the tool window. Each shows its title with the shortcut in the IDE's grey, as
  the tooltips of the IDE's own toolbar buttons do, and the icon buttons show no second tooltip (R11)

Commit point 5.

### The documentation and the checks

- [x] Describe in `README.md` the four actions by name and their keys on Linux and on macOS; that each acts on the
  one reading wherever the focus is, also in another project, and that "Read Aloud, Pause or Resume" reads the file of
  the selected editor only when nothing is read and pauses a reading also from another Markdown file; that it does
  nothing outside a Markdown file and "Stop Reading" nothing without a reading; that the speed keys work with no
  reading and share the half second with the buttons; that the tooltips of the buttons name the keys; and that the
  keys are changed or removed in Settings › Keymap, where the actions are found by name, and also run from Find
  Action. Keep every other statement of the paragraphs it touches (R15)
- [x] Write `changes/0017-reading-shortcut/developer-checks.md` in the form of that of unit 0016: the setup on each
  machine; on Linux x64, the look of the tooltips in the light and the dark theme; on Linux x64 and on macOS on Apple
  Silicon, each of the four keys with the focus on the file that is read, on another file and in a tool window, the
  speed changed by key with no file read, and for each key whether the desktop or another application took it before
  the IDE; and a key given to one action and one removed in Settings › Keymap, with the tooltip that follows (R10,
  R11, R14)

Commit point 6.

### The icons of the speed actions

- [ ] Write in `ReadingShortcutsTest` that "Read Slower" and "Read Faster" have the icons `AllIcons.General.Remove` and
  `AllIcons.General.Add`, and give them those icons in `plugin.xml`, as the spec's decision on their icons says (R1)
- [ ] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 8, made before 7.

### The developer's checks

- [ ] Hand the developer the checks of `developer-checks.md` on Linux x64 and on macOS on Apple Silicon, and record
  in `record.md` what they found, for each key and machine whether the desktop or another application took it first.
  Where a key is taken on either machine, stop and log it for the human, who chooses the keys (R10, R14)

Commit point 7.

## Verification

- [x] `./gradlew test` passes
- [x] `./gradlew buildPlugin` builds the archive and passes its size check
- [x] `npm run check-changes` passes on the committed branch
- [ ] The record holds the result of the look-up of the keystrokes in the distribution, and the developer's findings
  for R14 on both machines

## Trace

- R1: the actions by key, the speed by key
- R2: the actions by key
- R3: the actions by key
- R4: the actions by key
- R5: the actions by key
- R6: the speed by key
- R7: the actions by key, the speed by key
- R8: the shortcuts in the keymaps
- R9: the shortcuts in the keymaps, with the look-up in the distribution
- R10: the shortcuts given in the keymap only, and the developer's checks
- R11: the tooltips, the look in the sandbox IDE, the developer's checks
- R12: the tooltips
- R13: the tests of every section but the documentation and the checks
- R14: the developer's checks
- R15: the documentation

## Commit Points

- [x] 1. `feature-0017 Plan the reading shortcuts`: this plan and its log entry
- [x] 2. `feature-0017 Read, pause, resume and stop the reading by key`: `Readings`, the two tab actions,
  `plugin.xml` and their tests
- [x] 3. `feature-0017 Change the reading speed by key`: the setting, the speed actions, the panel, `plugin.xml` and
  their tests
- [x] 4. `feature-0017 Give the actions their default shortcuts`: `plugin.xml` and its tests
- [x] 5. `feature-0017 Name the shortcuts in the tooltips of the buttons`: `ShortcutTooltip.kt`, the tab actions, the
  panel and their tests
- [x] 6. `feature-0017 Describe the reading shortcuts`: `README.md` and `developer-checks.md`
- [ ] 8. `feature-0017 Give the speed actions the minus and plus icons`: the spec's decision, this section, `plugin.xml`
  and its test
- [ ] 7. `feature-0017 Record the developer's checks of the reading shortcuts`: `record.md`
