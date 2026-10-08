package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class ReadAloudActionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

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
    fun thePluginRegistersNoActionButTheButton() {
        val actionIds = ActionManager.getInstance().getActionIdList("com.ritenoar.aloudrite.")

        assertEquals(listOf(READ_ALOUD_ACTION_ID), actionIds)
    }

    @Test
    fun pressingTheButtonWithoutTheModelShowsWhereItIsMissingAndSpeaksNothing() {
        val emptyFolder = temporaryFolder.newFolder().toPath()
        val spoken = mutableListOf<String>()
        val action = speakingInto(spoken, "Linux", "amd64", emptyFolder)

        val messages = pressTheButtonOn(action, markdownFile("# Title"))

        assertEquals(1, messages.size)
        assertTrue(messages.single(), "model is missing" in messages.single())
        assertTrue(messages.single(), emptyFolder.toAbsolutePath().toString() in messages.single())
        assertTrue(messages.single(), "README" in messages.single())
        assertTrue(spoken.isEmpty())
    }

    @Test
    fun pressingTheButtonOnAnUnsupportedPlatformSaysSoAndSpeaksNothing() {
        val modelFolder = temporaryFolder.newFolder().toPath()
        fillWithModelStandIn(modelFolder)
        val spoken = mutableListOf<String>()
        val action = speakingInto(spoken, "Windows 11", "amd64", modelFolder)
        val file = markdownFile("# Title")

        val offered = isOfferedFor(action, file)
        val messages = pressTheButtonOn(action, file)

        assertTrue(offered)
        assertEquals(
            listOf("Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon."),
            messages)
        assertTrue(spoken.isEmpty())
    }

    @Test
    fun pressingTheButtonSpeaksTheSameSentenceWhateverTheFileHolds() {
        val modelFolder = temporaryFolder.newFolder().toPath()
        fillWithModelStandIn(modelFolder)
        val spoken = mutableListOf<String>()
        val action = speakingInto(spoken, "Linux", "amd64", modelFolder)

        val messagesForTextFile = pressTheButtonOn(action, markdownFile("# Title\n\nSome text"))
        val messagesForEmptyFile = pressTheButtonOn(action, markdownFile(""))

        val sentence = "AloudRite can speak. This sentence was made on your own machine."
        assertTrue(messagesForTextFile.isEmpty())
        assertTrue(messagesForEmptyFile.isEmpty())
        assertEquals(listOf(sentence, sentence), spoken)
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

    private fun isOfferedFor(file: VirtualFile?): Boolean = isOfferedFor(readAloudAction(), file)

    private fun isOfferedFor(action: AnAction, file: VirtualFile?): Boolean {
        val event = eventFor(action, file)
        ActionUtil.updateAction(action, event)
        return event.presentation.isEnabledAndVisible
    }

    private fun markdownFile(text: String): VirtualFile = LightVirtualFile("notes.md", markdownFileType(), text)

    private fun speakingInto(
        spoken: MutableList<String>,
        operatingSystemName: String,
        architectureName: String,
        modelFolder: Path,
    ) = ReadAloudAction(operatingSystemName, architectureName, modelFolder) { text, _, _ -> spoken += text }

    private fun pressTheButtonOn(action: AnAction, file: VirtualFile): List<String> {
        val messages = mutableListOf<String>()
        val previousDialog = TestDialogManager.setTestDialog(TestDialog { message ->
            messages += message
            Messages.OK
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

    companion object {
        // Opening a project fails in the test environment of IntelliJ IDEA 2026.2.3, so only the application is started
        @JvmField
        @ClassRule
        val application = ApplicationRule()

        private const val READ_ALOUD_ACTION_ID = "com.ritenoar.aloudrite.ReadAloud"
    }
}
