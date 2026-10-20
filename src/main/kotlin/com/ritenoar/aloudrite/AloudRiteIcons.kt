package com.ritenoar.aloudrite

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

// The icons drawn for the plugin. The pause and the stop are the IDE's own, so that they follow its restyling
object AloudRiteIcons {
    @JvmField
    val ReadAloud: Icon = IconLoader.getIcon("/icons/readAloud.svg", AloudRiteIcons::class.java)

    @JvmField
    val ResumeReading: Icon = IconLoader.getIcon("/icons/resumeReading.svg", AloudRiteIcons::class.java)

    @JvmField
    val ToolWindow: Icon = IconLoader.getIcon("/icons/toolWindow.svg", AloudRiteIcons::class.java)
}
