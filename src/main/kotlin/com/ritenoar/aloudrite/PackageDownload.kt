package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.io.HttpRequests
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val MAX_REDIRECTS = 10
private const val BUFFER_SIZE = 65_536
private const val LEAST_USABLE_BYTES = 10_000_000L
private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)

private sealed class Hop {
    data class Redirect(val location: String?) : Hop()
    data class Downloaded(val sha256: String) : Hop()
}

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
    modelPackage: ModelPackage,
    target: Path,
    indicator: ProgressIndicator,
    resolveRedirect: (current: String, location: String?) -> String = ::redirectTarget,
): String {
    var address = modelPackage.address
    repeat(MAX_REDIRECTS + 1) {
        when (val hop = fetch(address, modelPackage, target, indicator)) {
            is Hop.Downloaded -> return hop.sha256
            is Hop.Redirect -> address = resolveRedirect(address, hop.location)
        }
    }
    throw ModelInstallException(InstallFailure.TooManyRedirects)
}

private fun fetch(address: String, modelPackage: ModelPackage, target: Path, indicator: ProgressIndicator): Hop {
    indicator.checkCanceled()
    try {
        return HttpRequests.request(address)
            .followRedirects(false)
            .gzip(false)
            .throwStatusCodeException(false)
            .connect { request -> handleAnswer(request, modelPackage, target, indicator) }
    } catch (exception: IOException) {
        val host = URI(address).host ?: address
        throw ModelInstallException(InstallFailure.NoConnection(host), exception)
    }
}

private fun handleAnswer(
    request: HttpRequests.Request,
    modelPackage: ModelPackage,
    target: Path,
    indicator: ProgressIndicator,
): Hop {
    val connection = request.connection as HttpURLConnection
    val status = connection.responseCode
    if (status in REDIRECT_STATUSES) return Hop.Redirect(connection.getHeaderField("Location"))
    if (status != HttpURLConnection.HTTP_OK) throw ModelInstallException(InstallFailure.ServerAnswer(status))
    return Hop.Downloaded(receive(request, connection.contentLengthLong, modelPackage, target, indicator))
}

private fun receive(
    request: HttpRequests.Request,
    announcedLength: Long,
    modelPackage: ModelPackage,
    target: Path,
    indicator: ProgressIndicator,
): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_SIZE)
    var received = 0L
    indicator.isIndeterminate = false
    indicator.text = "Downloading the speech model"
    openForWriting(target, modelPackage).use { output ->
        val input = request.inputStream
        while (true) {
            indicator.checkCanceled()
            val count = readOrBreakOff(input::read, buffer, received, modelPackage)
            if (count < 0) break
            received += count
            // A server that sends more than the package must not be able to fill the disk
            if (received > modelPackage.sizeInBytes) {
                throw ModelInstallException(InstallFailure.ChecksumMismatch(modelPackage.address))
            }
            digest.update(buffer, 0, count)
            writeOrFail(output, buffer, count, target, modelPackage)
            indicator.fraction = received.toDouble() / modelPackage.sizeInBytes
            indicator.text2 = "${megabytes(received)} of ${megabytes(modelPackage.sizeInBytes)} MB"
        }
    }
    if (announcedLength >= 0 && received < announcedLength) {
        throw ModelInstallException(InstallFailure.BrokeOff(received, modelPackage.sizeInBytes))
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun readOrBreakOff(
    read: (ByteArray) -> Int,
    buffer: ByteArray,
    received: Long,
    modelPackage: ModelPackage,
): Int = try {
    read(buffer)
} catch (exception: IOException) {
    throw ModelInstallException(InstallFailure.BrokeOff(received, modelPackage.sizeInBytes), exception)
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
