package studio.chaosmos.intermission

import kotlin.math.roundToInt

/**
 * Where the banner sits in the plugin's full-screen layer, in pixels.
 *
 * TOP and BOTTOM centre it, `y` is its distance from that edge and `x` shifts it right of centre.
 * CUSTOM puts its top-left corner `x` and `y` from the layer's top left.
 */
data class BannerPlacement(
    val atBottom: Boolean,
    val centred: Boolean,
    val leftPx: Int,
    val topPx: Int,
    val bottomPx: Int,
) {
    companion object {
        fun of(anchor: Anchor, xDp: Double, yDp: Double, density: Float): BannerPlacement {
            val x = (xDp * density).roundToInt()
            val y = (maxOf(yDp, 0.0) * density).roundToInt()
            return when (anchor) {
                Anchor.TOP -> BannerPlacement(atBottom = false, centred = true, leftPx = x, topPx = y, bottomPx = 0)
                Anchor.BOTTOM -> BannerPlacement(atBottom = true, centred = true, leftPx = x, topPx = 0, bottomPx = y)
                Anchor.CUSTOM -> BannerPlacement(atBottom = false, centred = false, leftPx = maxOf(x, 0), topPx = y, bottomPx = 0)
            }
        }
    }
}
