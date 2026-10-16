package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Assert.assertNotNull

fun markdownFileType(): FileType {
    val markdownFileType = FileTypeManager.getInstance().findFileTypeByName("Markdown")
    assertNotNull("The file type Markdown is not registered", markdownFileType)
    return markdownFileType!!
}

fun markdownFile(text: String): VirtualFile = LightVirtualFile("notes.md", markdownFileType(), text)

fun eventFor(action: AnAction, file: VirtualFile?): AnActionEvent {
    val dataContextBuilder = SimpleDataContext.builder()
    if (file != null) dataContextBuilder.add(CommonDataKeys.VIRTUAL_FILE, file)
    return AnActionEvent.createEvent(
        action, dataContextBuilder.build(), null, "EditorTabsToolbar", ActionUiKind.TOOLBAR, null)
}

// Returns the messages that the press showed. A dialog is answered with what answerTo gives for its message
fun pressTheButtonOn(action: AnAction, file: VirtualFile, answerTo: (message: String) -> Int = { Messages.OK }): List<String> {
    val messages = mutableListOf<String>()
    val previousDialog = TestDialogManager.setTestDialog(TestDialog { message ->
        messages += message
        answerTo(message)
    })
    try {
        runInEdtAndWait {
            ActionUtil.performAction(action, eventFor(action, file))
        }
    } finally {
        TestDialogManager.setTestDialog(previousDialog)
    }
    return messages
}
