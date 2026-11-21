package studio.chaosmos.intermission

import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import com.unity3d.mediation.LevelPlayPrivacySettings
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener
import com.unity3d.mediation.rewarded.LevelPlayReward
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener

/** Unity LevelPlay, with whichever networks the game's build adds (Unity Ads through its adapter, at first). */
internal class LevelPlayProvider : Provider {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var host: ProviderHost
    private val interstitials = HashMap<String, LevelPlayInterstitialAd>()
    private val rewardeds = HashMap<String, LevelPlayRewardedAd>()
    private var banner: LevelPlayBannerAdView? = null
    private var testMode = false
    private var testSuite = false
    private var initialized = false

    private val events: ProviderEvents
        get() = host.events

    override val sdkVersion: String
        get() = LevelPlay.getSdkVersion()

    override fun attach(host: ProviderHost) {
        this.host = host
    }

    override fun setGdprConsent(granted: Boolean) = LevelPlayPrivacySettings.setGDPRConsent(granted)

    override fun setUsPrivacyOptOut(optedOut: Boolean) = LevelPlayPrivacySettings.setCCPA(optedOut)

    override fun setChildDirected(child: Boolean) = LevelPlayPrivacySettings.setCOPPA(child)

    override fun initialize(appKey: String, testMode: Boolean) {
        val activity = host.activity()
        if (activity == null) {
            events.onInitFailed(AdError.NO_ACTIVITY, 0, "The game has no activity.")
            return
        }
        this.testMode = testMode
        // LevelPlay reads this only at init. Debug builds and test mode get the test suite, release builds never.
        testSuite = testMode || (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (testSuite) LevelPlay.setMetaData("is_test_suite", "enable")
        if (testMode) LevelPlay.setAdaptersDebug(true)
        val request = LevelPlayInitRequest.Builder(if (testMode) DemoApp.APP_KEY else appKey).build()
        LevelPlay.init(activity, request, object : LevelPlayInitListener {
            override fun onInitSuccess(configuration: LevelPlayConfiguration) = onMain {
                initialized = true
                events.onInitialized()
            }

            override fun onInitFailed(error: LevelPlayInitError) = onMain {
                events.onInitFailed(AdError.INIT_FAILED, error.errorCode, error.errorMessage)
            }
        })
    }

    override fun load(format: Format, adUnitId: String) {
        when (format) {
            Format.INTERSTITIAL -> interstitials.getOrPut(adUnitId) {
                LevelPlayInterstitialAd(sdkAdUnit(format, adUnitId)).apply { setListener(InterstitialListener(adUnitId)) }
            }.loadAd()
            Format.REWARDED -> rewardeds.getOrPut(adUnitId) {
                LevelPlayRewardedAd(sdkAdUnit(format, adUnitId)).apply { setListener(RewardedListener(adUnitId)) }
            }.loadAd()
            Format.BANNER -> events.onLoadFailed(format, adUnitId, AdError.INVALID_ARGUMENT, 0, "Banners load with loadBanner.")
        }
    }

    override fun isReady(format: Format, adUnitId: String): Boolean = when (format) {
        Format.INTERSTITIAL -> interstitials[adUnitId]?.isAdReady() ?: false
        Format.REWARDED -> rewardeds[adUnitId]?.isAdReady() ?: false
        Format.BANNER -> false
    }

    override fun show(format: Format, adUnitId: String, placement: String) {
        val activity = host.activity()
        if (activity == null) {
            events.onShowFailed(format, adUnitId, AdError.NO_ACTIVITY, 0, "The game has no activity.")
            return
        }
        val placementName = placement.ifBlank { null }
        when (format) {
            Format.INTERSTITIAL -> interstitials[adUnitId]?.showAd(activity, placementName)
            Format.REWARDED -> rewardeds[adUnitId]?.showAd(activity, placementName)
            Format.BANNER -> null
        } ?: events.onShowFailed(format, adUnitId, AdError.NOT_LOADED, 0, "No ad was loaded for this ad unit.")
    }

    override fun loadBanner(adUnitId: String, size: BannerSize, placement: String) {
        val activity = host.activity()
        if (activity == null) {
            events.onBannerLoadFailed(adUnitId, AdError.NO_ACTIVITY, 0, "The game has no activity.")
            return
        }
        destroyBanner()
        val adSize = when (size) {
            BannerSize.BANNER -> LevelPlayAdSize.BANNER
            BannerSize.LARGE -> LevelPlayAdSize.LARGE
            BannerSize.MEDIUM_RECTANGLE -> LevelPlayAdSize.MEDIUM_RECTANGLE
            BannerSize.LEADERBOARD -> LevelPlayAdSize.LEADERBOARD
            BannerSize.ADAPTIVE -> LevelPlayAdSize.createAdaptiveAdSize(activity) ?: LevelPlayAdSize.BANNER
        }
        val config = LevelPlayBannerAdView.Config.Builder().setAdSize(adSize)
        if (placement.isNotBlank()) config.setPlacementName(placement)
        val view = LevelPlayBannerAdView(activity, sdkAdUnit(Format.BANNER, adUnitId), config.build())
        view.setBannerListener(BannerListener(adUnitId, adSize))
        banner = view
        host.setBannerView(view, adSize.width, adSize.height)
        view.loadAd()
    }

    override fun setBannerRefreshing(on: Boolean) {
        val view = banner ?: return
        if (on) view.resumeAutoRefresh() else view.pauseAutoRefresh()
    }

    override fun destroyBanner() {
        val view = banner ?: return
        banner = null
        host.setBannerView(null, 0, 0)
        view.destroy()
    }

    override fun openTestSuite(): Boolean {
        val activity = host.activity()
        if (!testSuite || !initialized || activity == null) return false
        LevelPlay.launchTestSuite(activity)
        return true
    }

    override fun release() {
        destroyBanner()
        interstitials.clear()
        rewardeds.clear()
    }

    private fun sdkAdUnit(format: Format, adUnitId: String) = if (testMode) DemoApp.adUnit(format) else adUnitId

    private fun onMain(work: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) work() else main.post(work)
    }

    private fun LevelPlayAdError.mapped() = LevelPlayErrors.map(errorCode)

    private inner class InterstitialListener(private val adUnitId: String) : LevelPlayInterstitialAdListener {
        private val format = Format.INTERSTITIAL

        override fun onAdLoaded(adInfo: LevelPlayAdInfo) = onMain { events.onLoaded(format, adUnitId) }

        override fun onAdLoadFailed(error: LevelPlayAdError) = onMain {
            events.onLoadFailed(format, adUnitId, error.mapped(), error.errorCode, error.errorMessage)
        }

        override fun onAdDisplayed(adInfo: LevelPlayAdInfo) = onMain { events.onShown(format, adUnitId, adInfo.adNetwork) }

        override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) = onMain {
            events.onShowFailed(format, adUnitId, error.mapped(), error.errorCode, error.errorMessage)
        }

        override fun onAdClicked(adInfo: LevelPlayAdInfo) = onMain { events.onClicked(format, adUnitId) }

        override fun onAdClosed(adInfo: LevelPlayAdInfo) = onMain { events.onClosed(format, adUnitId) }
    }

    private inner class RewardedListener(private val adUnitId: String) : LevelPlayRewardedAdListener {
        private val format = Format.REWARDED

        override fun onAdLoaded(adInfo: LevelPlayAdInfo) = onMain { events.onLoaded(format, adUnitId) }

        override fun onAdLoadFailed(error: LevelPlayAdError) = onMain {
            events.onLoadFailed(format, adUnitId, error.mapped(), error.errorCode, error.errorMessage)
        }

        override fun onAdDisplayed(adInfo: LevelPlayAdInfo) = onMain { events.onShown(format, adUnitId, adInfo.adNetwork) }

        override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) = onMain {
            events.onShowFailed(format, adUnitId, error.mapped(), error.errorCode, error.errorMessage)
        }

        override fun onAdRewarded(reward: LevelPlayReward, adInfo: LevelPlayAdInfo) = onMain { events.onRewarded(adUnitId) }

        override fun onAdClicked(adInfo: LevelPlayAdInfo) = onMain { events.onClicked(format, adUnitId) }

        override fun onAdClosed(adInfo: LevelPlayAdInfo) = onMain { events.onClosed(format, adUnitId) }
    }

    private inner class BannerListener(private val adUnitId: String, private val requested: LevelPlayAdSize) :
        LevelPlayBannerAdViewListener {

        override fun onAdLoaded(adInfo: LevelPlayAdInfo) = onMain {
            val size = adInfo.adSize ?: requested
            events.onBannerLoaded(adUnitId, size.width, size.height)
        }

        override fun onAdLoadFailed(error: LevelPlayAdError) = onMain {
            events.onBannerLoadFailed(adUnitId, error.mapped(), error.errorCode, error.errorMessage)
        }

        override fun onAdClicked(adInfo: LevelPlayAdInfo) = onMain { events.onBannerClicked(adUnitId) }

        override fun onAdLeftApplication(adInfo: LevelPlayAdInfo) = onMain { events.onBannerLeftApp(adUnitId) }
    }
}
