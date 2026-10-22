package com.ritenoar.aloudrite

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class SpeechModelSettingTest {

    private val int8 = SPEECH_MODELS[0]
    private val fp32 = SPEECH_MODELS[1]
    private val changes = mutableListOf<SpeechModel>()

    private fun settingWithDefault(default: SpeechModel) = SpeechModelSetting(default) { changes += it }

    @Test
    fun aNewSettingGivesTheDefaultItWasMadeWith() {
        assertSame(fp32, settingWithDefault(fp32).modelInUse)
        assertSame(int8, settingWithDefault(int8).modelInUse)
    }

    @Test
    fun theStateIsNullWhileNothingWasChosenSoThatAChangedDefaultReachesTheListener() {
        val first = settingWithDefault(int8)
        assertNull(first.state)

        val second = settingWithDefault(fp32)
        second.loadState(SpeechModelSetting.StoredModel())

        assertSame(fp32, second.modelInUse)
    }

    @Test
    fun aChosenModelIsGivenBackOnceTheStateIsLoadedWhateverTheDefaultIs() {
        val first = settingWithDefault(int8)
        first.choose(fp32)

        val second = settingWithDefault(int8).also { it.loadState(first.state!!) }
        val third = settingWithDefault(fp32).also { it.loadState(first.state!!) }

        assertSame(fp32, second.modelInUse)
        assertSame(fp32, third.modelInUse)
    }

    @Test
    fun aChosenModelEqualToTheDefaultIsStoredAndKeptWhenTheDefaultChanges() {
        val first = settingWithDefault(int8)
        first.choose(int8)

        val loaded = settingWithDefault(fp32).also { it.loadState(first.state!!) }

        assertSame(int8, loaded.modelInUse)
    }

    @Test
    fun aStoredNameOfNoEntryGivesTheDefault() {
        val loaded = settingWithDefault(fp32)

        loaded.loadState(SpeechModelSetting.StoredModel().also { it.packageName = "no-such-package" })

        assertSame(fp32, loaded.modelInUse)
        assertNull(loaded.state)
    }

    @Test
    fun aChoiceTellsTheCollaboratorOnceForAChangeAndNotForTheModelAlreadyInUse() {
        val setting = settingWithDefault(int8)

        setting.choose(int8)
        setting.choose(fp32)
        setting.choose(fp32)

        assertEquals(listOf(fp32), changes)
    }

    @Test
    fun theSettingIsOneApplicationServiceStoredInTheFileOfTheOthers() {
        assertSame(service<SpeechModelSetting>(), service<SpeechModelSetting>())
        assertEquals(listOf(Service.Level.APP), SpeechModelSetting::class.java.getAnnotation(Service::class.java).value.toList())
        assertTrue(PersistentStateComponent::class.java.isAssignableFrom(SpeechModelSetting::class.java))
        val state = SpeechModelSetting::class.java.getAnnotation(State::class.java)
        assertEquals("AloudRiteSpeechModel", state.name)
        assertEquals("aloudrite.xml", state.storages.single().value)
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
