package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundWatchTest {
    private var left = 0
    private val clock = FakeScheduler()
    private val watch = ForegroundWatch(clock, ForegroundWatch.GRACE_MS) { left++ }
    private val game = Any()
    private val ad = Any()

    @Test
    fun anAdOpeningAndClosingIsNotTheAppLeaving() {
        watch.activityResumed(game)
        watch.activityPaused(game)
        clock.advance(120)
        watch.activityResumed(ad)
        watch.activityPaused(ad)
        clock.advance(300)
        watch.activityResumed(game)
        clock.advance(ForegroundWatch.GRACE_MS * 4)
        assertEquals(0, left)
    }

    @Test
    fun somethingElseInFrontPastTheGraceIsTheAppLeaving() {
        watch.activityResumed(ad)
        watch.activityPaused(ad)
        clock.advance(ForegroundWatch.GRACE_MS - 1)
        assertEquals(0, left)
        clock.advance(1)
        assertEquals(1, left)
    }

    @Test
    fun comingBackWithinTheGraceCancelsIt() {
        watch.activityResumed(ad)
        watch.activityPaused(ad)
        clock.advance(ForegroundWatch.GRACE_MS / 2)
        watch.activityResumed(ad)
        clock.advance(ForegroundWatch.GRACE_MS * 4)
        assertEquals(0, left)
    }

    @Test
    fun anActivityMarkedTwiceIsCountedOnce() {
        watch.activityResumed(game)
        watch.activityResumed(game)
        watch.activityPaused(game)
        clock.advance(ForegroundWatch.GRACE_MS)
        assertEquals(1, left)
    }

    @Test
    fun aPauseNeverSeenResumingIsIgnored() {
        watch.activityPaused(ad)
        watch.activityResumed(game)
        watch.activityPaused(ad)
        clock.advance(ForegroundWatch.GRACE_MS * 4)
        assertEquals(0, left)
    }
}
