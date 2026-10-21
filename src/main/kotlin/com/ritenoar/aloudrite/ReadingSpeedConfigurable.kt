package com.ritenoar.aloudrite

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

// The page "AloudRite" under Tools in the settings, which shows the one speed of the IDE that the tool windows show
class ReadingSpeedConfigurable(private val setting: () -> ReadingSpeedSetting) : Configurable {

    constructor() : this({ service<ReadingSpeedSetting>() })

    internal val speedLabel = JBLabel(SPEED_LABEL_TEXT)
    internal val speedChoice = ComboBox(ReadingSpeed.STEPS.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create("") { it.display }
        speedLabel.labelFor = this
    }

    override fun getDisplayName(): String = DISPLAY_NAME

    // The layout of the IDE's own settings pages, which keeps the row at the top and each control at its own size
    override fun createComponent(): JComponent = panel {
        row(speedLabel) { cell(speedChoice) }
    }

    override fun isModified(): Boolean = chosen() != setting().speed

    // A value chosen here is one change, so it is set with no wait
    override fun apply() {
        setting().set(chosen(), afterAWait = false)
    }

    // The choices are made again, so that a speed typed between two steps is among them while it is the speed
    override fun reset() {
        val speed = setting().speed
        speedChoice.model = DefaultComboBoxModel(ReadingSpeed.stepsWith(speed).toTypedArray())
        speedChoice.selectedItem = speed
    }

    private fun chosen(): ReadingSpeed = speedChoice.selectedItem as ReadingSpeed

    internal companion object {
        const val DISPLAY_NAME = "AloudRite"
        const val SPEED_LABEL_TEXT = "Reading speed:"
    }
}
