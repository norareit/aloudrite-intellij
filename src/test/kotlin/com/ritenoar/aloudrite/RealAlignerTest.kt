package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

// Runs the real engine with the fp32 model and the real forced aligner with its model, without playing anything, and skips
// where either model is not in its folder. A sentence of the trial of unit 0028 is made at two speeds and aligned with its
// words, which the aligner must place one interval each, in order, within the audio
class RealAlignerTest {

    @Test
    fun theSentencesOfTheTrialAreAlignedOneIntervalToAWordInOrderWithinTheAudio() {
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val speechModel = SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }
        val speechFolder = speechModel.folderIn(userModelsFolder())
        assumeTrue("The model ${speechModel.packageName} is not in $speechFolder", speechModel.isInPlace(speechFolder))
        val alignerFolder = ALIGNER_MODEL.folderIn(userModelsFolder())
        assumeTrue("The model ${ALIGNER_MODEL.packageName} is not in $alignerFolder", ALIGNER_MODEL.isInPlace(alignerFolder))
        val voice = checkNotNull(voiceNamed("af_heart"))

        KokoroSpeechEngine(speechFolder, speechModel.modelFileName, engineThreadCount(Runtime.getRuntime().availableProcessors()), debugOutput = false).use { engine ->
            ForcedWordAligner(alignerFolder).use { aligner ->
                for (speed in listOf("1.0", "1.5").map { checkNotNull(parsedSpeed(it)) }) {
                    for (sentence in TRIAL_SENTENCES) {
                        assertAligned(sentence, speed, voice, engine, aligner)
                    }
                }
            }
        }
    }

    private fun assertAligned(sentence: String, speed: ReadingSpeed, voice: Voice, engine: KokoroSpeechEngine, aligner: WordAligner) {
        // A long sentence is cut into several pieces, each of which is aligned on its own as at play time
        ReadingSegments(placedWhatToRead(sentence).placedParts).segments.filter { it.words.isNotEmpty() }.forEach { piece ->
            assertPieceAligned(piece, sentence, speed, voice, engine, aligner)
        }
    }

    private fun assertPieceAligned(segment: ReadingSegment, sentence: String, speed: ReadingSpeed, voice: Voice, engine: KokoroSpeechEngine, aligner: WordAligner) {
        val engineText = (segment.spoken as Words).text
        val rows = mutableListOf<FloatArray>()
        engine.speak(engineText, speed.forTheEngine, voice) { samples ->
            rows += samples.copyOf()
            true
        }
        val samples = rows.reduce { all, row -> all + row }
        val parts = segment.words.map { partsOf(it) }
        val where = "\"$sentence\" at ${speed.display}"

        val intervals = aligner.align(samples, engine.sampleRate, parts.flatten())

        val spoken = intervals.filter { it.text.isNotBlank() }
        assertEquals("Intervals with text for $where", parts.sumOf { it.size }, spoken.size)
        assertEquals("Texts of the intervals for $where", parts.flatten(), spoken.map { it.text.lowercase() })
        val seconds = samples.size.toDouble() / engine.sampleRate
        for (interval in spoken) {
            assertTrue("$interval starts at or before its end for $where", interval.startSeconds <= interval.endSeconds)
            assertTrue("$interval is within the $seconds s of audio for $where", interval.startSeconds >= 0.0 && interval.endSeconds <= seconds + 0.05)
        }
        assertNotNull("An alignment for $where", wordStartsOf(parts, intervals, engine.sampleRate))
    }
}
