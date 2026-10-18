# Codex átadás – Crusoe natív 0.6.1 (N12 teljes erőforrás-respawn)

**Ág:** `fix/n12-stone-regrowth-softlock` · **PR:** [#17](https://github.com/fuloplevente1998/Crusoe_HD-Android/pull/17) · **Hiba:** [#16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16)

## Cél

A telefonon már telepített `hu.crusoe.standalone` 0.6.0-native-alpha alkalmazásra **adatvesztés nélkül rátelepíthető** 0.6.1-es APK készítése **a korábban használt keystore-ral**. Nem kell külön `n12test` alkalmazás, Windows, ADB vagy manuális mentésmozgatás a felhasználónak.

## Kiadási biztonsági feltételek

- **Alkalmazásazonosító:** `hu.crusoe.standalone` (nem változhat).
- **Korábbi verziókód:** 7. **Új verziókód:** 8, verziónév: `0.6.1-native-alpha`.
- **Várt aláíró tanúsítvány SHA-256:** `ce51e3426040b1497c65fd230f875e602d4766143ba3f505475e0996599b40c1` a korábbi 0.6.0 kiadás ellenőrzött metaadatai alapján. A keystore **privát**, tilos commitolni vagy logba másolni.
- A Codex munkakörnyezetében megőrzött kulcs elvárt relatív elérési útja: `native/private/signing/native-debug.keystore`. Ha más helyen van, biztonságosan helyezd ide **helyben**, de **ne készíts új keystore-t**.
- A `native/app/src/main/assets/world.pack` a privát vagy korábbi, ellenőrzött Android 0.6.0 munkakörnyezetből használható. Elvárt SHA-256: `324279ccfdb618d769572f1233d64c99a71725f5cf138acc84a7e1c0231bc6ce`; ne publikáld nyers erőforrásként a forráságon.
- Az APK-ra rátelepíthetőség csak akkor várható, ha a csomagnév, a nagyobb verziókód **és ugyanaz az aláíró tanúsítvány** teljesül.

## Egyetlen buildparancs Codexben

```bash
cd native
bash scripts/build_signed_update_codex.sh
```

A parancs futtatja a teljes natív motortesztet és az Android-egységteszteket, megépíti a debug APK-t, majd az `apksigner` segítségével ellenőrzi, hogy az APK **ugyanazzal a tanúsítvánnyal** készült. Rossz aláírás esetén hibával leáll, és **nem hoz létre kiadható `Update.apk` fájlt**.

Sikeres esetben a helyi eredmény: `native/artifacts/Crusoe-Native-0.6.1-Update.apk` és a `*.sha256` ellenőrzőösszeg.

## Fejlesztés tartalma

- A **04:00-s** eredeti Java ME térképi frissítés kiterjesztése a natív motorban **mind a 36 visszatérő természeti tárgykódra, 1 024 eredeti lelőhelyre** (a kőn kívül fa/ág/lián/fű, gyümölcsök, gombák, kagylók, giliszták stb.).
- Az eredeti `data.y.a(int)` kizárási listáját felhasználjuk az állati/dinamikus entitások kiszűrésére.
- A natív motor további védelmei: egyedi felszerelés, történeti tárgy, helykitöltő, nem szabad térképcsempe és épített objektum védelme; nincs önkényesen új helyen spawn.
- A játékos korábban elakadt, **0 köves v1 mentését** továbbra is betöltéskor javítjuk egy elérhető, eredeti kőlelőhellyel, ha a környezetében nincs megközelíthető kő.
- A mentési fájl formátuma változatlan; nincs készlet-/küldetés-/pozícióreset.

## Minőségkapu a felhasználói APK-hoz

1. N12 erőforrás- és teljes natív motortesztek, Robolectric/Android tesztek zöldek.
2. Ugyanaz a csomagnév és aláírás; verziókód 8. Ha eltér, **ne telepítsd**.
3. A telefonon **adatmegőrző frissítésként** telepítendő; a 0.6.0 alkalmazást **nem szabad eltávolítani**.
4. Az eredeti, kőhiányos játékállásból `Folytatás`, közvetlen kőhelyreállítás és más erőforrások 04:00-s frissülése ellenőrizve.
5. Mentés, ismételt betöltés, ismételt 04:00, pihenés, crafting, állatok/egyszeri tárgyak védelme ellenőrizve.
6. Fizikai telefonos próba és visszaellenőrzött aláírás nélkül a PR maradjon Draft, ne legyen végleges release.

A GitHub Actions `n12test` QA-buildje külön tesztcsomag, **nem** a végleges rátelepíthető frissítés.
