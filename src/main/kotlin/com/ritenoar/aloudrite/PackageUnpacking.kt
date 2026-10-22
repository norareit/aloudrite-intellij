package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.io.Decompressor
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

// Unpacks the package into the target directory. The progress is the part of the package read so far
fun unpackPackage(packageFile: Path, target: Path, modelPackage: ModelPackage, indicator: ProgressIndicator) {
    indicator.isIndeterminate = false
    indicator.text = "Unpacking the speech model"
    indicator.text2 = ""
    try {
        Files.newInputStream(packageFile).use { input ->
            val counting = ProgressInputStream(input, modelPackage.sizeInBytes, indicator)
            Decompressor.Tar(counting)
                .escapingSymlinkPolicy(Decompressor.EscapingSymlinkPolicy.DISALLOW)
                .extract(target)
        }
    } catch (exception: IOException) {
        throw ModelInstallException(unpackFailure(exception, target, modelPackage), exception)
    }
}

private fun unpackFailure(exception: IOException, target: Path, modelPackage: ModelPackage): InstallFailure {
    val folder = target.toAbsolutePath().parent
    if (isOutOfDiskSpace(exception.message, usableBytesIn(folder))) return InstallFailure.OutOfDiskSpace(folder, modelPackage)
    return InstallFailure.CannotUnpack(exception.message ?: exception.toString())
}

// Looks at the cancellation at every read, so that a cancel takes effect also inside the one large file of the model
private class ProgressInputStream(
    input: InputStream,
    private val sizeInBytes: Long,
    private val indicator: ProgressIndicator,
) : FilterInputStream(input) {

    private var bytesRead = 0L

    override fun read(): Int {
        indicator.checkCanceled()
        val value = super.read()
        if (value >= 0) advance(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        indicator.checkCanceled()
        val count = super.read(buffer, offset, length)
        if (count > 0) advance(count)
        return count
    }

    private fun advance(count: Int) {
        bytesRead += count
        indicator.fraction = (bytesRead.toDouble() / sizeInBytes).coerceAtMost(1.0)
    }
}
