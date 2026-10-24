package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class ReadFromTheCaretTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @get:Rule
    val editors = EditorsOfATest()

    private val standIns = SpeechStandIns()
    private val disposable = Disposer.newDisposable()
    private val notifications = mutableListOf<Notification>()
    private val project: Project = ProjectManager.getInstance().defaultProject
    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )

    // The editor that the key reads, which the IDE takes from the editors of the project
    private var selectedEditor: FileEditor? = null
    private val action by lazy {
        readAloudActionOn(
            standIns.service, idleDownloads, folderWithTheModelStandIn(), selectedEditor = { selectedEditor })
    }

    private val text = "# Title\n\nFirst one. Second two. Third three.\n\nNext paragraph here."
    private val file = markdownFile(text)
    private val target get() = ReadingTarget(null, file)

    @Before
    fun catchTheNotifications() {
        ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                notifications += notification
            }
        })
    }

    @After
    fun cleanUp() {
        Disposer.dispose(disposable)
    }

    private fun offsetOf(marker: String, after: Int = 0) = text.indexOf(marker) + after

    private fun press(editor: FileEditor?, answerTo: (String) -> Int = { 0 }): List<String> =
        pressTheButtonOn(action, file, null, editor, answerTo)

    private fun readText(): String = standIns.textsMade.joinToString("")

    // The press

    @Test
    fun aPressWithASelectionReadsWhatTheSelectionCoversAndNothingElse() {
        press(editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two.") + 4))
        standIns.runKeptWorks()

        assertEquals("Second two.", readText())
            }

    @Test
    fun aPressWithACaretInTheMiddleOfAParagraphReadsFromItsSentenceToTheEndOfTheFile() {
        press(editors.textEditorWithCaretAt(file, offsetOf("cond", 2)))
        standIns.runKeptWorks()

        assertEquals("Second two. Third three.Next paragraph here.", readText())
    }

    @Test
    fun aPressWithTheCaretAtTheStartReadsTheWholeFile() {
        press(editors.textEditorOf(file))
        standIns.runKeptWorks()

        assertEquals("TitleFirst one. Second two. Third three.Next paragraph here.", readText())
    }

    @Test
    fun anEmptySelectionReadsFromTheCaret() {
        val editor = editors.textEditorWithCaretAt(file, offsetOf("Third", 1))
        runInEdtAndWait { editor.editor.selectionModel.setSelection(offsetOf("Third", 1), offsetOf("Third", 1)) }

        press(editor)
        standIns.runKeptWorks()

        assertEquals("Third three.Next paragraph here.", readText())
    }

    @Test
    fun aFileShownOnlyAsAPreviewIsReadWhole() {
        press(editors.previewOnlyEditorOf(file))
        standIns.runKeptWorks()

        assertEquals("TitleFirst one. Second two. Third three.Next paragraph here.", readText())
    }

    @Test
    fun aSelectionThatReadsNothingSaysSoAndReadsAndAsksNothingWithoutTheModel() {
        val frontMatter = markdownFile("---\ntitle: Hello\n---\nBody.")
        val withoutTheModel = readAloudActionOn(standIns.service, idleDownloads, temporaryFolder.newFolder().toPath())

        val messages = pressTheButtonOn(
            withoutTheModel, frontMatter, null, editors.textEditorWithSelection(frontMatter, 4, 9))

        assertTrue(messages.isEmpty())
        assertEquals(listOf("There is nothing to read in notes.md."), notifications.map { it.content })
        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(standIns.textsMade.isEmpty())
    }

    @Test
    fun theSelectionIsReadAsItWasAtThePressAlsoAfterTheDocumentChangedWhilePaused() {
        press(editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two.") + 4))
        press(null)
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
        runInEdtAndWait { WriteCommandAction.runWriteCommandAction(null) { document.setText("Something else.") } }

        press(null)
        standIns.runKeptWorks()

        assertEquals("Second two.", readText())
    }

    @Test
    fun theCaretAndTheSelectionAreTheSameAfterAPressAPauseAResumeAndTheEnd() {
        val editor = editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two.") + 4)
        val before = caretOf(editor)

        press(editor)
        assertEquals(before, caretOf(editor))
        press(null)
        assertEquals(before, caretOf(editor))
        press(null)
        standIns.runKeptWorks()

        assertEquals(before, caretOf(editor))
        assertEquals(false, standIns.service.isReading(target))
    }

    private fun caretOf(editor: TextEditor) = runInEdtAndGet {
        val caret = editor.editor.caretModel.primaryCaret
        Triple(caret.offset, caret.selectionStart, caret.selectionEnd) to editor.editor.caretModel.caretCount
    }

    // The key

    @Test
    fun theKeyWithNothingReadReadsTheSelectionOfTheSelectedEditor() {
        selectedEditor = editors.textEditorWithSelection(file, offsetOf("Third"), offsetOf("three.") + 6)

        pressTheKeyOf(action, project)
        standIns.runKeptWorks()

        assertEquals("Third three.", readText())
    }

    @Test
    fun theKeyWithNothingReadReadsFromTheCaretOfTheSelectedEditor() {
        selectedEditor = editors.textEditorWithCaretAt(file, offsetOf("Third", 2))

        pressTheKeyOf(action, project)
        standIns.runKeptWorks()

        assertEquals("Third three.Next paragraph here.", readText())
    }

    // The tooltip

    private fun tooltipTitleWith(editor: FileEditor?) = titleAndShortcutOf(tooltipFor(action, file, null, editor)!!)

    @Test
    fun theTooltipIsReadAloudWhateverAPressWillReadAndNamesTheShortcut() {
        assertEquals(
            "Read Aloud" to SHORTCUT,
            tooltipTitleWith(editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two."))))
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(editors.textEditorWithCaretAt(file, offsetOf("Third"))))
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(editors.textEditorOf(file)))
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(editors.previewOnlyEditorOf(file)))
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(null))
    }

    @Test
    fun theIconIsThePersonSpeakingWhateverAPressWillRead() {
        val editorsOfTheCases = listOf(
            editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two.")),
            editors.textEditorWithCaretAt(file, offsetOf("Third")),
            editors.textEditorOf(file),
        )

        for (editor in editorsOfTheCases) {
            assertEquals(AloudRiteIcons.ReadAloud, presentationFor(action, file, null, editor).second)
        }
    }

    @Test
    fun theTooltipNamesNoShortcutWhileAnotherFileIsRead() {
        val other = markdownFile("Other file.")
        pressTheButtonOn(action, other, null, null)

        assertEquals(
            "Read Aloud" to null,
            tooltipTitleWith(editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two."))))
        assertEquals("Read Aloud" to null, tooltipTitleWith(editors.textEditorWithCaretAt(file, offsetOf("Third"))))
        assertEquals("Read Aloud" to null, tooltipTitleWith(editors.textEditorOf(file)))
    }

    @Test
    fun theTooltipStaysReadAloudWhenTheCaretMovesOrASelectionIsMade() {
        val editor = editors.textEditorWithCaretAt(file, offsetOf("Third"))
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(editor))

        runInEdtAndWait { editor.editor.caretModel.moveToOffset(0) }
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(editor))

        runInEdtAndWait { editor.editor.selectionModel.setSelection(offsetOf("Second"), offsetOf("two.")) }
        assertEquals("Read Aloud" to SHORTCUT, tooltipTitleWith(editor))
    }

    @Test
    fun theTextsOfPauseResumeAndStopAreAsTheyWere() {
        val editor = editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two."))
        press(editor)
        assertEquals("Pause Reading" to AllIcons.Actions.Pause, presentationFor(action, file, null, editor))

        press(editor)
        assertEquals("Resume Reading" to AloudRiteIcons.ResumeReading, presentationFor(action, file, null, editor))
        assertEquals("Read Aloud, Pause or Resume", registeredReadAloudAction().templateText)
        assertEquals("Stop Reading", registeredAction(STOP_READING_ACTION_ID).templateText)
    }

    // The tool window

    @Test
    fun theToolWindowShowsWhichPartAPressReads() {
        val reading = { standIns.service.partOfTheFile(target) }

        press(editors.textEditorWithSelection(file, offsetOf("Second"), offsetOf("two.")))
        assertEquals(PartOfTheFile.SELECTION, reading())
        press(null)
        assertEquals(PartOfTheFile.SELECTION, reading())
        press(null)
        standIns.service.stop(target)

        press(editors.textEditorWithCaretAt(file, offsetOf("Third")))
        assertEquals(PartOfTheFile.FROM_THE_CARET, reading())
        standIns.service.stop(target)

        press(editors.textEditorOf(file))
        assertEquals(PartOfTheFile.WHOLE_FILE, reading())
    }

    // Several carets and a block selection are not supported, and a press with them reads by the primary caret

    @Test
    fun severalCaretsWithoutSelectionsReadFromThePrimaryCaretAndTheReadingPausesResumesAndStops() {
        val editor = editors.textEditorWithCarets(file, listOf(offsetOf("Next") to null, offsetOf("Third") to null))

        assertReadByThePrimaryCaret(editor, "Third three.Next paragraph here.")
    }

    @Test
    fun severalCaretsWithSelectionsReadTheSelectionOfThePrimaryCaret() {
        val editor = editors.textEditorWithCarets(
            file, listOf(offsetOf("Next") to offsetOf("here"), offsetOf("Second") to offsetOf("two.") + 4))

        assertReadByThePrimaryCaret(editor, "Second two.")
    }

    @Test
    fun aBlockSelectionOverSeveralLinesReadsTheSelectionOfThePrimaryCaretAndTheReadingPausesResumesAndStops() {
        val editor = editors.textEditorWithABlockSelection(file, LogicalPosition(2, 0), LogicalPosition(4, 5))

        assertReadByThePrimaryCaret(editor, expectedForThePrimaryCaretOf(editor))
    }

    // The primary caret of a block selection holds the selection of one line of it, which is what is expected to be read
    private fun expectedForThePrimaryCaretOf(editor: TextEditor): String {
        val (start, end) = runInEdtAndGet {
            editor.editor.caretModel.primaryCaret.let { it.selectionStart to it.selectionEnd }
        }
        assertTrue("The primary caret has no selection", start < end)
        return whatToRead(text, PlaceInTheText.Selection(start, end)).parts
            .filterIsInstance<Words>().joinToString("") { it.text }.also { assertTrue(it.isNotBlank()) }
    }

    // The same through the key

    @Test
    fun theKeyWithSeveralCaretsReadsByThePrimaryCaretAndTheReadingPausesResumesAndStops() {
        selectedEditor = editors.textEditorWithCarets(file, listOf(offsetOf("Next") to null, offsetOf("Third") to null))
        assertReadByTheKey("Third three.Next paragraph here.")
    }

    @Test
    fun theKeyWithSeveralCaretsWithSelectionsReadsTheSelectionOfThePrimaryCaret() {
        selectedEditor = editors.textEditorWithCarets(
            file, listOf(offsetOf("Next") to offsetOf("here"), offsetOf("Second") to offsetOf("two.") + 4))
        assertReadByTheKey("Second two.")
    }

    @Test
    fun theKeyWithABlockSelectionReadsTheSelectionOfThePrimaryCaret() {
        val editor = editors.textEditorWithABlockSelection(file, LogicalPosition(2, 0), LogicalPosition(4, 5))
        selectedEditor = editor
        assertReadByTheKey(expectedForThePrimaryCaretOf(editor))
    }

    private fun assertReadByTheKey(expected: String) {
        // The key reads in the project of its event
        val keyTarget = ReadingTarget(project, file)
        pressTheKeyOf(action, project)
        pressTheKeyOf(action, project)
        assertTrue(standIns.service.isPaused(keyTarget))
        pressTheKeyOf(action, project)
        assertTrue(standIns.service.isReading(keyTarget))
        standIns.runKeptWorks()
        assertEquals(expected, readText())
        pressTheKeyOf(action, project)
        standIns.service.stop(keyTarget)
        assertEquals(false, standIns.service.isReading(keyTarget))
        assertTrue(standIns.failures.isEmpty())
        assertTrue(notifications.isEmpty())
        standIns.textsMade.clear()
        pressTheKeyOf(action, project)
        standIns.runKeptWorks()
        assertEquals(expected, readText())
    }

    private fun assertReadByThePrimaryCaret(editor: TextEditor, expected: String) {
        press(editor)
        press(editor)
        assertTrue(standIns.service.isPaused(target))
        press(editor)
        standIns.runKeptWorks()
        assertEquals(expected, readText())
        press(editor)
        standIns.service.stop(target)
        assertEquals(false, standIns.service.isReading(target))
        assertTrue(standIns.failures.isEmpty())
        assertTrue(notifications.isEmpty())
        assertReadsAgainAfterwards(editor)
    }

    private fun assertReadsAgainAfterwards(editor: TextEditor) {
        standIns.textsMade.clear()
        press(editor)
        standIns.runKeptWorks()
        assertTrue(readText().isNotEmpty())
    }

    private fun folderWithTheModelStandIn(): Path =
        temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) }

    companion object {
        private val SHORTCUT get() = shortcutText("ctrl alt shift SPACE")

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
