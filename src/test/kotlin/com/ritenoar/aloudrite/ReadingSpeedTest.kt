package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ReadingSpeedTest {

    private fun speed(typed: String): ReadingSpeed = checkNotNull(parsedSpeed(typed))

    @Test
    fun theNineteenStepsGoByATwentiethUpTo1Point3AndByATenthAboveIt() {
        val shown = ReadingSpeed.STEPS.map { it.display }

        assertEquals(
            listOf("0.75×", "0.8×", "0.85×", "0.9×", "0.95×", "1.0×", "1.05×", "1.1×", "1.15×", "1.2×", "1.25×",
                "1.3×", "1.4×", "1.5×", "1.6×", "1.7×", "1.8×", "1.9×", "2.0×"),
            shown,
        )
        assertTrue(ReadingSpeed.NORMAL in ReadingSpeed.STEPS)
    }

    @Test
    fun theSpeedIsShownWithTwoDecimalsWhereTheSecondIsNotNought() {
        assertEquals("0.75×", ReadingSpeed.SLOWEST.display)
        assertEquals("1.25×", speed("1.25").display)
        assertEquals("1.3×", speed("1.30").display)
        assertEquals("1.0×", ReadingSpeed.NORMAL.display)
        assertEquals("2.0×", ReadingSpeed.FASTEST.display)
        assertEquals("1.37", speed("1.37").number)
        assertEquals("1.0", ReadingSpeed.NORMAL.number)
    }

    @Test
    fun theNumberHasLatinDigitsUnderALocaleWhoseDigitsAreNotLatinAndReadsBack() {
        val before = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        try {
            assertEquals("0.75", ReadingSpeed.SLOWEST.number)
            assertEquals(ReadingSpeed.SLOWEST, parsedSpeed(ReadingSpeed.SLOWEST.number))
            assertEquals("1.37", speed("1.37").number)
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun theEngineIsGivenTheSpeedAsAFactor() {
        assertEquals(0.75f, ReadingSpeed.SLOWEST.forTheEngine)
        assertEquals(1.0f, ReadingSpeed.NORMAL.forTheEngine)
        assertEquals(2.0f, ReadingSpeed.FASTEST.forTheEngine)
    }

    @Test
    fun slowerAndFasterGoAlongTheStepsAndStayWithinTheRange() {
        assertEquals(speed("0.95"), ReadingSpeed.NORMAL.slower())
        assertEquals(speed("1.05"), ReadingSpeed.NORMAL.faster())
        assertEquals(speed("1.4"), speed("1.3").faster())
        assertEquals(speed("1.3"), speed("1.4").slower())
        assertTrue(ReadingSpeed.NORMAL.canGoSlower)
        assertTrue(ReadingSpeed.NORMAL.canGoFaster)

        assertFalse(ReadingSpeed.SLOWEST.canGoSlower)
        assertEquals(ReadingSpeed.SLOWEST, ReadingSpeed.SLOWEST.slower())
        assertFalse(ReadingSpeed.FASTEST.canGoFaster)
        assertEquals(ReadingSpeed.FASTEST, ReadingSpeed.FASTEST.faster())
    }

    @Test
    fun slowerAndFasterFromASpeedBetweenTwoStepsGoToTheNearestStep() {
        assertEquals(speed("1.3"), speed("1.37").slower())
        assertEquals(speed("1.4"), speed("1.37").faster())
        assertEquals(speed("0.75"), speed("0.77").slower())
        assertEquals(speed("0.8"), speed("0.77").faster())
        assertEquals(speed("2.0"), speed("1.93").faster())
    }

    @Test
    fun aSilenceLastsItsLengthAtNormalSpeedDividedByTheSpeed() {
        assertEquals(250, ReadingSpeed.FASTEST.silenceMilliseconds(500))
        assertEquals(667, ReadingSpeed.SLOWEST.silenceMilliseconds(500))
        assertEquals(1000, ReadingSpeed.FASTEST.silenceMilliseconds(2000))
        assertEquals(2667, ReadingSpeed.SLOWEST.silenceMilliseconds(2000))
        assertEquals(500, ReadingSpeed.NORMAL.silenceMilliseconds(500))
    }

    @Test
    fun aTypedSpeedFrom0Point75To2WithTwoDecimalsAtTheMostIsAccepted() {
        assertEquals("0.75×", parsedSpeed("0.75")?.display)
        assertEquals("1.37×", parsedSpeed("1.37")?.display)
        assertEquals("2.0×", parsedSpeed("2")?.display)
        assertEquals("1.5×", parsedSpeed("1.5")?.display)
        assertEquals("1.25×", parsedSpeed(" 1.25 ")?.display)
    }

    @Test
    fun aTypedSpeedOutOfTheRangeOrWrittenOtherwiseIsNotAccepted() {
        for (text in listOf("0.7", "2.01", "1.375", "1,5", "", "1.", ".75", "abc", "-1", "0.5")) {
            assertNull("\"$text\" was accepted", parsedSpeed(text))
        }
    }

    @Test
    fun theStepsWithASpeedBetweenTwoOfThemHoldItInItsPlace() {
        val choices = ReadingSpeed.stepsWith(speed("1.37"))

        assertEquals(20, choices.size)
        assertEquals(listOf(speed("1.3"), speed("1.37"), speed("1.4")), choices.subList(11, 14))
        assertEquals(ReadingSpeed.STEPS, ReadingSpeed.stepsWith(speed("1.25")))
    }
}
