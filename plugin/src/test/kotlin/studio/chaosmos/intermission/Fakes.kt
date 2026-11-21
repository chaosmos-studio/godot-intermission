package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Records every emission and checks it against the declared signal, as Godot's own check would. */
internal class RecordingSink : SignalSink {
    val emitted = mutableListOf<Pair<String, List<Any>>>()

    override fun emit(name: String, vararg args: Any) {
        val types = Signals.TYPES[name]
        assertTrue("undeclared signal $name", types != null)
        assertEquals("argument count of $name", types!!.size, args.size)
        for ((i, arg) in args.withIndex()) {
            assertTrue("argument $i of $name is ${arg::class.java}, declared ${types[i]}", types[i].isInstance(arg))
        }
        emitted.add(name to args.toList())
    }

    fun named(name: String) = emitted.filter { it.first == name }.map { it.second }
}

internal class FakeScheduler : Scheduler {
    private class Task(val at: Long, val work: () -> Unit) {
        var cancelled = false
    }

    private val tasks = mutableListOf<Task>()
    var now = 0L

    override fun nowMs() = now

    override fun after(delayMs: Long, task: () -> Unit): Cancellable {
        val entry = Task(now + delayMs, task)
        tasks.add(entry)
        return Cancellable { entry.cancelled = true }
    }

    fun advance(ms: Long) {
        val until = now + ms
        while (true) {
            val next = tasks.filter { !it.cancelled && it.at <= until }.minByOrNull { it.at } ?: break
            tasks.remove(next)
            now = next.at
            next.work()
        }
        now = until
    }
}

internal class FakeSurface : BannerSurface {
    var placed: Pair<Anchor, Double>? = null
    var shown = false

    override fun place(anchor: Anchor, marginDp: Double) {
        placed = anchor to marginDp
    }

    override fun setVisible(visible: Boolean) {
        shown = visible
    }
}

/** Answers nothing by itself: a test plays the SDK's part by calling the controller's events. */
internal class FakeProvider : Provider {
    val calls = mutableListOf<String>()
    val notReady = mutableSetOf<String>()

    override val sdkVersion = "1.0"

    override fun attach(host: ProviderHost) = Unit

    override fun setGdprConsent(granted: Boolean) {
        calls.add("gdpr $granted")
    }

    override fun setUsPrivacyOptOut(optedOut: Boolean) {
        calls.add("ccpa $optedOut")
    }

    override fun setChildDirected(child: Boolean) {
        calls.add("coppa $child")
    }

    override fun initialize(appKey: String, testMode: Boolean) {
        calls.add("init $appKey $testMode")
    }

    override fun load(format: Format, adUnitId: String) {
        calls.add("load ${format.wire} $adUnitId")
    }

    override fun isReady(format: Format, adUnitId: String) = adUnitId !in notReady

    override fun show(format: Format, adUnitId: String, placement: String) {
        calls.add("show $adUnitId $placement")
    }

    override fun loadBanner(adUnitId: String, size: BannerSize, placement: String) {
        calls.add("banner ${size.wire} $adUnitId $placement")
    }

    override fun setBannerRefreshing(on: Boolean) {
        calls.add("refresh $on")
    }

    override fun destroyBanner() {
        calls.add("destroy banner")
    }

    override fun openTestSuite(): Boolean {
        calls.add("test suite")
        return true
    }

    override fun release() {
        calls.add("release")
    }
}
