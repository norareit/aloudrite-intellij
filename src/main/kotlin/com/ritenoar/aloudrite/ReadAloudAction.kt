package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager

class ReadAloudAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = file?.fileType?.name == MARKDOWN_FILE_TYPE_NAME
    }

    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        FileContentDialog(event.project, file.name, document.text).show()
    }

    private companion object {
        // Compared by name so that the plugin needs no class of the Markdown plugin, which may be absent
        const val MARKDOWN_FILE_TYPE_NAME = "Markdown"
    }
}
