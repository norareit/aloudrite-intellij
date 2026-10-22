package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.mock.MockProject
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class ReadingShortcutsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIns = SpeechStandIns()
    private val disposable = Disposer.newDisposable()
    private val notifications = mutableListOf<Notification>()
    private val ownProject: Project = ProjectManager.getInstance().defaultProject
    private val otherProject: Project = MockProject(null, disposable)
    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )

    private val file = markdownFile("One. Two.")
    private val otherMarkdownFile = LightVirtualFile("other.md", markdownFileType(), "Other.")
    private val plainTextFile = LightVirtualFile("notes.txt", PlainTextFileType.INSTANCE, "Plain.")

    // The file of the selected editor, which the IDE takes from the editors of the project
    private var selectedFile: VirtualFile? = null
    private val action by lazy { readingAction(folderWithTheModelStandIn()) }
    private val stop by lazy { stopReadingActionOn(standIns.service) }

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

    @Test
    fun theTabActionsAreNamedForWhatTheirKeysDoAndEveryActionOfThePluginHasANameOfItsOwn() {
        val actionManager = ActionManager.getInstance()
        val texts = actionManager.getActionIdList("com.ritenoar.aloudrite.").mapNotNull { actionManager.getAction(it).templateText }

        assertEquals("Read Aloud, Pause or Resume", registeredReadAloudAction().templateText)
        assertEquals("Stop Reading", registeredAction(STOP_READING_ACTION_ID).templateText)
        assertEquals(texts.distinct(), texts)
    }

    @Test
    fun outsideTheTabTheActionsShowTheirNames() {
        assertEquals("Read Aloud, Pause or Resume" to AloudRiteIcons.ReadAloud, keyPresentationFor(action, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, keyPresentationFor(stop, ownProject))

        pressTheButtonOn(action, file, ownProject)

        assertEquals("Read Aloud, Pause or Resume" to AloudRiteIcons.ReadAloud, keyPresentationFor(action, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, keyPresentationFor(stop, ownProject))
    }

    @Test
    fun withNothingReadTheKeyReadsTheMarkdownFileOfTheSelectedEditorAndNotTheFileSelectedInTheProjectView() {
        selectedFile = file

        val messages = pressTheKeyOf(action, ownProject, otherMarkdownFile)

        assertTrue(messages.isEmpty())
        assertTrue(standIns.service.isReading(ReadingTarget(ownProject, file)))
        standIns.runKeptWorks()
        assertEquals(listOf("One. ", "Two."), standIns.textsMade)
    }

    @Test
    fun withNoModelInItsFolderTheKeyAsksAboutTheDownloadAsTheTabButtonDoes() {
        val modelsFolder = temporaryFolder.newFolder().toPath()
        val withoutTheModel = readingAction(modelsFolder)
        selectedFile = file

        val messagesOfTheKey = pressTheKeyOf(withoutTheModel, ownProject) { Messages.NO }
        val messagesOfTheButton = pressTheButtonOn(withoutTheModel, file, ownProject) { Messages.NO }

        assertEquals(listOf(downloadQuestion(INT8_MODEL, INT8_MODEL.folderIn(modelsFolder))), messagesOfTheKey)
        assertEquals(messagesOfTheButton, messagesOfTheKey)
        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
    }

    @Test
    fun withNothingReadTheKeyDoesNothingForASelectedFileThatIsNotMarkdownOrWithNoSelectedEditor() {
        for (selected in listOf(plainTextFile, null)) {
            selectedFile = selected

            val messages = pressTheKeyOf(action, ownProject, otherMarkdownFile)

            assertTrue(messages.isEmpty())
            assertNull(standIns.service.currentTarget())
        }
        assertTrue(notifications.isEmpty())
        assertTrue(standIns.keptWorks.isEmpty())
    }

    @Test
    fun whileAFileIsReadTheKeyPausesAndResumesItWhereverTheFocusIs() {
        val target = ReadingTarget(ownProject, file)
        pressTheButtonOn(action, file, ownProject)

        for ((project, selected) in placesOfTheFocus()) {
            selectedFile = selected

            pressTheKeyOf(action, project, selected)
            assertTrue("Paused from $selected in ${project.name}", standIns.service.isPaused(target))

            pressTheKeyOf(action, project, selected)
            assertTrue("Resumed from $selected in ${project.name}", standIns.service.isReading(target))
            assertFalse("Resumed from $selected in ${project.name}", standIns.service.isPaused(target))
        }
        assertEquals(target, standIns.service.currentTarget())
    }

    @Test
    fun theKeyOfStopReadingStopsAReadingAndAPausedReadingWhereverTheFocusIs() {
        val target = ReadingTarget(ownProject, file)

        for ((project, selected) in placesOfTheFocus()) {
            selectedFile = selected
            for (paused in listOf(false, true)) {
                pressTheButtonOn(action, file, ownProject)
                if (paused) pressTheButtonOn(action, file, ownProject)
                assertTrue(isEnabledByKey(stop, project, selected))

                pressTheKeyOf(stop, project, selected)

                assertFalse("Stopped from $selected in ${project.name}", standIns.service.isReading(target))
            }
        }
    }

    @Test
    fun withNothingReadTheKeyOfStopReadingIsDisabledAndDoesNothing() {
        selectedFile = file

        assertFalse(isEnabledByKey(stop, ownProject, file))
        val messages = pressTheKeyOf(stop, ownProject, file)

        assertTrue(messages.isEmpty())
        assertNull(standIns.service.currentTarget())
    }

    @Test
    fun onTheTabTheButtonsDoWhatTheyDidBefore() {
        pressTheButtonOn(action, file, ownProject)

        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(action, otherMarkdownFile, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, visiblePresentationFor(stop, file, ownProject))
        assertNull(visiblePresentationFor(stop, otherMarkdownFile, ownProject))

        pressTheButtonOn(action, otherMarkdownFile, ownProject)

        assertTrue(standIns.service.isReading(ReadingTarget(ownProject, otherMarkdownFile)))
        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(action, file, ownProject))
        assertNull(visiblePresentationFor(stop, file, ownProject))
    }

    @Test
    fun afterEachKeyTheButtonsOnTheTabShowTheStateOfTheReading() {
        pressTheButtonOn(action, file, ownProject)
        selectedFile = otherMarkdownFile

        pressTheKeyOf(action, otherProject)
        assertEquals("Resume Reading" to AloudRiteIcons.ResumeReading, presentationFor(action, file, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, visiblePresentationFor(stop, file, ownProject))

        pressTheKeyOf(action, otherProject)
        assertEquals("Pause Reading" to AllIcons.Actions.Pause, presentationFor(action, file, ownProject))
        assertEquals("Stop Reading" to AllIcons.Actions.Suspend, visiblePresentationFor(stop, file, ownProject))

        pressTheKeyOf(stop, otherProject)
        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(action, file, ownProject))
        assertNull(visiblePresentationFor(stop, file, ownProject))
    }

    // Each project and selected editor the focus can be in while a file of the own project is read: that file, another
    // Markdown file, one that is not Markdown and none, in the own project and in another
    private fun placesOfTheFocus(): List<Pair<Project, VirtualFile?>> =
        listOf(ownProject, otherProject).flatMap { project ->
            listOf(file, otherMarkdownFile, plainTextFile, null).map { selected -> project to selected }
        }

    private fun readingAction(modelsFolder: Path): ReadAloudAction =
        readAloudActionOn(standIns.service, idleDownloads, modelsFolder, selectedFile = { selectedFile })

    private fun folderWithTheModelStandIn(): Path =
        temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
