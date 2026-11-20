package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.mock.MockProject
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.swing.Icon

class ReadingTitleActionsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIns = SpeechStandIns()
    private val disposable = Disposer.newDisposable()
    private val ownProject: Project = ProjectManager.getInstance().defaultProject
    private val otherProject: Project = MockProject(null, disposable)
    private val file = markdownFile("One sentence.")
    private val target = ReadingTarget(ownProject, file)
    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )
    private val tabAction by lazy {
        readAloudActionOn(
            standIns.service, idleDownloads,
            temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) },
        )
    }
    private val pauseOrResume = PauseOrResumeReadingInTheTitleAction { standIns.service }
    private val stop = StopReadingInTheTitleAction { standIns.service }

    @Before
    fun publishTheChanges() {
        standIns.whenChanged = ::publishReadingChange
    }

    @After
    fun cleanUp() {
        Disposer.dispose(disposable)
    }

    @Test
    fun whileNothingIsReadBothButtonsAreShownGreyedOutWithThePauseAndTheStopIcon() {
        assertEquals(Shown(true, false, "Pause Reading", AllIcons.Actions.Pause), shownBy(pauseOrResume))
        assertEquals(Shown(true, false, "Stop Reading", AllIcons.Actions.Suspend), shownBy(stop))
    }

    @Test
    fun aPressWhileNothingIsReadDoesNothing() {
        press(pauseOrResume)
        press(stop)
        dispatchTheEvents()

        assertFalse(standIns.service.isReading(target))
        assertTrue(standIns.textsMade.isEmpty())
        assertEquals(Shown(true, false, "Pause Reading", AllIcons.Actions.Pause), shownBy(pauseOrResume))
    }

    @Test
    fun whileAFileIsReadBothButtonsAreEnabledWithThePauseAndTheStopIcon() {
        startTheReading()

        assertEquals(Shown(true, true, "Pause Reading", AllIcons.Actions.Pause), shownBy(pauseOrResume))
        assertEquals(Shown(true, true, "Stop Reading", AllIcons.Actions.Suspend), shownBy(stop))
    }

    @Test
    fun whileTheReadingIsPausedTheFirstButtonResumesAndTheSecondStillStops() {
        startTheReading()
        standIns.service.pause(target)

        assertEquals(Shown(true, true, "Resume Reading", AllIcons.Actions.Resume), shownBy(pauseOrResume))
        assertEquals(Shown(true, true, "Stop Reading", AllIcons.Actions.Suspend), shownBy(stop))
    }

    @Test
    fun theTooltipsNameTheShortcutsOfTheActionsTheyStandFor() {
        assertEquals("Pause Reading" to pluginShortcutTextInTheActiveKeymap(READ_ALOUD_ACTION_ID), tooltipOf(pauseOrResume))
        assertEquals("Stop Reading" to pluginShortcutTextInTheActiveKeymap(STOP_READING_ACTION_ID), tooltipOf(stop))

        startTheReading()
        standIns.service.pause(target)
        assertEquals("Resume Reading" to pluginShortcutTextInTheActiveKeymap(READ_ALOUD_ACTION_ID), tooltipOf(pauseOrResume))
    }

    @Test
    fun theTooltipsFollowAShortcutTheUserRemoved() {
        withAKeymapOfTheUser({ keymap -> keymap.removeAllActionShortcuts(STOP_READING_ACTION_ID) }) {
            assertEquals("Stop Reading" to null, tooltipOf(stop))
        }

        assertEquals("Stop Reading" to pluginShortcutTextInTheActiveKeymap(STOP_READING_ACTION_ID), tooltipOf(stop))
    }

    @Test
    fun theFirstButtonPausesAndResumesAndTheTabButtonFollows() {
        startTheReading()

        press(pauseOrResume)
        dispatchTheEvents()
        assertTrue(standIns.service.isPaused(target))
        assertEquals("Resume Reading" to AloudRiteIcons.ResumeReading, presentationFor(tabAction, file, ownProject))

        press(pauseOrResume)
        dispatchTheEvents()
        assertFalse(standIns.service.isPaused(target))
        assertTrue(standIns.service.isReading(target))
        assertEquals("Pause Reading" to AllIcons.Actions.Pause, presentationFor(tabAction, file, ownProject))

        standIns.runKeptWorks()
        assertEquals(listOf("One sentence."), standIns.heard())
    }

    @Test
    fun theSecondButtonStopsAPlayingAndAPausedReading() {
        startTheReading()
        press(stop)
        standIns.runKeptWorks()
        dispatchTheEvents()
        assertFalse(standIns.service.isReading(target))
        assertTrue(standIns.textsMade.isEmpty())
        assertEquals("Read Aloud" to AloudRiteIcons.ReadAloud, presentationFor(tabAction, file, ownProject))

        startTheReading()
        press(pauseOrResume)
        press(stop)
        standIns.runKeptWorks()
        dispatchTheEvents()
        assertFalse(standIns.service.isReading(target))
        assertTrue(standIns.samplesPlayed().isEmpty())
    }

    @Test
    fun theButtonsOfAnotherProjectPauseResumeAndStopTheOneReading() {
        startTheReading()

        press(pauseOrResume, otherProject)
        assertTrue(standIns.service.isPaused(target))
        press(pauseOrResume, otherProject)
        assertFalse(standIns.service.isPaused(target))
        press(stop, otherProject)
        assertFalse(standIns.service.isReading(target))
    }

    private data class Shown(val visible: Boolean, val enabled: Boolean, val text: String?, val icon: Icon?)

    private fun shownBy(action: AnAction): Shown {
        val event = titleEventFor(action, ownProject)
        ActionUtil.updateAction(action, event)
        return Shown(event.presentation.isVisible, event.presentation.isEnabled, event.presentation.text, event.presentation.icon)
    }

    private fun tooltipOf(action: AnAction): Pair<String, String?>? {
        val event = titleEventFor(action, ownProject)
        ActionUtil.updateAction(action, event)
        val tooltip = event.presentation.getClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP)
        assertNotNull(tooltip)
        return titleAndShortcutOf(tooltip!!)
    }

    private fun press(action: AnAction, project: Project = ownProject) {
        val event = titleEventFor(action, project)
        // ActionUtil asks the project for services that a MockProject has not, so its press calls the action itself
        runInEdtAndWait { if (project === ownProject) ActionUtil.performAction(action, event) else action.actionPerformed(event) }
    }

    private fun startTheReading() {
        pressTheButtonOn(tabAction, file, ownProject)
        dispatchTheEvents()
    }

    private fun dispatchTheEvents() {
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
