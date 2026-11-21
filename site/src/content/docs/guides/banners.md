---
title: Banners
description: Load a banner, place it at the top or the bottom, hide it and bring it back.
---

One banner at a time.
It loads hidden and appears with `show_banner`.

```gdscript
const BANNER := "YOUR_BANNER_AD_UNIT_ID"

func _ready() -> void:
	ads.banner_loaded.connect(_on_banner_loaded)
	ads.load_banner(BANNER, Intermission.BannerSize.BANNER, "menu")
	ads.show_banner(Intermission.Anchor.BOTTOM, 8.0)

func _on_banner_loaded(_ad_unit_id: String, size_dp: Vector2) -> void:
	make_room_for_the_banner(size_dp.y)
```

## Sizes

| `BannerSize` | Size in dp |
| --- | --- |
| `BANNER` | 320 by 50 |
| `LARGE` | 320 by 90 |
| `MEDIUM_RECTANGLE` | 300 by 250 |
| `LEADERBOARD` | 728 by 90, for tablets |
| `ADAPTIVE` | The screen's width, with a height the SDK picks |

`banner_loaded(ad_unit_id, size_dp)` reports the size that actually loaded, in dp.

## Placing it

`show_banner(anchor, margin_dp)` puts the banner centred at the top (`Anchor.TOP`) or the bottom (`Anchor.BOTTOM`) of the screen, `margin_dp` in from that edge.
Use the margin to keep the banner clear of the system's navigation bar or a notch.

The banner sits in its own layer over the game.
Touches anywhere outside the banner reach the game.
The game does not move out of its way, so leave room for it in your layout, using the height from `banner_loaded`.

## Hiding, showing and destroying

- `hide_banner()` hides it and pauses its refresh.
- `show_banner(anchor, margin_dp)` brings it back. Pass the anchor and margin again, since the defaults are the bottom edge with no margin.
- `destroy_banner()` removes it for good. Load a new one to show a banner again.
- Calling `load_banner` again replaces the current banner.

## Refreshes

LevelPlay refreshes a visible banner by itself, at the interval set in its dashboard.
`banner_loaded` arrives again after each refresh, so the size can change.
A refresh that fails sends `banner_failed_to_load`, and the banner on screen stays.

A hidden banner does not refresh.

## Taps that leave the app

When a banner opens something outside the app, `app_left(BANNER, ad_unit_id, clicked)` arrives.
`clicked` is true when the player tapped the banner in the 5 seconds before.
