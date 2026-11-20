package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
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
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
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
import com.intellij.ui.InplaceButton
import com.intellij.ui.TitledSeparator
import kotlin.math.abs
import java.awt.Component
import java.awt.Container
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractButton
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View
import javax.swing.text.JTextComponent

class ReadingPanelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIns = SpeechStandIns()
    private val disposable = Disposer.newDisposable()
    private val ownProject: Project = ProjectManager.getInstance().defaultProject
    // Only ever the own project of a panel or the project of a key: a press on a tab button with it would need services
    // that a MockProject has not
    private val otherProject: Project = MockProject(null, disposable)
    private val idleDownloads = ModelDownloadService(
        runInBackground = { },
        install = { _, _, _, _ -> },
        reportFailure = { _, _ -> },
        onRunningChanged = { },
    )
    private val action by lazy {
        readAloudActionOn(standIns.service, idleDownloads, temporaryFolder.newFolder().toPath().also { fillWithModelStandIn(INT8_MODEL.folderIn(it)) })
    }
    private val file = markdownFile("One sentence.")
    private var modelInUse = INT8_MODEL
    private var settingsOpened = 0

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
    fun whileAFileIsReadTheReadingGroupShowsTheBarAndTheTimesAsEstimatesUnderTheProject() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals("About 0:01 left", plainText(panel.timeLeftLabel))
        assertEquals("0:01 in all", plainText(panel.wholeTimeLabel))
        assertEquals(ReadingPanel.ESTIMATE_TOOLTIP_TEXT, runInEdtAndGet { panel.timeLeftLabel.toolTipText })
        assertEquals(ReadingPanel.ESTIMATE_TOOLTIP_TEXT, runInEdtAndGet { panel.wholeTimeLabel.toolTipText })
        assertEquals(0, runInEdtAndGet { panel.progressBar.value })
        assertEquals(1000, runInEdtAndGet { panel.progressBar.maximum })
        val order = runInEdtAndGet { panel.readingGroup.components.toList() }
        assertTrue(order.indexOf(panel.projectLabel) < order.indexOf(panel.timesRow))
        assertEquals(listOf(panel.progressBar, panel.timesLine), runInEdtAndGet { panel.timesRow.components.toList() })
    }

    @Test
    fun theBarMovesForwardAtATickOnceAudioWasHeard() {
        val panel = panelOf(ownProject)
        val values = mutableListOf<Int>()
        standIns.whilePlaying = {
            runInEdtAndWait { panel.showTheTimes() }
            values += runInEdtAndGet { panel.progressBar.value }
        }
        pressTheButtonOn(action, file, ownProject)

        standIns.runKeptWorks()

        assertEquals(0, values.first())
        assertEquals(values.sorted(), values)
        assertTrue(values.last() > 0)
    }

    @Test
    fun whilePausedTwoTicksShowTheSameTextAndValue() {
        val panel = panelOf(ownProject)
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == 3) standIns.service.pause(ReadingTarget(ownProject, file))
        }
        pressTheButtonOn(action, file, ownProject)
        standIns.runKeptWorks()
        dispatchTheEvents()

        runInEdtAndWait { panel.showTheTimes() }
        val text = runInEdtAndGet { panel.timeLeftLabel.text to panel.wholeTimeLabel.text }
        val value = runInEdtAndGet { panel.progressBar.value }
        runInEdtAndWait { panel.showTheTimes() }

        assertTrue(standIns.service.isPaused(ReadingTarget(ownProject, file)))
        assertEquals(text, runInEdtAndGet { panel.timeLeftLabel.text to panel.wholeTimeLabel.text })
        assertEquals(value, runInEdtAndGet { panel.progressBar.value })
    }

    @Test
    fun afterAChangeOfTheSpeedTheNextTickShowsTheTimesAtTheNewSpeed() {
        val longFile = markdownFile("Sentence number one. ".repeat(40))
        val target = ReadingTarget(ownProject, longFile)
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, longFile, ownProject)
        dispatchTheEvents()
        val wholeBefore = checkNotNull(standIns.service.timesOf(target)).wholeSeconds

        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        runInEdtAndWait { panel.showTheTimes() }

        val times = checkNotNull(standIns.service.timesOf(target))
        assertEquals(wholeBefore / 2, times.wholeSeconds, 0.01)
        assertEquals("About ${minutesAndSeconds(times.leftSeconds)} left", plainText(panel.timeLeftLabel))
        assertEquals("${minutesAndSeconds(times.wholeSeconds)} in all", plainText(panel.wholeTimeLabel))
    }

    @Test
    fun whileNothingIsReadAndOnceTheReadingEndedTheRowIsHiddenAndNoTimeIsShown() {
        val panel = panelOf(ownProject)
        assertFalse(runInEdtAndGet { panel.timesRow.isVisible })

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertTrue(runInEdtAndGet { panel.timesRow.isVisible })

        standIns.runKeptWorks()
        dispatchTheEvents()
        runInEdtAndWait { panel.showTheTimes() }

        assertFalse(runInEdtAndGet { panel.timesRow.isVisible })
        assertShowsNothingRead(panel)
    }

    @Test
    fun theTimerTicksAtLeastOnceASecondAndStopsWhenThePanelStopsFollowing() {
        val following = Disposer.newDisposable()
        val panel = panelOf(ownProject, following)

        assertTrue(panel.refreshTimer.delay <= 1000)
        assertTrue(panel.refreshTimer.isRunning)

        Disposer.dispose(following)

        assertFalse(panel.refreshTimer.isRunning)
    }

    @Test
    fun whileAFileOfItsOwnProjectIsReadThePanelShowsItsNameAndTheTimes() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun whileAFileOfAnotherProjectIsReadThePanelShowsTheNameOfThatProjectToo() {
        val panel = panelOf(otherProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Reading notes.md", "In the project ${ownProject.name}", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun aPanelMadeWhileAFileIsReadShowsTheReadingAtOnce() {
        pressTheButtonOn(action, file, ownProject)

        val panel = panelOf(ownProject)

        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun thePanelsOfTwoProjectsShowTheSameReadingAndEachStopsIt() {
        val ownPanel = panelOf(ownProject)
        val otherPanel = panelOf(otherProject)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(ownPanel))
        assertEquals(listOf("Reading notes.md", "In the project ${ownProject.name}", *ONE_SENTENCE_TIMES), shownBy(otherPanel))

        standIns.service.stop(ReadingTarget(ownProject, file))
        dispatchTheEvents()
        assertFalse(standIns.service.isReading(ReadingTarget(ownProject, file)))
        assertShowsNothingRead(ownPanel)
        assertShowsNothingRead(otherPanel)

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        standIns.service.stop(ReadingTarget(ownProject, file))
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
        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))

        pressTheButtonOn(action, otherFile, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading other.md", *ONE_SENTENCE_TIMES), shownBy(panel))

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
        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))
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
    fun whileASelectionIsReadThePanelShowsSelectionUnderTheNameOfTheFileAlsoWhilePaused() {
        val panel = panelOf(ownProject)
        val target = ReadingTarget(ownProject, file)

        readPartOfTheFile(target, PartOfTheFile.SELECTION)
        assertEquals(listOf("Reading notes.md", "Selection", *ONE_SENTENCE_TIMES), shownBy(panel))

        standIns.service.pause(target)
        dispatchTheEvents()
        assertEquals(listOf("Paused reading notes.md", "Selection", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun whileAReadingFromTheCaretIsReadThePanelShowsFromTheCaretAlsoWhilePaused() {
        val panel = panelOf(ownProject)
        val target = ReadingTarget(ownProject, file)

        readPartOfTheFile(target, PartOfTheFile.FROM_THE_CARET)
        assertEquals(listOf("Reading notes.md", "From the caret", *ONE_SENTENCE_TIMES), shownBy(panel))

        standIns.service.pause(target)
        dispatchTheEvents()
        assertEquals(listOf("Paused reading notes.md", "From the caret", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun theLineOfThePartStaysAfterAPauseAndAResume() {
        val panel = panelOf(ownProject)
        readPartOfTheFile(ReadingTarget(ownProject, file), PartOfTheFile.SELECTION)

        standIns.service.pause(ReadingTarget(ownProject, file))
        dispatchTheEvents()
        assertEquals(listOf("Paused reading notes.md", "Selection", *ONE_SENTENCE_TIMES), shownBy(panel))

        standIns.service.resume(ReadingTarget(ownProject, file), System.nanoTime())
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", "Selection", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun aWholeFileAndNothingReadShowNoLineOfThePart() {
        val panel = panelOf(ownProject)
        assertShowsNothingRead(panel)

        readPartOfTheFile(ReadingTarget(ownProject, file), PartOfTheFile.WHOLE_FILE)

        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))
        assertFalse(runInEdtAndGet { panel.partLabel.isVisible })
    }

    @Test
    fun theToolWindowsOfTwoProjectsShowTheSameLineOfThePart() {
        val own = panelOf(ownProject)
        val other = panelOf(otherProject)

        readPartOfTheFile(ReadingTarget(ownProject, file), PartOfTheFile.FROM_THE_CARET)

        assertEquals(listOf("Reading notes.md", "From the caret", *ONE_SENTENCE_TIMES), shownBy(own))
        assertEquals(
            listOf("Reading notes.md", "From the caret", "In the project ${ownProject.name}", *ONE_SENTENCE_TIMES), shownBy(other))
    }

    private fun readPartOfTheFile(target: ReadingTarget, partOfTheFile: PartOfTheFile) {
        standIns.service.read(target, placedReading(listOf(Words("One sentence.")), partOfTheFile), INT8_MODEL, System.nanoTime())
        dispatchTheEvents()
    }

    @Test
    fun whilePausedThePanelShowsThatItIsPaused() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Paused reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))
    }

    @Test
    fun thePanelFollowsAPauseAndAResumeOnTheTabAndTheEndOfTheFileAfterTheResume() {
        val panel = panelOf(ownProject)

        pressTheButtonOn(action, file, ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Paused reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))

        standIns.runKeptWorks()
        dispatchTheEvents()
        assertShowsNothingRead(panel)
    }

    @Test
    fun thePanelFollowsAPauseAResumeAndAStopByKeyFromAnotherProject() {
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)

        pressTheKeyOf(action, otherProject)
        dispatchTheEvents()
        assertEquals(listOf("Paused reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))

        pressTheKeyOf(action, otherProject)
        dispatchTheEvents()
        assertEquals(listOf("Reading notes.md", *ONE_SENTENCE_TIMES), shownBy(panel))

        pressTheKeyOf(stopReadingActionOn(standIns.service), otherProject)
        dispatchTheEvents()
        assertShowsNothingRead(panel)
    }

    @Test
    fun theTooltipsOfTheSpeedButtonsNameTheShortcutsOfTheirActionsAndTheyHaveNoPlainTooltip() {
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals("Slower" to pluginShortcutTextInTheActiveKeymap(READ_SLOWER_ACTION_ID), tooltipOf(panel.slowerButton))
        assertEquals("Faster" to pluginShortcutTextInTheActiveKeymap(READ_FASTER_ACTION_ID), tooltipOf(panel.fasterButton))
        assertNull(runInEdtAndGet { panel.slowerButton.toolTipText })
        assertNull(runInEdtAndGet { panel.fasterButton.toolTipText })
    }

    @Test
    fun theTooltipsFollowAShortcutTheUserGaveAndOneTheUserRemoved() {
        val panel = panelOf(ownProject)
        val ofTheUser = KeyboardShortcut(KeyStroke.getKeyStroke("ctrl alt shift F11"), null)

        withAKeymapOfTheUser({ keymap ->
            keymap.removeAllActionShortcuts(READ_FASTER_ACTION_ID)
            keymap.addShortcut(READ_FASTER_ACTION_ID, ofTheUser)
            keymap.removeAllActionShortcuts(READ_SLOWER_ACTION_ID)
        }) {
            dispatchTheEvents()
            assertEquals("Faster" to shortcutText("ctrl alt shift F11"), tooltipOf(panel.fasterButton))
            assertEquals("Slower" to null, tooltipOf(panel.slowerButton))
        }
        dispatchTheEvents()

        assertEquals("Faster" to pluginShortcutTextInTheActiveKeymap(READ_FASTER_ACTION_ID), tooltipOf(panel.fasterButton))
        assertEquals("Slower" to pluginShortcutTextInTheActiveKeymap(READ_SLOWER_ACTION_ID), tooltipOf(panel.slowerButton))
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
    fun theFieldAndTheLimitsOfTheButtonsShowTheSpeedAfterEachKey() {
        val panel = panelOf(ownProject)
        val slower = ReadSlowerAction { setting }
        val faster = ReadFasterAction { setting }

        pressTheKeyOf(faster, otherProject)
        dispatchTheEvents()
        assertEquals("1.05", fieldText(panel))
        assertEquals(true to true, buttonsEnabled(panel))

        setting.set(speed("0.8"), afterAWait = false)
        pressTheKeyOf(slower, otherProject)
        dispatchTheEvents()
        assertEquals("0.75", fieldText(panel))
        assertEquals(false to true, buttonsEnabled(panel))

        setting.set(speed("1.9"), afterAWait = false)
        pressTheKeyOf(faster, otherProject)
        dispatchTheEvents()
        assertEquals("2.0", fieldText(panel))
        assertEquals(true to false, buttonsEnabled(panel))
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
    fun thePanelShowsTheModelInUseWhenItIsCreatedWhileNothingIsReadAndWhileAFileIsRead() {
        val panel = panelOf(ownProject)
        assertEquals(listOf("Speech model", "Kokoro 1.0, int8"), modelShownBy(panel))

        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()

        assertEquals(listOf("Speech model", "Kokoro 1.0, int8"), modelShownBy(panel))
    }

    @Test
    fun thePanelShowsAnotherModelOnceItIsInUseAndTheNoticeIsDispatched() {
        val panel = panelOf(ownProject)

        modelInUse = FP32_MODEL
        publishTheModelChange(FP32_MODEL)
        dispatchTheEvents()

        assertEquals(listOf("Speech model", "Kokoro 1.0, fp32"), modelShownBy(panel))
    }

    @Test
    fun aModelChosenAndNotInPlaceDoesNotShowUntilItsDownloadEnds() {
        val panel = panelOf(ownProject)
        val modelsFolder = temporaryFolder.newFolder().toPath()
        val workToDo = mutableListOf<() -> Unit>()
        var answer = false
        val management = SpeechModelManagement(
            modelInUse = { modelInUse },
            chooseModel = { model ->
                modelInUse = model
                publishTheModelChange(model)
            },
            downloads = ModelDownloadService(
                runInBackground = { workToDo += it },
                install = { model, folder, _, _ -> fillWithModelStandIn(folder, model) },
                reportFailure = { _, _ -> },
                onRunningChanged = { },
            ),
            readingModel = object : ReadingModel {
                override fun modelOfTheReading(): SpeechModel? = null
            },
            modelsFolder = modelsFolder,
            ask = { _, _, _ -> answer },
            tell = { },
            runInBackground = { },
            onModelsChanged = { },
        )

        management.choose(FP32_MODEL)
        dispatchTheEvents()
        assertEquals(listOf("Speech model", "Kokoro 1.0, int8"), modelShownBy(panel))

        answer = true
        management.choose(FP32_MODEL)
        dispatchTheEvents()
        assertEquals(listOf("Speech model", "Kokoro 1.0, int8"), modelShownBy(panel))

        workToDo.single()()
        dispatchTheEvents()
        assertEquals(listOf("Speech model", "Kokoro 1.0, fp32"), modelShownBy(panel))
    }

    @Test
    fun theRowOfTheModelFitsANarrowPaneAndTheLongestNameFitsInFullInTheFirstDockedPane() {
        val panel = panelOf(ownProject)

        layOut(panel, width = 150)
        assertTrue(runInEdtAndGet { panel.modelNameLabel.x + panel.modelNameLabel.width } <= runInEdtAndGet { panel.settingsLink.parent.x })

        val room = FIRST_DOCKED_WIDTH - 2 * JBUI.scale(ReadingPanel.GAP)
        for (model in SPEECH_MODELS) {
            assertTrue(model.displayName, runInEdtAndGet { JBLabel(model.displayName).preferredSize.width } <= room)
        }
    }

    @Test
    fun thePanelHoldsTheReadingThenTheSpeedThenTheModelGroupAndOnlyTheLastTwoHaveATitledSeparator() {
        val panel = panelOf(ownProject)

        assertEquals(listOf(panel.readingGroup, panel.speedGroup, panel.modelGroup), runInEdtAndGet { panel.components.toList() })
        assertEquals(panel.speedSeparator, runInEdtAndGet { panel.speedGroup.components.first() })
        assertEquals("Reading speed", runInEdtAndGet { panel.speedSeparator.text })
        assertEquals(panel.modelSeparator, runInEdtAndGet { panel.modelGroup.components.first() })
        assertEquals("Speech model", runInEdtAndGet { panel.modelSeparator.text })
        assertTrue(runInEdtAndGet { panel.readingGroup.components.none { it is TitledSeparator } })
    }

    @Test
    fun theRowsOfAGroupAreCloserToEachOtherThanAGroupIsToTheNext() {
        pressTheButtonOn(action, file, ownProject)
        val panel = panelOf(ownProject)

        layOut(panel, width = FIRST_DOCKED_WIDTH)

        val betweenGroups = runInEdtAndGet { panel.speedGroup.y - (panel.readingGroup.y + panel.readingGroup.height) }
        val betweenRows = runInEdtAndGet {
            listOf(panel.readingGroup, panel.speedGroup, panel.modelGroup).flatMap { group ->
                group.components.filter { it.isVisible }.zipWithNext { upper, lower -> lower.y - (upper.y + upper.height) }
            }
        }
        assertTrue(ReadingPanel.ROW_GAP < ReadingPanel.GROUP_GAP)
        assertTrue("$betweenGroups against $betweenRows", betweenRows.all { it < betweenGroups })
    }

    @Test
    fun whileAFileIsReadThePanelHoldsNoButtonButTheLinkOfTheModel() {
        pressTheButtonOn(action, file, ownProject)
        val panel = panelOf(ownProject)

        assertEquals(
            setOf<Component>(panel.slowerButton, panel.speedField, panel.fasterButton, panel.settingsLink),
            runInEdtAndGet { controlsOf(panel) }.toSet(),
        )
        assertEquals(listOf(panel.settingsLink), runInEdtAndGet { controlsOf(panel.modelGroup) })
    }

    @Test
    fun theBarSpansThePaneAtAnyWidth() {
        pressTheButtonOn(action, file, ownProject)
        val panel = panelOf(ownProject)

        for (width in listOf(200, FIRST_DOCKED_WIDTH)) {
            layOut(panel, width)

            val insets = runInEdtAndGet { panel.insets }
            assertEquals(insets.left, leftEdgeIn(panel, panel.progressBar))
            assertEquals(width - insets.right, runInEdtAndGet { SwingUtilities.convertPoint(panel.progressBar, panel.progressBar.width, 0, panel).x })
        }
    }

    @Test
    fun theWholeTimeIsAtTheRightEdgeOfTheLineOfTheTimesInTheSmallerFontOfTheComment() {
        pressTheButtonOn(action, file, ownProject)
        val panel = panelOf(ownProject)

        layOut(panel, FIRST_DOCKED_WIDTH)

        assertEquals(runInEdtAndGet { panel.timesLine.width }, runInEdtAndGet { panel.wholeTimeLabel.x + panel.wholeTimeLabel.width })
        assertEquals(0, runInEdtAndGet { panel.timeLeftLabel.x })
        for (label in listOf(panel.timeLeftLabel, panel.wholeTimeLabel)) {
            assertTrue(runInEdtAndGet { label.font.size < panel.fileLabel.font.size })
        }
    }

    // The tool window is most often open before a reading starts, so its labels are laid out first with no text
    @Test
    fun theLineOfTheTimesIsAsHighAsItsTextWhenThePaneWasLaidOutBeforeTheReading() {
        val panel = panelOf(ownProject)
        layOut(panel, FIRST_DOCKED_WIDTH)

        pressTheButtonOn(action, file, ownProject)
        runInEdtAndWait { panel.showTheTimes() }
        layOut(panel, FIRST_DOCKED_WIDTH)

        for (label in listOf(panel.timeLeftLabel, panel.wholeTimeLabel)) {
            val textHeight = runInEdtAndGet { label.getFontMetrics(label.font).height }
            assertTrue(runInEdtAndGet { label.height } >= textHeight)
        }
        assertTrue(runInEdtAndGet { panel.timesLine.height } >= runInEdtAndGet { panel.timeLeftLabel.height })
    }

    @Test
    fun theFieldOfTheSpeedHoldsTheRemarkAsItsTooltipAndNoLabelHoldsIt() {
        pressTheButtonOn(action, file, ownProject)
        val panel = panelOf(ownProject)

        assertEquals(SPEED_COMMENT, runInEdtAndGet { panel.speedField.toolTipText })
        assertTrue(runInEdtAndGet { labelsIn(panel).none { it.text.orEmpty().contains(SPEED_COMMENT) } })
    }

    @Test
    fun theRowOfTheModelHoldsTheNameAtItsLeftAndTheLinkAtItsRightEdge() {
        val panel = panelOf(ownProject)

        layOut(panel, FIRST_DOCKED_WIDTH)

        assertEquals("Settings\u2026", runInEdtAndGet { panel.settingsLink.text })
        assertEquals(0, runInEdtAndGet { panel.modelNameLabel.x })
        assertEquals(runInEdtAndGet { panel.modelRow.width }, runInEdtAndGet { panel.settingsLink.parent.x + panel.settingsLink.parent.width })
    }

    @Test
    fun theRowOfTheModelIsHighEnoughForTheNameWrappedInANarrowPane() {
        val panel = panelOf(ownProject)

        for (width in listOf(134, 200)) {
            layOut(panel, width)

            val needed = runInEdtAndGet {
                val view = panel.modelNameLabel.getClientProperty(BasicHTML.propertyKey) as View
                view.setSize(panel.modelNameLabel.width.toFloat(), 0f)
                view.getPreferredSpan(View.Y_AXIS).toInt()
            }
            assertTrue("$width", needed > runInEdtAndGet { JBLabel("x").preferredSize.height } || width == 200)
            assertTrue("$width: $needed", runInEdtAndGet { panel.modelNameLabel.height } >= needed)
        }
    }

    private val emma = voiceNamed("bf_emma")!!
    private val nicole = voiceNamed("af_nicole")!!

    // A label is no control: the row holds nothing else, so there is nothing to press or to type in
    private fun allLabelsOnly(container: Container): Boolean =
        container.components.all { child -> child is JLabel || (child is javax.swing.JPanel && allLabelsOnly(child)) }

    private fun voiceShownBy(panel: ReadingPanel): List<String> = runInEdtAndGet {
        listOf(panel.voiceLabel.text, panel.voiceNameLabel.text.replace(Regex("<[^>]*>"), "").trim())
    }

    private fun publishTheVoiceChange(voice: Voice) {
        ApplicationManager.getApplication().messageBus.syncPublisher(VOICE_CHANGED).voiceChanged(voice)
    }

    @Test
    fun whileNothingIsReadTheModelGroupHoldsTheRowOfTheVoiceInUseUnderTheRowOfTheModelInEveryProject() {
        for (project in listOf(ownProject, otherProject)) {
            val panel = panelOf(project)

            assertEquals(listOf("Voice:", "Heart (American English, female)"), voiceShownBy(panel))
            assertEquals(listOf(panel.modelSeparator, panel.modelRow, panel.voiceRow), runInEdtAndGet { panel.modelGroup.components.toList() })
        }
    }

    @Test
    fun whileAFileIsReadAndPausedTheRowShowsTheVoiceOfTheReadingAlsoAfterAnotherWasChosenDuringIt() {
        standIns.voice = emma
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)
        dispatchTheEvents()
        assertEquals(listOf("Voice:", "Emma (British English, female)"), voiceShownBy(panel))

        standIns.voice = nicole
        publishTheVoiceChange(nicole)
        dispatchTheEvents()
        assertEquals(listOf("Voice:", "Emma (British English, female)"), voiceShownBy(panel))

        standIns.service.pause(ReadingTarget(ownProject, file))
        dispatchTheEvents()
        assertEquals(listOf("Voice:", "Emma (British English, female)"), voiceShownBy(panel))
    }

    @Test
    fun onceTheReadingEndsTheRowShowsTheVoiceChosenAndAChoiceWhileNothingIsReadShowsAfterTheNotice() {
        standIns.voice = emma
        val panel = panelOf(ownProject)
        pressTheButtonOn(action, file, ownProject)
        standIns.voice = nicole
        publishTheVoiceChange(nicole)
        standIns.runKeptWorks()
        dispatchTheEvents()

        assertEquals(listOf("Voice:", "Nicole (American English, female, whispers)"), voiceShownBy(panel))

        standIns.voice = DEFAULT_VOICE
        publishTheVoiceChange(DEFAULT_VOICE)
        dispatchTheEvents()
        assertEquals(listOf("Voice:", "Heart (American English, female)"), voiceShownBy(panel))
    }

    @Test
    fun theRowOfTheVoiceHoldsNoControlThatTakesTheFocusOrAClick() {
        val panel = panelOf(ownProject)

        assertEquals(emptyList<Component>(), runInEdtAndGet { controlsOf(panel.voiceRow) })
        assertTrue(runInEdtAndGet { allLabelsOnly(panel.voiceRow) })
    }

    @Test
    fun theLongestNameOfAVoiceWrapsAndFitsInFullInANarrowPane() {
        standIns.voice = VOICES.maxBy { it.displayName.length }
        val panel = panelOf(ownProject)

        for (width in listOf(134, 200, 150)) {
            layOut(panel, width)

            val needed = runInEdtAndGet {
                val view = panel.voiceNameLabel.getClientProperty(BasicHTML.propertyKey) as View
                view.setSize(panel.voiceNameLabel.width.toFloat(), 0f)
                view.getPreferredSpan(View.Y_AXIS).toInt()
            }
            assertTrue("$width: $needed", runInEdtAndGet { panel.voiceNameLabel.height } >= needed)
            assertTrue("$width", runInEdtAndGet { panel.voiceNameLabel.x + panel.voiceNameLabel.width <= panel.voiceRow.width })
        }
        assertEquals(nicole, standIns.voice)
    }

    @Test
    fun aPressOnTheLinkOpensTheSettingsOnce() {
        val panel = panelOf(ownProject)

        runInEdtAndWait { panel.settingsLink.doClick() }

        assertEquals(1, settingsOpened)
    }

    @Test
    fun theLeftEdgeOfEveryElementOfThePaneIsTheSame() {
        val panel = panelOf(ownProject)
        val ofAnotherProject = ReadingTarget(otherProject, file)
        readPartOfTheFile(ofAnotherProject, PartOfTheFile.FROM_THE_CARET)
        layOut(panel, FIRST_DOCKED_WIDTH)

        val edges = mapOf(
            "file" to leftEdgeIn(panel, panel.fileLabel),
            "part" to leftEdgeIn(panel, panel.partLabel),
            "project" to leftEdgeIn(panel, panel.projectLabel),
            "time left" to leftEdgeIn(panel, panel.timeLeftLabel),
            "bar" to leftEdgeIn(panel, panel.progressBar),
            "speed title" to leftEdgeIn(panel, panel.speedSeparator.label),
            "minus" to leftEdgeIn(panel, panel.slowerButton),
            "model title" to leftEdgeIn(panel, panel.modelSeparator.label),
            "model" to leftEdgeIn(panel, panel.modelNameLabel),
            "voice" to leftEdgeIn(panel, panel.voiceLabel),
        )
        assertTrue("$edges", edges.values.max() - edges.values.min() <= 1)

        standIns.service.stop(ofAnotherProject)
        dispatchTheEvents()
        layOut(panel, FIRST_DOCKED_WIDTH)
        assertTrue(abs(leftEdgeIn(panel, panel.nothingReadLabel) - edges.getValue("model")) <= 1)
    }

    // Where the component paints its left edge, in the coordinates of the panel: that of its text for a label, and that
    // of its bounds for a bar and a button, which paint their own border or icon from there
    private fun leftEdgeIn(panel: ReadingPanel, component: JComponent): Int = runInEdtAndGet {
        val inset = if (component is JLabel) component.border?.getBorderInsets(component)?.left ?: 0 else 0
        SwingUtilities.convertPoint(component, 0, 0, panel).x + inset
    }

    private fun labelsIn(container: Container): List<JLabel> = container.components.flatMap { child ->
        when (child) {
            is JLabel -> listOf(child)
            is Container -> labelsIn(child)
            else -> emptyList()
        }
    }

    private fun controlsOf(container: Container): List<Component> =
        container.components.flatMap { child ->
            if (child is Container && child !is JLabel && child !is JTextComponent &&
                child !is JButton && child !is InplaceButton
            ) controlsOf(child)
            else if (child is AbstractButton || child is JTextComponent ||
                child is InplaceButton
            ) listOf(child)
            else emptyList()
        }

    private fun modelShownBy(panel: ReadingPanel): List<String> = runInEdtAndGet {
        listOf(
            panel.modelSeparator.text,
            panel.modelNameLabel.text.replace(Regex("<[^>]*>"), "").trim(),
        )
    }

    private fun publishTheModelChange(model: SpeechModel) {
        ApplicationManager.getApplication().messageBus.syncPublisher(SPEECH_MODEL_CHANGED).modelChanged(model)
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

    // The title of the separator, the title of the tooltip of the button before the field, the field, the title of the tooltip of the
    // button after it, and the tooltip of the field
    private fun speedShownBy(panel: ReadingPanel): List<String> =
        if (!runInEdtAndGet { panel.speedRow.isVisible }) emptyList()
        else listOf(
            runInEdtAndGet { panel.speedSeparator.text },
            tooltipOf(panel.slowerButton)!!.first,
            runInEdtAndGet { panel.speedField.text },
            tooltipOf(panel.fasterButton)!!.first,
            runInEdtAndGet { panel.speedField.toolTipText },
        )

    private fun tooltipOf(button: JComponent): Pair<String, String?>? =
        runInEdtAndGet { HelpTooltip.getTooltipFor(button) }?.let(::titleAndShortcutOf)

    private fun layOut(panel: ReadingPanel, width: Int) {
        runInEdtAndWait {
            panel.setSize(width, 400)
            doLayoutDeep(panel)
        }
    }

    private fun doLayoutDeep(container: Container) {
        container.doLayout()
        for (child in container.components) if (child is Container) doLayoutDeep(child)
    }

    private fun panelOf(project: Project, following: Disposable = disposable): ReadingPanel =
        runInEdtAndGet {
            ReadingPanel(project, { standIns.service }, { setting }, { modelInUse }, { standIns.voice }, { settingsOpened++ })
                .also { it.followTheReading(following) }
        }

    private fun dispatchTheEvents() {
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    // The texts of what the reading group shows, from the top
    private fun shownBy(panel: ReadingPanel): List<String> = runInEdtAndGet {
        buildList {
            for (label in listOf(panel.nothingReadLabel, panel.fileLabel, panel.partLabel, panel.projectLabel)) {
                if (label.isVisible) add(label.text)
            }
            if (panel.timesRow.isVisible) {
                add(plainText(panel.timeLeftLabel))
                add(plainText(panel.wholeTimeLabel))
            }
        }
    }

    private fun plainText(label: JLabel): String =
        runInEdtAndGet { label.text.replace(Regex("(?s)<head>.*</head>|<[^>]*>"), "").trim() }

    private fun assertShowsNothingRead(panel: ReadingPanel) {
        assertEquals(listOf(ReadingPanel.NOTHING_READ_TEXT), shownBy(panel))
    }

    private fun readAloud() = "Read Aloud" to AloudRiteIcons.ReadAloud

    companion object {
        // The width of the pane in the sandbox IDE when its capture was taken, see the record of the unit
        // The estimate of "One sentence." from its text, 12 characters at 0.075 seconds, rounded up
        private val ONE_SENTENCE_TIMES = arrayOf("About 0:01 left", "0:01 in all")

        private const val FIRST_DOCKED_WIDTH = 468

        private const val SPEED_COMMENT =
            "A change starts the sentence that is playing again at the new speed. This can take a few seconds."

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
