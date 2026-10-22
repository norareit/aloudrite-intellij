package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

// The actions "Read Slower" and "Read Faster" of the keymap, which do what "Slower" and "Faster" in the tool window
// do, wherever the focus is and whether a file is read or not. Each is enabled while a step its way is possible, as
// its button is active
abstract class ReadingSpeedAction(private val setting: () -> ReadingSpeedSetting) : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = canStep(setting().speed)
    }

    override fun actionPerformed(event: AnActionEvent) = step(setting())

    protected abstract fun canStep(speed: ReadingSpeed): Boolean

    protected abstract fun step(setting: ReadingSpeedSetting)
}

class ReadSlowerAction(setting: () -> ReadingSpeedSetting) : ReadingSpeedAction(setting) {

    constructor() : this({ service<ReadingSpeedSetting>() })

    override fun canStep(speed: ReadingSpeed): Boolean = speed.canGoSlower

    override fun step(setting: ReadingSpeedSetting) = setting.slower()

    companion object {
        const val ID = "com.ritenoar.aloudrite.ReadSlower"
    }
}

class ReadFasterAction(setting: () -> ReadingSpeedSetting) : ReadingSpeedAction(setting) {

    constructor() : this({ service<ReadingSpeedSetting>() })

    override fun canStep(speed: ReadingSpeed): Boolean = speed.canGoFaster

    override fun step(setting: ReadingSpeedSetting) = setting.faster()

    companion object {
        const val ID = "com.ritenoar.aloudrite.ReadFaster"
    }
}
