package com.ritenoar.aloudrite

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReadingPanelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIns = SpeechStandIns()
    private val disposable = Disposer.newDisposable()
    private val ownProject: Project = ProjectManager.getInstance().defaultProject
    // Only ever the own project of a panel: a press with it would need services that a MockProject has not
    private val otherProject: Project = MockProject(null, disposable)
    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _ -> },
        reportFailure = { _, _ -> },
    )
    private val action by lazy {
        readAloudActionOn(standIns.service, idleDownloads, temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(it) })
    }
    private val file = markdownFile("One sentence.")

    @Before
    fun publishTheChanges() {
        standIns.whenChanged = ::publishReadingChange
    }

    @After
    fun cleanUp() {
        Disposer.dispose(disposable)
    }

    @Test
    fun whileNothingIsReadThePanelSaysSo() {
        val panel = panelOf(ownProject)

        assertShowsNothingRead(panel)
    }

    @Test
    fun whileAFileOfItsOwnProjectIsReadThePanelShowsItsNameAndStop() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Reading notes.md", "Stop"), shownBy(panel))
    }

    @Test
    fun whileAFileOfAnotherProjectIsReadThePanelShowsTheNameOfThatProjectToo() {
        val panel = panelOf(otherProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Reading notes.md", "In the project ${ownProject.name}", "Stop"), shownBy(panel))
    }

    @Test
    fun aPanelMadeWhileAFileIsReadShowsTheReadingAtOnce() {
        pressTheButtonOn(action, file, ownProject)

        val panel = panelOf(ownProject)

        assertEquals(listOf("Reading notes.md", "Stop"), shownBy(panel))
    }

    @Test
    fun stopEndsTheReadingAsTheStopOnTheTabDoes() {
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        runInEdtAndWait { panel.stopButton.doClick() }
        standIns.runKeptWorks()
        dispatchTheEvents()

        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
        assertTrue(standIns.textsMade.isEmpty())
        assertTrue(standIns.samplesPlayed().isEmpty())
        assertEquals(readAloud(), presentationFor(action, file, ownProject))
        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelsOfTwoProjectsShowTheSameReadingAndEachStopsIt() {
        val ownPanel = panelOf(ownProject)
        val otherPanel = panelOf(otherProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", "Stop"), shownBy(ownPanel))
        assertEquals(listOf("Reading notes.md", "In the project ${ownProject.name}", "Stop"), shownBy(otherPanel))

        runInEdtAndWait { otherPanel.stopButton.doClick() }
        dispatchTheEvents()
        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
        assertShowsNothingRead(ownPanel)
        assertShowsNothingRead(otherPanel)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        runInEdtAndWait { ownPanel.stopButton.doClick() }
        dispatchTheEvents()
        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
        assertShowsNothingRead(ownPanel)
        assertShowsNothingRead(otherPanel)
    }

    @Test
    fun thePanelFollowsAPressOnAnotherFileAndTheEndOfTheFile() {
        val panel = panelOf(ownProject)
        val otherFile = LightVirtualFile("other.md", markdownFileType(), "Other.")

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", "Stop"), shownBy(panel))

        pressTheButtonOn(action, otherFile, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading other.md", "Stop"), shownBy(panel))

        standIns.runKeptWorks()
        dispatchTheEvents()
        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelFollowsAStopOnTheTab() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelFollowsTheClosingOfTheFileAndOfItsProject() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        standIns.service.fileClosed(ReadingTarget(ownProject, file))
        dispatchTheEvents()
        assertShowsNothingRead(panel)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        standIns.service.projectClosed(ownProject)
        dispatchTheEvents()
        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelFollowsAFailure() {
        val panel = panelOf(ownProject)
        standIns.engineFailsAt = { true }

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", "Stop"), shownBy(panel))
        standIns.runKeptWorks()
        dispatchTheEvents()

        assertShowsNothingRead(panel)
        assertEquals(file, standIns.failures.single().first.file)
    }

    @Test
    fun aPanelNoLongerFollowsOnceItsDisposableIsDisposed() {
        val following = Disposer.newDisposable(disposable)
        val panel = panelOf(ownProject, following)

        Disposer.dispose(following)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertShowsNothingRead(panel)
    }

    private fun panelOf(project: Project, following: Disposable = disposable): ReadingPanel =
        runInEdtAndGet {
            ReadingPanel(project) { standIns.service }.also { it.followTheReading(following) }
        }

    private fun dispatchTheEvents() {
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    // The texts of what the panel shows, from the top
    private fun shownBy(panel: ReadingPanel): List<String> = runInEdtAndGet {
        buildList {
            for (label in listOf(panel.nothingReadLabel, panel.fileLabel, panel.projectLabel)) {
                if (label.isVisible) add(label.text)
            }
            if (panel.stopButton.isVisible) add(panel.stopButton.text)
        }
    }

    private fun assertShowsNothingRead(panel: ReadingPanel) {
        assertEquals(listOf(ReadingPanel.NOTHING_READ_TEXT), shownBy(panel))
    }

    private fun readAloud() = "Read Aloud" to registeredReadAloudAction().templatePresentation.icon

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
