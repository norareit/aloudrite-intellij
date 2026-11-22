package com.ritenoar.aloudrite.core

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Measures, for unit 0030, the milliseconds from the start of a reading to its first audio with the fp32 model: from the
// creation of an engine to the first call of its callback, and from the call of speak to that call with an engine made
// beforehand, for the first piece of words of samples/markdown-features.md. Runs only where ALOUDRITE_MEASURE_FIRST_AUDIO
// is set and the model is in its folder, as
// ALOUDRITE_MEASURE_FIRST_AUDIO=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.FirstAudioMeasurement
// and writes its table to build/first-audio.txt. For unit 0028 it also measures, from the call of read of the service with
// the real engine and a player that only notes its first write, to that first write, with the real aligner and with an
// aligner model that is never in place, which is what the aligner adds to the start of a reading. It then also needs the
// aligner model in its folder
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

    @Test
    fun measureTheFirstWriteOfAReadingWithTheAlignerAndWithoutIt() {
        assumeTrue("ALOUDRITE_MEASURE_FIRST_AUDIO is not set", System.getenv("ALOUDRITE_MEASURE_FIRST_AUDIO") != null)
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val fp32Model = SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }
        assumeTrue("The model is not in its folder", fp32Model.isInPlace(fp32Model.folderIn(userModelsFolder())))
        val alignerFolder = ALIGNER_MODEL.folderIn(userModelsFolder())
        assumeTrue("The aligner model is not in $alignerFolder", ALIGNER_MODEL.isInPlace(alignerFolder))
        val markdown = Files.readString(Paths.get("samples/markdown-features.md"))

        val alignmentThread = Executors.newSingleThreadExecutor()
        val withTheAligner = List(RUNS) {
            val realAligning = PieceAligning(
                { true },
                { ForcedWordAligner(alignerFolder) },
                { work -> alignmentThread.execute(work) },
                { failure -> throw failure },
            )
            millisecondsToTheFirstWrite(fp32Model, markdown, realAligning)
        }
        alignmentThread.shutdown()
        val withoutTheAligner = List(RUNS) {
            millisecondsToTheFirstWrite(fp32Model, markdown, PieceAligning({ false }, { error("never made") }, { it() }, { }))
        }

        val table = buildString {
            appendLine("read to first write through the service, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}")
            appendLine("with the aligner: $withTheAligner ms, median ${withTheAligner.sorted()[RUNS / 2]} ms")
            appendLine("with an aligner model never in place: $withoutTheAligner ms, median ${withoutTheAligner.sorted()[RUNS / 2]} ms")
        }
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/first-write.txt"), table)
        assertTrue(withTheAligner.size == RUNS && withoutTheAligner.size == RUNS)
    }

    // The player only notes the moment of its first write, after which the reading is stopped and its works are waited for
    private fun millisecondsToTheFirstWrite(fp32Model: SpeechModel, markdown: String, pieceAligning: PieceAligning): Long {
        val firstWrite = CountDownLatch(1)
        var firstWriteAt = 0L
        val background = Executors.newCachedThreadPool()
        val threads = engineThreadCount(Runtime.getRuntime().availableProcessors())
        val service = ReadingAloud<String>(
            runInBackground = { work -> background.execute(work) },
            createEngine = { model ->
                KokoroSpeechEngine(model.folderIn(userModelsFolder()), model.modelFileName, threads, debugOutput = false)
            },
            createPlayer = { _, _ ->
                object : SamplePlayer {
                    override fun play(samples: FloatArray) {
                        if (firstWrite.count > 0) {
                            firstWriteAt = System.nanoTime()
                            firstWrite.countDown()
                        }
                    }

                    override fun samplesHeard(): Long = 0

                    override fun close() = Unit

                    override fun discard() = Unit
                }
            },
            reportFirstSound = { _, _ -> },
            reportReadingTime = { _, _, _ -> },
            reportFailure = { _, failure -> throw failure },
            onChange = { },
            currentSpeed = { ReadingSpeed.NORMAL },
            currentVoice = { DEFAULT_VOICE },
            modelInUse = { fp32Model },
            nowMillis = { System.nanoTime() / 1_000_000 },
            runLater = { _, work -> work() },
            pieceAligning = pieceAligning,
            wordDelayMillis = { WORD_DELAY_MILLISECONDS },
        )
        val target = "measured.md"
        val start = System.nanoTime()
        service.read(target, placedWhatToRead(markdown), fp32Model, start)
        assertTrue("No write within a minute", firstWrite.await(1, TimeUnit.MINUTES))
        service.stop(target)
        background.shutdown()
        background.awaitTermination(1, TimeUnit.MINUTES)
        service.close()
        return (firstWriteAt - start) / 1_000_000
    }

    // With an engine made beforehand, the time is that of speak alone, and without one, that of the creation as well
    private fun millisecondsToFirstAudio(fp32Model: SpeechModel, modelFolder: Path, threads: Int, piece: String, engineMadeBeforehand: SpeechEngine?): Long {
        val start = System.nanoTime()
        val engine = engineMadeBeforehand ?: KokoroSpeechEngine(modelFolder, fp32Model.modelFileName, threads, debugOutput = false)
        var firstAudioAt = 0L
        try {
            engine.speak(piece, ReadingSpeed.NORMAL.forTheEngine, DEFAULT_VOICE) {
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
