package com.ritenoar.aloudrite

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

// Whether the words being read are highlighted, for the whole IDE and kept across restarts. It is on until it is cleared,
// so that a state stored before the setting existed leaves it on
@Service(Service.Level.APP)
@State(name = "AloudRiteHighlights", storages = [Storage("aloudrite.xml")])
class HighlightSetting : PersistentStateComponent<HighlightSetting.StoredHighlights> {

    class StoredHighlights {
        var highlights: Boolean = true
    }

    // Read by the highlighter from the event thread and set by the settings page
    @Volatile
    var highlights: Boolean = true

    override fun getState(): StoredHighlights = StoredHighlights().also { it.highlights = highlights }

    override fun loadState(state: StoredHighlights) {
        highlights = state.highlights
    }
}
