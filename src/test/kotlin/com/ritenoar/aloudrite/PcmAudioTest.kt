package com.ritenoar.aloudrite

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmAudioTest {

    @Test
    fun theResultIsTwiceAsLongAsTheInput() {
        assertEquals(6, toPcm16LittleEndian(floatArrayOf(0f, 0.5f, -0.5f)).size)
    }

    @Test
    fun zeroIsTwoZeroBytes() {
        assertArrayEquals(byteArrayOf(0x00, 0x00), toPcm16LittleEndian(floatArrayOf(0f)))
    }

    @Test
    fun plusOneIs32767LowByteFirst() {
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x7F), toPcm16LittleEndian(floatArrayOf(1f)))
    }

    @Test
    fun minusOneIsMinus32767LowByteFirst() {
        assertArrayEquals(byteArrayOf(0x01, 0x80.toByte()), toPcm16LittleEndian(floatArrayOf(-1f)))
    }

    @Test
    fun samplesBeyondTheRangeAreLimited() {
        assertArrayEquals(toPcm16LittleEndian(floatArrayOf(1f)), toPcm16LittleEndian(floatArrayOf(2f)))
        assertArrayEquals(toPcm16LittleEndian(floatArrayOf(-1f)), toPcm16LittleEndian(floatArrayOf(-2f)))
    }

    // The engine gives a piece with nothing to say, such as `---`, at times as samples that are all NaN
    @Test
    fun notANumberIsTwoZeroBytes() {
        assertArrayEquals(byteArrayOf(0x00, 0x00), toPcm16LittleEndian(floatArrayOf(Float.NaN)))
    }

    @Test
    fun theInputIsNotChanged() {
        val samples = floatArrayOf(2f, -2f, 0.25f)

        toPcm16LittleEndian(samples)

        assertArrayEquals(floatArrayOf(2f, -2f, 0.25f), samples, 0f)
    }
}
