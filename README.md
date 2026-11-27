# Intermission

Ads from Unity LevelPlay, with Unity Ads as the network, for Godot 4.7 games on Android.
Interstitial, rewarded and banner ads through one node and its typed signals.

**Documentation: [chaosmos.studio/tools/intermission](https://chaosmos.studio/tools/intermission/)**

## Install

1. Download `intermission-<version>.zip` and `SHA256SUMS.txt` from the [latest release](https://github.com/chaosmos-studio/godot-intermission/releases/latest), check the zip with `sha256sum -c SHA256SUMS.txt`, and unzip it into your project, so you have `addons/intermission/`.
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
Off Android every request fails with `NOT_AVAILABLE` on the next frame, so the same code runs everywhere.
Interstitials work the same way, and banners load with `load_banner` and appear with `show_banner`.

## Test mode

With `initialize(app_key, true)` every ad comes from LevelPlay's public demo app, so your own app and its fraud checks never see your test phones.
Never show your own app key's ads on your own phone: a phone registered as a test device can still get live ads, since LevelPlay sends a network's test ads only to a device that network is chosen for with "Test ads".
Check your own ad units with loads only.

## Consent

Intermission shows no consent form.
Ask in your own screen, or with a consent platform, and pass the answer on with `set_gdpr_consent`, `set_us_privacy_opt_out` and `set_child_directed`, before `initialize` and whenever the player changes it.

## Build from source

```bash
./gradlew :plugin:testDebugUnitTest addonZip
godot --headless --path demo --import
godot --headless --path demo -s res://tests/run_tests.gd
```

The addon lives in `demo/addons/intermission/`, the Android library in `plugin/`, the documentation site in `site/`, and the design notes in `docs/DESIGN.md`.

## Licence

MIT, see `LICENSE`.
Unity's SDKs are not part of this repository or its releases, and their own terms apply.

Intermission is not sponsored by or affiliated with Unity Technologies or its affiliates.
Unity, Unity Ads and Unity LevelPlay are trademarks or registered trademarks of Unity Technologies or its affiliates in the U.S. and elsewhere.
Intermission is not affiliated with the Godot Foundation.
