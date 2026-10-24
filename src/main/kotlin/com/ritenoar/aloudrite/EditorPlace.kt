package com.ritenoar.aloudrite

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

// Where the person is in the text of the file that the editor shows. Called under a read action. Only the primary caret
// and its selection count, and nothing is changed. TextEditorWithPreview is a class of the platform, which the Markdown
// plugin opens, so that the plugin needs no class of the Markdown plugin
fun placeShownBy(fileEditor: FileEditor?): PlaceInTheText {
    if (fileEditor is TextEditorWithPreview && fileEditor.getLayout() == TextEditorWithPreview.Layout.SHOW_PREVIEW) {
        return PlaceInTheText.NoText
    }
    val editor = (fileEditor as? TextEditor)?.editor ?: return PlaceInTheText.NoText
    val caret = editor.caretModel.primaryCaret
    if (caret.hasSelection() && caret.selectionStart < caret.selectionEnd) {
        return PlaceInTheText.Selection(caret.selectionStart, caret.selectionEnd)
    }
    return PlaceInTheText.Caret(caret.offset)
}
