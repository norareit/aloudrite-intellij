package com.ritenoar.aloudrite

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class PackageUnpackingTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIn = ModelPackageStandIn.create()

    @Test
    fun thePackageUnpacksToExactlyTheExpectedTree() {
        val target = temporaryFolder.newFolder().toPath().resolve("unpacked")

        unpackPackage(packageFileOf(standIn), target, standIn.modelPackage("unused"), EmptyProgressIndicator())

        assertEquals(setOf(INT8_MODEL.packageName), Files.list(target).use { entries -> entries.map { it.fileName.toString() }.toList().toSet() })
        assertEquals(standIn.expectedTree, ModelPackageStandIn.readTree(target.resolve(INT8_MODEL.packageName)))
    }

    @Test
    fun aCutOffPackageCannotBeUnpacked() {
        val cutOff = standIn.cutOffAtHalf()
        val target = temporaryFolder.newFolder().toPath().resolve("unpacked")

        try {
            unpackPackage(packageFileOf(cutOff), target, cutOff.modelPackage("unused"), EmptyProgressIndicator())
            fail("The cut-off package was unpacked")
        } catch (exception: ModelInstallException) {
            assertEquals(InstallFailure.CannotUnpack::class, exception.failure::class)
        }
    }

    @Test
    fun anIndicatorCancelledDuringTheUnpackingStopsIt() {
        val target = temporaryFolder.newFolder().toPath().resolve("unpacked")
        val cancelsWhileUnpacking = object : ProgressIndicatorBase() {
            override fun setFraction(fraction: Double) {
                super.setFraction(fraction)
                if (fraction > 0.0 && text == "Unpacking the speech model") cancel()
            }
        }

        try {
            unpackPackage(packageFileOf(standIn), target, standIn.modelPackage("unused"), cancelsWhileUnpacking)
            fail("The unpacking was not cancelled")
        } catch (_: ProcessCanceledException) {
        }
    }

    private fun packageFileOf(modelPackage: ModelPackageStandIn): Path {
        val file = temporaryFolder.newFile("package.tar.bz2").toPath()
        Files.write(file, modelPackage.bytes)
        return file
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
