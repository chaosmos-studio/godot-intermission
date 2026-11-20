package studio.chaosmos.intermission

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.roundToInt

/**
 * A full-screen layer over the game that holds the banner. It handles no touches itself, so a touch anywhere but
 * on the banner falls through to the game.
 */
@SuppressLint("ViewConstructor")
class BannerLayer(context: Context) : FrameLayout(context), BannerSurface {
    private val holder = FrameLayout(context)
    private val density = resources.displayMetrics.density

    init {
        holder.visibility = GONE
        addView(holder, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
    }

    fun setBannerView(view: View?, widthDp: Int, heightDp: Int) {
        holder.removeAllViews()
        if (view == null) return
        (view.parent as? ViewGroup)?.removeView(view)
        val width = if (widthDp > 0) (widthDp * density).roundToInt() else LayoutParams.MATCH_PARENT
        val height = if (heightDp > 0) (heightDp * density).roundToInt() else LayoutParams.WRAP_CONTENT
        holder.addView(view, LayoutParams(width, height))
    }

    override fun place(anchor: Anchor, xDp: Double, yDp: Double) {
        val placement = BannerPlacement.of(anchor, xDp, yDp, density)
        val params = holder.layoutParams as LayoutParams
        val vertical = if (placement.atBottom) Gravity.BOTTOM else Gravity.TOP
        val horizontal = if (placement.centred) Gravity.CENTER_HORIZONTAL else Gravity.LEFT
        params.gravity = vertical or horizontal
        params.setMargins(placement.leftPx, placement.topPx, 0, placement.bottomPx)
        holder.layoutParams = params
    }

    override fun setVisible(visible: Boolean) {
        holder.visibility = if (visible) VISIBLE else GONE
    }
}
