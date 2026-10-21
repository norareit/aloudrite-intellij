package com.ritenoar.aloudrite

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

// The page "AloudRite" under Tools in the settings, which shows the one speed of the IDE that the tool windows show
class ReadingSpeedConfigurable(private val setting: () -> ReadingSpeedSetting) : Configurable {

    constructor() : this({ service<ReadingSpeedSetting>() })

    internal val speedLabel = JBLabel(SPEED_LABEL_TEXT)
    internal val speedChoice = ComboBox(ReadingSpeed.ALL.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create("") { it.display }
        speedLabel.labelFor = this
    }

    override fun getDisplayName(): String = DISPLAY_NAME

    override fun createComponent(): JComponent = JPanel(HorizontalLayout(JBUI.scale(GAP))).apply {
        add(speedLabel)
        add(speedChoice)
    }

    override fun isModified(): Boolean = chosen() != setting().speed

    // A value chosen here is one change, so it is set with no wait
    override fun apply() {
        setting().set(chosen(), afterAWait = false)
    }

    override fun reset() {
        speedChoice.selectedItem = setting().speed
    }

    private fun chosen(): ReadingSpeed = speedChoice.selectedItem as ReadingSpeed

    internal companion object {
        const val DISPLAY_NAME = "AloudRite"
        const val SPEED_LABEL_TEXT = "Reading speed:"
        const val GAP = 8
    }
}
