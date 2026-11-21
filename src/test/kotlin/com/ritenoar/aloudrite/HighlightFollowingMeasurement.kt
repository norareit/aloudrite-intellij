package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// Measures, for unit 0032, how often the word that the highlight shows is the word that is heard, for every piece of words of
// samples/markdown-features.md read from its start, in Heart with the fp32 model at 1.0 and 1.5. The word shown is that of the
// estimate of the plugin, run as a reading runs it, and the word heard is the one whose phonemes the model says at the
// moment, by the durations that the Kokoro Python package gives for the same phonemes, the voice and the speed, which
// scripts/kokoro-durations.py reads. Only the moments within a word count. Runs only where ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING
// is set, on a platform with speech, with the fp32 model in its folder and with kokoro-env and Kokoro-82M in the working
// copy, see README.md, as
// ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.HighlightFollowingMeasurement
// and writes its table to build/highlight-following.txt
class HighlightFollowingMeasurement {

    // The phonemes of a piece and the samples the engine makes for it, or the reason there are none
    private class MadePiece(val number: Int, val text: String, val rows: List<IntArray>, val engineSamples: Long)

    @Test
    fun measureHowOftenTheWordShownIsTheWordHeard() {
        assumeTrue("ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING is not set", System.getenv("ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING") != null)
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val model = SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }
        val modelFolder = model.folderIn(userModelsFolder())
        assumeTrue("The model ${model.packageName} is not in $modelFolder", model.isInPlace(modelFolder))
        val python = Paths.get("kokoro-env/bin/python")
        val weights = Paths.get("Kokoro-82M")
        for (needed in listOf(python, weights.resolve("config.json"), weights.resolve("kokoro-v1_0.pth"), weights.resolve("voices/$VOICE_NAME.pt"))) {
            assumeTrue("$needed is missing, see README.md", Files.isRegularFile(needed))
        }
        val voice = checkNotNull(voiceNamed(VOICE_NAME))
        val speeds = SPEED_TEXTS.map { checkNotNull(parsedSpeed(it)) }
        val segments = ReadingSegments(placedWhatToRead(Files.readString(Paths.get("samples/markdown-features.md"))).placedParts).segments

        val threads = engineThreadCount(Runtime.getRuntime().availableProcessors())
        val results = KokoroSpeechEngine(modelFolder, model.modelFileName, threads, debugOutput = false).use { engine ->
            assertEquals("The durations are in frames at the sample rate of Kokoro", DURATIONS_SAMPLE_RATE, engine.sampleRate)
            val phonemeRows = PhonemeRows(tokenCodePointsOf(Files.readAllLines(modelFolder.resolve(TOKENS_FILE_NAME))), EspeakPhonemes::sentencesOf)
            val madeAtSpeed = speeds.map { speed -> madePiecesOf(segments, engine, phonemeRows, voice, speed) }
            val durations = durationsOf(speeds, madeAtSpeed, python, weights)
            speeds.indices.map { number -> SpeedResult(speeds[number].display, follow(segments, madeAtSpeed[number], durations[number], engine.sampleRate, speeds[number])) }
        }

        val table = "model ${modelFolder.resolve(model.modelFileName)}, voice $VOICE_NAME, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}\n\n" + tableOf(results)
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/highlight-following.txt"), table)
        val piecesOfWords = segments.count { it.words.isNotEmpty() }
        for (result in results) assertEquals(result.speed, piecesOfWords, result.pieces.size)
    }

    private fun madePiecesOf(segments: List<ReadingSegment>, engine: KokoroSpeechEngine, phonemeRows: PhonemeRows, voice: Voice, speed: ReadingSpeed): List<MadePiece> =
        segments.withIndex().filter { it.value.words.isNotEmpty() }.map { (number, segment) ->
            val text = (segment.spoken as Words).text
            var samples = 0L
            engine.speak(text, speed.forTheEngine, voice) { made ->
                samples += made.size
                true
            }
            MadePiece(number, text, phonemeRows.rowsOf(text, voice.accent.espeakLanguage), samples)
        }

    // The durations of every row of every speed, in the order of the pieces, from one run of the script. A row of another
    // number of durations than its tokens and the two padding tokens means that the vocabularies differ, and fails the run
    private fun durationsOf(speeds: List<ReadingSpeed>, madeAtSpeed: List<List<MadePiece>>, python: Path, weights: Path): List<List<List<IntArray>>> {
        val rowsFile = Paths.get("build/highlight-following-rows.txt")
        Files.createDirectories(rowsFile.parent)
        val rowLines = speeds.indices.flatMap { number ->
            madeAtSpeed[number].flatMap { piece -> piece.rows.map { row -> "${speeds[number].forTheEngine}\t${String(row, 0, row.size)}" } }
        }
        Files.write(rowsFile, rowLines)
        val process = ProcessBuilder(python.toString(), "scripts/kokoro-durations.py", weights.toString(), VOICE_NAME)
            .redirectInput(rowsFile.toFile())
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        val outputLines = process.inputStream.bufferedReader().readLines()
        assertEquals("The script ended with an error", 0, process.waitFor())
        assertEquals("One line of durations for each row", rowLines.size, outputLines.size)
        var line = 0
        return madeAtSpeed.map { pieces ->
            pieces.map { piece ->
                piece.rows.map { row ->
                    val durations = outputLines[line].trim().split(" ").map { it.toInt() }.toIntArray()
                    assertEquals("Durations of row ${line + 1} of $rowsFile, ${String(row, 0, row.size)}", row.size + 2, durations.size)
                    line++
                    durations
                }
            }
        }
    }

    // One ReadingTime and one estimate serve the whole reading of a speed, as those of a reading do, and the reading is told
    // that every piece is made in full before any moment is heard
    private fun follow(segments: List<ReadingSegment>, made: List<MadePiece>, durations: List<List<IntArray>>, sampleRate: Int, speed: ReadingSpeed): List<PieceResult> {
        val reading = ReadingTime(segments.map { it.spoken }, sampleRate, speed)
        for (piece in made) {
            reading.audioMade(piece.number, piece.engineSamples.toInt())
            reading.segmentMade(piece.number)
        }
        val estimate = HeardWordEstimate(segments)
        return made.mapIndexed { index, piece ->
            val reason = reasonLeftOut(piece, segments[piece.number], durations[index])
            if (reason != null) return@mapIndexed LeftOutPiece(piece.number, piece.text, reason)
            val pairing = pairedWords(phonemeWordsOf(piece.rows), segments[piece.number].words) as Pairing.Paired
            val followed = followPiece(piece.number, pairing.wordsInOrder, spansOf(piece.rows, durations[index]), piece.engineSamples, reading, estimate)
            FollowedPiece(piece.number, piece.text, followed.counted, followed.matching)
        }
    }

    private fun reasonLeftOut(piece: MadePiece, segment: ReadingSegment, durations: List<IntArray>): String? {
        if (piece.rows.isEmpty()) return "eSpeak NG gives no phonemes"
        val pairing = pairedWords(phonemeWordsOf(piece.rows), segment.words)
        if (pairing is Pairing.NotPaired) return pairing.reason
        return lengthDisagreement(lengthByTheDurations(durations), piece.engineSamples)
    }

    private companion object {
        const val VOICE_NAME = "af_heart"
        val SPEED_TEXTS = listOf("1.0", "1.5")
    }
}
