package com.ritenoar.aloudrite

import java.nio.file.Files
import java.nio.file.Path

val INT8_MODEL: SpeechModel = SPEECH_MODELS.first()

fun fileNamesOf(model: SpeechModel): List<String> = listOf(model.modelFileName) + SHARED_MODEL_FILE_NAMES

fun fillWithModelStandIn(folder: Path, model: SpeechModel = INT8_MODEL) {
    Files.createDirectories(folder)
    fileNamesOf(model).forEach { Files.createFile(folder.resolve(it)) }
    Files.createDirectories(folder.resolve(MODEL_DATA_DIRECTORY_NAME))
}

fun SpeechModel.withPackage(modelPackage: ModelPackage): SpeechModel = copy(modelPackage = modelPackage)

val FP32_MODEL: SpeechModel = SPEECH_MODELS[1]
