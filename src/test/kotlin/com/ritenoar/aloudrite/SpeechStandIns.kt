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

data class ReadingTimeReport<Target : Any>(val target: Target, val firstEstimateSeconds: Double?, val tookMillis: Long)

// A ReadingAloud without the model, without an audio device and without real time. Its background works are kept, in
// the order they were handed over, until the test runs them, and its delayed works run when the test passes the time
class SpeechStandIns<Target : Any> {

    private class LaterWork(val dueMillis: Long, val work: () -> Unit)

    val keptWorks: MutableList<() -> Unit> = Collections.synchronizedList(mutableListOf())
    val engines = mutableListOf<StandInEngine>()
    val players: MutableList<StandInPlayer> = CopyOnWriteArrayList()
    // The model of each engine made, in order, and the model in use that the service asks for
    val modelsMade = mutableListOf<SpeechModel>()
    val firstSounds: MutableList<FirstSound> = Collections.synchronizedList(mutableListOf())
    val readingTimeReports: MutableList<ReadingTimeReport<Target>> = Collections.synchronizedList(mutableListOf())
    val failures: MutableList<Pair<Target, Throwable>> = Collections.synchronizedList(mutableListOf())
    val changes = AtomicInteger()

    // Every text the engines were given, in order, also across engines, and the speed it was given with
    val textsMade: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val speedsMade: MutableList<Float> = Collections.synchronizedList(mutableListOf())

    // The voice each of those texts was given with
    val voicesMade: MutableList<Voice> = Collections.synchronizedList(mutableListOf())

    // The speed of the setting, which a reading takes when it starts
    var speed: ReadingSpeed = ReadingSpeed.NORMAL

    // The voice of the setting, which a reading takes when it starts
    var voice: Voice = DEFAULT_VOICE

    var modelInUse: SpeechModel = INT8_MODEL

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

    // The works of the aligner's thread are kept, as the background works are, until the test runs them, so that it decides
    // whether an alignment is ready before a piece plays, while it plays or never
    val keptAlignmentWorks: MutableList<() -> Unit> = Collections.synchronizedList(mutableListOf())
    var alignerModelInPlace = true
    var aligningFails: Throwable? = null
    val loggedAlignmentFailures: MutableList<Throwable> = Collections.synchronizedList(mutableListOf())
    val alignedWords: MutableList<List<String>> = Collections.synchronizedList(mutableListOf())

    // What the aligner answers for the words it is given, and the samples of the piece. By default each word of a piece
    // takes an equal part of it, which is not the part the estimate gives
    var alignerAnswer: (words: List<String>, samples: FloatArray) -> List<AlignedInterval> = { words, samples ->
        val seconds = samples.size.toDouble() / STAND_IN_SAMPLE_RATE
        words.mapIndexed { number, word ->
            AlignedInterval(seconds * number / words.size, seconds * (number + 1) / words.size, word)
        }
    }

    // The delay of the word that a reading takes when it starts
    var wordDelayMilliseconds = 0

    // Not published by default, since some tests use the stand-ins without the application, and so without its bus
    var whenChanged: () -> Unit = { }

    val reading = ReadingAloud<Target>(
        runInBackground = { work -> keptWorks += work },
        createEngine = { model ->
            // The service keeps at most one engine, so a second one is made only after the first was closed
            check(engines.all { it.closed }) { "An engine was made while another one was not closed" }
            modelsMade.add(model)
            makingTheEngineFails?.let { throw it }
            StandInEngine().also { engines += it }
        },
        createPlayer = { sampleRate, onFirstWrite ->
            check(sampleRate == STAND_IN_SAMPLE_RATE) { "The player was made at $sampleRate" }
            openingThePlayerFails?.let { throw it }
            StandInPlayer(onFirstWrite).also { players += it }
        },
        reportFirstSound = { milliseconds, after -> firstSounds += FirstSound(milliseconds, after) },
        reportReadingTime = { target, firstEstimateSeconds, tookMillis ->
            readingTimeReports += ReadingTimeReport(target, firstEstimateSeconds, tookMillis)
        },
        reportFailure = { target, failure -> failures += target to failure },
        onChange = {
            changes.incrementAndGet()
            whenChanged()
        },
        currentSpeed = { speed },
        currentVoice = { voice },
        modelInUse = { modelInUse },
        nowMillis = {
            val now = clockMillis
            afterReadingTheClock()
            now
        },
        runLater = { delayMillis, work -> laterWorks += LaterWork(clockMillis + delayMillis, work) },
        pieceAligning = PieceAligning(
            isModelInPlace = { alignerModelInPlace },
            createAligner = {
                object : WordAligner {
                    override fun align(samples: FloatArray, sampleRate: Int, words: List<String>): List<AlignedInterval> {
                        alignedWords += words
                        aligningFails?.let { throw it }
                        return alignerAnswer(words, samples)
                    }

                    override fun close() = Unit
                }
            },
            runOnItsThread = { work -> keptAlignmentWorks += work },
            logFailure = { loggedAlignmentFailures += it },
        ),
        wordDelayMillis = { wordDelayMilliseconds },
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

    fun runKeptAlignmentWorks() {
        while (keptAlignmentWorks.isNotEmpty()) keptAlignmentWorks.removeAt(0)()
    }

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

        override fun speak(text: String, speed: Float, voice: Voice, onSamples: (FloatArray) -> Boolean) {
            textsMade += text
            speedsMade += speed
            voicesMade += voice
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

// What a reading of a whole file is given. The words are placed as if the file held exactly them, each character at its
// own offset and each words of a block of its own
fun wholeFile(parts: List<SpokenPart>) = placedReading(parts, PartOfTheFile.WHOLE_FILE)

fun placedReading(parts: List<SpokenPart>, partOfTheFile: PartOfTheFile, documentStamp: Long? = null): WhatToRead {
    var offset = 0
    val placed = parts.mapIndexed { block, part ->
        when (part) {
            is Words -> PlacedWords(part.text, List(part.text.length) { offset + it }, block).also { offset += part.text.length }
            is Silence -> PlacedSilence(part.milliseconds)
        }
    }
    return WhatToRead(placed, partOfTheFile, documentStamp)
}

// What a press of the whole file reads, as the action makes it from the Markdown
fun placedWhatToRead(markdown: String, documentStamp: Long? = null) =
    whatToRead(markdown, PlaceInTheText.NoText, documentStamp)
