package studio.chaosmos.intermission

/**
 * Tracks the app's started activities, the ad SDKs' own full-screen ones among them. The game's activity stops
 * whenever a full-screen ad covers it, so only the last one stopping means the app itself went to the background.
 *
 * Activities are kept by identity, so the game's activity, marked as started when the plugin arrives late in its
 * start, is never counted twice.
 */
class ForegroundCounter(private val onBackground: () -> Unit) {
    private val started = HashSet<Int>()

    fun activityStarted(activity: Any) {
        started.add(System.identityHashCode(activity))
    }

    fun activityStopped(activity: Any) {
        if (!started.remove(System.identityHashCode(activity))) return
        if (started.isEmpty()) onBackground()
    }
}
