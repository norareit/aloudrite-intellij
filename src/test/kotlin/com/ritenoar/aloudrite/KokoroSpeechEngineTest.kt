package com.ritenoar.aloudrite

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class KokoroSpeechEngineTest {

    @Test
    fun theTestSentenceIsMadeAsAudioOfAPlausibleLength() {
        val operatingSystemName = System.getProperty("os.name")
        assumeTrue(isSpeechSupported(operatingSystemName, System.getProperty("os.arch")))
        val modelFolder = userModelFolder(operatingSystemName, System.getProperty("user.home"), System.getenv("XDG_DATA_HOME"))
        assumeTrue("The model is not in $modelFolder", isModelInPlace(modelFolder))

        val engine = KokoroSpeechEngine(modelFolder)
        try {
            var sampleCount = 0L
            engine.speak("AloudRite can speak. This sentence was made on your own machine.") { samples ->
                sampleCount += samples.size
            }

            val seconds = sampleCount.toDouble() / engine.sampleRate
            assertTrue("The audio lasts $seconds seconds", seconds in 2.0..15.0)
        } finally {
            engine.close()
        }
    }
}
