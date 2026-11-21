package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LevelPlayErrorsTest {
    @Test
    fun theCommonCodesHaveTheirOwnErrors() {
        assertEquals(AdError.NO_FILL, LevelPlayErrors.map(509))
        assertEquals(AdError.NO_FILL, LevelPlayErrors.map(606))
        assertEquals(AdError.NO_CONNECTION, LevelPlayErrors.map(520))
        assertEquals(AdError.INVALID_ARGUMENT, LevelPlayErrors.map(626))
        assertEquals(AdError.NOT_INITIALIZED, LevelPlayErrors.map(625))
        assertEquals(AdError.ALREADY_SHOWING, LevelPlayErrors.map(630))
        assertEquals(AdError.CAPPED, LevelPlayErrors.map(524))
        assertEquals(AdError.EXPIRED, LevelPlayErrors.map(1057))
        assertEquals(AdError.NO_ACTIVITY, LevelPlayErrors.map(1061))
    }

    @Test
    fun anyOtherCodeIsTheProvidersOwn() {
        assertEquals(AdError.PROVIDER, LevelPlayErrors.map(0))
        assertEquals(AdError.PROVIDER, LevelPlayErrors.map(510))
        assertEquals(AdError.PROVIDER, LevelPlayErrors.map(-1))
    }

    @Test
    fun theDemoAppHasAnAdUnitPerFormat() {
        val units = Format.entries.map { DemoApp.adUnit(it) }
        assertEquals(units.size, units.toSet().size)
        assertNotEquals("", DemoApp.APP_KEY)
    }
}
