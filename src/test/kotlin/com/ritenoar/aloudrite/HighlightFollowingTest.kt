package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightFollowingTest {

    private fun row(phonemes: String): IntArray = phonemes.codePoints().toArray()

    private fun wordsOf(vararg phonemes: String): List<List<String>> =
        phonemeWordsOf(phonemes.map { row(it) }).map { word ->
            listOf(word.row.toString(), String(row(phonemes[word.row]), word.firstToken, word.endToken - word.firstToken))
        }

    private fun heardWord(start: Int, end: Int, weight: Int) = HeardWord(SourceRange(start, end), SourceRange(0, 100), weight)

    // Phoneme words

    @Test
    fun aPhonemeWordIsARunOfTokensBetweenSpaces() {
        assertEquals(listOf(listOf("0", "abc"), listOf("0", "de")), wordsOf("abc de"))
    }

    @Test
    fun punctuationTokensAreInNoWordAlsoWhereTheyTouchOne() {
        assertEquals(listOf(listOf("0", "ab"), listOf("0", "cd"), listOf("0", "ef")), wordsOf("ab,cd. “ef—"))
    }

    @Test
    fun theStressAndLengthMarksBelongToTheWord() {
        assertEquals(listOf(listOf("0", "hˈɜːt")), wordsOf("hˈɜːt"))
    }

    @Test
    fun thePhonemeWordsOfSeveralRowsFollowOneAnother() {
        assertEquals(listOf(listOf("0", "ab"), listOf("1", "cd"), listOf("1", "ef")), wordsOf("ab", "cd ef"))
    }

    @Test
    fun aRowOfPunctuationAndSpacesHasNoWord() {
        assertEquals(emptyList<List<String>>(), wordsOf(". ,"))
    }

    // Pairing

    private fun pairing(phonemes: String, vararg words: HeardWord) =
        pairedWords(phonemeWordsOf(listOf(row(phonemes))), words.toList())

    @Test
    fun asManyPhonemeWordsAsWordsArePairedInOrder() {
        val first = heardWord(0, 3, 3)
        val second = heardWord(4, 7, 3)

        val pairing = pairing("ab cd", first, second)

        assertEquals(listOf(first, second), (pairing as Pairing.Paired).wordsInOrder)
    }

    @Test
    fun wordsThatWeighNothingAreLeftOutOfThePairing() {
        val first = heardWord(0, 3, 3)
        val dash = heardWord(4, 5, 0)
        val second = heardWord(6, 9, 3)

        val pairing = pairing("ab cd", first, dash, second)

        assertEquals(listOf(first, second), (pairing as Pairing.Paired).wordsInOrder)
    }

    @Test
    fun anExtraPhonemeWordGivesTheReasonWithBothNumbers() {
        val pairing = pairing("ab cd ef", heardWord(0, 3, 3), heardWord(4, 7, 3))

        assertEquals("3 phoneme words for 2 words", (pairing as Pairing.NotPaired).reason)
    }

    @Test
    fun aMissingPhonemeWordGivesTheReasonWithBothNumbers() {
        val pairing = pairing("ab", heardWord(0, 3, 3), heardWord(4, 7, 3))

        assertEquals("1 phoneme words for 2 words", (pairing as Pairing.NotPaired).reason)
    }

    // Spans

    @Test
    fun aPaddingTokenAndASpaceHoldNoWord() {
        // Tokens a, space, b with the padding token at both ends: frames 1 (padding), 2 (a), 3 (space), 4 (b), 5 (padding)
        val spans = spansOf(listOf(row("a b")), listOf(intArrayOf(1, 2, 3, 4, 5)))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan(6 * FRAME_SAMPLES, 10 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aWordRunsFromTheFirstSampleOfItsFirstTokenToTheEndOfItsLast() {
        val spans = spansOf(listOf(row("ab")), listOf(intArrayOf(0, 2, 3, 0)))

        assertEquals(listOf(WordSpan(0, 5 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun theWordsOfTheSecondRowBeginAfterTheWholeOfTheFirstItsClosingPaddingIncluded() {
        val spans = spansOf(listOf(row("a"), row("b")), listOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6)))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan((6 + 4) * FRAME_SAMPLES, 15 * FRAME_SAMPLES)), spans)
    }

    // Lengths

    @Test
    fun theLengthByTheDurationsIsTheFramesOfEveryRowInSamples() {
        assertEquals(10L * FRAME_SAMPLES, lengthByTheDurations(listOf(intArrayOf(1, 2), intArrayOf(3, 4))))
    }

    @Test
    fun lengthsTwoPercentApartAgree() {
        assertNull(lengthDisagreement(durationSamples = 98_000, engineSamples = 100_000))
        assertNull(lengthDisagreement(durationSamples = 102_000, engineSamples = 100_000))
    }

    @Test
    fun lengthsJustOverTwoPercentApartDisagreeWithBothLengthsAndTheDifference() {
        val reason = lengthDisagreement(durationSamples = 102_001, engineSamples = 100_000, sampleRate = 24_000)

        assertEquals("audio 4.17 s by the engine, 4.25 s by the durations, 2.0% apart", reason)
    }

    @Test
    fun twoLengthsOfNothingAgree() {
        assertNull(lengthDisagreement(durationSamples = 0, engineSamples = 0))
    }

    // Moments

    @Test
    fun aLengthOf480SamplesGivesTheMoments0And240() {
        assertEquals(listOf(0L, 240L), momentsOf(480))
    }

    @Test
    fun aLengthOf481SamplesGivesTheMoments0240And480() {
        assertEquals(listOf(0L, 240L, 480L), momentsOf(481))
    }

    @Test
    fun aMomentOnTheFirstSampleOfAWordHoldsItAndOneAfterItsLastDoesNot() {
        val spans = listOf(WordSpan(100, 200), WordSpan(300, 400))

        assertNull(wordHeardAt(spans, 99))
        assertEquals(0, wordHeardAt(spans, 100))
        assertEquals(0, wordHeardAt(spans, 199))
        assertNull(wordHeardAt(spans, 200))
        assertEquals(1, wordHeardAt(spans, 300))
    }

    // Following a piece

    private val oneToOne = "aaaa bbbb cccc dddd."
    private val speed = checkNotNull(parsedSpeed("1.0"))
    private val sampleRate = 24_000

    private class Followed(val counted: Int, val matching: Int)

    // The audio of 1 second, which the moments divide in 100, with the spans in the samples of it
    private fun follow(text: String, spans: List<WordSpan>, samples: Int = 24_000): Followed {
        val segments = ReadingSegments(whatToRead(text, PlaceInTheText.NoText).placedParts)
        val piece = segments.segments.indexOfFirst { it.words.isNotEmpty() }
        val reading = ReadingTime(segments.spoken, sampleRate, speed)
        reading.audioMade(piece, samples)
        reading.segmentMade(piece)
        val followed = followPiece(piece, segments.segments[piece].words, spans, samples.toLong(), reading, HeardWordEstimate(segments.segments))
        return Followed(followed.counted, followed.matching)
    }

    private fun spansOfFourEqualWords(): List<WordSpan> = (0 until 4).map { WordSpan(it * 6_000L, (it + 1) * 6_000L) }

    @Test
    fun spansThatAgreeWithTheLettersGiveAShareOfOne() {
        val followed = follow(oneToOne, spansOfFourEqualWords())

        assertEquals(100, followed.counted)
        assertEquals(100, followed.matching)
    }

    @Test
    fun momentsInPausesAreNotCounted() {
        // The four words are heard in the first and the third quarter of the audio only, so 25 + 25 moments count
        val spans = listOf(WordSpan(0, 3_000), WordSpan(3_000, 6_000), WordSpan(12_000, 15_000), WordSpan(15_000, 18_000))

        assertEquals(50, follow(oneToOne, spans).counted)
    }

    @Test
    fun aWordThatTheEstimateMovesOnFromTooEarlyCountsAgainstTheShare() {
        // Four words of 4 letters, so the estimate shows the first word until 0.25 s, the second until 0.5 s, the third
        // until 0.75 s and then the fourth. The model says the first word lasts through the first half and the others
        // 1/6 s each, so the word heard is the second from 0.5 s to 0.66 s, the third to 0.83 s and then the fourth
        val spans = listOf(WordSpan(0, 12_000), WordSpan(12_000, 16_000), WordSpan(16_000, 20_000), WordSpan(20_000, 24_000))

        val followed = follow(oneToOne, spans)

        // The first word matches at the 25 moments before 0.25 s, the third at 0.67 s through 0.74 s, 8 moments, and the
        // fourth at 0.84 s through 0.99 s, 16 moments. The second never does
        assertEquals(100, followed.counted)
        assertEquals(25 + 8 + 16, followed.matching)
    }

    // The share of a speed

    private fun followedPiece(number: Int, counted: Int, matching: Int) = FollowedPiece(number, "text", counted, matching)

    @Test
    fun theShareOfASpeedWeighsEachPieceByItsCountedMoments() {
        val pieces = listOf(followedPiece(0, counted = 10, matching = 10), followedPiece(1, counted = 90, matching = 0))

        assertEquals(0.1, shareOf(pieces)!!, 1e-9)
    }

    @Test
    fun aPieceWithNoCountedMomentsShowsADashAndAddsNothingToTheShare() {
        val empty = followedPiece(0, counted = 0, matching = 0)

        assertEquals("-", empty.shareText())
        assertEquals(0.5, shareOf(listOf(empty, followedPiece(1, 10, 5)))!!, 1e-9)
        assertNull(shareOf(listOf(empty)))
    }

    @Test
    fun theTableNamesEachLeftOutPieceWithItsReasonAndGivesEachSpeedsFigures() {
        val table = tableOf(listOf(SpeedResult("1.0×", listOf(
            followedPiece(0, counted = 100, matching = 75),
            LeftOutPiece(1, "A very long text of a piece, which is cut after fifty characters at the most.", "3 phoneme words for 2 words"),
        ))))

        assertTrue(table, table.contains("speed 1.0×: share 75.0%, 1.0 s of words counted, 1 pieces counted, 1 left out"))
        assertTrue(table, table.contains("left out: 3 phoneme words for 2 words"))
        assertTrue(table, table.contains("75.0%"))
        assertTrue(table, table.contains("A very long text of a piece, which is cut after fi"))
        assertTrue(table, !table.contains("fifty characters at the most"))
    }
}
