package com.ritenoar.aloudrite

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.abs

// Measures, for unit 0016, whether the engine makes any piece of samples/markdown-features.md as silence or as samples
// that are not a number, at 1.0, 1.5 and 2.0, with the threads the plugin uses and with one thread, and whether a piece
// made so is made so again on a second try. Runs only where ALOUDRITE_MEASURE_ENGINE_SILENCE is set and the model is in
// its folder, as
// ALOUDRITE_MEASURE_ENGINE_SILENCE=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineSilenceMeasurement
// and writes its table to build/engine-silence.txt. ALOUDRITE_MEASURE_MODEL_FOLDER and ALOUDRITE_MEASURE_MODEL_FILE name
// another model to measure, such as the fp32 model.onnx of kokoro-multi-lang-v1_0, in place of the plugin's own
class EngineSilenceMeasurement {

    private class PieceMeasurement(val text: String, val samples: Int, val notANumber: Int, val silent: Int) {
        val isMostlySilence: Boolean get() = samples == 0 || (notANumber + silent) * 10 >= samples * 9
    }

    // A piece made as mostly silence is made a second time with the same engine, and that try is kept beside the first
    private class Tried(val first: PieceMeasurement, val second: PieceMeasurement?)

    private class Setup(val threads: Int, val speed: ReadingSpeed)

    @Test
    fun measureTheSilenceOfEveryPieceAtThreeSpeedsAndTwoThreadCounts() {
        assumeTrue("ALOUDRITE_MEASURE_ENGINE_SILENCE is not set", System.getenv("ALOUDRITE_MEASURE_ENGINE_SILENCE") != null)
        val operatingSystemName = System.getProperty("os.name")
        assumeTrue(isSpeechSupported(operatingSystemName, System.getProperty("os.arch")))
        val modelFolder = System.getenv("ALOUDRITE_MEASURE_MODEL_FOLDER")?.let { Paths.get(it) }
            ?: INT8_MODEL.folderIn(userModelsFolder())
        val modelFileName = System.getenv("ALOUDRITE_MEASURE_MODEL_FILE") ?: INT8_MODEL.modelFileName
        assumeTrue("The model is not in $modelFolder", Files.isRegularFile(modelFolder.resolve(modelFileName)))
        val pieces = spokenPartsOf(Files.readString(Paths.get("samples/markdown-features.md")))
            .filterIsInstance<Words>()
            .flatMap { piecesOf(it.text) }
            .filterNot { it.isBlank() }
        val setups = listOf(engineThreadCount(Runtime.getRuntime().availableProcessors()), 1)
            .distinct()
            .flatMap { threads -> SPEEDS.map { Setup(threads, it) } }

        // A new engine for each setup, as a reading in a newly started IDE has, and the pieces in the order they are read
        val measurements = setups.associateWith { setup ->
            KokoroSpeechEngine(modelFolder, modelFileName, setup.threads, debugOutput = false).use { engine ->
                pieces.map { piece ->
                    val first = measure(engine, piece, setup.speed)
                    Tried(first, if (first.isMostlySilence) measure(engine, piece, setup.speed) else null)
                }
            }
        }

        val table = "model ${modelFolder.resolve(modelFileName)}\n" + table(measurements)
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

    private fun table(measurements: Map<Setup, List<Tried>>): String = buildString {
        appendLine("processors ${Runtime.getRuntime().availableProcessors()}, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
        for ((setup, pieces) in measurements) {
            val mostlySilence = pieces.count { it.first.isMostlySilence }
            val again = pieces.count { it.second?.isMostlySilence == true }
            appendLine()
            appendLine("threads ${setup.threads}, speed ${setup.speed.display}: ${pieces.size} pieces, $mostlySilence of them " +
                "mostly silence or not a number, $again of those again on a second try")
            appendLine("piece  samples  not a number  silent  second try  text")
            pieces.forEachIndexed { index, tried ->
                val piece = tried.first
                val secondTry = when (tried.second?.isMostlySilence) {
                    null -> "-"
                    true -> "silence"
                    false -> "sound"
                }
                val mark = if (piece.isMostlySilence) "  <-- mostly silence" else ""
                appendLine("%5d  %7d  %12d  %6d  %10s  %s%s".format(
                    index, piece.samples, piece.notANumber, piece.silent, secondTry, shown(piece.text), mark))
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
