package com.ritenoar.aloudrite

import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

// A tenth of a second is then 10 samples, which keeps the slices of the playing easy to count
const val STAND_IN_SAMPLE_RATE = 100
const val STAND_IN_PIECES_PER_TEXT = 2
const val STAND_IN_SAMPLES_PER_PIECE = 25

data class FirstSound(val milliseconds: Long, val after: FirstSoundAfter)

// A SpeechService without the model, without an audio device and without real time. Its background works are kept, in
// the order they were handed over, until the test runs them, and its delayed works run when the test passes the time
class SpeechStandIns {

    private class LaterWork(val dueMillis: Long, val work: () -> Unit)

    val keptWorks: MutableList<() -> Unit> = Collections.synchronizedList(mutableListOf())
    val engines = mutableListOf<StandInEngine>()
    val players: MutableList<StandInPlayer> = CopyOnWriteArrayList()
    val modelFolders = mutableListOf<Path>()
    val firstSounds: MutableList<FirstSound> = Collections.synchronizedList(mutableListOf())
    val failures: MutableList<Pair<ReadingTarget, Throwable>> = Collections.synchronizedList(mutableListOf())
    val changes = AtomicInteger()

    // Every text the engines were given, in order, also across engines, and the speed it was given with
    val textsMade: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val speedsMade: MutableList<Float> = Collections.synchronizedList(mutableListOf())

    // The speed of the setting, which a reading takes when it starts
    var speed: ReadingSpeed = ReadingSpeed.NORMAL

    // The clock of the service, which only passTime moves on
    var clockMillis = 0L
        private set
    private val laterWorks: MutableList<LaterWork> = Collections.synchronizedList(mutableListOf())

    var engineFailsAt: (text: String) -> Boolean = { false }
    var whileMaking: (text: String) -> Unit = { }
    var whilePlaying: (slicesPlayed: Int) -> Unit = { }
    var makingTheEngineFails: Throwable? = null
    var playerFails: Throwable? = null
    var openingThePlayerFails: Throwable? = null
    var closingThePlayerFails: Throwable? = null
    var whileClosing: () -> Unit = { }

    // Runs after the clock was read and before its value is handed back, as a thread that is preempted there
    var afterReadingTheClock: () -> Unit = { }

    // How many samples of what a player played it has not made audible yet, as the audio line holds some
    var playerLag = 0

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
            openingThePlayerFails?.let { throw it }
            StandInPlayer(onFirstWrite).also { players += it }
        },
        reportFirstSound = { milliseconds, after -> firstSounds += FirstSound(milliseconds, after) },
        reportFailure = { target, failure -> failures += target to failure },
        onChange = {
            changes.incrementAndGet()
            whenChanged()
        },
        currentSpeed = { speed },
        nowMillis = {
            val now = clockMillis
            afterReadingTheClock()
            now
        },
        runLater = { delayMillis, work -> laterWorks += LaterWork(clockMillis + delayMillis, work) },
    )

    // Hands the delayed works over to the test, which runs them where it wants, and leaves none to passTime
    fun takeLaterWorks(): List<() -> Unit> = synchronized(laterWorks) {
        laterWorks.map { it.work }.also { laterWorks.clear() }
    }

    // Runs the delayed works that come due, in the order of their time, each with the clock at its time
    fun passTime(milliseconds: Long) {
        val untilMillis = clockMillis + milliseconds
        while (true) {
            val next = synchronized(laterWorks) {
                laterWorks.filter { it.dueMillis <= untilMillis }.minByOrNull { it.dueMillis }?.also { laterWorks.remove(it) }
            } ?: break
            clockMillis = maxOf(clockMillis, next.dueMillis)
            next.work()
        }
        clockMillis = untilMillis
    }

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

        override fun speak(text: String, speed: Float, onSamples: (FloatArray) -> Boolean) {
            textsMade += text
            speedsMade += speed
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

        override fun samplesHeard(): Long = maxOf(0, played.sumOf { it.size } - playerLag).toLong()

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
