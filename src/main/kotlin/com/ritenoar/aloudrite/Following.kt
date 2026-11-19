package com.ritenoar.aloudrite

// Whether an editor follows the reading, which is to scroll by itself to keep the word heard in view. It follows from the
// start of a reading and from every resume, and stops at the first scroll or caret move of the user. It knows nothing of
// editors: it is told what happens in one
class Following(originX: Int, originY: Int) {

    var isFollowing = true
        private set

    private var originX = originX
    private var originY = originY
    private var scrollingItself = false

    // A new reading and a resume are the moments the user turns back to the reading
    fun startFollowing() {
        isFollowing = true
    }

    fun caretMoved() {
        isFollowing = false
    }

    // A change of the size of the visible area alone leaves its origin as it was, and is no scroll
    fun visibleAreaChanged(newOriginX: Int, newOriginY: Int) {
        val moved = newOriginX != originX || newOriginY != originY
        originX = newOriginX
        originY = newOriginY
        if (moved && !scrollingItself) isFollowing = false
    }

    // The scroll that the highlighter makes to keep the word in view is not the user's
    fun <T> scrollItself(scroll: () -> T): T {
        scrollingItself = true
        try {
            return scroll()
        } finally {
            scrollingItself = false
        }
    }
}
