package com.ritenoar.aloudrite

import java.io.IOException
import java.nio.file.Path

// Offers the download of a model: takes the lock of the installation, looks again whether the model is in place, asks,
// and hands the attempt to the download. The action that reads and the settings page do the same thing, and differ in
// the question and in what follows. The downloads and the dialogs are handed in so that a test needs neither
class ModelDownloadOffer(
    private val downloads: ModelDownloads,
    private val modelsFolder: Path,
    private val ask: (question: String) -> Boolean,
    private val tell: (message: String) -> Unit,
) {

    // The lock is taken before the question, so that nothing is asked while another IDE window downloads, and it is
    // held until the download ends or the user declines. onAccepted runs after a yes and gives what runs once the
    // model is in place. onFailureRetry is run by the retry of a notification about a failure to take the lock, and
    // asks the question again, since nothing has been agreed to yet. Returns true where the download was started
    fun offer(
        model: SpeechModel,
        question: String,
        onAlreadyInPlace: () -> Unit,
        onAccepted: () -> () -> Unit,
        onFailureRetry: () -> Unit,
    ): Boolean {
        val attempt = try {
            downloads.claim(modelsFolder)
        } catch (exception: IOException) {
            downloads.report(unexpectedFailure(exception), onFailureRetry)
            return false
        }
        if (attempt == null) {
            tell(DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE)
            return false
        }
        var handedOver = false
        try {
            // Another IDE window may have finished between the first look and the lock
            if (model.isInPlace(model.folderIn(modelsFolder))) {
                attempt.close()
                onAlreadyInPlace()
                return false
            }
            if (!ask(question)) return false
            val onModelInPlace = onAccepted()
            handedOver = true
            return downloads.start(model, modelsFolder, attempt, onModelInPlace)
        } finally {
            if (!handedOver) attempt.close()
        }
    }
}
