package com.ritenoar.aloudrite.core

import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class KokoroTtsConfigTest {

    private val modelFolder = Paths.get("/model")

    @Test
    fun theConfigurationHasTheDebugOutputOffWhenItIsBuiltWithItOff() {
        assertFalse(debugOutputOf(kokoroTtsConfig(modelFolder, "model.onnx", 2, debugOutput = false).model))
    }

    @Test
    fun theConfigurationHasTheDebugOutputOnWhenItIsBuiltWithItOn() {
        assertTrue(debugOutputOf(kokoroTtsConfig(modelFolder, "model.onnx", 2, debugOutput = true).model))
    }

    @Test
    fun theConfigurationHoldsTheModelFileAndTheThreadsItWasGiven() {
        val kokoro = kokoroTtsConfig(modelFolder, "model.int8.onnx", 3, debugOutput = false).model.kokoro

        assertEquals("/model/model.int8.onnx", kokoro.model)
        assertEquals("/model/voices.bin", kokoro.voices)
    }

    @Test
    fun theGenerationConfigurationHoldsTheSpeakerNumberTheSpeedAndTheRowsOfEachVoice() {
        val rows = listOf(intArrayOf(1, 2, 3), intArrayOf(4))

        for (voice in VOICES) {
            val config = kokoroGenerationConfig(voice, 1.25f, rows)

            assertEquals(voice.kokoroName, voice.speakerNumber, config.sid)
            assertEquals(voice.kokoroName, 1.25f, config.speed, 0f)
            assertEquals(voice.kokoroName, rows.map { it.toList() }, config.phonemeCodepoints.map { it.toList() })
        }
    }

    @Test
    fun onlyTheValueTrueTurnsTheDebugOutputOn() {
        assertTrue(isEngineDebugOutputOn("true"))
        listOf(null, "", "false", "yes", "TRUE").forEach { value ->
            assertFalse("The value $value turned the debug output on", isEngineDebugOutputOn(value))
        }
    }

    // The binding has a setter for debug and no getter, so the field is read
    private fun debugOutputOf(modelConfig: OfflineTtsModelConfig): Boolean =
        OfflineTtsModelConfig::class.java.getDeclaredField("debug").apply { isAccessible = true }.getBoolean(modelConfig)
}
