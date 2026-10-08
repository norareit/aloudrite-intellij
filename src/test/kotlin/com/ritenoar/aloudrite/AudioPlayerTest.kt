package com.ritenoar.aloudrite

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import javax.sound.sampled.SourceDataLine

class AudioPlayerTest {

    private val calls = mutableListOf<String>()
    private val written = mutableListOf<ByteArray>()
    private var failOnDrain = false

    private val line = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(SourceDataLine::class.java)
    ) { _, method, arguments ->
        calls += method.name
        when (method.name) {
            "write" -> {
                @Suppress("UNCHECKED_CAST")
                val bytes = arguments[0] as ByteArray
                written += bytes.copyOfRange(arguments[1] as Int, (arguments[1] as Int) + (arguments[2] as Int))
                arguments[2]
            }
            "drain" -> if (failOnDrain) throw IllegalStateException("drain failed") else null
            else -> null
        }
    } as SourceDataLine

    @Test
    fun theLineIsStartedWhenThePlayerIsMade() {
        AudioPlayer(line) { }

        assertEquals(listOf("start"), calls)
    }

    @Test
    fun playWritesTheSamplesAsPcm() {
        val player = AudioPlayer(line) { }

        player.play(floatArrayOf(0f, 1f))

        assertEquals(1, written.size)
        assertArrayEquals(toPcm16LittleEndian(floatArrayOf(0f, 1f)), written.single())
    }

    @Test
    fun theFirstWriteIsAnnouncedOnceAndBeforeTheFirstWrite() {
        var announcements = 0
        val player = AudioPlayer(line) {
            announcements++
            assertTrue("write came before the announcement", "write" !in calls)
        }

        player.play(floatArrayOf(0.5f))
        player.play(floatArrayOf(0.5f))

        assertEquals(1, announcements)
        assertEquals(2, written.size)
    }

    @Test
    fun closeDrainsTheLineAndThenClosesIt() {
        val player = AudioPlayer(line) { }

        player.close()

        assertEquals(listOf("start", "drain", "close"), calls)
    }

    @Test
    fun closeStillClosesTheLineWhenDrainFails() {
        val player = AudioPlayer(line) { }
        failOnDrain = true

        val failure = runCatching { player.close() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(listOf("start", "drain", "close"), calls)
    }

}
