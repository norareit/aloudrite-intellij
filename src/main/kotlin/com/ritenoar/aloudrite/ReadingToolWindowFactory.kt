package com.ritenoar.aloudrite

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ReadingToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ReadingPanel(project, { service<SpeechService>() }, { service<ReadingSpeedSetting>() })
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
        // The content is disposed with its tool window and its project, which ends the following
        panel.followTheReading(content)
    }

    // The button is put on the bar once per project. After that it is the user's, so that a button they removed or
    // moved stays where they put it. This also reaches a project that was opened before the plugin was installed,
    // which a default layout of the platform would leave to the layout the user has stored
    override fun init(toolWindow: ToolWindow) {
        val properties = PropertiesComponent.getInstance(toolWindow.project)
        if (properties.getBoolean(BUTTON_PLACED_KEY)) return
        toolWindow.isShowStripeButton = true
        properties.setValue(BUTTON_PLACED_KEY, true)
    }

    private companion object {
        const val BUTTON_PLACED_KEY = "com.ritenoar.aloudrite.toolWindowButtonPlaced"
    }
}
