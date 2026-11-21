package studio.chaosmos.intermission

/** Where signals go: Godot in a game, a list in the tests. */
internal fun interface SignalSink {
    fun emit(name: String, vararg args: Any)
}

internal fun interface Cancellable {
    fun cancel()
}

/** Time and delayed work on the thread the controller runs on. */
internal interface Scheduler {
    fun nowMs(): Long
    fun after(delayMs: Long, task: () -> Unit): Cancellable
}

/** The part of the banner layer the controller drives. */
internal interface BannerSurface {
    fun place(anchor: Anchor, marginDp: Double)
    fun setVisible(visible: Boolean)
}
