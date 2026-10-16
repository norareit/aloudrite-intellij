package com.ritenoar.aloudrite

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.components.Service
import java.io.IOException
import java.nio.file.Path

// What the action needs of the download, and nothing more
interface ModelDownloads {
    val isRunning: Boolean

    // Returns false, and starts nothing, while a download is running
    fun start(modelFolder: Path, onModelInPlace: () -> Unit): Boolean
}

// The collaborators are handed in so that a test can run the service without the network and without a window
@Service(Service.Level.APP)
class ModelDownloadService(
    runInBackground: (() -> Unit) -> Unit,
    private val install: (modelFolder: Path, indicator: ProgressIndicator) -> Unit,
    private val reportFailure: (failure: InstallFailure, retry: () -> Unit) -> Unit,
) : ModelDownloads {

    constructor() : this(
        ::runAsCancellableTask,
        { modelFolder, indicator -> ModelInstaller(KOKORO_MODEL_PACKAGE).install(modelFolder, indicator) },
        ::notifyAboutInstallFailure,
    )

    private val oneAtATime = OneAtATime(runInBackground)

    override val isRunning: Boolean get() = oneAtATime.isRunning

    override fun start(modelFolder: Path, onModelInPlace: () -> Unit): Boolean =
        oneAtATime.tryRun { downloadInBackground(modelFolder, onModelInPlace) }

    // What decides the outcome is what this does, and not the callbacks of the task: the platform calls the one for
    // a cancellation also for an indicator that was cancelled after the work ended
    private fun downloadInBackground(modelFolder: Path, onModelInPlace: () -> Unit) {
        try {
            install(modelFolder, ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator())
        } catch (exception: ProcessCanceledException) {
            LOG.info("The user cancelled the download of the speech model")
            throw exception
        } catch (exception: ModelInstallException) {
            return fail(exception.failure, exception, modelFolder, onModelInPlace)
        } catch (exception: IOException) {
            return fail(InstallFailure.Unexpected(exception.message ?: exception.toString()), exception, modelFolder, onModelInPlace)
        }
        onModelInPlace()
    }

    // A warning is not an error report of the IDE, and an error would be one
    private fun fail(failure: InstallFailure, cause: Throwable, modelFolder: Path, onModelInPlace: () -> Unit) {
        LOG.warn("AloudRite could not download the speech model: $failure", cause)
        reportFailure(failure) { start(modelFolder, onModelInPlace) }
    }

    private companion object {
        val LOG = Logger.getInstance(ModelDownloadService::class.java)
        const val TASK_TITLE = "Downloading the AloudRite speech model"

        // Without a project, because the model belongs to the user: the progress then shows in the frame that
        // was focused last, which is the one where the button was pressed
        fun runAsCancellableTask(work: () -> Unit) {
            ProgressManager.getInstance().run(object : Task.Backgroundable(null, TASK_TITLE, true) {
                override fun run(indicator: ProgressIndicator) = work()
            })
        }
    }
}

private const val NOTIFICATION_GROUP_ID = "AloudRite"
private const val NOTIFICATION_TITLE = "AloudRite could not download the speech model"

fun notifyAboutInstallFailure(failure: InstallFailure, retry: () -> Unit) {
    Notification(NOTIFICATION_GROUP_ID, NOTIFICATION_TITLE, failureText(failure), NotificationType.WARNING)
        .addAction(NotificationAction.createSimpleExpiring("Try Again") { retry() })
        .notify(null)
}
