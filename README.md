# Crusoe – saját motoros Android-fejlesztés

Ez az ág az önálló Android-játék **első, 0.3-as alpha-változatát** tartalmazza. Saját játéklogika és natív Canvas-rajzolás; nincs Java ME-kompatibilitási réteg, J2ME Loader, JAR-importáló felület vagy WebView.

A projekt, a reprodukció és a pontos korlátok a [native/README.md](native/README.md) fájlban találhatók. A teljes eredeti küldetéslánc, minijátékok, állati ellenfelek, árapály és befejezés átültetése még hátravan. A korábbi emulátoros kísérlet külön, a `feat/hd-renderer-v01` ágon és PR #2-ben marad meg.

Helyben ellenőrizve: mind az öt eredeti térképréteg bájtpontos betöltése, 255 motorfeltétel (beleértve mind a 45 receptet), öt Android/Skia felületi teszt és sikeres önálló APK-fordítás. Fizikai telefonos teljesítményteszt még nincs. Az APK nem tartalmaz Java ME- vagy J2ME Loader-osztályt.

## Development rules

1. Preserve original gameplay rules when porting; explicitly document behavior that is still simplified or absent.
2. Keep original game assets, extracted resources, and ROM/JAR files out of the public repository and CI artifacts.
3. Separate the original 180×180 tile map and game coordinates from physical screen pixel density.
4. Keep changes on feature branches, review them in draft PRs and test on Android before release.
5. A passing build or synthetic rendering test does not establish full game compatibility or measured phone performance.

This public source tree excludes the local world-data package, generated art inputs, test screenshots and APK. Data is prepared locally from the owner's supplied original file. The Java ME game is not executed by this implementation.
