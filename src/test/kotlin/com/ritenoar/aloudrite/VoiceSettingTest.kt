package com.ritenoar.aloudrite

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class VoiceSettingTest {

    private val changes = mutableListOf<Voice>()
    private val emma = voiceNamed("bf_emma")!!

    private fun setting() = VoiceSetting { changes += it }

    @Test
    fun aNewSettingHoldsHeart() {
        assertSame(DEFAULT_VOICE, setting().voice)
    }

    @Test
    fun chooseHoldsTheVoiceAndTellsTheCollaboratorOnce() {
        val chosen = setting()

        chosen.choose(emma)

        assertSame(emma, chosen.voice)
        assertEquals(listOf(emma), changes)
    }

    @Test
    fun aChoiceOfTheVoiceHeldIsNoChange() {
        val unchanged = setting()

        unchanged.choose(DEFAULT_VOICE)

        assertTrue(changes.isEmpty())
    }

    @Test
    fun theVoiceChosenIsGivenBackOnceTheStateIsLoadedIntoANewSetting() {
        val first = setting()
        first.choose(emma)

        val second = setting().also { it.loadState(first.state) }

        assertSame(emma, second.voice)
    }

    @Test
    fun aStoredNameOfNoVoiceAnEmptyNameAndNoNameAreLoadedAsHeart() {
        for (name in listOf("af_alloy", "", null)) {
            val loaded = setting()
            loaded.choose(emma)
            loaded.loadState(VoiceSetting.StoredVoice().also { it.kokoroName = name })

            assertSame(name, DEFAULT_VOICE, loaded.voice)
        }
    }

    @Test
    fun theSettingIsOneApplicationServiceStoredInItsOwnFile() {
        assertSame(service<VoiceSetting>(), service<VoiceSetting>())
        assertEquals(listOf(Service.Level.APP), VoiceSetting::class.java.getAnnotation(Service::class.java).value.toList())
        assertTrue(PersistentStateComponent::class.java.isAssignableFrom(VoiceSetting::class.java))
        val state = VoiceSetting::class.java.getAnnotation(State::class.java)
        assertEquals("AloudRiteVoice", state.name)
        assertEquals(listOf("aloudrite.xml"), state.storages.map { it.value })
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
