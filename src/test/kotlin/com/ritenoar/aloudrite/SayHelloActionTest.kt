package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class SayHelloActionTest {

    @Test
    fun sayHelloIsInTheToolsMenuAndShowsTheGreeting() {
        val actionManager = ActionManager.getInstance()
        val action = actionManager.getAction(SAY_HELLO_ACTION_ID)
        assertNotNull("The action $SAY_HELLO_ACTION_ID is not registered", action)

        val toolsMenu = actionManager.getAction("ToolsMenu") as DefaultActionGroup
        val toolsMenuActionIds = toolsMenu.childActionsOrStubs.map { actionManager.getId(it) }
        assertTrue("The Tools menu does not contain $SAY_HELLO_ACTION_ID", SAY_HELLO_ACTION_ID in toolsMenuActionIds)
        assertEquals("Say Hello", action.templateText)

        val shownMessages = mutableListOf<String>()
        val previousDialog = TestDialogManager.setTestDialog(TestDialog { message ->
            shownMessages += message
            Messages.OK
        })
        try {
            runInEdtAndWait {
                ActionUtil.performAction(action, TestActionEvent.createTestEvent(action))
            }
        } finally {
            TestDialogManager.setTestDialog(previousDialog)
        }

        assertEquals(listOf("Hello, world!"), shownMessages)
    }

    companion object {
        // Opening a project fails in the test environment of IntelliJ IDEA 2026.2.3, so only the application is started
        @JvmField
        @ClassRule
        val application = ApplicationRule()

        private const val SAY_HELLO_ACTION_ID = "com.ritenoar.aloudrite.SayHello"
    }
}
