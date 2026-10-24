package com.ritenoar.aloudrite

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.runInEdtAndWait
import org.junit.rules.ExternalResource
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

// A file editor that shows no text, as the editor of a file that is not text, which gives no place in the text
fun editorWithoutText(file: VirtualFile): FileEditor = FileEditorWithoutText(file)

private class FileEditorWithoutText(private val shownFile: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val panel = JPanel()
    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = panel
    override fun getName(): String = "Without text"
    override fun setState(state: FileEditorState) = Unit
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun getFile(): VirtualFile = shownFile
    override fun dispose() = Unit
}

// Text editors on the documents of files, and previews, with the carets and selections of a test, all released after it
class EditorsOfATest : ExternalResource() {

    private val made = mutableListOf<Editor>()

    override fun after() {
        runInEdtAndWait { made.forEach { EditorFactory.getInstance().releaseEditor(it) } }
        made.clear()
    }

    // Its caret is at the start of the file
    fun textEditorOf(file: VirtualFile): TextEditor {
        val editor = runEditorOn(file)
        return TextEditorProvider.getInstance().getTextEditor(editor)
    }

    fun textEditorWithCaretAt(file: VirtualFile, offset: Int): TextEditor =
        textEditorOf(file).also { runInEdtAndWait { it.editor.caretModel.moveToOffset(offset) } }

    fun textEditorWithSelection(file: VirtualFile, start: Int, end: Int): TextEditor =
        textEditorOf(file).also { runInEdtAndWait { it.editor.selectionModel.setSelection(start, end) } }

    // The last of the carets is the primary one, as the platform makes it. Each is at its offset, or at the end of its selection that begins there
    fun textEditorWithCarets(file: VirtualFile, carets: List<Pair<Int, Int?>>): TextEditor =
        textEditorOf(file).also { textEditor ->
            runInEdtAndWait {
                val editor = textEditor.editor
                editor.caretModel.setCaretsAndSelections(carets.map { (offset, selectionEnd) ->
                    val start = editor.offsetToLogicalPosition(offset)
                    val end = selectionEnd?.let(editor::offsetToLogicalPosition) ?: start
                    CaretState(end, start, end)
                })
            }
        }

    fun textEditorWithABlockSelection(file: VirtualFile, from: LogicalPosition, to: LogicalPosition): TextEditor =
        textEditorOf(file).also { runInEdtAndWait { it.editor.selectionModel.setBlockSelection(from, to) } }

    // The editor that the Markdown plugin opens for a file, which shows its preview alone
    fun previewOnlyEditorOf(file: VirtualFile): FileEditor {
        val text = textEditorOf(file)
        return object : TextEditorWithPreview(text, FileEditorWithoutText(file), "Markdown", Layout.SHOW_PREVIEW) {
            override fun getFile(): VirtualFile = file
        }
    }

    private fun runEditorOn(file: VirtualFile): Editor {
        var editor: Editor? = null
        runInEdtAndWait {
            editor = EditorFactory.getInstance().createEditor(FileDocumentManager.getInstance().getDocument(file)!!)
                .also { made += it }
        }
        return editor!!
    }
}
