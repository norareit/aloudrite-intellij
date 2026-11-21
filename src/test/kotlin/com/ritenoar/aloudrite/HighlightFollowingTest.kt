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

    private fun heardWord(start: Int, end: Int, weight: Int) = HeardWord(SourceRange(start, end), SourceRange(0, 100), weight, "w".repeat(weight))

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
    fun whereOneToOneAndOneJoinWithOneSplitCostTheSameAGroupOfOneWordEachIsTaken() {
        // Three one-to-one steps cost 2. So does joining the first two words for "aa" and splitting the last word over "a" and "a"
        val first = spoken("first", "a")
        val second = spoken("second", "aa")
        val third = spoken("third", "a")

        val grouping = groupsOf(listOf("aa", "a", "a"), listOf(first, second, third))

        assertEquals(listOf(listOf("first"), listOf("second"), listOf("third")), groupsTexts(grouping, first, second, third))
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

    // The shortening of silences

    private val rate = 24_000

    // Samples of 0.5 around runs of zeros of the given lengths: a block of sound, then each run followed by a block of sound
    private fun audioWithSilences(vararg runs: Int, soundLength: Int = 100, startsSilent: Boolean = false, endsSilent: Boolean = false): FloatArray {
        val parts = mutableListOf<FloatArray>()
        if (!startsSilent) parts += FloatArray(soundLength) { 0.5f }
        runs.forEachIndexed { index, run ->
            parts += FloatArray(run)
            if (index < runs.size - 1 || !endsSilent) parts += FloatArray(soundLength) { 0.5f }
        }
        return parts.reduce { all, part -> all + part }
    }

    @Test
    fun aSilentRunInsideKeepsItsFirstFifthAndTheRestIsCut() {
        val shortened = shortenSilences(audioWithSilences(7_200), rate, 0.2f)

        assertEquals(listOf(CutRange(100 + 1_440, 100 + 7_200)), shortened.cuts)
        assertEquals(100 + 1_440 + 100, shortened.samples.size)
    }

    @Test
    fun aRunOf4799SilentSamplesIsKeptWholeAndOneOf4800IsShortenedTo960() {
        assertEquals(emptyList<CutRange>(), shortenSilences(audioWithSilences(4_799), rate, 0.2f).cuts)
        assertEquals(listOf(CutRange(100 + 960, 100 + 4_800)), shortenSilences(audioWithSilences(4_800), rate, 0.2f).cuts)
    }

    @Test
    fun aRunAtTheStartAndOneAtTheEndAreShortenedAsOneInsideIs() {
        val atStart = shortenSilences(audioWithSilences(5_000, startsSilent = true), rate, 0.2f)
        val atEnd = shortenSilences(audioWithSilences(5_000, endsSilent = true), rate, 0.2f)

        assertEquals(listOf(CutRange(1_000, 5_000)), atStart.cuts)
        assertEquals(listOf(CutRange(100 + 1_000, 100 + 5_000)), atEnd.cuts)
        assertEquals(1_000 + 100, atStart.samples.size)
        assertEquals(100 + 1_000, atEnd.samples.size)
    }

    @Test
    fun aSampleOfMagnitude001IsSilentAndOneOf00101EndsTheRunAlsoWhereItIsNegative() {
        val ended = FloatArray(12_000)
        ended[6_000] = -0.0101f
        val silent = FloatArray(12_000)
        silent[6_000] = -0.01f

        assertEquals(listOf(CutRange(1_200, 6_000), CutRange(6_001 + 1_199, 12_000)), shortenSilences(ended, rate, 0.2f).cuts)
        assertEquals(listOf(CutRange(2_400, 12_000)), shortenSilences(silent, rate, 0.2f).cuts)
    }

    @Test
    fun aScaleOf1KeepsTheAudioAsItIs() {
        val audio = audioWithSilences(7_200)

        val shortened = shortenSilences(audio, rate, 1f)

        assertEquals(emptyList<CutRange>(), shortened.cuts)
        assertEquals(audio.toList(), shortened.samples.toList())
    }

    @Test
    fun twoRunsInOneAudioAreBothShortened() {
        val shortened = shortenSilences(audioWithSilences(5_000, 6_000), rate, 0.2f)

        assertEquals(listOf(CutRange(100 + 1_000, 100 + 5_000), CutRange(100 + 5_000 + 100 + 1_200, 100 + 5_000 + 100 + 6_000)), shortened.cuts)
        assertEquals(100 + 1_000 + 100 + 1_200 + 100, shortened.samples.size)
    }

    // Carrying a time

    private val cuts = listOf(CutRange(100, 300), CutRange(500, 600))

    @Test
    fun aPositionBeforeTheFirstCutIsUnchanged() {
        assertEquals(50L, carriedThrough(cuts, 50L))
        assertEquals(100L, carriedThrough(cuts, 100L))
    }

    @Test
    fun aPositionWithinACutIsCarriedToThePlaceOfTheCut() {
        assertEquals(100L, carriedThrough(cuts, 200L))
        assertEquals(100L, carriedThrough(cuts, 300L))
    }

    @Test
    fun aPositionAfterACutMovesBackByItsLengthAndAfterTwoCutsByBoth() {
        assertEquals(300L, carriedThrough(cuts, 500L))
        assertEquals(600L - 300L, carriedThrough(cuts, 600L))
        assertEquals(1_000L - 300L, carriedThrough(cuts, 1_000L))
    }

    @Test
    fun theEndOfTheModelsAudioIsCarriedToTheLengthOfTheShortenedAudio() {
        val audio = audioWithSilences(7_200, 5_000)
        val shortened = shortenSilences(audio, rate, 0.2f)

        assertEquals(shortened.samples.size.toLong(), carriedThrough(shortened.cuts, audio.size.toLong()))
    }

    // The check

    private fun loud(length: Int, level: Float = 0.5f) = FloatArray(length) { if (it % 2 == 0) level else -level }

    @Test
    fun theSameAudioStandsForTheEngines() {
        assertNull(disagreementOfShortened(loud(10_000), loud(10_000)))
    }

    @Test
    fun lengths09PercentApartPassAndLengths15PercentApartFailInBothDirections() {
        assertNull(disagreementOfShortened(loud(10_090), loud(10_000)))
        assertEquals("the shortened audio is 1.5% longer than the engine's", disagreementOfShortened(loud(10_150), loud(10_000)))
        assertEquals("the shortened audio is 1.5% shorter than the engine's", disagreementOfShortened(loud(9_850), loud(10_000)))
    }

    @Test
    fun anAudioWhoseSamplesAre12PercentLargerFailsAndOne8PercentLargerPasses() {
        assertEquals("the shortened audio differs in level by 12.0% from the engine's", disagreementOfShortened(loud(10_000, 0.56f), loud(10_000)))
        assertNull(disagreementOfShortened(loud(10_000, 0.54f), loud(10_000)))
    }

    @Test
    fun aNoiseThatKeepsTheLevelsOfTheStepsPasses() {
        // Each step of 240 samples holds the same magnitudes in another order and with the signs changed
        val engine = loud(10_000)
        val shortened = FloatArray(10_000) { if (it % 2 == 0) -0.5f else 0.5f }

        assertNull(disagreementOfShortened(shortened, engine))
    }

    @Test
    fun anEngineAudioOfZerosIsSilent() {
        assertEquals("the engine's audio is silent", disagreementOfShortened(loud(10_000), FloatArray(10_000)))
    }

    // Spans

    private fun carriedRow(durations: IntArray, cuts: List<CutRange> = emptyList(), start: Long = 0, length: Long = Long.MAX_VALUE / 4) =
        CarriedRow(durations, cuts, start, length)

    @Test
    fun aPaddingTokenAndASpaceHoldNoWord() {
        // Tokens a, space, b with the padding token at both ends: frames 1 (padding), 2 (a), 3 (space), 4 (b), 5 (padding)
        val spans = spansOf(listOf(row("a b")), listOf(carriedRow(intArrayOf(1, 2, 3, 4, 5))))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan(6 * FRAME_SAMPLES, 10 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aRowWithNothingCutGivesTheSpansOfItsDurations() {
        val spans = spansOf(listOf(row("ab")), listOf(carriedRow(intArrayOf(0, 2, 3, 0))))

        assertEquals(listOf(WordSpan(0, 5 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aWordAfterACutBeginsAndEndsEarlierByTheCutsLength() {
        // Frames: padding 1, a 3, space 0, b 2, padding 1. The cut is the first frame after the padding, 600 samples
        val cut = CutRange(FRAME_SAMPLES.toInt(), 2 * FRAME_SAMPLES.toInt())
        val spans = spansOf(listOf(row("a b")), listOf(carriedRow(intArrayOf(1, 3, 0, 2, 1), listOf(cut))))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan(3 * FRAME_SAMPLES, 5 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aWordWhollyInAPartThatIsCutIsAnEmptySpanThatHoldsNoMoment() {
        // Frames: padding 1, a 1, space 0, b 2, padding 1. The cut covers the frame of a
        val cut = CutRange(FRAME_SAMPLES.toInt(), 2 * FRAME_SAMPLES.toInt())
        val spans = spansOf(listOf(row("a b")), listOf(carriedRow(intArrayOf(1, 1, 0, 2, 1), listOf(cut))))

        assertEquals(spans[0].start, spans[0].end)
        assertEquals(1, wordHeardAt(spans, spans[0].start))
        assertEquals(null, wordHeardAt(listOf(spans[0]), spans[0].start))
    }

    @Test
    fun aWordOverACutIsShorterByThePartCut() {
        // Frames: padding 1, a 4, space 0, b 1, padding 1. The cut takes the middle two frames of a
        val cut = CutRange(2 * FRAME_SAMPLES.toInt(), 4 * FRAME_SAMPLES.toInt())
        val spans = spansOf(listOf(row("a b")), listOf(carriedRow(intArrayOf(1, 4, 0, 1, 1), listOf(cut))))

        assertEquals(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), spans[0])
    }

    @Test
    fun theSecondRowsSpansBeginAtItsStartInTheEnginesAudioAndNotAtTheEndOfTheFirstRowsModelAudio() {
        val first = carriedRow(intArrayOf(1, 2, 3), start = 0, length = 4 * FRAME_SAMPLES)
        val second = carriedRow(intArrayOf(4, 5, 6), start = 4 * FRAME_SAMPLES, length = 10 * FRAME_SAMPLES)

        val spans = spansOf(listOf(row("a"), row("b")), listOf(first, second))

        assertEquals(listOf(WordSpan(1 * FRAME_SAMPLES, 3 * FRAME_SAMPLES), WordSpan(8 * FRAME_SAMPLES, 13 * FRAME_SAMPLES)), spans)
    }

    @Test
    fun aSpanIsHeldWithinTheEnginesAudioOfItsRow() {
        val spans = spansOf(listOf(row("a")), listOf(carriedRow(intArrayOf(1, 10, 1), start = 1_000, length = 3_000)))

        assertEquals(listOf(WordSpan(1_000 + 1 * FRAME_SAMPLES, 4_000)), spans)
    }

    // Following a piece

    private val oneToOne = "aaaa bbbb cccc dddd."
    private val speed = checkNotNull(parsedSpeed("1.0"))
    private val sampleRate = 24_000

    private class Followed(val counted: Int, val matching: Int, val grouped: Int, val aligned: List<Int>)

    // The audio of 1 second, which the moments divide in 100, with the spans in the samples of it. A group of one word each
    // unless `groupSizes` says otherwise
    // The words are shown by the alignment, where `starts` is not null, with each of the `delays` in samples
    private fun follow(
        text: String,
        spans: List<WordSpan>,
        groupSizes: List<Int>? = null,
        samples: Int = 24_000,
        starts: List<Long?>? = null,
        delays: List<Long> = emptyList(),
    ): Followed {
        val segments = ReadingSegments(whatToRead(text, PlaceInTheText.NoText).placedParts)
        val piece = segments.segments.indexOfFirst { it.words.isNotEmpty() }
        val reading = ReadingTime(segments.spoken, sampleRate, speed)
        reading.audioMade(piece, samples)
        reading.segmentMade(piece)
        val words = segments.segments[piece].words
        var next = 0
        val groups = (groupSizes ?: words.map { 1 }).map { size -> words.subList(next, next + size).also { next += size } }
        val followed = followPiece(piece, words, groups, spans, samples.toLong(), reading, HeardWordEstimate(segments.segments), starts, delays)
        return Followed(followed.counted, followed.matching, followed.grouped, followed.matchingAligned)
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

    @Test
    fun wordsStartingWhereTheyAreHeardGiveAShareOfOneByTheAlignmentWithNoDelay() {
        val starts = listOf<Long?>(0L, 6_000L, 12_000L, 18_000L)

        val followed = follow(oneToOne, spansOfFourEqualWords(), starts = starts, delays = listOf(0L))

        assertEquals(listOf(100), followed.aligned)
    }

    @Test
    fun aDelayShowsTheWordBeforeForItsLengthAtEachStartAfterTheFirst() {
        val starts = listOf<Long?>(0L, 6_000L, 12_000L, 18_000L)

        val followed = follow(oneToOne, spansOfFourEqualWords(), starts = starts, delays = listOf(0L, 2_400L))

        // The delay of 100 ms is 10 moments at each of the three starts after the first, where the word before is shown
        assertEquals(listOf(100, 70), followed.aligned)
    }

    @Test
    fun aPieceWithNoStartsCountsByTheEstimateForTheAlignmentToo() {
        val spans = listOf(WordSpan(0, 12_000), WordSpan(12_000, 16_000), WordSpan(16_000, 20_000), WordSpan(20_000, 24_000))

        val followed = follow(oneToOne, spans, starts = null, delays = listOf(0L, 2_400L))

        assertEquals(25 + 8 + 16, followed.matching)
        assertEquals(listOf(followed.matching, followed.matching), followed.aligned)
    }

    @Test
    fun noDelaysGiveNoSharesByTheAlignment() {
        assertEquals(emptyList<Int>(), follow(oneToOne, spansOfFourEqualWords(), starts = listOf(0L, 6_000L, 12_000L, 18_000L)).aligned)
    }

    @Test
    fun theWordShownByTheAlignmentMatchesAGroupWhenItIsOneOfItsWords() {
        // The first two words are heard together through the first half, and the alignment starts them at 0 and 3,000
        val spans = listOf(WordSpan(0, 12_000), WordSpan(12_000, 18_000), WordSpan(18_000, 24_000))
        val starts = listOf<Long?>(0L, 3_000L, 12_000L, 18_000L)

        val followed = follow(oneToOne, spans, groupSizes = listOf(2, 1, 1), starts = starts, delays = listOf(0L))

        assertEquals(listOf(100), followed.aligned)
    }

    // The share of a speed

    private fun followedPiece(label: String, counted: Int, matching: Int, grouped: Int = 0, aligned: List<Int> = emptyList(), estimateReason: String? = null) =
        FollowedPiece(label, "text", counted, matching, grouped, aligned, estimateReason)

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
    fun theShareByTheAlignmentOfADelayWeighsEachPieceByItsCountedMoments() {
        val pieces = listOf(
            followedPiece("0", counted = 10, matching = 1, aligned = listOf(10, 5)),
            followedPiece("1", counted = 90, matching = 0, aligned = listOf(0, 45)),
        )

        assertEquals(0.1, shareByAlignmentOf(pieces, 0)!!, 1e-9)
        assertEquals(0.5, shareByAlignmentOf(pieces, 1)!!, 1e-9)
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

        val table = tableOf(listOf(SetResult("the trial", listOf(SpeedResult("1.0×", pieces)))), emptyList())

        assertTrue(table, table.contains("the trial"))
        assertTrue(table, table.contains("speed 1.0×: share 75.0%, share in groups 20.0%, 1.0 s of words counted, 1 pieces counted, 1 left out"))
        assertTrue(table, table.contains("3.2  75.0%"))
        assertTrue(table, table.contains("left out: 3 runs of text for 2 words"))
        assertTrue(table, table.contains("A very long text of a piece, which is cut after fi"))
        assertTrue(table, !table.contains("fifty characters at the most"))
    }

    @Test
    fun theTableGivesTheSharesByTheAlignmentBesideTheEstimateAndNamesThePiecesThatCountedByTheEstimate() {
        val pieces = listOf(
            followedPiece("1", counted = 100, matching = 50, aligned = listOf(80, 90)),
            followedPiece("2", counted = 100, matching = 70, aligned = listOf(70, 70), estimateReason = "the aligner gave 3 intervals for 4 words"),
        )

        val table = tableOf(listOf(SetResult("the trial", listOf(SpeedResult("1.5×", pieces)))), listOf(0, 70))

        assertTrue(table, table.contains("speed 1.5×: share by the estimate 60.0%, by the alignment 75.0% with no delay and 80.0% with a delay of 70 ms"))
        assertTrue(table, table.contains("1 pieces counted by the estimate for the alignment"))
        assertTrue(table, table.contains("   1  50.0%  80.0% 90.0%"))
        assertTrue(table, table.contains("   2  70.0%  70.0% 70.0%  by the estimate: the aligner gave 3 intervals for 4 words"))
    }

    @Test
    fun withoutDelaysTheTableGivesTheShareByTheEstimateAloneAndSaysTheAlignerModelIsMissing() {
        val pieces = listOf(followedPiece("1", counted = 100, matching = 50))

        val table = tableOf(listOf(SetResult("the trial", listOf(SpeedResult("1.0×", pieces)))), emptyList())

        assertTrue(table, table.contains("The aligner model is missing, so the shares are by the estimate alone"))
        assertTrue(table, table.contains("speed 1.0×: share 50.0%"))
        assertTrue(table, !table.contains("by the alignment"))
    }
}
