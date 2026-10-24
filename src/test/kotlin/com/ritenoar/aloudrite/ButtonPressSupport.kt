package com.ritenoar.aloudrite

import com.intellij.ide.HelpTooltip
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Assert.assertNotNull
import java.nio.file.Path
import java.awt.Container
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.KeyStroke

const val READ_ALOUD_ACTION_ID = "com.ritenoar.aloudrite.ReadAloud"
const val STOP_READING_ACTION_ID = "com.ritenoar.aloudrite.StopReading"
const val READING_BUTTONS_ACTION_ID = "com.ritenoar.aloudrite.ReadingButtons"
const val READING_BUTTON_GROUP_ID = "com.ritenoar.aloudrite.ReadingButtonGroup"
const val READ_SLOWER_ACTION_ID = "com.ritenoar.aloudrite.ReadSlower"
const val READ_FASTER_ACTION_ID = "com.ritenoar.aloudrite.ReadFaster"

fun markdownFileType(): FileType {
    val markdownFileType = FileTypeManager.getInstance().findFileTypeByName("Markdown")
    assertNotNull("The file type Markdown is not registered", markdownFileType)
    return markdownFileType!!
}

fun markdownFile(text: String): VirtualFile = LightVirtualFile("notes.md", markdownFileType(), text)

fun registeredReadAloudAction(): AnAction = registeredAction(READ_ALOUD_ACTION_ID)

fun registeredAction(id: String): AnAction {
    val action = ActionManager.getInstance().getAction(id)
    assertNotNull("The action $id is not registered", action)
    return action
}

// The text and the icon of the button come from plugin.xml, so the action made here is given those of the
// registered one
fun readAloudActionOn(
    readings: Readings,
    downloads: ModelDownloads,
    modelsFolder: Path,
    modelInUse: () -> SpeechModel = { INT8_MODEL },
    selectedEditor: (Project) -> FileEditor? = { null },
): ReadAloudAction =
    ReadAloudAction("Linux", "amd64", modelsFolder, modelInUse, { readings }, { downloads }, selectedEditor).apply {
        templatePresentation.copyFrom(registeredReadAloudAction().templatePresentation)
    }

fun stopReadingActionOn(readings: Readings): StopReadingAction =
    StopReadingAction { readings }.apply {
        templatePresentation.copyFrom(registeredAction(STOP_READING_ACTION_ID).templatePresentation)
    }

// The file editor is the one that the tab holds, which the tabs of the IDE give as the file editor of the event
fun eventFor(
    action: AnAction, file: VirtualFile?, project: Project? = null, fileEditor: FileEditor? = null,
): AnActionEvent = AnActionEvent.createEvent(
    action, dataContextOf(project, file, fileEditor), null, "EditorTabsToolbar", ActionUiKind.TOOLBAR, null)

// An event of the shortcut of the action, as the keymap makes it, with no toolbar. The file stands for one selected in
// the project view, which the data context of a key holds where the project view has the focus
fun keyEventFor(action: AnAction, project: Project?, file: VirtualFile? = null): AnActionEvent =
    AnActionEvent.createEvent(
        action, dataContextOf(project, file), null, ActionPlaces.KEYBOARD_SHORTCUT, ActionUiKind.NONE, null)

private fun dataContextOf(project: Project?, file: VirtualFile?, fileEditor: FileEditor? = null): DataContext {
    val dataContextBuilder = SimpleDataContext.builder()
    if (file != null) dataContextBuilder.add(CommonDataKeys.VIRTUAL_FILE, file)
    if (fileEditor != null) dataContextBuilder.add(PlatformCoreDataKeys.FILE_EDITOR, fileEditor)
    if (project != null) dataContextBuilder.add(CommonDataKeys.PROJECT, project)
    return dataContextBuilder.build()
}

// Returns the messages that the press showed. A dialog is answered with what answerTo gives for its message
fun pressTheButtonOn(
    action: AnAction,
    file: VirtualFile,
    project: Project? = null,
    fileEditor: FileEditor? = null,
    answerTo: (message: String) -> Int = { Messages.OK },
): List<String> = answeringTheDialogs(answerTo) { ActionUtil.performAction(action, eventFor(action, file, project, fileEditor)) }

// As pressTheButtonOn, for a press of the shortcut of the action, which the keymap performs only where its update
// enabled it. The action is called itself, since ActionUtil asks the project for services a MockProject has not
fun pressTheKeyOf(
    action: AnAction,
    project: Project?,
    file: VirtualFile? = null,
    answerTo: (message: String) -> Int = { Messages.OK },
): List<String> = answeringTheDialogs(answerTo) {
    val event = keyEventFor(action, project, file)
    action.update(event)
    if (event.presentation.isEnabled) action.actionPerformed(event)
}

fun isEnabledByKey(action: AnAction, project: Project?, file: VirtualFile? = null): Boolean =
    keyPresentationOf(action, project, file).isEnabled

// The text and the icon the action shows for a press of its shortcut, as Find Action shows them
fun keyPresentationFor(action: AnAction, project: Project?): Pair<String?, Icon?> =
    keyPresentationOf(action, project, null).let { it.text to it.icon }

private fun keyPresentationOf(action: AnAction, project: Project?, file: VirtualFile?): Presentation {
    val event = keyEventFor(action, project, file)
    action.update(event)
    return event.presentation
}

private fun answeringTheDialogs(answerTo: (message: String) -> Int, press: () -> Unit): List<String> {
    val messages = mutableListOf<String>()
    val previousDialog = TestDialogManager.setTestDialog(TestDialog { message ->
        messages += message
        answerTo(message)
    })
    try {
        runInEdtAndWait(press)
    } finally {
        TestDialogManager.setTestDialog(previousDialog)
    }
    return messages
}

// What the button shows for the file: its text, which is its tooltip, and its icon
fun presentationFor(
    action: AnAction, file: VirtualFile, project: Project? = null, fileEditor: FileEditor? = null,
): Pair<String?, Icon?> {
    val event = eventFor(action, file, project, fileEditor)
    ActionUtil.updateAction(action, event)
    return event.presentation.text to event.presentation.icon
}

// What the button shows for the file, and null where it is not shown
fun visiblePresentationFor(action: AnAction, file: VirtualFile, project: Project? = null): Pair<String?, Icon?>? {
    val event = eventFor(action, file, project)
    ActionUtil.updateAction(action, event)
    return if (event.presentation.isVisible) event.presentation.text to event.presentation.icon else null
}

// The tooltip the button on the tab shows for the file, which the action hands it in its presentation
fun tooltipFor(
    action: AnAction, file: VirtualFile, project: Project? = null, fileEditor: FileEditor? = null,
): HelpTooltip? {
    val event = eventFor(action, file, project, fileEditor)
    ActionUtil.updateAction(action, event)
    return event.presentation.getClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP)
}

// The title of the tooltip and the shortcut it names, or null where it names none, read from the labels it shows. The
// title label holds both, the shortcut in a font element of the grey of a shortcut
fun titleAndShortcutOf(tooltip: HelpTooltip): Pair<String, String?> = runInEdtAndGet {
    val html = labelsIn(tooltip.createTipPanel()).joinToString("") { it.text.orEmpty() }
    val shortcut = SHORTCUT_IN_A_TOOLTIP.find(html)?.groupValues?.get(1)
    val title = html.replace(SHORTCUT_IN_A_TOOLTIP, "").replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()
    StringUtil.unescapeXmlEntities(title) to shortcut
}

private val SHORTCUT_IN_A_TOOLTIP = Regex("<font[^>]*>(.*?)</font>")

private fun labelsIn(container: Container): List<JLabel> = container.components.flatMap { child ->
    when (child) {
        is JLabel -> listOf(child)
        is Container -> labelsIn(child)
        else -> emptyList()
    }
}

// The text the keymap gives the keystroke, as a tooltip shows it
fun shortcutText(keystroke: String): String = KeymapUtil.getShortcutText(KeyboardShortcut(KeyStroke.getKeyStroke(keystroke), null))

// Runs the work with a keymap of the user's active, derived from the active one and changed by change, as the user
// does in Settings › Keymap, and makes the earlier one active again afterwards
fun withAKeymapOfTheUser(change: (Keymap) -> Unit, work: () -> Unit) {
    val keymaps = KeymapManagerEx.getInstanceEx()
    val previous = keymaps.activeKeymap
    val ofTheUser = previous.deriveKeymap("AloudRite test keymap")
    runInEdtAndWait { keymaps.activeKeymap = ofTheUser }
    try {
        runInEdtAndWait { change(ofTheUser) }
        work()
    } finally {
        runInEdtAndWait { keymaps.activeKeymap = previous }
    }
}
