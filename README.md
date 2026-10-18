# Crusoe HD – Android

Experimental Android rendering project for a modernized **Robinson Crusoe: Shipwrecked** experience, keeping the original Java ME gameplay in a separate, user-supplied runtime file.

## Current status

- Public repository initialized; **no playable Crusoe APK is published here**.
- HD renderer integration is experimental and must pass an actual Android build and device test.
- GitHub Actions may build a debug J2ME Loader-based test APK; it will **not bundle the original game's JAR, JAD, graphics, or extracted code**.
- Import a legally obtained JAR locally on the phone after installing the test emulator.

## Development rules

1. Maintain original game logic; apply experiments only to the open-source Java ME runtime.
2. Keep original game assets, extracted resources, and ROM/JAR files **out of the public repository and CI artifacts**.
3. Separate the original 180×180 tile map and game coordinates from physical screen pixel density.
4. Make small reversible changes on feature branches, review in PRs, and test on Android before considering release.
5. Report build status accurately; a green build does not by itself mean HD rendering works in-game.

## Upstream

Runtime based on [J2ME Loader](https://github.com/nikita36078/J2ME-Loader), licensed under Apache-2.0. Preserve its notices and license whenever redistributing its source or binaries. Original game copyrights remain with their respective rights holders.

> This public repository contains only original integration code, configuration, and documentation unless compatible redistribution rights have been confirmed.
