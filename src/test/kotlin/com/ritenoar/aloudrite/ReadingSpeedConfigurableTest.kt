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
import javax.swing.JLabel
import javax.swing.JList

class ReadingSpeedConfigurableTest {

    private val changes = mutableListOf<Pair<ReadingSpeed, Boolean>>()
    private val setting = ReadingSpeedSetting { speed, afterAWait -> changes += speed to afterAWait }
    private val page = runInEdtAndGet { ReadingSpeedConfigurable { setting }.also { it.createComponent() } }

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
    fun thePageShowsTheLabelAndTheSixteenSpeeds() {
        val shown = runInEdtAndGet {
            val list = JList<ReadingSpeed>()
            (0 until page.speedChoice.itemCount).map { index ->
                val item = page.speedChoice.getItemAt(index)
                val rendered = page.speedChoice.renderer.getListCellRendererComponent(list, item, index, false, false)
                (rendered as JLabel).text
            }
        }

        assertEquals("Reading speed:", page.speedLabel.text)
        assertEquals(ReadingSpeed.ALL.map { it.display }, shown)
        assertEquals("0.5×", shown.first())
        assertEquals("2.0×", shown.last())
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
