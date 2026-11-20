# Intermission

An Android plugin for Godot 4.7 that shows ads from Unity LevelPlay, with Unity Ads as its network.
Interstitial, rewarded and banner ads, consent setters, a test mode that never touches your own ad account, and two signals for bad ads.

**Pre-release.** The API may still change before 0.1.0.

## Install

1. Download `intermission-<version>.zip` from the [releases](https://github.com/chaosmos-studio/godot-intermission/releases) and unzip it into your project, so you have `addons/intermission/`.
2. Enable Intermission under Project Settings, Plugins.
3. In your Android export preset, turn on Use Gradle Build (Project, Install Android Build Template, first).

The export adds the plugin and Unity's SDK (fetched by Gradle from Maven Central, versions pinned) to the build.

## Use

```gdscript
var ads := Intermission.new()

func _ready() -> void:
	add_child(ads)
	ads.initialized.connect(_on_initialized)
	ads.ad_closed.connect(_on_ad_closed)
	ads.set_gdpr_consent(false)
	ads.initialize("YOUR_APP_KEY", OS.is_debug_build())

func _on_initialized(ok: bool, _error: Intermission.AdError, _code: int, _message: String) -> void:
	if ok:
		ads.load_ad(Intermission.Format.REWARDED, "YOUR_REWARDED_AD_UNIT")

func show_video() -> void:
	if ads.is_ad_ready("YOUR_REWARDED_AD_UNIT"):
		ads.show_ad("YOUR_REWARDED_AD_UNIT", "hint")

func _on_ad_closed(format: Intermission.Format, _ad_unit_id: String, rewarded: bool) -> void:
	if format == Intermission.Format.REWARDED and rewarded:
		give_hint()
```

Every call returns at once and every answer comes back as a signal.
Off Android every request fails with `NOT_AVAILABLE` on the next frame, so the same code runs everywhere.
The class reference is in `addons/intermission/intermission.gd`, and `demo/` tries every call on a phone.

## Test mode

LevelPlay has no switch for test ads in code.
So in test mode the plugin asks LevelPlay's public demo app for every ad, and your own app and its fraud checks never see your test devices.
Signals still carry your own ad unit ids.
Never tap a live ad on your own phone.

## Bad ads

- `app_left(format, ad_unit_id, clicked)` fires when something outside the app (a store page, another app, the home screen) covers a full-screen ad. With `clicked` false, the ad sent the player away by itself, or the player left.
- `ad_shown` and `banner_shown` name the network that filled the ad.
- `ad_overlong(format, ad_unit_id, seconds)` fires when a full-screen ad stays up longer than `set_overlong_seconds` (60 by default).

## Build

```bash
./gradlew :plugin:testDebugUnitTest :levelplay:testDebugUnitTest addonZip
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
