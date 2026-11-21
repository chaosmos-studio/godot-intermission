---
title: Consent
description: Pass the player's privacy choices to LevelPlay and Unity Ads.
---

Intermission shows no consent form.
Ask in your own screen, or with a consent platform, and pass the answer on.

```gdscript
func _start_ads(choice_made: bool, personalised: bool) -> void:
	if choice_made:
		ads.set_gdpr_consent(personalised)
	ads.set_child_directed(false)
	ads.initialize("YOUR_APP_KEY", OS.is_debug_build())
```

## The three setters

| Method | What it tells the SDKs | LevelPlay call | What Unity Ads receives |
| --- | --- | --- | --- |
| `set_gdpr_consent(granted)` | Consent for personalised ads where the GDPR applies (the EEA, the UK, Switzerland) | `LevelPlayPrivacySettings.setGDPRConsent` | `gdpr.consent` |
| `set_us_privacy_opt_out(opted_out)` | A US player's choice not to have their data sold or shared | `LevelPlayPrivacySettings.setCCPA` | `privacy.consent` |
| `set_child_directed(child)` | The app, or this player, is directed at children | `LevelPlayPrivacySettings.setCOPPA` | `user.nonBehavioral` |

## When to call them

- Before `initialize`, so the first requests already carry the choice.
- Again whenever the player changes it, for example from a privacy button in your settings.
- Only where you actually asked. Leave `set_gdpr_consent` unset for a player you did not ask, rather than sending a refusal you never received.

## The legal part is yours

Which players must be asked, and how, depends on the law where they are and on your app.
Intermission only carries the answer to the SDKs.
Read Unity's [privacy guidance for LevelPlay](https://docs.unity.com/en-us/grow/levelplay) and take advice where you need it.
