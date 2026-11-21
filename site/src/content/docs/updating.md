---
title: Updating
description: How games get new versions of Intermission, what the version numbers mean, and how to hear about releases.
---

Godot does not update addons by itself.
A game keeps the version it was built with until its developer installs a new one, and a game already on players' phones changes only when you ship a new build.

## Getting a new version

1. Read the release's notes on [GitHub Releases](https://github.com/chaosmos-studio/godot-intermission/releases) or in the [Changelog](/intermission/changelog/).
2. Close the project in Godot.
3. Delete `addons/intermission/`.
4. Download the new `intermission-<version>.zip`, check it against `SHA256SUMS.txt`, and unzip it in your project's folder, as on [Install](/intermission/install/).
5. Open the project, export, and test in [test mode](/intermission/guides/test-mode/) before you ship.

Deleting the old folder first matters: files a release removed would otherwise stay behind.

If you installed from the Godot Asset Library or the Asset Store, install the new version from there the same way.

## Version numbers

Intermission uses [semantic versioning](https://semver.org/): `MAJOR.MINOR.PATCH`.

- **Patch** (0.1.0 to 0.1.1): fixes only. Your code keeps working.
- **Minor** (0.1 to 0.2): new features, and new pinned SDK versions. While the version is below 1.0, a minor release may also break something, and its notes say what and how to change your code.
- **Major** (from 1.0 on): any breaking change.

These count as breaking:

- a method, signal, argument or enum value removed or renamed,
- a default that changes what happens (for example a check that was off turning on),
- a higher minimum Godot version or Android API level,
- an SDK update that needs a change in the game's dashboard setup or its store declarations.

## The SDKs each release pins

Each release pins exact versions of Unity's SDKs, so a build never picks up an untested one.
Updating Intermission is how a game gets newer SDKs.

| Intermission | LevelPlay | Unity Ads | Unity Ads adapter | Ad Quality | coherencelib | Play services ads-identifier | Play services appset |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0.1.0 | 9.8.0 | 4.22.0 | 5.17.0 | 9.12.1 | 0.3.0 | 18.3.0 | 16.1.0 |

## Hearing about releases

On the [GitHub repository](https://github.com/chaosmos-studio/godot-intermission), choose **Watch**, then **Custom**, then **Releases**.
GitHub then tells you about each new release, and nothing else.
