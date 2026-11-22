package com.ritenoar.aloudrite.core

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

// Measures, for units 0030 and 0035, how many samples the real engine makes for the sentences of requirements 3 and 6 of
// unit 0030 and of requirement 6 of unit 0035, each as it stands, at the normal speed with the int8 model. Runs only where
// ALOUDRITE_MEASURE_ENGINE_LENGTHS is set and the model is in its folder, as
// ALOUDRITE_MEASURE_ENGINE_LENGTHS=1 ./gradlew :core:cleanTest :core:test --tests com.ritenoar.aloudrite.core.EngineLengthsMeasurement
// and writes its table to build/engine-lengths.txt
class EngineLengthsMeasurement {

    @Test
    fun measureTheLengthOfEverySentenceOfTheUnit() {
        assumeTrue("ALOUDRITE_MEASURE_ENGINE_LENGTHS is not set", System.getenv("ALOUDRITE_MEASURE_ENGINE_LENGTHS") != null)
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val modelFolder = INT8_MODEL.folderIn(userModelsFolder())
        assumeTrue("The model is not in $modelFolder", INT8_MODEL.isInPlace(modelFolder))

        val lengths = KokoroSpeechEngine(
            modelFolder, INT8_MODEL.modelFileName, engineThreadCount(Runtime.getRuntime().availableProcessors()), debugOutput = false,
        ).use { engine ->
            SENTENCES.map { sentence ->
                var sampleCount = 0L
                engine.speak(sentence, ReadingSpeed.NORMAL.forTheEngine, DEFAULT_VOICE) { samples ->
                    sampleCount += samples.size
                    true
                }
                Triple(sentence, sampleCount, sampleCount.toDouble() / engine.sampleRate)
            }
        }

        val table = buildString {
            appendLine("model ${INT8_MODEL.modelFileName}, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
            appendLine("samples  seconds  sentence")
            for ((sentence, sampleCount, seconds) in lengths) appendLine("%7d  %7.3f  %s".format(sampleCount, seconds, sentence))
        }
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/engine-lengths.txt"), table)
        assertTrue(lengths.size == SENTENCES.size)
    }

    private companion object {
        fun withSign(sign: String) = "Before the sign, $sign and after the sign."

        val SENTENCES = listOf(
            "I have 3 cats in 2026.",
            "Version 4.2 is 5% faster & smaller.",
            "Read the README of the API with npm.",
            "It builds for x86_64 and ARM64 with v1.13.8.",
            "AloudRite reads Markdown aloud.",
            "Before the sign, and after the sign.",
            withSign("❤"),
            withSign(String(Character.toChars(0x1F600))),
            withSign(String(Character.toChars(0x20000))),
            withSign(String(Character.toChars(0x10330))),
            withSign(String(Character.toChars(0x1D400))),
        )
    }
}
