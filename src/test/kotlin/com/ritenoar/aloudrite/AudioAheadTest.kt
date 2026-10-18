package com.ritenoar.aloudrite

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AudioAheadTest {

    private val audioAhead = AudioAhead(limitInSamples = 4)

    @Test
    fun whatIsPutIsTakenInOrder() {
        audioAhead.put(floatArrayOf(0.1f))
        audioAhead.put(floatArrayOf(0.2f, 0.3f))

        assertArrayEquals(floatArrayOf(0.1f), audioAhead.take(), 0f)
        assertArrayEquals(floatArrayOf(0.2f, 0.3f), audioAhead.take(), 0f)
    }

    @Test
    fun takeWaitsWhileItIsEmptyAndGivesTheNextPieceOnceOneIsPut() {
        val taken = inTheBackground { audioAhead.take() }

        assertStillWaiting(taken)
        audioAhead.put(floatArrayOf(0.5f))

        assertArrayEquals(floatArrayOf(0.5f), taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), 0f)
    }

    @Test
    fun afterFinishTakeGivesWhatIsLeftAndThenNull() {
        audioAhead.put(floatArrayOf(0.1f))
        audioAhead.finish()

        assertArrayEquals(floatArrayOf(0.1f), audioAhead.take(), 0f)
        assertNull(audioAhead.take())
    }

    @Test
    fun finishWakesATakeThatWaits() {
        val taken = inTheBackground { audioAhead.take() }

        assertStillWaiting(taken)
        audioAhead.finish()

        assertNull(taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun putWaitsWhileTheSamplesWaitingReachTheLimitAndGoesOnOnceEnoughWasTaken() {
        audioAhead.put(floatArrayOf(0.1f, 0.2f))
        audioAhead.put(floatArrayOf(0.3f, 0.4f))
        val put = inTheBackground { audioAhead.put(floatArrayOf(0.5f)) }

        assertStillWaiting(put)
        audioAhead.take()

        assertTrue(put.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        audioAhead.take()
        assertArrayEquals(floatArrayOf(0.5f), audioAhead.take(), 0f)
    }

    @Test
    fun cancelWakesAPutAndATakeThatWait() {
        val emptyAhead = AudioAhead(limitInSamples = 1)
        val taken = inTheBackground { emptyAhead.take() }
        assertStillWaiting(taken)
        emptyAhead.cancel()
        assertNull(taken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))

        audioAhead.put(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
        val put = inTheBackground { audioAhead.put(floatArrayOf(0.5f)) }
        assertStillWaiting(put)
        audioAhead.cancel()
        assertFalse(put.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun afterCancelPutDropsWhatItIsGivenAndTakeGivesNull() {
        audioAhead.put(floatArrayOf(0.1f))
        audioAhead.cancel()

        assertFalse(audioAhead.put(floatArrayOf(0.2f)))
        assertNull(audioAhead.take())
        assertNull(audioAhead.take())
    }

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
