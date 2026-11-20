package studio.chaosmos.intermission

import java.util.concurrent.ConcurrentHashMap

/**
 * The plugin's own logic: state per ad unit, the queue of loads asked for before the SDK is ready, the rewarded
 * grace period, the banner, and the two quality signals. It knows nothing of Android or Godot, so plain JVM tests
 * drive it.
 *
 * It runs on one thread, the Android main thread in a game. Godot's thread reads only [isReady] and the
 * volatile fields.
 */
class AdController(
    private val provider: Provider,
    private val signals: SignalSink,
    private val scheduler: Scheduler,
    private val banner: BannerSurface,
) : ProviderEvents {

    companion object {
        const val DEFAULT_OVERLONG_MS = 60_000L

        /** A reward can be reported after the close, so a rewarded close waits this long for it. */
        const val REWARD_GRACE_MS = 1_000L

        /** An ad that has neither shown nor failed this long after `show` is given up on. */
        const val SHOW_START_TIMEOUT_MS = 8_000L

        /** A banner that sends the player away this soon after a tap counts as clicked. */
        const val BANNER_CLICK_WINDOW_MS = 5_000L
    }

    private enum class Init { IDLE, STARTED, DONE, FAILED }
    private enum class State { IDLE, LOADING, READY, SHOWING }

    private class Slot(val format: Format) {
        var state = State.IDLE
    }

    private class Show(val format: Format, val adUnitId: String) {
        var shownAtMs = -1L
        var clicked = false
        var rewarded = false
        var leftReported = false
        var start: Cancellable? = null
        var overlong: Cancellable? = null
        var grace: Cancellable? = null

        fun cancelTimers() {
            start?.cancel()
            overlong?.cancel()
            grace?.cancel()
        }
    }

    private data class BannerRequest(val adUnitId: String, val size: BannerSize, val placement: String)

    @Volatile var initialized = false
        private set
    @Volatile var testMode = false
        private set
    @Volatile var bannerWidthDp = 0
        private set
    @Volatile var bannerHeightDp = 0
        private set
    var overlongMs = DEFAULT_OVERLONG_MS
        private set

    private val ready: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var init = Init.IDLE
    private val slots = HashMap<String, Slot>()
    private val pendingLoads = LinkedHashMap<String, Format>()
    private var pendingBanner: BannerRequest? = null
    private var showing: Show? = null
    private var closing: Show? = null
    private var bannerUnit: String? = null
    private var bannerWanted = false
    private var bannerClickedAtMs = -1L

    fun isReady(adUnitId: String): Boolean = adUnitId in ready

    fun setGdprConsent(granted: Boolean) = provider.setGdprConsent(granted)

    fun setUsPrivacyOptOut(optedOut: Boolean) = provider.setUsPrivacyOptOut(optedOut)

    fun setChildDirected(child: Boolean) = provider.setChildDirected(child)

    fun setOverlongSeconds(seconds: Double) {
        overlongMs = if (seconds > 0.0) (seconds * 1000.0).toLong() else 0L
    }

    fun enableTestSuite() = provider.enableTestSuite()

    fun openTestSuite(): Boolean = provider.openTestSuite()

    fun initialize(appKey: String, testMode: Boolean) {
        when (init) {
            Init.STARTED -> return
            Init.DONE -> {
                signals.emit(Signals.INITIALIZED, true, AdError.NONE.wire, 0, "")
                return
            }
            Init.IDLE, Init.FAILED -> Unit
        }
        if (appKey.isBlank() && !testMode) {
            signals.emit(Signals.INITIALIZED, false, AdError.INVALID_ARGUMENT.wire, 0, "The app key is empty.")
            return
        }
        this.testMode = testMode
        init = Init.STARTED
        provider.initialize(appKey.trim(), testMode)
    }

    override fun onInitialized() {
        if (init == Init.DONE) return
        init = Init.DONE
        initialized = true
        signals.emit(Signals.INITIALIZED, true, AdError.NONE.wire, 0, "")
        val loads = pendingLoads.toList()
        pendingLoads.clear()
        for ((adUnitId, format) in loads) load(format, adUnitId)
        pendingBanner?.let {
            pendingBanner = null
            loadBanner(it.adUnitId, it.size.wire, it.placement)
        }
    }

    override fun onInitFailed(error: AdError, code: Int, message: String) {
        if (init == Init.DONE) return
        init = Init.FAILED
        signals.emit(Signals.INITIALIZED, false, error.wire, code, message)
        val loads = pendingLoads.toList()
        pendingLoads.clear()
        for ((adUnitId, format) in loads) {
            failLoad(format.wire, adUnitId, AdError.NOT_INITIALIZED, 0, "The SDK failed to initialize.")
        }
        pendingBanner?.let {
            pendingBanner = null
            failBanner(it.adUnitId, AdError.NOT_INITIALIZED, 0, "The SDK failed to initialize.")
        }
    }

    fun load(formatWire: String, adUnitId: String) {
        val format = Format.fromWire(formatWire)
        if (format == null || format == Format.BANNER) {
            failLoad(formatWire, adUnitId, AdError.INVALID_ARGUMENT, 0, "Load interstitial or rewarded ads here, and banners with loadBanner.")
            return
        }
        load(format, adUnitId)
    }

    private fun load(format: Format, adUnitId: String) {
        if (adUnitId.isBlank()) {
            failLoad(format.wire, adUnitId, AdError.INVALID_ARGUMENT, 0, "The ad unit id is empty.")
            return
        }
        when (init) {
            Init.STARTED -> {
                pendingLoads[adUnitId] = format
                return
            }
            Init.IDLE, Init.FAILED -> {
                failLoad(format.wire, adUnitId, AdError.NOT_INITIALIZED, 0, "Call initialize first.")
                return
            }
            Init.DONE -> Unit
        }
        val slot = slots.getOrPut(adUnitId) { Slot(format) }
        if (slot.format != format) {
            failLoad(format.wire, adUnitId, AdError.INVALID_ARGUMENT, 0, "This ad unit is already used for ${slot.format.wire} ads.")
            return
        }
        when (slot.state) {
            State.LOADING -> Unit
            State.READY -> signals.emit(Signals.AD_LOADED, format.wire, adUnitId)
            State.SHOWING -> failLoad(format.wire, adUnitId, AdError.ALREADY_SHOWING, 0, "This ad unit is showing an ad.")
            State.IDLE -> {
                slot.state = State.LOADING
                provider.load(format, adUnitId)
            }
        }
    }

    override fun onLoaded(format: Format, adUnitId: String) {
        val slot = slots.getOrPut(adUnitId) { Slot(format) }
        if (slot.state == State.SHOWING) return
        slot.state = State.READY
        ready.add(adUnitId)
        signals.emit(Signals.AD_LOADED, format.wire, adUnitId)
    }

    override fun onLoadFailed(format: Format, adUnitId: String, error: AdError, code: Int, message: String) {
        slots[adUnitId]?.let { if (it.state != State.SHOWING) it.state = State.IDLE }
        ready.remove(adUnitId)
        failLoad(format.wire, adUnitId, error, code, message)
    }

    fun show(adUnitId: String, placement: String) {
        val slot = slots[adUnitId]
        val formatWire = slot?.format?.wire ?: Format.UNKNOWN_WIRE
        if (slot == null || slot.state != State.READY) {
            failShow(formatWire, adUnitId, AdError.NOT_LOADED, 0, "No ad is loaded for this ad unit.")
            return
        }
        if (showing != null) {
            failShow(formatWire, adUnitId, AdError.ALREADY_SHOWING, 0, "Another ad is showing.")
            return
        }
        ready.remove(adUnitId)
        if (!provider.isReady(slot.format, adUnitId)) {
            slot.state = State.IDLE
            failShow(formatWire, adUnitId, AdError.EXPIRED, 0, "The loaded ad is no longer available. Load another.")
            return
        }
        slot.state = State.SHOWING
        val show = Show(slot.format, adUnitId)
        showing = show
        show.start = scheduler.after(SHOW_START_TIMEOUT_MS) { giveUp(show) }
        provider.show(slot.format, adUnitId, placement)
    }

    private fun giveUp(show: Show) {
        if (showing !== show || show.shownAtMs >= 0) return
        showing = null
        show.cancelTimers()
        slots[show.adUnitId]?.state = State.IDLE
        failShow(show.format.wire, show.adUnitId, AdError.TIMEOUT, 0, "The ad neither showed nor failed in time.")
    }

    override fun onShown(format: Format, adUnitId: String, network: String) {
        val show = showing?.takeIf { it.adUnitId == adUnitId }
        if (show != null && show.shownAtMs < 0) {
            show.start?.cancel()
            show.shownAtMs = scheduler.nowMs()
            if (overlongMs > 0) show.overlong = scheduler.after(overlongMs) { reportOverlong(show) }
        }
        signals.emit(Signals.AD_SHOWN, format.wire, adUnitId, network)
    }

    private fun reportOverlong(show: Show) {
        if (showing !== show) return
        val seconds = (scheduler.nowMs() - show.shownAtMs) / 1000.0
        signals.emit(Signals.AD_OVERLONG, show.format.wire, show.adUnitId, seconds)
    }

    override fun onShowFailed(format: Format, adUnitId: String, error: AdError, code: Int, message: String) {
        showing?.takeIf { it.adUnitId == adUnitId }?.let {
            it.cancelTimers()
            showing = null
        }
        slots[adUnitId]?.state = State.IDLE
        ready.remove(adUnitId)
        failShow(format.wire, adUnitId, error, code, message)
    }

    override fun onClicked(format: Format, adUnitId: String) {
        showing?.takeIf { it.adUnitId == adUnitId }?.clicked = true
        signals.emit(Signals.AD_CLICKED, format.wire, adUnitId)
    }

    override fun onRewarded(adUnitId: String, name: String, amount: Int) {
        val show = listOfNotNull(showing, closing).firstOrNull { it.adUnitId == adUnitId }
        show?.rewarded = true
        signals.emit(Signals.AD_REWARDED, adUnitId, name, amount)
        if (show != null && show === closing) {
            show.grace?.cancel()
            reportClosed(show)
        }
    }

    override fun onClosed(format: Format, adUnitId: String) {
        slots[adUnitId]?.let { if (it.state == State.SHOWING) it.state = State.IDLE }
        val show = showing?.takeIf { it.adUnitId == adUnitId }
        if (show == null) {
            signals.emit(Signals.AD_CLOSED, format.wire, adUnitId, false)
            return
        }
        showing = null
        show.cancelTimers()
        if (show.format == Format.REWARDED && !show.rewarded) {
            closing = show
            show.grace = scheduler.after(REWARD_GRACE_MS) { reportClosed(show) }
        } else {
            reportClosed(show)
        }
    }

    private fun reportClosed(show: Show) {
        if (closing === show) closing = null
        signals.emit(Signals.AD_CLOSED, show.format.wire, show.adUnitId, show.rewarded)
    }

    /** Something outside the app came to the front. While a full-screen ad is up, that is the ad leaving the app. */
    fun onAppLeft() {
        val show = showing ?: return
        if (show.leftReported) return
        show.leftReported = true
        signals.emit(Signals.APP_LEFT, show.format.wire, show.adUnitId, show.clicked)
    }

    fun loadBanner(adUnitId: String, sizeWire: String, placement: String) {
        val size = BannerSize.fromWire(sizeWire)
        if (size == null || adUnitId.isBlank()) {
            failBanner(adUnitId, AdError.INVALID_ARGUMENT, 0, "A banner needs an ad unit id and a known size.")
            return
        }
        when (init) {
            Init.STARTED -> {
                pendingBanner = BannerRequest(adUnitId, size, placement)
                return
            }
            Init.IDLE, Init.FAILED -> {
                failBanner(adUnitId, AdError.NOT_INITIALIZED, 0, "Call initialize first.")
                return
            }
            Init.DONE -> Unit
        }
        if (bannerUnit != null) provider.destroyBanner()
        bannerUnit = adUnitId
        bannerWidthDp = 0
        bannerHeightDp = 0
        provider.loadBanner(adUnitId, size, placement)
        provider.setBannerRefreshing(bannerWanted)
    }

    fun showBanner(anchorWire: String, xDp: Double, yDp: Double) {
        val anchor = Anchor.fromWire(anchorWire) ?: return
        bannerWanted = true
        banner.place(anchor, xDp, yDp)
        banner.setVisible(true)
        if (bannerUnit != null) provider.setBannerRefreshing(true)
    }

    fun hideBanner() {
        bannerWanted = false
        banner.setVisible(false)
        if (bannerUnit != null) provider.setBannerRefreshing(false)
    }

    fun destroyBanner() {
        pendingBanner = null
        if (bannerUnit != null) provider.destroyBanner()
        bannerUnit = null
        bannerWidthDp = 0
        bannerHeightDp = 0
    }

    /** Again on every refresh, since the SDK reloads a banner by itself. */
    override fun onBannerLoaded(adUnitId: String, widthDp: Int, heightDp: Int) {
        if (adUnitId != bannerUnit) return
        bannerWidthDp = widthDp
        bannerHeightDp = heightDp
        signals.emit(Signals.BANNER_LOADED, adUnitId, widthDp, heightDp)
    }

    override fun onBannerLoadFailed(adUnitId: String, error: AdError, code: Int, message: String) {
        if (adUnitId != bannerUnit) return
        failBanner(adUnitId, error, code, message)
    }

    override fun onBannerShown(adUnitId: String) {
        signals.emit(Signals.BANNER_SHOWN, adUnitId)
    }

    override fun onBannerClicked(adUnitId: String) {
        bannerClickedAtMs = scheduler.nowMs()
        signals.emit(Signals.BANNER_CLICKED, adUnitId)
    }

    override fun onBannerLeftApp(adUnitId: String) {
        val clicked = bannerClickedAtMs >= 0 && scheduler.nowMs() - bannerClickedAtMs <= BANNER_CLICK_WINDOW_MS
        signals.emit(Signals.APP_LEFT, Format.BANNER.wire, adUnitId, clicked)
    }

    fun release() {
        showing?.cancelTimers()
        closing?.cancelTimers()
        showing = null
        closing = null
        pendingLoads.clear()
        pendingBanner = null
        provider.release()
    }

    private fun failLoad(formatWire: String, adUnitId: String, error: AdError, code: Int, message: String) {
        signals.emit(Signals.AD_FAILED_TO_LOAD, formatWire, adUnitId, error.wire, code, message)
    }

    private fun failShow(formatWire: String, adUnitId: String, error: AdError, code: Int, message: String) {
        signals.emit(Signals.AD_FAILED_TO_SHOW, formatWire, adUnitId, error.wire, code, message)
    }

    private fun failBanner(adUnitId: String, error: AdError, code: Int, message: String) {
        signals.emit(Signals.BANNER_FAILED_TO_LOAD, adUnitId, error.wire, code, message)
    }
}
