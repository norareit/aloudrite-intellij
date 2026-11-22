package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.io.HttpRequests
import com.ritenoar.aloudrite.core.ArchivePackage
import com.ritenoar.aloudrite.core.ModelPackage
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val MAX_REDIRECTS = 10
const val HTTP_TOO_MANY_REQUESTS = 429
private const val BUFFER_SIZE = 65_536
private const val LEAST_USABLE_BYTES = 10_000_000L
private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)

private sealed class Hop {
    data class Redirect(val location: String?) : Hop()
    data class Downloaded(val sha256: String, val receivedBytes: Long) : Hop()
}

// What the server of one address answered when it refused the request because it limits them, with the headers that say
// for how long. Only the downloads that wait for the limit to end look at it. downloadPackage turns it into a plain answer
class RateLimitedAnswer(val retryAfter: String?, val rateLimit: String?) : Exception("HTTP 429")

// One file to fetch from one address: where it goes, the size it must not pass, which package its disk space is told
// about on a failure, and what to do while its bytes arrive
class FileFetch(
    val address: String,
    val target: Path,
    val sizeInBytes: Long,
    val modelPackage: ModelPackage,
    val checkCanceled: () -> Unit,
    val onReceived: (count: Int, receivedBytes: Long) -> Unit,
)

class FetchedFile(val sha256: String, val receivedBytes: Long)

// Resolves a redirect against the address that sent it. The plugin follows redirects itself, because the platform
// would follow one to any scheme, and the package must only ever come over HTTPS
fun redirectTarget(current: String, location: String?): String {
    val target = location?.let { runCatching { URI(current).resolve(it) }.getOrNull() }
    if (target == null || target.scheme != "https") {
        throw ModelInstallException(InstallFailure.RedirectRefused(location ?: "no address"))
    }
    return target.toString()
}

fun isOutOfDiskSpace(message: String?, usableBytes: Long): Boolean =
    message?.contains("No space left on device") == true || usableBytes < LEAST_USABLE_BYTES

// Returns the SHA-256 of what arrived, as lowercase hexadecimal, computed while the bytes were written
// The stand-in server of the tests speaks plain HTTP, so they hand in a resolver that accepts it
fun downloadPackage(
    archive: ArchivePackage,
    target: Path,
    indicator: ProgressIndicator,
    resolveRedirect: (current: String, location: String?) -> String = ::redirectTarget,
    progressName: String = "the speech model",
): String {
    indicator.isIndeterminate = false
    indicator.text = "Downloading $progressName"
    val fetch = FileFetch(archive.address, target, archive.sizeInBytes, archive, indicator::checkCanceled) { _, receivedBytes ->
        indicator.fraction = receivedBytes.toDouble() / archive.sizeInBytes
        indicator.text2 = "${megabytes(receivedBytes)} of ${megabytes(archive.sizeInBytes)} MB"
    }
    try {
        return fetchFile(fetch, resolveRedirect).sha256
    } catch (_: RateLimitedAnswer) {
        throw ModelInstallException(InstallFailure.ServerAnswer(HTTP_TOO_MANY_REQUESTS))
    }
}

// Follows at most MAX_REDIRECTS redirects of the address of the file, and writes what the
// last address answers into the target. A refusal for the limit of requests is thrown as RateLimitedAnswer
fun fetchFile(
    fetch: FileFetch,
    resolveRedirect: (current: String, location: String?) -> String = ::redirectTarget,
): FetchedFile {
    var address = fetch.address
    repeat(MAX_REDIRECTS + 1) {
        when (val hop = fetchOnce(address, fetch)) {
            is Hop.Downloaded -> return FetchedFile(hop.sha256, hop.receivedBytes)
            is Hop.Redirect -> address = resolveRedirect(address, hop.location)
        }
    }
    throw ModelInstallException(InstallFailure.TooManyRedirects)
}

private fun fetchOnce(address: String, fetch: FileFetch): Hop {
    fetch.checkCanceled()
    try {
        return HttpRequests.request(address)
            .followRedirects(false)
            .gzip(false)
            .throwStatusCodeException(false)
            .connect { request -> handleAnswer(request, fetch) }
    } catch (exception: IOException) {
        val host = URI(address).host ?: address
        throw ModelInstallException(InstallFailure.NoConnection(host), exception)
    }
}

private fun handleAnswer(request: HttpRequests.Request, fetch: FileFetch): Hop {
    val connection = request.connection as HttpURLConnection
    val status = connection.responseCode
    if (status in REDIRECT_STATUSES) return Hop.Redirect(connection.getHeaderField("Location"))
    if (status == HTTP_TOO_MANY_REQUESTS) {
        throw RateLimitedAnswer(connection.getHeaderField("Retry-After"), connection.getHeaderField("ratelimit"))
    }
    if (status != HttpURLConnection.HTTP_OK) throw ModelInstallException(InstallFailure.ServerAnswer(status))
    return receive(request, connection.contentLengthLong, fetch)
}

private fun receive(request: HttpRequests.Request, announcedLength: Long, fetch: FileFetch): Hop.Downloaded {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_SIZE)
    var received = 0L
    openForWriting(fetch.target, fetch.modelPackage).use { output ->
        val input = request.inputStream
        while (true) {
            fetch.checkCanceled()
            val count = readOrBreakOff(input::read, buffer, received, fetch.sizeInBytes)
            if (count < 0) break
            received += count
            // A server that sends more than the file must not be able to fill the disk
            if (received > fetch.sizeInBytes) throw ModelInstallException(InstallFailure.ChecksumMismatch(fetch.address))
            digest.update(buffer, 0, count)
            writeOrFail(output, buffer, count, fetch.target, fetch.modelPackage)
            fetch.onReceived(count, received)
        }
    }
    if (announcedLength >= 0 && received < announcedLength) {
        throw ModelInstallException(InstallFailure.BrokeOff(received, fetch.sizeInBytes))
    }
    return Hop.Downloaded(digest.digest().joinToString("") { "%02x".format(it) }, received)
}

private fun readOrBreakOff(
    read: (ByteArray) -> Int,
    buffer: ByteArray,
    received: Long,
    sizeInBytes: Long,
): Int = try {
    read(buffer)
} catch (exception: IOException) {
    throw ModelInstallException(InstallFailure.BrokeOff(received, sizeInBytes), exception)
}

private fun openForWriting(target: Path, modelPackage: ModelPackage): OutputStream = try {
    Files.newOutputStream(target)
} catch (exception: IOException) {
    throw ioFailure(exception, target.toAbsolutePath().parent, modelPackage)
}

private fun writeOrFail(output: OutputStream, buffer: ByteArray, count: Int, target: Path, modelPackage: ModelPackage) {
    try {
        output.write(buffer, 0, count)
    } catch (exception: IOException) {
        throw ioFailure(exception, target.toAbsolutePath().parent, modelPackage)
    }
}

// What a failed write means, given the folder that it was written into
fun ioFailure(exception: IOException, folder: Path, modelPackage: ModelPackage): ModelInstallException {
    val failure = if (isOutOfDiskSpace(exception.message, usableBytesIn(folder))) {
        InstallFailure.OutOfDiskSpace(folder, modelPackage)
    } else {
        unexpectedFailure(exception)
    }
    return ModelInstallException(failure, exception)
}

// The message of a file system exception is only the name of the file, so the kind of the exception goes with it
fun unexpectedFailure(exception: IOException): InstallFailure.Unexpected = InstallFailure.Unexpected(
    when {
        exception is FileSystemException -> "${exception.javaClass.simpleName}: ${exception.message}"
        else -> exception.message ?: exception.toString()
    })

fun usableBytesIn(folder: Path): Long = try {
    Files.getFileStore(folder).usableSpace
} catch (_: IOException) {
    Long.MAX_VALUE
}
