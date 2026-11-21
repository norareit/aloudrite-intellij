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
import org.junit.Assert.assertSame
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
    private var downloadingNow: InstallablePackage? = null
    private var alignerInPlace = false
    private var alignerDownloads = 0
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
        override fun isAlignerModelInPlace() = alignerInPlace
        override fun downloadAlignerModel() {
            alignerDownloads++
        }
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
    private val highlightSetting = HighlightSetting()
    private val voiceSetting = VoiceSetting { voice ->
        ApplicationManager.getApplication().messageBus.syncPublisher(VOICE_CHANGED).voiceChanged(voice)
    }
    private val page = ReadingSpeedConfigurable({ setting }, { modelInUse }, { models }, { highlightSetting }, { voiceSetting })
    private val emma = voiceNamed("bf_emma")!!
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

    private fun allComponents(container: Container): List<java.awt.Component> =
        container.components.flatMap { listOf(it) + if (it is Container) allComponents(it) else emptyList() }

    private val fourBoxes get() = listOf(page.textEditorCheckBox, page.previewCheckBox, page.sentenceCheckBox, page.wordCheckBox)

    private fun shownChoices() = runInEdtAndGet { fourBoxes.map { it.isSelected } }

    @Test
    fun thePageShowsFourCheckboxesInOrderUnderTheSpeedAndAboveTheModelsAndNotTheOldOne() {
        val (speed, ys, models) = runInEdtAndGet {
            component.setSize(800, 900)
            layOutAll(component)
            Triple(yOnPageOf(page.speedChoice), fourBoxes.map(::yOnPageOf), yOnPageOf(page.modelRows.first().radioButton))
        }

        assertEquals(
            listOf("Highlight in the text editor", "Highlight in the preview", "Highlight the sentence being read",
                "Highlight the word being read"),
            fourBoxes.map { it.text },
        )
        assertEquals(ys.sorted(), ys)
        assertEquals(4, ys.toSet().size)
        assertTrue(speed < ys.first())
        assertTrue(ys.last() < models)
        assertTrue(runInEdtAndGet { allComponents(component).none { (it as? javax.swing.AbstractButton)?.text == "Highlight the words being read" } })
    }

    @Test
    fun afterResetWithTheDefaultsAllFourAreCheckedAndEnabledAndThePageIsNotModified() {
        runInEdtAndWait { page.reset() }

        assertEquals(listOf(true, true, true, true), shownChoices())
        assertTrue(runInEdtAndGet { fourBoxes.all { it.isEnabled } })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun eachBoxChangedOnItsOwnMakesThePageModifiedAndApplyStoresItWithTheOthersAsTheyWere() {
        val defaults = HighlightChoices()
        val boxesAndChanges = listOf<Pair<(ReadingSpeedConfigurable) -> javax.swing.JCheckBox, (HighlightChoices) -> HighlightChoices>>(
            { configurable: ReadingSpeedConfigurable -> configurable.textEditorCheckBox } to { choices -> choices.copy(inTextEditor = !choices.inTextEditor) },
            { configurable: ReadingSpeedConfigurable -> configurable.previewCheckBox } to { choices -> choices.copy(inPreview = !choices.inPreview) },
            { configurable: ReadingSpeedConfigurable -> configurable.sentenceCheckBox } to { choices -> choices.copy(sentence = !choices.sentence) },
            { configurable: ReadingSpeedConfigurable -> configurable.wordCheckBox } to { choices -> choices.copy(word = !choices.word) },
        )
        for ((box, change) in boxesAndChanges) {
            highlightSetting.choices = defaults
            runInEdtAndWait { page.reset() }

            runInEdtAndWait { box(page).isSelected = !box(page).isSelected }
            assertTrue(runInEdtAndGet { page.isModified })
            assertEquals(defaults, highlightSetting.choices)
            runInEdtAndWait { page.apply() }

            assertEquals(change(defaults), highlightSetting.choices)
            assertFalse(runInEdtAndGet { page.isModified })
        }
    }

    @Test
    fun resetShowsEachOfTheFourStoredValues() {
        highlightSetting.choices = HighlightChoices(inTextEditor = false, inPreview = true, sentence = false, word = true)

        runInEdtAndWait { page.reset() }

        assertEquals(listOf(false, true, false, true), shownChoices())
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun clearingBothViewsGreysOutTheHighlightsWithTheirMarksAndCheckingOneEnablesThemAgain() {
        for (marks in listOf(true to false, false to true)) {
            highlightSetting.choices = HighlightChoices(sentence = marks.first, word = marks.second)
            runInEdtAndWait { page.reset() }

            runInEdtAndWait { page.textEditorCheckBox.isSelected = false }
            assertTrue(runInEdtAndGet { page.sentenceCheckBox.isEnabled && page.wordCheckBox.isEnabled })
            runInEdtAndWait { page.previewCheckBox.isSelected = false }
            assertFalse(runInEdtAndGet { page.sentenceCheckBox.isEnabled || page.wordCheckBox.isEnabled })
            assertEquals(marks.toList(), runInEdtAndGet { listOf(page.sentenceCheckBox.isSelected, page.wordCheckBox.isSelected) })

            runInEdtAndWait { page.previewCheckBox.isSelected = true }
            assertTrue(runInEdtAndGet { page.sentenceCheckBox.isEnabled && page.wordCheckBox.isEnabled })
            assertEquals(marks.toList(), runInEdtAndGet { listOf(page.sentenceCheckBox.isSelected, page.wordCheckBox.isSelected) })
        }
    }

    @Test
    fun applyWhileTheHighlightsAreGreyedOutStoresTheirMarksAsShown() {
        highlightSetting.choices = HighlightChoices(sentence = true, word = true)
        runInEdtAndWait { page.reset() }
        runInEdtAndWait {
            page.textEditorCheckBox.isSelected = false
            page.previewCheckBox.isSelected = false
        }

        runInEdtAndWait { page.apply() }

        assertEquals(HighlightChoices(inTextEditor = false, inPreview = false, sentence = true, word = true), highlightSetting.choices)
    }

    @Test
    fun resetWithBothViewsStoredAsClearedShowsTheHighlightsGreyedOut() {
        highlightSetting.choices = HighlightChoices(inTextEditor = false, inPreview = false, sentence = true, word = false)

        runInEdtAndWait { page.reset() }

        assertFalse(runInEdtAndGet { page.sentenceCheckBox.isEnabled || page.wordCheckBox.isEnabled })
        assertEquals(listOf(true, false), runInEdtAndGet { listOf(page.sentenceCheckBox.isSelected, page.wordCheckBox.isSelected) })
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
    fun anOpenPageShowsTheSpeedAfterEachKey() {
        runInEdtAndWait { page.reset() }

        pressTheKeyOf(ReadFasterAction { setting }, null)
        dispatchTheEvents()
        assertEquals(speed("1.05"), runInEdtAndGet { page.speedChoice.selectedItem })

        pressTheKeyOf(ReadSlowerAction { setting }, null)
        pressTheKeyOf(ReadSlowerAction { setting }, null)
        dispatchTheEvents()
        assertEquals(speed("0.95"), runInEdtAndGet { page.speedChoice.selectedItem })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aClosedPageNoLongerFollowsTheSpeed() {
        runInEdtAndWait { page.reset() }
        runInEdtAndWait { page.disposeUIResources() }

        setting.set(ReadingSpeed.FASTEST, afterAWait = false)
        dispatchTheEvents()

        assertEquals(ReadingSpeed.NORMAL, runInEdtAndGet { page.speedChoice.selectedItem })
    }

    @Test
    fun thePageShowsTheLabelVoiceForTheThirteenNamesInOrder() {
        val shown = runInEdtAndGet {
            val list = JList<Voice>()
            (0 until page.voiceChoice.itemCount).map { index ->
                val rendered = page.voiceChoice.renderer.getListCellRendererComponent(list, page.voiceChoice.getItemAt(index), index, false, false)
                (rendered as JLabel).text
            }
        }

        assertEquals("Voice:", page.voiceLabel.text)
        assertSame(page.voiceChoice, page.voiceLabel.labelFor)
        assertEquals(VOICES.map { it.displayName }, shown)
        assertEquals(13, shown.size)
    }

    @Test
    fun resetShowsTheVoiceOfTheSettingAndThePageIsNotModified() {
        runInEdtAndWait { page.reset() }
        assertEquals(DEFAULT_VOICE, runInEdtAndGet { page.voiceChoice.selectedItem })

        voiceSetting.choose(emma)
        runInEdtAndWait { page.reset() }

        assertEquals(emma, runInEdtAndGet { page.voiceChoice.selectedItem })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun anotherVoiceMakesThePageModifiedAndApplyChoosesItAndLeavesThePageUnmodified() {
        runInEdtAndWait { page.reset() }

        runInEdtAndWait { page.voiceChoice.selectedItem = emma }
        assertTrue(runInEdtAndGet { page.isModified })
        runInEdtAndWait { page.apply() }

        assertEquals(emma, voiceSetting.voice)
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aVoiceChosenWhileThePageIsOpenIsShownAndLeavesASpeedChosenAndNotAppliedAsItIs() {
        runInEdtAndWait { page.reset() }
        runInEdtAndWait { page.speedChoice.selectedItem = speed("1.6") }

        voiceSetting.choose(emma)
        dispatchTheEvents()

        assertEquals(emma, runInEdtAndGet { page.voiceChoice.selectedItem })
        assertEquals(speed("1.6"), runInEdtAndGet { page.speedChoice.selectedItem })
    }

    @Test
    fun theRowOfTheVoiceIsDirectlyUnderTheRowOfTheSpeedAndAboveTheHighlights() {
        val (speed, voice, boxes) = runInEdtAndGet {
            component.setSize(800, 900)
            layOutAll(component)
            Triple(yOnPageOf(page.speedChoice), yOnPageOf(page.voiceChoice), fourBoxes.map(::yOnPageOf))
        }

        assertTrue(speed < voice)
        assertTrue(voice < boxes.first())
        assertEquals(voice, runInEdtAndGet { yOnPageOf(page.voiceLabel) }.let { labelY -> voice.takeIf { labelY in speed + 1..boxes.first() - 1 } })
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
    fun theRowOfTheAlignerModelSaysItsSizesAndWhereItIs() {
        showModels()
        assertEquals("Word timing (english-mfa-v3.1.0)", page.alignerRow.label.text)
        assertEquals("48 MB download, 52 MB on disk, not on this machine", runInEdtAndGet { page.alignerRow.comment.text })

        downloadRuns = true
        downloadingNow = ALIGNER_MODEL
        showModels()
        assertEquals("48 MB download, 52 MB on disk, downloading", runInEdtAndGet { page.alignerRow.comment.text })

        downloadRuns = false
        downloadingNow = null
        alignerInPlace = true
        showModels()
        assertEquals("48 MB download, 52 MB on disk, on this machine", runInEdtAndGet { page.alignerRow.comment.text })
    }

    @Test
    fun downloadOfTheAlignerModelIsEnabledOnlyWhereASpeechModelIsInPlaceTheAlignerModelIsNotAndNoDownloadRuns() {
        showModels()
        assertTrue(runInEdtAndGet { page.alignerRow.downloadButton.isEnabled })

        inPlace.clear()
        showModels()
        assertFalse(runInEdtAndGet { page.alignerRow.downloadButton.isEnabled })

        inPlace += int8
        downloadRuns = true
        showModels()
        assertFalse(runInEdtAndGet { page.alignerRow.downloadButton.isEnabled })

        downloadRuns = false
        alignerInPlace = true
        showModels()
        assertFalse(runInEdtAndGet { page.alignerRow.downloadButton.isEnabled })
    }

    @Test
    fun theButtonOfTheAlignerModelAsksTheManagementToDownloadTheAlignerModelAlone() {
        showModels()

        runInEdtAndWait { page.alignerRow.downloadButton.doClick() }

        assertEquals(1, alignerDownloads)
        assertTrue(downloadCalls.isEmpty() && removeCalls.isEmpty())
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

    @Test
    fun theButtonsOfTheModelsLineUpAndTheDescriptionIsBelowItsRadioButton() {
        val rows = runInEdtAndGet {
            component.setSize(800, 700)
            layOutAll(component)
            page.modelRows
        }

        runInEdtAndWait {
            assertEquals(1, rows.map { xOnPageOf(it.downloadButton) }.distinct().size)
            assertEquals(1, rows.map { xOnPageOf(it.removeButton) }.distinct().size)
            assertEquals(1, rows.map { it.downloadButton.size }.distinct().size)
            assertEquals(1, rows.map { it.removeButton.size }.distinct().size)
            rows.forEach {
                assertEquals(centreYOnPageOf(it.radioButton), centreYOnPageOf(it.downloadButton))
                assertTrue(yOnPageOf(it.comment) > yOnPageOf(it.radioButton))
                assertTrue(xOnPageOf(it.comment) > xOnPageOf(it.radioButton))
                assertTrue(xOnPageOf(it.downloadButton) > xOnPageOf(it.radioButton))
            }
        }
    }

    private fun labelsIn(container: Container): List<JLabel> =
        container.components.flatMap { child ->
            (if (child is JLabel) listOf(child) else emptyList()) + (if (child is Container) labelsIn(child) else emptyList())
        }

    private fun xOnPageOf(target: java.awt.Component): Int = SwingUtilities.convertPoint(target.parent, target.location, component).x

    private fun centreYOnPageOf(target: java.awt.Component): Int = yOnPageOf(target) + target.height / 2

    private fun yOnPageOf(target: java.awt.Component): Int = SwingUtilities.convertPoint(target.parent, target.location, component).y

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
