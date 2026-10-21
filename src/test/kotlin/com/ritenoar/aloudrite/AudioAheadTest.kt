package com.ritenoar.aloudrite

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AudioAheadTest {

    private val audioAhead = AudioAhead(limitInSamples = 4)

    @Test
    fun whatIsPutIsTakenInOrder() {
        audioAhead.put(run(0.1f))
        audioAhead.put(run(0.2f, 0.3f))

        assertArrayEquals(floatArrayOf(0.1f), audioAhead.takeRun().samples, 0f)
        assertArrayEquals(floatArrayOf(0.2f, 0.3f), audioAhead.takeRun().samples, 0f)
    }

    @Test
    fun takeWaitsWhileItIsEmptyAndGivesTheNextPieceOnceOneIsPut() {
        val taken = inTheBackground { audioAhead.take() }

        assertStillWaiting(taken)
        audioAhead.put(run(0.5f))

        assertArrayEquals(floatArrayOf(0.5f), taken.runTaken().samples, 0f)
    }

    @Test
    fun afterFinishTakeGivesWhatIsLeftAndThenNull() {
        audioAhead.put(run(0.1f))
        audioAhead.finish()

        assertArrayEquals(floatArrayOf(0.1f), audioAhead.takeRun().samples, 0f)
        assertSame(Taken.End, audioAhead.take())
    }

    @Test
    fun finishWakesATakeThatWaits() {
        val taken = inTheBackground { audioAhead.take() }

        assertStillWaiting(taken)
        audioAhead.finish()

        assertSame(Taken.End, taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun putWaitsWhileTheSamplesWaitingReachTheLimitAndGoesOnOnceEnoughWasTaken() {
        audioAhead.put(run(0.1f, 0.2f))
        audioAhead.put(run(0.3f, 0.4f))
        val put = inTheBackground { audioAhead.put(run(0.5f)) }

        assertStillWaiting(put)
        audioAhead.take()

        assertTrue(put.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        audioAhead.take()
        assertArrayEquals(floatArrayOf(0.5f), audioAhead.takeRun().samples, 0f)
    }

    @Test
    fun cancelWakesAPutAndATakeThatWait() {
        val emptyAhead = AudioAhead(limitInSamples = 1)
        val taken = inTheBackground { emptyAhead.take() }
        assertStillWaiting(taken)
        emptyAhead.cancel()
        assertSame(Taken.End, taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))

        audioAhead.put(run(0.1f, 0.2f, 0.3f, 0.4f))
        val put = inTheBackground { audioAhead.put(run(0.5f)) }
        assertStillWaiting(put)
        audioAhead.cancel()
        assertFalse(put.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun afterCancelPutDropsWhatItIsGivenAndTakeGivesNull() {
        audioAhead.put(run(0.1f))
        audioAhead.cancel()

        assertFalse(audioAhead.put(run(0.2f)))
        assertSame(Taken.End, audioAhead.take())
        assertSame(Taken.End, audioAhead.take())
    }

    @Test
    fun aRunKeepsItsSegmentAndWhetherItIsASilence() {
        audioAhead.put(AudioRun(segment = 3, isSilence = true, samples = floatArrayOf(0f)))

        val taken = audioAhead.takeRun()

        assertEquals(3, taken.segment)
        assertTrue(taken.isSilence)
    }

    @Test
    fun theLimitCountsTheSamplesOfTheRunsAndNotTheRuns() {
        audioAhead.put(run(0.1f, 0.2f, 0.3f, 0.4f))
        val put = inTheBackground { audioAhead.put(run(0.5f)) }

        assertStillWaiting(put)
        audioAhead.take()

        assertTrue(put.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun wakeMakesATakeThatWaitsSayThatItWasWoken() {
        val taken = inTheBackground { audioAhead.take() }

        assertStillWaiting(taken)
        audioAhead.wake()

        assertSame(Taken.Woken, taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun aWakeBeforeTheTakeGivesTheRunThatWaitsAndIsAnsweredOnce() {
        audioAhead.put(run(0.1f))
        audioAhead.wake()

        assertArrayEquals(floatArrayOf(0.1f), audioAhead.takeRun().samples, 0f)
        val taken = inTheBackground { audioAhead.take() }
        assertStillWaiting(taken)
        audioAhead.put(run(0.2f))
        assertArrayEquals(floatArrayOf(0.2f), taken.runTaken().samples, 0f)
    }

    @Test
    fun aWakeOfATakeThatDoesNotWaitIsAnsweredByTheNextTakeOnly() {
        audioAhead.wake()

        assertSame(Taken.Woken, audioAhead.take())
        val taken = inTheBackground { audioAhead.take() }
        assertStillWaiting(taken)
        audioAhead.finish()
        assertSame(Taken.End, taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private fun AudioAhead.takeRun(): AudioRun = (take() as Taken.Run).run

    private fun CompletableFuture<Taken>.runTaken(): AudioRun =
        (get(TIMEOUT_SECONDS, TimeUnit.SECONDS) as Taken.Run).run

    private fun run(vararg samples: Float) = AudioRun(segment = 0, isSilence = false, samples = samples)

    private fun <T> inTheBackground(work: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(work)

    private fun assertStillWaiting(future: CompletableFuture<*>) {
        try {
            future.get(WAITING_MILLISECONDS, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            return
        }
        throw AssertionError("It did not wait")
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
        const val WAITING_MILLISECONDS = 200L
    }
}
