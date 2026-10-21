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
        assumeTrue("The model is not in $MODEL_FOLDER", isModelInPlace(MODEL_FOLDER))
    }

    @Test
    fun aSentenceIsMadeAsAudioOfAPlausibleLength() {
        val engine = engine()
        var sampleCount = 0L

        engine.speak("AloudRite reads this sentence. It was made on your own machine.", ReadingSpeed.NORMAL.forTheEngine) { samples ->
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
        engine().speak(text, speed.forTheEngine) { samples ->
            sampleCount += samples.size
            true
        }
        return sampleCount
    }

    @Test
    fun aCallbackThatAnswersStopGetsOneCall() {
        var calls = 0

        engine().speak("This is the first sentence. This is the second one. And this is the third.", ReadingSpeed.NORMAL.forTheEngine) {
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
                engine.speak(piece, ReadingSpeed.NORMAL.forTheEngine) { true }
            }
        }
    }

    @Test
    fun anEmojiInASentenceAddsNoBabble() {
        val engine = engine()
        var sampleCount = 0L
        val text = withCharactersAboveFfffAsSpaces("Before the sign, ${String(Character.toChars(0x1F600))} and after the sign.")

        engine.speak(text, ReadingSpeed.NORMAL.forTheEngine) { samples ->
            sampleCount += samples.size
            true
        }

        // The sentence alone gave 1.9 seconds, and with the emoji handed over as it is 7.4 to 7.6 seconds
        val seconds = sampleCount.toDouble() / engine.sampleRate
        assertTrue("The audio lasts $seconds seconds", seconds < 4.0)
    }

    private fun engine(): SpeechEngine = sharedEngine ?: KokoroSpeechEngine(MODEL_FOLDER, engineThreadCount(Runtime.getRuntime().availableProcessors()))
        .also { sharedEngine = it }

    companion object {
        private val OPERATING_SYSTEM_NAME: String = System.getProperty("os.name")
        private val MODEL_FOLDER = userModelFolder(
            OPERATING_SYSTEM_NAME, System.getProperty("user.home"), System.getenv("XDG_DATA_HOME"))

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
