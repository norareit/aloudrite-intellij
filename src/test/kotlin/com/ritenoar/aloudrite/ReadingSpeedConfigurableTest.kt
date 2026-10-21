package com.ritenoar.aloudrite

import com.intellij.openapi.options.Configurable
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.awt.Container
import javax.swing.JLabel
import javax.swing.JList

class ReadingSpeedConfigurableTest {

    private val changes = mutableListOf<Pair<ReadingSpeed, Boolean>>()
    private val setting = ReadingSpeedSetting { speed, afterAWait -> changes += speed to afterAWait }
    private val page = ReadingSpeedConfigurable { setting }
    private val component = runInEdtAndGet { page.createComponent() }

    @Test
    fun thePluginRegistersAnApplicationPageNamedAloudRiteUnderTools() {
        val registered = Configurable.APPLICATION_CONFIGURABLE.extensionList.single {
            it.pluginDescriptor.pluginId.idString == "com.ritenoar.aloudrite"
        }

        assertEquals("AloudRite", registered.displayName)
        assertEquals("tools", registered.parentId)
        assertEquals("com.ritenoar.aloudrite.ReadingSpeedConfigurable", registered.instanceClass)
    }

    @Test
    fun thePageShowsTheLabelAndTheNineteenSteps() {
        val shown = runInEdtAndGet {
            val list = JList<ReadingSpeed>()
            (0 until page.speedChoice.itemCount).map { index ->
                val item = page.speedChoice.getItemAt(index)
                val rendered = page.speedChoice.renderer.getListCellRendererComponent(list, item, index, false, false)
                (rendered as JLabel).text
            }
        }

        assertEquals("Reading speed:", page.speedLabel.text)
        assertEquals(ReadingSpeed.STEPS.map { it.display }, shown)
        assertEquals("0.75×", shown.first())
        assertEquals("2.0×", shown.last())
    }

    @Test
    fun theChoiceKeepsItsOwnHeightOnATallPage() {
        val (height, preferredHeight) = runInEdtAndGet {
            component.setSize(600, 500)
            layOutAll(component)
            page.speedChoice.height to page.speedChoice.preferredSize.height
        }

        assertEquals(preferredHeight, height)
    }

    private fun layOutAll(container: Container) {
        container.doLayout()
        for (child in container.components) if (child is Container) layOutAll(child)
    }

    @Test
    fun resetSelectsTheSpeedOfTheSetting() {
        setting.set(speed("1.3"), afterAWait = false)

        runInEdtAndWait { page.reset() }

        assertEquals(speed("1.3"), runInEdtAndGet { page.speedChoice.selectedItem })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun anotherChoiceIsModifiedAndApplySetsItWithNoWait() {
        runInEdtAndWait { page.reset() }

        runInEdtAndWait { page.speedChoice.selectedItem = speed("1.6") }
        assertTrue(runInEdtAndGet { page.isModified })
        runInEdtAndWait { page.apply() }

        assertEquals(speed("1.6"), setting.speed)
        assertEquals(listOf(speed("1.6") to false), changes)
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aTypedSpeedBetweenTwoStepsIsShownAmongThemInItsPlace() {
        setting.set(speed("1.37"), afterAWait = false)

        runInEdtAndWait { page.reset() }

        assertEquals(speed("1.37"), runInEdtAndGet { page.speedChoice.selectedItem })
        assertEquals(ReadingSpeed.stepsWith(speed("1.37")), runInEdtAndGet {
            (0 until page.speedChoice.itemCount).map { page.speedChoice.getItemAt(it) }
        })
        assertFalse(runInEdtAndGet { page.isModified })
    }

    @Test
    fun aSpeedSetInThePanelShowsOnThePageAfterAReset() {
        runInEdtAndWait { page.reset() }

        setting.set(ReadingSpeed.FASTEST, afterAWait = true)
        runInEdtAndWait { page.reset() }

        assertEquals(ReadingSpeed.FASTEST, runInEdtAndGet { page.speedChoice.selectedItem })
    }

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
