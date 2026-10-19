package com.ritenoar.aloudrite

// The Java binding of sherpa-onnx hands the text to its native code in modified UTF-8, in which a character above
// U+FFFF arrives as the two halves of its surrogate pair and never intact, and the engine makes about 5.7 seconds
// of babble for each one. A space leaves it out without joining the words on its two sides. A half of a pair that
// stands alone is left as it is
fun withCharactersAboveFfffAsSpaces(text: String): String {
    val result = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (codePoint > 0xFFFF) result.append(' ') else result.appendCodePoint(codePoint)
        index += Character.charCount(codePoint)
    }
    return result.toString()
}
