package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The times of a reading as the reading keeps them. Each text of the stand-ins makes half a second of audio, so the
// reading below is one and a half seconds long, in fifteen slices of a tenth of a second
class ReadingAloudTimeTest {

    private val standIns = SpeechStandIns<String>()
    private val reading = standIns.reading
    private val target = "notes.md"
    private val parts = listOf(Words("One."), Silence(500), Words("Two."))

    private fun read(readingParts: List<SpokenPart> = parts) =
        reading.read(target, wholeFile(readingParts), INT8_MODEL, System.nanoTime())

    private fun timesOfTheReading(): ReadingTimes = checkNotNull(reading.timesOf(target))

    @Test
    fun thereAreNoTimesBeforeAReadingAndNoneForAnotherTarget() {
        assertNull(reading.timesOf(target))

        read()

        assertNull(reading.timesOf("other.md"))
    }

    @Test
    fun atTheStartTheWholeComesFromTheTextAndTheShareIsNought() {
        read()

        val times = timesOfTheReading()

        assertEquals(0.3 + 0.5 + 0.3, times.wholeSeconds, DELTA)
        assertEquals(times.wholeSeconds, times.leftSeconds, DELTA)
        assertEquals(0.0, times.shareRead, DELTA)
    }

    @Test
    fun onceTheAudioIsMadeTheWholeIsTheLengthOfTheAudioSilencesIncluded() {
        read()

        standIns.runNextKeptWork()

        val times = timesOfTheReading()
        assertEquals(1.5, times.wholeSeconds, DELTA)
        assertEquals(1.5, times.leftSeconds, DELTA)
    }

    @Test
    fun theTimeLeftFallsAndTheShareGrowsSliceBySliceWhileTheAudioPlays() {
        val lefts = mutableListOf<Double>()
        val shares = mutableListOf<Double>()
        standIns.whilePlaying = {
            lefts += timesOfTheReading().leftSeconds
            shares += timesOfTheReading().shareRead
        }
        read()

        standIns.runKeptWorks()

        // The position is taken after a slice is played, so the first look is at the start
        assertEquals(1.5, lefts.first(), DELTA)
        assertEquals(lefts.sortedDescending(), lefts)
        assertTrue(lefts.last() < 0.3)
        assertEquals(shares.sorted(), shares)
        assertEquals(0.0, shares.first(), DELTA)
        assertTrue(shares.last() > 0.5)
    }

    @Test
    fun bothTimesStandStillDuringAPauseAlthoughTheMakingRunsOn() {
        val partsWithALongText = listOf(Words("One."), Words("Two three four."))
        var frozen: ReadingTimes? = null
        standIns.whileMaking = { text ->
            if (text == "Two three four.") {
                reading.pause(target)
                frozen = timesOfTheReading()
            }
        }
        read(partsWithALongText)

        standIns.runKeptWorks()

        val atTheEnd = timesOfTheReading()
        assertEquals(frozen!!.wholeSeconds, atTheEnd.wholeSeconds, DELTA)
        assertEquals(frozen!!.leftSeconds, atTheEnd.leftSeconds, DELTA)
        assertEquals(frozen!!.shareRead, atTheEnd.shareRead, DELTA)

        reading.resume(target, System.nanoTime())

        assertEquals(1.0, timesOfTheReading().wholeSeconds, DELTA)
    }

    @Test
    fun aChangeOfTheSpeedGivesTheTimesAtTheNewSpeedBeforeTheNewMakingStartsAndTheShareDoesNotFall() {
        var shareBefore = 0.0
        var timesAfter: ReadingTimes? = null
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == 8) {
                shareBefore = timesOfTheReading().shareRead
                reading.changeSpeed(ReadingSpeed.FASTEST, afterAWait = false)
                timesAfter = timesOfTheReading()
            }
        }
        read()

        standIns.runKeptWorks()

        // Both words at 0.25 seconds from their entries at 1.0, and the silence of 500 milliseconds at 0.25
        assertEquals(0.75, timesAfter!!.wholeSeconds, DELTA)
        assertTrue(timesAfter!!.shareRead >= shareBefore)
    }

    @Test
    fun thereAreNoTimesOnceTheReadingEndedWasStoppedOrFailed() {
        read()
        standIns.runKeptWorks()
        assertNull(reading.timesOf(target))

        read()
        reading.stop(target)
        assertNull(reading.timesOf(target))

        standIns.makingTheEngineFails = IllegalStateException("No engine")
        read()
        standIns.runKeptWorks()
        assertNull(reading.timesOf(target))
    }

    @Test
    fun theEndOfAReadingIsReportedOnceWithTheFirstEstimateAndTheTimeSinceTheFirstSound() {
        standIns.whilePlaying = { slicesPlayed -> if (slicesPlayed == 3) standIns.passTime(2000) }
        read()

        standIns.runKeptWorks()

        val report = standIns.readingTimeReports.single()
        assertEquals(target, report.target)
        assertEquals(1.5, report.firstEstimateSeconds!!, DELTA)
        assertEquals(2000L, report.tookMillis)
    }

    @Test
    fun aReadingThatWasStoppedIsNotReported() {
        standIns.whilePlaying = { slicesPlayed -> if (slicesPlayed == 3) reading.stop(target) }
        read()

        standIns.runKeptWorks()

        assertTrue(standIns.readingTimeReports.isEmpty())
    }

    private companion object {
        const val DELTA = 1e-6
    }
}
