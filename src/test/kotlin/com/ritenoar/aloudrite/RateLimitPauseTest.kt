package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimitPauseTest {

    @Test
    fun retryAfterGivesItsSecondsWhateverTheRateLimitHeaderSays() {
        assertEquals(30L, rateLimitPause("30", "\"resolvers\";r=0;t=120"))
    }

    @Test
    fun onlyTheRateLimitHeaderGivesItsTimeUntilTheWindowStartsAgain() {
        assertEquals(120L, rateLimitPause(null, "\"resolvers\";r=0;t=120"))
    }

    @Test
    fun neitherHeaderGivesAMinute() {
        assertEquals(60L, rateLimitPause(null, null))
    }

    @Test
    fun aPauseIsNeverLongerThanFiveMinutes() {
        assertEquals(300L, rateLimitPause("900", null))
        assertEquals(300L, rateLimitPause(null, "\"resolvers\";r=0;t=900"))
    }

    @Test
    fun aRetryAfterThatIsNotANumberOfSecondsFallsToTheRateLimitHeader() {
        assertEquals(120L, rateLimitPause("Wed, 21 Oct 2026 07:28:00 GMT", "\"resolvers\";r=0;t=120"))
        assertEquals(60L, rateLimitPause("soon", null))
    }

    @Test
    fun aRateLimitHeaderWithoutATimeFallsToAMinute() {
        assertEquals(60L, rateLimitPause(null, "\"resolvers\";r=12"))
    }

    @Test
    fun theCountdownShowsMinutesAndSeconds() {
        assertEquals("Continuing in 4:37", rateLimitCountdownText(277))
        assertEquals("Continuing in 0:05", rateLimitCountdownText(5))
        assertEquals("Continuing in 5:00", rateLimitCountdownText(300))
    }
}
