# Design

How Intermission is built, and the facts about Unity's SDKs it rests on.
Every SDK fact below was read from the libraries on Maven Central (mediation-sdk 9.8.0, unityads-adapter 5.17.0, unity-ads 4.22.0), with `javap`, not from Unity's documentation.

## Shape

- `plugin/` is one Android library: a Godot 4.7 Android plugin (v2) named `IntermissionAndroid`.
  It compiles against LevelPlay but does not bundle it, and the game's Gradle build fetches the SDKs.
  - `IntermissionPlugin` is the only public class: the methods Godot calls and the signals it declares.
  - `AdController` keeps the state of every ad unit and decides every signal, with no Android or SDK code in it, so the unit tests drive it with a fake provider, a fake clock and a fake banner layer.
  - `LevelPlayProvider` is the only code that touches LevelPlay, behind the `Provider` interface.
  - `BannerLayer` is a view over the game that holds the banner and lets every other touch through.
- `demo/addons/intermission/` is the addon: `intermission.gd` (the typed `Intermission` class) and the export plugin, which adds the library and the SDKs' Maven coordinates to a Gradle build.
- `demo/` is a project that tries every call on a phone, and `demo/tests/` checks the addon headless against a stand-in for the Android singleton and against the Kotlin source.

## Threads

- Godot calls the plugin on its own thread.
  Each call is posted to the Android main thread, where the SDK and the controller run, in the order it was made.
- Only `isAdReady`, `isInitialized`, `isTestMode` and `sdkVersion` answer at once, from values the controller publishes for other threads.
- Signals go through Godot's `emitSignal`, which delivers them on Godot's thread.
  While a full-screen ad covers the game, Godot is paused and the signals wait, in order, until it resumes.

## The SDK facts it rests on

- **Classes and listeners:** `LevelPlay.init(Context, LevelPlayInitRequest, LevelPlayInitListener)`, `LevelPlayInterstitialAd` and `LevelPlayRewardedAd` (`loadAd`, `isAdReady`, `showAd(Activity, String?)`, `setListener`), and `LevelPlayBannerAdView(Context, String, Config)` with `pauseAutoRefresh`, `resumeAutoRefresh` and `destroy`.
  Banner listeners take `(adInfo, error)` for a failed display, while the full-screen ones take `(error, adInfo)`.
- **Banner sizes:** `LevelPlayAdSize.BANNER` is 320x50, `LARGE` 320x90, `MEDIUM_RECTANGLE` 300x250 and `LEADERBOARD` 728x90, and `createAdaptiveAdSize(Context)` makes the adaptive size.
- **Consent:** `LevelPlayPrivacySettings.setGDPRConsent`, `setCCPA` and `setCOPPA`.
  The Unity Ads adapter passes them on as Unity's `gdpr.consent`, `privacy.consent` and `user.nonBehavioral`.
- **No test mode in code:** the adapter builds Unity's `InitializationConfiguration` with `withTestMode(false)`, always.
  So the plugin's test mode asks LevelPlay's public demo app (`DemoApp.kt`, from Unity's own demo) for every ad.
  A game's own account then never sees a test device, which is what Unity's fraud checks watch.
- **Test suite:** LevelPlay reads the `is_test_suite` metadata at init, so the plugin sets it before `init` in debug builds and in test mode, and `launchTestSuite` opens the suite after.
- **Bidding only:** the adapter loads Unity Ads with `LoadConfiguration.Builder(placement).withAdMarkup(...)`, the bid from LevelPlay's auction.
  Unity's bidding placements cannot be loaded without that markup, and new waterfall placements can no longer be created, which is why the plugin goes through LevelPlay and not the Unity Ads SDK alone.
- **Errors:** LevelPlay's codes are int constants in `LevelPlayAdError` (624 to 631) and `com.ironsource.mediationsdk.logger.IronSourceError`.
  `LevelPlayErrors.kt` maps the common ones (no fill, no connection, capped, timeout, expired) to `AdError`, and the rest stay `PROVIDER` with the raw code.
- **No expiry callback:** LevelPlay never says that a loaded ad expired.
  `show` asks the SDK's `isAdReady` first and reports `EXPIRED` when the ad is gone.
- **Rewards can trail the close:** the plugin waits up to a second after a rewarded close for `onAdRewarded`, so `ad_closed(rewarded)` is right.
- **Banners skip refreshes while invisible** (codes 613 and 623), so the layer shows the banner's holder whenever the game wants a banner.
  Until it loads it is empty and lets touches through.
- **Version ranges:** the mediation-sdk POM asks for `adquality-sdk [9.12.0,10.0.0)`, and unity-ads asks for `coherencelib [0.3.0,0.4.0)`.
  The export plugin holds both strictly (`9.12.1!!`, `0.3.0!!`).
- **Code that runs without `initialize`:** the merged manifest gets two LevelPlay content providers (`IronsourceLifecycleProvider`, `LevelPlayActivityLifecycleProvider`) and Unity's `androidx.startup` initializer `AdsSdkInitializer`.
  They start with the app, even when a game never initializes the plugin.
- **Permissions:** the SDKs' manifests add `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION` and `ACCESS_ADSERVICES_TOPICS`.
  A game may remove the last two in its own manifest.

## Ads that misbehave

- `app_left`: the plugin tracks every resumed activity of the app, the ads' own full-screen activities included.
  When none has been in front for half a second while a full-screen ad is up, something outside the app covered the ad, and the plugin reports it once for that ad, with whether a tap came first.
  Resumed, not started, activities count, because Google Play's install overlay is see-through and leaves the ad's activity started.
  On a test phone a Unity Ads test interstitial opened that overlay by itself about 10 seconds in, with no touch (Unity's WebView called `openUrl("https://play.google.com/d?id=...", overlay: true)`), and LevelPlay reported no click.
  Counting started activities missed it, and counting resumed ones catches it.
  No switch turns this off in code: Unity Ads 4.22.0's `AndroidHandleOpenUrl` starts whatever intent the ad's own script asks for through the WebView bridge, and neither SDK's classes mention `overlay`, `autoOpen`, `storeOverlay` or `inlineInstall`.
  For banners it uses LevelPlay's `onAdLeftApplication`, and a tap in the 5 seconds before counts.
- `ad_overlong`: off until a game sets a limit, since how long is too long depends on the game.
  Once set, a timer starts when a full-screen ad shows and fires once if the ad is still up at the limit.
- An ad that neither shows nor fails within 8 seconds of `show` is given up with `TIMEOUT`, so one stuck ad never blocks the next.

## Choices that keep the API small

- One provider, LevelPlay, built into the one library, with no provider setting or second library to pick.
- The reward is the `rewarded` flag on `ad_closed`, not a signal of its own, so a game grants it in one place.
  The reward's name and amount from the dashboard are left out, since a game knows what it gives.
- Banners sit centred at the top or the bottom with a margin, and their size comes with `banner_loaded`.
- `open_test_suite` needs no setup call, since the plugin enables the suite in debug builds and test mode.

## Changes from the first design

The first design (written before the SDK was chosen) planned Unity Ads alone with placements.
The real SDK changed these:

- LevelPlay is the provider, since Unity's bidding placements need a mediator.
- Ad unit ids replace placement ids, and an optional placement name goes with `show_ad` and `load_banner`.
- `ad_closed(rewarded)` replaces `ad_finished(completed)`, since LevelPlay reports a close and a reward, not a finish state.
- There is no `ad_expired` signal (see above).
- `load_ad`, `show_ad` and `is_ad_ready` avoid GDScript's own `load`.
- `AdError` avoids shadowing Godot's global `Error`.
- The singleton is `IntermissionAndroid`, apart from the GDScript class `Intermission`.
- `app_left` counts the app's resumed activities rather than watching the game's own pause, as explained above.

## Device checklist

Run on a phone in test mode only, never tapping a live ad:

1. Initialize: `initialized(true)` online, and a clear failure offline.
2. Interstitial: loads, shows, closes with its close button, `ad_closed` once, and the game resumes where it was.
   Unity's ad player ignores Back, during the video and on the end card.
3. Rewarded: watched to the end, `ad_closed(rewarded = true)` once.
4. Banner: loads, shows at the top and the bottom with a margin, hides, comes back, and touches beside it reach the game.
5. An ad that opens the store without a tap raises `app_left(clicked = false)`, and a normal show and close raises nothing.
6. Home during an ad, then back: the game resumes and the ad state is right.
7. Rotation keeps the banner and the log.
8. Logcat shows no errors from `studio.chaosmos.intermission` and no "Invalid type" or "not registered" warnings from `GodotPlugin`.
