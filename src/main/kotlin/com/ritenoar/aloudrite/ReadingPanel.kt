package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.InplaceButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Graphics
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

// What the tool window of a project shows of the one reading of the IDE, its pause, resume and stop, the speed, and the model in use
class ReadingPanel(
    private val ownProject: Project,
    private val reading: () -> CurrentReading,
    private val setting: () -> ReadingSpeedSetting,
    private val modelInUse: () -> SpeechModel,
) : JPanel(VerticalLayout(JBUI.scale(GAP))) {

    internal val nothingReadLabel = JBLabel(NOTHING_READ_TEXT)
    internal val fileLabel = JBLabel()
    internal val projectLabel = JBLabel()
    internal val pauseButton = JButton(PAUSE_TEXT)
    internal val stopButton = JButton(STOP_TEXT)
    internal val speedLabel = JBLabel(SPEED_LABEL_TEXT)
    internal val slowerButton = iconButton(SLOWER_TEXT, AllIcons.General.Remove) {
        setting().slower()
        showTheSpeed()
    }
    internal val fasterButton = iconButton(FASTER_TEXT, AllIcons.General.Add) {
        setting().faster()
        showTheSpeed()
    }

    // The sign is the unit of the number, so it sits inside the field in the grey of a hint and does nothing on a click
    internal val speedField = ExtendableTextField(SPEED_FIELD_COLUMNS).apply {
        addExtension(ExtendableTextComponent.Extension.create(TimesSign(), TimesSign(), null, null))
    }
    internal val speedComment: JLabel = ComponentPanelBuilder.createCommentComponent(SPEED_COMMENT_TEXT, true)

    // Information only: the model is chosen, downloaded and removed on the settings page. The name is HTML so that a
    // longer one wraps at the width of the pane and is never cut
    internal val modelLabel = JBLabel(MODEL_LABEL_TEXT)
    internal val modelNameLabel = JBLabel()

    // Registered on the field, so that Escape there puts the speed back rather than moving the focus to the editor,
    // as Escape in a tool window does
    internal val escapeInTheField: AnAction = DumbAwareAction.create { showTheSpeed() }

    // The layout gives every row the width of the pane, which the labels need to wrap in a narrow one. The row of the
    // buttons lays them out from the left, so that each keeps its own width
    internal val controlRow = JPanel(HorizontalLayout(JBUI.scale(GAP))).apply {
        isOpaque = false
        add(pauseButton)
        add(stopButton)
    }

    internal val speedRow = JPanel(HorizontalLayout(JBUI.scale(GAP))).apply {
        isOpaque = false
        add(slowerButton)
        add(speedField)
        add(fasterButton)
    }

    // What the labels show, so that a press acts on what the user saw and not on a reading that came after. Whether
    // it was paused decides whether the first button pauses or resumes
    private var shownTarget: ReadingTarget? = null
    private var shownPaused = false
    private var typedSpeedRefused = false

    init {
        border = JBUI.Borders.empty(GAP)
        // The tool window focuses its preferred component when it opens, which is this panel, so that the field of
        // the speed does not light up as the first control that could take the focus
        isFocusable = true
        add(nothingReadLabel)
        add(fileLabel)
        add(projectLabel)
        add(controlRow)
        add(speedLabel)
        add(speedRow)
        add(speedComment)
        add(modelLabel)
        add(modelNameLabel)
        pauseButton.addActionListener { pauseOrResume(System.nanoTime()) }
        stopButton.addActionListener { shownTarget?.let { reading().stop(it) } }
        speedField.addActionListener { setTheTypedSpeed() }
        speedField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) = setTheTypedSpeed()
        })
        escapeInTheField.registerCustomShortcutSet(CommonShortcuts.ESCAPE, speedField)
        showTheSpeed()
        showTheModel()
    }

    // A reading ends on a pooled thread, so the change is handed to the EDT. Any modality, so that the panel follows
    // also while a dialog is open, such as the message of a failure, since setting a label touches no model of the IDE.
    // The speed is followed the same way, and the mark of a refused speed needs the same disposable
    fun followTheReading(parentDisposable: Disposable) {
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(
            READING_CHANGED,
            ReadingChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheCurrentReading, ModalityState.any())
            },
        )
        connection.subscribe(
            READING_SPEED_CHANGED,
            ReadingSpeedChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheSpeed, ModalityState.any())
            },
        )
        connection.subscribe(
            SPEECH_MODEL_CHANGED,
            SpeechModelChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheModel, ModalityState.any())
            },
        )
        ComponentValidator(parentDisposable)
            .withValidator { if (typedSpeedRefused) ValidationInfo(REFUSED_SPEED_TEXT, speedField) else null }
            .installOn(speedField)
        // While the field is marked, every edit looks again, so that the mark goes once the text is a speed
        speedField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                if (typedSpeedRefused && parsedSpeed(speedField.text) != null) markTheTypedSpeed(refused = false)
            }
        })
        showTheCurrentReading()
    }

    fun showTheCurrentReading() {
        val target = reading().currentTarget()
        val paused = target != null && reading().isPaused(target)
        shownTarget = target
        shownPaused = paused
        val otherProject = target?.project?.takeUnless { it === ownProject }
        nothingReadLabel.isVisible = target == null
        fileLabel.isVisible = target != null
        fileLabel.text = target?.let { if (paused) "Paused reading ${it.file.name}" else "Reading ${it.file.name}" }
        projectLabel.isVisible = otherProject != null
        projectLabel.text = otherProject?.let { "In the project ${it.name}" }
        pauseButton.text = if (paused) RESUME_TEXT else PAUSE_TEXT
        controlRow.isVisible = target != null
        revalidate()
        repaint()
    }

    // Also what Escape in the field does, which drops what was typed and its mark
    private fun showTheSpeed() {
        val speed = setting().speed
        speedField.text = speed.number
        slowerButton.isActive = speed.canGoSlower
        fasterButton.isActive = speed.canGoFaster
        markTheTypedSpeed(refused = false)
    }

    private fun showTheModel() {
        modelNameLabel.text = "<html>${StringUtil.escapeXmlEntities(modelInUse().displayName)}</html>"
    }

    private fun setTheTypedSpeed() {
        val typed = parsedSpeed(speedField.text) ?: return markTheTypedSpeed(refused = true)
        setting().set(typed, afterAWait = false)
        showTheSpeed()
    }

    private fun markTheTypedSpeed(refused: Boolean) {
        typedSpeedRefused = refused
        ComponentValidator.getInstance(speedField).ifPresent { it.revalidate() }
    }

    private fun pauseOrResume(pressedAtNanos: Long) {
        val target = shownTarget ?: return
        if (shownPaused) reading().resume(target, pressedAtNanos) else reading().pause(target)
    }

    // An inactive button shows its icon greyed and ignores a click, as the disabled buttons of the IDE do
    private fun iconButton(tooltip: String, icon: Icon, onPress: () -> Unit): InplaceButton {
        lateinit var button: InplaceButton
        button = InplaceButton(IconButton(tooltip, icon, icon, IconLoader.getDisabledIcon(icon))) {
            if (button.isActive) onPress()
        }
        return button
    }

    // The "×" after the number, painted in the grey of a hint with the font of the field
    internal class TimesSign : Icon {
        val text = TIMES_TEXT
        private val font = UIUtil.getLabelFont()
        private val metrics = JBLabel().getFontMetrics(font)

        override fun paintIcon(component: Component?, graphics: Graphics, x: Int, y: Int) {
            graphics.font = font
            graphics.color = NamedColorUtil.getInactiveTextColor()
            graphics.drawString(text, x, y + metrics.ascent)
        }

        override fun getIconWidth(): Int = metrics.stringWidth(text)

        override fun getIconHeight(): Int = metrics.height
    }

    internal companion object {
        const val TIMES_TEXT = "×"
        const val NOTHING_READ_TEXT =
            "<html>Nothing is being read.<br>Start a reading with \"Read Aloud\" on the tab of a Markdown file.</html>"
        const val PAUSE_TEXT = "Pause"
        const val RESUME_TEXT = "Resume"
        const val STOP_TEXT = "Stop"
        const val SLOWER_TEXT = "Slower"
        const val FASTER_TEXT = "Faster"
        const val SPEED_LABEL_TEXT = "Reading speed"
        const val MODEL_LABEL_TEXT = "Speech model"
        const val SPEED_COMMENT_TEXT =
            "A change starts the sentence that is playing again at the new speed. This can take a few seconds."
        const val REFUSED_SPEED_TEXT = "Type a speed from 0.75 to 2.0"
        // Room for "0.75" and for the "×" inside the field, which the columns of a text field do not count. Five
        // columns cut off the first digit of a speed with two decimals
        const val SPEED_FIELD_COLUMNS = 6
        const val GAP = 8
    }
}
