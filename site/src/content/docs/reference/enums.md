---
title: Enums and errors
description: Format, BannerSize, Anchor and AdError, and the LevelPlay codes behind each error.
---

## `Format`

| Value | Meaning |
| --- | --- |
| `INTERSTITIAL` | A full-screen ad between moments of play. |
| `REWARDED` | A full-screen video the player chooses to watch for a reward. |
| `BANNER` | A strip ad. Banners load with `load_banner`, not `load_ad`. |

`Intermission.UNKNOWN_FORMAT` (-1) is the format a signal carries for an ad unit that was never loaded.

## `BannerSize`

`BANNER` (320 by 50 dp), `LARGE` (320 by 90), `MEDIUM_RECTANGLE` (300 by 250), `LEADERBOARD` (728 by 90) and `ADAPTIVE` (the screen's width, with a height the SDK picks).

## `Anchor`

`TOP` or `BOTTOM`: the edge the banner sits on, centred.

## `AdError`

| Value | Meaning | LevelPlay codes |
| --- | --- | --- |
| `NONE` | No error. | |
| `NOT_AVAILABLE` | The plugin is not in this build, or this is not Android. | |
| `NOT_INITIALIZED` | `initialize` was not called, failed, or had not finished. | 625 |
| `INIT_FAILED` | LevelPlay failed to start. | 508 |
| `INVALID_ARGUMENT` | An empty or unknown ad unit id, or an ad unit used for the wrong format. | 624, 626 |
| `NOT_LOADED` | No ad is loaded for that ad unit. | 628, 631, 1023, 1066, 1069 |
| `ALREADY_SHOWING` | Another ad is on screen. | 629, 630, 1022, 1036, 1064, 1067 |
| `NO_ACTIVITY` | The game had no activity to show the ad in. | 1060, 1061, 1062 |
| `NO_FILL` | No network had an ad. | 509, 606, 1024, 1035, 1044, 1058, 1158 |
| `NO_CONNECTION` | The phone is offline. | 520 |
| `TIMEOUT` | The request took too long, or an ad neither showed nor failed within 8 seconds. | 608, 1006, 1025, 1052, 1055 |
| `CAPPED` | A frequency cap or pacing rule set in the dashboard stopped the ad. | 524, 525, 526, 530, 604, 1042, 1043 |
| `EXPIRED` | The loaded ad expired before it was shown. | 1057 |
| `PROVIDER` | Any other code. `provider_code` and `message` carry LevelPlay's own. | all others |

The codes are the constants in LevelPlay 9.8.0's `LevelPlayAdError` and `IronSourceError`.
