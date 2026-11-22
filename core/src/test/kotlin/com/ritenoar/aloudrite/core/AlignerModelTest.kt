package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Paths

class AlignerModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val files = listOf("final.alimdl", "lda.mat", "tree", "phones.txt", "lexicon.txt", "meta.json")

    private fun folderWith(names: List<String>) =
        temporaryFolder.newFolder().toPath().also { folder -> names.forEach { Files.writeString(folder.resolve(it), "x") } }

    @Test
    fun theModelIsTheEnglishPackageOfTheFaModelsRelease() {
        assertEquals("english-mfa-v3.1.0", ALIGNER_MODEL.packageName)
        assertEquals(
            ModelPackage(
                "https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2",
                47_567_475L, 51_887_069L, "86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da"),
            ALIGNER_MODEL.modelPackage)
    }

    @Test
    fun itsFolderIsInTheModelsFolderUnderThePackageName() {
        assertEquals(Paths.get("models", "english-mfa-v3.1.0"), ALIGNER_MODEL.folderIn(Paths.get("models")))
    }

    @Test
    fun itIsInPlaceOnlyWithAllSixFiles() {
        assertTrue(ALIGNER_MODEL.isInPlace(folderWith(files)))
        for (missing in files) {
            assertFalse(missing, ALIGNER_MODEL.isInPlace(folderWith(files - missing)))
        }
    }

    @Test
    fun aFolderThatIsNotThereIsNotInPlace() {
        assertFalse(ALIGNER_MODEL.isInPlace(temporaryFolder.root.toPath().resolve("none")))
    }
}
