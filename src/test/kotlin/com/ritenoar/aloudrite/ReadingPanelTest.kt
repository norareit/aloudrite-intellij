package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
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
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

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

    // As the setting of the IDE does, but with the stand-in service of the reading
    private val setting = ReadingSpeedSetting { speed, afterAWait ->
        standIns.service.changeSpeed(speed, afterAWait)
        ApplicationManager.getApplication().messageBus.syncPublisher(READING_SPEED_CHANGED).speedChanged(speed)
    }

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

        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(panel))
    }

    @Test
    fun whileAFileOfAnotherProjectIsReadThePanelShowsTheNameOfThatProjectToo() {
        val panel = panelOf(otherProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Reading notes.md", "In the project ${ownProject.name}", "Pause", "Stop"), shownBy(panel))
    }

    @Test
    fun aPanelMadeWhileAFileIsReadShowsTheReadingAtOnce() {
        pressTheButtonOn(action, file, ownProject)

        val panel = panelOf(ownProject)

        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(panel))
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
        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(ownPanel))
        assertEquals(listOf("Reading notes.md", "In the project ${ownProject.name}", "Pause", "Stop"), shownBy(otherPanel))

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
        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(panel))

        pressTheButtonOn(action, otherFile, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading other.md", "Pause", "Stop"), shownBy(panel))

        standIns.runKeptWorks()
        dispatchTheEvents()
        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelFollowsAStopOnTheTab() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(stopReadingActionOn(standIns.service), file, ownProject)
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
        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(panel))
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

    @Test
    fun theTwoButtonsKeepTheirOwnWidthInAWidePane() {
        pressTheButtonOn(action, file, ownProject)
        val panel = panelOf(ownProject)

        layOut(panel, width = 800)

        assertEquals(runInEdtAndGet { panel.pauseButton.preferredSize.width }, runInEdtAndGet { panel.pauseButton.width })
        assertEquals(runInEdtAndGet { panel.stopButton.preferredSize.width }, runInEdtAndGet { panel.stopButton.width })
        assertTrue(runInEdtAndGet { panel.pauseButton.x < panel.stopButton.x })
        // In line with the labels above them
        assertEquals(0, runInEdtAndGet { panel.pauseButton.x })
    }

    @Test
    fun whilePausedThePanelShowsThatItIsPausedResumeAndStop() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Paused reading notes.md", "Resume", "Stop"), shownBy(panel))
    }

    @Test
    fun pauseAndResumeInThePanelDoWhatTheTabButtonDoesAndTheTabButtonFollows() {
        val panel = panelOf(ownProject)
        val target = ReadingTarget(ownProject, file)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        runInEdtAndWait { panel.pauseButton.doClick() }
        dispatchTheEvents()
        assertTrue(standIns.service.isPaused(target))
        assertEquals("Resume Reading" to AloudRiteIcons.ResumeReading, presentationFor(action, file, ownProject))
        assertEquals(listOf("Paused reading notes.md", "Resume", "Stop"), shownBy(panel))

        runInEdtAndWait { panel.pauseButton.doClick() }
        dispatchTheEvents()
        assertFalse(standIns.service.isPaused(target))
        assertTrue(standIns.service.isReading(target))
        assertEquals("Pause Reading" to AllIcons.Actions.Pause, presentationFor(action, file, ownProject))
        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(panel))

        standIns.runKeptWorks()
        dispatchTheEvents()
        assertEquals(listOf("One sentence."), standIns.heard())
        assertShowsNothingRead(panel)
    }

    @Test
    fun stopEndsAPausedReading() {
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        runInEdtAndWait { panel.stopButton.doClick() }
        standIns.runKeptWorks()
        dispatchTheEvents()

        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
        assertTrue(standIns.samplesPlayed().isEmpty())
        assertEquals(readAloud(), presentationFor(action, file, ownProject))
        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelFollowsAPauseAndAResumeOnTheTabAndTheEndOfTheFileAfterTheResume() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Paused reading notes.md", "Resume", "Stop"), shownBy(panel))

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", "Pause", "Stop"), shownBy(panel))

        standIns.runKeptWorks()
        dispatchTheEvents()
        assertShowsNothingRead(panel)
    }

    @Test
    fun theTextWhileNothingIsReadFitsANarrowPane() {
        val panel = panelOf(ownProject)

        layOut(panel, width = 150)

        assertTrue(runInEdtAndGet { panel.nothingReadLabel.width } <= 150)
    }

    @Test
    fun whileNothingIsReadAndWhileAFileIsReadThePanelShowsTheSpeed() {
        val panel = panelOf(ownProject)
        assertEquals(listOf("Reading speed", "Slower", "1.0", "Faster", SPEED_COMMENT), speedShownBy(panel))

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Reading speed", "Slower", "1.0", "Faster", SPEED_COMMENT), speedShownBy(panel))
    }

    @Test
    fun fasterAndSlowerSetTheSpeedOneStepAfterAWaitWithinTheLimits() {
        val panel = panelOf(ownProject)

        runInEdtAndWait { panel.fasterButton.doClick() }
        dispatchTheEvents()
        assertEquals(speed("1.05"), setting.speed)
        assertEquals("1.05", fieldText(panel))
        runInEdtAndWait { panel.slowerButton.doClick() }
        runInEdtAndWait { panel.slowerButton.doClick() }
        dispatchTheEvents()
        assertEquals(speed("0.95"), setting.speed)

        setting.set(ReadingSpeed.SLOWEST, afterAWait = false)
        dispatchTheEvents()
        assertEquals(false to true, buttonsEnabled(panel))
        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        dispatchTheEvents()
        assertEquals(true to false, buttonsEnabled(panel))
        setting.set(ReadingSpeed.NORMAL, afterAWait = false)
        dispatchTheEvents()
        assertEquals(true to true, buttonsEnabled(panel))
    }

    @Test
    fun aTypedSpeedIsSetWithNoWaitOnEnterOrTheLossOfFocusAndShownWithOneDecimal() {
        val panel = panelOf(ownProject)

        typeAndPressEnter(panel, "2")
        dispatchTheEvents()
        assertEquals(ReadingSpeed.FASTEST, setting.speed)
        assertEquals("2.0", fieldText(panel))

        type(panel, "1.5")
        loseFocus(panel)
        dispatchTheEvents()
        assertEquals(speed("1.5"), setting.speed)
        assertEquals("1.5", fieldText(panel))
    }

    @Test
    fun aSpeedTheFieldDoesNotAcceptLeavesTheSpeedAndMarksTheFieldUntilCorrectedOrEscaped() {
        val panel = panelOf(ownProject)

        for (text in listOf("2.5", "1,5", "1.555", "0.7")) {
            typeAndPressEnter(panel, text)

            assertEquals(ReadingSpeed.NORMAL, setting.speed)
            assertEquals("Type a speed from 0.75 to 2.0", errorOf(panel))
            assertEquals("error", runInEdtAndGet { panel.speedField.getClientProperty("JComponent.outline") })
        }
        loseFocus(panel)
        assertEquals("Type a speed from 0.75 to 2.0", errorOf(panel))

        type(panel, "1.5")
        assertNull(errorOf(panel))
        assertEquals(ReadingSpeed.NORMAL, setting.speed)

        typeAndPressEnter(panel, "3")
        runInEdtAndWait { panel.escapeInTheField.actionPerformed(TestActionEvent.createTestEvent()) }
        assertEquals("1.0", fieldText(panel))
        assertNull(errorOf(panel))
        assertNull(runInEdtAndGet { panel.speedField.getClientProperty("JComponent.outline") })
        assertTrue(runInEdtAndGet {
            ActionUtil.getActions(panel.speedField).any { action ->
                action === panel.escapeInTheField &&
                    action.shortcutSet.shortcuts.contains(KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null))
            }
        })
    }

    @Test
    fun aPressDuringAReadingChangesItsSpeedAfterTheWaitAndATypedSpeedAtOnce() {
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)
        standIns.runNextKeptWork()

        runInEdtAndWait { panel.fasterButton.doClick() }
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.0"), standIns.speedsMade)
        standIns.passTime(500)
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.0", "1.05"), standIns.speedsMade)

        // The stand-ins start a reading at a speed of their own, 1.0, and not at the speed of this setting
        pressTheButtonOn(action, file, ownProject)
        standIns.runNextKeptWork()
        typeAndPressEnter(panel, "1.37")
        standIns.runKeptWorks()
        assertEquals(speedsOf("1.0", "1.05", "1.0", "1.37"), standIns.speedsMade)
    }

    @Test
    fun aChangeInThePanelOfOneProjectShowsInThatOfAnotherWithTheLimitsOfItsButtons() {
        val ownPanel = panelOf(ownProject)
        val otherPanel = panelOf(otherProject)

        typeAndPressEnter(ownPanel, "2")
        dispatchTheEvents()
        assertEquals("2.0", fieldText(otherPanel))
        assertEquals(true to false, buttonsEnabled(otherPanel))

        setting.set(speed("0.75"), afterAWait = false)
        dispatchTheEvents()
        assertEquals("0.75", fieldText(ownPanel))
        assertEquals("0.75", fieldText(otherPanel))
        assertEquals(false to true, buttonsEnabled(ownPanel))
    }

    @Test
    fun theButtonsOfTheSpeedAreTheMinusAndPlusIconsAndTheTimesSignInTheFieldIsNoButton() {
        val panel = panelOf(ownProject)

        assertEquals(AllIcons.General.Remove, runInEdtAndGet { panel.slowerButton.icon })
        assertEquals(AllIcons.General.Add, runInEdtAndGet { panel.fasterButton.icon })
        val extension = runInEdtAndGet { panel.speedField.extensions.single() }
        assertNull(extension.actionOnClick)
        assertEquals("×", (extension.getIcon(false) as ReadingPanel.TimesSign).text)
        assertTrue(runInEdtAndGet { panel.speedRow.components.toList() == listOf(panel.slowerButton, panel.speedField, panel.fasterButton) })
    }

    @Test
    fun thePanelCanTakeTheFocusItselfSoThatOpeningThePaneFocusesNoControl() {
        val panel = panelOf(ownProject)

        assertTrue(runInEdtAndGet { panel.isFocusable })
    }

    @Test
    fun theTextUnderTheSpeedFitsANarrowPane() {
        val panel = panelOf(ownProject)

        layOut(panel, width = 150)

        assertTrue(runInEdtAndGet { panel.speedComment.width } <= 150)
    }

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    private fun speedsOf(vararg typed: String): List<Float> = typed.map { speed(it).forTheEngine }

    private fun type(panel: ReadingPanel, text: String) = runInEdtAndWait { panel.speedField.text = text }

    private fun typeAndPressEnter(panel: ReadingPanel, text: String) = runInEdtAndWait {
        panel.speedField.text = text
        panel.speedField.postActionEvent()
    }

    private fun loseFocus(panel: ReadingPanel) = runInEdtAndWait {
        for (listener in panel.speedField.focusListeners) listener.focusLost(FocusEvent(panel.speedField, FocusEvent.FOCUS_LOST))
    }

    private fun fieldText(panel: ReadingPanel): String = runInEdtAndGet { panel.speedField.text }

    private fun errorOf(panel: ReadingPanel): String? =
        runInEdtAndGet { ComponentValidator.getInstance(panel.speedField).orElseThrow().validationInfo?.message }

    private fun buttonsEnabled(panel: ReadingPanel): Pair<Boolean, Boolean> =
        runInEdtAndGet { panel.slowerButton.isActive to panel.fasterButton.isActive }

    // The label, the tooltip of the button before the field, the field, the tooltip of the button after it, and the
    // text under the row without its markup
    private fun speedShownBy(panel: ReadingPanel): List<String> = runInEdtAndGet {
        if (!panel.speedRow.isVisible) emptyList()
        else listOf(
            panel.speedLabel.text,
            panel.slowerButton.toolTipText,
            panel.speedField.text,
            panel.fasterButton.toolTipText,
            panel.speedComment.text.replace(Regex("(?s)<head>.*</head>|<[^>]*>"), "").trim(),
        )
    }

    private fun layOut(panel: ReadingPanel, width: Int) {
        runInEdtAndWait {
            panel.setSize(width, 400)
            panel.doLayout()
            panel.controlRow.doLayout()
        }
    }

    private fun panelOf(project: Project, following: Disposable = disposable): ReadingPanel =
        runInEdtAndGet {
            ReadingPanel(project, { standIns.service }, { setting }).also { it.followTheReading(following) }
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
            if (panel.controlRow.isVisible) {
                add(panel.pauseButton.text)
                add(panel.stopButton.text)
            }
        }
    }

    private fun assertShowsNothingRead(panel: ReadingPanel) {
        assertEquals(listOf(ReadingPanel.NOTHING_READ_TEXT), shownBy(panel))
    }

    private fun readAloud() = "Read Aloud" to AloudRiteIcons.ReadAloud

    companion object {
        private const val SPEED_COMMENT =
            "A change starts the sentence that is playing again at the new speed. This can take a few seconds."

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
