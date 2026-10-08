package com.ritenoar.aloudrite

import java.nio.file.Files
import java.nio.file.Path

fun fillWithModelStandIn(folder: Path) {
    Files.createDirectories(folder)
    MODEL_FILE_NAMES.forEach { Files.createFile(folder.resolve(it)) }
    Files.createDirectories(folder.resolve(MODEL_DATA_DIRECTORY_NAME))
}
