package com.ritenoar.aloudrite

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Random
import kotlin.io.path.relativeTo

// A small package with the shape of the real one: one top directory holding the four files and the directory of
// KokoroModel.kt. A tree maps a relative path to the content of a file, or to null for a directory
class ModelPackageStandIn private constructor(
    val model: SpeechModel,
    val bytes: ByteArray,
    val expectedTree: Map<String, List<Byte>?>,
) {

    val sha256: String = sha256Of(bytes)

    fun modelPackage(address: String) = ModelPackage(
        address = address,
        sizeInBytes = bytes.size.toLong(),
        unpackedSizeInBytes = expectedTree.values.sumOf { (it?.size ?: 0).toLong() },
        sha256 = sha256,
    )

    fun cutOffAtHalf() = ModelPackageStandIn(model, bytes.copyOf(bytes.size / 2), expectedTree)

    fun without(relativePath: String) =
        ModelPackageStandIn(model, archiveOf(model, expectedTree - relativePath), expectedTree - relativePath)

    companion object {
        private const val FILE_SIZE = 50_000

        fun create(model: SpeechModel = INT8_MODEL): ModelPackageStandIn {
            val random = Random(5)
            val files = (fileNamesOf(model) + listOf("espeak-ng-data/en_dict", "LICENSE")).associateWith {
                ByteArray(FILE_SIZE).also { content -> random.nextBytes(content) }.toList()
            }
            val tree: Map<String, List<Byte>?> = files + (MODEL_DATA_DIRECTORY_NAME to null)
            return ModelPackageStandIn(model, archiveOf(model, tree), tree)
        }

        fun sha256Of(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        fun readTree(folder: Path): Map<String, List<Byte>?> = Files.walk(folder).use { paths ->
            paths.filter { it != folder }.toList().associate { path ->
                val content = if (Files.isDirectory(path)) null else Files.readAllBytes(path).toList()
                path.relativeTo(folder).toString().replace('\\', '/') to content
            }
        }

        private fun archiveOf(model: SpeechModel, tree: Map<String, List<Byte>?>): ByteArray {
            val output = ByteArrayOutputStream()
            TarArchiveOutputStream(BZip2CompressorOutputStream(output)).use { tar ->
                tar.putArchiveEntry(TarArchiveEntry("${model.packageName}/"))
                tar.closeArchiveEntry()
                tree.toSortedMap().forEach { (path, content) ->
                    if (content == null) {
                        tar.putArchiveEntry(TarArchiveEntry("${model.packageName}/$path/"))
                    } else {
                        val entry = TarArchiveEntry("${model.packageName}/$path")
                        entry.size = content.size.toLong()
                        tar.putArchiveEntry(entry)
                        tar.write(content.toByteArray())
                    }
                    tar.closeArchiveEntry()
                }
            }
            return output.toByteArray()
        }
    }
}
