package com.ritenoar.aloudrite

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// Measures, for unit 0032, how often the word that the highlight shows is the word that is heard, for two sets read in Heart
// with the fp32 model at 1.0 and 1.5: every piece of words of samples/markdown-features.md read from its start, and the 11
// sentences of the trial of unit 0028, each read as a file of its own. The word shown is that of the estimate of the plugin,
// run as a reading runs it. The word heard is the one whose phoneme tokens the model says at the moment, by the durations
// that model.onnx itself gives for them, run here with ONNX Runtime for Java on the tokens that the plugin hands sherpa-onnx,
// moved back by where the engine's audio starts within the audio of model.onnx. Only the moments within a word count, and
// words that eSpeak NG joins count as a group. Runs only where ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING is set, on a platform
// with speech and with the fp32 model in its folder, as
// ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.HighlightFollowingMeasurement
// and writes its table to build/highlight-following.txt
class HighlightFollowingMeasurement {

    // The audio that the engine made for each row of a piece, in the order of the rows
    private class MadePiece(val text: String, val rows: List<IntArray>, val engineRows: List<FloatArray>) {
        val engineSamples: Long get() = engineRows.sumOf { it.size.toLong() }
    }

    // What the model says for one row: its audio and the frames of its tokens with the padding at both ends
    private class ModelRow(val audio: FloatArray, val durations: IntArray)

    // Everything the pieces of every file and speed are measured with
    private class Tools(
        val engine: KokoroSpeechEngine,
        val phonemeRows: PhonemeRows,
        val session: OrtSession,
        val environment: OrtEnvironment,
        val voice: Voice,
        val voices: FloatArray,
        val ids: Map<Int, Int>,
    )

    @Test
    fun measureHowOftenTheWordShownIsTheWordHeard() {
        assumeTrue("ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING is not set", System.getenv("ALOUDRITE_MEASURE_HIGHLIGHT_FOLLOWING") != null)
        assumeTrue(isSpeechSupported(System.getProperty("os.name"), System.getProperty("os.arch")))
        val model = SPEECH_MODELS.single { it.packageName == "kokoro-multi-lang-v1_0" }
        val modelFolder = model.folderIn(userModelsFolder())
        assumeTrue("The model ${model.packageName} is not in $modelFolder", model.isInPlace(modelFolder))
        val voice = checkNotNull(voiceNamed(VOICE_NAME))
        val speeds = SPEED_TEXTS.map { checkNotNull(parsedSpeed(it)) }
        val sets = listOf(
            "samples/markdown-features.md" to listOf(("" to Files.readString(Paths.get("samples/markdown-features.md")))),
            "the 11 sentences of the trial of unit 0028" to TRIAL_SENTENCES.mapIndexed { number, sentence -> "${number + 1}." to sentence },
        )

        val threads = engineThreadCount(Runtime.getRuntime().availableProcessors())
        val modelFile = modelFolder.resolve(model.modelFileName)
        val environment = OrtEnvironment.getEnvironment()
        val results = run {
            OrtSession.SessionOptions().use { options ->
                options.setIntraOpNumThreads(threads)
                environment.createSession(modelFile.toString(), options).use { session ->
                    checkTheSession(session)
                    KokoroSpeechEngine(modelFolder, model.modelFileName, threads, debugOutput = false).use { engine ->
                        assertEquals("The durations are in frames at the sample rate of Kokoro", DURATIONS_SAMPLE_RATE, engine.sampleRate)
                        val lines = Files.readAllLines(modelFolder.resolve(TOKENS_FILE_NAME))
                        val tools = Tools(
                            engine, PhonemeRows(tokenCodePointsOf(lines), EspeakPhonemes::sentencesOf), session, environment, voice,
                            floatsOf(modelFolder.resolve(VOICES_FILE_NAME)), tokenIdsOf(lines),
                        )
                        sets.map { (name, files) -> SetResult(name, speeds.map { speed -> followedAt(speed, files, tools) }) }
                    }
                }
            }
        }

        val table = "model $modelFile, voice $VOICE_NAME, os ${System.getProperty("os.name")}, arch ${System.getProperty("os.arch")}\n\n" + tableOf(results)
        println(table)
        Files.createDirectories(Paths.get("build"))
        Files.writeString(Paths.get("build/highlight-following.txt"), table)
        for ((set, result) in sets.zip(results)) {
            val piecesOfWords = set.second.sumOf { (_, text) -> piecesOf(text).count { it.words.isNotEmpty() } }
            for (speedResult in result.speeds) assertEquals("${result.name} at ${speedResult.speed}", piecesOfWords, speedResult.pieces.size)
        }
    }

    private fun piecesOf(text: String): List<ReadingSegment> = ReadingSegments(placedWhatToRead(text).placedParts).segments

    private fun checkTheSession(session: OrtSession) {
        assertEquals("The inputs of model.onnx", setOf("tokens", "style", "speed"), session.inputNames)
        assertEquals("model.onnx has the durations as an output", true, DURATIONS_OUTPUT in session.outputNames)
    }

    private fun floatsOf(file: Path): FloatArray {
        val buffer = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(buffer.remaining()).also { buffer.get(it) }
    }

    // The pieces of the files of a set at a speed. A piece is made by the engine first, also those that are left out, because
    // the reading is told that every piece is made in full before any moment is heard
    private fun followedAt(speed: ReadingSpeed, files: List<Pair<String, String>>, tools: Tools): SpeedResult =
        SpeedResult(speed.display, files.flatMap { (prefix, text) -> followedFile(prefix, text, speed, tools) })

    private fun followedFile(prefix: String, text: String, speed: ReadingSpeed, tools: Tools): List<PieceResult> {
        val segments = piecesOf(text)
        val made = segments.mapIndexedNotNull { number, segment ->
            if (segment.words.isEmpty()) null else number to madePieceOf(number, segment, speed, tools)
        }
        val reading = ReadingTime(segments.map { it.spoken }, tools.engine.sampleRate, speed)
        for ((number, piece) in made) {
            reading.audioMade(number, piece.engineSamples.toInt())
            reading.segmentMade(number)
        }
        val estimate = HeardWordEstimate(segments)
        return made.map { (number, piece) -> followedPiece(prefix + number, number, piece, segments[number], speed, reading, estimate, tools) }
    }

    // One array of samples per call of the callback is the audio of one row, which fails the run where the numbers differ
    private fun madePieceOf(number: Int, segment: ReadingSegment, speed: ReadingSpeed, tools: Tools): MadePiece {
        val text = (segment.spoken as Words).text
        val rows = tools.phonemeRows.rowsOf(text, tools.voice.accent.espeakLanguage)
        val engineRows = mutableListOf<FloatArray>()
        if (rows.isNotEmpty()) {
            tools.engine.speak(text, speed.forTheEngine, tools.voice) { samples ->
                engineRows += samples.copyOf()
                true
            }
        }
        assertEquals("Arrays of samples from the engine for piece $number, ${text.take(SHOWN_TEXT)}", rows.size, engineRows.size)
        return MadePiece(text, rows, engineRows)
    }

    private fun followedPiece(
        label: String,
        number: Int,
        piece: MadePiece,
        segment: ReadingSegment,
        speed: ReadingSpeed,
        reading: ReadingTime,
        estimate: HeardWordEstimate,
        tools: Tools,
    ): PieceResult {
        fun leftOut(reason: String) = LeftOutPiece(label, piece.text, reason)
        if (piece.rows.isEmpty()) return leftOut("eSpeak NG gives no phonemes")
        val texts = when (val found = textsOfWords(piece.text, segment.words.size)) {
            is WordTexts.NotFound -> return leftOut(found.reason)
            is WordTexts.Found -> found.texts
        }
        val language = tools.voice.accent.espeakLanguage
        val wordsToGroup = segment.words.zip(texts) { word, wordText ->
            val phonemes = if (word.weight > 0) barePhonemesOfRows(tools.phonemeRows.rowsOf(wordText, language)) else ""
            WordToGroup(word, wordText, phonemes)
        }
        val phonemeWords = phonemeWordsOf(piece.rows).map { barePhonemesOf(piece.rows[it.row], it) }
        val groups = when (val grouping = groupsOf(phonemeWords, wordsToGroup)) {
            is Grouping.NotGrouped -> return leftOut(grouping.reason)
            is Grouping.Grouped -> grouping.groups
        }
        val placed = placedRowsOf(label, piece, speed, tools).let { it.first ?: return leftOut(checkNotNull(it.second)) }
        val followed = followPiece(number, groups, spansOf(piece.rows, placed), piece.engineSamples, reading, estimate)
        return FollowedPiece(label, piece.text, followed.counted, followed.matching, followed.grouped)
    }

    // The rows placed in the engine's audio, or the reason that one cannot be. A row whose durations are not its tokens and
    // the two padding tokens, or do not add up to its audio of the model, fails the run
    private fun placedRowsOf(label: String, piece: MadePiece, speed: ReadingSpeed, tools: Tools): Pair<List<PlacedRow>?, String?> {
        var start = 0L
        val placedRows = mutableListOf<PlacedRow>()
        for ((index, row) in piece.rows.withIndex()) {
            val modelRow = modelRowOf(row, speed, tools)
            val where = "piece $label, row ${index + 1}"
            assertEquals("Durations of $where", row.size + 2, modelRow.durations.size)
            assertEquals("Samples of the model's audio of $where", modelRow.durations.sumOf { it.toLong() } * FRAME_SAMPLES, modelRow.audio.size.toLong())
            val engineRow = piece.engineRows[index]
            when (val placement = placementOf(engineRow, modelRow.audio, index + 1)) {
                is Placement.NotPlaced -> return null to placement.reason
                is Placement.Placed -> placedRows += PlacedRow(modelRow.durations, start, engineRow.size.toLong(), placement.offset.toLong())
            }
            start += engineRow.size
        }
        return placedRows to null
    }

    private fun modelRowOf(row: IntArray, speed: ReadingSpeed, tools: Tools): ModelRow {
        val ids = idsOfRow(row, tools.ids)
        val style = styleOf(tools.voices, tools.voice.speakerNumber, row.size)
        val inputs = mapOf(
            "tokens" to OnnxTensor.createTensor(tools.environment, arrayOf(ids)),
            "style" to OnnxTensor.createTensor(tools.environment, arrayOf(style)),
            "speed" to OnnxTensor.createTensor(tools.environment, floatArrayOf(speed.forTheEngine)),
        )
        try {
            tools.session.run(inputs).use { outputs ->
                val audioName = tools.session.outputNames.single { it != DURATIONS_OUTPUT }
                val audio = numbersOf(outputs.get(audioName).get().value).map { it.toFloat() }.toFloatArray()
                val durations = numbersOf(outputs.get(DURATIONS_OUTPUT).get().value).map { it.toInt() }.toIntArray()
                return ModelRow(audio, durations)
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    // The numbers of an output of any rank, in order
    private fun numbersOf(value: Any?): List<Number> = when (value) {
        is FloatArray -> value.toList()
        is LongArray -> value.toList()
        is IntArray -> value.toList()
        is Array<*> -> value.flatMap { numbersOf(it) }
        else -> error("An output of the model is of the type ${value?.javaClass}")
    }

    private companion object {
        const val VOICE_NAME = "af_heart"
        const val DURATIONS_OUTPUT = "onnx::Shape_3411"
        const val SHOWN_TEXT = 50
        val SPEED_TEXTS = listOf("1.0", "1.5")

        // The sentences of the trial of unit 0028, as the spec of unit 0032 gives them
        val TRIAL_SENTENCES = listOf(
            "The lighthouse stood at the end of a narrow spit of rock, and for most of the year nobody went near it.",
            "The keeper came twice a month with a boat full of supplies, checked the lamp, and wrote a few lines in a book that nobody else would ever read.",
            "In the spring the birds returned.",
            "They nested in the cracks of the old stone wall, and their noise filled the mornings until the keeper could hardly hear the sea.",
            "He did not mind.",
            "He had grown used to the company of things that did not ask questions, and he liked the way the gulls argued over the scraps he threw from the landing.",
            "The tower itself was older than anyone could say for certain.",
            "Two candles and a box of matches.",
            "Climb the stairs and unlock the lamp room.",
            "Call record found boat once for every boat, and never change the date by hand.",
            "The answer was in the old register, which he had read twice.",
        )
    }
}
