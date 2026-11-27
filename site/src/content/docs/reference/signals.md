---
title: Signals
description: Every signal of the Intermission class, with its arguments.
---

Signals arrive on the thread your scripts run on.
While a full-screen ad covers the game, Godot is paused, and the signals wait in order until it resumes.

Where a signal carries an error, `error` is an [`AdError`](/tools/intermission/reference/enums/#aderror), and `provider_code` and `message` are LevelPlay's own code and text, or `0` and the plugin's own text when the plugin found the problem itself.

## Start

### initialized

```gdscript
signal initialized(ok: bool, error: AdError, provider_code: int, message: String)
```

`initialize` finished. `error` is `NONE` when `ok`.

## Full-screen ads

### ad_loaded

```gdscript
signal ad_loaded(format: Format, ad_unit_id: String)
```

An ad is loaded and ready to show.

### ad_failed_to_load

```gdscript
signal ad_failed_to_load(format: Format, ad_unit_id: String, error: AdError, provider_code: int, message: String)
```

The load failed. `NO_FILL` is the common one: no network had an ad.

### ad_shown

```gdscript
signal ad_shown(format: Format, ad_unit_id: String, network: String)
```

The ad is on screen. `network` names the ad network that filled it, as LevelPlay reports it.

### ad_failed_to_show

```gdscript
signal ad_failed_to_show(format: Format, ad_unit_id: String, error: AdError, provider_code: int, message: String)
```

The ad could not be shown.
For an ad unit that was never loaded, `format` is `Intermission.UNKNOWN_FORMAT` (-1).

### ad_clicked

```gdscript
signal ad_clicked(format: Format, ad_unit_id: String)
```

The player tapped the ad.

### ad_closed

```gdscript
signal ad_closed(format: Format, ad_unit_id: String, rewarded: bool)
```

The ad is gone.
`rewarded` is true once a rewarded ad has earned its reward. Give the reward here.

## Banners

### banner_loaded

```gdscript
signal banner_loaded(ad_unit_id: String, size_dp: Vector2)
```

The banner loaded, at this size in dp. Again after every refresh.

### banner_failed_to_load

```gdscript
signal banner_failed_to_load(ad_unit_id: String, error: AdError, provider_code: int, message: String)
```

The banner, or one of its refreshes, failed to load.

## Ads that misbehave

See [Misbehaving ads](/tools/intermission/guides/misbehaving-ads/).

### app_left

```gdscript
signal app_left(format: Format, ad_unit_id: String, clicked: bool)
```

Something outside the app covered an ad.
With `clicked` false, nobody tapped the ad first.

### ad_overlong

```gdscript
signal ad_overlong(format: Format, ad_unit_id: String, seconds: float)
```

A full-screen ad is still up after the limit set with `set_overlong_seconds`.
