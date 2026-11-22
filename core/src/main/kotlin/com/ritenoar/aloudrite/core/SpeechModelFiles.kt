package com.ritenoar.aloudrite.core

private const val LIST_RESOURCE_NAME = "speech-model-files.tsv"
private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
private val COMMIT_PATTERN = Regex("[0-9a-f]{40}")

// The lists of the files of the speech models, by package name, in the order of the resource file. The plugin downloads
// by them and the check of the sources reads them, so the two cannot disagree
fun loadSpeechModelFiles(): Map<String, RepositoryFiles> {
    val resource = checkNotNull(RepositoryFiles::class.java.getResourceAsStream(LIST_RESOURCE_NAME)) {
        "The plugin does not carry $LIST_RESOURCE_NAME"
    }
    return resource.use { parseSpeechModelFiles(it.readBytes().toString(Charsets.UTF_8), LIST_RESOURCE_NAME) }
}

// A line that opens a model is `model`, the package name, the repository and the commit, tab separated. A line of one of
// its files is `file`, the size, the SHA-256 and the path, with the path last so that it may hold a space. Lines that start
// with `#` and empty lines are skipped. A line that does not hold is rejected with its number
fun parseSpeechModelFiles(text: String, sourceName: String): Map<String, RepositoryFiles> {
    val models = LinkedHashMap<String, RepositoryFiles>()
    var open: OpenModel? = null
    fun close() {
        open?.let { models[it.packageName] = RepositoryFiles(it.repository, it.commit, it.files) }
        open = null
    }
    text.lines().forEachIndexed { index, line ->
        if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
        val reject = { reason: String -> throw IllegalArgumentException("$sourceName line ${index + 1}: $reason") }
        val fields = line.split('\t')
        when (fields[0]) {
            "model" -> {
                close()
                if (fields.size != 4) reject("a model line holds the package name, the repository and the commit")
                if (fields[1] in models) reject("the model ${fields[1]} is listed twice")
                if (!COMMIT_PATTERN.matches(fields[3])) reject("the commit is not 40 lowercase hexadecimal digits")
                open = OpenModel(fields[1], fields[2], fields[3])
            }
            "file" -> {
                val model = open ?: reject("a file line comes before any model line")
                if (fields.size != 4) reject("a file line holds the size, the SHA-256 and the path")
                val size = fields[1].toLongOrNull()?.takeIf { it > 0 } ?: reject("the size is not a positive number")
                if (!SHA256_PATTERN.matches(fields[2])) reject("the SHA-256 is not 64 lowercase hexadecimal digits")
                pathProblem(fields[3])?.let { reject(it) }
                if (model.files.any { it.path == fields[3] }) reject("the path ${fields[3]} is listed twice")
                model.files += RepositoryFile(fields[3], size, fields[2])
            }
            else -> reject("the line is neither a model nor a file line")
        }
    }
    close()
    return models
}

private class OpenModel(val packageName: String, val repository: String, val commit: String) {
    val files = mutableListOf<RepositoryFile>()
}

private fun pathProblem(path: String): String? {
    if (path.startsWith("/")) return "the path is absolute"
    if (path.contains('\\')) return "the path holds a backslash"
    val segments = path.split('/')
    if (segments.any { it.isEmpty() }) return "the path has an empty segment"
    if (segments.any { it == "." || it == ".." }) return "the path holds . or .."
    return null
}
