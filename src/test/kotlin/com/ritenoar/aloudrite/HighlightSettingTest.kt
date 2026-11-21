package com.ritenoar.aloudrite

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.testFramework.ApplicationRule
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.io.StringReader

class HighlightSettingTest {

    @Test
    fun allFourAreOnUntilTheUserChangesThem() {
        assertEquals(
            HighlightChoices(inTextEditor = true, inPreview = true, sentence = true, word = true),
            HighlightSetting().choices,
        )
    }

    @Test
    fun eachChoiceChangedOnItsOwnIsGivenBackOnceItsStateIsLoadedIntoANewSetting() {
        val defaults = HighlightChoices()
        val changes = mapOf(
            "inTextEditor" to defaults.copy(inTextEditor = !defaults.inTextEditor),
            "inPreview" to defaults.copy(inPreview = !defaults.inPreview),
            "sentence" to defaults.copy(sentence = !defaults.sentence),
            "word" to defaults.copy(word = !defaults.word),
        )
        for ((name, changed) in changes) {
            val first = HighlightSetting().also { it.choices = changed }

            val second = HighlightSetting()
            second.loadState(first.state)

            assertEquals(name, changed, second.choices)
        }
    }

    @Test
    fun aStateStoredByTheCheckboxOfTheEarlierUnitLoadsAsTheDefaults() {
        val element: Element = SAXBuilder().build(
            StringReader("""<StoredHighlights><option name="highlights" value="false" /></StoredHighlights>"""),
        ).rootElement
        val stored = XmlSerializer.deserialize(element, HighlightSetting.StoredHighlights::class.java)
        val setting = HighlightSetting().also { it.choices = HighlightChoices(false, false, false, false) }

        setting.loadState(stored)

        assertEquals(HighlightChoices(), setting.choices)
    }

    @Test
    fun theHighlightsCanBeChosenUnlessBothViewsAreCleared() {
        assertTrue(HighlightChoices(inTextEditor = true, inPreview = true).highlightsCanBeChosen)
        assertTrue(HighlightChoices(inTextEditor = true, inPreview = false).highlightsCanBeChosen)
        assertTrue(HighlightChoices(inTextEditor = false, inPreview = true).highlightsCanBeChosen)
        assertFalse(HighlightChoices(inTextEditor = false, inPreview = false).highlightsCanBeChosen)
    }

    @Test
    fun theSettingIsOneApplicationServiceStoredInTheFileOfTheOtherSettings() {
        assertSame(service<HighlightSetting>(), service<HighlightSetting>())
        assertEquals(listOf(Service.Level.APP), HighlightSetting::class.java.getAnnotation(Service::class.java).value.toList())
        assertTrue(PersistentStateComponent::class.java.isAssignableFrom(HighlightSetting::class.java))
        assertEquals("AloudRiteHighlights", HighlightSetting::class.java.getAnnotation(State::class.java).name)
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
