package com.ritenoar.aloudrite

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class ReadingToolWindowTest {

    @Test
    fun thePluginRegistersOneToolWindowDockedOnTheRightThatDoesNotOpenByItself() {
        val toolWindow = ToolWindowEP.EP_NAME.extensionList.single {
            it.pluginDescriptor.pluginId.idString == "com.ritenoar.aloudrite"
        }

        assertEquals("AloudRite", toolWindow.id)
        assertEquals("right", toolWindow.anchor)
        assertEquals("/icons/readAloud.svg", toolWindow.icon)
        assertTrue(toolWindow.isDoNotActivateOnStart)
        assertEquals("com.ritenoar.aloudrite.ReadingToolWindowFactory", toolWindow.factoryClass)
    }

    @Test
    fun theFactoryOfTheToolWindowWorksWhileTheProjectIsIndexed() {
        val factoryClass = Class.forName("com.ritenoar.aloudrite.ReadingToolWindowFactory")

        assertTrue(ToolWindowFactory::class.java.isAssignableFrom(factoryClass))
        assertTrue(DumbAware::class.java.isAssignableFrom(factoryClass))
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
