# Reading tool window

## Approach

The plugin registers a tool window, "AloudRite", whose one content is a panel that shows the one reading of the IDE
and stops it. The panel asks `SpeechService` what is read and tells it to stop, and it learns of every change of the
reading from a topic on the message bus of the application that `SpeechService` publishes to. Every project has its
own tool window and its own panel, and all of them listen to the same service and the same topic, so they show the
same reading (R5).

This plan is written against the branch `feature-0015-reading-tool-window`, cut from `master` at `3f4c129`, on which
units 0006 and 0010 are implemented and units 0012, 0013, 0016 and 0017 are drafts. On that tree:

* `SpeechService`, an application service, holds the reading as `current: Reading?` under its monitor, behind the
  interface `Readings`, which is what `ReadAloudAction` needs of it: `isReading`, `read`, `stop` and
  `readOnceTheModelIsInPlace`
* Every start and every end of a reading goes through one of four places, `read`, `stopWhere`, `end` and through
  `end` also `fail`, and each of them calls `onChange` once the state has changed. `stopWhere` serves the stop on the
  tab, the closing of the file's tab (`fileClosed`) and of its project (`projectClosed`), and `read` the press on
  another file, which stops the first. `onChange` is `ActivityTracker.getInstance().inc()` in the IDE, which has the
  tab button update, and a counter in `SpeechStandIns`
* `onChange` is called on the EDT for a press and a stop, and on a pooled thread for the end of the file and a
  failure
* `ReadingTarget(project: Project?, file: VirtualFile)` names what is read. In the IDE the project is that of the
  editor tab, and in the tests it is `null` unless the test hands one
* Projects cannot be opened in the test environment of IntelliJ IDEA 2026.2.3, so the tests start only the
  application with `ApplicationRule`. A test that needs a `Project` uses `ProjectManager.getInstance().defaultProject`
* `ReadAloudActionTest.thePluginRegistersNoActionButTheButton` checks that the only action of the plugin is
  `com.ritenoar.aloudrite.ReadAloud`

`./gradlew test` on that tree ran 261 tests: none failed, and two, `RealModelPackageTest` and
`EngineThreadsMeasurement`, were skipped.

### What the tool window needs of the reading

`SpeechService.kt` gets a second interface beside `Readings`, `CurrentReading`, with `fun currentTarget():
ReadingTarget?` and `fun stop(target: ReadingTarget)`. `SpeechService` implements both, with `currentTarget()`
giving `current?.target` under its monitor. The panel depends on `CurrentReading` only, which is all it uses, and
the action keeps depending on `Readings`.

### The change of the reading

A new file `ReadingChange.kt` holds `fun interface ReadingChangeListener { fun readingChanged() }`, the application
topic `READING_CHANGED: Topic<ReadingChangeListener>`, made with `Topic.create`, and `fun publishReadingChange()`,
which publishes on `ApplicationManager.getApplication().messageBus.syncPublisher(READING_CHANGED)`. The default
`onChange` of `SpeechService` becomes `ActivityTracker.getInstance().inc()` followed by `publishReadingChange()`.
Nothing else in `SpeechService` changes: every place that already calls `onChange` now reaches the tool windows too
(R6).

`SpeechStandIns` keeps its counter, and gets `var whenChanged: () -> Unit = { }`, which its `onChange` calls after
counting. The tests of the panel set it to `::publishReadingChange`, so that a stand-in reading reaches the panel by
the same topic as in the IDE. It is not published by default, because `SpokenMarkdownTest` uses the stand-ins
without starting the application, and the bus is then not there.

### The panel

A new file `ReadingPanel.kt` holds `class ReadingPanel(private val ownProject: Project, private val reading: () ->
CurrentReading)`, a `JPanel` with a `VerticalLayout` of a gap of `JBUI.scale(8)` and an empty border of
`JBUI.Borders.empty(8)`. The stop button is held in a row of its own, so that it keeps its own width, as task 10
says. The reading is given as a function, as `ReadAloudAction` is given its services, so that the
service is looked up when it is used.

It holds:

* `nothingReadLabel`, a `JBLabel` with the text of `NOTHING_READ_TEXT`, which is the HTML
  `<html>Nothing is being read.<br>Start a reading with "Read Aloud" on the tab of a Markdown file.</html>`, so that
  it wraps in a narrow pane (R3)
* `fileLabel`, a `JBLabel` with `Reading <file name>` (R4)
* `projectLabel`, a `JBLabel` with `In the project <project name>`, shown only when the target's project is not null
  and is not `ownProject`, compared by identity. A target without a project, which only a test makes, shows no
  project (R4, R5)
* `stopButton`, a plain `JButton` with the text `Stop`. It is not an action, so the plugin still registers no action
  but the tab button. Its listener stops the target that the panel shows, through `reading().stop(shownTarget)`, so
  that a press stops what the user saw. Where that reading has already ended or been replaced, the stop does nothing,
  as a stop on the tab of a file that is not read does nothing, and the panel shows the new state with the change
  that is on its way (R4)

`fun showTheCurrentReading()`, called on the EDT, reads `reading().currentTarget()` once, keeps it as `shownTarget`,
and sets which labels are visible and their texts: `nothingReadLabel` alone while nothing is read, and `fileLabel`,
`projectLabel` where it applies, and `stopButton` while a file is read. It ends with `revalidate()` and `repaint()`.

`fun followTheReading(parentDisposable: Disposable)` connects to the bus of the application with
`messageBus.connect(parentDisposable)`, subscribes to `READING_CHANGED` with a listener that calls
`ApplicationManager.getApplication().invokeLater(::showTheCurrentReading, ModalityState.any())`, and then calls
`showTheCurrentReading()` once, so that a panel made during a reading shows it at once. The change is handed to the
EDT because a reading ends on a pooled thread. `ModalityState.any()` is used so that the panel follows the reading
also while a dialog is open, such as the message of a failure, since setting a label touches no model of the IDE. A
change that is queued when the panel is disposed only sets the labels of a panel that is no longer shown, which is
harmless, so no expiry condition is given.

The texts are constants in the companion of `ReadingPanel`, as `ReadAloudAction` keeps its own. The panel's own
project is not nullable: a tool window always has one.

### The tool window

A new file `ReadingToolWindowFactory.kt` holds `class ReadingToolWindowFactory : ToolWindowFactory, DumbAware`, so
that the tool window works while the project is indexed.

* `createToolWindowContent(project, toolWindow)` makes a `ReadingPanel(project) { service<SpeechService>() }`, puts
  it in a content made with `ContentFactory.getInstance().createContent(panel, null, false)`, adds the content to
  `toolWindow.contentManager`, and calls `panel.followTheReading(content)`, so that the subscription ends when the
  content is disposed, with its tool window or its project
* `init(toolWindow)` puts the button on the tool window bar once per project: where the project's
  `PropertiesComponent` does not hold `com.ritenoar.aloudrite.toolWindowButtonPlaced`, it sets
  `toolWindow.isShowStripeButton = true` and then that value to `true`. Once it is set, the button is left to the
  user and to the layout the IDE keeps, so a button the user removed or moved stays where the user put it (R1, R2).
  This uses only the public API of `ToolWindow`, and works alike for a new project and for a project that was opened
  before the plugin was installed, which the default layout of the platform, the extension point
  `com.intellij.defaultToolWindowLayout` in the package `com.intellij.toolWindow`, would leave to whatever default
  layout the user has stored

In `plugin.xml`, in the `extensions` of `com.intellij`:

```xml
<toolWindow id="AloudRite" anchor="right" icon="/icons/readAloud.svg" doNotActivateOnStart="true"
            factoryClass="com.ritenoar.aloudrite.ReadingToolWindowFactory"/>
```

The id is the name shown on the button, in the header of the tool window and under View › Tool Windows (R1). The
icon is the file of the tab button, so that the dark variant `readAloud_dark.svg` is found beside it as it is for
the button (R1). `anchor="right"` docks it on the right for a project that has no layout of it yet, and the layout
the IDE keeps of the project then holds where the user put it (R1, R2). `doNotActivateOnStart="true"` keeps it closed
when a project is opened, and nothing in the plugin activates or shows it, also not at a start of a reading (R2).

### Tests

The panel is tested without a tool window, since no project can be opened: a test makes a `ReadingPanel` on the EDT
with `runInEdtAndWait`, hands it `standIns.service` as its reading, calls `followTheReading` with a disposable of
the test, and after each step runs `PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()` on the EDT before
it looks at the labels. What a panel shows is read from the visibility and text of its four components, which the
test reaches as `internal` properties of `ReadingPanel`.

The own project of a panel is the default project. The project of another tool window is a `MockProject(null,
disposable)`, which is in `intellij.platform.core.impl` of the IDE and on the test classpath. It is used only as the
own project of a panel, which compares it by identity and calls nothing else on it. A file is never pressed with it,
since `ActionUtil.performAction` asks the event's project for its `DumbService`, which a `MockProject` does not have,
and a `Proxy` of `Project` would not have either. So the reading of a file of another project is a press with the
default project, seen from the panel of the `MockProject`, which shows the name of the default project.

A reading is started by pressing the tab button with `pressTheButtonOn` from `ButtonPressSupport.kt`, with the
project of the file handed as `project`, so that the target holds it, and with a `ReadAloudAction` on the stand-ins.
`ReadAloudActionTest` makes that action with its private `readingAction`, and the panel's tests need the same, so
the making of it moves to `ButtonPressSupport.kt` first, in a commit of its own that changes no behavior.

That the tool window is registered as the requirements say is tested on the extension point `ToolWindowEP.EP_NAME`,
which the application of the tests loads from `plugin.xml`. The button on the tool window bar, the side, floating and
the window of its own are the IDE's behavior on a real project, and are checked by the developer (R9).

The default `onChange` of `SpeechService`, which calls `publishReadingChange()`, is the one line of the chain that
the tests do not run, since a reading by the real service needs the model. The developer's stop from the tool window
while another file is selected runs it (R9).

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: the interface `CurrentReading`, implemented by
  `SpeechService` with `currentTarget()`, and the default `onChange` also publishes the change
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingChange.kt`: new, the listener, the topic and
  `publishReadingChange()`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: new, what the tool window shows and its "Stop"
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingToolWindowFactory.kt`: new, the content of the tool window and its
  button on the bar
- `src/main/resources/META-INF/plugin.xml`: the `toolWindow` extension
- `src/test/kotlin/com/ritenoar/aloudrite/ButtonPressSupport.kt`: the registered tab action and a reading action on
  given services, moved from `ReadAloudActionTest`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: uses the moved helpers
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: `whenChanged`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`: `currentTarget()`
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: new, what the panel shows, its stop, and how it
  follows the reading
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingToolWindowTest.kt`: new, the registration of the tool window, and
  the button put on the bar once per project (task 11)
- `README.md`: the tool window and what it shows
- `AGENTS.md`: when a developer's check on macOS is asked for (task 12)
- `changes/0015-reading-tool-window/record.md`: the log and what the developer found

## Tasks

Each task names the requirements it delivers. The test of a task comes before its code.

### The making of the reading action, moved

- [x] 1. Move `readAloudAction()` of `ReadAloudActionTest`, which gives the registered action and fails where it is
      not registered, to `ButtonPressSupport.kt` as `registeredReadAloudAction(): AnAction`, with the constant
      `READ_ALOUD_ACTION_ID` beside it. Add there `readAloudActionOn(readings: Readings, downloads: ModelDownloads,
      modelFolder: Path): ReadAloudAction`, which makes `ReadAloudAction("Linux", "amd64", modelFolder, { readings },
      { downloads })` and copies the template presentation of the registered action into it, with the comment that
      `readingAction` has now about why. Have `readingAction` of `ReadAloudActionTest` call it with
      `standIns.service` and `idleDownloads`, and every other use of `readAloudAction()` there call
      `registeredReadAloudAction()`. Nothing else changes, and `./gradlew test` passes with the same number of tests
      (R8)

### Commit point A: `feature-0015 Make the reading action in the support of the tests`

### What is read, and its change

- [x] 2. In `SpeechServiceTest`, add: `currentTarget()` is `null` before a reading; it is the target while it is
      read; it is the second target after a reading of a second one; and it is `null` again after a stop, after the
      end of the file once the kept works have run, and after a failure of the engine. The tests do not compile
      until task 3 (R4, R6)
- [x] 3. In `SpeechService.kt`, add the interface `CurrentReading` and implement it in `SpeechService` as "What the
      tool window needs of the reading" says. Add `ReadingChange.kt` as "The change of the reading" says, and make
      the default `onChange` of the no-argument constructor `{ ActivityTracker.getInstance().inc();
      publishReadingChange() }`. In `SpeechStandIns`, add `whenChanged` and call it from `onChange` after the
      counter. Run `./gradlew test`: all pass (R6)

### The panel

- [x] 4. Write `ReadingPanelTest`, with `ApplicationRule` as its class rule as the other tests that use the
      application have, and a disposable that is disposed after each test. Each test sets `standIns.whenChanged =
      ::publishReadingChange` and makes the panel of the default project, following the reading, as "Tests" says.
      The file pressed is `markdownFile("One sentence.")`, named `notes.md`, with the model stand-in in a temporary
      folder as `ReadAloudActionTest` makes it. The cases:
      * While nothing is read, the panel shows `nothingReadLabel` with `NOTHING_READ_TEXT`, and neither the labels of
        a reading nor `stopButton` (R3)
      * After a press with the default project, it shows `Reading notes.md` and `stopButton` with the text `Stop`, and
        neither `projectLabel` nor `nothingReadLabel` (R4)
      * After a press with the default project, the panel of the other project shows `Reading notes.md`, `In the
        project ` with the name of the default project, and `stopButton` (R4, R5)
      * A panel made while a file is read shows the reading at once, before any change is published (R4, R6)
      * A click on `stopButton`, with `doClick()` on the EDT, ends the reading: `standIns.service.isReading` is false
        for the target, the stand-in player is discarded or plays no slice after the click once the kept works have
        run, the tab button's `presentationFor` the file gives `Read Aloud` and the loudspeaker icon of the registered
        action again, and after the events are dispatched the panel shows that nothing is read (R4)
      * A panel of the default project and a panel of the other project show the same reading of a file pressed with
        the default project, the second with `projectLabel` and the first without; a click on `stopButton` of the
        second ends it, and both show that nothing is read. A second reading, pressed again, is stopped from the first
        panel, and both show that nothing is read again (R5)
      * The panel follows: a press on `notes.md` shows it; a press on a second file, `other.md`, shows `Reading
        other.md`; running the kept works to the end of the file shows that nothing is read; a press and then a
        second press on the tab, which stops, shows that nothing is read; `standIns.service.fileClosed` and
        `standIns.service.projectClosed` of the target's project, each after a press, show that nothing is read; and a
        press with `standIns.engineFailsAt = { true }` shows that nothing is read once the kept works have run, with
        the failure in `standIns.failures` (R6)
      * After the disposable of `followTheReading` is disposed, a press leaves the panel as it was, since it no longer
        follows
      The tests do not compile until task 5 (R3, R4, R5, R6, R8)
- [x] 5. Write `ReadingPanel.kt` as "The panel" says. Run `./gradlew test`: all pass, among them those of
      `ReadAloudActionTest`, unchanged, also `thePluginRegistersNoActionButTheButton` (R3, R4, R5, R6, R7)

### Commit point B: `feature-0015 Show the reading in a panel and stop it there`

### The tool window

- [x] 6. Write `ReadingToolWindowTest`, with `ApplicationRule`: `ToolWindowEP.EP_NAME.extensionList` holds exactly
      one tool window of this plugin, whose `id` is `AloudRite`, `anchor` is `right`, `icon` is `/icons/readAloud.svg`,
      `isDoNotActivateOnStart` is true and `factoryClass` is `com.ritenoar.aloudrite.ReadingToolWindowFactory`, and
      the class of that name loads and is a `ToolWindowFactory` that is `DumbAware`. The test fails until task 7 (R1,
      R2)
- [x] 7. Write `ReadingToolWindowFactory.kt` as "The tool window" says, and add the `toolWindow` extension to
      `plugin.xml`. Run `./gradlew test`: all pass (R1, R2)

### Commit point C: `feature-0015 Add the AloudRite tool window`

### Documentation

- [x] 8. In `README.md`, after the paragraph that ends "When the speech fails during a reading, the reading ends, a
      message says so, and the next press works.", add a paragraph that says: every project has a tool window named
      "AloudRite", with the loudspeaker icon, docked on the right and closed when a project is first opened with the
      plugin, with its button on the tool window bar on that side, and also under View › Tool Windows; it does not
      open by itself, also not when a reading starts, and stays where the user puts it, docked on any side, floating
      or in a window of its own, as any tool window; while nothing is read it says so and that a reading is started
      with "Read Aloud" on the tab of a Markdown file; while a file is read it shows the name of the file, the name of
      its project when that is another one, and a "Stop" button that stops the reading as the button on the tab does;
      and since one file is read in the whole IDE, every project's tool window shows that reading and can stop it.
      Leave the rest of the README as it is (R10)

### Commit point D: `feature-0015 Describe the tool window`

### The developer's check

- [x] 9. Build with `./gradlew buildPlugin` and start the sandbox IDE with `./gradlew runIde`. Have the developer
      check, on Linux x64, and write what they found in `record.md`:
      * In a project opened for the first time in the sandbox, and in one that was opened there before this branch,
        the AloudRite button is on the tool window bar on the right, and a click opens the tool window, which shows
        that nothing is read (R1, R3, R9)
      * The tool window is under View › Tool Windows, with the loudspeaker icon in both the light and the dark theme
        (R1)
      * Pressing "Read Aloud" on `samples/markdown-features.md` does not open the tool window; once opened, it shows
        `Reading markdown-features.md` within a second of the press, and after selecting another file, `README.md`,
        "Stop" in the tool window ends the sound within a second, the tab button shows the loudspeaker again, and the
        tool window shows that nothing is read (R2, R4, R6, R7, R9)
      * With a second project open in another window, its tool window shows the reading with the name of the first
        project, and its "Stop" ends it (R4, R5)
      * The tool window moved to the left side, floated, and moved into a window of its own each keep showing the
        reading and stopping it, and after closing and reopening the project the tool window is where it was put
        (R2, R9)
      * The button removed from the bar stays removed after reopening the project, and the tool window still opens
        from View › Tool Windows (R1)
      Where a check fails, log what was found in `record.md` and fix it in a task added below, before the unit is
      reviewed (R9)

### What the developer found: the size of the stop

- [x] 10. Added after the developer's check on Linux, which found the "Stop" button stretched across the whole width
      of the tool window, since `VerticalLayout` fills the width with each component by default. In
      `ReadingPanelTest`, add: a panel laid out at 800 by 400 pixels while a file is read gives `stopButton` its
      preferred width; and a panel laid out at 150 pixels wide while nothing is read gives `nothingReadLabel` no
      more than that width, so that its text still fits a narrow pane. Run them and see the first fail. Then put
      `stopButton` at the west of a `BorderLayout` in a row of its own, `stopRow`, which is what the panel shows and
      hides in place of the button, and leave the `VerticalLayout` filling the width, which the labels need to wrap.
      Aligning the whole layout to the left, tried first, gave `nothingReadLabel` its preferred width and failed the
      second test. Run `./gradlew test`: all pass. The width of the tool window itself is left to the platform and to
      the user, as the human decided (R4)

### Commit point E: `feature-0015 Keep the stop button at its own size`

### Commit point F: `feature-0015 Record what the developer found`

### The findings of the review

- [x] 11. Added for finding C1 of the review, which found that no test calls `init` of `ReadingToolWindowFactory`. In
      `ReadingToolWindowTest`, add tests that call `init` with a stand-in of `ToolWindow`, a `java.lang.reflect.Proxy`
      that answers `getProject` with the default project and keeps what `setShowStripeButton` was given, and fails
      on any other method. The property `com.ritenoar.aloudrite.toolWindowButtonPlaced` of the default project's
      `PropertiesComponent` is unset before and after each test. The cases: the first `init` puts the button on the
      bar and sets the property; a later `init`, once the property is set, leaves the button alone, also where the
      user has removed it. Run them against the factory as it is: they pass, since they cover code that exists, and
      each is seen to fail with the line of `init` it covers taken out. Run `./gradlew test`: all pass (R1, R2)
- [x] 12. Added for finding P2 of the review: the rule in `AGENTS.md` under "Building, running and testing", that a
      developer's check on macOS on Apple Silicon is asked for only when a unit touches what differs between the
      platforms, which the human asked for in this unit and which was made at `011b5d2`, before this task was in
      the plan. Nothing more is changed for it

### Commit point G: `feature-0015 Test the button put on the bar once`

## Verification

- [x] `./gradlew test` passes, with no test skipped beyond the two that are skipped on `master`
- [x] `./gradlew buildPlugin` passes its archive size check
- [x] The developer's check of task 9 on Linux x64 is in `record.md`
- [x] `npm run check-changes` passes
- [x] Every requirement in the trace has its tasks ticked

## Trace

- R1: tasks 6, 7, 9, 11
- R2: tasks 6, 7, 9, 11
- R3: tasks 4, 5, 9
- R4: tasks 2, 4, 5, 9, 10
- R5: tasks 4, 5, 9
- R6: tasks 2, 3, 4, 5, 9
- R7: tasks 5, 9
- R8: tasks 1, 4
- R9: task 9
- R10: task 8

## Commit Points

- [x] A: `feature-0015 Make the reading action in the support of the tests`, after task 1
- [x] B: `feature-0015 Show the reading in a panel and stop it there`, after task 5
- [x] C: `feature-0015 Add the AloudRite tool window`, after task 7
- [x] D: `feature-0015 Describe the tool window`, after task 8
- [x] E: `feature-0015 Keep the stop button at its own size`, after task 10
- [x] F: `feature-0015 Record what the developer found`, after tasks 9 and 10
- [x] G: `feature-0015 Test the button put on the bar once`, after tasks 11 and 12
