package com.ritenoar.aloudrite

import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

// A tenth of a second is then 10 samples, which keeps the slices of the playing easy to count
const val STAND_IN_SAMPLE_RATE = 100
const val STAND_IN_PIECES_PER_TEXT = 2
const val STAND_IN_SAMPLES_PER_PIECE = 25

// A SpeechService without the model and without an audio device. Its background works are kept, in the order they
// were handed over, until the test runs them
class SpeechStandIns {

    val keptWorks: MutableList<() -> Unit> = Collections.synchronizedList(mutableListOf())
    val engines = mutableListOf<StandInEngine>()
    val players: MutableList<StandInPlayer> = CopyOnWriteArrayList()
    val modelFolders = mutableListOf<Path>()
    val firstSounds = mutableListOf<Long>()
    val failures: MutableList<Pair<ReadingTarget, Throwable>> = Collections.synchronizedList(mutableListOf())
    val changes = AtomicInteger()

    // Every text the engines were given, in order, also across engines
    val textsMade: MutableList<String> = Collections.synchronizedList(mutableListOf())

    var engineFailsAt: (text: String) -> Boolean = { false }
    var whileMaking: (text: String) -> Unit = { }
    var whilePlaying: (slicesPlayed: Int) -> Unit = { }
    var makingTheEngineFails: Throwable? = null
    var playerFails: Throwable? = null
    var closingThePlayerFails: Throwable? = null
    var whileClosing: () -> Unit = { }

    // Not published by default, since some tests use the stand-ins without the application, and so without its bus
    var whenChanged: () -> Unit = { }

    val service = SpeechService(
        runInBackground = { work -> keptWorks += work },
        createEngine = { folder ->
            modelFolders.add(folder)
            makingTheEngineFails?.let { throw it }
            StandInEngine().also { engines += it }
        },
        createPlayer = { sampleRate, onFirstWrite ->
            check(sampleRate == STAND_IN_SAMPLE_RATE) { "The player was made at $sampleRate" }
            StandInPlayer(onFirstWrite).also { players += it }
        },
        reportFirstSound = { milliseconds -> firstSounds += milliseconds },
        reportFailure = { target, failure -> failures += target to failure },
        onChange = {
            changes.incrementAndGet()
            whenChanged()
        },
    )

    fun runKeptWorks() {
        while (keptWorks.isNotEmpty()) keptWorks.removeAt(0)()
    }

    fun runNextKeptWork() = keptWorks.removeAt(0)()

    // The value of every sample says which text it was made from: the first text the engines were given makes 1.0
    fun samplesPlayed(): List<Float> = players.flatMap { player -> player.played.flatMap { it.toList() } }

    // What was played, in order: each run of silence as its length, and each run of sound as the texts it was made
    // from, joined, so that the cuts of piecesOf do not show
    fun heard(): List<String> {
        val runs = mutableListOf<List<Float>>()
        for (sample in samplesPlayed()) {
            val lastRun = runs.lastOrNull()
            if (lastRun != null && (lastRun.first() == 0f) == (sample == 0f)) {
                runs[runs.lastIndex] = lastRun + sample
            } else {
                runs += listOf(sample)
            }
        }
        return runs.map { run ->
            if (run.first() == 0f) {
                "<silence ${run.size * 1000 / STAND_IN_SAMPLE_RATE} ms>"
            } else {
                run.distinct().joinToString("") { textNumber -> textsMade[textNumber.toInt() - 1] }
            }
        }
    }

    inner class StandInEngine : SpeechEngine {
        override val sampleRate = STAND_IN_SAMPLE_RATE
        var closed = false
        val answers: MutableList<Boolean> = Collections.synchronizedList(mutableListOf())

        override fun speak(text: String, onSamples: (FloatArray) -> Boolean) {
            textsMade += text
            val textNumber = textsMade.size.toFloat()
            whileMaking(text)
            if (engineFailsAt(text)) throw IllegalStateException("The engine failed at \"$text\"")
            repeat(STAND_IN_PIECES_PER_TEXT) {
                val goOn = onSamples(FloatArray(STAND_IN_SAMPLES_PER_PIECE) { textNumber })
                answers += goOn
                if (!goOn) return
            }
        }

        override fun close() {
            closed = true
        }
    }

    inner class StandInPlayer(private val onFirstWrite: () -> Unit) : SamplePlayer {
        val played: MutableList<FloatArray> = CopyOnWriteArrayList()
        var closed = false
        var discarded = false

        override fun play(samples: FloatArray) {
            if (played.isEmpty()) onFirstWrite()
            playerFails?.let { throw it }
            played += samples.copyOf()
            whilePlaying(played.size)
        }

        override fun close() {
            whileClosing()
            closingThePlayerFails?.let { throw it }
            closed = true
        }

        override fun discard() {
            discarded = true
        }
    }
}
