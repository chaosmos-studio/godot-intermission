---
title: Misbehaving ads
description: Find out when an ad sends players to the store by itself or will not end.
---

Some ads do things a game would never choose: they open a store page without a tap, or they stay up with no way to close them.
No SDK setting prevents this, so Intermission tells you when it happens, and your game decides what to do.

## `app_left`

```gdscript
ads.app_left.connect(func(format: Intermission.Format, ad_unit_id: String, clicked: bool) -> void:
	if not clicked:
		report_bad_ad(format, ad_unit_id))
```

`app_left(format, ad_unit_id, clicked)` arrives when something outside the app covers a full-screen ad: a store page, another app or the home screen.
It arrives once per ad.

- `clicked` true: the player tapped the ad first. That is a normal click.
- `clicked` false: nobody tapped the ad. Either the ad sent the player away by itself, or the player left the app.

The plugin watches the app's resumed activities, the ad's own full-screen activity among them.
When none has been in front for half a second while an ad is up, the ad has been covered.
Resumed activities, not started ones, are what count, because Google Play's install overlay is see-through and leaves the ad's activity started.

Unity Ads 4.22.0 can open that overlay by itself: its ad player starts whatever store link the ad's own script asks for.
Neither LevelPlay nor Unity Ads has a setting that turns this off.

For banners, `app_left` comes from LevelPlay's own report that the banner left the app, and `clicked` is true after a tap in the 5 seconds before.

## `ad_overlong`

```gdscript
ads.set_overlong_seconds(75.0)
ads.ad_overlong.connect(func(format: Intermission.Format, ad_unit_id: String, seconds: float) -> void:
	report_bad_ad(format, ad_unit_id))
```

`ad_overlong(format, ad_unit_id, seconds)` arrives once if a full-screen ad is still up after the limit set with `set_overlong_seconds`.
It is off until you set a limit, since how long is too long depends on your game.
A rewarded video and its end card often take 30 to 60 seconds, so leave room for them.
`set_overlong_seconds(0)` turns the check off again.

The signal cannot close the ad.
It tells you that the player was stuck, so you can stop showing that kind of ad.

## The network behind an ad

`ad_shown(format, ad_unit_id, network)` names the ad network that filled each full-screen ad, as LevelPlay reports it (for example `unityads`).
Keep it with your reports of bad ads, so you can tell which network they came from.

## What a game can do

- Count bad ads in your analytics, with the format and the network.
- After a bad interstitial, show no interstitials for a while, a day for example.
- Report repeat offenders to the network, and use LevelPlay's and Unity's ad controls to block categories you do not want.
