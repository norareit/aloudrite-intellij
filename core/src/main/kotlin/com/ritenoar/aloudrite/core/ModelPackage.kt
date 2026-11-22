package com.ritenoar.aloudrite.core

// What the plugin downloads to put a model in place: an archive that is unpacked, or the files of a repository
sealed interface ModelPackage {
    val address: String
    val sizeInBytes: Long
    val unpackedSizeInBytes: Long
}

data class ArchivePackage(
    override val address: String,
    override val sizeInBytes: Long,
    override val unpackedSizeInBytes: Long,
    val sha256: String,
) : ModelPackage

const val HUGGING_FACE_HOST = "https://huggingface.co"

data class RepositoryFile(val path: String, val sizeInBytes: Long, val sha256: String)

// The files of a repository on Hugging Face at one commit, which arrive one by one into the folder of the model. The host
// is a parameter so that the tests can point the download at a stand-in server
data class RepositoryFiles(
    val repository: String,
    val commit: String,
    val files: List<RepositoryFile>,
    val host: String = HUGGING_FACE_HOST,
) : ModelPackage {
    // The files arrive unpacked, so what is downloaded is what the model takes
    override val sizeInBytes: Long = files.sumOf { it.sizeInBytes }
    override val unpackedSizeInBytes: Long get() = sizeInBytes

    // Where the model comes from, as the question before a download names it
    override val address: String get() = "$host/$repository/tree/$commit"

    fun fileAddress(file: RepositoryFile): String = "$host/$repository/resolve/$commit/${encodePath(file.path)}"
}

private val UNRESERVED_CHARACTERS = (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')).map { it.code }.toSet()

// Each segment of the path encoded as RFC 3986 asks for a segment: every byte of its UTF-8 form that is not an unreserved
// character becomes a percent sign and two capital hexadecimal digits, so that a space and "!" are found by the server
fun encodePath(path: String): String = path.split('/').joinToString("/") { segment ->
    segment.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
        val code = byte.toInt() and 0xFF
        if (code in UNRESERVED_CHARACTERS) code.toChar().toString() else "%%%02X".format(code)
    }
}
