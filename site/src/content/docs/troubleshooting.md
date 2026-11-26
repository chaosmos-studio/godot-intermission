---
title: Troubleshooting
description: No fill, live ads on a test phone, store pages that open by themselves, and other surprises.
---

## Every load fails with `NO_FILL` (509)

`ad_failed_to_load` with `NO_FILL` and provider code 509, "Mediation No fill", means LevelPlay found no network with an ad for that request.
In test mode this is rare, since the demo app always has ads.
With your own app key, check these in order:

1. **A network is set up for the ad unit.** In LevelPlay, the ad unit needs at least one network instance, such as a Unity Ads bidding placement, turned on for the countries you test in.
2. **The change has reached the SDK.** LevelPlay's dashboard changes can take time to reach phones. On 21 Nov 2026, units answered 509 until Unity Ads was connected, and filled once it was.
3. **Each format is covered.** A banner ad unit needs its own banner instance. An interstitial filling does not mean the banner will.
4. **There is demand.** A brand-new app, a country with little demand, or strict ad controls can leave requests unfilled. That is normal, and your game should carry on without the ad.

Never check fill by showing your own app's ads on your own phone. Loads are enough to see whether a unit fills. See [Test mode](/tools/intermission/guides/test-mode/).

## Live ads appear on a registered test phone

Registering a phone under **Test devices** is not enough.
LevelPlay sends a network's test ads to a device only when that network, with its ad units, is chosen for the device with **Test ads**, and Unity's guide says this configuration resets within the hour by default.
Until then, your own app key serves the phone live ads.

If it happens, close the ad with its own close button, never tap anything else in it, and switch back to test mode.
The full story and Unity's guides are on [Test mode](/tools/intermission/guides/test-mode/).

## An ad opens Google Play by itself

Some Unity Ads creatives open Google Play's install page on their own, with no tap, for example as a video ends.
Play's page appears over the ad, and `app_left` arrives with `clicked` false.
No setting in LevelPlay or Unity Ads stops it.
Close Play's page with Back and the ad is still there behind it, with its own close button.
See [Misbehaving ads](/tools/intermission/guides/misbehaving-ads/) for what a game can do about these ads.

## An ad has no close button

Some playable ads offer only a link to the store for a long time.
`ad_overlong` reports an ad that stays up past your limit, once you set one with `set_overlong_seconds`.
Back does not close Unity's ad player.

## `initialized` fails

- `INVALID_ARGUMENT`: the app key is empty and test mode is off.
- `INIT_FAILED`: LevelPlay could not start, and its code and message say why.
- `NOT_AVAILABLE`: the plugin is not in this build. Check that the addon is enabled, that the export preset uses the Gradle build, and that the build is for Android.

## Nothing happens at all

- Check `available()`. In the editor and on desktop it is false, and every request fails on the next frame with `NOT_AVAILABLE`.
- Connect the signals before you call `initialize` and `load_ad`, or the answers arrive before anyone listens.
- Read the device log with `adb logcat` and look for lines from `Intermission`, `LevelPlay` and `UnityAds`.

## The test suite does not open

`open_test_suite()` works only after `initialize` has finished, in a debug build or in test mode.
LevelPlay's suite supports portrait orientation only.

## The build fails in Gradle

- **Use Gradle Build** must be on in the Android export preset, and the Android build template installed.
- The first build downloads Unity's SDKs from Maven Central, so it needs a connection.
- If another plugin brings different versions of the same Google or Unity libraries, Gradle reports a conflict. Intermission's pins are listed on [Install](/tools/intermission/install/).
