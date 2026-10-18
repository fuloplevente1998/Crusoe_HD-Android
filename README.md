# Crusoe – saját motoros Android-fejlesztés

Ez az ág a saját Android/Canvas-motoros játék **0.5.0-s alpha-változatát** tartalmazza. Az eredeti öt térképréteg, 120 tárgybejegyzés és 45 recept mellett érintéses mozgás, mentés, horgászat, hálózás és íjas vadászat működik. A teljes történeti port még nem készült el.

Új: közös tárgyazonos ikonok a pályán, a natív tarisznyában és a recepteknél. Külön kagyló-, gyümölcs-, gomba- és szerszámgrafikák, képes készítési kártyák és hozzávalólisták. Az eredeti kezdőhely pontosítása és a kés melletti láda helyes megjelenítése; opcionális út a késhez a naplóból.

Helyben ellenőrizve: 280 motorfeltétel, 11 Android/Skia felületi teszt, öt bájtpontos térképréteg és aláírt APK. A 0.3.1-es natív mentés betölthető. Fizikai telefonos teljesítménymérés még nincs. A korábbi emulátoros kísérlet PR #2-ben külön marad.

Részletek és helyi fordítás: [native/README.md](native/README.md). [Tárgygrafikák és korlátok](native/planning/ITEM_GRAPHICS.md), [minijátékok egyezései és eltérései](native/planning/MINIGAMES.md).

## Development rules

1. Preserve original gameplay rules when porting; explicitly document behavior that is still simplified or absent.
2. Keep original game assets, extracted resources, and ROM/JAR files out of the public repository and CI artifacts.
3. Separate the original 180×180 tile map and game coordinates from physical screen pixel density.
4. Keep changes on feature branches, review them in draft PRs and test on Android before release.
5. A passing build or synthetic rendering test does not establish full game compatibility or measured phone performance.

This public source tree excludes the local world-data package, generated art inputs, test screenshots and APK. Data is prepared locally from the owner's supplied original file. The Java ME game is not executed by this implementation.
