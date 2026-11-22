package com.ritenoar.aloudrite.core

import java.nio.file.Path

// What the plugin downloads and puts in place as a folder of the models folder: a speech model or the aligner model. The
// installer, the offer and the download service work on this and know none of them. The package name is also the name of
// the folder
interface InstallablePackage {
    val displayName: String
    val packageName: String

    // What the progress of the download and of the unpacking says it is working on, as "the speech model"
    val progressName: String
    val modelPackage: ModelPackage

    fun folderIn(modelsFolder: Path): Path = modelsFolder.resolve(packageName)

    // Only looks whether the entries exist, since the native code may end the whole IDE process when it is given a model
    // that is not there
    fun isInPlace(modelFolder: Path): Boolean
}
