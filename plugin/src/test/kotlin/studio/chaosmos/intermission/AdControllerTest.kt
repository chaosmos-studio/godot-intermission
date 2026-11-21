package studio.chaosmos.intermission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdControllerTest {
    private val provider = FakeProvider()
    private val sink = RecordingSink()
    private val clock = FakeScheduler()
    private val surface = FakeSurface()
    private val ads = AdController(provider, sink, clock, surface)

    private fun ready() {
        ads.initialize("key", false)
        ads.onInitialized()
    }

    private fun loaded(format: Format, unit: String) {
        ads.load(format.wire, unit)
        ads.onLoaded(format, unit)
    }

    private fun showing(format: Format, unit: String) {
        loaded(format, unit)
        ads.show(unit, "")
        ads.onShown(format, unit, "UnityAds")
    }

    @Test
    fun anEmptyAppKeyFailsOutsideTestMode() {
        ads.initialize("  ", false)
        assertEquals(listOf(listOf(false, "invalid_argument", 0, "The app key is empty.")), sink.named(Signals.INITIALIZED))
        assertTrue(provider.calls.isEmpty())
    }

    @Test
    fun testModeNeedsNoAppKey() {
        ads.initialize("", true)
        assertEquals(listOf("init  true"), provider.calls)
        assertTrue(ads.testMode)
    }

    @Test
    fun initializingTwiceStartsTheSdkOnceAndAnswersAgainOnceDone() {
        ads.initialize("key", false)
        ads.initialize("key", false)
        ads.onInitialized()
        ads.initialize("key", false)
        assertEquals(1, provider.calls.count { it.startsWith("init") })
        assertEquals(2, sink.named(Signals.INITIALIZED).size)
        assertTrue(ads.initialized)
    }

    @Test
    fun aFailedInitCanBeRetried() {
        ads.initialize("key", false)
        ads.onInitFailed(AdError.INIT_FAILED, 508, "no config")
        ads.initialize("key", false)
        assertEquals(2, provider.calls.count { it.startsWith("init") })
        assertEquals(listOf(false, "init_failed", 508, "no config"), sink.named(Signals.INITIALIZED)[0])
    }

    @Test
    fun loadsAskedForDuringInitWaitForIt() {
        ads.initialize("key", false)
        ads.load("interstitial", "i1")
        ads.load("rewarded", "r1")
        assertTrue(provider.calls.none { it.startsWith("load") })
        ads.onInitialized()
        assertEquals(listOf("init key false", "load interstitial i1", "load rewarded r1"), provider.calls)
    }

    @Test
    fun loadsAskedForDuringAFailedInitFailToo() {
        ads.initialize("key", false)
        ads.load("rewarded", "r1")
        ads.onInitFailed(AdError.INIT_FAILED, 508, "no config")
        assertEquals("not_initialized", sink.named(Signals.AD_FAILED_TO_LOAD).single()[2])
    }

    @Test
    fun loadingBeforeInitializeFails() {
        ads.load("interstitial", "i1")
        assertEquals(listOf("interstitial", "i1", "not_initialized"), sink.named(Signals.AD_FAILED_TO_LOAD).single().take(3))
    }

    @Test
    fun loadRejectsBadArguments() {
        ready()
        ads.load("banner", "b1")
        ads.load("video", "v1")
        ads.load("interstitial", " ")
        val errors = sink.named(Signals.AD_FAILED_TO_LOAD).map { it[2] }
        assertEquals(listOf("invalid_argument", "invalid_argument", "invalid_argument"), errors)
        assertTrue(provider.calls.none { it.startsWith("load") })
    }

    @Test
    fun anAdUnitKeepsItsFormat() {
        ready()
        ads.load("interstitial", "u1")
        ads.load("rewarded", "u1")
        assertEquals("invalid_argument", sink.named(Signals.AD_FAILED_TO_LOAD).single()[2])
    }

    @Test
    fun aLoadInFlightIsNotRepeatedAndALoadedAdAnswersAtOnce() {
        ready()
        ads.load("interstitial", "i1")
        ads.load("interstitial", "i1")
        ads.onLoaded(Format.INTERSTITIAL, "i1")
        ads.load("interstitial", "i1")
        assertEquals(1, provider.calls.count { it == "load interstitial i1" })
        assertEquals(2, sink.named(Signals.AD_LOADED).size)
    }

    @Test
    fun readinessFollowsLoadAndShow() {
        ready()
        assertFalse(ads.isReady("i1"))
        loaded(Format.INTERSTITIAL, "i1")
        assertTrue(ads.isReady("i1"))
        ads.show("i1", "level_end")
        assertFalse(ads.isReady("i1"))
        assertEquals("show i1 level_end", provider.calls.last())
    }

    @Test
    fun aFailedLoadCanLoadAgain() {
        ready()
        ads.load("rewarded", "r1")
        ads.onLoadFailed(Format.REWARDED, "r1", AdError.NO_FILL, 509, "No fill")
        ads.load("rewarded", "r1")
        assertEquals(2, provider.calls.count { it == "load rewarded r1" })
        assertEquals(listOf("rewarded", "r1", "no_fill", 509, "No fill"), sink.named(Signals.AD_FAILED_TO_LOAD).single())
    }

    @Test
    fun showingAnUnknownAdUnitNamesNoFormat() {
        ready()
        ads.show("nope", "")
        assertEquals(listOf("unknown", "nope", "not_loaded"), sink.named(Signals.AD_FAILED_TO_SHOW).single().take(3))
    }

    @Test
    fun oneFullScreenAdAtATime() {
        ready()
        showing(Format.INTERSTITIAL, "i1")
        loaded(Format.REWARDED, "r1")
        ads.show("r1", "")
        assertEquals("already_showing", sink.named(Signals.AD_FAILED_TO_SHOW).single()[2])
        assertTrue("the rewarded ad keeps its load", ads.isReady("r1"))
    }

    @Test
    fun anAdTheSdkNoLongerHasIsReportedExpired() {
        ready()
        loaded(Format.INTERSTITIAL, "i1")
        provider.notReady.add("i1")
        ads.show("i1", "")
        assertEquals("expired", sink.named(Signals.AD_FAILED_TO_SHOW).single()[2])
        provider.notReady.clear()
        ads.load("interstitial", "i1")
        assertEquals(2, provider.calls.count { it == "load interstitial i1" })
    }

    @Test
    fun aShowThatFailsFreesTheAdUnit() {
        ready()
        loaded(Format.INTERSTITIAL, "i1")
        ads.show("i1", "")
        ads.onShowFailed(Format.INTERSTITIAL, "i1", AdError.PROVIDER, 1039, "show exception")
        loaded(Format.INTERSTITIAL, "i1")
        ads.show("i1", "")
        assertEquals(1, sink.named(Signals.AD_FAILED_TO_SHOW).size)
        assertEquals(2, provider.calls.count { it.startsWith("show i1") })
    }

    @Test
    fun anAdThatNeverStartsIsGivenUp() {
        ready()
        loaded(Format.INTERSTITIAL, "i1")
        ads.show("i1", "")
        clock.advance(AdController.SHOW_START_TIMEOUT_MS)
        assertEquals("timeout", sink.named(Signals.AD_FAILED_TO_SHOW).single()[2])
        loaded(Format.INTERSTITIAL, "i1")
        ads.show("i1", "")
        assertEquals(2, provider.calls.count { it.startsWith("show i1") })
    }

    @Test
    fun anAdThatStartsInTimeIsNotGivenUp() {
        ready()
        showing(Format.INTERSTITIAL, "i1")
        clock.advance(AdController.SHOW_START_TIMEOUT_MS * 2)
        assertTrue(sink.named(Signals.AD_FAILED_TO_SHOW).isEmpty())
    }

    @Test
    fun aShownAdNamesItsNetwork() {
        ready()
        showing(Format.REWARDED, "r1")
        assertEquals(listOf("rewarded", "r1", "UnityAds"), sink.named(Signals.AD_SHOWN).single())
    }

    @Test
    fun theTestSuiteIsTheProvidersToOpen() {
        assertTrue(ads.openTestSuite())
        assertEquals(listOf("test suite"), provider.calls)
    }

    @Test
    fun anInterstitialClosesAtOnce() {
        ready()
        showing(Format.INTERSTITIAL, "i1")
        ads.onClosed(Format.INTERSTITIAL, "i1")
        assertEquals(listOf(listOf("interstitial", "i1", false)), sink.named(Signals.AD_CLOSED))
    }

    @Test
    fun aRewardBeforeTheCloseClosesAtOnce() {
        ready()
        showing(Format.REWARDED, "r1")
        ads.onRewarded("r1")
        ads.onClosed(Format.REWARDED, "r1")
        assertEquals(listOf("rewarded", "r1", true), sink.named(Signals.AD_CLOSED).single())
    }

    @Test
    fun aRewardForNoAdOnScreenIsIgnored() {
        ready()
        ads.onRewarded("r1")
        assertTrue(sink.emitted.none { it.first == Signals.AD_CLOSED })
    }

    @Test
    fun aRewardJustAfterTheCloseStillCounts() {
        ready()
        showing(Format.REWARDED, "r1")
        ads.onClosed(Format.REWARDED, "r1")
        assertTrue(sink.named(Signals.AD_CLOSED).isEmpty())
        clock.advance(AdController.REWARD_GRACE_MS / 2)
        ads.onRewarded("r1")
        clock.advance(AdController.REWARD_GRACE_MS * 2)
        assertEquals(listOf(listOf("rewarded", "r1", true)), sink.named(Signals.AD_CLOSED))
    }

    @Test
    fun aRewardedAdClosedEarlyEarnsNothingAfterTheGrace() {
        ready()
        showing(Format.REWARDED, "r1")
        ads.onClosed(Format.REWARDED, "r1")
        clock.advance(AdController.REWARD_GRACE_MS - 1)
        assertTrue(sink.named(Signals.AD_CLOSED).isEmpty())
        clock.advance(1)
        assertEquals(listOf(listOf("rewarded", "r1", false)), sink.named(Signals.AD_CLOSED))
    }

    @Test
    fun leavingTheAppDuringAnAdWithoutATapIsReportedOnce() {
        ready()
        showing(Format.INTERSTITIAL, "i1")
        ads.onAppLeft()
        ads.onAppLeft()
        assertEquals(listOf(listOf("interstitial", "i1", false)), sink.named(Signals.APP_LEFT))
    }

    @Test
    fun leavingTheAppAfterATapSaysSo() {
        ready()
        showing(Format.REWARDED, "r1")
        ads.onClicked(Format.REWARDED, "r1")
        ads.onAppLeft()
        assertEquals(listOf(listOf("rewarded", "r1", true)), sink.named(Signals.APP_LEFT))
    }

    @Test
    fun leavingTheAppWithNoAdUpIsNotAnAdLeaving() {
        ready()
        showing(Format.INTERSTITIAL, "i1")
        ads.onClosed(Format.INTERSTITIAL, "i1")
        ads.onAppLeft()
        assertTrue(sink.named(Signals.APP_LEFT).isEmpty())
    }

    @Test
    fun theOverlongCheckIsOffUntilAsked() {
        ready()
        showing(Format.INTERSTITIAL, "i1")
        clock.advance(3_600_000)
        assertTrue(sink.named(Signals.AD_OVERLONG).isEmpty())
    }

    @Test
    fun anAdUpTooLongIsReportedOnce() {
        ready()
        ads.setOverlongSeconds(60.0)
        showing(Format.INTERSTITIAL, "i1")
        clock.advance(59_999)
        assertTrue(sink.named(Signals.AD_OVERLONG).isEmpty())
        clock.advance(180_000)
        assertEquals(listOf(listOf("interstitial", "i1", 60.0)), sink.named(Signals.AD_OVERLONG))
    }

    @Test
    fun anAdClosedInTimeIsNotOverlong() {
        ready()
        ads.setOverlongSeconds(60.0)
        showing(Format.INTERSTITIAL, "i1")
        clock.advance(10_000)
        ads.onClosed(Format.INTERSTITIAL, "i1")
        clock.advance(120_000)
        assertTrue(sink.named(Signals.AD_OVERLONG).isEmpty())
    }

    @Test
    fun theOverlongCheckCanBeTurnedOffAgain() {
        ready()
        ads.setOverlongSeconds(20.0)
        showing(Format.INTERSTITIAL, "i1")
        clock.advance(20_000)
        assertEquals(20.0, sink.named(Signals.AD_OVERLONG).single()[2])
        ads.onClosed(Format.INTERSTITIAL, "i1")
        ads.setOverlongSeconds(0.0)
        showing(Format.INTERSTITIAL, "i1")
        clock.advance(600_000)
        assertEquals(1, sink.named(Signals.AD_OVERLONG).size)
    }

    @Test
    fun aBannerAskedForDuringInitWaitsForIt() {
        ads.initialize("key", false)
        ads.loadBanner("b1", "banner", "home")
        assertTrue(provider.calls.none { it.startsWith("banner") })
        ads.onInitialized()
        assertTrue(provider.calls.contains("banner banner b1 home"))
    }

    @Test
    fun aBannerNeedsAKnownSizeAndAnAdUnit() {
        ready()
        ads.loadBanner("b1", "huge", "")
        ads.loadBanner("", "banner", "")
        assertEquals(listOf("invalid_argument", "invalid_argument"), sink.named(Signals.BANNER_FAILED_TO_LOAD).map { it[1] })
    }

    @Test
    fun aLoadedBannerReportsItsSize() {
        ready()
        ads.loadBanner("b1", "banner", "")
        ads.onBannerLoaded("b1", 320, 50)
        assertEquals(listOf("b1", 320, 50), sink.named(Signals.BANNER_LOADED).single())
    }

    @Test
    fun showingAndHidingTheBannerMovesTheLayerAndPausesRefresh() {
        ready()
        ads.loadBanner("b1", "banner", "")
        ads.showBanner("bottom", 72.0)
        assertEquals(Anchor.BOTTOM to 72.0, surface.placed)
        assertTrue(surface.shown)
        ads.hideBanner()
        assertFalse(surface.shown)
        assertEquals(listOf("refresh false", "refresh true", "refresh false"), provider.calls.filter { it.startsWith("refresh") })
    }

    @Test
    fun anUnknownAnchorChangesNothing() {
        ready()
        ads.showBanner("custom", 0.0)
        assertEquals(null, surface.placed)
        assertFalse(surface.shown)
    }

    @Test
    fun aNewBannerReplacesTheOldOneAndIgnoresItsEvents() {
        ready()
        ads.loadBanner("b1", "banner", "")
        ads.loadBanner("b2", "large", "")
        assertEquals(1, provider.calls.count { it == "destroy banner" })
        ads.onBannerLoaded("b1", 320, 50)
        ads.onBannerLoadFailed("b1", AdError.NO_FILL, 606, "No fill")
        assertTrue(sink.named(Signals.BANNER_LOADED).isEmpty())
        assertTrue(sink.named(Signals.BANNER_FAILED_TO_LOAD).isEmpty())
    }

    @Test
    fun aDestroyedBannerIgnoresLateEvents() {
        ready()
        ads.loadBanner("b1", "banner", "")
        ads.onBannerLoaded("b1", 320, 50)
        ads.destroyBanner()
        ads.onBannerLoaded("b1", 320, 50)
        assertEquals(1, sink.named(Signals.BANNER_LOADED).size)
        assertTrue(provider.calls.contains("destroy banner"))
    }

    @Test
    fun aBannerLeavingTheAppRightAfterATapCountsAsClicked() {
        ready()
        ads.loadBanner("b1", "banner", "")
        ads.onBannerLeftApp("b1")
        ads.onBannerClicked("b1")
        clock.advance(1_000)
        ads.onBannerLeftApp("b1")
        clock.advance(AdController.BANNER_CLICK_WINDOW_MS)
        ads.onBannerLeftApp("b1")
        assertEquals(listOf(false, true, false), sink.named(Signals.APP_LEFT).map { it[2] })
        assertEquals("banner", sink.named(Signals.APP_LEFT)[0][0])
    }

    @Test
    fun consentReachesTheProvider() {
        ads.setGdprConsent(false)
        ads.setUsPrivacyOptOut(true)
        ads.setChildDirected(false)
        assertEquals(listOf("gdpr false", "ccpa true", "coppa false"), provider.calls)
    }

    @Test
    fun releaseStopsEveryTimer() {
        ready()
        ads.setOverlongSeconds(60.0)
        showing(Format.INTERSTITIAL, "i1")
        ads.release()
        clock.advance(120_000)
        assertTrue(sink.named(Signals.AD_OVERLONG).isEmpty())
        assertEquals("release", provider.calls.last())
    }
}
