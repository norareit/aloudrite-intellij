package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndWait
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.InputStream
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
    fun theTabRowHoldsTheActionOfTheReadingButtonsWhoseGroupHoldsReadAloudAndStopReading() {
        val actionManager = ActionManager.getInstance()
        val tabRowGroup = actionManager.getAction("EditorTabsToolbarActions") as DefaultActionGroup
        val tabRowActionIds = tabRowGroup.childActionsOrStubs.map { actionManager.getId(it) }
        val buttonGroup = actionManager.getAction(READING_BUTTON_GROUP_ID) as DefaultActionGroup

        assertTrue("The group EditorTabsToolbarActions does not contain $READING_BUTTONS_ACTION_ID",
            READING_BUTTONS_ACTION_ID in tabRowActionIds)
        assertFalse(READ_ALOUD_ACTION_ID in tabRowActionIds)
        assertEquals(listOf(READ_ALOUD_ACTION_ID, STOP_READING_ACTION_ID),
            buttonGroup.childActionsOrStubs.map { actionManager.getId(it) })
        assertEquals("Read Aloud, Pause or Resume", registeredReadAloudAction().templateText)
        assertNotNull("The action has no icon", registeredReadAloudAction().templatePresentation.icon)
        assertEquals("Stop Reading", registeredAction(STOP_READING_ACTION_ID).templateText)
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
    fun thePluginRegistersTheActionsOfTheTabButtonsAndNoOther() {
        val actionIds = ActionManager.getInstance().getActionIdList("com.ritenoar.aloudrite.")

        assertEquals(
            listOf(READ_ALOUD_ACTION_ID, READING_BUTTONS_ACTION_ID, READING_BUTTON_GROUP_ID, STOP_READING_ACTION_ID).sorted(),
            actionIds.sorted())
    }

    @Test
    fun aPressOnAFileWithTextReadsThatText() {
        val action = readingAction()

        val messages = pressTheButtonOn(action, markdownFile("# Title\n\nSome text."))
        standIns.runKeptWorks()

        assertTrue(messages.isEmpty())
        assertEquals(listOf("Title", "<silence 500 ms>", "Some text."), standIns.heard())
    }

    @Test
    fun aPressWithTheFp32ModelInUseAndInPlaceReadsWithTheFp32Model() {
        val modelsFolder = temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(FP32_MODEL.folderIn(it), FP32_MODEL) }
        val action = readAloudActionOn(standIns.service, idleDownloads, modelsFolder, modelInUse = { FP32_MODEL })

        val messages = pressTheButtonOn(action, markdownFile("# Title\n\nSome text."))
        standIns.runKeptWorks()

        assertTrue(messages.isEmpty())
        assertEquals(listOf(FP32_MODEL), standIns.modelsMade)
        assertEquals(listOf("Title", "<silence 500 ms>", "Some text."), standIns.heard())
    }

    @Test
    fun aPressOnAFileOfNothingButMarkdownThatIsNotReadReadsNothingAndSaysSo() {
        val action = readingAction()

        val messages = FILES_THAT_READ_NOTHING.flatMap { pressTheButtonOn(action, markdownFile(it)) }

        assertTrue(messages.isEmpty())
        assertNothingToReadWasShownAndNothingRead(FILES_THAT_READ_NOTHING.size)
    }

    @Test
    fun aPressOnAFileOfNothingButMarkdownThatIsNotReadWithoutTheModelAsksAndDownloadsNothing() {
        val action = readingAction(temporaryFolder.newFolder().toPath())

        val messages = FILES_THAT_READ_NOTHING.flatMap { pressTheButtonOn(action, markdownFile(it)) }

        assertTrue(messages.isEmpty())
        assertNothingToReadWasShownAndNothingRead(FILES_THAT_READ_NOTHING.size)
        assertTrue(keptWorks.isEmpty())
    }

    @Test
    fun aPressOnAFileOfNothingButACodeBlockReadsCodeExample() {
        val action = readingAction()

        val messages = pressTheButtonOn(action, markdownFile("```\nval x = 1\n```\n"))
        standIns.runKeptWorks()

        assertTrue(messages.isEmpty())
        assertTrue(notifications.isEmpty())
        assertEquals(listOf("code example"), standIns.heard())
    }

    @Test
    fun aPressChangesNeitherTheDocumentNorTheFileOnDisk() {
        val markdown = "---\ntitle: Notes\n---\n# Title\n\n- [ ] A *task* <!-- note -->\n\n```\ncode\n```\n"
        val file = temporaryFolder.newFile("notes.md").apply { writeText(markdown) }
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile)!! }

        pressTheButtonOn(readingAction(), virtualFile)
        standIns.runKeptWorks()

        assertEquals(listOf("Title", "<silence 500 ms>", "A task.", "<silence 500 ms>", "code example"), standIns.heard())
        assertEquals(markdown, runReadAction { document.text })
        assertEquals(markdown, file.readText())
        assertFalse(FileDocumentManager.getInstance().isDocumentUnsaved(document))
    }

    @Test
    fun aPressReadsTheChangesThatAreNotSavedAndNoLaterChange() {
        val action = readingAction()
        val file = markdownFile("The saved text.")
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
        changeInAWriteCommand(document, "The text that is not saved.")

        pressTheButtonOn(action, file)
        changeInAWriteCommand(document, "A change after the press.")
        standIns.runKeptWorks()

        assertEquals(listOf("The text that is not saved."), standIns.textsMade)
        assertEquals("The saved text.", VfsUtilCore.loadText(file))
    }

    @Test
    fun whileNothingIsReadTheFirstButtonShowsReadAloudAlsoWhileAnotherFileIsReadAndNoTabShowsTheStopButton() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One.")
        val otherFile = markdownFile("Other.")

        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
        assertNull(visiblePresentationFor(stop, otherFile))

        pressTheButtonOn(action, otherFile)
        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
    }

    @Test
    fun fromThePressOnTheFirstButtonShowsPauseReadingAndTheStopButtonShowsOnTheTabOfThatFileOnly() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        val otherFile = markdownFile("Other.")

        pressTheButtonOn(action, file)

        assertEquals(PAUSE_READING, presentationFor(action, file))
        assertEquals(STOP_READING, visiblePresentationFor(stop, file))
        assertNull(visiblePresentationFor(stop, otherFile))
        assertEquals(READ_ALOUD, presentationFor(action, otherFile))
    }

    @Test
    fun aPressWhileItReadsPausesAndTheFirstButtonShowsResumeReadingWhileTheStopButtonStays() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)
        pressOnceAtSlice(action, file, slice = 2)

        standIns.runKeptWorks()

        assertTrue(standIns.service.isPaused(ReadingTarget(null, file)))
        assertEquals(2, standIns.players.single().played.size)
        assertEquals(RESUME_READING, presentationFor(action, file))
        assertEquals(STOP_READING, visiblePresentationFor(stop, file))
    }

    @Test
    fun aPressWhilePausedResumesAndTheFirstButtonShowsPauseReadingAgain() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)
        pressOnceAtSlice(action, file, slice = 2)
        standIns.runKeptWorks()

        pressTheButtonOn(action, file)

        assertEquals(PAUSE_READING, presentationFor(action, file))
        assertEquals(STOP_READING, visiblePresentationFor(stop, file))
        standIns.runKeptWorks()
        assertEquals(listOf("One. Two."), standIns.heard())
        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
    }

    @Test
    fun aPressOnTheStopButtonWhileItReadsEndsTheReading() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)

        val messages = pressTheButtonOn(stop, file)
        standIns.runKeptWorks()

        assertTrue(messages.isEmpty())
        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
        assertTrue(standIns.textsMade.isEmpty())
        assertTrue(standIns.keptWorks.isEmpty())
    }

    @Test
    fun aPressOnTheStopButtonWhilePausedEndsTheReading() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)
        pressOnceAtSlice(action, file, slice = 2)
        standIns.runKeptWorks()

        pressTheButtonOn(stop, file)

        assertFalse(standIns.service.isReading(ReadingTarget(null, file)))
        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
        assertTrue(standIns.keptWorks.isEmpty())
    }

    @Test
    fun aChangeOfTheDocumentWhilePausedDoesNotReachWhatIsHeardAfterTheResume() {
        val action = readingAction()
        val file = markdownFile("Before the pause.")
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(file)!! }
        pressTheButtonOn(action, file)
        pressOnceAtSlice(action, file, slice = 2)
        standIns.runKeptWorks()

        changeInAWriteCommand(document, "A change while paused.")
        pressTheButtonOn(action, file)
        standIns.runKeptWorks()

        assertEquals(listOf("Before the pause."), standIns.textsMade)
        assertEquals(listOf("Before the pause."), standIns.heard())
    }

    @Test
    fun closingTheTabOfAPausedReadingEndsIt() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)
        pressOnceAtSlice(action, file, slice = 2)
        standIns.runKeptWorks()

        standIns.service.fileClosed(ReadingTarget(null, file))

        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
    }

    @Test
    fun aPressOnAnotherFileEndsAPausedReadingAndReadsTheOtherFile() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        val otherFile = markdownFile("Other.")
        pressTheButtonOn(action, file)
        pressOnceAtSlice(action, file, slice = 2)
        standIns.runKeptWorks()

        pressTheButtonOn(action, otherFile)

        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
        assertEquals(PAUSE_READING, presentationFor(action, otherFile))
        standIns.runKeptWorks()
        assertEquals(listOf("One. ", "Two.", "Other."), standIns.textsMade)
        assertEquals(List(STAND_IN_PIECES_PER_TEXT * STAND_IN_SAMPLES_PER_PIECE) { 3f },
            standIns.players.last().played.flatMap { it.toList() })
    }

    @Test
    fun aReadingPausedAndResumedSeveralTimesEndsAtTheEndOfTheFile() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two. Three. Four.")
        pressTheButtonOn(action, file)
        // A piece plays as six slices, so each pause at slice 7 is one piece further on
        var presses = 0
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == 7 && presses < 3) {
                presses++
                pressTheButtonOn(action, file)
            }
        }

        standIns.runKeptWorks()
        while (standIns.service.isPaused(ReadingTarget(null, file))) {
            assertEquals(RESUME_READING, presentationFor(action, file))
            pressTheButtonOn(action, file)
            standIns.runKeptWorks()
        }

        assertEquals(3, presses)
        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
        assertTrue(standIns.players.last().closed)
    }

    @Test
    fun aFailureWhilePausedPutsBothButtonsBack() {
        val action = readingAction()
        val stop = stopReadingActionOn(standIns.service)
        val file = markdownFile("One. Two.")
        standIns.whileMaking = { text -> if (text == "Two.") pressTheButtonOn(action, file) }
        standIns.engineFailsAt = { it == "Two." }
        pressTheButtonOn(action, file)

        standIns.runKeptWorks()

        assertEquals(file, standIns.failures.single().first.file)
        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertNull(visiblePresentationFor(stop, file))
    }

    @Test
    fun noIconOfEitherButtonInAnyStateIsRunOrTheResumeOfTheIde() {
        val icons = listOf(READ_ALOUD, PAUSE_READING, RESUME_READING, STOP_READING).map { it.second }

        assertTrue(icons.none { it === AllIcons.Actions.Execute || it === AllIcons.Actions.Resume })
        assertTrue(icons.none { it.toString() == AllIcons.Actions.Execute.toString() })
        assertTrue(icons.none { it.toString() == AllIcons.Actions.Resume.toString() })
    }

    // Presses the button on the file once, from the playing thread, at the given slice of the player that plays then
    private fun pressOnceAtSlice(action: AnAction, file: VirtualFile, slice: Int) {
        var pressed = false
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == slice && !pressed) {
                pressed = true
                pressTheButtonOn(action, file)
            }
        }
    }

    @Test
    fun theButtonShowsReadAloudAgainAtTheEndOfTheFile() {
        val action = readingAction()
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)

        standIns.runKeptWorks()

        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertTrue(standIns.players.single().closed)
    }

    @Test
    fun aPressOnAnotherFileStopsTheFirstAndReadsTheSecond() {
        val action = readingAction()
        val firstFile = markdownFile("First.")
        val secondFile = markdownFile("Second.")

        pressTheButtonOn(action, firstFile)
        pressTheButtonOn(action, secondFile)
        standIns.runKeptWorks()

        assertEquals(listOf("Second."), standIns.textsMade)
        assertEquals(READ_ALOUD, presentationFor(action, firstFile))

        pressTheButtonOn(action, firstFile)
        pressTheButtonOn(action, secondFile)
        assertEquals(READ_ALOUD, presentationFor(action, firstFile))
        assertEquals(PAUSE_READING, presentationFor(action, secondFile))
    }

    @Test
    fun closingTheTabOfTheFilePutsTheButtonBackAndPlaysNoMore() {
        val action = readingAction()
        val file = markdownFile("One. Two.")
        pressTheButtonOn(action, file)

        standIns.service.fileClosed(ReadingTarget(null, file))
        standIns.runKeptWorks()

        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertTrue(standIns.textsMade.isEmpty())
        assertTrue(standIns.samplesPlayed().isEmpty())
    }

    @Test
    fun aPressOnAnEmptyFileReadsNothingAndSaysSo() {
        val action = readingAction()

        val messages = pressTheButtonOn(action, markdownFile("")) + pressTheButtonOn(action, markdownFile(" \n\t\n "))

        assertTrue(messages.isEmpty())
        assertNothingToReadWasShownTwiceAndNothingRead()
    }

    @Test
    fun aPressOnAnEmptyFileWithoutTheModelAsksAndDownloadsNothing() {
        val action = readingAction(temporaryFolder.newFolder().toPath())

        val messages = pressTheButtonOn(action, markdownFile("")) + pressTheButtonOn(action, markdownFile(" \n\t\n "))

        assertTrue(messages.isEmpty())
        assertNothingToReadWasShownTwiceAndNothingRead()
        assertTrue(keptWorks.isEmpty())
    }

    @Test
    fun aPressOnAnEmptyFileLeavesTheReadingOfAnotherFileGoing() {
        val action = readingAction()
        val file = markdownFile("Text.")
        pressTheButtonOn(action, file)

        pressTheButtonOn(action, LightVirtualFile("empty.md", markdownFileType(), ""))
        standIns.runKeptWorks()

        assertEquals(listOf("Text."), standIns.textsMade)
        assertTrue(standIns.players.single().closed)
    }

    @Test
    fun aFailureDuringAReadingPutsTheButtonBackAndTheNextPressReads() {
        val action = readingAction()
        val file = markdownFile("Text.")
        standIns.engineFailsAt = { true }
        pressTheButtonOn(action, file)

        standIns.runKeptWorks()

        assertEquals(READ_ALOUD, presentationFor(action, file))
        assertEquals(file, standIns.failures.single().first.file)
        standIns.engineFailsAt = { false }
        pressTheButtonOn(action, file)
        standIns.runKeptWorks()
        assertEquals(listOf("Text.", "Text."), standIns.textsMade)
        assertEquals(1, standIns.failures.size)
    }

    @Test
    fun aFileWhoseTextCannotBeLoadedSaysSoAndReadsAndDownloadsNothing() {
        // A file of a binary type has no document, so its text is loaded from disk, which fails here
        val unreadableFile = object : LightVirtualFile("notes.md", UnknownFileType.INSTANCE, "") {
            override fun getLength(): Long = 1
            override fun contentsToByteArray(): ByteArray = throw IOException("The disk is gone")
            override fun getInputStream(): InputStream = throw IOException("The disk is gone")
        }

        val messagesWithTheModel = pressTheButtonOn(readingAction(), unreadableFile)
        val messagesWithoutTheModel = pressTheButtonOn(readingAction(temporaryFolder.newFolder().toPath()), unreadableFile)

        val message = "AloudRite could not read notes.md aloud: The disk is gone"
        assertEquals(listOf(message), messagesWithTheModel)
        assertEquals(listOf(message), messagesWithoutTheModel)
        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(keptWorks.isEmpty())
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun anEmojiIsGivenToTheEngineAsOneSpace() {
        assertTheEngineIsGiven("Before   after.", forAFileOf = "Before $GRINNING_FACE after.")
    }

    @Test
    fun threeEmojiInARowBetweenTwoWordsAreGivenAsThreeSpaces() {
        assertTheEngineIsGiven("one     two", forAFileOf = "one ${GRINNING_FACE.repeat(3)} two")
    }

    @Test
    fun anEmojiAloneBetweenTwoSentencesIsGivenAsASpace() {
        assertTheEngineIsGiven(
            "First sentence.   Second sentence.", forAFileOf = "First sentence. $GRINNING_FACE Second sentence.")
    }

    @Test
    fun charactersOfTwoAndThreeBytesAreGivenAsTheyAre() {
        assertTheEngineIsGiven("caf\u00E9 costs \u20AC \u2192 more.", forAFileOf = "caf\u00E9 costs \u20AC \u2192 more.")
    }

    @Test
    fun aPressOnAFileOfNothingButEmojiReadsNothingAndSaysSo() {
        val action = readingAction()
        val emojiOnly = " $GRINNING_FACE\n${GRINNING_FACE.repeat(3)} "

        val messages = pressTheButtonOn(action, markdownFile(emojiOnly)) + pressTheButtonOn(action, markdownFile(GRINNING_FACE))

        assertTrue(messages.isEmpty())
        assertNothingToReadWasShownTwiceAndNothingRead()
    }

    @Test
    fun aPressOnAFileOfNothingButEmojiWithoutTheModelAsksAndDownloadsNothing() {
        val action = readingAction(temporaryFolder.newFolder().toPath())
        val emojiOnly = " $GRINNING_FACE\n${GRINNING_FACE.repeat(3)} "

        val messages = pressTheButtonOn(action, markdownFile(emojiOnly)) + pressTheButtonOn(action, markdownFile(GRINNING_FACE))

        assertTrue(messages.isEmpty())
        assertNothingToReadWasShownTwiceAndNothingRead()
        assertTrue(keptWorks.isEmpty())
    }

    private fun assertTheEngineIsGiven(text: String, forAFileOf: String) {
        pressTheButtonOn(readingAction(), markdownFile(forAFileOf))
        standIns.runKeptWorks()

        assertEquals(text, standIns.textsMade.joinToString(""))
    }

    private fun assertNothingToReadWasShownTwiceAndNothingRead() = assertNothingToReadWasShownAndNothingRead(2)

    private fun assertNothingToReadWasShownAndNothingRead(times: Int) {
        assertEquals(times, notifications.size)
        notifications.forEach { notification ->
            assertEquals("AloudRite", notification.groupId)
            assertEquals(NotificationType.INFORMATION, notification.type)
            assertEquals("", notification.title)
            assertEquals("There is nothing to read in notes.md.", notification.content)
            assertTrue(notification.actions.isEmpty())
        }
        assertTrue(standIns.keptWorks.isEmpty())
        assertTrue(standIns.textsMade.isEmpty())
    }

    private fun changeInAWriteCommand(document: Document, text: String) {
        runInEdtAndWait { WriteCommandAction.runWriteCommandAction(null) { document.setText(text) } }
    }

    @Test
    fun sayHelloIsGone() {
        val actionManager = ActionManager.getInstance()
        val toolsMenu = actionManager.getAction("ToolsMenu") as DefaultActionGroup
        val toolsMenuTexts = toolsMenu.childActionsOrStubs.map { it.templateText }

        assertNull(actionManager.getAction("com.ritenoar.aloudrite.SayHello"))
        assertFalse("Say Hello" in toolsMenuTexts)
    }

    private val keptWorks = mutableListOf<() -> Unit>()
    private val standIns = SpeechStandIns()
    private val notifications = mutableListOf<Notification>()
    private val disposable = Disposer.newDisposable()

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

    private val idleDownloads = ModelDownloadService(
        runInBackground = { work -> keptWorks += work },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )

    private fun isOfferedFor(file: VirtualFile?): Boolean = isOfferedFor(registeredReadAloudAction(), file)

    private fun isOfferedFor(action: AnAction, file: VirtualFile?): Boolean {
        val event = eventFor(action, file)
        ActionUtil.updateAction(action, event)
        return event.presentation.isEnabledAndVisible
    }

    private fun readingAction(modelsFolder: Path = folderWithTheModelStandIn()) =
        readAloudActionOn(standIns.service, idleDownloads, modelsFolder)

    private fun folderWithTheModelStandIn(): Path =
        temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) }

    private val READ_ALOUD = "Read Aloud" to AloudRiteIcons.ReadAloud
    private val PAUSE_READING = "Pause Reading" to AllIcons.Actions.Pause
    private val RESUME_READING = "Resume Reading" to AloudRiteIcons.ResumeReading
    private val STOP_READING = "Stop Reading" to AllIcons.Actions.Suspend

    companion object {
        // Opening a project fails in the test environment of IntelliJ IDEA 2026.2.3, so only the application is started
        @JvmField
        @ClassRule
        val application = ApplicationRule()

        private val GRINNING_FACE = String(Character.toChars(0x1F600))

        private val FILES_THAT_READ_NOTHING = listOf(
            "---\ntitle: Notes\ntags: [a, b]\n---\n",
            "<!-- one -->\n\n<!-- two -->",
            "~~All of it struck out.~~\n\n~~And this.~~",
        )
    }
}
