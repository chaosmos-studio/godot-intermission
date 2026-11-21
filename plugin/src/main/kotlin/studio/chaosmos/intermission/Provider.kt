package studio.chaosmos.intermission

import android.app.Activity
import android.view.View

/**
 * The ad SDK behind the plugin, LevelPlay, kept behind this seam so the controller is tested against a fake.
 * Every call arrives on the Android main thread, and every event must be reported on it.
 */
internal interface Provider {
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

    /** False when the SDK cannot open its test suite now. */
    fun openTestSuite(): Boolean

    fun release()
}

internal interface ProviderHost {
    val events: ProviderEvents

    fun activity(): Activity?

    /** Puts the provider's banner view in the plugin's layer, sized in dp, or removes it with null. */
    fun setBannerView(view: View?, widthDp: Int, heightDp: Int)
}

/**
 * What a provider reports, with its own raw code and message beside the plugin's [AdError]. `network` names the ad
 * network that filled the ad, as the SDK reports it.
 */
internal interface ProviderEvents {
    fun onInitialized()
    fun onInitFailed(error: AdError, code: Int, message: String)

    fun onLoaded(format: Format, adUnitId: String)
    fun onLoadFailed(format: Format, adUnitId: String, error: AdError, code: Int, message: String)
    fun onShown(format: Format, adUnitId: String, network: String)
    fun onShowFailed(format: Format, adUnitId: String, error: AdError, code: Int, message: String)
    fun onClicked(format: Format, adUnitId: String)
    fun onRewarded(adUnitId: String)
    fun onClosed(format: Format, adUnitId: String)

    fun onBannerLoaded(adUnitId: String, widthDp: Int, heightDp: Int)
    fun onBannerLoadFailed(adUnitId: String, error: AdError, code: Int, message: String)
    fun onBannerClicked(adUnitId: String)
    fun onBannerLeftApp(adUnitId: String)
}
