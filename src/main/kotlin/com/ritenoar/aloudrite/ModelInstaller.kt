package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

// One attempt to put the model in place. The model folder comes into being by one rename of a complete directory
// that was flushed to the disk, and at no other moment. Everything before lies in an attempt directory that
// isModelInPlace never looks at, so a killed IDE or a lost power leaves no model that is incomplete
class ModelInstaller(private val modelPackage: ModelPackage) {

    fun install(modelFolder: Path, indicator: ProgressIndicator) {
        if (isModelInPlace(modelFolder)) return
        if (isInTheWay(modelFolder)) throw ModelInstallException(InstallFailure.FolderInTheWay(modelFolder))
        val parent = modelFolder.toAbsolutePath().parent
        var createdDirectories = emptyList<Path>()
        var installed = false
        try {
            createdDirectories = createMissingDirectories(parent)
            installInAttempt(modelFolder.toAbsolutePath(), parent, indicator)
            installed = true
        } catch (exception: IOException) {
            throw ioFailure(exception, parent)
        } finally {
            if (!installed) removeEmptyDirectories(createdDirectories)
        }
    }

    // Anything but an empty directory is not the plugin's: it only ever creates the model folder by a rename
    private fun isInTheWay(modelFolder: Path): Boolean {
        if (!Files.exists(modelFolder)) return false
        if (!Files.isDirectory(modelFolder)) return true
        return Files.list(modelFolder).use { it.findFirst().isPresent }
    }

    private fun installInAttempt(modelFolder: Path, parent: Path, indicator: ProgressIndicator) {
        InstallAttempt.removeLeftovers(parent)
        InstallAttempt.start(parent).use { attempt ->
            downloadAndCheck(attempt, indicator)
            unpackAndCheck(attempt, indicator)
            val unpackedModel = attempt.unpackedDirectory.resolve(MODEL_PACKAGE_NAME)
            flushTree(unpackedModel)
            indicator.checkCanceled()
            moveIntoPlace(unpackedModel, modelFolder, parent)
        }
    }

    private fun downloadAndCheck(attempt: InstallAttempt, indicator: ProgressIndicator) {
        val sha256 = downloadPackage(modelPackage, attempt.packageFile, indicator)
        indicator.checkCanceled()
        if (sha256 != modelPackage.sha256) {
            throw ModelInstallException(InstallFailure.ChecksumMismatch(modelPackage.address))
        }
    }

    private fun unpackAndCheck(attempt: InstallAttempt, indicator: ProgressIndicator) {
        unpackPackage(attempt.packageFile, attempt.unpackedDirectory, modelPackage.sizeInBytes, indicator)
        val entries = Files.list(attempt.unpackedDirectory).use { stream -> stream.map { it.fileName.toString() }.toList() }
        if (entries != listOf(MODEL_PACKAGE_NAME) || !isModelInPlace(attempt.unpackedDirectory.resolve(MODEL_PACKAGE_NAME))) {
            throw ModelInstallException(InstallFailure.CannotUnpack("it does not hold a complete model"))
        }
    }

    // Another IDE may have been faster. Its complete model is as good as ours, and ours is then dropped
    private fun moveIntoPlace(unpackedModel: Path, modelFolder: Path, parent: Path) {
        try {
            Files.move(unpackedModel, modelFolder, StandardCopyOption.ATOMIC_MOVE)
        } catch (exception: IOException) {
            if (isModelInPlace(modelFolder)) return
            throw exception
        }
        flush(parent)
    }

    private fun createMissingDirectories(directory: Path): List<Path> {
        val missing = generateSequence(directory) { it.parent }.takeWhile { !Files.exists(it) }.toList()
        Files.createDirectories(directory)
        return missing
    }

    // Deepest first, and only what is empty: a directory that holds anything now is not ours to remove
    private fun removeEmptyDirectories(directories: List<Path>) {
        directories.forEach { directory ->
            try {
                Files.deleteIfExists(directory)
            } catch (_: DirectoryNotEmptyException) {
            }
        }
    }

    private fun flushTree(root: Path) {
        Files.walk(root).use { paths -> paths.forEach { flush(it) } }
    }

    private fun flush(path: Path) {
        FileChannel.open(path, StandardOpenOption.READ).use { it.force(true) }
    }
}
