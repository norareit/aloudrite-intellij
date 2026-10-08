package com.ritenoar.aloudrite

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneAtATimeTest {

    private val handedOver = mutableListOf<() -> Unit>()
    private val oneAtATime = OneAtATime { handedOver += it }

    @Test
    fun workIsHandedOverWhenNothingIsRunning() {
        assertTrue(oneAtATime.tryRun { })

        assertTrue(handedOver.size == 1)
        assertTrue(oneAtATime.isRunning)
    }

    @Test
    fun aSecondWorkIsRefusedAndNeverHandedOverWhileTheFirstIsRunning() {
        var secondRan = false
        oneAtATime.tryRun { }

        assertFalse(oneAtATime.tryRun { secondRan = true })

        assertTrue(handedOver.size == 1)
        handedOver.single()()
        assertFalse(secondRan)
    }

    @Test
    fun workIsAcceptedAgainAfterTheFirstHasEnded() {
        oneAtATime.tryRun { }
        handedOver.single()()

        assertFalse(oneAtATime.isRunning)
        assertTrue(oneAtATime.tryRun { })
    }

    @Test
    fun workIsAcceptedAgainAfterWorkThatThrows() {
        oneAtATime.tryRun { throw IllegalStateException("failed") }
        runCatching { handedOver.single()() }

        assertFalse(oneAtATime.isRunning)
        assertTrue(oneAtATime.tryRun { })
    }

    @Test
    fun workIsAcceptedAgainAfterABackgroundFunctionThatThrows() {
        var failNext = true
        val handed = mutableListOf<() -> Unit>()
        val flaky = OneAtATime { work ->
            if (failNext) throw IllegalStateException("no thread")
            handed += work
        }

        assertTrue(runCatching { flaky.tryRun { } }.isFailure)
        assertFalse(flaky.isRunning)

        failNext = false
        assertTrue(flaky.tryRun { })
    }
}
