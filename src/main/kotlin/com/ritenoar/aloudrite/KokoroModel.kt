package com.ritenoar.aloudrite

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val MODEL_PACKAGE_NAME = "kokoro-int8-multi-lang-v1_0"
const val MODEL_FILE_NAME = "model.int8.onnx"
const val VOICES_FILE_NAME = "voices.bin"
const val TOKENS_FILE_NAME = "tokens.txt"
const val LEXICON_FILE_NAME = "lexicon-us-en.txt"
const val MODEL_DATA_DIRECTORY_NAME = "espeak-ng-data"

val MODEL_FILE_NAMES = listOf(MODEL_FILE_NAME, VOICES_FILE_NAME, TOKENS_FILE_NAME, LEXICON_FILE_NAME)

fun userModelFolder(operatingSystemName: String, userHome: String, xdgDataHome: String?): Path {
    if (operatingSystemName.startsWith("Mac")) {
        return Paths.get(userHome, "Library", "Application Support", "AloudRite", MODEL_PACKAGE_NAME)
    }
    val dataHome = xdgDataHome?.takeIf { Paths.get(it).isAbsolute }?.let { Paths.get(it) }
        ?: Paths.get(userHome, ".local", "share")
    return dataHome.resolve("aloudrite").resolve(MODEL_PACKAGE_NAME)
}

// Only looks whether the entries exist. The engine ends the whole IDE process when it is given a configuration
// without a lexicon, so it must never be created before this holds
fun isModelInPlace(modelFolder: Path): Boolean =
    MODEL_FILE_NAMES.all { Files.isRegularFile(modelFolder.resolve(it)) } &&
        Files.isDirectory(modelFolder.resolve(MODEL_DATA_DIRECTORY_NAME))
