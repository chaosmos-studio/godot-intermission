package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Test

class BannerPlacementTest {
    @Test
    fun theMarginIsInDp() {
        assertEquals(BannerPlacement(atBottom = true, marginPx = 198), BannerPlacement.of(Anchor.BOTTOM, 72.0, 2.75f))
        assertEquals(BannerPlacement(atBottom = false, marginPx = 48), BannerPlacement.of(Anchor.TOP, 24.0, 2.0f))
    }

    @Test
    fun theBannerNeverLeavesTheScreenEdge() {
        assertEquals(0, BannerPlacement.of(Anchor.BOTTOM, -30.0, 2.0f).marginPx)
    }
}
