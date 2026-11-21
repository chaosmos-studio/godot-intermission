---
title: Test mode
description: See test ads on your own phones without ever touching your own ad account.
---

:::danger[The safe rule]
Never show your own app key's ads on your own phone.
Test with test mode, and check your own ad units with loads only.
Ad networks can lock accounts whose owners view or tap their own live ads.
:::

## Turning it on

```gdscript
ads.initialize("YOUR_APP_KEY", true)
```

LevelPlay has no switch for test ads in code, and its Unity Ads adapter always starts Unity Ads with test mode off.
So in test mode, Intermission asks [LevelPlay's public demo app](https://github.com/ironsource-mobile/Mediation-Demo-Apps) for every ad instead of yours:

- every request goes to the demo app's key and ad units, whatever ad unit ids you pass,
- your signals still carry your own ad unit ids, so the rest of your code runs unchanged,
- your own app and its fraud checks never see your test phones,
- the app key may be empty.

`is_test_mode()` tells which mode the plugin started in.
A good default is `initialize(app_key, OS.is_debug_build())`: debug builds get test ads, release builds your own.

## The integration test suite

`open_test_suite()` opens LevelPlay's Integration Test Suite, which shows the app's setup, its ad networks and its ad units.
It opens once `initialize` has finished, in a debug build or in test mode.
LevelPlay reads the switch that enables it at start, so Intermission sets it for you before `initialize`.
Unity documents it in [Integration test suite](https://docs.unity.com/en-us/grow/levelplay/sdk/android/integration-test-suite).
It supports portrait orientation only.

## Your own app on a test device

LevelPlay can send test ads to a registered device from your own app, but only when both of these are true ([Unity's guide](https://docs.unity.com/en-us/grow/levelplay/sdk/android/integration-testing)):

1. The device is added under **Test devices** in the LevelPlay dashboard, with its advertising ID.
2. A network is chosen for it, with its ad units, and **Test ads** selected at the end of that row. The device then gets ads only from that network.

Unity's guide also says: "By default, the ad network configuration resets within the hour."
In the Integration Test Suite, bidding networks have a **Live / Test** toggle, and non-bidding networks always test with live ads.

Registering the device alone is not enough.
On 21 Nov 2026, a phone registered under Test devices, but without Unity Ads chosen for it with **Test ads**, received two live campaigns from Unity Ads through LevelPlay with a game's own app key, the second after Unity's "Force test mode ON for all devices" override was saved.
The missing **Test ads** choice is the most likely reason.
One of the two offered no close button for over four minutes, and the other opened Google Play's install page by itself.

So keep to the safe rule: show your own app's ads only on players' phones, and use test mode everywhere else.
