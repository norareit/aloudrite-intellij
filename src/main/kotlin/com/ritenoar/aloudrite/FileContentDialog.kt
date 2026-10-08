package com.ritenoar.aloudrite

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import javax.swing.Action
import javax.swing.JComponent

class FileContentDialog(project: Project?, fileName: String, val content: String) : DialogWrapper(project) {

    init {
        title = fileName
        init()
    }

    override fun createCenterPanel(): JComponent {
        val textArea = JBTextArea(content, VISIBLE_ROWS, VISIBLE_COLUMNS)
        textArea.isEditable = false
        // Opens the dialog scrolled to the top instead of to the end of the text
        textArea.caretPosition = 0
        return JBScrollPane(textArea)
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    private companion object {
        // The preferred size of a text area in a scroll pane follows these, not the text, so the dialog stays modest
        const val VISIBLE_ROWS = 25
        const val VISIBLE_COLUMNS = 80
    }
}
