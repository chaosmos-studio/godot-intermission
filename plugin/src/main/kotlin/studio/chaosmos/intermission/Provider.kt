package studio.chaosmos.intermission

import android.app.Activity
import android.view.View

/**
 * One ad SDK behind the plugin. A provider library names its implementation in its manifest under
 * `studio.chaosmos.intermission.provider`, and it needs a public constructor with no arguments.
 *
 * Every call arrives on the Android main thread, and every event must be reported on it.
 */
interface Provider {
    val name: String
    val sdkVersion: String

    fun attach(host: ProviderHost)

    fun setGdprConsent(granted: Boolean)
    fun setUsPrivacyOptOut(optedOut: Boolean)
    fun setChildDirected(child: Boolean)

    /** In test mode no request may ever reach the game's own ad account. */
    fun initialize(appKey: String, testMode: Boolean)

    fun load(format: Format, adUnitId: String)
    fun isReady(format: Format, adUnitId: String): Boolean
    fun show(format: Format, adUnitId: String, placement: String)

    fun loadBanner(adUnitId: String, size: BannerSize, placement: String)
    fun setBannerRefreshing(on: Boolean)
    fun destroyBanner()

    /** False when the SDK has no test suite or cannot open it now. */
    fun openTestSuite(): Boolean

    fun release()
}

interface ProviderHost {
    val events: ProviderEvents

    fun activity(): Activity?

    /** Puts the provider's banner view in the plugin's layer, sized in dp, or removes it with null. */
    fun setBannerView(view: View?, widthDp: Int, heightDp: Int)
}

/** What a provider reports, with its own raw code and message beside the plugin's [AdError]. */
interface ProviderEvents {
    fun onInitialized()
    fun onInitFailed(error: AdError, code: Int, message: String)

    fun onLoaded(format: Format, adUnitId: String)
    fun onLoadFailed(format: Format, adUnitId: String, error: AdError, code: Int, message: String)
    fun onShown(format: Format, adUnitId: String)
    fun onShowFailed(format: Format, adUnitId: String, error: AdError, code: Int, message: String)
    fun onClicked(format: Format, adUnitId: String)
    fun onRewarded(adUnitId: String, name: String, amount: Int)
    fun onClosed(format: Format, adUnitId: String)

    fun onBannerLoaded(adUnitId: String, widthDp: Int, heightDp: Int)
    fun onBannerLoadFailed(adUnitId: String, error: AdError, code: Int, message: String)
    fun onBannerShown(adUnitId: String)
    fun onBannerClicked(adUnitId: String)
    fun onBannerLeftApp(adUnitId: String)
}
