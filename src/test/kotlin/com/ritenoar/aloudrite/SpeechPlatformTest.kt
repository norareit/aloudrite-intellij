package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechPlatformTest {

    @Test
    fun speechIsSupportedOnLinuxX64AndOnMacOnAppleSilicon() {
        assertEquals(true, isSpeechSupported("Linux", "amd64"))
        assertEquals(true, isSpeechSupported("Linux", "x86_64"))
        assertEquals(true, isSpeechSupported("Mac OS X", "aarch64"))
        assertEquals(true, isSpeechSupported("Mac OS X", "arm64"))
    }

    @Test
    fun speechIsNotSupportedOnAnyOtherPlatform() {
        assertEquals(false, isSpeechSupported("Linux", "aarch64"))
        assertEquals(false, isSpeechSupported("Mac OS X", "x86_64"))
        assertEquals(false, isSpeechSupported("Windows 11", "amd64"))
        assertEquals(false, isSpeechSupported("FreeBSD", "amd64"))
    }
}
