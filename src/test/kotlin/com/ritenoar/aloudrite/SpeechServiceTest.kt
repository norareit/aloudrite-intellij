package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class SpeechServiceTest {

    private val modelFolder: Path = Paths.get("/model")
    private val events = mutableListOf<String>()
    private val handedOver = mutableListOf<() -> Unit>()
    private val failures = mutableListOf<Throwable>()
    private val firstSounds = mutableListOf<Long>()
    private val enginesCreated = mutableListOf<FakeEngine>()
    private val playersCreated = mutableListOf<FakePlayer>()

    private var whileSpeaking: () -> Unit = { }
    private var whileClosingPlayer: () -> Unit = { }
    private var engineFailure: Throwable? = null
    private var speakFailure: Throwable? = null
    private var playerFailure: Throwable? = null

    private inner class FakeEngine : SpeechEngine {
        override val sampleRate = 24_000
        var closed = false

        override fun speak(text: String, onSamples: (FloatArray) -> Unit) {
            events += "engine speaks $text"
            whileSpeaking()
            speakFailure?.let { throw it }
            onSamples(floatArrayOf(0.1f))
            onSamples(floatArrayOf(0.2f, 0.3f))
        }

        override fun close() {
            closed = true
        }
    }

    private inner class FakePlayer(val sampleRate: Int, val onFirstWrite: () -> Unit) : SamplePlayer {
        val played = mutableListOf<Int>()
        var closed = false

        override fun play(samples: FloatArray) {
            if (played.isEmpty()) onFirstWrite()
            played += samples.size
            events += "player plays ${samples.size}"
        }

        override fun close() {
            events += "player closes"
            whileClosingPlayer()
            closed = true
        }
    }

    private val service = SpeechService(
        runInBackground = { handedOver += it },
        createEngine = { folder ->
            assertEquals(modelFolder, folder)
            engineFailure?.let { throw it }
            FakeEngine().also { enginesCreated += it }
        },
        createPlayer = { sampleRate, onFirstWrite ->
            playerFailure?.let { throw it }
            FakePlayer(sampleRate, onFirstWrite).also { playersCreated += it }
        },
        reportFirstSound = { firstSounds += it },
        reportFailure = { failures += it },
    )

    private fun press(text: String = "sentence") = service.speak(text, modelFolder, System.nanoTime())

    private fun runNextHandedOverWork() = handedOver.removeAt(0)()

    @Test
    fun speakReturnsAtOnceAndOnlyHandsTheWorkToTheBackground() {
        press()

        assertEquals(1, handedOver.size)
        assertTrue(enginesCreated.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun theWorkMakesTheTextAndPlaysEachPieceAtTheRateOfTheEngine() {
        press("hello")

        runNextHandedOverWork()

        assertEquals(listOf("engine speaks hello", "player plays 1", "player plays 2", "player closes"), events)
        assertEquals(24_000, playersCreated.single().sampleRate)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun theFirstSoundIsReportedOncePerPressAtTheFirstWrite() {
        press()
        runNextHandedOverWork()
        press()
        runNextHandedOverWork()

        assertEquals(2, firstSounds.size)
        assertTrue(firstSounds.all { it >= 0 })
    }

    @Test
    fun theEngineIsCreatedAtTheFirstPressAndReused() {
        press()
        runNextHandedOverWork()
        press()
        runNextHandedOverWork()

        assertEquals(1, enginesCreated.size)
        assertEquals(2, playersCreated.size)
        assertTrue(playersCreated.all { it.closed })
    }

    @Test
    fun aPressWhileOneIsRunningIsIgnoredUntilThePlayerIsClosed() {
        var handedOverWhileSpeaking = -1
        var handedOverWhileClosing = -1
        whileSpeaking = {
            press()
            handedOverWhileSpeaking = handedOver.size
        }
        whileClosingPlayer = {
            press()
            handedOverWhileClosing = handedOver.size
        }
        press()

        runNextHandedOverWork()

        assertEquals(0, handedOverWhileSpeaking)
        assertEquals(0, handedOverWhileClosing)
        press()
        assertEquals(1, handedOver.size)
    }

    @Test
    fun aFailureOfTheEngineIsReportedAndTheNextPressWorks() {
        val failure = IllegalStateException("no engine")
        engineFailure = failure
        press()

        runNextHandedOverWork()

        assertSame(failure, failures.single())
        assertTrue(playersCreated.isEmpty())
        engineFailure = null
        press()
        runNextHandedOverWork()
        assertEquals(1, failures.size)
        assertEquals(1, enginesCreated.size)
        assertEquals(listOf(1, 2), playersCreated.single().played)
    }

    @Test
    fun aFailureWhileSpeakingIsReportedClosesThePlayerAndTheNextPressWorks() {
        val failure = IllegalStateException("speech failed")
        speakFailure = failure
        press()

        runNextHandedOverWork()

        assertSame(failure, failures.single())
        assertTrue(playersCreated.single().closed)
        speakFailure = null
        press()
        runNextHandedOverWork()
        assertEquals(1, failures.size)
        assertEquals(1, enginesCreated.size)
    }

    @Test
    fun aFailureOfThePlayerIsReportedAndTheNextPressWorks() {
        val failure = IllegalStateException("no audio device")
        playerFailure = failure
        press()

        runNextHandedOverWork()

        assertSame(failure, failures.single())
        assertTrue(playersCreated.isEmpty())
        playerFailure = null
        events.clear()
        press()
        runNextHandedOverWork()
        assertEquals(1, failures.size)
        assertEquals(listOf(1, 2), playersCreated.single().played)
        assertTrue(playersCreated.single().closed)
        assertEquals(listOf("engine speaks sentence", "player plays 1", "player plays 2", "player closes"), events)
    }

    @Test
    fun disposeClosesTheEngineWhenNothingIsRunning() {
        press()
        runNextHandedOverWork()

        service.dispose()

        assertTrue(enginesCreated.single().closed)
    }

    @Test
    fun disposeLeavesTheEngineAloneWhileASentenceIsBeingMade() {
        press()
        runNextHandedOverWork()
        whileSpeaking = { service.dispose() }
        press()

        runNextHandedOverWork()

        assertFalse(enginesCreated.single().closed)
    }

    @Test
    fun disposeWithoutAnEngineDoesNothing() {
        service.dispose()

        assertTrue(enginesCreated.isEmpty())
    }
}
