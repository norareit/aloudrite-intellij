package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

// What the tool window of a project shows of the one reading of the IDE, its pause, resume and stop, and the speed
class ReadingPanel(
    private val ownProject: Project,
    private val reading: () -> CurrentReading,
    private val setting: () -> ReadingSpeedSetting,
) : JPanel(VerticalLayout(JBUI.scale(GAP))) {

    internal val nothingReadLabel = JBLabel(NOTHING_READ_TEXT)
    internal val fileLabel = JBLabel()
    internal val projectLabel = JBLabel()
    internal val pauseButton = JButton(PAUSE_TEXT)
    internal val stopButton = JButton(STOP_TEXT)
    internal val slowerButton = JButton(SLOWER_TEXT)
    internal val speedField = JBTextField(SPEED_FIELD_COLUMNS)
    internal val timesLabel = JBLabel(TIMES_TEXT)
    internal val fasterButton = JButton(FASTER_TEXT)

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
        add(timesLabel)
        add(fasterButton)
    }

    // What the labels show, so that a press acts on what the user saw and not on a reading that came after. Whether
    // it was paused decides whether the first button pauses or resumes
    private var shownTarget: ReadingTarget? = null
    private var shownPaused = false
    private var typedSpeedRefused = false

    init {
        border = JBUI.Borders.empty(GAP)
        add(nothingReadLabel)
        add(fileLabel)
        add(projectLabel)
        add(controlRow)
        add(speedRow)
        pauseButton.addActionListener { pauseOrResume(System.nanoTime()) }
        stopButton.addActionListener { shownTarget?.let { reading().stop(it) } }
        slowerButton.addActionListener { setTheSpeed(setting().speed.slower(), afterAWait = true) }
        fasterButton.addActionListener { setTheSpeed(setting().speed.faster(), afterAWait = true) }
        speedField.addActionListener { setTheTypedSpeed() }
        speedField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) = setTheTypedSpeed()
        })
        escapeInTheField.registerCustomShortcutSet(CommonShortcuts.ESCAPE, speedField)
        showTheSpeed()
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
        slowerButton.isEnabled = speed.canGoSlower
        fasterButton.isEnabled = speed.canGoFaster
        markTheTypedSpeed(refused = false)
    }

    private fun setTheSpeed(speed: ReadingSpeed, afterAWait: Boolean) {
        setting().set(speed, afterAWait)
        showTheSpeed()
    }

    private fun setTheTypedSpeed() {
        val typed = parsedSpeed(speedField.text) ?: return markTheTypedSpeed(refused = true)
        setTheSpeed(typed, afterAWait = false)
    }

    private fun markTheTypedSpeed(refused: Boolean) {
        typedSpeedRefused = refused
        ComponentValidator.getInstance(speedField).ifPresent { it.revalidate() }
    }

    private fun pauseOrResume(pressedAtNanos: Long) {
        val target = shownTarget ?: return
        if (shownPaused) reading().resume(target, pressedAtNanos) else reading().pause(target)
    }

    internal companion object {
        const val NOTHING_READ_TEXT =
            "<html>Nothing is being read.<br>Start a reading with \"Read Aloud\" on the tab of a Markdown file.</html>"
        const val PAUSE_TEXT = "Pause"
        const val RESUME_TEXT = "Resume"
        const val STOP_TEXT = "Stop"
        const val SLOWER_TEXT = "Slower"
        const val FASTER_TEXT = "Faster"
        const val TIMES_TEXT = "×"
        const val REFUSED_SPEED_TEXT = "Type a speed from 0.5 to 2.0"
        const val SPEED_FIELD_COLUMNS = 3
        const val GAP = 8
    }
}
