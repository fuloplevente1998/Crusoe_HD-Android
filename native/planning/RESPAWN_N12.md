# N12 / P0 – Kőhiány feloldása és átmeneti erőforrás-utánpótlás

Kapcsolódó hiba: [#16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16). Kiindulás: natív **0.6.0 alpha**, Java-futtatókörnyezet nélküli saját motor.

## Eredet és bizonyosság

- A játékos beszámolója: az eredeti Java ME-változatban a térképen található gyűjthető dolgok újratermelődtek.
- Az eredeti JAR `BIGislandObjects.byt` rétegében 54 helyen a kő (natív tárgyazonosító 3, térképkód 4) szerepel.
- A jelenlegi natív motor a felszedett tárgyat `objects[index]=0` utasítással eltávolította, de nem pótolta.
- A pontos **eredeti respawn-időzítés, esély, tárgylista és időjárási/küldetésfüggőség MÉG NINCS igazolva**. Ezért a kód **ideiglenes softlock-megelőző javítás**, nem végleges, bizonyított rekonstrukció.

## Bevezetett átmeneti szabály

1. Játékállás betöltésekor, **ha a játékosnál nincs kő és a jelenlegi bejárható területéről egy kő sem megközelíthető**, helyreállít egy követ.
2. Az eredeti 54 kőlelőhely közül csak **eredetileg követ tartalmazó, most üres és a játékos által bejárható mezőt** választ.
3. A választás determinisztikus: a játékoshoz legközelebb eső, a fenti feltételeknek megfelelő kőmezőt állítja vissza. A játékos aktuális mezőjét és más objektummal vagy építménnyel foglalt mezőt kihagyja.
4. Minden új játékbeli nap kezdetén legfeljebb **egy újabb kő** jelenik meg, ugyanilyen védett feltételekkel. A napi ütemezés **ideiglenes, nem az eredeti játékból bizonyított érték**.
5. Ha a játékos jelenlegi területén nincs biztonságosan elérhető üres, eredeti kőmező, nem jelenik meg új kő és nem adunk automatikusan készlettárgyat; ez külön hibajelzésként kezelendő.

## Mentés és visszafelé kompatibilitás

A mentési fejléc, `sourceHash`, CRC, teljes `objects[]` mező, receptállapot és más adatok **nem módosulnak**. Nem szükséges új mentésverzió; a változás csak a már sikeresen ellenőrzött v1 állapot betöltése UTÁN történik. Egy visszaállított kő egyszer megjelenik az objektumtömbben és a következő mentéssel megőrződik. Az ismételt betöltés nem hoz létre második követ, ha már van megközelíthető.

**Meglévő mentés:** ne töröljük, ne írjuk felül a 0.6.0-s APK-t addig, amíg az új APK aláírását és a migrációt valódi eszközön nem teszteltük.

## Regressziós tesztek

- Játékfüggetlen egységteszt (az eredeti JAR nélkül): `bash native/scripts/test_respawn.sh` (natív könyvtárból: `bash scripts/test_respawn.sh`).
- Privát világadatot használó motorregresszió: `bash native/scripts/test_engine.sh` a korábbi saját `world.pack`-kel. Tartalmazza a 0 köves v1 mentés és az elzárt térképrésszel kapcsolatos teszteket.
- A kezdőterületről a mostani bejárhatósági modell szerint az 54 eredeti kőlelőhelyből **5 közvetlenül megközelíthető** akkor, ha az összes korábbi követ felszedtük.
- Fizikai telefonon még ellenőrzendő: eredeti mentés feloldása, a ténylegesen használható kő, napváltás, elérhető kövek, crafting, kókuszminijáték, automatikus mentés, kilépés-visszatérés.

## Kiadási korlát

**Nincs jóváhagyott APK/release ebben a PR-ben.** A módosítást először le kell fordítani, majd a felhasználó 0 köves mentésével eszközön kell tesztelni. Végleges lezáráshoz fel kell tárni az eredeti játék teljes (nem csak kőre vonatkozó) erőforrás-regenerációját.
