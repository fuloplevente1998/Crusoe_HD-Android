# HD renderer verification – 2026-10-18

PR [#2](https://github.com/fuloplevente1998/Crusoe_HD-Android/pull/2) remains **draft**. A successful build and synthetic rendering tests do not establish complete gameplay compatibility.

## Reproduced evidence

- Pinned J2ME Loader source: `13e29830632464719a643043e1113310fe6e64cc`; TinySoundFont submodule: `92a8f0e9fe3c98358be7d8564db21fc4b1142d04`.
- Real local Android build of the previous feature head `0137b6a`: **passed**, `assembleOpenDebug` (6m 24s). This is a new HD build; the older [Actions run](https://github.com/fuloplevente1998/Crusoe_HD-Android/actions/runs/39085239691) only built the baseline.
- Toolchain: JDK 21, Gradle 9.6.0, AGP 9.4.1, Android compile SDK 36, build-tools 36.0.0, NDK 29.0.14206865, ACRA 5.13.1. minSdk 21 / targetSdk 34.
- All ten historical patch installers from `bf82455` to `0137b6a` applied to separate clean copies of the pinned source. The current patch also applies; it changes six open-source runtime files, including `game/GameCanvas.java`.
- The fixed renderer also built successfully: `assembleOpenDebug` (28s incremental). Its APK contains the `CrusoeHD` DEX class, none of the 33 original game classes, no original game asset payload, and no Robolectric test classes. The debug APK signature verifies with v1 and v2 schemes.
- The complete `scripts/build_hd_debug.sh` also passed from a fresh isolated checkout (2m 35s, all 65 tasks executed). The final incremental build with the weak-reference cache-owner correction also passed (21s).
- Six Android graphics regressions passed under Robolectric 4.16.1, SDK 35, **native Skia**: repeated graphics resets, partial flush coordinates, resized-buffer copying, logical image size/anchor, square-art aspect ratio/footpoint, and standard resolution for non-Crusoe/oversized initial viewports.
- Six Python asset-audit tests passed: unchanged originals accepted; changed bytecode, changed terrain, missing frame, truncated PNG and invalid pixel-chunk CRC rejected. All fixtures are synthetic.
- The supplied original game's atlas dimensions, all 20 frame rectangles and all five opaque-pixel fingerprints agree with the current renderer.
- The complete original-data extraction was repeated and matched the old files; the earlier web prototype's six logic tests and three viewport UI runs also passed. Their limited meaning is documented in [PREVIOUS_STATE_AUDIT.md](PREVIOUS_STATE_AUDIT.md).

The first graphics-test attempt bound `javax.microedition` classes to Gradle's mockable Android JAR because Robolectric excludes `javax.*` by default. The committed test runner explicitly loads Java ME classes in the Android sandbox. The passing run therefore checks actual native pixel results, rather than no-op Android mocks.

## Changes from the previous feature head

The old patch enlarged only Canvas presentation buffers. Crusoe draws through GameCanvas, which retained a low-resolution bitmap. The patch now enlarges GameCanvas's drawing surface too, adjusts partial flushes and `drawImage`, copies only active native bounds after resizing, and captures the full active bitmap for raw screenshots. Caches are cleared when another MIDlet classloader becomes active.

Game logic, map coordinates and touch-coordinate conversion are not changed. Optional overrides still use the original 20 BS3 regions. Missing overrides fall back to the original sprite. Source-atlas recognition supports its resource path and five exact opaque-pixel fingerprints for byte-array loading.

## Reproduce locally

Install a full JDK 21 and set its `bin` on `PATH`; set `JAVA_HOME` and `ANDROID_HOME`. The Android SDK needs platform 36, build-tools 36.0.0 and NDK 29.0.14206865, with accepted SDK licenses. Gradle also needs network access for upstream dependencies. Preserve any environment-specific proxy and CA trust settings.

```sh
bash scripts/build_hd_debug.sh
bash scripts/test_hd_renderer.sh
python3 -m unittest discover -s tests -v
python3 scripts/audit_local_assets.py /private/original.jar /private/modern.jar
```

The build refuses to overwrite an existing isolated checkout. Pass another nonexistent directory if you need a fresh build. The test script accepts the same directory as its first argument. APK paths and SHA-256 are printed by the build script. The resource audit never copies or uploads its inputs.

## Still blocking release

1. The older modern splash/v02/20-frame resource JARs referenced in the shared chat are absent here. Re-run the strengthened audit against the actual modern import package. Previous claims of 20 transparent valid PNGs are historical evidence only.
2. Run the complete original MIDlet in this APK on Android, checking startup, all four walking directions, inventory, crafting, interaction, minigames and save/load. No physical device is attached in this environment.
3. Test GameCanvas presentation, screenshots, scrolling, portrait touch input and rotations at 1080×2400 and 1440×3200. Synthetic pixel tests do not cover the full activity, import or OpenGL path.
4. Observe memory and frame rate. At 360×640, the three 4× RGBA drawing/presentation surfaces alone consume about **42.2 MiB**, before cached art, GL textures and runtime overhead. Initial allocation is guarded at 360×640 logical pixels; later viewport growth still needs review.
5. Check partially clipped character frames. Exact full-frame overrides currently fall back to original artwork for partial source rectangles, potentially causing a visible style change at screen edges. Square HD art can be about 1.79–2.34× wider than the legacy slots; overlap and clipping need an actual art/game test.
6. Confirm another Java ME game remains unchanged. The synthetic non-Crusoe allocation test does not replace an import/run test.
7. Terrain, items, creatures, effects and UI art are not yet modernized. This branch currently supplies optional **character** replacements, not a finished whole-game remaster.

Keep original commercial JAR/JAD files, disassembled classes, graphics and map layers out of this public repository and CI artifacts. Build scripts and test fixtures contain no original game payload. The Android APK is a modified emulator; the user imports their game locally. Upstream license notices must accompany redistribution; see [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

## Delivered local APK

`Crusoe-HD-Test-0.1-debug.apk`, 22052368 bytes. SHA-256:

`ed4c9ff64a5bff718378c7311fa8b1c63ebaca75878beb088a502574a74033bc`
