---
title: Full-screen ads
description: Load, show and close interstitial and rewarded ads.
---

Interstitial and rewarded ads work the same way: load one per ad unit, show it when the moment comes, then load the next.

```gdscript
const INTERSTITIAL := "YOUR_INTERSTITIAL_AD_UNIT_ID"

func _ready() -> void:
	ads.ad_loaded.connect(_on_loaded)
	ads.ad_failed_to_load.connect(_on_load_failed)
	ads.ad_closed.connect(_on_closed)
	ads.load_ad(Intermission.Format.INTERSTITIAL, INTERSTITIAL)

func between_levels() -> void:
	if ads.is_ad_ready(INTERSTITIAL):
		ads.show_ad(INTERSTITIAL, "level_end")
```

## Loading

- Each ad unit holds one loaded ad at a time.
- Asking again while a load is running does nothing, and asking again once an ad is ready answers `ad_loaded` again.
- An ad unit belongs to one format. Loading it as the other format fails with `INVALID_ARGUMENT`.
- A load asked for before `initialize` has finished waits for it. Before `initialize` is called at all, it fails with `NOT_INITIALIZED`.
- When a load fails, wait before you try again. Starting at 30 seconds and doubling up to 10 minutes keeps a game polite when the network has no ads.

## Showing

`show_ad(ad_unit_id, placement)` shows the ad loaded for that ad unit.
`placement` names the moment in LevelPlay's reports, and it may be empty.

The ad either shows, with `ad_shown(format, ad_unit_id, network)`, or fails, with `ad_failed_to_show(format, ad_unit_id, error, provider_code, message)`:

| Error | When |
| --- | --- |
| `NOT_LOADED` | No ad is loaded for that ad unit. |
| `ALREADY_SHOWING` | Another full-screen ad is on screen. |
| `EXPIRED` | The loaded ad is no longer available. LevelPlay has no signal for this, so the plugin checks just before it shows. Load another. |
| `TIMEOUT` | The ad neither showed nor failed within 8 seconds. |

While the ad covers the game, Godot pauses, and your scripts get `NOTIFICATION_APPLICATION_PAUSED` and then `NOTIFICATION_APPLICATION_RESUMED`.

## Closing and rewards

`ad_closed(format, ad_unit_id, rewarded)` arrives once, when the ad is gone.

For a rewarded ad, `rewarded` is true once the player has earned the reward.
LevelPlay can report the reward a moment after the close, so the plugin waits up to a second for it before it sends `ad_closed`.
Give the reward in your `ad_closed` handler and nowhere else.

The amount is yours to choose in the game.
The reward name and amount set in LevelPlay's dashboard are not passed on.

## Taps

`ad_clicked(format, ad_unit_id)` arrives when the player taps the ad.
Never tap your own live ads. See [Test mode](/intermission/guides/test-mode/).

## Good manners

Interstitials are the format players resent most.
Some rules of thumb that keep them bearable:

- Never on the first launch, and never in the middle of play.
- Only at a natural break, such as after a level is won.
- A few minutes apart, and none right after a purchase or a rewarded video.
- Back off for a day after one [misbehaves](/intermission/guides/misbehaving-ads/).
