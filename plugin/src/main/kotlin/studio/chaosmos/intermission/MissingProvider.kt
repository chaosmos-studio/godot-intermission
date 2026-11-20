package studio.chaosmos.intermission

/** Stands in when no provider library is in the build, so every call still answers with a clear failure. */
class MissingProvider(private val reason: String) : Provider {
    private var host: ProviderHost? = null

    override val name = "none"
    override val sdkVersion = ""

    override fun attach(host: ProviderHost) {
        this.host = host
    }

    override fun setGdprConsent(granted: Boolean) = Unit
    override fun setUsPrivacyOptOut(optedOut: Boolean) = Unit
    override fun setChildDirected(child: Boolean) = Unit

    override fun initialize(appKey: String, testMode: Boolean) {
        host?.events?.onInitFailed(AdError.INIT_FAILED, 0, reason)
    }

    override fun load(format: Format, adUnitId: String) {
        host?.events?.onLoadFailed(format, adUnitId, AdError.NOT_INITIALIZED, 0, reason)
    }

    override fun isReady(format: Format, adUnitId: String) = false

    override fun show(format: Format, adUnitId: String, placement: String) {
        host?.events?.onShowFailed(format, adUnitId, AdError.NOT_LOADED, 0, reason)
    }

    override fun loadBanner(adUnitId: String, size: BannerSize, placement: String) {
        host?.events?.onBannerLoadFailed(adUnitId, AdError.NOT_INITIALIZED, 0, reason)
    }

    override fun setBannerRefreshing(on: Boolean) = Unit
    override fun destroyBanner() = Unit
    override fun openTestSuite() = false
    override fun release() = Unit
}
