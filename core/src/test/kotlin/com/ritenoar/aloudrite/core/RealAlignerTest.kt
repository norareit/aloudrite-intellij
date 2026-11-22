package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files

// Runs the real engine with the fp32 model and the real forced aligner with its model, without playing anything, and skips
// where either model is not in its folder. A sentence of the trial of unit 0028 is made at two speeds and aligned with its
// words, which the aligner must place one interval each, in order, within the audio, an end allowed to lie past it by less than one step
// of the aligner's times. Unit 0035 adds a sentence with an
// emoji, which the voice reads by its name and the aligner is given as the word it has no line for
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
                    for (sentence in TRIAL_SENTENCES + SENTENCE_WITH_AN_EMOJI) {
                        assertAligned(sentence, speed, voice, engine, aligner)
                    }
                }
            }
        }
    }

    // The aligner gives no interval for a character above U+FFFF, so the plugin gives it the word it names as WORD_FOR_AN_EMOJI.
    // It places the speech of the name there only while its lexicon has no line for that word
    @Test
    fun theLexiconOfTheAlignerHoldsNoLineForTheWordGivenInPlaceOfAnEmoji() {
        val alignerFolder = ALIGNER_MODEL.folderIn(userModelsFolder())
        assumeTrue("The model ${ALIGNER_MODEL.packageName} is not in $alignerFolder", ALIGNER_MODEL.isInPlace(alignerFolder))

        val lines = Files.readAllLines(alignerFolder.resolve("lexicon.txt"))

        assertTrue("lexicon.txt holds ${lines.size} lines", lines.size > 1000)
        assertEquals(emptyList<String>(), lines.filter { it.substringBefore('\t').substringBefore(' ') == WORD_FOR_AN_EMOJI })
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
            assertTrue("$interval starts within the audio for $where", interval.startSeconds >= 0.0)
            assertTrue("$interval ends within the $seconds s of audio and less than $ALIGNER_STEP_SECONDS s past it for $where", isEndWithinTheAudio(interval.endSeconds, seconds))
        }
        assertNotNull("An alignment for $where", wordStartsOf(parts, intervals, engine.sampleRate, samples.size))
    }

    private companion object {
        val SENTENCE_WITH_AN_EMOJI = "Before the sign, ${String(Character.toChars(0x1F600))} and after the sign."
    }
}
