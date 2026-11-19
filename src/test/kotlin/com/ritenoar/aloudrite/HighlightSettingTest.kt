package com.ritenoar.aloudrite

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class HighlightSettingTest {

    @Test
    fun theHighlightsAreOnUntilTheyAreCleared() {
        val setting = HighlightSetting()

        assertTrue(setting.highlights)

        setting.highlights = false
        assertFalse(setting.highlights)
    }

    @Test
    fun aClearedBoxIsGivenBackOnceItsStateIsLoadedIntoANewSetting() {
        val first = HighlightSetting().also { it.highlights = false }

        val second = HighlightSetting()
        second.loadState(first.state)

        assertFalse(second.highlights)
    }

    @Test
    fun aCheckedBoxIsGivenBackOnceItsStateIsLoaded() {
        val second = HighlightSetting().also { it.highlights = false }

        second.loadState(HighlightSetting().state)

        assertTrue(second.highlights)
    }

    @Test
    fun aStateStoredBeforeTheSettingExistedLeavesTheHighlightsOn() {
        val setting = HighlightSetting()

        setting.loadState(HighlightSetting.StoredHighlights())

        assertTrue(setting.highlights)
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
