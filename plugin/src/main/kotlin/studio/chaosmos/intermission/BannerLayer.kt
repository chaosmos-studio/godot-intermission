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
internal class BannerLayer(context: Context) : FrameLayout(context), BannerSurface {
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

    override fun place(anchor: Anchor, marginDp: Double) {
        val placement = BannerPlacement.of(anchor, marginDp, density)
        val params = holder.layoutParams as LayoutParams
        params.gravity = (if (placement.atBottom) Gravity.BOTTOM else Gravity.TOP) or Gravity.CENTER_HORIZONTAL
        params.setMargins(0, if (placement.atBottom) 0 else placement.marginPx, 0, if (placement.atBottom) placement.marginPx else 0)
        holder.layoutParams = params
    }

    override fun setVisible(visible: Boolean) {
        holder.visibility = if (visible) VISIBLE else GONE
    }
}
