package com.ritenoar.aloudrite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic

// Told of every change of the model in use, on whichever thread it was chosen
fun interface SpeechModelChangeListener {
    fun modelChanged(model: SpeechModel)
}

val SPEECH_MODEL_CHANGED: Topic<SpeechModelChangeListener> =
    Topic.create("AloudRite speech model changed", SpeechModelChangeListener::class.java)

// The one model in use of the IDE, kept across restarts. Only a choice is stored. Until the user chooses, the model in
// use is the default of the version of the plugin that runs, so a release with another default moves the listeners who
// never chose. The collaborator is handed in so that a test can choose without the bus of the application
@Service(Service.Level.APP)
@State(name = "AloudRiteSpeechModel", storages = [Storage("aloudrite.xml")])
class SpeechModelSetting(
    private val defaultModel: SpeechModel,
    private val onChange: (model: SpeechModel) -> Unit,
) : PersistentStateComponent<SpeechModelSetting.StoredModel> {

    constructor() : this(defaultSpeechModel(System.getProperty("os.name")), { model ->
        service<SpeechService>().modelChanged()
        ApplicationManager.getApplication().messageBus.syncPublisher(SPEECH_MODEL_CHANGED).modelChanged(model)
    })

    class StoredModel {
        var packageName: String? = null
    }

    @Volatile
    private var chosen: SpeechModel? = null

    val modelInUse: SpeechModel get() = chosen ?: defaultModel

    // A choice of the model that is in use already is no change, but it is stored, also where it is the default
    fun choose(model: SpeechModel) {
        val changed = synchronized(this) {
            (model != modelInUse).also { chosen = model }
        }
        if (changed) onChange(model)
    }

    override fun getState(): StoredModel? = chosen?.let { model -> StoredModel().also { it.packageName = model.packageName } }

    // The file can be edited by hand, so a name of no model is taken as no choice
    override fun loadState(state: StoredModel) {
        chosen = SPEECH_MODELS.firstOrNull { it.packageName == state.packageName }
    }
}
