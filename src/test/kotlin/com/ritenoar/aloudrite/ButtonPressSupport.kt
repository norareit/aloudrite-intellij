package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Assert.assertNotNull
import java.nio.file.Path
import javax.swing.Icon

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
    selectedFile: (Project) -> VirtualFile? = { null },
): ReadAloudAction =
    ReadAloudAction("Linux", "amd64", modelsFolder, modelInUse, { readings }, { downloads }, selectedFile).apply {
        templatePresentation.copyFrom(registeredReadAloudAction().templatePresentation)
    }

fun stopReadingActionOn(readings: Readings): StopReadingAction =
    StopReadingAction { readings }.apply {
        templatePresentation.copyFrom(registeredAction(STOP_READING_ACTION_ID).templatePresentation)
    }

fun eventFor(action: AnAction, file: VirtualFile?, project: Project? = null): AnActionEvent =
    AnActionEvent.createEvent(action, dataContextOf(project, file), null, "EditorTabsToolbar", ActionUiKind.TOOLBAR, null)

// An event of the shortcut of the action, as the keymap makes it, with no toolbar. The file stands for one selected in
// the project view, which the data context of a key holds where the project view has the focus
fun keyEventFor(action: AnAction, project: Project?, file: VirtualFile? = null): AnActionEvent =
    AnActionEvent.createEvent(
        action, dataContextOf(project, file), null, ActionPlaces.KEYBOARD_SHORTCUT, ActionUiKind.NONE, null)

private fun dataContextOf(project: Project?, file: VirtualFile?): DataContext {
    val dataContextBuilder = SimpleDataContext.builder()
    if (file != null) dataContextBuilder.add(CommonDataKeys.VIRTUAL_FILE, file)
    if (project != null) dataContextBuilder.add(CommonDataKeys.PROJECT, project)
    return dataContextBuilder.build()
}

// Returns the messages that the press showed. A dialog is answered with what answerTo gives for its message
fun pressTheButtonOn(
    action: AnAction,
    file: VirtualFile,
    project: Project? = null,
    answerTo: (message: String) -> Int = { Messages.OK },
): List<String> = answeringTheDialogs(answerTo) { ActionUtil.performAction(action, eventFor(action, file, project)) }

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
fun presentationFor(action: AnAction, file: VirtualFile, project: Project? = null): Pair<String?, Icon?> {
    val event = eventFor(action, file, project)
    ActionUtil.updateAction(action, event)
    return event.presentation.text to event.presentation.icon
}

// What the button shows for the file, and null where it is not shown
fun visiblePresentationFor(action: AnAction, file: VirtualFile, project: Project? = null): Pair<String?, Icon?>? {
    val event = eventFor(action, file, project)
    ActionUtil.updateAction(action, event)
    return if (event.presentation.isVisible) event.presentation.text to event.presentation.icon else null
}
