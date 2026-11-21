# Intermission

Ads from Unity LevelPlay, with Unity Ads as the network, for Godot 4.7 games on Android.
Interstitial, rewarded and banner ads through one node and its typed signals.

**Pre-release.** The API may still change before 0.1.0.

## Install

1. Download `intermission-<version>.zip` from the [releases](https://github.com/chaosmos-studio/godot-intermission/releases) and unzip it into your project, so you have `addons/intermission/`.
2. Enable Intermission under Project Settings, Plugins.
3. Install the Android build template (Project, Install Android Build Template) and turn on Use Gradle Build in your Android export preset.

The export adds the plugin and Unity's SDKs to the build, every version pinned, and Gradle fetches the SDKs from Maven Central.

## Use

```gdscript
const REWARDED := "YOUR_REWARDED_AD_UNIT_ID"
var ads := Intermission.new()

func _ready() -> void:
	add_child(ads)
	ads.ad_closed.connect(_on_ad_closed)
	ads.initialize("YOUR_APP_KEY", OS.is_debug_build())
	ads.load_ad(Intermission.Format.REWARDED, REWARDED)

func offer_hint() -> void:
	if ads.is_ad_ready(REWARDED):
		ads.show_ad(REWARDED, "hint")

func _on_ad_closed(_format: Intermission.Format, ad_unit_id: String, rewarded: bool) -> void:
	if ad_unit_id == REWARDED and rewarded:
		give_hint()
	ads.load_ad(Intermission.Format.REWARDED, REWARDED)
```

Every call returns at once and every answer comes back as a signal.
Loads asked for before the SDK is ready wait for it.
Off Android every request fails with `NOT_AVAILABLE` on the next frame, so the same code runs everywhere.

Interstitials work the same way with `Format.INTERSTITIAL`.
A banner loads with `load_banner(ad_unit_id, size)` and stays hidden until `show_banner(anchor, margin_dp)`, centred at the top or the bottom of the screen.
The sizes are `BANNER` (320x50), `LARGE` (320x90), `MEDIUM_RECTANGLE` (300x250), `LEADERBOARD` (728x90) and `ADAPTIVE` (the screen's width, with a height the SDK picks).

The class reference is in `addons/intermission/intermission.gd`, and `demo/` tries every call on a phone.

## Test mode

LevelPlay has no switch for test ads in code.
So with `initialize(app_key, true)` every ad comes from LevelPlay's public demo app, and your own app and its fraud checks never see your test devices.
Signals still carry your own ad unit ids.

- `open_test_suite()` opens LevelPlay's integration test suite, in debug builds and in test mode, once initialized.
- Your own app key may serve live ads even on a phone registered as a test device in the LevelPlay and Unity dashboards. Check your own ad units with loads only, and never show or tap one of their ads on your own phone.

## Consent

Intermission shows no consent form.
Ask in your own screen, or with a consent platform, and pass the answer on before `initialize`, and again whenever the player changes it.

- `set_gdpr_consent(granted)`: consent for personalised ads where the GDPR applies (the EEA, the UK, Switzerland).
- `set_us_privacy_opt_out(opted_out)`: a US player's choice not to have their data sold or shared.
- `set_child_directed(child)`: for apps directed at children.

## Ads that misbehave

- `app_left(format, ad_unit_id, clicked)`: something outside the app (a store page, another app, the home screen) covered a full-screen ad. With `clicked` false, nobody tapped the ad first.
- `ad_overlong(format, ad_unit_id, seconds)`: a full-screen ad is still up after `set_overlong_seconds(seconds)`. Off until you set it.
- `ad_shown(format, ad_unit_id, network)` names the network that filled the ad.

## Build

```bash
./gradlew :plugin:testDebugUnitTest addonZip
godot --headless --path demo --import
godot --headless --path demo -s res://tests/run_tests.gd
```

`docs/DESIGN.md` has the design and the SDK facts it rests on.

## Licence

MIT, see `LICENSE`.
Unity's SDKs are not part of this repository or its releases, and their own terms apply.

Intermission is not sponsored by or affiliated with Unity Technologies or its affiliates.
Unity, Unity Ads and Unity LevelPlay are trademarks or registered trademarks of Unity Technologies or its affiliates in the U.S. and elsewhere.
Intermission is not affiliated with the Godot Foundation.
