package com.ritenoar.aloudrite.sources

import com.ritenoar.aloudrite.core.RepositoryFile
import com.ritenoar.aloudrite.core.RepositoryFiles
import com.ritenoar.aloudrite.core.loadSpeechModelFiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.system.exitProcess

// What Hugging Face answered to one request, with the names of the headers in lowercase
class SourceAnswer(val status: Int, val headers: Map<String, String>, val body: String = "")

private val REVISION_SHA = Regex("\"sha\"\\s*:\\s*\"([0-9a-f]{40})\"")

fun revisionAddress(files: RepositoryFiles): String = "${files.host}/api/models/${files.repository}/revision/${files.commit}"

// The file whose headers the check reads: the largest one, since it is the one that costs a user most if it were gone
fun largestFile(files: RepositoryFiles): RepositoryFile = files.files.maxBy { it.sizeInBytes }

// What does not match between what the plugin was built for and what Hugging Face answers about one model, one text per
// problem, empty when the commit is still served. Both answers are handed in, so that this needs no network
fun problemsOf(model: String, files: RepositoryFiles, revision: SourceAnswer, largestFileHead: SourceAnswer): List<String> =
    revisionProblems(model, files, revision) + headProblems(model, files, largestFileHead)

private fun revisionProblems(model: String, files: RepositoryFiles, answer: SourceAnswer): List<String> {
    val address = revisionAddress(files)
    if (answer.status == 404) return listOf("$model, $address: the commit ${files.commit} is missing from ${files.repository}")
    if (answer.status !in 200..299) return listOf("$model, $address: the answer was HTTP ${answer.status}")
    val sha = REVISION_SHA.find(answer.body)?.groupValues?.get(1)
    if (sha != files.commit) {
        return listOf("$model, $address: the commit ${files.commit} is missing, the revision answered with ${sha ?: "no commit"}")
    }
    return emptyList()
}

private fun headProblems(model: String, files: RepositoryFiles, answer: SourceAnswer): List<String> {
    val file = largestFile(files)
    val address = files.fileAddress(file)
    if (answer.status !in 200..399) return listOf("$model, $address: the answer was HTTP ${answer.status}")
    return listOfNotNull(
        differing(model, address, "x-repo-commit", files.commit, answer.headers["x-repo-commit"]),
        differing(model, address, "x-linked-size", file.sizeInBytes.toString(), answer.headers["x-linked-size"]),
        differing(model, address, "x-linked-etag", file.sha256, answer.headers["x-linked-etag"]?.trim('"')),
    )
}

private fun differing(model: String, address: String, header: String, expected: String, actual: String?): String? =
    if (actual == expected) null else "$model, $address: $header is ${actual ?: "missing"}, the list has $expected"

fun main() {
    val client = HttpClient.newHttpClient()
    val problems = loadSpeechModelFiles().flatMap { (model, files) ->
        problemsOf(model, files, ask(client, "GET", revisionAddress(files)), ask(client, "HEAD", files.fileAddress(largestFile(files))))
    }
    problems.forEach(::println)
    if (problems.isNotEmpty()) exitProcess(1)
    println("Hugging Face still serves the commits of ${loadSpeechModelFiles().size} speech models.")
}

// A redirect is not followed: the headers of the first answer are the ones that name the commit, the size and the checksum
private fun ask(client: HttpClient, method: String, address: String): SourceAnswer {
    val request = HttpRequest.newBuilder(URI(address)).method(method, HttpRequest.BodyPublishers.noBody()).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val headers = response.headers().map().mapKeys { it.key.lowercase() }.mapValues { it.value.first() }
    return SourceAnswer(response.statusCode(), headers, response.body())
}
