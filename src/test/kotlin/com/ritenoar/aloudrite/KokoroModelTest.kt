package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class KokoroModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun theModelFolderOnMacIsInApplicationSupportWhateverXdgDataHomeIs() {
        val expected = Paths.get("/Users/someone/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0")

        assertEquals(expected, INT8_MODEL.folderIn(userModelsFolder("Mac OS X", "/Users/someone", null)))
        assertEquals(expected, INT8_MODEL.folderIn(userModelsFolder("Mac OS X", "/Users/someone", "/data")))
    }

    @Test
    fun theModelFolderOnLinuxIsUnderAnAbsoluteXdgDataHome() {
        val expected = Paths.get("/data/aloudrite/kokoro-int8-multi-lang-v1_0")

        assertEquals(expected, INT8_MODEL.folderIn(userModelsFolder("Linux", "/home/someone", "/data")))
    }

    @Test
    fun theModelFolderOnLinuxIsUnderLocalShareWhenXdgDataHomeIsNullEmptyOrRelative() {
        val expected = Paths.get("/home/someone/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0")

        assertEquals(expected, INT8_MODEL.folderIn(userModelsFolder("Linux", "/home/someone", null)))
        assertEquals(expected, INT8_MODEL.folderIn(userModelsFolder("Linux", "/home/someone", "")))
        assertEquals(expected, INT8_MODEL.folderIn(userModelsFolder("Linux", "/home/someone", "relative/data")))
    }

    @Test
    fun aFolderThatDoesNotExistHoldsNoModel() {
        assertFalse(INT8_MODEL.isInPlace(temporaryFolder.root.toPath().resolve("absent")))
    }

    @Test
    fun anEmptyFolderHoldsNoModel() {
        assertFalse(INT8_MODEL.isInPlace(temporaryFolder.root.toPath()))
    }

    @Test
    fun aFolderWithOneEntryMissingHoldsNoModel() {
        val entries = listOf("model.int8.onnx", "voices.bin", "tokens.txt", "lexicon-us-en.txt", "espeak-ng-data")

        entries.forEach { missing ->
            val folder = temporaryFolder.newFolder().toPath()
            fillWithModelStandIn(folder)
            deleteRecursively(folder.resolve(missing))

            assertFalse("A folder without $missing counts as a model", INT8_MODEL.isInPlace(folder))
        }
    }

    @Test
    fun aFolderWithAllFiveEntriesHoldsTheModel() {
        val folder = temporaryFolder.newFolder().toPath()
        fillWithModelStandIn(folder)

        assertTrue(INT8_MODEL.isInPlace(folder))
    }

    private fun deleteRecursively(path: Path) {
        Files.walk(path).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }
}
