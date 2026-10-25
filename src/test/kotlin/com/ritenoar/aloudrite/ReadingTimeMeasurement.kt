package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.Executors

// Measures, for unit 0022, how close the estimate of the whole reading that the tool window shows is to the length of the
// whole audio. For each of two samples, each model and each of three speeds, it makes the audio of every segment with the
// real engine, as a reading does but without a player, and then drives a ReadingTime as a reading would: after the first
// segment of words, and at a quarter, a half and three quarters of the audio, with every segment made that begins before
// that point and SECONDS_AHEAD seconds more, which is how far the engine runs ahead of the playing. Runs only where
// ALOUDRITE_MEASURE_READING_TIME is set, on a platform with speech, and with both models in place, as
// ALOUDRITE_MEASURE_READING_TIME=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.ReadingTimeMeasurement
// and writes its table to build/reading-time.txt. The setups run side by side, each with its own engine, which changes
// how long the run takes and nothing it measures
class ReadingTimeMeasurement {

    private class Point(val name: String, val wholeSeconds: Double, val totalSeconds: Double) {
        val errorSeconds: Double get() = wholeSeconds - totalSeconds
        val errorPercent: Double get() = errorSeconds / totalSeconds * PERCENT
    }

    private class Row(val file: String, val model: SpeechModel, val speed: ReadingSpeed, val totalSeconds: Double, val points: List<Point>)

    // The audio of one segment: its samples, and the seconds they take at the sample rate
    private class MadeSegment(val samples: Long, val seconds: Double)

    private class Setup(val model: SpeechModel, val speed: ReadingSpeed)

    @Test
    fun measureTheEstimateOfTheWholeReadingAgainstTheLengthOfTheAudio() {
        assumeTrue("ALOUDRITE_MEASURE_READING_TIME is not set", System.getenv("ALOUDRITE_MEASURE_READING_TIME") != null)
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val modelsFolder = userModelsFolder()
        for (model in SPEECH_MODELS) {
            assumeTrue("The model ${model.packageName} is not in $modelsFolder", model.isInPlace(model.folderIn(modelsFolder)))
        }
        val setups = SPEECH_MODELS.flatMap { model -> SPEEDS.map { Setup(model, it) } }
        val pool = Executors.newFixedThreadPool(setups.size)
        val rows = try {
            pool.invokeAll(setups.map { setup -> Callable { measure(setup, modelsFolder) } }).flatMap { it.get() }
        } finally {
            pool.shutdown()
        }

        val table = table(rows.sortedWith(compareBy({ it.file }, { it.model.packageName }, { it.speed.hundredths })))
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/reading-time.txt"), table)
        assertEquals(setups.size * FILES.size, rows.size)
    }

    // One engine for the setup, new as the one of a reading in a newly started IDE is, and the files one after the other
    private fun measure(setup: Setup, modelsFolder: Path): List<Row> {
        val threads = engineThreadCount(Runtime.getRuntime().availableProcessors())
        return KokoroSpeechEngine(setup.model.folderIn(modelsFolder), setup.model.modelFileName, threads, debugOutput = false).use { engine ->
            FILES.map { file ->
                val segments = spokenPartsOf(Files.readString(Paths.get("samples/$file"))).flatMap(::segmentsOf)
                val made = segments.map { makeTheAudioOf(it, engine, setup.speed) }
                Row(file, setup.model, setup.speed, made.sumOf { it.seconds }, pointsOf(segments, made, engine.sampleRate, setup.speed))
            }
        }
    }

    private fun makeTheAudioOf(segment: SpokenPart, engine: SpeechEngine, speed: ReadingSpeed): MadeSegment {
        val samples = when (segment) {
            is Words -> {
                var count = 0L
                engine.speak(segment.text, speed.forTheEngine) { made ->
                    count += made.size
                    true
                }
                count
            }
            is Silence -> (engine.sampleRate * speed.silenceMilliseconds(segment.milliseconds) / MILLISECONDS_PER_SECOND).toLong()
        }
        return MadeSegment(samples, samples.toDouble() / engine.sampleRate)
    }

    private fun pointsOf(segments: List<SpokenPart>, made: List<MadeSegment>, sampleRate: Int, speed: ReadingSpeed): List<Point> {
        val total = made.sumOf { it.seconds }
        val starts = made.runningFold(0.0) { start, segment -> start + segment.seconds }
        val afterTheFirstPiece = ReadingTime(segments, sampleRate, speed).apply {
            val first = segments.indexOfFirst { it is Words }
            audioMade(first, made[first].samples.toInt())
            segmentMade(first)
        }
        val atQuarters = QUARTERS.map { quarter ->
            val heardSeconds = total * quarter / QUARTERS_IN_ALL
            val reading = ReadingTime(segments, sampleRate, speed)
            for (number in segments.indices) {
                if (starts[number] >= heardSeconds + SECONDS_AHEAD) break
                reading.audioMade(number, made[number].samples.toInt())
                reading.segmentMade(number)
            }
            val heardSegment = (starts.indexOfLast { it <= heardSeconds }).coerceAtMost(segments.lastIndex)
            reading.heard(heardSegment, ((heardSeconds - starts[heardSegment]) * sampleRate).toLong())
            Point("$quarter/$QUARTERS_IN_ALL", reading.times().wholeSeconds, total)
        }
        return listOf(Point("first piece", afterTheFirstPiece.times().wholeSeconds, total)) + atQuarters
    }

    private fun table(rows: List<Row>): String = buildString {
        appendLine("processors ${Runtime.getRuntime().availableProcessors()}, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
        appendLine("estimate of the whole reading after the first piece and when 1/4, 2/4 and 3/4 of the audio have played")
        appendLine("error is the estimate minus the length of the audio, in seconds and in percent of that length")
        appendLine()
        appendLine("%-24s %-12s %-6s %10s  %-12s %10s %9s %8s".format("file", "model", "speed", "audio s", "at", "estimate s", "error s", "error %"))
        for (row in rows) {
            for (point in row.points) {
                appendLine("%-24s %-12s %-6s %10.1f  %-12s %10.1f %9.1f %8.1f".format(
                    row.file, row.model.displayName.removePrefix("Kokoro 1.0, "), row.speed.display, row.totalSeconds,
                    point.name, point.wholeSeconds, point.errorSeconds, point.errorPercent))
            }
        }
    }

    private companion object {
        val SPEEDS = listOf("1.0", "1.5", "2.0").map { checkNotNull(parsedSpeed(it)) }
        val FILES = listOf("markdown-features.md", "ten-thousand-words.md")
        val QUARTERS = listOf(1, 2, 3)
        const val QUARTERS_IN_ALL = 4
        const val PERCENT = 100.0
        const val MILLISECONDS_PER_SECOND = 1_000
    }
}
