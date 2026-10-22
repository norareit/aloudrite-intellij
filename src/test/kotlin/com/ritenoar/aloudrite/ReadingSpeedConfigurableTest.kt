package com.ritenoar.aloudrite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.awt.Container
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingUtilities

class ReadingSpeedConfigurableTest {

    private val changes = mutableListOf<Pair<ReadingSpeed, Boolean>>()
    // Publishes each change, as the setting of the IDE does
    private val setting = ReadingSpeedSetting { speed, afterAWait ->
        changes += speed to afterAWait
        ApplicationManager.getApplication().messageBus.syncPublisher(READING_SPEED_CHANGED).speedChanged(speed)
    }
    private val int8 = INT8_MODEL
    private val fp32 = FP32_MODEL
    private var modelInUse = int8

    // The models the stand-in management says are on the machine, and what it was asked
    private val inPlace = mutableSetOf(int8)
    private val removable = mutableSetOf<SpeechModel>()
    private var downloadRuns = false
    private var downloadingNow: SpeechModel? = null
    private var pendingNow: SpeechModel? = null
    private var chooseAnswer = true
    private val chooseCalls = mutableListOf<SpeechModel>()
    private val downloadCalls = mutableListOf<SpeechModel>()
    private val removeCalls = mutableListOf<SpeechModel>()
    private val models = object : ManagedSpeechModels {
        override val isDownloadRunning get() = downloadRuns
        override val pendingChoice get() = pendingNow
        override val downloadingModel get() = downloadingNow
        override fun isInPlace(model: SpeechModel) = model in inPlace
        override fun canRemove(model: SpeechModel) = model in removable
        override fun download(model: SpeechModel) {
            downloadCalls += model
        }
        override fun choose(model: SpeechModel): Boolean {
            chooseCalls += model
            if (chooseAnswer) modelInUse = model
            return chooseAnswer
        }
        override fun remove(model: SpeechModel) {
            removeCalls += model
        }
    }
    private val page = ReadingSpeedConfigurable({ setting }, { modelInUse }, { models })
    private val component = runInEdtAndGet { page.createComponent() }

    @After
    fun closeThePage() {
        runInEdtAndWait { page.disposeUIResources() }
    }

    @Test
    fun thePluginRegistersAnApplicationPageNamedAloudRiteUnderTools() {
        val registered = Configurable.APPLICATION_CONFIGURABLE.extensionList.single {
            it.pluginDescriptor.pluginId.idString == "com.ritenoar.aloudrite"
        }

        assertEquals("AloudRite", registered.displayName)
        assertEquals("tools", registered.parentId)
        assertEquals("com.ritenoar.aloudrite.ReadingSpeedConfigurable", registered.instanceClass)
    }

    @Test
    fun thePageShowsTheLabelAndTheNineteenSteps() {
        val shown = runInEdtAndGet {
            val list = JList<ReadingSpeed>()
            (0 until page.speedChoice.itemCount).map { index ->
                val item = page.speedChoice.getItemAt(index)
                val rendered = page.speedChoice.renderer.getListCellRendererComponent(list, item, index, false, false)
                (rendered as JLabel).text
            }
        }

        assertEquals("Reading speed:", page.speedLabel.text)
        assertEquals(ReadingSpeed.STEPS.map { it.display }, shown)
        assertEquals("0.75×", shown.first())
        assertEquals("2.0×", shown.last())
    }

    @Test
    fun theChoiceKeepsItsOwnHeightOnATallPage() {
        val (height, preferredHeight) = runInEdtAndGet {
            component.setSize(600, 500)
            layOutAll(component)
            page.speedChoice.height to page.speedChoice.preferredSize.height
        }

        assertEquals(preferredHeight, height)
    }

    private fun layOutAll(container: Container) {
        container.doLayout()
        for (child in container.components) if (child is Container) layOutAll(child)
    }

    @Test
    fun resetSelectsTheSpeedOfTheSetting() {
        setting.set(speed("1.3"), afterAWait = false)

        runInEdtAndWait { page.reset() }

        assertEquals(speed("1.3"), runInEdtAndGet { page.speedChoice.selectedItem })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun anotherChoiceIsModifiedAndApplySetsItWithNoWait() {
        runInEdtAndWait { page.reset() }

        runInEdtAndWait { page.speedChoice.selectedItem = speed("1.6") }
        assertTrue(runInEdtAndGet { page.isModified })
        runInEdtAndWait { page.apply() }

        assertEquals(speed("1.6"), setting.speed)
        assertEquals(listOf(speed("1.6") to false), changes)
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aTypedSpeedBetweenTwoStepsIsShownAmongThemInItsPlace() {
        setting.set(speed("1.37"), afterAWait = false)

        runInEdtAndWait { page.reset() }

        assertEquals(speed("1.37"), runInEdtAndGet { page.speedChoice.selectedItem })
        assertEquals(ReadingSpeed.stepsWith(speed("1.37")), runInEdtAndGet {
            (0 until page.speedChoice.itemCount).map { page.speedChoice.getItemAt(it) }
        })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun anOpenPageFollowsAChangeMadeElsewhereAndAnApplyWithoutAnEditWritesNothing() {
        runInEdtAndWait { page.reset() }

        setting.set(speed("1.37"), afterAWait = false)
        dispatchTheEvents()
        assertEquals(speed("1.37"), runInEdtAndGet { page.speedChoice.selectedItem })
        assertTrue(runInEdtAndGet { speed("1.37") in (0 until page.speedChoice.itemCount).map { page.speedChoice.getItemAt(it) } })
        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        dispatchTheEvents()

        assertEquals(ReadingSpeed.FASTEST, runInEdtAndGet { page.speedChoice.selectedItem })
        assertFalse(runInEdtAndGet { page.isModified })
        changes.clear()
        runInEdtAndWait { page.apply() }
        assertEquals(ReadingSpeed.FASTEST, setting.speed)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun aClosedPageNoLongerFollowsTheSpeed() {
        runInEdtAndWait { page.reset() }
        runInEdtAndWait { page.disposeUIResources() }

        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        dispatchTheEvents()

        assertEquals(ReadingSpeed.NORMAL, runInEdtAndGet { page.speedChoice.selectedItem })
    }

    private fun dispatchTheEvents() {
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    @Test
    fun aSpeedSetInThePanelShowsOnThePageAfterAReset() {
        runInEdtAndWait { page.reset() }

        setting.set(ReadingSpeed.FASTEST, afterAWait = true)
        runInEdtAndWait { page.reset() }

        assertEquals(ReadingSpeed.FASTEST, runInEdtAndGet { page.speedChoice.selectedItem })
    }

    private fun row(model: SpeechModel) = page.modelRows.single { it.speechModel == model }

    private fun showModels() = runInEdtAndWait { page.reset() }

    private fun commentOf(model: SpeechModel) = runInEdtAndGet { row(model).comment.text }

    @Test
    fun thePageShowsAGroupSpeechModelBelowTheSpeedWithOneRowPerModelInOrder() {
        val (speedY, groupY) = runInEdtAndGet {
            component.setSize(800, 500)
            layOutAll(component)
            val title = labelsIn(component).single { it.text == "Speech model" }
            yOnPageOf(page.speedChoice) to yOnPageOf(title)
        }

        assertTrue(groupY > speedY)
        assertEquals(SPEECH_MODELS, page.modelRows.map { it.speechModel })
        assertEquals(SPEECH_MODELS.map { it.displayName }, runInEdtAndGet { page.modelRows.map { it.radioButton.text } })
        showModels()
        assertTrue(commentOf(int8), commentOf(int8).startsWith("132 MB download, 190 MB on disk"))
        assertTrue(commentOf(fp32), commentOf(fp32).startsWith("350 MB download, 401 MB on disk"))
    }

    @Test
    fun theRadioButtonOfTheModelInUseIsSelectedAndItsCommentSaysInUse() {
        modelInUse = fp32
        inPlace += fp32

        showModels()

        assertTrue(runInEdtAndGet { row(fp32).radioButton.isSelected })
        assertFalse(runInEdtAndGet { row(int8).radioButton.isSelected })
        assertTrue(commentOf(fp32), commentOf(fp32).endsWith("on this machine, in use"))
        assertEquals("132 MB download, 190 MB on disk, on this machine", commentOf(int8))
    }

    @Test
    fun theCommentsSayWhichModelIsOnTheMachine() {
        showModels()

        assertTrue(commentOf(int8), commentOf(int8).contains(", on this machine"))
        assertEquals("350 MB download, 401 MB on disk, not on this machine", commentOf(fp32))
    }

    @Test
    fun downloadIsEnabledOnlyForAModelNotOnTheMachineWhileNoDownloadRuns() {
        showModels()
        assertEquals(listOf(false, true), runInEdtAndGet { page.modelRows.map { it.downloadButton.isEnabled } })

        downloadRuns = true
        downloadingNow = fp32
        showModels()

        assertEquals(listOf(false, false), runInEdtAndGet { page.modelRows.map { it.downloadButton.isEnabled } })
        assertEquals("350 MB download, 401 MB on disk, downloading", commentOf(fp32))
    }

    @Test
    fun removeIsEnabledOnlyForAModelInPlaceThatTheManagementAllowsToRemove() {
        inPlace += fp32
        removable += fp32

        showModels()

        assertEquals(listOf(false, true), runInEdtAndGet { page.modelRows.map { it.removeButton.isEnabled } })
    }

    @Test
    fun theEndOfAReadingMakesAModelRemovableAndKeepsAModelSelectedAndNotAppliedYet() {
        inPlace += fp32
        showModels()
        runInEdtAndWait { row(fp32).radioButton.isSelected = true }
        assertFalse(runInEdtAndGet { row(int8).removeButton.isEnabled })

        removable += int8
        ApplicationManager.getApplication().messageBus.syncPublisher(READING_CHANGED).readingChanged()
        dispatchTheEvents()

        assertTrue(runInEdtAndGet { row(int8).removeButton.isEnabled })
        assertTrue(runInEdtAndGet { row(fp32).radioButton.isSelected })
    }

    @Test
    fun theButtonsAskTheManagementToDownloadAndRemoveTheirModel() {
        showModels()

        runInEdtAndWait {
            row(fp32).downloadButton.doClick()
            row(int8).removeButton.isEnabled = true
            row(int8).removeButton.doClick()
        }

        assertEquals(listOf(fp32), downloadCalls)
        assertEquals(listOf(int8), removeCalls)
    }

    @Test
    fun selectingTheOtherModelMakesThePageModifiedAndApplyChoosesIt() {
        inPlace += fp32
        showModels()

        runInEdtAndWait { row(fp32).radioButton.isSelected = true }
        assertTrue(runInEdtAndGet { page.isModified })
        runInEdtAndWait { page.apply() }

        assertEquals(listOf(fp32), chooseCalls)
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aChoiceDeclinedAtApplySelectsTheModelInUseAgainAndThePageIsNotModified() {
        chooseAnswer = false
        showModels()

        runInEdtAndWait { row(fp32).radioButton.isSelected = true }
        runInEdtAndWait { page.apply() }

        assertEquals(listOf(fp32), chooseCalls)
        assertTrue(runInEdtAndGet { row(int8).radioButton.isSelected })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aPendingChoiceIsShownSelectedAndIsNotModifiedUntilAnotherModelIsSelected() {
        pendingNow = fp32
        showModels()

        assertTrue(runInEdtAndGet { row(fp32).radioButton.isSelected })
        assertFalse(runInEdtAndGet { page.isModified })

        runInEdtAndWait { row(int8).radioButton.isSelected = true }

        assertTrue(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aChoiceMadeOnAnotherPageShowsOnThisOne() {
        inPlace += fp32
        showModels()

        modelInUse = fp32
        ApplicationManager.getApplication().messageBus.syncPublisher(SPEECH_MODEL_CHANGED).modelChanged(fp32)
        dispatchTheEvents()

        assertTrue(runInEdtAndGet { row(fp32).radioButton.isSelected })
        assertFalse(runInEdtAndGet { page.isModified })
        assertTrue(commentOf(fp32).endsWith("in use"))
    }

    @Test
    fun aChangeOfTheModelsShowsWhatIsOnTheMachineAndKeepsTheSelection() {
        showModels()
        runInEdtAndWait { row(fp32).radioButton.isSelected = true }

        inPlace += fp32
        ApplicationManager.getApplication().messageBus.syncPublisher(SPEECH_MODELS_CHANGED).modelsChanged()
        dispatchTheEvents()

        assertEquals("350 MB download, 401 MB on disk, on this machine", commentOf(fp32))
        assertTrue(runInEdtAndGet { row(fp32).radioButton.isSelected })
    }

    @Test
    fun aChangeOfTheSpeedMeanwhileLeavesAModelSelectedAndNotAppliedAsItIs() {
        showModels()
        runInEdtAndWait { row(fp32).radioButton.isSelected = true }

        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        dispatchTheEvents()

        assertEquals(ReadingSpeed.FASTEST, runInEdtAndGet { page.speedChoice.selectedItem })
        assertTrue(runInEdtAndGet { row(fp32).radioButton.isSelected })
    }

    @Test
    fun aChangeOfTheModelMeanwhileLeavesASpeedChosenAndNotAppliedAsItIs() {
        showModels()
        runInEdtAndWait { page.speedChoice.selectedItem = speed("1.6") }

        modelInUse = fp32
        ApplicationManager.getApplication().messageBus.syncPublisher(SPEECH_MODEL_CHANGED).modelChanged(fp32)
        dispatchTheEvents()

        assertEquals(speed("1.6"), runInEdtAndGet { page.speedChoice.selectedItem })
    }

    @Test
    fun theRadioButtonsKeepTheirOwnHeightOnATallPage() {
        val heights = runInEdtAndGet {
            component.setSize(800, 700)
            layOutAll(component)
            page.modelRows.map { it.radioButton.height to it.radioButton.preferredSize.height }
        }

        heights.forEach { (height, preferredHeight) -> assertEquals(preferredHeight, height) }
    }

    private fun labelsIn(container: Container): List<JLabel> =
        container.components.flatMap { child ->
            (if (child is JLabel) listOf(child) else emptyList()) + (if (child is Container) labelsIn(child) else emptyList())
        }

    private fun yOnPageOf(target: java.awt.Component): Int = SwingUtilities.convertPoint(target.parent, target.location, component).y

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
