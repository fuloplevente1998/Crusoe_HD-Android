# Crusoe HD – Android

Experimental Android rendering project for a modernized **Robinson Crusoe: Shipwrecked** experience, keeping the original Java ME gameplay in a separate, user-supplied runtime file.

## Current status

- A local HD runtime debug APK now compiles; it is a modified emulator and contains no Crusoe game payload.
- The renderer remains experimental. Six native Android graphics tests pass; complete game and physical-device testing are still required.
- GitHub Actions may build a debug J2ME Loader-based test APK; it will **not bundle the original game's JAR, JAD, graphics, or extracted code**.
- Import a legally obtained JAR locally on the phone after installing the test emulator.

## Verification and local builds

See [the current verification record](docs/HD_RENDERER_QA.md) and [the audit of previous states](docs/PREVIOUS_STATE_AUDIT.md). The old web prototype implements reduced gameplay and its passing tests do not prove the original game is fully ported.

With JDK 21 on `PATH`, `JAVA_HOME` set and an Android SDK at `ANDROID_HOME`:

```sh
bash scripts/build_hd_debug.sh
bash scripts/test_hd_renderer.sh
python3 -m unittest discover -s tests -v
```

The build uses an isolated checkout and refuses to overwrite an existing directory. It creates **Crusoe HD Test**, package `ru.playsoftware.j2meloader.crusoehd`, alongside a stock Loader install. Import the original JAR locally; optional modern character frames must be in that same imported package under `crusoe_hd/BS03_000.png` through `BS03_019.png`. An unmodified JAR retains original art. Only character overrides exist so far; the rest of the graphics have not been remastered.

The older modern resource JAR from the shared conversation is not available in this workspace and has not been revalidated. Use `scripts/audit_local_assets.py` to verify that a modern import package preserves all original game code and data.

## Development rules

1. Maintain original game logic; apply experiments only to the open-source Java ME runtime.
2. Keep original game assets, extracted resources, and ROM/JAR files **out of the public repository and CI artifacts**.
3. Separate the original 180×180 tile map and game coordinates from physical screen pixel density.
4. Make small reversible changes on feature branches, review in PRs, and test on Android before considering release.
5. Report build status accurately; a green build does not by itself mean HD rendering works in-game.

## Upstream

Runtime based on [J2ME Loader](https://github.com/nikita36078/J2ME-Loader), licensed under Apache-2.0. Preserve its notices and license whenever redistributing its source or binaries; see [third-party notices](THIRD_PARTY_NOTICES.md). Original game copyrights remain with their respective rights holders.

> This public repository contains only original integration code, configuration, and documentation unless compatible redistribution rights have been confirmed.
