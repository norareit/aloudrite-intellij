package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingTimeTest {

    // 40 characters at the default 0.075 seconds each is 3 seconds, 20 are 1.5, and the silence is 1
    private val segments = listOf(Words("a".repeat(40)), Silence(1000), Words("bb ".repeat(10)))

    private fun readingTime(parts: List<SpokenPart> = segments) = ReadingTime(parts, SAMPLE_RATE, ReadingSpeed.NORMAL)

    // The samples of a segment come in two runs, as the engine's do
    private fun ReadingTime.made(segment: Int, seconds: Double) {
        audioMade(segment, (seconds * SAMPLE_RATE / 2).toInt())
        audioMade(segment, (seconds * SAMPLE_RATE / 2).toInt())
        segmentMade(segment)
    }

    private fun ReadingTime.heardSeconds(segment: Int, seconds: Double) = heard(segment, (seconds * SAMPLE_RATE).toLong())

    private fun assertTimes(whole: Double, left: Double, share: Double, actual: ReadingTimes) {
        assertEquals("whole", whole, actual.wholeSeconds, DELTA)
        assertEquals("left", left, actual.leftSeconds, DELTA)
        assertEquals("share", share, actual.shareRead, DELTA)
    }

    @Test
    fun beforeAnythingIsMadeTheTimesComeFromTheDefaultRate() {
        assertTimes(5.5, 5.5, 0.0, readingTime().times())
    }

    @Test
    fun aSilenceIsCountedAtItsExactLengthAtTheSpeedInUse() {
        val reading = readingTime()
        reading.speedChanged(ReadingSpeed.FASTEST)

        // 3 and 1.5 seconds halved, and the silence of 1000 milliseconds at half its length
        assertTimes(2.75, 2.75, 0.0, reading.times())
    }

    @Test
    fun aSegmentMadeInFullIsCountedAtTheLengthOfItsAudio() {
        val reading = readingTime()
        reading.made(0, 2.0)

        // The 2 seconds of audio, the silence, and 20 characters at the 0.05 seconds a character that the audio shows
        assertTimes(4.0, 4.0, 0.0, reading.times())
    }

    @Test
    fun aSegmentMadeInPartIsCountedAtTheLargerOfThePartAndTheEstimate() {
        val reading = readingTime()
        reading.audioMade(0, 100)
        assertTimes(5.5, 5.5, 0.0, reading.times())

        reading.audioMade(0, 300)
        assertTimes(6.5, 6.5, 0.0, reading.times())
    }

    @Test
    fun theRateLearntFromSegmentsMadeInFullIsKeptAtNormalSpeedAndUsedForTheRest() {
        val reading = ReadingTime(segments, SAMPLE_RATE, ReadingSpeed.FASTEST)
        reading.made(0, 1.0)

        // 1 second at 2.0 is 2 at 1.0, which is 0.05 a character, so 20 characters at 2.0 take 0.5 second
        assertTimes(1.0 + 0.5 + 0.5, 2.0, 0.0, reading.times())
    }

    @Test
    fun theTimeLeftFallsAsThePositionMovesThroughASegmentAndIntoTheNext() {
        val reading = readingTime()

        reading.heardSeconds(0, 1.0)
        assertEquals(4.5, reading.times().leftSeconds, DELTA)

        reading.heardSeconds(1, 0.5)
        assertEquals(0.5 + 1.5, reading.times().leftSeconds, DELTA)
    }

    @Test
    fun aChangeOfTheSpeedGivesBothTimesAtTheNewSpeedAtOnceDroppingTheMadeAudioAndKeepingTheRate() {
        val reading = readingTime()
        reading.made(0, 2.0)

        reading.speedChanged(ReadingSpeed.FASTEST)

        // The first segment from its entry of 0.05 a character: 1 second. The silence 0.5, and 20 characters 0.5
        assertTimes(2.0, 2.0, 0.0, reading.times())
    }

    @Test
    fun aSegmentMadeAgainAfterAChangeReplacesItsEntryInTheRate() {
        val reading = readingTime()
        reading.made(0, 2.0)
        reading.speedChanged(ReadingSpeed.FASTEST)

        reading.made(0, 1.5)

        // The entry is 3 seconds at 1.0, so 0.075 a character and not the 0.0625 of both entries together
        assertTimes(1.5 + 0.5 + 0.75, 2.75, 0.0, reading.times())
    }

    @Test
    fun theSecondsHeardIntoASegmentAreKeptForTheNewSpeedByTheirShare() {
        val reading = readingTime()
        reading.heardSeconds(0, 2.0)
        assertEquals(3.5, reading.times().leftSeconds, DELTA)

        reading.speedChanged(ReadingSpeed.FASTEST)

        assertEquals(2.75 - 1.0, reading.times().leftSeconds, DELTA)
    }

    @Test
    fun theShareReadIsNoughtAtTheStartAndGrowsWithTheSecondsHeardWithinASegment() {
        val reading = readingTime()
        assertEquals(0.0, reading.times().shareRead, DELTA)

        reading.heardSeconds(0, 1.5)
        assertEquals(40 * 0.5 / 60, reading.times().shareRead, DELTA)

        reading.heardSeconds(2, 0.0)
        assertEquals(40.0 / 60, reading.times().shareRead, DELTA)
    }

    @Test
    fun theShareReadNeverFallsWhenThePositionGoesBackToTheStartOfTheSegment() {
        val reading = readingTime()
        reading.heardSeconds(0, 2.0)
        val before = reading.times().shareRead

        reading.heardSeconds(0, 0.0)

        assertEquals(before, reading.times().shareRead, DELTA)
    }

    @Test
    fun theShareReadNeverFallsWhenTheEstimateGrows() {
        val reading = readingTime()
        reading.heardSeconds(0, 2.0)
        val before = reading.times().shareRead

        reading.audioMade(0, 1000)

        assertEquals(before, reading.times().shareRead, DELTA)
    }

    @Test
    fun theTimesStandStillWhilePausedAlthoughMoreAudioIsMade() {
        val reading = readingTime()
        reading.heardSeconds(0, 1.0)
        val atThePause = reading.times()
        reading.pause()

        reading.made(0, 2.0)
        reading.heardSeconds(0, 1.5)

        assertTimes(atThePause.wholeSeconds, atThePause.leftSeconds, atThePause.shareRead, reading.times())
    }

    @Test
    fun aChangeOfTheSpeedWhilePausedTakesTheFrozenTimesAgainAtTheNewSpeedAndAResumeFollowsAgain() {
        val reading = readingTime()
        reading.pause()

        reading.speedChanged(ReadingSpeed.FASTEST)
        assertEquals(2.75, reading.times().wholeSeconds, DELTA)

        reading.made(0, 1.0)
        assertEquals(2.75, reading.times().wholeSeconds, DELTA)

        reading.resume()
        assertEquals(1.0 + 0.5 + 0.5, reading.times().wholeSeconds, DELTA)
    }

    @Test
    fun theFirstEstimateIsTheWholeAtTheMomentTheFirstSegmentOfWordsIsMadeInFull() {
        val reading = readingTime()
        assertNull(reading.firstEstimateSeconds)
        reading.audioMade(0, 200)
        assertNull(reading.firstEstimateSeconds)

        reading.segmentMade(0)
        reading.made(2, 3.0)

        assertEquals(2.0 + 1.0 + 1.0, reading.firstEstimateSeconds!!, DELTA)
    }

    @Test
    fun aReadingOfSilencesOnlyHasAShareOfNought() {
        val reading = readingTime(listOf(Silence(500), Silence(500)))

        reading.heardSeconds(1, 0.25)

        assertTimes(1.0, 0.25, 0.0, reading.times())
    }

    @Test
    fun minutesAndSecondsRoundUpToTheWholeSecond() {
        assertEquals("0:00", minutesAndSeconds(0.0))
        assertEquals("0:01", minutesAndSeconds(0.2))
        assertEquals("1:00", minutesAndSeconds(60.0))
        assertEquals("12:40", minutesAndSeconds(760.0000000001))
        assertEquals("64:05", minutesAndSeconds(3844.5))
    }

    private companion object {
        const val SAMPLE_RATE = 100
        const val DELTA = 1e-6
    }
}
