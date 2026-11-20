package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundCounterTest {
    private var background = 0
    private val counter = ForegroundCounter { background++ }
    private val game = Any()
    private val ad = Any()

    @Test
    fun anAdCoveringTheGameIsNotTheAppLeaving() {
        counter.activityStarted(game)
        // Android starts the covering activity before it stops the covered one, and the same on the way back.
        counter.activityStarted(ad)
        counter.activityStopped(game)
        counter.activityStarted(game)
        counter.activityStopped(ad)
        assertEquals(0, background)
    }

    @Test
    fun theLastActivityStoppingIsTheAppLeaving() {
        counter.activityStarted(game)
        counter.activityStarted(ad)
        counter.activityStopped(game)
        counter.activityStopped(ad)
        assertEquals(1, background)
    }

    @Test
    fun anActivityMarkedTwiceIsCountedOnce() {
        counter.activityStarted(game)
        counter.activityStarted(game)
        counter.activityStopped(game)
        assertEquals(1, background)
    }

    @Test
    fun aStopNeverSeenStartingIsIgnored() {
        counter.activityStopped(ad)
        counter.activityStarted(game)
        counter.activityStopped(ad)
        assertEquals(0, background)
    }
}
