package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.ActionButtonComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.ApplicationRule
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.JBUI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.awt.Dimension
import javax.swing.JButton

class ReadingButtonsTest {

    private val action get() = registeredAction(READING_BUTTONS_ACTION_ID)

    @Test
    fun theActionOfTheTabRowIsShownForAMarkdownFileOnly() {
        assertTrue(isShownFor(markdownFile("# Title")))
        assertFalse(isShownFor(LightVirtualFile("notes.txt", PlainTextFileType.INSTANCE, "text")))
        assertFalse(isShownFor(LightVirtualFile("notes.md", PlainTextFileType.INSTANCE, "text")))
    }

    @Test
    fun itsComponentIsAPanelOnTheHoverColorOfTheIdesButtons() {
        val panel = panel()

        assertEquals(JBUI.CurrentTheme.ActionButton.hoverBackground(), panel.backgroundColor())
        assertFalse(panel.isOpaque)
    }

    @Test
    fun thePanelHoldsAToolbarOverTheGroupOfTheReadingButtonsWhoseTargetIsThePanel() {
        val panel = panel()

        assertSame(ActionManager.getInstance().getAction(READING_BUTTON_GROUP_ID), panel.toolbar.actionGroup)
        assertSame(panel, panel.toolbar.targetComponent)
        assertSame(panel, panel.toolbar.component.parent)
        assertFalse(panel.toolbar.component.isOpaque)
    }

    @Test
    fun aButtonOnThePanelShowsTheHoverInThePressedColorAndNotInTheBackground() {
        val hover = ReadingButtonLook().getStateBackground(JButton(), ActionButtonComponent.POPPED)

        assertEquals(JBUI.CurrentTheme.ActionButton.pressedBackground(), hover)
        assertNotEquals(JBUI.CurrentTheme.ActionButton.hoverBackground(), hover)
    }

    @Test
    fun everyButtonOfTheToolbarIsGivenTheReadingButtonLookWhenItIsAdded() {
        val panel = panel()
        val looks = mutableListOf<ActionButtonLook?>()
        val presentation = Presentation()
        val late = object : ActionButton(action, presentation, "EditorTabsToolbar", Dimension(20, 20)) {
            override fun setLook(look: ActionButtonLook) {
                looks += look
                super.setLook(look)
            }
        }

        runInEdtAndWait { panel.toolbar.component.add(late) }

        assertEquals(1, looks.size)
        assertTrue(looks.single() is ReadingButtonLook)
    }

    private fun panel(): ReadingButtonsPanel = runInEdtAndGet {
        (action as CustomComponentAction).createCustomComponent(action.templatePresentation.clone(), "EditorTabsToolbar")
            as ReadingButtonsPanel
    }

    private fun isShownFor(file: VirtualFile): Boolean {
        val event = eventFor(action, file)
        ActionUtil.updateAction(action, event)
        return event.presentation.isEnabledAndVisible
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
