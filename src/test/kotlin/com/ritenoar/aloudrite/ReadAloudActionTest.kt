package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.ui.UiInterceptors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class ReadAloudActionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun clearInterceptors() {
        UiInterceptors.clear()
    }

    @Test
    fun markdownFileTypeIsRegisteredAndBelongsToMarkdownFiles() {
        val markdownFileType = markdownFileType()

        assertEquals(markdownFileType, FileTypeManager.getInstance().getFileTypeByFileName("notes.md"))
    }

    @Test
    fun actionIsInTheEditorTabRowWithTextAndIcon() {
        val actionManager = ActionManager.getInstance()
        val action = readAloudAction()
        val tabRowGroup = actionManager.getAction("EditorTabsToolbarActions") as DefaultActionGroup
        val tabRowActionIds = tabRowGroup.childActionsOrStubs.map { actionManager.getId(it) }

        assertTrue("The group EditorTabsToolbarActions does not contain $READ_ALOUD_ACTION_ID",
            READ_ALOUD_ACTION_ID in tabRowActionIds)
        assertEquals("Read Aloud", action.templateText)
        assertNotNull("The action has no icon", action.templatePresentation.icon)
    }

    @Test
    fun buttonIsOfferedForAMarkdownFile() {
        val file = LightVirtualFile("notes.md", markdownFileType(), "# Title")

        assertTrue(isOfferedFor(file))
    }

    @Test
    fun buttonIsNotOfferedForFilesThatAreNotMarkdown() {
        val plainTextFile = LightVirtualFile("notes.txt", PlainTextFileType.INSTANCE, "text")
        val markdownNamedPlainTextFile = LightVirtualFile("notes.md", PlainTextFileType.INSTANCE, "text")

        assertFalse(isOfferedFor(plainTextFile))
        assertFalse(isOfferedFor(markdownNamedPlainTextFile))
        assertFalse(isOfferedFor(null))
    }

    @Test
    fun pressingTheButtonShowsTheTextAsTheEditorHoldsItWithoutSavingIt() {
        val savedText = "# Saved\n"
        val unsavedAddition = "<b>not saved</b> &amp; *x*\n\n  indented\n"
        val file = markdownFileOnDisk("notes.md", savedText)
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
        runInEdtAndWait {
            WriteCommandAction.runWriteCommandAction(null, Runnable {
                document.insertString(document.textLength, unsavedAddition)
            })
        }

        val dialogs = pressTheButtonOn(file)

        assertEquals(1, dialogs.size)
        assertEquals("notes.md", dialogs.single().title)
        assertEquals(savedText + unsavedAddition, dialogs.single().content)
        assertEquals(savedText + unsavedAddition, dialogs.single().displayedText)
        assertEquals(savedText, Files.readString(temporaryFolder.root.toPath().resolve("notes.md")))
        assertTrue(FileDocumentManager.getInstance().isDocumentUnsaved(document))
    }

    @Test
    fun pressingTheButtonOnAnEmptyFileShowsAnEmptyDialog() {
        val file = markdownFileOnDisk("empty.md", "")

        val dialogs = pressTheButtonOn(file)

        assertEquals(1, dialogs.size)
        assertEquals("empty.md", dialogs.single().title)
        assertEquals("", dialogs.single().content)
        assertEquals("", dialogs.single().displayedText)
    }

    @Test
    fun pressingTheButtonOnALongFileShowsAllOfItsText() {
        val text = (1..20_000).joinToString("\n") { "line $it " + "word ".repeat(40) }
        val file = markdownFileOnDisk("long.md", text)

        val dialogs = pressTheButtonOn(file)

        assertEquals(text, dialogs.single().content)
        assertEquals(text, dialogs.single().displayedText)
    }

    @Test
    fun sayHelloIsGone() {
        val actionManager = ActionManager.getInstance()
        val toolsMenu = actionManager.getAction("ToolsMenu") as DefaultActionGroup
        val toolsMenuTexts = toolsMenu.childActionsOrStubs.map { it.templateText }

        assertNull(actionManager.getAction("com.ritenoar.aloudrite.SayHello"))
        assertFalse("Say Hello" in toolsMenuTexts)
    }

    private fun readAloudAction(): AnAction {
        val action = ActionManager.getInstance().getAction(READ_ALOUD_ACTION_ID)
        assertNotNull("The action $READ_ALOUD_ACTION_ID is not registered", action)
        return action
    }

    private fun markdownFileType(): FileType {
        val markdownFileType = FileTypeManager.getInstance().findFileTypeByName("Markdown")
        assertNotNull("The file type Markdown is not registered", markdownFileType)
        return markdownFileType!!
    }

    private fun eventFor(action: AnAction, file: VirtualFile?): AnActionEvent {
        val dataContextBuilder = SimpleDataContext.builder()
        if (file != null) dataContextBuilder.add(CommonDataKeys.VIRTUAL_FILE, file)
        return AnActionEvent.createEvent(
            action, dataContextBuilder.build(), null, "EditorTabsToolbar", ActionUiKind.TOOLBAR, null)
    }

    private fun isOfferedFor(file: VirtualFile?): Boolean {
        val action = readAloudAction()
        val event = eventFor(action, file)
        ActionUtil.updateAction(action, event)
        return event.presentation.isEnabledAndVisible
    }

    private fun markdownFileOnDisk(name: String, text: String): VirtualFile {
        val path = temporaryFolder.newFile(name).toPath()
        Files.writeString(path, text)
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)!!
    }

    private fun pressTheButtonOn(file: VirtualFile): List<ShownDialog> {
        val shownDialogs = mutableListOf<ShownDialog>()
        UiInterceptors.register(object : UiInterceptors.UiInterceptor<FileContentDialog>(FileContentDialog::class.java) {
            override fun doIntercept(component: FileContentDialog) {
                shownDialogs += ShownDialog(component.title, component.content, component.displayedText)
                Disposer.dispose(component.disposable)
            }
        })
        val action = readAloudAction()
        runInEdtAndWait {
            ActionUtil.performAction(action, eventFor(action, file))
        }
        return shownDialogs
    }

    private data class ShownDialog(val title: String, val content: String, val displayedText: String)

    companion object {
        // Opening a project fails in the test environment of IntelliJ IDEA 2026.2.3, so only the application is started
        @JvmField
        @ClassRule
        val application = ApplicationRule()

        private const val READ_ALOUD_ACTION_ID = "com.ritenoar.aloudrite.ReadAloud"
    }
}
