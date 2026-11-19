package com.ritenoar.aloudrite

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

// Which views show highlights and which highlights they show. They are read together, so that a refresh never sees half
// of an apply
data class HighlightChoices(
    val inTextEditor: Boolean = true,
    val inPreview: Boolean = true,
    val sentence: Boolean = true,
    val word: Boolean = false,
) {
    // The choice of the highlights acts only within the views that show highlights
    val highlightsCanBeChosen: Boolean get() = inTextEditor || inPreview
}

// The choices of the highlights, for the whole IDE and kept across restarts. The word is off until it is checked, since it
// is an estimate, and the rest are on until they are cleared. A state stored by an earlier version loads as the defaults
@Service(Service.Level.APP)
@State(name = "AloudRiteHighlights", storages = [Storage("aloudrite.xml")])
class HighlightSetting : PersistentStateComponent<HighlightSetting.StoredHighlights> {

    class StoredHighlights {
        var inTextEditor: Boolean = true
        var inPreview: Boolean = true
        var sentence: Boolean = true
        var word: Boolean = false
    }

    // Read by the highlighter from the event thread and set by the settings page
    @Volatile
    var choices: HighlightChoices = HighlightChoices()

    override fun getState(): StoredHighlights = StoredHighlights().also {
        val current = choices
        it.inTextEditor = current.inTextEditor
        it.inPreview = current.inPreview
        it.sentence = current.sentence
        it.word = current.word
    }

    override fun loadState(state: StoredHighlights) {
        choices = HighlightChoices(state.inTextEditor, state.inPreview, state.sentence, state.word)
    }
}
