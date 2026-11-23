package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.ui.UIUtil
import com.ritenoar.aloudrite.core.ALIGNER_MODEL
import com.ritenoar.aloudrite.core.ModelPackage
import com.ritenoar.aloudrite.core.ReadingSpeed
import com.ritenoar.aloudrite.core.SPEECH_MODELS
import com.ritenoar.aloudrite.core.SpeechModel
import com.ritenoar.aloudrite.core.VOICES
import com.ritenoar.aloudrite.core.Voice
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList

// The page "AloudRite" under Tools in the settings, which shows the one speed and the one model in use of the IDE that
// the tool windows show, chooses the one voice, and manages the models on the machine
class ReadingSpeedConfigurable(
    private val setting: () -> ReadingSpeedSetting,
    private val modelInUse: () -> SpeechModel,
    private val models: () -> ManagedSpeechModels,
    private val highlightSetting: () -> HighlightSetting,
    private val voiceSetting: () -> VoiceSetting,
) : Configurable {

    constructor() : this(
        { service<ReadingSpeedSetting>() },
        { service<SpeechModelSetting>().modelInUse },
        { service<SpeechModelManagement>() },
        { service<HighlightSetting>() },
        { service<VoiceSetting>() },
    )

    internal val speedLabel = JBLabel(SPEED_LABEL_TEXT)
    internal val speedChoice = ComboBox(ReadingSpeed.STEPS.toTypedArray()).apply {
        renderer = textRenderer { it.display }
        speedLabel.labelFor = this
    }

    internal val voiceLabel = JBLabel(VOICE_LABEL_TEXT)
    internal val voiceChoice = ComboBox(VOICES.toTypedArray()).apply {
        renderer = textRenderer { it.displayName }
        voiceLabel.labelFor = this
    }

    internal val textEditorCheckBox = JBCheckBox(TEXT_EDITOR_TEXT)
    internal val previewCheckBox = JBCheckBox(PREVIEW_TEXT)
    internal val sentenceCheckBox = JBCheckBox(SENTENCE_TEXT)
    internal val wordCheckBox = JBCheckBox(WORD_TEXT)

    // One row per entry of SPEECH_MODELS, in its order
    internal val modelRows: List<ModelRow> = SPEECH_MODELS.map(::ModelRow)

    internal val alignerRow = AlignerRow()

    // Ends the following of the speed and of the models when the page closes
    private var whileOpen: Disposable? = null

    internal inner class ModelRow(val speechModel: SpeechModel) {
        val radioButton = JBRadioButton(speechModel.displayName)
        val comment = JBLabel("", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER)
        val downloadButton = JButton(DOWNLOAD_TEXT).apply { addActionListener { models().download(speechModel) } }
        val removeButton = JButton(REMOVE_TEXT).apply { addActionListener { models().remove(speechModel) } }

        // What is on the machine and what runs is asked each time, since it changes without the page
        fun show() {
            val management = models()
            val inPlace = management.isInPlace(speechModel)
            comment.text = modelComment(
                speechModel, inPlace, management.downloadingModel == speechModel, speechModel == modelInUse())
            downloadButton.isEnabled = !inPlace && !management.isDownloadRunning
            removeButton.isEnabled = management.canRemove(speechModel)
        }
    }

    // The aligner model has no "Remove" button: it goes with the last speech model
    internal inner class AlignerRow {
        val label = JBLabel(ALIGNER_MODEL.displayName)
        val comment = JBLabel("", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER)
        val downloadButton = JButton(DOWNLOAD_TEXT).apply { addActionListener { models().downloadAlignerModel() } }

        fun show() {
            val management = models()
            val inPlace = management.isAlignerModelInPlace()
            comment.text = alignerComment(inPlace, management.downloadingModel == ALIGNER_MODEL)
            downloadButton.isEnabled =
                !inPlace && !management.isDownloadRunning && SPEECH_MODELS.any(management::isInPlace)
        }
    }

    // Not SimpleListCellRenderer.create, of which both overloads are deprecated
    private fun <T> textRenderer(textOf: (T) -> String): SimpleListCellRenderer<T> = object : SimpleListCellRenderer<T>() {
        override fun customize(list: JList<out T>, value: T?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.let(textOf) ?: ""
        }
    }

    override fun getDisplayName(): String = DISPLAY_NAME

    // The layout of the IDE's own settings pages, which keeps each row at the top and each control at its own size. A
    // change made elsewhere while the page is open is shown at once, so that an apply does not write back a value that
    // is no longer the value. The settings are a modal dialog, so the change is shown in any modality. Each change
    // resets only its own part of the page, so that what is chosen here and not applied yet is kept
    override fun createComponent(): JComponent {
        val following = Disposer.newDisposable("AloudRite settings page").also { whileOpen = it }
        val connection = ApplicationManager.getApplication().messageBus.connect(following)
        connection.subscribe(READING_SPEED_CHANGED, ReadingSpeedChangeListener { later(::resetSpeed) })
        connection.subscribe(VOICE_CHANGED, VoiceChangeListener { later(::resetVoice) })
        connection.subscribe(SPEECH_MODEL_CHANGED, SpeechModelChangeListener { later(::resetModel) })
        connection.subscribe(SPEECH_MODELS_CHANGED, SpeechModelsChangeListener { later(::showModels) })
        // The model of a reading cannot be removed, so the end of a reading may make a model removable
        connection.subscribe(READING_CHANGED, ReadingChangeListener { later(::showModels) })
        listOf(textEditorCheckBox, previewCheckBox).forEach { it.addItemListener { showWhetherHighlightsCanBeChosen() } }
        return panel {
            row(speedLabel) { cell(speedChoice) }
            row(voiceLabel) { cell(voiceChoice) }
            row { cell(textEditorCheckBox) }
            row { cell(previewCheckBox) }
            row { cell(sentenceCheckBox) }
            row { cell(wordCheckBox) }
            group(MODEL_GROUP_TEXT) {
                // The group of the layout makes the radio buttons one choice
                buttonsGroup {
                    modelRows.forEach { modelRow ->
                        row {
                            cell(modelRow.radioButton).widthGroup(RADIO_WIDTH_GROUP)
                            cell(modelRow.downloadButton).widthGroup(BUTTON_WIDTH_GROUP)
                            cell(modelRow.removeButton).widthGroup(BUTTON_WIDTH_GROUP)
                        }
                        // The rows of one panel share their columns, so the buttons of the models line up. The
                        // description is indented to the label of the radio button
                        row {
                            cell(modelRow.comment).customize(UnscaledGaps(left = COMMENT_INDENT))
                        }
                    }
                }
                row {
                    cell(alignerRow.label).widthGroup(RADIO_WIDTH_GROUP)
                    cell(alignerRow.downloadButton).widthGroup(BUTTON_WIDTH_GROUP)
                }
                row {
                    cell(alignerRow.comment).customize(UnscaledGaps(left = COMMENT_INDENT))
                }
            }
        }
    }

    private fun later(work: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(work, ModalityState.any())
    }

    override fun disposeUIResources() {
        whileOpen?.let(Disposer::dispose)
        whileOpen = null
    }

    override fun isModified(): Boolean =
        chosenSpeed() != setting().speed || chosenVoice() != voiceSetting().voice || chosenModel() != modelToShow() || chosenHighlights() != highlightSetting().choices

    // A value chosen here is one change, so the speed is set with no wait. A model that is not on the machine is asked
    // for a download first, and when that is declined the model that was in use is shown again
    override fun apply() {
        setting().set(chosenSpeed(), afterAWait = false)
        voiceSetting().choose(chosenVoice())
        highlightSetting().choices = chosenHighlights()
        val model = chosenModel()
        if (model != modelToShow() && !models().choose(model)) resetModel()
    }

    override fun reset() {
        showHighlights(highlightSetting().choices)
        resetSpeed()
        resetVoice()
        resetModel()
    }

    private fun chosenHighlights() = HighlightChoices(
        inTextEditor = textEditorCheckBox.isSelected,
        inPreview = previewCheckBox.isSelected,
        sentence = sentenceCheckBox.isSelected,
        word = wordCheckBox.isSelected,
    )

    private fun showHighlights(choices: HighlightChoices) {
        textEditorCheckBox.isSelected = choices.inTextEditor
        previewCheckBox.isSelected = choices.inPreview
        sentenceCheckBox.isSelected = choices.sentence
        wordCheckBox.isSelected = choices.word
        showWhetherHighlightsCanBeChosen()
    }

    // Greyed out rather than hidden, with their check marks as they were, since a disabled box keeps its mark
    private fun showWhetherHighlightsCanBeChosen() {
        val canBeChosen = chosenHighlights().highlightsCanBeChosen
        sentenceCheckBox.isEnabled = canBeChosen
        wordCheckBox.isEnabled = canBeChosen
    }

    // The choices are made again, so that a speed typed between two steps is among them while it is the speed
    private fun resetSpeed() {
        val speed = setting().speed
        speedChoice.model = DefaultComboBoxModel(ReadingSpeed.stepsWith(speed).toTypedArray())
        speedChoice.selectedItem = speed
    }

    private fun resetVoice() {
        voiceChoice.selectedItem = voiceSetting().voice
    }

    // The model that is chosen and will be in use once it is in place, or else the model in use
    private fun modelToShow(): SpeechModel = models().pendingChoice ?: modelInUse()

    private fun resetModel() {
        val shown = modelToShow()
        modelRows.single { it.speechModel == shown }.radioButton.isSelected = true
        showModels()
    }

    private fun showModels() {
        modelRows.forEach(ModelRow::show)
        alignerRow.show()
    }

    private fun chosenSpeed(): ReadingSpeed = speedChoice.selectedItem as ReadingSpeed

    private fun chosenVoice(): Voice = voiceChoice.selectedItem as Voice

    private fun chosenModel(): SpeechModel = modelRows.single { it.radioButton.isSelected }.speechModel

    internal companion object {
        const val DISPLAY_NAME = "AloudRite"
        const val SPEED_LABEL_TEXT = "Reading speed:"
        const val VOICE_LABEL_TEXT = "Voice:"
        const val TEXT_EDITOR_TEXT = "Highlight in the text editor"
        const val PREVIEW_TEXT = "Highlight in the preview"
        const val SENTENCE_TEXT = "Highlight the sentence being read"
        const val WORD_TEXT = "Highlight the word being read"
        const val MODEL_GROUP_TEXT = "Speech model"
        const val DOWNLOAD_TEXT = "Download"
        const val REMOVE_TEXT = "Remove"
        const val COMMENT_INDENT = 24
        const val RADIO_WIDTH_GROUP = "model radio buttons"
        const val BUTTON_WIDTH_GROUP = "model buttons"

        // The sizes, then where the model is
        fun modelComment(model: SpeechModel, inPlace: Boolean, downloading: Boolean, inUse: Boolean): String =
            listOfNotNull(sizesAndPlace(model.modelPackage, inPlace, downloading), "in use".takeIf { inUse }).joinToString(", ")

        fun alignerComment(inPlace: Boolean, downloading: Boolean): String =
            sizesAndPlace(ALIGNER_MODEL.modelPackage, inPlace, downloading)

        private fun sizesAndPlace(modelPackage: ModelPackage, inPlace: Boolean, downloading: Boolean): String {
            val sizes = "${megabytes(modelPackage.sizeInBytes)} MB download, ${megabytes(modelPackage.unpackedSizeInBytes)} MB on disk"
            val place = when {
                downloading -> "downloading"
                inPlace -> "on this machine"
                else -> "not on this machine"
            }
            return "$sizes, $place"
        }
    }
}
