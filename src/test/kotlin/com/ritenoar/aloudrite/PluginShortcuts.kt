package com.ritenoar.aloudrite

// "Mac OS X 10.5+" is the keymap the IDE names "macOS"
const val MAC_KEYMAP = "Mac OS X 10.5+"

// The keystroke the plugin gives each of its actions in the default keymaps of Linux, as plugin.xml declares it. The
// tests write it down themselves rather than read it from a keymap, so that a wrong shortcut in plugin.xml fails them
val PLUGIN_KEYSTROKES_OF_LINUX = listOf(
    READ_ALOUD_ACTION_ID to "ctrl alt shift SPACE",
    STOP_READING_ACTION_ID to "ctrl alt shift O",
    READ_SLOWER_ACTION_ID to "ctrl alt shift COMMA",
    READ_FASTER_ACTION_ID to "ctrl alt shift PERIOD",
)

// macOS has the keys of Linux with Command for Control
fun pluginKeystroke(keystrokeOfLinux: String, inTheKeymapOfMacOs: Boolean): String =
    if (inTheKeymapOfMacOs) keystrokeOfLinux.replace("ctrl ", "meta ") else keystrokeOfLinux
