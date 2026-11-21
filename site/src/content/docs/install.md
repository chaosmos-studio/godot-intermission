---
title: Install
description: Add Intermission to a Godot 4.7 project and set up the Android export.
---

Intermission is a Godot 4.7 addon with an Android library inside it.
It works in Android exports that use the Gradle build.

## What you need

- Godot 4.7 with its Android export templates.
- An Android export preset with **Use Gradle Build** on.
- An app in Unity LevelPlay, with Unity Ads set up as a network, and its app key and ad unit ids. Unity's [LevelPlay documentation](https://docs.unity.com/en-us/grow/levelplay) covers the dashboard.

## Add the addon

1. Download `intermission-<version>.zip` and `SHA256SUMS.txt` from the [latest release](https://github.com/chaosmos-studio/godot-intermission/releases/latest).
2. Check the download, in the folder you saved both files to:

   ```sh
   sha256sum -c SHA256SUMS.txt          # Linux
   shasum -a 256 -c SHA256SUMS.txt      # macOS
   ```

   On Windows, compare `Get-FileHash intermission-<version>.zip` in PowerShell with the line in `SHA256SUMS.txt`.

3. Unzip it in your project's folder, so you have `addons/intermission/`.
4. In Godot, open **Project > Project Settings > Plugins** and enable **Intermission**.
5. Open **Project > Install Android Build Template**.
6. In **Project > Export**, select your Android preset and turn on **Use Gradle Build**.

## What the export adds

When you export for Android, the addon adds two things to the Gradle build:

- its own library, `addons/intermission/bin/<debug or release>/intermission-<debug or release>.aar`, and
- Unity's SDKs, which Gradle downloads from Maven Central.

Every version is pinned:

| Library | Version |
| --- | --- |
| `com.unity3d.ads-mediation:mediation-sdk` (LevelPlay) | 9.8.0 |
| `com.unity3d.ads-mediation:adquality-sdk` | 9.12.1 |
| `com.unity3d.ads-mediation:unityads-adapter` | 5.17.0 |
| `com.unity3d.ads:unity-ads` | 4.22.0 |
| `com.unity3d.coherence:coherencelib` | 0.3.0 |
| `com.google.android.gms:play-services-ads-identifier` | 18.3.0 |
| `com.google.android.gms:play-services-appset` | 16.1.0 |

Unity's own packages ask for `adquality-sdk` and `coherencelib` by a version range, so the addon holds those two strictly, and a newer release inside the range never slips into a build.

The plugin's library needs Android 7.0 (API 24) or later.

## Permissions

Unity's SDKs add these permissions to your app:
`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION` and `ACCESS_ADSERVICES_TOPICS`.
The last two are for Android's Privacy Sandbox, and your own manifest may remove them.

## Before you publish

- Declare in Google Play Console that the app contains ads, that it uses the advertising ID, and what the SDKs collect, in Data safety.
- Publish the `app-ads.txt` lines LevelPlay gives you on your developer website.
- Ask for consent where the law requires it, and pass the answer on. See [Consent](/intermission/guides/consent/).

Next: the [Quick start](/intermission/quick-start/).
