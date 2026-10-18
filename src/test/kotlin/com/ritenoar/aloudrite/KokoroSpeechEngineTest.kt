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

        engine.speak("AloudRite reads this sentence. It was made on your own machine.") { samples ->
            sampleCount += samples.size
            true
        }

        val seconds = sampleCount.toDouble() / engine.sampleRate
        assertTrue("The audio lasts $seconds seconds", seconds in 2.0..15.0)
    }

    @Test
    fun aCallbackThatAnswersStopGetsOneCall() {
        var calls = 0

        engine().speak("This is the first sentence. This is the second one. And this is the third.") {
            calls++
            false
        }

        assertEquals(1, calls)
    }

    // One call with more than about 1,500 characters ends the process, so these would end the test run where a
    // piece were too long. A piece that is the same as another is made once: the engine makes about 5.7 seconds of
    // babble for every character of four bytes, so the piece of 100 emoji is about 8 minutes of audio that take
    // about 3 minutes to make
    @Test
    fun everyPieceOfLongTextsWithoutSentenceEndsIsMadeWithoutACrash() {
        val texts = listOf(
            "word ".repeat(400),
            "x".repeat(2_000),
            "1234567890".repeat(200),
            "\uD83D\uDE00".repeat(600),
        )
        val engine = engine()

        for (text in texts) {
            for (piece in piecesOf(text).filter { it.isNotBlank() }.distinct()) {
                engine.speak(piece) { true }
            }
        }
    }

    private fun engine(): SpeechEngine = sharedEngine ?: KokoroSpeechEngine(MODEL_FOLDER).also { sharedEngine = it }

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
