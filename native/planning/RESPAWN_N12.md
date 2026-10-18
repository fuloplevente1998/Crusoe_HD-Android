# N12 / P0 – Kőhiány feloldása és átmeneti erőforrás-utánpótlás

Kapcsolódó hiba: [#16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16). Kiindulás: natív **0.6.0 alpha**, Java-futtatókörnyezet nélküli saját motor.

## Eredet és bizonyosság

- A játékos beszámolója: az eredeti Java ME-változatban a térképen található gyűjthető dolgok újratermelődtek.
- Az eredeti JAR `BIGislandObjects.byt` rétegében 54 helyen a kő (natív tárgyazonosító 3, térképkód 4) szerepel.
- A jelenlegi natív motor a felszedett tárgyat `objects[index]=0` utasítással eltávolította, de nem pótolta.
- Az eredeti Java ME-kódban a **04:00-s tárgytérkép-visszatöltés időpontja igazolt**: a `data.v.a()` által meghívott `data.ad.s()` akkor futtatja a `data.g.a("BIGislandObjects", ...)` visszatöltést, ha a `data/p.K` játékóra 4, és egy jelző megakadályozza az ismétlést ugyanabban az órában. A `data.v` a `K` órát 0–23 között lépteti. **A többi erőforrás és az egyszeri tárgyak eredeti szűrőrendszere még részben feltárandó**; csak a követ állítjuk vissza automatikusan.

## Bevezetett átmeneti szabály

1. Játékállás betöltésekor, **ha a játékosnál nincs kő és a jelenlegi bejárható területéről egy kő sem megközelíthető**, helyreállít egy követ.
2. Az eredeti 54 kőlelőhely közül csak **eredetileg követ tartalmazó, most üres és a játékos által bejárható mezőt** választ.
3. A választás determinisztikus: a játékoshoz legközelebb eső, a fenti feltételeknek megfelelő kőmezőt állítja vissza. A játékos aktuális mezőjét és más objektummal vagy építménnyel foglalt mezőt kihagyja.
4. A játékbeli **04:00 átlépésekor** az eredeti, jelenleg üres kőlelőhelyeket újratöltjük. A játékos mezőjét, az építményeket és más objektumokkal foglalt helyeket nem módosítjuk. A 00:00-s napváltás önmagában nem termel új követ. Ez a bizonyított eredeti tárgytérkép-visszatöltési mechanika **kőre szűkített, biztonságos első lépése**.
5. Ha a játékos jelenlegi területén nincs biztonságosan elérhető üres, eredeti kőmező, nem jelenik meg új kő és nem adunk automatikusan készlettárgyat; ez külön hibajelzésként kezelendő.

## Mentés és visszafelé kompatibilitás

A mentési fejléc, `sourceHash`, CRC, teljes `objects[]` mező, receptállapot és más adatok **nem módosulnak**. Nem szükséges új mentésverzió; a változás csak a már sikeresen ellenőrzött v1 állapot betöltése UTÁN történik. Egy visszaállított kő egyszer megjelenik az objektumtömbben és a következő mentéssel megőrződik. Az ismételt betöltés nem hoz létre második követ, ha már van megközelíthető.

**Meglévő mentés:** ne töröljük, ne írjuk felül a 0.6.0-s APK-t addig, amíg az új APK aláírását és a migrációt valódi eszközön nem teszteltük.

## Regressziós tesztek

- Játékfüggetlen egységteszt (az eredeti JAR nélkül): `bash native/scripts/test_respawn.sh` (natív könyvtárból: `bash scripts/test_respawn.sh`).
- Privát világadatot használó motorregresszió: `bash native/scripts/test_engine.sh` a korábbi saját `world.pack`-kel. Tartalmazza a 0 köves v1 mentés és az elzárt térképrésszel kapcsolatos teszteket.
- A kezdőterületről a mostani bejárhatósági modell szerint az 54 eredeti kőlelőhelyből **5 közvetlenül megközelíthető** akkor, ha az összes korábbi követ felszedtük. Helyi térképaudit: `(13,136)`, `(23,147)`, `(33,139)`, `(38,153)`, `(35,164)` (0-tól számozott mezők).
- A tiszta kőmodul kiegészített Java-egységtesztje **16/16 sikeres**. Ez nem a teljes GameEngine fordítása, nem Android APK-teszt.
- Fizikai telefonon még ellenőrzendő: régi mentés feloldása; 04:00-s utánpótlás; 00:00-nál korai visszaállás hiánya; tábori pihenéssel átlépett 04:00; crafting; kókuszminijáték; automatikus mentés; kilépés-visszatérés.

## Kiadási korlát

**Nincs jóváhagyott APK/release ebben a PR-ben.** A módosítást először le kell fordítani, majd a felhasználó 0 köves mentésével eszközön kell tesztelni. Végleges lezáráshoz fel kell tárni az eredeti játék teljes (nem csak kőre vonatkozó) erőforrás-regenerációját.
