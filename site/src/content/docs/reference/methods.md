---
title: Methods
description: Every method of the Intermission class.
---

`Intermission` extends `Node`.
Every method returns at once.
Answers arrive as [signals](/intermission/reference/signals/).

## Set up

### available

```gdscript
func available() -> bool
```

True when the Android plugin is in this build.
False in the editor, on other platforms, and in an Android build without the plugin.
When it is false, every request fails with `AdError.NOT_AVAILABLE` on the next frame.

### initialize

```gdscript
func initialize(app_key: String, test_mode: bool = false) -> void
```

Starts LevelPlay with your app key.
Answers with `initialized`.
In test mode every ad comes from LevelPlay's demo app, and `app_key` may be empty. See [Test mode](/intermission/guides/test-mode/).
Calling it again after it succeeded answers `initialized(true)` again, and after a failure it tries again.

### is_initialized

```gdscript
func is_initialized() -> bool
```

True once `initialize` has succeeded.

### is_test_mode

```gdscript
func is_test_mode() -> bool
```

True when the plugin was initialized in test mode.

### sdk_version

```gdscript
func sdk_version() -> String
```

LevelPlay's version, such as `"9.8.0"`, or empty when `available()` is false.

## Privacy

Call these before `initialize`, and again whenever the player changes their choice. See [Consent](/intermission/guides/consent/).

### set_gdpr_consent

```gdscript
func set_gdpr_consent(granted: bool) -> void
```

Consent for personalised ads where the GDPR applies.

### set_us_privacy_opt_out

```gdscript
func set_us_privacy_opt_out(opted_out: bool) -> void
```

A US player's choice not to have their data sold or shared.

### set_child_directed

```gdscript
func set_child_directed(child: bool) -> void
```

For apps, or players, directed at children.

## Full-screen ads

See [Full-screen ads](/intermission/guides/full-screen-ads/).

### load_ad

```gdscript
func load_ad(format: Format, ad_unit_id: String) -> void
```

Loads one interstitial (`Format.INTERSTITIAL`) or rewarded (`Format.REWARDED`) ad for the ad unit.
Answers with `ad_loaded` or `ad_failed_to_load`.
A load asked for before `initialize` has finished waits for it.

### is_ad_ready

```gdscript
func is_ad_ready(ad_unit_id: String) -> bool
```

True when an ad is loaded for the ad unit and not yet shown.

### show_ad

```gdscript
func show_ad(ad_unit_id: String, placement: String = "") -> void
```

Shows the ad loaded for the ad unit.
Answers with `ad_shown` or `ad_failed_to_show`, then `ad_closed`.
`placement` names the moment in LevelPlay's reports, and may be empty.

## Banners

See [Banners](/intermission/guides/banners/).

### load_banner

```gdscript
func load_banner(ad_unit_id: String, size: BannerSize = BannerSize.BANNER, placement: String = "") -> void
```

Loads a banner, hidden until `show_banner`.
Loading another replaces it.
Answers with `banner_loaded` or `banner_failed_to_load`, and `banner_loaded` again after every refresh.

### show_banner

```gdscript
func show_banner(anchor: Anchor = Anchor.BOTTOM, margin_dp: float = 0.0) -> void
```

Shows the banner centred at the top or the bottom of the screen, `margin_dp` in from that edge.

### hide_banner

```gdscript
func hide_banner() -> void
```

Hides the banner. A hidden banner does not refresh.

### destroy_banner

```gdscript
func destroy_banner() -> void
```

Removes the banner.

## Diagnostics

### set_overlong_seconds

```gdscript
func set_overlong_seconds(seconds: float) -> void
```

Turns on `ad_overlong` for full-screen ads still up after `seconds`.
Off (0) by default. See [Misbehaving ads](/intermission/guides/misbehaving-ads/).

### open_test_suite

```gdscript
func open_test_suite() -> void
```

Opens LevelPlay's Integration Test Suite, in a debug build or in test mode, once initialized.
Elsewhere it does nothing. See [Test mode](/intermission/guides/test-mode/).
