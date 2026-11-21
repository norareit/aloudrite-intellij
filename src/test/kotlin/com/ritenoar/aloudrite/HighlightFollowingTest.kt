package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    // Bare phonemes

    @Test
    fun theBarePhonemesLeaveOutTheStressMarksTheSpaceAndPunctuationAndKeepTheLengthMark() {
        assertEquals("hɜːt", barePhonemesOf(row("hˈɜːt, ˌ")))
    }

    @Test
    fun theBarePhonemesOfAPhonemeWordAreThoseOfItsTokens() {
        val word = phonemeWordsOf(listOf(row("ab ˈcd"))).last()

        assertEquals("cd", barePhonemesOf(row("ab ˈcd"), word))
    }

    @Test
    fun theBarePhonemesOfTheRowsOfAWordAreJoined() {
        assertEquals("abcd", barePhonemesOfRows(listOf(row("ab"), row("ˈcd"))))
    }

    // The words' texts

    @Test
    fun theTextsOfTheWordsAreTheRunsOfCharactersThatAreNotWhiteSpace() {
        val found = textsOfWords("one  two\n three", 3) as WordTexts.Found

        assertEquals(listOf("one", "two", "three"), found.texts)
    }

    @Test
    fun aWordThatAPiecesLimitCutsGivesThePartInThePiece() {
        val found = textsOfWords("one tw", 2) as WordTexts.Found

        assertEquals(listOf("one", "tw"), found.texts)
    }

    @Test
    fun runsOfTextThatAreNotAsManyAsTheWordsGiveTheReasonWithBothNumbers() {
        val notFound = textsOfWords("one two three", 2) as WordTexts.NotFound

        assertEquals("3 runs of text for 2 words", notFound.reason)
    }

    // Groups

    private fun spoken(text: String, phonemes: String, weight: Int = text.length) =
        WordToGroup(heardWord(text.hashCode() and 0xffff, (text.hashCode() and 0xffff) + text.length, weight), text, phonemes)

    private fun grouped(phonemeWords: List<String>, vararg words: WordToGroup): Grouping = groupsOf(phonemeWords, words.toList())

    private fun groupsTexts(grouping: Grouping, vararg words: WordToGroup): List<List<String>> =
        (grouping as Grouping.Grouped).groups.map { group -> group.map { place -> words.first { it.word == place }.text } }

    @Test
    fun asManyPhonemeWordsAsWordsGiveAGroupOfOneWordEachInOrder() {
        val first = spoken("one", "wʌn")
        val second = spoken("two", "tuː")

        val grouping = grouped(listOf("wɐn", "tuː"), first, second)

        assertEquals(listOf(listOf("one"), listOf("two")), groupsTexts(grouping, first, second))
    }

    @Test
    fun wordsThatWeighNothingAreInNoGroup() {
        val first = spoken("one", "wʌn")
        val dash = spoken("--", "", weight = 0)
        val second = spoken("two", "tuː")

        val grouping = grouped(listOf("wʌn", "tuː"), first, dash, second)

        assertEquals(listOf(listOf("one"), listOf("two")), groupsTexts(grouping, first, dash, second))
    }

    @Test
    fun onePhonemeWordOfTwoWordsJoinedGivesAGroupOfBoth() {
        val of = spoken("of", "ʌv")
        val the = spoken("the", "ðə")
        val level = spoken("level", "lɛvəl")

        val grouping = grouped(listOf("ʌvðə", "lɛvəl"), of, the, level)

        assertEquals(listOf(listOf("of", "the"), listOf("level")), groupsTexts(grouping, of, the, level))
    }

    @Test
    fun twoJoinsInOnePieceGiveTwoGroups() {
        val words = listOf(spoken("of", "ʌv"), spoken("the", "ðə"), spoken("in", "ɪn"), spoken("a", "ə"))

        val grouping = groupsOf(listOf("ʌvðə", "ɪnə"), words)

        assertEquals(listOf(listOf("of", "the"), listOf("in", "a")), groupsTexts(grouping, *words.toTypedArray()))
    }

    @Test
    fun aWordOfTheBarePhonemesOfTwoPhonemeWordsJoinedGivesTheReasonNamingIt() {
        val number = spoken("1989", "naɪntiːnˈeɪtinaɪn")

        val grouping = grouped(listOf("naɪntiːn", "eɪtinaɪn"), number) as Grouping.NotGrouped

        assertEquals("eSpeak NG reads 1989 as 2 phoneme words", grouping.reason)
    }

    // Token ids

    @Test
    fun theIdOfATokenIsTheNumberAfterTheLastSpaceOfItsLineAndTheSpaceTokenHasItsOwn() {
        val ids = tokenIdsOf(listOf("\$ 0", "; 1", "  16", "ɑ 51"))

        assertEquals(16, ids[' '.code])
        assertEquals(51, ids['ɑ'.code])
    }

    @Test
    fun theIdsOfARowHaveZeroAtBothEnds() {
        val ids = mapOf('a'.code to 43, 'b'.code to 44)

        assertEquals(listOf(0L, 43L, 44L, 0L), idsOfRow(row("ab"), ids).toList())
    }

    // The style

    private val styleRows = 510
    private val styleValues = 256

    private fun voicesOfSpeakers(speakers: Int) =
        FloatArray(speakers * styleRows * styleValues) { it.toFloat() }

    @Test
    fun theStyleOfARowOfNTokensIsRowNOfTheSpeakerHeldWithinTheRows() {
        val voices = voicesOfSpeakers(2)

        assertEquals(1f * styleValues, styleOf(voices, speaker = 0, tokens = 1)[0], 0f)
        assertEquals(3f * styleValues, styleOf(voices, speaker = 0, tokens = 3)[0], 0f)
        assertEquals(509f * styleValues, styleOf(voices, speaker = 0, tokens = 600)[0], 0f)
        assertEquals(styleValues, styleOf(voices, speaker = 0, tokens = 1).size)
    }

    @Test
    fun theStyleOfTheSecondSpeakerBeginsAfterTheRowsOfTheFirst() {
        assertEquals(((styleRows + 1) * styleValues).toFloat(), styleOf(voicesOfSpeakers(2), speaker = 1, tokens = 1)[0], 0f)
    }

    @Test
    fun aVoicesFileThatIsNotAWholeNumberOfSpeakersFailsWithItsSize() {
        val failure = assertThrows(IllegalArgumentException::class.java) { styleOf(FloatArray(1000), speaker = 0, tokens = 1) }

        assertTrue(failure.message, failure.message!!.contains("1000"))
    }

    // Placement

    private fun varied(length: Int): FloatArray = java.util.Random(1).let { random -> FloatArray(length) { random.nextFloat() * 2 - 1 } }

    @Test
    fun anEngineAudioCutFromTheModelsIsPlacedAtItsOffsetWithNoDifference() {
        val model = varied(5_000)

        val placement = placementOf(model.copyOfRange(1_234, 3_234), model, rowNumber = 1) as Placement.Placed

        assertEquals(1_234, placement.offset)
        assertEquals(0.0, placement.difference, 1e-9)
    }

    @Test
    fun anOffsetThatIsNotAMultipleOf16IsFoundExactly() {
        val model = varied(5_000)

        val placement = placementOf(model.copyOfRange(1_237, 3_237), model, rowNumber = 1) as Placement.Placed

        assertEquals(1_237, placement.offset)
    }

    @Test
    fun aSmallNoiseKeepsThePlaceAndGivesADifferenceUnder20Percent() {
        val model = varied(5_000)
        val random = java.util.Random(2)
        val noisy = FloatArray(2_000) { model[777 + it] + (random.nextFloat() - 0.5f) * 0.2f }

        val placement = placementOf(noisy, model, rowNumber = 1) as Placement.Placed

        assertEquals(777, placement.offset)
        assertTrue(placement.difference > 0.0 && placement.difference < 0.2)
    }

    @Test
    fun anUnrelatedEngineAudioDiffersByMoreThan20PercentAndNamesTheRowAndTheShare() {
        val model = varied(5_000)
        val unrelated = FloatArray(2_000) { Math.cos(it * 1.91).toFloat() * 5 }

        val notPlaced = placementOf(unrelated, model, rowNumber = 2) as Placement.NotPlaced

        assertTrue(notPlaced.reason, Regex("row 2: differs by \\d+\\.\\d% at the best place").matches(notPlaced.reason))
    }

    @Test
    fun anEngineAudioLongerThanTheModelsGivesItsReason() {
        val notPlaced = placementOf(FloatArray(100), FloatArray(50), rowNumber = 1) as Placement.NotPlaced

        assertEquals("row 1: the engine's audio is longer than the model's", notPlaced.reason)
    }

    @Test
    fun aModelAudioOfZerosGivesItsReason() {
        val notPlaced = placementOf(FloatArray(100) { 1f }, FloatArray(500), rowNumber = 3) as Placement.NotPlaced

        assertEquals("row 3: the model's audio is silent where it is placed", notPlaced.reason)
    }

    // Spans

    private fun placedRow(durations: IntArray, start: Long = 0, length: Long = Long.MAX_VALUE / 4, offset: Long = 0) =
        PlacedRow(durations, start, length, offset)

    @Test
    fun aPaddingTokenAndASpaceHoldNoWord() {
        // Tokens a, space, b with the padding token at both ends: frames 1 (padding), 2 (a), 3 (space), 4 (b), 5 (padding)
        val spans = spansOf(listOf(row("a b")), listOf(placedRow(intArrayOf(1, 2, 3, 4, 5))))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan(6 * FRAME_SAMPLES, 10 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aRowAtOffsetZeroGivesTheSpansOfItsDurations() {
        val spans = spansOf(listOf(row("ab")), listOf(placedRow(intArrayOf(0, 2, 3, 0))))

        assertEquals(listOf(WordSpan(0, 5 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aRowAtAnOffsetOfTwoFramesGivesEverySpanTwoFramesEarlierAndTheWordInTheFirstTwoFramesIsCutToWhatRemains() {
        // Frames: padding 1, a 3, b 2, padding 1. The word "a" lies in the model's frames 1 to 4, of which 2 are cut
        val spans = spansOf(listOf(row("a b")), listOf(placedRow(intArrayOf(1, 3, 0, 2, 1), offset = 2 * FRAME_SAMPLES)))

        assertEquals(listOf(WordSpan(0, 2 * FRAME_SAMPLES), WordSpan(2 * FRAME_SAMPLES, 4 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aWordWhollyInTheCutSilenceIsAnEmptySpanThatHoldsNoMoment() {
        val spans = spansOf(listOf(row("a b")), listOf(placedRow(intArrayOf(1, 1, 0, 2, 1), offset = 2 * FRAME_SAMPLES)))

        assertEquals(2, spans.size)
        assertEquals(spans[0].start, spans[0].end)
        // The moment on the empty span's one sample is held by the next word, whose span begins there
        assertEquals(1, wordHeardAt(spans, spans[0].start))
    }

    @Test
    fun theSecondRowsSpansBeginAtItsStartInTheEnginesAudioAndNotAtTheEndOfTheFirstRowsModelAudio() {
        val first = placedRow(intArrayOf(1, 2, 3), start = 0, length = 4 * FRAME_SAMPLES)
        val second = placedRow(intArrayOf(4, 5, 6), start = 4 * FRAME_SAMPLES, length = 10 * FRAME_SAMPLES)

        val spans = spansOf(listOf(row("a"), row("b")), listOf(first, second))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan(8 * FRAME_SAMPLES, 13 * FRAME_SAMPLES)), spans)
    }

    // Following a piece

    private val oneToOne = "aaaa bbbb cccc dddd."
    private val speed = checkNotNull(parsedSpeed("1.0"))
    private val sampleRate = 24_000

    private class Followed(val counted: Int, val matching: Int, val grouped: Int)

    // The audio of 1 second, which the moments divide in 100, with the spans in the samples of it. A group of one word each
    // unless `groupSizes` says otherwise
    private fun follow(text: String, spans: List<WordSpan>, groupSizes: List<Int>? = null, samples: Int = 24_000): Followed {
        val segments = ReadingSegments(whatToRead(text, PlaceInTheText.NoText).placedParts)
        val piece = segments.segments.indexOfFirst { it.words.isNotEmpty() }
        val reading = ReadingTime(segments.spoken, sampleRate, speed)
        reading.audioMade(piece, samples)
        reading.segmentMade(piece)
        val words = segments.segments[piece].words
        var next = 0
        val groups = (groupSizes ?: words.map { 1 }).map { size -> words.subList(next, next + size).also { next += size } }
        val followed = followPiece(piece, groups, spans, samples.toLong(), reading, HeardWordEstimate(segments.segments))
        return Followed(followed.counted, followed.matching, followed.grouped)
    }

    private fun spansOfFourEqualWords(): List<WordSpan> = (0 until 4).map { WordSpan(it * 6_000L, (it + 1) * 6_000L) }

    @Test
    fun spansThatAgreeWithTheLettersGiveAShareOfOne() {
        val followed = follow(oneToOne, spansOfFourEqualWords())

        assertEquals(100, followed.counted)
        assertEquals(100, followed.matching)
        assertEquals(0, followed.grouped)
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

    @Test
    fun aMomentInAGroupOfTwoMatchesWhereTheEstimateShowsEitherWordAndNotWhereItShowsAThird() {
        // The group of the first two words is heard through the first half, where the estimate shows the first word until
        // 0.25 s and the second until 0.5 s, and the third word through the second half, where it shows the third word until
        // 0.75 s and then the fourth. The fourth word has a group of its own, heard from 0.75 s on
        val spans = listOf(WordSpan(0, 12_000), WordSpan(12_000, 18_000), WordSpan(18_000, 24_000))

        val followed = follow(oneToOne, spans, groupSizes = listOf(2, 1, 1))

        // The 50 moments of the first half match, and the third word at 0.5 s to 0.74 s, 25 moments, whereas the 25 moments
        // after 0.75 s have the fourth word shown against the fourth heard, which matches too
        assertEquals(100, followed.counted)
        assertEquals(100, followed.matching)
        assertEquals(50, followed.grouped)
    }

    @Test
    fun aMomentInAGroupOfTwoDoesNotMatchWhereTheEstimateShowsAThird() {
        // The group of the first two words is heard through the first 0.75 s, the estimate shows the third word from 0.5 s
        val spans = listOf(WordSpan(0, 18_000), WordSpan(18_000, 24_000))

        val followed = follow(oneToOne, spans, groupSizes = listOf(2, 2))

        assertEquals(75 + 25, followed.counted)
        assertEquals(50 + 25, followed.matching)
    }

    // The share of a speed

    private fun followedPiece(label: String, counted: Int, matching: Int, grouped: Int = 0) =
        FollowedPiece(label, "text", counted, matching, grouped)

    @Test
    fun theShareOfASpeedWeighsEachPieceByItsCountedMoments() {
        val pieces = listOf(followedPiece("0", counted = 10, matching = 10), followedPiece("1", counted = 90, matching = 0))

        assertEquals(0.1, shareOf(pieces)!!, 1e-9)
    }

    @Test
    fun theShareInGroupsIsTheMomentsInGroupsOfSeveralWordsOverTheCountedMoments() {
        val pieces = listOf(followedPiece("0", counted = 10, matching = 10, grouped = 4), followedPiece("1", counted = 30, matching = 0, grouped = 6))

        assertEquals(0.25, shareInGroupsOf(pieces)!!, 1e-9)
    }

    @Test
    fun aPieceWithNoCountedMomentsShowsADashAndAddsNothingToTheShare() {
        val empty = followedPiece("0", counted = 0, matching = 0)

        assertEquals("-", empty.shareText())
        assertEquals(0.5, shareOf(listOf(empty, followedPiece("1", 10, 5)))!!, 1e-9)
        assertNull(shareOf(listOf(empty)))
    }

    @Test
    fun theTableGivesEachSetsHeadingEachSpeedsFiguresAndNamesEachLeftOutPieceWithItsReason() {
        val pieces = listOf(
            followedPiece("3.2", counted = 100, matching = 75, grouped = 20),
            LeftOutPiece("4", "A very long text of a piece, which is cut after fifty characters at the most.", "3 runs of text for 2 words"),
        )

        val table = tableOf(listOf(SetResult("the trial", listOf(SpeedResult("1.0×", pieces)))))

        assertTrue(table, table.contains("the trial"))
        assertTrue(table, table.contains("speed 1.0×: share 75.0%, share in groups 20.0%, 1.0 s of words counted, 1 pieces counted, 1 left out"))
        assertTrue(table, table.contains("3.2  75.0%"))
        assertTrue(table, table.contains("left out: 3 runs of text for 2 words"))
        assertTrue(table, table.contains("A very long text of a piece, which is cut after fi"))
        assertTrue(table, !table.contains("fifty characters at the most"))
    }
}
