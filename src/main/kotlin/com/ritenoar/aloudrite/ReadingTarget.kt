package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// What is read. The same file in another project is another target, since its tab and its stop button are others
data class ReadingTarget(val project: Project?, val file: VirtualFile)

// A press on a button of the tab comes from its toolbar, and a press of a key or in Find Action from none
fun isOnTheTab(event: AnActionEvent): Boolean = event.uiKind is ActionUiKind.Toolbar

// The file of the tab whose button was pressed
fun targetOfTheTab(event: AnActionEvent): ReadingTarget? =
    event.getData(CommonDataKeys.VIRTUAL_FILE)?.let { ReadingTarget(event.project, it) }
