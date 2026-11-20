package com.ritenoar.aloudrite

import com.github.csukuangfj.piper.phonemize.PiperPhonemize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val ESPEAK_VOICE = "en-us"

// The only class that names a class of piper-phonemize. eSpeak NG gives the phonemes of the text of a piece.
//
// eSpeak NG can be initialized once in a process, with the folder that holds `espeak-ng-data`, and it reads the files of
// that folder again at every call. Initialized with the folder of a model that is removed later, it would give nothing for
// every word. So it is initialized with a folder of the plugin's own that holds one link, `espeak-ng-data`, which
// points at the data of the model in use, and the link is moved to the model of each new engine
object EspeakPhonemes {

    private var linkFolder: Path? = null

    // Points eSpeak NG at the data of the model, which is the folder `espeak-ng-data` of the model folder. A failed
    // initialization names the folder
    @Synchronized
    fun useDataOf(espeakDataFolder: Path) {
        val folder = linkFolder
        if (folder == null) initializeWith(espeakDataFolder) else pointTheLinkAt(folder, espeakDataFolder)
    }

    // The phonemes of each sentence of the text, as code points, and none where the call failed
    @Synchronized
    fun sentencesOf(text: String): List<IntArray> {
        checkNotNull(linkFolder) { "eSpeak NG has no data folder yet, see useDataOf" }
        val result = PiperPhonemize.text(text, ESPEAK_VOICE) ?: return emptyList()
        return result.use { phonemes -> List(phonemes.numSentences) { phonemes.getPhonemes(it) } }
    }

    private fun initializeWith(espeakDataFolder: Path) {
        val folder = Files.createTempDirectory("aloudrite-espeak-ng")
        folder.toFile().deleteOnExit()
        val link = folder.resolve(MODEL_DATA_DIRECTORY_NAME)
        Files.createSymbolicLink(link, espeakDataFolder)
        // Deleted in the reverse order of the registration, so the link goes before its folder
        link.toFile().deleteOnExit()
        check(PiperPhonemize.initializeWithDataDir(folder.toString()) >= 0) {
            "eSpeak NG could not be initialized with the data of $espeakDataFolder, through $folder"
        }
        linkFolder = folder
    }

    // The new link is made under another name and moved over the old one, so the link never points nowhere
    private fun pointTheLinkAt(folder: Path, espeakDataFolder: Path) {
        val newLink = folder.resolve("$MODEL_DATA_DIRECTORY_NAME.new")
        Files.deleteIfExists(newLink)
        Files.createSymbolicLink(newLink, espeakDataFolder)
        Files.move(newLink, folder.resolve(MODEL_DATA_DIRECTORY_NAME), StandardCopyOption.ATOMIC_MOVE)
    }
}
