package com.ritenoar.aloudrite

import com.intellij.ide.HelpTooltip
import com.intellij.openapi.keymap.KeymapUtil

// A tooltip in the style of the IDE's toolbar buttons, which names the first keyboard shortcut the action has in the
// keymap in use, and is the title alone where the action has none or no action is given
fun shortcutTooltip(title: String, actionId: String?): HelpTooltip {
    val tooltip = HelpTooltip().setPlainTextTitle(title)
    val shortcut = actionId?.let(KeymapUtil::getFirstKeyboardShortcutText)
    if (!shortcut.isNullOrEmpty()) tooltip.setShortcut(shortcut)
    return tooltip
}
