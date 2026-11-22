package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.ritenoar.aloudrite.core.CurrentReading

const val PAUSE_READING_TITLE_TEXT = "Pause Reading"
const val RESUME_READING_TITLE_TEXT = "Resume Reading"
const val STOP_READING_TITLE_TEXT = "Stop Reading"

// The first button in the title bar of the tool window, which pauses or resumes the one reading of the IDE whichever
// project's tool window it is in. It is shown also while nothing is read, greyed out, so that the title bar keeps its
// layout. It is not registered in plugin.xml, since "Read Aloud, Pause or Resume" is the action of the keymap
class PauseOrResumeReadingInTheTitleAction(private val reading: () -> CurrentReading<ReadingTarget>) : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val target = reading().currentTarget()
        val paused = target != null && reading().isPaused(target)
        val text = if (paused) RESUME_READING_TITLE_TEXT else PAUSE_READING_TITLE_TEXT
        event.presentation.isVisible = true
        event.presentation.isEnabled = target != null
        event.presentation.text = text
        event.presentation.icon = if (paused) AllIcons.Actions.Resume else AllIcons.Actions.Pause
        // An unregistered action has no shortcut of its own, so the tooltip names the one of the action it stands for
        event.presentation.putClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP, shortcutTooltip(text, ReadAloudAction.ID))
    }

    override fun actionPerformed(event: AnActionEvent) {
        val pressedAtNanos = System.nanoTime()
        val target = reading().currentTarget() ?: return
        if (reading().isPaused(target)) reading().resume(target, pressedAtNanos) else reading().pause(target)
    }
}

// The second button in the title bar, which stops the one reading of the IDE, greyed out while nothing is read
class StopReadingInTheTitleAction(private val reading: () -> CurrentReading<ReadingTarget>) : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = true
        event.presentation.isEnabled = reading().currentTarget() != null
        event.presentation.text = STOP_READING_TITLE_TEXT
        event.presentation.icon = AllIcons.Actions.Suspend
        event.presentation.putClientProperty(
            ActionButton.CUSTOM_HELP_TOOLTIP, shortcutTooltip(STOP_READING_TITLE_TEXT, StopReadingAction.ID))
    }

    override fun actionPerformed(event: AnActionEvent) {
        val target = reading().currentTarget() ?: return
        reading().stop(target)
    }
}
