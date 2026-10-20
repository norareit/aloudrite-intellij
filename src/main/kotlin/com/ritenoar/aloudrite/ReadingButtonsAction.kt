package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionButtonComponent
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JPanel

// The one action of the plugin in the tab row. A group there would be flattened into the row, so the plugin's buttons
// stand in the component of this action, on a background of their own
class ReadingButtonsAction : AnAction(), CustomComponentAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isMarkdown(event.getData(CommonDataKeys.VIRTUAL_FILE))
    }

    // The buttons in the component act, and this action does nothing of its own
    override fun actionPerformed(event: AnActionEvent) = Unit

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent = ReadingButtonsPanel(place)
}

// Holds a toolbar of its own over the group of the reading buttons, made as the tab row makes its own. Its target is
// the panel, so that the data context, and with it the file of the tab, is found up the component tree in the tab row
class ReadingButtonsPanel(place: String) : JPanel(BorderLayout()) {

    internal val toolbar: ActionToolbar =
        ActionManager.getInstance().createActionToolbar(place, readingButtonGroup(), true).apply {
            targetComponent = this@ReadingButtonsPanel
            layoutStrategy = ToolbarLayoutStrategy.NOWRAP_STRATEGY
            (this as? ActionToolbarImpl)?.setCustomButtonLook(ReadingButtonLook())
            component.border = JBUI.Borders.empty()
            component.isOpaque = false
        }

    init {
        isOpaque = false
        add(toolbar.component, BorderLayout.CENTER)
    }

    // Looked up at every paint, so that the background follows a change of theme
    fun backgroundColor(): Color = JBUI.CurrentTheme.ActionButton.hoverBackground()

    override fun paintComponent(graphics: Graphics) {
        val graphics2D = graphics.create() as Graphics2D
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics2D.color = backgroundColor()
            val arc = JBUI.scale(BACKGROUND_ARC)
            graphics2D.fillRoundRect(0, 0, width, height, arc, arc)
        } finally {
            graphics2D.dispose()
        }
        super.paintComponent(graphics)
    }

    private companion object {
        const val READING_BUTTON_GROUP_ID = "com.ritenoar.aloudrite.ReadingButtonGroup"
        const val BACKGROUND_ARC = 6

        fun readingButtonGroup(): ActionGroup = ActionManager.getInstance().getAction(READING_BUTTON_GROUP_ID) as ActionGroup
    }
}

// The background of the panel is the color the IDE's buttons show on hover, so a button on it shows the hover in the
// color of a pressed button, and keeps the rest of the IDE's look
class ReadingButtonLook : IdeaActionButtonLook() {
    public override fun getStateBackground(component: JComponent, state: Int): Color? =
        if (state == ActionButtonComponent.POPPED) {
            JBUI.CurrentTheme.ActionButton.pressedBackground()
        } else {
            super.getStateBackground(component, state)
        }
}
