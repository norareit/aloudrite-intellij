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

class ReadingSpeedSettingTest {

    private val changes = mutableListOf<Pair<ReadingSpeed, Boolean>>()

    private fun setting() = ReadingSpeedSetting { speed, afterAWait -> changes += speed to afterAWait }

    @Test
    fun aNewSettingGivesTheNormalSpeed() {
        assertEquals(ReadingSpeed.NORMAL, setting().speed)
    }

    @Test
    fun aSpeedThatWasSetIsGivenBackOnceItsStateIsLoadedIntoANewSetting() {
        val first = setting()
        first.set(parsedSpeed("1.7")!!, afterAWait = false)

        val second = setting()
        second.loadState(first.state)

        assertEquals("1.7×", second.speed.display)
    }

    @Test
    fun aStoredSpeedOutsideTheRangeIsTakenAsTheNormalSpeed() {
        for (tenths in listOf(3, 25)) {
            val loaded = setting()
            loaded.loadState(ReadingSpeedSetting.StoredSpeed().also { it.tenths = tenths })

            assertEquals(ReadingSpeed.NORMAL, loaded.speed)
        }
    }

    @Test
    fun aSetHandsTheSpeedAndWhetherToWaitToTheCollaborator() {
        val changed = setting()

        changed.set(ReadingSpeed.FASTEST, afterAWait = true)
        changed.set(ReadingSpeed.SLOWEST, afterAWait = false)

        assertEquals(listOf(ReadingSpeed.FASTEST to true, ReadingSpeed.SLOWEST to false), changes)
        assertEquals(ReadingSpeed.SLOWEST, changed.speed)
    }

    @Test
    fun aSetToTheCurrentSpeedHandsNothingOn() {
        val unchanged = setting()

        unchanged.set(ReadingSpeed.NORMAL, afterAWait = false)

        assertTrue(changes.isEmpty())
    }

    @Test
    fun theSettingIsOneApplicationServiceStoredInItsOwnFile() {
        assertSame(service<ReadingSpeedSetting>(), service<ReadingSpeedSetting>())
        assertEquals(listOf(Service.Level.APP), ReadingSpeedSetting::class.java.getAnnotation(Service::class.java).value.toList())
        assertTrue(PersistentStateComponent::class.java.isAssignableFrom(ReadingSpeedSetting::class.java))
        val state = ReadingSpeedSetting::class.java.getAnnotation(State::class.java)
        assertEquals("AloudRiteReadingSpeed", state.name)
        assertEquals(listOf("aloudrite.xml"), state.storages.map { it.value })
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
