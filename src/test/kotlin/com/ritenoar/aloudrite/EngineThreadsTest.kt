package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test

class EngineThreadsTest {

    @Test
    fun oneProcessorGivesOneThread() = assertEquals(1, engineThreadCount(1))

    @Test
    fun twoProcessorsGiveOneThread() = assertEquals(1, engineThreadCount(2))

    @Test
    fun threeProcessorsGiveOneThread() = assertEquals(1, engineThreadCount(3))

    @Test
    fun fourProcessorsGiveTwoThreads() = assertEquals(2, engineThreadCount(4))

    @Test
    fun fiveProcessorsGiveTwoThreads() = assertEquals(2, engineThreadCount(5))

    @Test
    fun sixProcessorsGiveThreeThreads() = assertEquals(3, engineThreadCount(6))

    @Test
    fun eightProcessorsGiveFourThreads() = assertEquals(4, engineThreadCount(8))

    @Test
    fun nineProcessorsGiveFourThreads() = assertEquals(4, engineThreadCount(9))

    @Test
    fun twelveProcessorsGiveSixThreads() = assertEquals(6, engineThreadCount(12))

    @Test
    fun thirteenProcessorsGiveSixThreads() = assertEquals(6, engineThreadCount(13))

    @Test
    fun sixteenProcessorsGiveSixThreads() = assertEquals(6, engineThreadCount(16))

    @Test
    fun thirtyTwoProcessorsGiveSixThreads() = assertEquals(6, engineThreadCount(32))
}
