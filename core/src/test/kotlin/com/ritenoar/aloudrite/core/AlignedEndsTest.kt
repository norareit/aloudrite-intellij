package com.ritenoar.aloudrite.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlignedEndsTest {

    @Test
    fun anEndRoundedUpPastTheAudioByLessThanOneStepPasses() {
        assertTrue(isEndWithinTheAudio(1.3899997472763062, 1.3851666666666667))
    }

    @Test
    fun anEndAtTheEndOfTheAudioPasses() {
        assertTrue(isEndWithinTheAudio(1.3851666666666667, 1.3851666666666667))
    }

    @Test
    fun anEndOneStepPastTheAudioFails() {
        val audioSeconds = 1.3851666666666667

        assertFalse(isEndWithinTheAudio(audioSeconds + ALIGNER_STEP_SECONDS, audioSeconds))
    }
}
