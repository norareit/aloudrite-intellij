package com.ritenoar.aloudrite

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.intellij.openapi.vfs.VirtualFile

// The platform calls this once the file is open in no editor of the project, so a split editor keeps it read
class ReadingTabListener(private val project: Project) : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        service<SpeechService>().fileClosed(ReadingTarget(project, file))
    }
}

class ReadingProjectListener : ProjectCloseListener {
    override fun projectClosing(project: Project) {
        service<SpeechService>().projectClosed(project)
    }
}
