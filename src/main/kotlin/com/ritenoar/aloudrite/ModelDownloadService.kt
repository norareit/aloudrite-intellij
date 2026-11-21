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

    // The model of the download that runs, whoever started it, and null where none runs
    val downloadingModel: InstallablePackage?

    // Takes the lock of the installation before anything is asked or touched. Returns null, and starts nothing, when
    // another IDE window holds it. The caller closes the attempt unless it hands it to start
    fun claim(modelsFolder: Path): InstallAttempt?

    // Takes over the attempt and closes it when the work ends, also when this returns false, which it does, and
    // starts nothing, while a download is running
    fun start(model: InstallablePackage, modelsFolder: Path, attempt: InstallAttempt, onModelInPlace: () -> Unit): Boolean

    // Tells the user of a failure in a notification that offers to try again
    fun report(failure: InstallFailure, retry: () -> Unit)
}

// The collaborators are handed in so that a test can run the service without the network and without a window
@Service(Service.Level.APP)
class ModelDownloadService(
    runInBackground: (() -> Unit) -> Unit,
    private val install: (model: InstallablePackage, modelFolder: Path, attempt: InstallAttempt, indicator: ProgressIndicator) -> Unit,
    private val reportFailure: (failure: InstallFailure, retry: () -> Unit) -> Unit,
    onRunningChanged: () -> Unit,
    // What is installed after a package in the same attempt, which a package that is in place is left out of
    private val installedWith: (InstallablePackage) -> List<InstallablePackage> = { emptyList() },
) : ModelDownloads {

    constructor() : this(
        ::runAsCancellableTask,
        { model, modelFolder, attempt, indicator -> ModelInstaller(model).install(modelFolder, attempt, indicator) },
        ::notifyAboutInstallFailure,
        ::publishModelsChanged,
        ::alignerModelWithASpeechModel,
    )

    private val oneAtATime = OneAtATime(runInBackground, onRunningChanged)

    override val isRunning: Boolean get() = oneAtATime.isRunning

    @Volatile
    private var startedModel: InstallablePackage? = null

    // A model left over from a download that ended is not reported, since isRunning is read first
    override val downloadingModel: InstallablePackage? get() = startedModel.takeIf { isRunning }

    override fun claim(modelsFolder: Path): InstallAttempt? = InstallAttempt.claim(modelsFolder.toAbsolutePath())

    override fun report(failure: InstallFailure, retry: () -> Unit) = reportFailure(failure, retry)

    override fun start(model: InstallablePackage, modelsFolder: Path, attempt: InstallAttempt, onModelInPlace: () -> Unit): Boolean {
        val started = try {
            oneAtATime.tryRun { downloadInBackground(model, modelsFolder, attempt, onModelInPlace) }
        } catch (exception: Throwable) {
            attempt.close()
            throw exception
        }
        if (!started) attempt.close() else startedModel = model
        return started
    }

    // What decides the outcome is what this does, and not the callbacks of the task: the platform calls the one for
    // a cancellation also for an indicator that was cancelled after the work ended. The attempt is closed before the
    // speech starts, so the lock is not held while the sentence is spoken. A package that is installed with the model
    // does not hold back what follows the model where it fails: the failure is reported with a retry of its own
    private fun downloadInBackground(model: InstallablePackage, modelsFolder: Path, attempt: InstallAttempt, onModelInPlace: () -> Unit) {
        val companions = installedWith(model).filterNot { it.isInPlace(it.folderIn(modelsFolder)) }
        val failureOfACompanion = try {
            attempt.use {
                installOne(model, modelsFolder, it)
                companions.firstNotNullOfOrNull { companion -> failureOfInstalling(companion, modelsFolder, it) }
            }
        } catch (exception: ProcessCanceledException) {
            LOG.info("The user cancelled the download of the speech model")
            throw exception
        } catch (exception: ModelInstallException) {
            return fail(exception.failure, exception, model, modelsFolder, onModelInPlace)
        } catch (exception: IOException) {
            return fail(unexpectedFailure(exception), exception, model, modelsFolder, onModelInPlace)
        }
        failureOfACompanion?.let { (companion, failure, cause) -> fail(failure, cause, companion, modelsFolder) {} }
        onModelInPlace()
    }

    private fun installOne(model: InstallablePackage, modelsFolder: Path, attempt: InstallAttempt) {
        startedModel = model
        install(model, model.folderIn(modelsFolder), attempt, ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator())
    }

    private data class CompanionFailure(val companion: InstallablePackage, val failure: InstallFailure, val cause: Throwable)

    // Null where it was installed. A cancellation goes on to the caller, as for the model
    private fun failureOfInstalling(companion: InstallablePackage, modelsFolder: Path, attempt: InstallAttempt): CompanionFailure? =
        try {
            installOne(companion, modelsFolder, attempt)
            null
        } catch (exception: ModelInstallException) {
            CompanionFailure(companion, exception.failure, exception)
        } catch (exception: IOException) {
            CompanionFailure(companion, unexpectedFailure(exception), exception)
        }

    private fun retry(model: InstallablePackage, modelsFolder: Path, onModelInPlace: () -> Unit) {
        val attempt = try {
            claim(modelsFolder)
        } catch (exception: IOException) {
            return fail(unexpectedFailure(exception), exception, model, modelsFolder, onModelInPlace)
        } ?: return reportFailure(InstallFailure.RunningElsewhere) { retry(model, modelsFolder, onModelInPlace) }
        start(model, modelsFolder, attempt, onModelInPlace)
    }

    // A warning is not an error report of the IDE, and an error would be one
    private fun fail(
        failure: InstallFailure, cause: Throwable, model: InstallablePackage, modelsFolder: Path, onModelInPlace: () -> Unit,
    ) {
        LOG.warn("AloudRite could not download ${model.progressName}: $failure", cause)
        reportFailure(failure) { retry(model, modelsFolder, onModelInPlace) }
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

// The aligner model is downloaded with every speech model that is not downloaded on its own, where it is not on the machine
fun alignerModelWithASpeechModel(model: InstallablePackage): List<InstallablePackage> =
    if (model is SpeechModel) listOf(ALIGNER_MODEL) else emptyList()

const val NOTIFICATION_GROUP_ID = "AloudRite"
private const val NOTIFICATION_TITLE = "AloudRite could not download a model"

fun notifyAboutInstallFailure(failure: InstallFailure, retry: () -> Unit) {
    Notification(NOTIFICATION_GROUP_ID, NOTIFICATION_TITLE, failureText(failure), NotificationType.WARNING)
        .addAction(NotificationAction.createSimpleExpiring("Try Again") { retry() })
        .notify(null)
}
