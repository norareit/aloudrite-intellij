package com.ritenoar.aloudrite

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.testFramework.ApplicationRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import java.lang.reflect.Proxy

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

    @Test
    fun theFirstInitPutsTheButtonOnTheBarAndRemembersIt() {
        val toolWindow = StandInToolWindow()

        ReadingToolWindowFactory().init(toolWindow.proxy)

        assertEquals(true, toolWindow.showsTheButton)
        assertTrue(properties.getBoolean(BUTTON_PLACED_KEY))
    }

    @Test
    fun aLaterInitLeavesTheButtonToTheUser() {
        ReadingToolWindowFactory().init(StandInToolWindow().proxy)
        val toolWindowOfTheNextOpening = StandInToolWindow()

        ReadingToolWindowFactory().init(toolWindowOfTheNextOpening.proxy)

        assertNull(toolWindowOfTheNextOpening.showsTheButton)
    }

    @Test
    fun aLaterInitLeavesAButtonTheUserRemovedRemoved() {
        val toolWindow = StandInToolWindow()
        ReadingToolWindowFactory().init(toolWindow.proxy)
        toolWindow.proxy.isShowStripeButton = false

        ReadingToolWindowFactory().init(toolWindow.proxy)

        assertEquals(false, toolWindow.showsTheButton)
    }

    private val properties get() = PropertiesComponent.getInstance(ProjectManager.getInstance().defaultProject)

    @Before
    @After
    fun forgetTheButton() {
        properties.unsetValue(BUTTON_PLACED_KEY)
    }

    // Answers only what init asks of a tool window, and keeps whether the button is on the bar, null until it is set
    private class StandInToolWindow {
        var showsTheButton: Boolean? = null

        val proxy = Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(ToolWindow::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "getProject" -> ProjectManager.getInstance().defaultProject
                "setShowStripeButton" -> showsTheButton = arguments[0] as Boolean
                else -> throw UnsupportedOperationException("The stand-in tool window has no ${method.name}")
            }
        } as ToolWindow
    }

    companion object {
        private const val BUTTON_PLACED_KEY = "com.ritenoar.aloudrite.toolWindowButtonPlaced"

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
