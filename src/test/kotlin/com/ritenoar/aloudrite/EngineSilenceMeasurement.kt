package com.ritenoar.aloudrite

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.abs

// Measures, for unit 0016, whether the engine makes any piece of samples/markdown-features.md as silence or as samples
// that are not a number, at 1.0, 1.5 and 2.0. Runs only where ALOUDRITE_MEASURE_ENGINE_SILENCE is set and the model is
// in its folder, as
// ALOUDRITE_MEASURE_ENGINE_SILENCE=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineSilenceMeasurement
// and writes its table to build/engine-silence.txt
class EngineSilenceMeasurement {

    private class PieceMeasurement(val text: String, val samples: Int, val notANumber: Int, val silent: Int) {
        val isMostlySilence: Boolean get() = samples == 0 || (notANumber + silent) * 10 >= samples * 9
    }

    @Test
    fun measureTheSilenceOfEveryPieceAtThreeSpeeds() {
        assumeTrue("ALOUDRITE_MEASURE_ENGINE_SILENCE is not set", System.getenv("ALOUDRITE_MEASURE_ENGINE_SILENCE") != null)
        val operatingSystemName = System.getProperty("os.name")
        assumeTrue(isSpeechSupported(operatingSystemName, System.getProperty("os.arch")))
        val modelFolder = userModelFolder(operatingSystemName, System.getProperty("user.home"), System.getenv("XDG_DATA_HOME"))
        assumeTrue("The model is not in $modelFolder", isModelInPlace(modelFolder))
        val pieces = spokenPartsOf(Files.readString(Paths.get("samples/markdown-features.md")))
            .filterIsInstance<Words>()
            .flatMap { piecesOf(it.text) }
            .filterNot { it.isBlank() }

        // A new engine for each speed, as a reading in a newly started IDE has, and the pieces in the order they are read
        val measurements = SPEEDS.associateWith { speed ->
            KokoroSpeechEngine(modelFolder, engineThreadCount(Runtime.getRuntime().availableProcessors())).use { engine ->
                pieces.map { piece -> measure(engine, piece, speed) }
            }
        }

        val table = table(measurements)
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/engine-silence.txt"), table)
        assertTrue(measurements.values.all { it.size == pieces.size })
    }

    private fun measure(engine: SpeechEngine, piece: String, speed: ReadingSpeed): PieceMeasurement {
        var samples = 0
        var notANumber = 0
        var silent = 0
        engine.speak(piece, speed.forTheEngine) { made ->
            samples += made.size
            notANumber += made.count { it.isNaN() }
            silent += made.count { !it.isNaN() && abs(it) < SILENT_BELOW }
            true
        }
        return PieceMeasurement(piece, samples, notANumber, silent)
    }

    private fun table(measurements: Map<ReadingSpeed, List<PieceMeasurement>>): String = buildString {
        appendLine("processors ${Runtime.getRuntime().availableProcessors()}, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
        for ((speed, pieces) in measurements) {
            val mostlySilence = pieces.count { it.isMostlySilence }
            appendLine()
            appendLine("speed ${speed.display}: ${pieces.size} pieces, $mostlySilence of them mostly silence or not a number")
            appendLine("piece  samples  not a number  silent  text")
            pieces.forEachIndexed { index, piece ->
                val mark = if (piece.isMostlySilence) "  <-- mostly silence" else ""
                appendLine("%5d  %7d  %12d  %6d  %s%s".format(
                    index, piece.samples, piece.notANumber, piece.silent, shown(piece.text), mark))
            }
        }
    }

    private fun shown(text: String): String {
        val oneLine = text.replace("\n", "\\n")
        return if (oneLine.length <= SHOWN_CHARACTERS) oneLine else oneLine.take(SHOWN_CHARACTERS) + "..."
    }

    private companion object {
        val SPEEDS = listOf("1.0", "1.5", "2.0").map { checkNotNull(parsedSpeed(it)) }
        const val SILENT_BELOW = 0.0001f
        const val SHOWN_CHARACTERS = 50
    }
}
