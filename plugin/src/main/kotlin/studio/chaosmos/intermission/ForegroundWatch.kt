package studio.chaosmos.intermission

/**
 * Tells when none of the app's activities, the ad SDKs' full-screen ones among them, has been in front for
 * [graceMs]: another app, the home screen or a store page has covered the app.
 *
 * Resumed, not started, activities are counted, because Google Play's install overlay is see-through: an ad that
 * opens it leaves the app's activities started but paused. The grace covers the moment between one of the app's
 * activities pausing and the next one resuming, as when an ad opens or closes.
 *
 * Activities are kept by identity, so one marked as resumed when the plugin arrives late is never counted twice.
 */
class ForegroundWatch(
    private val scheduler: Scheduler,
    private val graceMs: Long,
    private val onLeft: () -> Unit,
) {
    companion object {
        const val GRACE_MS = 500L
    }

    private val resumed = HashSet<Int>()
    private var pending: Cancellable? = null

    fun activityResumed(activity: Any) {
        resumed.add(System.identityHashCode(activity))
        pending?.cancel()
        pending = null
    }

    fun activityPaused(activity: Any) {
        if (!resumed.remove(System.identityHashCode(activity)) || resumed.isNotEmpty()) return
        pending?.cancel()
        pending = scheduler.after(graceMs) {
            pending = null
            if (resumed.isEmpty()) onLeft()
        }
    }
}
