package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service

// The second button on the tab, shown on the tab of the file that is read, also while its reading is paused
class StopReadingAction(private val readings: () -> Readings) : AnAction() {

    constructor() : this({ service<SpeechService>() })

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val target = targetOf(event)
        event.presentation.isEnabledAndVisible = target != null && readings().isReading(target)
        event.presentation.icon = AllIcons.Actions.Suspend
    }

    override fun actionPerformed(event: AnActionEvent) {
        targetOf(event)?.let { readings().stop(it) }
    }

    private fun targetOf(event: AnActionEvent): ReadingTarget? =
        event.getData(CommonDataKeys.VIRTUAL_FILE)?.let { ReadingTarget(event.project, it) }
}
