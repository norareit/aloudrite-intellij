package com.ritenoar.aloudrite

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// What is read. The same file in another project is another target, since its tab and its stop button are others
data class ReadingTarget(val project: Project?, val file: VirtualFile)
