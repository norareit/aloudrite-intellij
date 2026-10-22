package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

// One attempt to put the model in place. The model folder comes into being by one rename of a complete directory
// that was flushed to the disk, and at no other moment. Everything before lies in an attempt directory that
// SpeechModel.isInPlace never looks at, so a killed IDE or a lost power leaves no model that is incomplete
class ModelInstaller(private val model: SpeechModel) {

    private val modelPackage: ModelPackage get() = model.modelPackage

    // The attempt holds the lock, and its owner closes it, which removes what the attempt wrote
    fun install(modelFolder: Path, attempt: InstallAttempt, indicator: ProgressIndicator) {
        try {
            // Before the folder in the way is looked at: what is not the plugin's stays, what is a dead attempt's goes
            attempt.removeLeftovers()
            if (model.isInPlace(modelFolder)) return
            if (isInTheWay(modelFolder)) throw ModelInstallException(InstallFailure.FolderInTheWay(modelFolder))
            installInAttempt(modelFolder.toAbsolutePath(), attempt, indicator)
        } catch (exception: IOException) {
            throw ioFailure(exception, modelFolder.toAbsolutePath().parent, modelPackage)
        }
    }

    // Anything but an empty directory is not the plugin's: it only ever creates the model folder by a rename
    private fun isInTheWay(modelFolder: Path): Boolean {
        if (!Files.exists(modelFolder)) return false
        if (!Files.isDirectory(modelFolder)) return true
        return Files.list(modelFolder).use { it.findFirst().isPresent }
    }

    private fun installInAttempt(modelFolder: Path, attempt: InstallAttempt, indicator: ProgressIndicator) {
        attempt.prepareDirectory()
        downloadAndCheck(attempt, indicator)
        unpackAndCheck(attempt, indicator)
        val unpackedModel = attempt.unpackedDirectory.resolve(model.packageName)
        flushTree(unpackedModel)
        indicator.checkCanceled()
        moveIntoPlace(unpackedModel, modelFolder, modelFolder.parent)
    }

    private fun downloadAndCheck(attempt: InstallAttempt, indicator: ProgressIndicator) {
        val sha256 = downloadPackage(modelPackage, attempt.packageFile, indicator)
        indicator.checkCanceled()
        if (sha256 != modelPackage.sha256) {
            throw ModelInstallException(InstallFailure.ChecksumMismatch(modelPackage.address))
        }
    }

    private fun unpackAndCheck(attempt: InstallAttempt, indicator: ProgressIndicator) {
        unpackPackage(attempt.packageFile, attempt.unpackedDirectory, modelPackage, indicator)
        val entries = Files.list(attempt.unpackedDirectory).use { stream -> stream.map { it.fileName.toString() }.toList() }
        if (entries != listOf(model.packageName) || !model.isInPlace(attempt.unpackedDirectory.resolve(model.packageName))) {
            throw ModelInstallException(InstallFailure.CannotUnpack("it does not hold a complete model"))
        }
    }

    // Another IDE may have been faster. Its complete model is as good as ours, and ours is then dropped
    private fun moveIntoPlace(unpackedModel: Path, modelFolder: Path, parent: Path) {
        try {
            Files.move(unpackedModel, modelFolder, StandardCopyOption.ATOMIC_MOVE)
        } catch (exception: IOException) {
            if (model.isInPlace(modelFolder)) return
            throw exception
        }
        flush(parent)
    }

    private fun flushTree(root: Path) {
        Files.walk(root).use { paths -> paths.forEach { flush(it) } }
    }

    private fun flush(path: Path) {
        FileChannel.open(path, StandardOpenOption.READ).use { it.force(true) }
    }
}
