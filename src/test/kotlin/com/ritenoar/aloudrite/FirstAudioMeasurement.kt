package com.ritenoar.aloudrite

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// Measures, for unit 0030, the milliseconds from the start of a reading to its first audio with the fp32 model: from the
// creation of an engine to the first call of its callback, and from the call of speak to that call with an engine made
// beforehand, for the first piece of words of samples/markdown-features.md. Runs only where ALOUDRITE_MEASURE_FIRST_AUDIO
// is set and the model is in its folder, as
// ALOUDRITE_MEASURE_FIRST_AUDIO=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.FirstAudioMeasurement
// and writes its table to build/first-audio.txt
class FirstAudioMeasurement {

    @Test
    fun measureTheFirstAudioWithTheCreationOfTheEngineAndWithoutIt() {
        assumeTrue("ALOUDRITE_MEASURE_FIRST_AUDIO is not set", System.getenv("ALOUDRITE_MEASURE_FIRST_AUDIO") != null)
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val fp32Model = SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }
        val modelFolder = fp32Model.folderIn(userModelsFolder())
        assumeTrue("The model is not in $modelFolder", fp32Model.isInPlace(modelFolder))
        val threads = engineThreadCount(Runtime.getRuntime().availableProcessors())
        val piece = spokenPartsOf(Files.readString(Paths.get("samples/markdown-features.md")))
            .filterIsInstance<Words>()
            .flatMap { piecesOf(it.text) }
            .first { it.isNotBlank() }

        val firstCreation = millisecondsToFirstAudio(fp32Model, modelFolder, threads, piece, engineMadeBeforehand = null)
        val withCreation = List(RUNS) { millisecondsToFirstAudio(fp32Model, modelFolder, threads, piece, engineMadeBeforehand = null) }
        val withoutCreation = KokoroSpeechEngine(modelFolder, fp32Model.modelFileName, threads, debugOutput = false).use { engine ->
            List(RUNS) { millisecondsToFirstAudio(fp32Model, modelFolder, threads, piece, engine) }
        }

        val table = buildString {
            appendLine("model ${fp32Model.modelFileName}, threads $threads, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
            appendLine("piece: ${piece.replace("\n", "\\n")}")
            appendLine("first creation in the process: $firstCreation ms")
            appendLine("creation and first audio: $withCreation ms, median ${withCreation.sorted()[RUNS / 2]} ms")
            appendLine("first audio of an engine made beforehand: $withoutCreation ms, median ${withoutCreation.sorted()[RUNS / 2]} ms")
        }
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/first-audio.txt"), table)
        assertTrue(withCreation.size == RUNS && withoutCreation.size == RUNS)
    }

    // With an engine made beforehand, the time is that of speak alone, and without one, that of the creation as well
    private fun millisecondsToFirstAudio(fp32Model: SpeechModel, modelFolder: Path, threads: Int, piece: String, engineMadeBeforehand: SpeechEngine?): Long {
        val start = System.nanoTime()
        val engine = engineMadeBeforehand ?: KokoroSpeechEngine(modelFolder, fp32Model.modelFileName, threads, debugOutput = false)
        var firstAudioAt = 0L
        try {
            engine.speak(piece, ReadingSpeed.NORMAL.forTheEngine) {
                if (firstAudioAt == 0L) firstAudioAt = System.nanoTime()
                false
            }
        } finally {
            if (engineMadeBeforehand == null) engine.close()
        }
        return (firstAudioAt - start) / 1_000_000
    }

    private companion object {
        const val RUNS = 5
    }
}
