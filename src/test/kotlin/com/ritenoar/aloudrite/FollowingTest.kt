package com.ritenoar.aloudrite

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowingTest {

    private val following = Following(originX = 0, originY = 100)

    @Test
    fun itFollowsAtTheStart() {
        assertTrue(following.isFollowing)
    }

    @Test
    fun itStopsAfterAScrollOfTheUser() {
        following.visibleAreaChanged(0, 400)

        assertFalse(following.isFollowing)
    }

    @Test
    fun itStopsAfterACaretMove() {
        following.caretMoved()

        assertFalse(following.isFollowing)
    }

    @Test
    fun aScrollThatTheHighlighterMakesIsNoScrollOfTheUser() {
        following.scrollItself { following.visibleAreaChanged(0, 400) }

        assertTrue(following.isFollowing)
    }

    @Test
    fun aScrollOfTheUserAfterTheOneOfTheHighlighterStopsIt() {
        following.scrollItself { following.visibleAreaChanged(0, 400) }

        following.visibleAreaChanged(0, 450)

        assertFalse(following.isFollowing)
    }

    @Test
    fun aChangeOfTheSizeAloneIsNoScroll() {
        following.visibleAreaChanged(0, 100)

        assertTrue(following.isFollowing)
    }

    @Test
    fun itFollowsAgainAfterAResumeOrANewReading() {
        following.caretMoved()

        following.startFollowing()

        assertTrue(following.isFollowing)
    }
}
