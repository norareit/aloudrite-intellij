package com.ritenoar.aloudrite

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

// Measures the engine at 1 to 8 threads for requirement 5 of unit 0011, which settles the cap of the thread count. Runs
// only where ALOUDRITE_MEASURE_ENGINE_THREADS is set and the model is in its folder, as
// ALOUDRITE_MEASURE_ENGINE_THREADS=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineThreadsMeasurement
// and writes its table to build/engine-threads.txt
class EngineThreadsMeasurement {

    private class Measurement(val threads: Int, val firstAudioMilliseconds: Long, val audioSeconds: Double, val audioPerWorkSecond: Double)

    @Test
    fun measureOneToEightThreads() {
        assumeTrue("ALOUDRITE_MEASURE_ENGINE_THREADS is not set", System.getenv("ALOUDRITE_MEASURE_ENGINE_THREADS") != null)
        val operatingSystemName = System.getProperty("os.name")
        assumeTrue(isSpeechSupported(operatingSystemName, System.getProperty("os.arch")))
        val modelFolder = userModelFolder(operatingSystemName, System.getProperty("user.home"), System.getenv("XDG_DATA_HOME"))
        assumeTrue("The model is not in $modelFolder", isModelInPlace(modelFolder))
        val sentence = Files.readString(Paths.get("samples/one-sentence.md")).trim()
        val passage = Files.readString(Paths.get("samples/ten-thousand-words.md")).take(PASSAGE_CHARACTERS)

        val measurements = THREAD_COUNTS.map { threads ->
            KokoroSpeechEngine(modelFolder, threads).use { engine ->
                engine.speak(sentence, ReadingSpeed.NORMAL.forTheEngine) { false }
                Measurement(
                    threads,
                    (1..FIRST_AUDIO_RUNS).map { firstAudioMilliseconds(engine, sentence) }.sorted()[FIRST_AUDIO_RUNS / 2],
                    0.0, 0.0,
                ).withPassage(engine, passage)
            }
        }

        val table = table(measurements)
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/engine-threads.txt"), table)
        assertTrue(measurements.all { it.firstAudioMilliseconds > 0 && it.audioSeconds > 0.0 })
    }

    private fun firstAudioMilliseconds(engine: SpeechEngine, sentence: String): Long {
        val startedAtNanos = System.nanoTime()
        var firstAudioAtNanos = 0L
        engine.speak(sentence, ReadingSpeed.NORMAL.forTheEngine) {
            firstAudioAtNanos = System.nanoTime()
            false
        }
        return (firstAudioAtNanos - startedAtNanos) / NANOSECONDS_PER_MILLISECOND
    }

    private fun Measurement.withPassage(engine: SpeechEngine, passage: String): Measurement {
        var sampleCount = 0L
        val startedAtNanos = System.nanoTime()
        for (piece in piecesOf(passage).filter { it.isNotBlank() }) {
            engine.speak(piece, ReadingSpeed.NORMAL.forTheEngine) { samples ->
                sampleCount += samples.size
                true
            }
        }
        val workSeconds = (System.nanoTime() - startedAtNanos) / NANOSECONDS_PER_SECOND
        val audioSeconds = sampleCount.toDouble() / engine.sampleRate
        return Measurement(threads, firstAudioMilliseconds, audioSeconds, audioSeconds / workSeconds)
    }

    private fun table(measurements: List<Measurement>): String = buildString {
        appendLine("processors ${Runtime.getRuntime().availableProcessors()}, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
        appendLine("threads  first audio (ms)  audio (s)  audio per second of work")
        for (measurement in measurements) {
            appendLine("%7d  %16d  %9.2f  %24.2f".format(
                measurement.threads, measurement.firstAudioMilliseconds, measurement.audioSeconds, measurement.audioPerWorkSecond))
        }
    }

    private companion object {
        val THREAD_COUNTS = listOf(1, 2, 4, 6, 8)
        const val FIRST_AUDIO_RUNS = 5
        const val PASSAGE_CHARACTERS = 3_000
        const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
        const val NANOSECONDS_PER_SECOND = 1_000_000_000.0
    }
}
