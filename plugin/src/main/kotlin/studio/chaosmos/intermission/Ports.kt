package studio.chaosmos.intermission

/** Where signals go: Godot in a game, a list in the tests. */
fun interface SignalSink {
    fun emit(name: String, vararg args: Any)
}

fun interface Cancellable {
    fun cancel()
}

/** Time and delayed work on the thread the controller runs on. */
interface Scheduler {
    fun nowMs(): Long
    fun after(delayMs: Long, task: () -> Unit): Cancellable
}

/** The part of the banner layer the controller drives. */
interface BannerSurface {
    fun place(anchor: Anchor, xDp: Double, yDp: Double)
    fun setVisible(visible: Boolean)
}
