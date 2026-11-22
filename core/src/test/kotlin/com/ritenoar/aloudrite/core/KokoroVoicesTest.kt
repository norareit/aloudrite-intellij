package com.ritenoar.aloudrite.core

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

// Runs the real engine with each model and makes a sentence in each voice, without playing anything, only where the
// platform has speech and the model is in its folder. "AloudRite" is in no dictionary, so eSpeak NG spells it
class KokoroVoicesTest {

    @Test
    fun everyVoiceMakesAudioWithTheInt8Model() = everyVoiceMakesAudioWith(SPEECH_MODELS[0])

    @Test
    fun everyVoiceMakesAudioWithTheFp32Model() = everyVoiceMakesAudioWith(SPEECH_MODELS[1])

    private fun everyVoiceMakesAudioWith(model: SpeechModel) {
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val modelFolder = model.folderIn(userModelsFolder())
        assumeTrue("The model is not in $modelFolder", model.isInPlace(modelFolder))

        KokoroSpeechEngine(
            modelFolder, model.modelFileName, engineThreadCount(Runtime.getRuntime().availableProcessors()), debugOutput = false,
        ).use { engine ->
            for (voice in VOICES) {
                val samples = mutableListOf<Float>()
                engine.speak("AloudRite reads this schedule aloud.", ReadingSpeed.NORMAL.forTheEngine, voice) { made ->
                    samples += made.toList()
                    true
                }
                val where = "in ${voice.kokoroName} with ${model.packageName}"
                assertTrue("No sample was made $where", samples.isNotEmpty())
                assertTrue("A sample is not a number $where", samples.none { it.isNaN() || it.isInfinite() })
            }
        }
    }
}
