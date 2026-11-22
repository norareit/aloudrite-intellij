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

private fun releaseAddress(packageName: String) =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$packageName.tar.bz2"

private val KOKORO_INT8 = SpeechModel(
    displayName = "Kokoro 1.0, int8",
    packageName = "kokoro-int8-multi-lang-v1_0",
    modelFileName = "model.int8.onnx",
    modelPackage = ArchivePackage(
        address = releaseAddress("kokoro-int8-multi-lang-v1_0"),
        sizeInBytes = 132_303_094L,
        unpackedSizeInBytes = 189_882_497L,
        sha256 = "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e",
    ),
)

private val KOKORO_FP32 = SpeechModel(
    displayName = "Kokoro 1.0, fp32",
    packageName = "kokoro-multi-lang-v1_0",
    modelFileName = "model.onnx",
    modelPackage = ArchivePackage(
        address = releaseAddress("kokoro-multi-lang-v1_0"),
        sizeInBytes = 349_906_910L,
        unpackedSizeInBytes = 401_239_297L,
        sha256 = "c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298",
    ),
)

// The models the plugin knows. The page, the download, the removal and the reading iterate this and name none of them
val SPEECH_MODELS: List<SpeechModel> = listOf(KOKORO_INT8, KOKORO_FP32)

// Which model a listener has before they choose one, per platform. The fp32 model makes its audio much faster than the
// int8 one on both platforms with speech, and the int8 model also loses pieces of a reading on macOS on Apple Silicon,
// so both take the fp32 model. A platform without speech reads nothing, so it takes the smaller int8 one
fun defaultSpeechModel(operatingSystemName: String): SpeechModel =
    if (operatingSystemName.startsWith("Mac") || operatingSystemName.startsWith("Linux")) KOKORO_FP32 else KOKORO_INT8
