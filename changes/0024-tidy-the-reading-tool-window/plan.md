# Tidy the reading tool window

## Approach

The pane stays a hand-built `ReadingPanel` of the layouts it uses now, `VerticalLayout` and `HorizontalLayout` of the
platform, rather than moving to the Kotlin UI DSL, so that the change is a regrouping of the components it has and
not a rewrite. What changes is decided here:

* **Three groups.** The panel becomes a `VerticalLayout` of `GROUP_GAP = 14` holding three group panels, each a
  `VerticalLayout` of `ROW_GAP = 6`, both scaled with `JBUI.scale`. A gap of the panel is the distance between two
  groups and a gap of a group the distance between two of its rows, so the rows within a group are always closer than
  a group is to the next (R1). Should the captures of R12 show other values to look better, they may change, as long
  as `ROW_GAP` stays below `GROUP_GAP`, which a test asserts
    * `readingGroup`: `nothingReadLabel`, `fileLabel`, `partLabel`, `projectLabel` and `timesRow`, in that order
    * `speedGroup`: a `TitledSeparator` of the platform reading "Reading speed", then `speedRow`
    * `modelGroup`: a `TitledSeparator` reading "Speech model", then `modelRow`
    * `speedLabel` and `modelLabel` go, since the separators carry their titles. The constants `SPEED_LABEL_TEXT` and
      `MODEL_LABEL_TEXT` stay and become the titles
* **The bar and the times.** `timesRow` becomes a `VerticalLayout` of `ROW_GAP` holding the `progressBar` and
  `timesLine`, a `JPanel(BorderLayout())` with `timeLeftLabel` at `WEST` and `wholeTimeLabel` at `EAST`. A
  `VerticalLayout` gives each child the width of its parent, so the bar spans the pane at any width (R4). Both labels
  are made with `ComponentPanelBuilder.createCommentComponent("", false)`, which is the IDE's comment: its smaller font
  and its grey, following the theme (R5). Their texts are `About <left> left` and `<whole> in all`, with
  `minutesAndSeconds` of unit 0022, and both get `ESTIMATE_TOOLTIP_TEXT` as their tooltip. `timesLabel` goes
* **Pause, resume and stop in the title bar.** Two new actions in a new file `ReadingTitleActions.kt`, set on the
  tool window by `ReadingToolWindowFactory.createToolWindowContent` with `toolWindow.setTitleActions`. They are not
  registered in `plugin.xml`, so that they add nothing to Find Action or the keymap, where "Read Aloud, Pause or
  Resume" and "Stop Reading" already are. They do not reuse `ReadAloudAction` and `StopReadingAction`, because a title
  bar is a toolbar, which `isOnTheTab` takes for the tab, and because those hide themselves or start a reading where
  R3 asks for a greyed, idle button
    * `PauseOrResumeReadingInTheTitleAction(reading: () -> CurrentReading)`: its update, on `BGT`, keeps it visible,
      enabled only while `currentTarget()` is not null, with the icon `AllIcons.Actions.Resume` and the text "Resume
      Reading" while that target is paused, and otherwise `AllIcons.Actions.Pause` and "Pause Reading", also while
      nothing is read (R2, R3). A press takes `System.nanoTime()` first, then resumes the current target with it where
      it is paused and pauses it otherwise, and does nothing where there is none
    * `StopReadingInTheTitleAction(reading: () -> CurrentReading)`: visible, enabled only while a target is current,
      with `AllIcons.Actions.Suspend`, the IDE's stop icon that the tab's stop button shows, and "Stop Reading". A
      press stops the current target
    * Both put `shortcutTooltip(text, ReadAloudAction.ID)` and `shortcutTooltip(text, StopReadingAction.ID)` into
      `ActionButton.CUSTOM_HELP_TOOLTIP`, as `StopReadingAction` does on the tab, since an unregistered action has no
      shortcut of its own to show. The tooltip is made at every update, so that it follows a change of the keymap as
      the panel's tooltips do now
    * Both act on the one reading of the IDE at the press, through the application service, so the title bar of
      every project's tool window pauses, resumes and stops it (R2). A toolbar follows a change of the reading at once,
      since `SpeechService` already calls `ActivityTracker.getInstance().inc()` on each change
    * `pauseButton`, `stopButton`, `controlRow`, `pauseOrResume`, `shownPaused` and the texts `PAUSE_TEXT`,
      `RESUME_TEXT` and `STOP_TEXT` go from the panel. The tooltip texts move to the new file
* **The remark on the speed.** `speedComment` goes, and `speedField.toolTipText` is `SPEED_COMMENT_TEXT` (R6)
* **The model and the link.** `modelRow` is a `JPanel(BorderLayout())` with `modelNameLabel` at `CENTER`, HTML as now
  so that it wraps, and `settingsLink`, an `ActionLink` of the platform reading `Settings…` with the ellipsis
  character U+2026, at `EAST` inside a panel that keeps it at the top of the row when the name wraps. The panel gains a
  constructor parameter `openTheSettings: () -> Unit`, which the link calls. The factory passes
  `{ openTheAloudRiteSettings(project) }`, a top-level function in `ReadingToolWindowFactory.kt` that calls
  `ShowSettingsUtil.getInstance().showSettingsDialog(project, ALOUDRITE_SETTINGS_PAGE)` with
  `ALOUDRITE_SETTINGS_PAGE = ReadingSpeedConfigurable::class.java`, the class of the page "AloudRite" under Tools (R7)
* **One left edge.** The edge is the inner edge of the panel's border, `JBUI.Borders.empty(GAP)` as now. The labels,
  the bar and the comment labels start there by themselves. Two components paint their content inset from their own
  left edge, and are corrected where they do: the minus button, an `InplaceButton` that centers its icon in a size of
  its own, gets a preferred size equal to its icon, so that the icon starts at its left edge; and each
  `TitledSeparator`'s label, whose border may hold a left inset for its focus mark, gets that left inset set to 0 with
  its other insets kept. A test measures the left edge of every element in the panel's coordinates (R8). The tests run
  without the IDE's look and feel, so the captures of R12 are what confirms the edge in the IDE; where they show an
  element off the edge, it is corrected the same way, by removing the inset of the component that paints it inset
* While nothing is read, the reading group shows only `nothingReadLabel` with `NOTHING_READ_TEXT` as now, and
  `timesRow` is hidden as now (R9). Everything else the panel does, the following of the reading, the speed field and
  its mark, the tooltips of minus and plus, the following of the model and the timer, stays as it is (R10)

The tests of the text buttons in `ReadingPanelTest` are moved, not dropped: what they check of a press, the
following of the tab and the two projects, is checked on the title actions in a new `ReadingTitleActionsTest`, so that
R10's "what a press does" stays covered.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/ReadingTitleActions.kt`: new, `PauseOrResumeReadingInTheTitleAction`,
  `StopReadingInTheTitleAction` and their tooltip texts
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingToolWindowFactory.kt`: sets the title actions, passes the opener of
  the settings, `openTheAloudRiteSettings` and `ALOUDRITE_SETTINGS_PAGE`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: the groups, the separators, the bar and the line of the
  times, the tooltip of the field, the row of the model with the link, the left edge, and the text buttons removed
- `src/test/kotlin/com/ritenoar/aloudrite/ButtonPressSupport.kt`: `titleEventFor(action, project)`, an event with the
  place `ActionPlaces.TOOLWINDOW_TITLE` and `ActionUiKind.TOOLBAR`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTitleActionsTest.kt`: new, the title actions
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: the groups, the times, the speed, the model, the edge,
  and the tests of the text buttons removed
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingToolWindowTest.kt`: the page the link opens
- `README.md`: the tool window as it is after the unit
- `changes/0024-tidy-the-reading-tool-window/record.md`: what the captures showed, and the developer's check

## Tasks

- [x] Add `titleEventFor(action, project)` to `ButtonPressSupport.kt`, with a data context of the project alone, and
  write `ReadingTitleActionsTest` with `SpeechStandIns` and the `readAloudActionOn` of the panel test to start a
  reading, so that it fails to compile until the actions exist. It covers, for each of the two actions: while nothing
  is read, visible, disabled, the pause icon and "Pause Reading" for the first, the stop icon and "Stop Reading" for
  the second, and a press of the first through `ActionUtil.performAction` reads nothing and changes nothing; while a
  file is read, enabled, `AllIcons.Actions.Pause` and `AllIcons.Actions.Suspend`; while it is paused, the first
  `AllIcons.Actions.Resume` and "Resume Reading"; the tooltips, read with `titleAndShortcutOf` from
  `CUSTOM_HELP_TOOLTIP`, are "Pause Reading", "Resume Reading" and "Stop Reading" each with `ctrl alt shift SPACE` or
  `ctrl alt shift O`, and follow a keymap of the user from `withAKeymapOfTheUser` that removes the shortcut of
  "Stop Reading"; a press of the first pauses the reading and the tab button then shows "Resume Reading", a second
  press resumes it; a press of the second stops a playing and a paused reading, after which no text is made and the tab
  button shows "Read Aloud"; and a reading started in one project is paused and stopped by the title actions of an
  event of another project (R2, R3, R11)
- [x] Write `ReadingTitleActions.kt` as the approach says, until `ReadingTitleActionsTest` passes (R2, R3)
- [x] In `ReadingToolWindowFactory.createToolWindowContent`, set the title actions of the tool window to the two
  actions, in the order pause or resume, then stop, each given `{ service<SpeechService>() }` (R2)
- [x] Commit: `feature-0024 Pause, resume and stop the reading from the title bar of the tool window`
- [x] Change `ReadingPanelTest` for the new pane, and see it fail. Remove the tests of the text buttons that
  `ReadingTitleActionsTest` now covers: `stopEndsTheReadingAsTheStopOnTheTabDoes`,
  the presses of `thePanelsOfTwoProjectsShowTheSameReadingAndEachStopsIt`, `theTwoButtonsKeepTheirOwnWidthInAWidePane`,
  `pauseAndResumeInThePanelDoWhatTheTabButtonDoesAndTheTabButtonFollows`, `stopEndsAPausedReading`, the presses of
  `theLineOfThePartStaysAfterAPressOfPauseAndResumeInThePanel`, which then pauses and resumes through the service,
  and the pause and stop lines of the tooltip test; keep each test's check of what the panel shows. `shownBy` gives
  the visible labels of the reading group and the two times, without buttons, and `ONE_SENTENCE_TIMES` becomes
  `About 0:01 left` and `0:01 in all`. Add or change tests for (R1, R4, R5, R6, R7, R8, R9, R11):
    * the panel holds the reading group, then the speed group, then the model group; the speed group begins with a
      `TitledSeparator` whose text is "Reading speed" and the model group with one whose text is "Speech model"; the
      reading group has none; laid out at the width of the first docked pane with a file read, the distance from the
      bottom of the reading group to the top of the speed separator is larger than every distance between two rows
      within a group (R1)
    * with a file read, the reading group shows from the top the name, the part, the project, the bar and the line of
      the times, and the panel holds no `JButton` other than the link of the model, since `ActionLink` is one (R2, R4)
    * laid out at 200 and at 468 pixels wide, the bar's left edge is at the panel's left inset and its right edge at the
      panel's width less its right inset (R4)
    * the two times are `About 0:01 left` and `0:01 in all` once their markup is removed, the second at the right edge
      of the line, both with `ESTIMATE_TOOLTIP_TEXT`, both in a font smaller than that of `fileLabel`, and the existing
      tests of the times after a change of the speed and while paused check both labels (R5)
    * the panel no longer holds the text of `SPEED_COMMENT_TEXT` in any label, and the speed field's tooltip is that
      text; `speedShownBy` reads the separator's title and the field's tooltip in place of the label and the comment
      (R6)
    * the model row shows the name of the model on its left and a link `Settings…` at its right edge; the link is the
      only control of the model group; a `doClick` on it calls `openTheSettings` once; the longest name of
      `SPEECH_MODELS` fits on one line beside the link in the first docked pane, and in a pane of 150 pixels the name
      label is no wider than the room left of the link (R7)
    * at the IDE's default scale, laid out at 468 pixels with a file of another project read from the caret, the left
      edge of the text of `fileLabel`, `partLabel`, `projectLabel`, `timeLeftLabel` and `modelNameLabel`, of the text of
      each separator's label, of the bar and of the icon of the minus button, all in the panel's coordinates, differ by
      at most one pixel; and while nothing is read, that of `nothingReadLabel` too (R8)
    * while nothing is read, the reading group shows only `NOTHING_READ_TEXT` (R9)
- [x] Change `ReadingPanel` as the approach says, until `ReadingPanelTest` passes, and pass `openTheSettings` from
  the factory and from the `panelOf` of the test (R1, R4, R5, R6, R7, R8, R9, R10)
- [x] Add to `ReadingToolWindowTest` that `ALOUDRITE_SETTINGS_PAGE` is the instance class of the application
  configurable the plugin registers with the display name "AloudRite" under the parent "tools", read from the
  extension point `com.intellij.applicationConfigurable` (R7, R11)
- [x] Run `./gradlew test` and `./gradlew buildPlugin`, and fix what fails (R10, R11)
- [x] Commit: `feature-0024 Group the reading tool window under titled separators`
- [x] Start the sandbox IDE with `./gradlew runIde`, open `samples/markdown-features.md` and capture only the window
  whose title ends in `[aloudrite-intellij]` with `xwd -id <window id>`, as `AGENTS.md` says, while nothing is read,
  while the file is read and while it is paused, each at the width the tool window opens with and at one narrow
  enough that the model's name wraps, in the light theme and in the dark one. In each, look at the groups and the gaps
  (R1), the title bar buttons and their greyed state (R2, R3), the bar across the pane and the times under it (R4,
  R5), the link at the right of the model row with the name wrapped in full (R7), and the one left edge (R8), and
  correct what is off, the gaps and insets as the approach allows. Where the narrow capture shows the wrapped name
  cut, make `modelNameLabel` report a preferred height for the width the layout gives it. Log in `record.md` what the
  captures showed of R1, R4, R5, R7 and R8 in each theme (R12)
- [x] The captures showed no line of the times under the bar when the pane was laid out before the reading started:
  the labels of `createCommentComponent`, copyable and wrapping, keep the height of 0 they had with no text. Add to
  `ReadingPanelTest` a test that lays the pane out while nothing is read, starts a reading, lays it out again and
  asserts that the line of the times and both labels are as high as their text, and see it fail. Then make both labels
  with `ComponentPanelBuilder.createNonWrappingCommentComponent`, the IDE's comment that neither wraps nor is
  copyable, with the same font and grey, until it passes (R4, R5)
- [x] Commit, where the captures led to a change: `feature-0024 Correct the tool window after looking at it`
- [x] Rewrite the paragraphs on the tool window in `README.md` for the pane after the unit: the three groups and
  their separators; "Pause", "Resume" and "Stop" as icon buttons in the title bar of the tool window, greyed out
  while nothing is read, with their tooltips and what a press does; the bar across the pane and the line under it, as
  `About 9:28 left` and `12:40 in all`, in place of `~9:28 left of ~12:40` and of the `~` as the mark of an estimate;
  the remark on the speed as the field's tooltip in place of the grey text under the row; the model under its
  separator with the link `Settings…` that opens the page "AloudRite" under Settings › Tools; and, among the tooltips
  of the shortcuts, the title bar buttons in place of the text buttons. Keep every other statement on the tool window
  as it is (R10, R14)
- [x] Commit: `feature-0024 Describe the tidied tool window in the README`

## Verification

- [x] `./gradlew test` passes. The unit changes neither the speech engine nor the text handed to it, so the tests
  with the real engine may be skipped
- [x] `./gradlew buildPlugin` builds the archive
- [x] `npm run check-changes` passes on the committed branch
- [x] Every requirement has a task: R1, R4 to R9 the panel's tests and code, R2 and R3 the title actions and the
  factory, R7 also the page the link opens, R10 the moved tests and the full test run, R11 the two test classes, R12
  the captures, R13 the developer's check, R14 the README
- [x] Requirement 13 is left to the developer: looking at the tool window on Linux x64 while nothing is read, while a
  file is read and while it is paused, with what they found logged in `record.md`. No check on macOS is asked for,
  since the unit touches only user interface of the IntelliJ Platform

## Commit Points

- [x] `feature-0024 Pause, resume and stop the reading from the title bar of the tool window`
- [x] `feature-0024 Group the reading tool window under titled separators`
- [x] `feature-0024 Correct the tool window after looking at it`, where the captures led to a change
- [x] `feature-0024 Describe the tidied tool window in the README`
