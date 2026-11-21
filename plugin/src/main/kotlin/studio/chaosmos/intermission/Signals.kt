package studio.chaosmos.intermission

/**
 * Every signal the plugin emits, with its argument types. Godot drops an emission whose arguments do not match
 * (GodotPlugin checks each with `isInstance`), so the types are the boxed classes, never primitives.
 */
internal object Signals {
    const val INITIALIZED = "initialized"
    const val AD_LOADED = "ad_loaded"
    const val AD_FAILED_TO_LOAD = "ad_failed_to_load"
    const val AD_SHOWN = "ad_shown"
    const val AD_FAILED_TO_SHOW = "ad_failed_to_show"
    const val AD_CLICKED = "ad_clicked"
    const val AD_CLOSED = "ad_closed"
    const val BANNER_LOADED = "banner_loaded"
    const val BANNER_FAILED_TO_LOAD = "banner_failed_to_load"
    const val APP_LEFT = "app_left"
    const val AD_OVERLONG = "ad_overlong"

    private val STRING = String::class.java
    private val INT = Int::class.javaObjectType
    private val BOOL = Boolean::class.javaObjectType
    private val DOUBLE = Double::class.javaObjectType

    val TYPES: Map<String, List<Class<*>>> = mapOf(
        INITIALIZED to listOf(BOOL, STRING, INT, STRING),
        AD_LOADED to listOf(STRING, STRING),
        AD_FAILED_TO_LOAD to listOf(STRING, STRING, STRING, INT, STRING),
        AD_SHOWN to listOf(STRING, STRING, STRING),
        AD_FAILED_TO_SHOW to listOf(STRING, STRING, STRING, INT, STRING),
        AD_CLICKED to listOf(STRING, STRING),
        AD_CLOSED to listOf(STRING, STRING, BOOL),
        BANNER_LOADED to listOf(STRING, INT, INT),
        BANNER_FAILED_TO_LOAD to listOf(STRING, STRING, INT, STRING),
        APP_LEFT to listOf(STRING, STRING, BOOL),
        AD_OVERLONG to listOf(STRING, STRING, DOUBLE),
    )
}
