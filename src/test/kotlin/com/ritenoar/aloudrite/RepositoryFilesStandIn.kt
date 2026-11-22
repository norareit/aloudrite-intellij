package com.ritenoar.aloudrite

import com.ritenoar.aloudrite.core.INT8_MODEL
import com.ritenoar.aloudrite.core.MODEL_DATA_DIRECTORY_NAME
import com.ritenoar.aloudrite.core.RepositoryFile
import com.ritenoar.aloudrite.core.RepositoryFiles
import com.ritenoar.aloudrite.core.SpeechModel
import com.ritenoar.aloudrite.core.encodePath
import com.ritenoar.aloudrite.core.fileNamesOf
import java.util.Random

// A small repository with the shape of a model: its files, one of them with a space and one with "!" in the path, of a few
// kilobytes each, served under the addresses of Hugging Face by their encoded paths only, so that an address that is not
// encoded gets 404
class RepositoryFilesStandIn(val model: SpeechModel = INT8_MODEL) {

    val repository = "owner/${model.packageName}"
    val commit = "0123456789abcdef0123456789abcdef01234567"

    val contents: Map<String, ByteArray> = run {
        val random = Random(11)
        val paths = fileNamesOf(model) + SPACE_PATH + (1..FILLER_FILES).map { "$MODEL_DATA_DIRECTORY_NAME/dict/file-$it" }
        paths.associateWith { ByteArray(FILE_SIZE + it.length).also(random::nextBytes) }
    }

    fun repositoryFiles(server: PackageServerStandIn): RepositoryFiles = RepositoryFiles(
        repository, commit,
        contents.map { (path, bytes) -> RepositoryFile(path, bytes.size.toLong(), ModelPackageStandIn.sha256Of(bytes)) },
        host = server.address(""),
    )

    fun rawPathOf(path: String) = "/$repository/resolve/$commit/${encodePath(path)}"

    // The special answer is given for a raw path and the number of the request with it, and the content of the file where
    // it gives none
    fun serveOn(server: PackageServerStandIn, special: (path: String, requestNumber: Int) -> StandInAnswer? = { _, _ -> null }) {
        val byRawPath = contents.mapKeys { (path, _) -> rawPathOf(path) }
        server.serveBy("/$repository/resolve/$commit/") { rawPath, requestNumber ->
            val path = contents.keys.firstOrNull { rawPathOf(it) == rawPath }
            val answer = path?.let { special(it, requestNumber) }
            answer ?: byRawPath[rawPath]?.let { StandInAnswer(body = it) }
        }
    }

    companion object {
        const val SPACE_PATH = "$MODEL_DATA_DIRECTORY_NAME/voices/!v/Mr serious"
        private const val FILLER_FILES = 12
        private const val FILE_SIZE = 6_000
    }
}

