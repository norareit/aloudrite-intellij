package com.ritenoar.aloudrite

private val LINUX_X64_ARCHITECTURES = setOf("amd64", "x86_64")
private val MAC_APPLE_SILICON_ARCHITECTURES = setOf("aarch64", "arm64")

// These are the platforms of the two native libraries the plugin archive holds, named as the binding names them
fun isSpeechSupported(operatingSystemName: String, architectureName: String): Boolean = when {
    operatingSystemName.startsWith("Linux") -> architectureName in LINUX_X64_ARCHITECTURES
    operatingSystemName.startsWith("Mac") -> architectureName in MAC_APPLE_SILICON_ARCHITECTURES
    else -> false
}
