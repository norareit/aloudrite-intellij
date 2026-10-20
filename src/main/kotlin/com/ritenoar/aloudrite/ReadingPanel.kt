package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JPanel

// What the tool window of a project shows of the one reading of the IDE, and its pause, resume and stop
class ReadingPanel(
    private val ownProject: Project,
    private val reading: () -> CurrentReading,
) : JPanel(VerticalLayout(JBUI.scale(GAP))) {

    internal val nothingReadLabel = JBLabel(NOTHING_READ_TEXT)
    internal val fileLabel = JBLabel()
    internal val projectLabel = JBLabel()
    internal val pauseButton = JButton(PAUSE_TEXT)
    internal val stopButton = JButton(STOP_TEXT)

    // The layout gives every row the width of the pane, which the labels need to wrap in a narrow one. The row of the
    // buttons lays them out from the left, so that each keeps its own width
    internal val controlRow = JPanel(HorizontalLayout(JBUI.scale(GAP))).apply {
        isOpaque = false
        add(pauseButton)
        add(stopButton)
    }

    // What the labels show, so that a press acts on what the user saw and not on a reading that came after. Whether
    // it was paused decides whether the first button pauses or resumes
    private var shownTarget: ReadingTarget? = null
    private var shownPaused = false

    init {
        border = JBUI.Borders.empty(GAP)
        add(nothingReadLabel)
        add(fileLabel)
        add(projectLabel)
        add(controlRow)
        pauseButton.addActionListener { pauseOrResume(System.nanoTime()) }
        stopButton.addActionListener { shownTarget?.let { reading().stop(it) } }
    }

    // A reading ends on a pooled thread, so the change is handed to the EDT. Any modality, so that the panel follows
    // also while a dialog is open, such as the message of a failure, since setting a label touches no model of the IDE
    fun followTheReading(parentDisposable: Disposable) {
        ApplicationManager.getApplication().messageBus.connect(parentDisposable).subscribe(
            READING_CHANGED,
            ReadingChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheCurrentReading, ModalityState.any())
            },
        )
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
        const val GAP = 8
    }
}
