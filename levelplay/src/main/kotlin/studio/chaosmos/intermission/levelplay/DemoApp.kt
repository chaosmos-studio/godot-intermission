package studio.chaosmos.intermission.levelplay

import studio.chaosmos.intermission.Format

/**
 * LevelPlay's public demo app, from Unity's own demo (github.com/ironsource-mobile/Mediation-Demo-Apps,
 * `Android/Kotlin/.../DemoActivity.kt`, commit 80742fe of 29 July 2026).
 *
 * LevelPlay has no switch for test ads in code: its Unity Ads adapter 5.17.0 always initializes Unity Ads with
 * `withTestMode(false)`. So test mode sends every request to this app instead of the game's own, and a phone
 * under test never adds a view or a click to the game's ad account.
 */
object DemoApp {
    const val APP_KEY = "25b63cf85"
    const val INTERSTITIAL = "h3xw38h9214adgxo"
    const val REWARDED = "syz3d8ekts22q0or"
    const val BANNER = "4fpetq4lhe5lsw3e"

    fun adUnit(format: Format): String = when (format) {
        Format.INTERSTITIAL -> INTERSTITIAL
        Format.REWARDED -> REWARDED
        Format.BANNER -> BANNER
    }
}
