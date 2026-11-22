package com.ritenoar.aloudrite.core

import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsCallback
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.nio.file.Files
import java.nio.file.Path

private const val CALLBACK_GO_ON = 1
private const val CALLBACK_STOP = 0

// The native code writes a line per piece to the error output of the IDE with the debug output on, which only a
// developer reads. Building the configuration loads no native code
fun kokoroTtsConfig(modelFolder: Path, modelFileName: String, threadCount: Int, debugOutput: Boolean): OfflineTtsConfig =
    OfflineTtsConfig.builder()
        .setModel(
            OfflineTtsModelConfig.builder()
                .setKokoro(
                    OfflineTtsKokoroModelConfig.builder()
                        .setModel(modelFolder.resolve(modelFileName).toString())
                        .setVoices(modelFolder.resolve(VOICES_FILE_NAME).toString())
                        .setTokens(modelFolder.resolve(TOKENS_FILE_NAME).toString())
                        // Without a lexicon the native code ends the whole process, so it is always given, though the
                        // plugin hands the engine phonemes and does not use the lexicon
                        .setLexicon(modelFolder.resolve(LEXICON_FILE_NAME).toString())
                        .build()
                )
                .setNumThreads(threadCount)
                .setDebug(debugOutput)
                .build()
        )
        .build()

// The configuration of one piece: the speaker number of the voice, the speed and the phonemes, which are given in place of
// the text. Building it loads no native code
fun kokoroGenerationConfig(voice: Voice, speed: Float, rows: List<IntArray>): GenerationConfig {
    val config = GenerationConfig()
    config.sid = voice.speakerNumber
    config.speed = speed
    config.phonemeCodepoints = rows.toTypedArray()
    return config
}

// The sandbox IDE of `./gradlew runIde` is started with this system property, and an installed plugin never is
const val ENGINE_DEBUG_PROPERTY = "aloudrite.engine.debug"

fun isEngineDebugOutputOn(propertyValue: String?): Boolean = propertyValue == "true"

// The classes of sherpa-onnx are named here, and those of piper-phonemize in EspeakPhonemes. The model folder has to hold
// a complete model, see SpeechModel.isInPlace. The engine is handed the phonemes of each piece and not its text, which
// the plugin makes with eSpeak NG, so that every word is spoken, see PhonemeRows. One engine serves every voice, since the
// voice is given with each piece, as the language of its phonemes is
class KokoroSpeechEngine(
    modelFolder: Path,
    modelFileName: String,
    threadCount: Int,
    debugOutput: Boolean,
) : SpeechEngine {

    private val tts: OfflineTts
    private val phonemeRows: PhonemeRows

    init {
        EspeakPhonemes.useDataOf(modelFolder.resolve(MODEL_DATA_DIRECTORY_NAME))
        phonemeRows = PhonemeRows(tokenCodePointsOf(Files.readAllLines(modelFolder.resolve(TOKENS_FILE_NAME))), EspeakPhonemes::sentencesOf)
        tts = OfflineTts(kokoroTtsConfig(modelFolder, modelFileName, threadCount, debugOutput))
    }

    override val sampleRate: Int get() = tts.sampleRate

    override fun speak(text: String, speed: Float, voice: Voice, onSamples: (FloatArray) -> Boolean) {
        val rows = phonemeRows.rowsOf(text, voice.accent.espeakLanguage)
        if (rows.isEmpty()) return
        // The text is not read where the phonemes are given
        tts.generateWithConfigAndCallback(text, kokoroGenerationConfig(voice, speed, rows), OfflineTtsCallback { samples ->
            if (onSamples(samples)) CALLBACK_GO_ON else CALLBACK_STOP
        })
    }

    override fun close() {
        tts.release()
    }
}
