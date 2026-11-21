package studio.chaosmos.intermission

import kotlin.math.roundToInt

/** The banner sits centred at the top or the bottom of the game's view, `margin` in from that edge, in pixels. */
internal data class BannerPlacement(val atBottom: Boolean, val marginPx: Int) {
    companion object {
        fun of(anchor: Anchor, marginDp: Double, density: Float): BannerPlacement =
            BannerPlacement(anchor == Anchor.BOTTOM, (maxOf(marginDp, 0.0) * density).roundToInt())
    }
}
