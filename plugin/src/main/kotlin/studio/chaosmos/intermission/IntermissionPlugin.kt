package studio.chaosmos.intermission

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
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
class IntermissionPlugin(godot: Godot) : GodotPlugin(godot), ProviderHost {

    companion object {
        const val NAME = "IntermissionAndroid"
        const val PROVIDER_KEY = "studio.chaosmos.intermission.provider"
        private const val TAG = "Intermission"
    }

    private val main = Handler(Looper.getMainLooper())
    private var layer: BannerLayer? = null
    private var provider: Provider? = null
    private var controller: AdController? = null
    private var callbacks: Application.ActivityLifecycleCallbacks? = null

    override val events: ProviderEvents
        get() = checkNotNull(controller)

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
        val provider = createProvider(activity)
        this.provider = provider
        val sink = SignalSink { name, args -> emitSignal(name, *args) }
        val scheduler = MainScheduler(main)
        val controller = AdController(provider, sink, scheduler, layer)
        this.controller = controller
        provider.attach(this)
        watchForeground(activity, controller, scheduler)
        return layer
    }

    override fun onMainDestroy() {
        callbacks?.let { getActivity()?.application?.unregisterActivityLifecycleCallbacks(it) }
        callbacks = null
        controller?.release()
        layer?.setBannerView(null, 0, 0)
    }

    override fun activity(): Activity? = getActivity()

    override fun setBannerView(view: View?, widthDp: Int, heightDp: Int) {
        layer?.setBannerView(view, widthDp, heightDp)
    }

    @UsedByGodot
    fun initialize(appKey: String, testMode: Boolean) = onMain { it.initialize(appKey, testMode) }

    @UsedByGodot
    fun isInitialized(): Boolean = controller?.initialized ?: false

    @UsedByGodot
    fun isTestMode(): Boolean = controller?.testMode ?: false

    @UsedByGodot
    fun providerName(): String = provider?.name ?: ""

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
    fun showBanner(anchor: String, xDp: Double, yDp: Double) = onMain { it.showBanner(anchor, xDp, yDp) }

    @UsedByGodot
    fun hideBanner() = onMain { it.hideBanner() }

    @UsedByGodot
    fun destroyBanner() = onMain { it.destroyBanner() }

    @UsedByGodot
    fun bannerSizeDp(): IntArray = controller?.let { intArrayOf(it.bannerWidthDp, it.bannerHeightDp) } ?: intArrayOf(0, 0)

    @UsedByGodot
    fun setOverlongSeconds(seconds: Double) = onMain { it.setOverlongSeconds(seconds) }

    @UsedByGodot
    fun enableTestSuite() = onMain { it.enableTestSuite() }

    @UsedByGodot
    fun openTestSuite() = onMain {
        if (!it.openTestSuite()) Log.w(TAG, "The test suite opens only once enabled before initialize, and after it.")
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

    private fun createProvider(activity: Activity): Provider {
        val className = try {
            val info = activity.packageManager.getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
            info.metaData?.getString(PROVIDER_KEY)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (className.isNullOrEmpty()) {
            return MissingProvider("No provider library is in the build. Add one with the addon's export plugin.")
        }
        return try {
            Class.forName(className).getDeclaredConstructor().newInstance() as Provider
        } catch (e: ReflectiveOperationException) {
            Log.e(TAG, "Could not create the provider $className.", e)
            MissingProvider("Could not create the provider $className: $e")
        } catch (e: LinkageError) {
            Log.e(TAG, "The provider $className is missing its SDK.", e)
            MissingProvider("The provider $className is missing its SDK: $e")
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
