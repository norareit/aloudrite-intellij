package com.ritenoar.aloudrite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.messages.Topic
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// Told when a download starts or ends and when a model was removed, so that a page shows what is on the machine
fun interface SpeechModelsChangeListener {
    fun modelsChanged()
}

val SPEECH_MODELS_CHANGED: Topic<SpeechModelsChangeListener> =
    Topic.create("AloudRite speech models changed", SpeechModelsChangeListener::class.java)

fun publishModelsChanged() {
    ApplicationManager.getApplication().messageBus.syncPublisher(SPEECH_MODELS_CHANGED).modelsChanged()
}

// What the settings page needs of the management of the models, and nothing more
interface ManagedSpeechModels {
    val isDownloadRunning: Boolean

    // The model that was chosen and is being downloaded, and becomes the model in use once it is in place
    val pendingChoice: SpeechModel?

    // The model that the download which runs is fetching, started from the page or by a press of "Read Aloud", and null
    // where none runs
    val downloadingModel: SpeechModel?

    fun isInPlace(model: SpeechModel): Boolean

    fun canRemove(model: SpeechModel): Boolean

    // Asks, then downloads in the background. Does not change the model in use
    fun download(model: SpeechModel)

    // Makes the model the model in use at once where it is in place, and otherwise asks whether to download it and use
    // it once it is in place. Returns false where nothing was chosen or started, such as after a no
    fun choose(model: SpeechModel): Boolean

    fun remove(model: SpeechModel)
}

// The collaborators are handed in so that a test needs no network, no window and no reading
@Service(Service.Level.APP)
class SpeechModelManagement(
    private val modelInUse: () -> SpeechModel,
    private val chooseModel: (SpeechModel) -> Unit,
    private val downloads: ModelDownloads,
    private val readingModel: ReadingModel,
    private val modelsFolder: Path,
    private val ask: (question: String, yesText: String, noText: String) -> Boolean,
    private val tell: (message: String) -> Unit,
    private val runInBackground: (() -> Unit) -> Unit,
    private val onModelsChanged: () -> Unit,
) : ManagedSpeechModels {

    constructor() : this(
        { service<SpeechModelSetting>().modelInUse },
        { model -> service<SpeechModelSetting>().choose(model) },
        service<ModelDownloadService>(),
        service<SpeechService>(),
        userModelsFolder(),
        { question, yesText, noText ->
            Messages.showYesNoDialog(NO_PROJECT, question, DIALOG_TITLE, yesText, noText, Messages.getQuestionIcon()) ==
                Messages.YES
        },
        { message -> Messages.showInfoMessage(NO_PROJECT, message, DIALOG_TITLE) },
        { work -> AppExecutorUtil.getAppExecutorService().execute(work) },
        ::publishModelsChanged,
    )

    // Changed only under the monitor
    private var pending: SpeechModel? = null

    override val isDownloadRunning: Boolean get() = downloads.isRunning

    // A choice whose download failed or was cancelled is no longer pending, which shows as the download not running
    override val pendingChoice: SpeechModel?
        get() = synchronized(this) {
            if (!downloads.isRunning) pending = null
            pending
        }

    override val downloadingModel: SpeechModel? get() = downloads.downloadingModel

    override fun isInPlace(model: SpeechModel): Boolean = model.isInPlace(model.folderIn(modelsFolder))

    override fun canRemove(model: SpeechModel): Boolean =
        isInPlace(model) && model != modelInUse() && model != readingModel.modelOfTheReading()

    override fun download(model: SpeechModel) {
        if (isInPlace(model)) return onModelsChanged()
        if (downloads.isRunning) return tell(DOWNLOAD_RUNNING_HERE_MESSAGE)
        offer(model, downloadQuestionFromThePage(model, model.folderIn(modelsFolder)),
            onAlreadyInPlace = onModelsChanged, onAccepted = { {} }, onFailureRetry = { download(model) })
    }

    override fun choose(model: SpeechModel): Boolean {
        if (isInPlace(model)) return chooseTheModelInPlace(model)
        if (downloads.isRunning) return false.also { tell(DOWNLOAD_RUNNING_HERE_MESSAGE) }
        val started = offer(
            model, chooseQuestion(model, model.folderIn(modelsFolder)),
            onAlreadyInPlace = { chooseTheModelInPlace(model) },
            onAccepted = { acceptTheChoice(model) },
            onFailureRetry = { choose(model) },
        )
        if (!started) forgetPending(model)
        return started || modelInUse() == model
    }

    override fun remove(model: SpeechModel) {
        if (model == modelInUse() || model == readingModel.modelOfTheReading()) return tell(removeRefusedMessage(model))
        val folder = model.folderIn(modelsFolder)
        if (!ask(removeQuestion(model, folder), "Remove", "Keep")) return
        val attempt = claimForRemoval() ?: return
        val movedAside = try {
            moveAside(model, folder, attempt)
        } catch (exception: IOException) {
            attempt.close()
            return tell(failureText(unexpectedFailure(exception)))
        } catch (exception: Throwable) {
            attempt.close()
            throw exception
        }
        if (!movedAside) {
            attempt.close()
            return tell(removeRefusedNotThePluginsMessage(folder))
        }
        deleteInBackground(attempt)
        onModelsChanged()
    }

    private fun chooseTheModelInPlace(model: SpeechModel): Boolean {
        synchronized(this) { pending = null }
        chooseModel(model)
        return true
    }

    // Returns what runs once the model is in place
    private fun acceptTheChoice(model: SpeechModel): () -> Unit {
        synchronized(this) { pending = model }
        return { useOnceInPlace(model) }
    }

    // A choice that a later one replaced is not carried out when its download ends
    private fun useOnceInPlace(model: SpeechModel) {
        val stillPending = synchronized(this) { (pending == model).also { if (it) pending = null } }
        if (stillPending) chooseModel(model)
    }

    private fun forgetPending(model: SpeechModel) {
        synchronized(this) { if (pending == model) pending = null }
    }

    private fun offer(
        model: SpeechModel,
        question: String,
        onAlreadyInPlace: () -> Unit,
        onAccepted: () -> () -> Unit,
        onFailureRetry: () -> Unit,
    ): Boolean {
        return ModelDownloadOffer(downloads, modelsFolder, { ask(it, "Download", "Not Now") }, tell)
            .offer(model, question, onAlreadyInPlace, onAccepted, onFailureRetry)
    }

    // The lock of the installation also keeps a removal from meeting a download, in this IDE and in another
    private fun claimForRemoval(): InstallAttempt? {
        val attempt = try {
            downloads.claim(modelsFolder)
        } catch (exception: IOException) {
            return null.also { tell(failureText(unexpectedFailure(exception))) }
        }
        if (attempt == null) tell(if (downloads.isRunning) DOWNLOAD_RUNNING_HERE_MESSAGE else DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE)
        return attempt
    }

    // One rename into the directory of the attempt, so that no half removed model is ever in place. What the folder
    // holds is looked at again with the lock held, because the user may have changed it since the question
    private fun moveAside(model: SpeechModel, folder: Path, attempt: InstallAttempt): Boolean {
        if (!model.isInPlace(folder)) return false
        attempt.prepareDirectory()
        Files.move(folder, attempt.directory.resolve(model.packageName), StandardCopyOption.ATOMIC_MOVE)
        return true
    }

    // The deletion of hundreds of megabytes does not hold the thread of the page. Closing the attempt deletes what it
    // wrote, which is the model, and releases the lock
    private fun deleteInBackground(attempt: InstallAttempt) {
        try {
            runInBackground { attempt.close() }
        } catch (exception: Throwable) {
            attempt.close()
            throw exception
        }
    }

    private companion object {
        const val DIALOG_TITLE = "AloudRite"
        val NO_PROJECT: Project? = null
    }
}
