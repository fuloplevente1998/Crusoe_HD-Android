# Crusoe HD Renderer – verification gate (experimental)

**Branch:** `feat/hd-renderer-v01` · **PR:** #2 (draft; do not merge until validated)

## Verified in this iteration

- Baseline open-source J2ME Loader `openDebug` APK compiled successfully on GitHub Actions [run #39085239691](https://github.com/fuloplevente1998/Crusoe_HD-Android/actions/runs/39085239691).
- Baseline APK **does not include this HD renderer**. HD APK compilation and on-device verification are outstanding.
- The optional HD renderer now checks for the imported game's distinct `/BIGislandFULL.byt` and `/data/BS3.png` resources before allocating higher-resolution framebuffers.
- Source-atlas matching now requires the resource **name** `/data/BS3.png`, not merely a coincidentally matching 348×36 bitmap.
- The memory guard limits 4× framebuffer allocation to logical virtual screens no larger than 360×640.
- Original Java ME game classes, assets and terrain data are **not committed** to this public repository.

## Local-resource inspection (private JAR, not committed)

- Original `data/BS3.png`: **348 × 36 px**.
- HD resource package: **20/20** named `crusoe_hd/BS03_000.png` through `BS03_019.png`.
- All 20 PNGs decode and contain nonempty alpha.
- HD PNG dimensions: **313–314 px square**. The original logical sprites are very narrow and tall; drawing these square replacements directly into the original width/height risks visible distortion.

## Blocking tests before merging

1. Confirm that the modified `apply_hd_patch.py` executes against the exact pinned upstream commit without anchor errors.
2. Compile `assembleOpenDebug` with the modified open-source runtime. The workflow for the feature branch is **not yet enabled**; the connected GitHub write to Actions configuration was blocked.
3. On an Android phone, import the user's local **Crusoe HD resources JAR**; check that original gameplay, inventory, interactions, saves, movement directions and controls still work.
4. Compare Robinson in each of four walking directions and fix the square-image aspect ratio issue; animations must not be stretched.
5. Verify 4× rendering and touch mapping on 1080×2400 and 1440×3200 physical displays; confirm no crashes or excessive memory use.
6. Import a different Java ME game to confirm it stays on standard resolution and its rendering remains unchanged.

## Test policy

Never publish the original commercial JAR/JAD, disassembled game classes, proprietary PNGs, or game data layers to this public repo or Actions artifacts. If a build cannot be reproduced without those inputs, provide a separate local test procedure and do not label it a distributable game APK.


## 2026-10-18 – next iteration: resource recognition and aspect-safe drawing

- The game's original loader can use both `Image.createImage(String)` and `Image.createImage(byte[],offset,length)`; the renderer now accepts the original 348×36 atlas identified by **either its exact resource path or five verified opaque-pixel signatures**.
- The five expected pixel signatures were compared to the owner's local original JAR, with **5/5 exact matches**.
- The graphics patch now resets the backing Canvas matrix between redraw passes to prevent cumulative 4× scaling.
- Modern sprite draw size preserves source aspect ratio and the old logical position/footpoint; the art can occupy more horizontal space than the original (approx. **1.79–2.34×** wider for these 20 frames). This is an intentional visual tradeoff and needs a phone test before approving.
- Offline resource audit confirmed the original 20 animation slots, exact atlas dimensions, 20 named modern PNGs, and valid crop rectangles. **All 20 modern inputs are square-like**, so drawing them into the original narrow 15–19-pixel-wide source rectangle would visibly distort them.
- `scripts/audit_local_assets.py`: verify the owner's original and modern resource JAR files locally, without uploading them.
- `scripts/build_hd_debug.sh`: reproducible, isolated HD emulator debug build instructions, using the working upstream commit, API 36, and ACRA 5.13.1. **This build script has not yet been run in an Android SDK environment**. The working GitHub Action is still the baseline, not HD.

### Still blocking

1. Compile `scripts/build_hd_debug.sh` in an Android SDK/NDK environment and confirm the modified source compiles.
2. Verify resource loading and 4× rendering in Android after importing the owner's HD-resource JAR.
3. Confirm no stretched sprites, clipped oversized characters, broken clipping, or frame-rate regressions, especially while scrolling and opening menus.
4. Confirm snapshots, character animation direction, item interactions, saves and portrait touch controls remain functional.
5. Keep this PR **draft**, separate from `develop`, until the actual HD APK passes device tests.
