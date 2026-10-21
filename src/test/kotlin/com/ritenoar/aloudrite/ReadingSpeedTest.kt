package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSpeedTest {

    @Test
    fun theSixteenValuesGoFromHalfToTwiceInStepsOfATenth() {
        val shown = ReadingSpeed.ALL.map { it.display }

        assertEquals(
            listOf("0.5×", "0.6×", "0.7×", "0.8×", "0.9×", "1.0×", "1.1×", "1.2×",
                "1.3×", "1.4×", "1.5×", "1.6×", "1.7×", "1.8×", "1.9×", "2.0×"),
            shown,
        )
        assertTrue(ReadingSpeed.NORMAL in ReadingSpeed.ALL)
    }

    @Test
    fun theSpeedIsShownWithOneDecimalAndTheMultiplicationSign() {
        assertEquals("0.5×", ReadingSpeed.SLOWEST.display)
        assertEquals("1.0×", ReadingSpeed.NORMAL.display)
        assertEquals("2.0×", ReadingSpeed.FASTEST.display)
        assertEquals("1.0", ReadingSpeed.NORMAL.number)
    }

    @Test
    fun theEngineIsGivenTheSpeedAsAFactor() {
        assertEquals(1.0f, ReadingSpeed.NORMAL.forTheEngine)
        assertEquals(2.0f, ReadingSpeed.FASTEST.forTheEngine)
    }

    @Test
    fun slowerAndFasterGoOneStepAndStayWithinTheRange() {
        assertEquals("0.9×", ReadingSpeed.NORMAL.slower().display)
        assertEquals("1.1×", ReadingSpeed.NORMAL.faster().display)
        assertTrue(ReadingSpeed.NORMAL.canGoSlower)
        assertTrue(ReadingSpeed.NORMAL.canGoFaster)

        assertFalse(ReadingSpeed.SLOWEST.canGoSlower)
        assertEquals(ReadingSpeed.SLOWEST, ReadingSpeed.SLOWEST.slower())
        assertFalse(ReadingSpeed.FASTEST.canGoFaster)
        assertEquals(ReadingSpeed.FASTEST, ReadingSpeed.FASTEST.faster())
    }

    @Test
    fun aSilenceLastsItsLengthAtNormalSpeedDividedByTheSpeed() {
        assertEquals(250, ReadingSpeed.FASTEST.silenceMilliseconds(500))
        assertEquals(1000, ReadingSpeed.SLOWEST.silenceMilliseconds(500))
        assertEquals(1000, ReadingSpeed.FASTEST.silenceMilliseconds(2000))
        assertEquals(4000, ReadingSpeed.SLOWEST.silenceMilliseconds(2000))
        assertEquals(357, parsedSpeed("0.7")!!.silenceMilliseconds(250))
        assertEquals(500, ReadingSpeed.NORMAL.silenceMilliseconds(500))
    }

    @Test
    fun aTypedSpeedFromHalfToTwiceWithOneDecimalAtTheMostIsAccepted() {
        assertEquals("2.0×", parsedSpeed("2")?.display)
        assertEquals("1.5×", parsedSpeed("1.5")?.display)
        assertEquals("2.0×", parsedSpeed("2.0")?.display)
        assertEquals("0.5×", parsedSpeed("0.5")?.display)
        assertEquals("1.5×", parsedSpeed(" 1.5 ")?.display)
    }

    @Test
    fun aTypedSpeedOutOfTheRangeOrWrittenOtherwiseIsNotAccepted() {
        for (text in listOf("1.55", "1,5", "2.1", "0.4", "", "1.", ".5", "abc", "-1")) {
            assertNull("\"$text\" was accepted", parsedSpeed(text))
        }
    }
}
