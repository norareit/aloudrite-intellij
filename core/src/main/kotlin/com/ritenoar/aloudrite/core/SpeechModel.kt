package com.ritenoar.aloudrite.core

import java.nio.file.Files
import java.nio.file.Path

// One package of Kokoro 1.0 that the plugin can speak with. The package name is also the name of its folder and the
// id under which a choice of it is stored
data class SpeechModel(
    override val displayName: String,
    override val packageName: String,
    val modelFileName: String,
    override val modelPackage: ModelPackage,
) : InstallablePackage {
    override val progressName: String get() = "the speech model"

    // The engine ends the whole IDE process when it is given a configuration without a lexicon, so it must never be
    // created before this holds
    override fun isInPlace(modelFolder: Path): Boolean =
        (SHARED_MODEL_FILE_NAMES + modelFileName).all { Files.isRegularFile(modelFolder.resolve(it)) } &&
            Files.isDirectory(modelFolder.resolve(MODEL_DATA_DIRECTORY_NAME))
}

private val SPEECH_MODEL_FILES = loadSpeechModelFiles()

private fun filesOf(packageName: String): RepositoryFiles =
    checkNotNull(SPEECH_MODEL_FILES[packageName]) { "The list of the speech model files does not hold $packageName" }

private val KOKORO_INT8 = SpeechModel(
    displayName = "Kokoro 1.0, int8",
    packageName = "kokoro-int8-multi-lang-v1_0",
    modelFileName = "model.int8.onnx",
    modelPackage = filesOf("kokoro-int8-multi-lang-v1_0"),
)

private val KOKORO_FP32 = SpeechModel(
    displayName = "Kokoro 1.0, fp32",
    packageName = "kokoro-multi-lang-v1_0",
    modelFileName = "model.onnx",
    modelPackage = filesOf("kokoro-multi-lang-v1_0"),
)

// The models the plugin knows. The page, the download, the removal and the reading iterate this and name none of them
val SPEECH_MODELS: List<SpeechModel> = listOf(KOKORO_INT8, KOKORO_FP32)

// Which model a listener has before they choose one, per platform. The fp32 model makes its audio much faster than the
// int8 one on both platforms with speech, and the int8 model also loses pieces of a reading on macOS on Apple Silicon,
// so both take the fp32 model. A platform without speech reads nothing, so it takes the smaller int8 one
fun defaultSpeechModel(operatingSystemName: String): SpeechModel =
    if (operatingSystemName.startsWith("Mac") || operatingSystemName.startsWith("Linux")) KOKORO_FP32 else KOKORO_INT8
