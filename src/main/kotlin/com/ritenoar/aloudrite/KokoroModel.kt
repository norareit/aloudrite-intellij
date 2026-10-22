package com.ritenoar.aloudrite

import java.nio.file.Path
import java.nio.file.Paths

// The files that every package of Kokoro 1.0 holds, and the one that differs is the model of each SpeechModel
const val VOICES_FILE_NAME = "voices.bin"
const val TOKENS_FILE_NAME = "tokens.txt"
const val LEXICON_FILE_NAME = "lexicon-us-en.txt"
const val MODEL_DATA_DIRECTORY_NAME = "espeak-ng-data"

val SHARED_MODEL_FILE_NAMES = listOf(VOICES_FILE_NAME, TOKENS_FILE_NAME, LEXICON_FILE_NAME)

// The folder that holds the folders of the models
fun userModelsFolder(operatingSystemName: String, userHome: String, xdgDataHome: String?): Path {
    if (operatingSystemName.startsWith("Mac")) {
        return Paths.get(userHome, "Library", "Application Support", "AloudRite")
    }
    val dataHome = xdgDataHome?.takeIf { Paths.get(it).isAbsolute }?.let { Paths.get(it) }
        ?: Paths.get(userHome, ".local", "share")
    return dataHome.resolve("aloudrite")
}

fun userModelsFolder(): Path =
    userModelsFolder(System.getProperty("os.name"), System.getProperty("user.home"), System.getenv("XDG_DATA_HOME"))
