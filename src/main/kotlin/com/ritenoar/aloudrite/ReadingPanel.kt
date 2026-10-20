package com.ritenoar.aloudrite

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JPanel

// What the tool window of a project shows of the one reading of the IDE, and its stop
class ReadingPanel(
    private val ownProject: Project,
    private val reading: () -> CurrentReading,
) : JPanel(VerticalLayout(JBUI.scale(GAP))) {

    internal val nothingReadLabel = JBLabel(NOTHING_READ_TEXT)
    internal val fileLabel = JBLabel()
    internal val projectLabel = JBLabel()
    internal val stopButton = JButton(STOP_TEXT)

    // What the labels show, so that a press of the stop stops what the user saw and not a reading that came after
    private var shownTarget: ReadingTarget? = null

    init {
        border = JBUI.Borders.empty(GAP)
        add(nothingReadLabel)
        add(fileLabel)
        add(projectLabel)
        add(stopButton)
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
        shownTarget = target
        val otherProject = target?.project?.takeUnless { it === ownProject }
        nothingReadLabel.isVisible = target == null
        fileLabel.isVisible = target != null
        fileLabel.text = target?.let { "Reading ${it.file.name}" }
        projectLabel.isVisible = otherProject != null
        projectLabel.text = otherProject?.let { "In the project ${it.name}" }
        stopButton.isVisible = target != null
        revalidate()
        repaint()
    }

    internal companion object {
        const val NOTHING_READ_TEXT =
            "<html>Nothing is being read.<br>Start a reading with \"Read Aloud\" on the tab of a Markdown file.</html>"
        const val STOP_TEXT = "Stop"
        const val GAP = 8
    }
}
