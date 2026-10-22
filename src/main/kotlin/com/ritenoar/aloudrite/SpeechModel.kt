package com.ritenoar.aloudrite

import java.nio.file.Files
import java.nio.file.Path

// One package of Kokoro 1.0 that the plugin can speak with. The package name is also the name of its folder and the
// id under which a choice of it is stored
data class SpeechModel(
    val displayName: String,
    val packageName: String,
    val modelFileName: String,
    val modelPackage: ModelPackage,
) {
    fun folderIn(modelsFolder: Path): Path = modelsFolder.resolve(packageName)

    // Only looks whether the entries exist. The engine ends the whole IDE process when it is given a configuration
    // without a lexicon, so it must never be created before this holds
    fun isInPlace(modelFolder: Path): Boolean =
        (SHARED_MODEL_FILE_NAMES + modelFileName).all { Files.isRegularFile(modelFolder.resolve(it)) } &&
            Files.isDirectory(modelFolder.resolve(MODEL_DATA_DIRECTORY_NAME))
}

private fun releaseAddress(packageName: String) =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$packageName.tar.bz2"

private val KOKORO_INT8 = SpeechModel(
    displayName = "Kokoro 1.0, int8",
    packageName = "kokoro-int8-multi-lang-v1_0",
    modelFileName = "model.int8.onnx",
    modelPackage = ModelPackage(
        address = releaseAddress("kokoro-int8-multi-lang-v1_0"),
        sizeInBytes = 132_303_094L,
        unpackedSizeInBytes = 189_882_497L,
        sha256 = "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e",
    ),
)

// The models the plugin knows. The page, the download, the removal and the reading iterate this and name none of them
val SPEECH_MODELS: List<SpeechModel> = listOf(KOKORO_INT8)
