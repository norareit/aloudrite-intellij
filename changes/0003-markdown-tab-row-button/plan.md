# Markdown tab row button

## Approach

One action replaces `SayHelloAction`. It is contributed to the platform's action group `EditorTabsToolbarActions`,
which the editor tab row draws at its right end, and it shows and hides itself in its `update` by the type of the
file in the selected tab. Pressing it opens a small dialog class of the plugin's own, which holds the file's text in
a read-only text area inside a scroll pane.

Everything below marked "trial" was established on 2026-10-08 in a throwaway copy of the build in a scratch
directory outside the repository, deleted afterwards: the action, the dialog, the icons and six tests were written
there, `./gradlew test` passed all six and `./gradlew buildPlugin` produced the archive. Nothing in the trial was
looked at in an IDE window. Log it in `record.md` when something here turns out not to hold.

Things the implementer would otherwise have to find out:

* **The group and what it hands the action.** `EditorTabsToolbarActions` is declared empty in the platform's
  `idea/PlatformActions.xml`. The tab row, `com.intellij.openapi.fileEditor.impl.EditorTabs`, shows it in front of
  the `⋮` group `EditorTabsEntryPoint`. The data context of that tab row holds `CommonDataKeys.VIRTUAL_FILE`, the
  file of the tab selected in that row, and `CommonDataKeys.PROJECT`. With the editor split, each tab row has its
  own, so the action needs nothing else to act on the right tab. This was read from the classes of IntelliJ IDEA
  2026.2.3, not seen running.
* **What counts as Markdown, without the Markdown plugin.** The Markdown plugin registers its file type under the
  name `Markdown`. The action compares `VirtualFile.getFileType().getName()` with that name and references no class
  of the Markdown plugin, so `plugin.xml` keeps `com.intellij.modules.platform` as its only `<depends>`. Where the
  Markdown plugin is absent or disabled, no file has that type and the button never shows. The name is a constant
  in the action, stated once.
* **The text comes from the document.** `FileDocumentManager.getInstance().getDocument(file)` returns the document
  the editor works on, with changes that are not saved, and its `text` is the raw content. It needs no project.
* **Why not `Messages.showInfoMessage`.** The platform's message dialogs render their text as HTML, so `<b>` or `&amp;`
  in a Markdown file would not be shown character for character, and runs of whitespace would collapse. The dialog
  is therefore a `DialogWrapper` subclass with a `JBTextArea`.
* **The size of the dialog.** The text area is created with a fixed number of visible rows and columns, 25 and 80,
  and is wrapped in a `JBScrollPane`. A Swing text area inside a scroll pane takes its preferred viewport size from
  those two numbers and not from its text, so the dialog has the same modest size for an empty file and for a very
  long one, the scroll bars reach all of the text, and the button row stays in view. The text area does not wrap
  lines: the content is shown raw, and a long line scrolls sideways.
* **The dialog has one button.** `createActions()` returns the OK action alone. There is nothing to cancel.
* **How a test sees the dialog.** `DialogWrapper.show()` first offers the dialog to `com.intellij.ui.UiInterceptors`.
  A test registers a `UiInterceptors.UiInterceptor<FileContentDialog>` before invoking the action; its `doIntercept`
  receives the dialog instead of it being shown, reads the title and the content, and disposes the dialog with
  `Disposer.dispose(dialog.disposable)`. An `@After` method calls `UiInterceptors.clear()`. For this the dialog
  exposes its content as a read-only property (trial).
* **The tests open no project.** As in unit 0001 the tests run under the test framework's `ApplicationRule` alone,
  because opening a project fails in the test environment of IntelliJ IDEA 2026.2.3. Nothing in this unit needs a
  project: the action gets its file from the event, the document from the application-level `FileDocumentManager`,
  and passes `event.project`, which is `null` in the tests, to the dialog as its parent (trial).
* **The Markdown plugin in the tests.** The test environment loads only the bundled plugins the build names.
  `testBundledPlugin("org.intellij.plugins.markdown")` in the `intellijPlatform` dependencies puts the Markdown
  plugin on the test classpath only. The packaged descriptor still has the one `<depends>` (trial).
* **The event the tests build.** `SimpleDataContext.builder().add(CommonDataKeys.VIRTUAL_FILE, file).build()` is the
  data context, and `AnActionEvent.createEvent(action, context, null, "EditorTabsToolbar", ActionUiKind.TOOLBAR,
  null)` the event. `ActionUtil.updateAction(action, event)` runs the `update`, after which
  `event.presentation.isEnabledAndVisible` says whether the button is offered. `ActionUtil.performAction(action,
  event)` inside `runInEdtAndWait` presses it (trial).
* **The files the tests use.** A `LightVirtualFile(name, fileType, text)` is enough where only the type and the
  content matter. The Markdown type is `FileTypeManager.getInstance().findFileTypeByName("Markdown")`, and the plain
  text type is `PlainTextFileType.INSTANCE`. The test with a change that is not saved uses a real file instead,
  because `FileDocumentManager.isDocumentUnsaved` is never true for a `LightVirtualFile`: it writes `notes.md` into a
  JUnit `TemporaryFolder`, finds it with `LocalFileSystem.getInstance().refreshAndFindFileByNioFile`, gets its
  document inside a read action, and appends to the document inside
  `WriteCommandAction.runWriteCommandAction(null) { ... }` on the event dispatch thread (trial).
* **The icon is drawn for the plugin.** The platform ships no loudspeaker: a search of every jar of IntelliJ IDEA
  2026.2.3 for icons named after volume, speaker, sound, audio, speech or voice found none. The plugin brings two
  16 by 16 SVG files, in the stroke style and the two greys of the platform's new UI icons. `plugin.xml` names the
  light one in the action's `icon` attribute as a resource path, and the platform picks the file with the `_dark`
  suffix in a dark theme by itself. There is no icons class, because one icon with one user needs none. The trial
  packaged both files, and its test saw an icon on the action; nobody has looked at the drawing yet.

  `readAloud.svg`, and `readAloud_dark.svg` is the same with `#CED0D6` in place of `#6C707E`:

  ```xml
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M2.5 6.5H4.5L8 3.5V12.5L4.5 9.5H2.5V6.5Z" stroke="#6C707E" stroke-linejoin="round"/>
  <path d="M10.5 6C11.2 6.5 11.5 7.2 11.5 8C11.5 8.8 11.2 9.5 10.5 10" stroke="#6C707E" stroke-linecap="round"/>
  <path d="M12.3 4C13.4 5 14 6.4 14 8C14 9.6 13.4 11 12.3 12" stroke="#6C707E" stroke-linecap="round"/>
  </svg>
  ```
* **The tooltip is the action's text.** A toolbar button shows the `text` attribute of its action as its tooltip, so
  `text="Read Aloud"` in `plugin.xml` answers requirement 4. There is no resource bundle, because translations are a
  non-goal.
* **Names that the spec leaves to the implementation.** The action class is `ReadAloudAction` and its id is
  `com.ritenoar.aloudrite.ReadAloud`. The dialog class is `FileContentDialog`. The event parameter of the action's
  methods is named `event`: the human read the `e` of unit 0001 as breaking the naming rule in `process.md`, as the
  record of this unit notes.
* **The action updates in the background.** `getActionUpdateThread()` returns `ActionUpdateThread.BGT`. The `update`
  reads only the file and its type, which needs no event dispatch thread.
* **Build on the host.** The agent sandbox still admits none of the hosts the Gradle build reaches. Unit 0002, which
  would change that, is a draft.

## Files

- `build.gradle.kts`: add `testBundledPlugin("org.intellij.plugins.markdown")` to the `intellijPlatform` dependencies
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: new, the tests
- `src/test/kotlin/com/ritenoar/aloudrite/SayHelloActionTest.kt`: deleted
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: new, the action
- `src/main/kotlin/com/ritenoar/aloudrite/FileContentDialog.kt`: new, the dialog
- `src/main/kotlin/com/ritenoar/aloudrite/SayHelloAction.kt`: deleted
- `src/main/resources/icons/readAloud.svg`, `src/main/resources/icons/readAloud_dark.svg`: new, the icon
- `src/main/resources/META-INF/plugin.xml`: the `ReadAloud` action replaces the `SayHello` action
- `README.md`: the second paragraph and the sentence on dependencies

## Tasks

- [x] Add `testBundledPlugin("org.intellij.plugins.markdown")` under `testFramework(TestFrameworkType.Platform)` in
      `build.gradle.kts` (R12)
- [x] Delete `SayHelloActionTest.kt` and write `ReadAloudActionTest`, a JUnit 4 test class under the
      `ApplicationRule` that opens no project, looks the action up by the id `com.ritenoar.aloudrite.ReadAloud`
      through `ActionManager`, and has these tests. Run `./gradlew test` and see it fail to compile because
      `FileContentDialog` does not exist (R12):
    - [x] the file type named `Markdown` is registered in the test environment and is the type of a file named
          `notes.md`, so that a missing Markdown plugin fails here by name and not in the other tests (R12)
    - [x] the action is a child of the group `EditorTabsToolbarActions`, its template text is `Read Aloud`, and its
          template presentation has an icon (R1, R4, R5)
    - [x] the button is offered for a file of the Markdown type (R1, R12)
    - [x] the button is not offered for a `notes.txt` of the plain text type, not for a `notes.md` of the plain text
          type, and not for an event without a file (R3, R11)
    - [x] for a `notes.md` on disk holding `# Saved\n`, whose document has had
          `<b>not saved</b> &amp; *x*\n\n  indented\n` appended and not saved, pressing the button shows one dialog
          whose title is `notes.md` and whose content is the two texts joined, character for character; afterwards
          the file on disk still holds `# Saved\n` and `FileDocumentManager.isDocumentUnsaved` is true for the
          document (R6, R7)
    - [x] for an empty `empty.md`, pressing the button shows one dialog whose title is `empty.md` and whose content
          is the empty string (R9)
    - [x] for a file of 20,000 long lines, the content of the dialog equals the whole text (R8)
    - [x] `ActionManager` returns no action for the id `com.ritenoar.aloudrite.SayHello`, and no child of the group
          `ToolsMenu` has the template text `Say Hello` (R10)
- [x] Write `FileContentDialog(project: Project?, fileName: String, val content: String)`, a `DialogWrapper` whose
      title is the file name, whose center panel is a `JBScrollPane` around a `JBTextArea` that holds the content,
      is not editable, has 25 visible rows and 80 visible columns as named constants and its caret at position 0 so
      that it opens scrolled to the top, and whose only button is OK (R6, R7, R8, R9)
- [x] Add the two icon files under `src/main/resources/icons/` with the content given under "Approach" (R5)
- [x] Write `ReadAloudAction`, an `AnAction` that updates on `ActionUpdateThread.BGT`. Its `update` sets
      `event.presentation.isEnabledAndVisible` to whether the event holds a `CommonDataKeys.VIRTUAL_FILE` whose file
      type is named `Markdown`. Its `actionPerformed` returns early when the event holds no file or the file has no
      document, and otherwise shows a `FileContentDialog` with `event.project`, the name of the file and the text of
      its document (R1, R2, R3, R6, R7, R11)
- [x] In `plugin.xml`, replace the `SayHello` action with
      `<action id="com.ritenoar.aloudrite.ReadAloud" class="com.ritenoar.aloudrite.ReadAloudAction"
      text="Read Aloud" icon="/icons/readAloud.svg">` holding `<add-to-group group-id="EditorTabsToolbarActions"/>`,
      and delete `SayHelloAction.kt`. The `<depends>` stays the only one. Run `./gradlew test` and see every test
      pass (R1, R2, R4, R5, R10, R11)
- [x] Rewrite the second paragraph of `README.md`, which describes "Say Hello": the plugin still reads nothing
      aloud; it adds a "Read Aloud" button with a loudspeaker icon to the editor tab row of a Markdown file, beside
      the icons that switch between editor and preview, and pressing it shows the content of that file, unsaved
      changes included, in a dialog; the dialog is a placeholder for the reading. In the third paragraph, add that
      the button appears only where the IDE's Markdown plugin is enabled, and leave the statement that the plugin
      depends on nothing beyond the platform. Leave every other section as it is (R13)

## Verification

- [x] `./gradlew clean buildPlugin` succeeds and `./gradlew test` passes, with only JDK 25 on the path (R12)
- [x] The `META-INF/plugin.xml` inside the jar of `build/distributions/aloudrite-intellij-0.0.1.zip` holds
      `com.intellij.modules.platform` as its only `<depends>`, and the jar holds both icon files and no class of
      the Markdown plugin (R5, R11)
- [x] `grep -rn -i 'hello' src README.md` finds nothing (R10, R13)
- [ ] `npm run check-changes` passes on the committed branch
- [ ] In the sandbox IDE started with `./gradlew runIde`, with a Markdown file open: the loudspeaker button is at
      the right end of the editor tab row beside the three layout icons; it stays there in each of "Editor",
      "Editor and Preview" and "Preview"; its tooltip is "Read Aloud"; and it is gone when the tab of a file that
      is not Markdown is selected. This needs a display. Where the implementer has none, log that in `record.md`
      and leave the check to the review, as the spec assigns it (R1, R2, R3, R4)
- [ ] There, the icon reads as a loudspeaker in a light and in a dark theme. This is the human's check: where the
      human rejects the drawing, the two SVG files are redrawn and nothing else changes (R5)
- [ ] There, after typing into a Markdown file without saving, pressing the button shows a dialog titled with the
      file name that holds the text as typed. With a Markdown file longer than the screen, the dialog stays within
      the screen, scrolls to the last line and closes with OK. With an empty Markdown file, it opens empty. With the
      editor split and a different Markdown file in each half, each button shows the file of its own half (R6, R7,
      R8, R9)
- [ ] There, Tools holds no "Say Hello" (R10)
- [ ] There, with the Markdown plugin disabled under Settings, Plugins and the IDE restarted, the IDE reports no
      error for AloudRite and a `.md` file shows no button. Enable the Markdown plugin again afterwards (R11)

## Commit Points

- [x] `feature-0003 Replace Say Hello with the Read Aloud tab row button`: the test dependency, the tests, the
      dialog, the icons, the action and its registration, and the removal of `SayHelloAction` and its test. The
      tests pass
- [x] `feature-0003 Describe the Read Aloud button in the README`: `README.md`
