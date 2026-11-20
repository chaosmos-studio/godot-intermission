package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Test

class BannerPlacementTest {
    @Test
    fun bottomCentresTheBannerAboveTheEdge() {
        val placement = BannerPlacement.of(Anchor.BOTTOM, 0.0, 72.0, 2.75f)
        assertEquals(BannerPlacement(atBottom = true, centred = true, leftPx = 0, topPx = 0, bottomPx = 198), placement)
    }

    @Test
    fun topCentresTheBannerBelowTheEdgeAndShiftsItSideways() {
        val placement = BannerPlacement.of(Anchor.TOP, -10.0, 24.0, 2.0f)
        assertEquals(BannerPlacement(atBottom = false, centred = true, leftPx = -20, topPx = 48, bottomPx = 0), placement)
    }

    @Test
    fun customPlacesTheTopLeftCorner() {
        val placement = BannerPlacement.of(Anchor.CUSTOM, 20.5, 600.0, 3.0f)
        assertEquals(BannerPlacement(atBottom = false, centred = false, leftPx = 62, topPx = 1800, bottomPx = 0), placement)
    }

    @Test
    fun theBannerNeverLeavesTheScreenEdge() {
        assertEquals(0, BannerPlacement.of(Anchor.BOTTOM, 0.0, -30.0, 2.0f).bottomPx)
        assertEquals(0, BannerPlacement.of(Anchor.CUSTOM, -5.0, -5.0, 2.0f).leftPx)
    }
}
