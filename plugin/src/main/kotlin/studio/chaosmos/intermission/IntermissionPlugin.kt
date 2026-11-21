package studio.chaosmos.intermission

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

/**
 * The Godot side of the plugin. Godot calls these methods on its own thread, and each one hands its work to the
 * Android main thread, where the SDK and the controller live. Signals reach GDScript on Godot's thread.
 */
class IntermissionPlugin(godot: Godot) : GodotPlugin(godot) {

    companion object {
        const val NAME = "IntermissionAndroid"
        private const val TAG = "Intermission"
    }

    private val main = Handler(Looper.getMainLooper())
    private var layer: BannerLayer? = null
    private var provider: Provider? = null
    private var controller: AdController? = null
    private var callbacks: Application.ActivityLifecycleCallbacks? = null

    private val host = object : ProviderHost {
        override val events: ProviderEvents
            get() = checkNotNull(controller)

        override fun activity(): Activity? = getActivity()

        override fun setBannerView(view: View?, widthDp: Int, heightDp: Int) {
            layer?.setBannerView(view, widthDp, heightDp)
        }
    }

    override fun getPluginName() = NAME

    override fun getPluginSignals(): Set<SignalInfo> =
        Signals.TYPES.map { (name, types) -> SignalInfo(name, *types.toTypedArray()) }.toSet()

    override fun onMainCreate(activity: Activity?): View? {
        if (activity == null) {
            Log.e(TAG, "Godot gave the plugin no activity, so it stays off.")
            return null
        }
        val layer = BannerLayer(activity)
        this.layer = layer
        val provider = LevelPlayProvider()
        this.provider = provider
        val sink = SignalSink { name, args -> emitSignal(name, *args) }
        val scheduler = MainScheduler(main)
        val controller = AdController(provider, sink, scheduler, layer)
        this.controller = controller
        provider.attach(host)
        watchForeground(activity, controller, scheduler)
        return layer
    }

    override fun onMainDestroy() {
        callbacks?.let { getActivity()?.application?.unregisterActivityLifecycleCallbacks(it) }
        callbacks = null
        controller?.release()
        layer?.setBannerView(null, 0, 0)
    }

    @UsedByGodot
    fun initialize(appKey: String, testMode: Boolean) = onMain { it.initialize(appKey, testMode) }

    @UsedByGodot
    fun isInitialized(): Boolean = controller?.initialized ?: false

    @UsedByGodot
    fun isTestMode(): Boolean = controller?.testMode ?: false

    @UsedByGodot
    fun sdkVersion(): String = runCatching { provider?.sdkVersion ?: "" }.getOrDefault("")

    @UsedByGodot
    fun setGdprConsent(granted: Boolean) = onMain { it.setGdprConsent(granted) }

    @UsedByGodot
    fun setUsPrivacyOptOut(optedOut: Boolean) = onMain { it.setUsPrivacyOptOut(optedOut) }

    @UsedByGodot
    fun setChildDirected(child: Boolean) = onMain { it.setChildDirected(child) }

    @UsedByGodot
    fun loadAd(format: String, adUnitId: String) = onMain { it.load(format, adUnitId) }

    @UsedByGodot
    fun isAdReady(adUnitId: String): Boolean = controller?.isReady(adUnitId) ?: false

    @UsedByGodot
    fun showAd(adUnitId: String, placement: String) = onMain { it.show(adUnitId, placement) }

    @UsedByGodot
    fun loadBanner(adUnitId: String, size: String, placement: String) = onMain { it.loadBanner(adUnitId, size, placement) }

    @UsedByGodot
    fun showBanner(anchor: String, marginDp: Double) = onMain { it.showBanner(anchor, marginDp) }

    @UsedByGodot
    fun hideBanner() = onMain { it.hideBanner() }

    @UsedByGodot
    fun destroyBanner() = onMain { it.destroyBanner() }

    @UsedByGodot
    fun setOverlongSeconds(seconds: Double) = onMain { it.setOverlongSeconds(seconds) }

    @UsedByGodot
    fun openTestSuite() = onMain {
        if (!it.openTestSuite()) Log.w(TAG, "The test suite opens only in a debug build or test mode, once initialized.")
    }

    private fun onMain(work: (AdController) -> Unit) {
        val controller = controller
        if (controller == null) {
            Log.e(TAG, "Called before the plugin was created.")
            return
        }
        main.post {
            try {
                work(controller)
            } catch (e: Exception) {
                Log.e(TAG, "An ad call failed.", e)
            }
        }
    }

    private fun watchForeground(activity: Activity, controller: AdController, scheduler: Scheduler) {
        val watch = ForegroundWatch(scheduler, ForegroundWatch.GRACE_MS) { controller.onAppLeft() }
        // Godot may create this plugin after its activity resumed, once that was already announced.
        if ((activity as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
            watch.activityResumed(activity)
        }
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = watch.activityResumed(activity)
            override fun onActivityPaused(activity: Activity) = watch.activityPaused(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        activity.application.registerActivityLifecycleCallbacks(callbacks)
        this.callbacks = callbacks
    }

    private class MainScheduler(private val handler: Handler) : Scheduler {
        override fun nowMs() = SystemClock.elapsedRealtime()

        override fun after(delayMs: Long, task: () -> Unit): Cancellable {
            val runnable = Runnable { task() }
            handler.postDelayed(runnable, delayMs)
            return Cancellable { handler.removeCallbacks(runnable) }
        }
    }
}
