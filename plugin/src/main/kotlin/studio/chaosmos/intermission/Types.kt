package studio.chaosmos.intermission

/** Each value crosses to GDScript as its [wire] name, so reordering never changes what a game sees. */
enum class Format(val wire: String) {
    INTERSTITIAL("interstitial"),
    REWARDED("rewarded"),
    BANNER("banner");

    companion object {
        /** What a signal carries for an ad unit the plugin has never been asked to load. */
        const val UNKNOWN_WIRE = "unknown"

        fun fromWire(wire: String): Format? = entries.firstOrNull { it.wire == wire }
    }
}

enum class AdError(val wire: String) {
    NONE("none"),
    NOT_INITIALIZED("not_initialized"),
    INIT_FAILED("init_failed"),
    INVALID_ARGUMENT("invalid_argument"),
    NOT_LOADED("not_loaded"),
    ALREADY_SHOWING("already_showing"),
    NO_ACTIVITY("no_activity"),
    NO_FILL("no_fill"),
    NO_CONNECTION("no_connection"),
    TIMEOUT("timeout"),
    CAPPED("capped"),
    EXPIRED("expired"),

    /** Anything else the SDK reported: its own code and message travel with it. */
    PROVIDER("provider"),
}

enum class BannerSize(val wire: String, val widthDp: Int, val heightDp: Int) {
    BANNER("banner", 320, 50),
    LARGE("large", 320, 90),
    MEDIUM_RECTANGLE("medium_rectangle", 300, 250),
    LEADERBOARD("leaderboard", 728, 90),

    /** As wide as the screen, with a height the SDK picks. 0 means "decided by the SDK". */
    ADAPTIVE("adaptive", 0, 0);

    companion object {
        fun fromWire(wire: String): BannerSize? = entries.firstOrNull { it.wire == wire }
    }
}

enum class Anchor(val wire: String) {
    TOP("top"),
    BOTTOM("bottom"),
    CUSTOM("custom");

    companion object {
        fun fromWire(wire: String): Anchor? = entries.firstOrNull { it.wire == wire }
    }
}
