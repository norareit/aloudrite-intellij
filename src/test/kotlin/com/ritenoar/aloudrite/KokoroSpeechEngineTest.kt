package com.ritenoar.aloudrite

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

// Runs the real engine, without playing anything, only where the model is in its folder
class KokoroSpeechEngineTest {

    @Before
    fun requireTheModel() {
        assumeTrue(isSpeechSupported(OPERATING_SYSTEM_NAME, System.getProperty("os.arch")))
        assumeTrue("The model is not in $MODEL_FOLDER", INT8_MODEL.isInPlace(MODEL_FOLDER))
    }

    @Test
    fun aSentenceIsMadeAsAudioOfAPlausibleLength() {
        val engine = engine()
        var sampleCount = 0L

        engine.speak("AloudRite reads this sentence. It was made on your own machine.", ReadingSpeed.NORMAL.forTheEngine, DEFAULT_VOICE) { samples ->
            sampleCount += samples.size
            true
        }

        val seconds = sampleCount.toDouble() / engine.sampleRate
        assertTrue("The audio lasts $seconds seconds", seconds in 2.0..15.0)
    }

    @Test
    fun aSentenceMadeFasterOrSlowerTakesLessOrMoreTime() {
        val sentence = "AloudRite reads this sentence at the speed the listener chose."
        val atNormalSpeed = samplesOf(sentence, ReadingSpeed.NORMAL)

        val fastest = samplesOf(sentence, ReadingSpeed.FASTEST).toDouble() / atNormalSpeed
        val slowest = samplesOf(sentence, ReadingSpeed.SLOWEST).toDouble() / atNormalSpeed

        assertTrue("At 2.0 the audio lasts $fastest of its length at 1.0", fastest in 0.40..0.65)
        assertTrue("At 0.75 the audio lasts $slowest of its length at 1.0", slowest in 1.15..1.60)
    }

    private fun samplesOf(text: String, speed: ReadingSpeed): Long {
        var sampleCount = 0L
        engine().speak(text, speed.forTheEngine, DEFAULT_VOICE) { samples ->
            sampleCount += samples.size
            true
        }
        return sampleCount
    }

    @Test
    fun aCallbackThatAnswersStopGetsOneCall() {
        var calls = 0

        engine().speak("This is the first sentence. This is the second one. And this is the third.", ReadingSpeed.NORMAL.forTheEngine, DEFAULT_VOICE) {
            calls++
            false
        }

        assertEquals(1, calls)
    }

    // One call with more than about 1,500 characters ends the process, so these would end the test run where a
    // piece were too long. The arrows are characters of three bytes, so that the cut at 400 bytes is made with
    // characters of more than one byte; characters of four bytes no longer reach the engine. A piece that is the
    // same as another is made once
    @Test
    fun everyPieceOfLongTextsWithoutSentenceEndsIsMadeWithoutACrash() {
        val texts = listOf(
            "word ".repeat(400),
            "x".repeat(2_000),
            "1234567890".repeat(200),
            "\u2192".repeat(700),
        )
        val engine = engine()

        for (text in texts) {
            for (piece in piecesOf(text).filter { it.isNotBlank() }.distinct()) {
                engine.speak(piece, ReadingSpeed.NORMAL.forTheEngine, DEFAULT_VOICE) { true }
            }
        }
    }

    // Every word is spoken, also those that the lexicon lacks: a word that is left out shortens a short sentence by far more
    // than 10%. The lengths are those that sherpa-onnx 1.13.8 gave on this branch before the move to 2.0, which spoke those
    // words through eSpeak NG itself, as measured by EngineLengthsMeasurement at commit adb78f4
    @Test
    fun everyWordOfASentenceIsSpokenWithinTenPercentOfTheLengthUnder1_13_8() {
        for ((sentence, samplesUnder1_13_8) in LENGTHS_UNDER_1_13_8) {
            val samples = sampleCountOf(sentence)

            assertTrue(
                "\"$sentence\" made $samples samples, and $samplesUnder1_13_8 were made under 1.13.8",
                samples in (samplesUnder1_13_8 * 0.9).toLong()..(samplesUnder1_13_8 * 1.1).toLong(),
            )
        }
    }

    // The characters above U+FFFF reach the front end as a space, and leave neither babble nor anything in their place
    @Test
    fun aCharacterAboveFfffAddsNothingToTheLengthOfASentence() {
        val without = sampleCountOf("Before the sign, and after the sign.")

        for (codePoint in listOf(0x1F600, 0x20000, 0x10330, 0x1D400)) {
            val sentence = "Before the sign, ${String(Character.toChars(codePoint))} and after the sign."
            val samples = sampleCountOf(sentence)

            assertTrue(
                "With U+${codePoint.toString(16).uppercase()} the sentence made $samples samples, and $without without it",
                samples in (without * 0.9).toLong()..(without * 1.1).toLong(),
            )
        }
    }

    private fun sampleCountOf(sentence: String): Long {
        var sampleCount = 0L
        engine().speak(withCharactersAboveFfffAsSpaces(sentence), ReadingSpeed.NORMAL.forTheEngine, DEFAULT_VOICE) { samples ->
            sampleCount += samples.size
            true
        }
        return sampleCount
    }

    private fun engine(): SpeechEngine = sharedEngine ?: KokoroSpeechEngine(MODEL_FOLDER, INT8_MODEL.modelFileName, engineThreadCount(Runtime.getRuntime().availableProcessors()), debugOutput = false)
        .also { sharedEngine = it }

    companion object {
        private val LENGTHS_UNDER_1_13_8 = mapOf(
            "I have 3 cats in 2026." to 60_305L,
            "Version 4.2 is 5% faster & smaller." to 80_202L,
            "Read the README of the API with npm." to 65_916L,
            "It builds for x86_64 and ARM64 with v1.13.8." to 138_351L,
            "AloudRite reads Markdown aloud." to 47_194L,
            "Before the sign, and after the sign." to 45_037L,
        )
        private val OPERATING_SYSTEM_NAME: String = System.getProperty("os.name")
        private val MODEL_FOLDER = INT8_MODEL.folderIn(userModelsFolder())

        // Loading the model takes about a second, so the cases share one engine
        private var sharedEngine: SpeechEngine? = null

        @JvmStatic
        @AfterClass
        fun closeTheEngine() {
            sharedEngine?.close()
            sharedEngine = null
        }
    }
}
