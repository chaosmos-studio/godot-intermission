---
title: Quick start
description: Show a rewarded video and give its reward, in one script.
---

Intermission is one class, `Intermission`, a `Node`.
Add one to the tree, connect its signals, initialize it, then load and show ads.
Making it an autoload is a good way to share one across scenes.

```gdscript
extends Node

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

What it does:

- `initialize(app_key, test_mode)` starts LevelPlay. Here debug builds use [test mode](/tools/intermission/guides/test-mode/), so they only ever get test ads, and release builds use your own app.
- `load_ad` asks for one ad for the ad unit. A load asked for before the SDK is ready waits for it.
- `is_ad_ready` answers at once, so a button can show or hide on it.
- `show_ad` shows the loaded ad. The second argument is an optional placement name for LevelPlay's reports.
- `ad_closed` arrives once the ad is gone. For a rewarded ad, `rewarded` is true once the player earned the reward, so that is the one place to give it.
- After an ad closes, load the next one.

## How answers arrive

Every call returns at once.
Every answer comes back as a signal on the thread your scripts run on, so it is safe to touch nodes from a handler.
While a full-screen ad covers the game, Godot is paused, and the signals wait in order until it resumes.

## Off Android

In the editor, on desktop, or in an Android build without the plugin, `available()` is false.
Every request then fails on the next frame with `AdError.NOT_AVAILABLE`, the way a network with no ads would, so a game needs no separate path for these cases.

## Next

- [Full-screen ads](/tools/intermission/guides/full-screen-ads/) for interstitials and the details of loading and showing.
- [Banners](/tools/intermission/guides/banners/).
- [Consent](/tools/intermission/guides/consent/) before you ship.
