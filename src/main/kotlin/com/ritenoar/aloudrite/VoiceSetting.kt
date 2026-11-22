package com.ritenoar.aloudrite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.ritenoar.aloudrite.core.DEFAULT_VOICE
import com.ritenoar.aloudrite.core.Voice
import com.ritenoar.aloudrite.core.voiceNamed

// Told of every change of the voice, on whichever thread it was chosen
fun interface VoiceChangeListener {
    fun voiceChanged(voice: Voice)
}

val VOICE_CHANGED: Topic<VoiceChangeListener> =
    Topic.create("AloudRite voice changed", VoiceChangeListener::class.java)

// The one voice of the IDE, kept across restarts. A reading takes the voice when it starts, and a choice makes no engine
// and frees none, since one engine serves every voice, so the speech service is told nothing. The collaborator is handed
// in so that a test can choose without the bus of the application
@Service(Service.Level.APP)
@State(name = "AloudRiteVoice", storages = [Storage("aloudrite.xml")])
class VoiceSetting(
    private val onChange: (voice: Voice) -> Unit,
) : PersistentStateComponent<VoiceSetting.StoredVoice> {

    constructor() : this({ voice ->
        ApplicationManager.getApplication().messageBus.syncPublisher(VOICE_CHANGED).voiceChanged(voice)
    })

    class StoredVoice {
        var kokoroName: String? = null
    }

    @Volatile
    var voice: Voice = DEFAULT_VOICE
        private set

    // A choice of the voice held is no change
    fun choose(newVoice: Voice) {
        synchronized(this) {
            if (newVoice == voice) return
            voice = newVoice
        }
        onChange(newVoice)
    }

    override fun getState(): StoredVoice = StoredVoice().also { it.kokoroName = voice.kokoroName }

    // The file can be edited by hand, and a later version may drop a voice, so a name of no voice is read as Heart
    override fun loadState(state: StoredVoice) {
        voice = state.kokoroName?.let(::voiceNamed) ?: DEFAULT_VOICE
    }
}
