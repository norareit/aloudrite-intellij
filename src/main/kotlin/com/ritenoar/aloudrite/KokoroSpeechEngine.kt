package com.ritenoar.aloudrite

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsCallback
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.nio.file.Path

// The number of `af_heart` in the `id2speaker` metadata of the model. The binding has no lookup by name
private const val AF_HEART_SPEAKER_ID = 3
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
                        // Without a lexicon the native code ends the whole process, so it is always given
                        .setLexicon(modelFolder.resolve(LEXICON_FILE_NAME).toString())
                        .setDataDir(modelFolder.resolve(MODEL_DATA_DIRECTORY_NAME).toString())
                        .build()
                )
                .setNumThreads(threadCount)
                .setDebug(debugOutput)
                .build()
        )
        .build()

// The sandbox IDE of `./gradlew runIde` is started with this system property, and an installed plugin never is
const val ENGINE_DEBUG_PROPERTY = "aloudrite.engine.debug"

fun isEngineDebugOutputOn(propertyValue: String?): Boolean = propertyValue == "true"

// The only class that names a class of sherpa-onnx. The model folder has to hold a complete model, see
// SpeechModel.isInPlace
class KokoroSpeechEngine(
    modelFolder: Path,
    modelFileName: String,
    threadCount: Int,
    debugOutput: Boolean,
) : SpeechEngine {

    private val tts: OfflineTts = OfflineTts(kokoroTtsConfig(modelFolder, modelFileName, threadCount, debugOutput))

    override val sampleRate: Int get() = tts.sampleRate

    override fun speak(text: String, speed: Float, onSamples: (FloatArray) -> Boolean) {
        tts.generateWithCallback(text, AF_HEART_SPEAKER_ID, speed, OfflineTtsCallback { samples ->
            if (onSamples(samples)) CALLBACK_GO_ON else CALLBACK_STOP
        })
    }

    override fun close() {
        tts.release()
    }
}
