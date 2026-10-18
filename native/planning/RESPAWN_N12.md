# N12 / P0 – A teljes, naponta 04:00-kor újratermelődő erőforrásrendszer

Kapcsolódó hiba: [#16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16). Kiindulás: natív **0.6.0 alpha**, Java-futtatókörnyezet nélküli saját motor.

## Eredet és bizonyosság

- A játékos beszámolója: az eredeti Java ME-változatban a térképen található gyűjthető dolgok újratermelődtek.
- Az eredeti JAR `BIGislandObjects.byt` rétegében **1 379 elhelyezett térképi objektum** szerepel. Ebből **36 rendszeresen gyűjthető tárgykód 1 024 lelőhelyen**; a kő (ID 3, térképkód 4) 54 lelőhelyet ad. A visszatérő kategóriák között kő, ág, lián, száraz fű, farönk, bambusz, moha, agyag, giliszta, gyapot, gyümölcsök, gombák, kagylók, tojás és további természeti készletek vannak.
- A jelenlegi natív motor a felszedett tárgyat `objects[index]=0` utasítással eltávolította, de nem pótolta.
- Az eredeti Java ME-kódban a **04:00-s tárgytérkép-visszatöltés időpontja igazolt**: a `data.v.a()` által meghívott `data.ad.s()` akkor futtatja a `data.g.a("BIGislandObjects", ...)` visszatöltést, ha a `data/p.K` játékóra 4, és egy jelző megakadályozza az ismétlést ugyanabban az órában. A `data.v` a `K` órát 0–23 között lépteti. Az eredeti `data.y.a(int)` kivételeit a natív szűréshez ellenőriztük. Az állatok külön dinamikus rendszert alkotnak, az egyszeri felszerelések és építmények védelmét a natív port külön biztosítja.

### Eredeti játék – ellenőrzött időzítés és szűrés

Az eredeti JAR obfuszkált osztályaiban az alábbi kapcsolódás azonosítható:

- `data.v.a()` → `data.ad.s()`: az ellenőrzés futás közben ismétlődik.
- `data.ad.s()`: ha az óra `data/p.K == 4` és az egyszeri belső jelző még nincs beállítva, meghívja a `data.g.a("BIGislandObjects", objectMap)` betöltést és az objektumok inicializálását. A jelző a 04:00-s órán kívül törlődik, így legközelebb ismét végrehajtódhat.
- `data.v`: a játékóra 0–23 között lépked; a `K` a játékbeli óra, a `J` a perc.
- `data.g.a(String, byte[])` közvetlenül a JAR-beli `/BIGislandObjects.byt` adatokat olvassa; bizonyos objektumkódokat a `data.y.a(int)` szűrővel nem enged újra megjelenni.
- `data.y.a(int)` az `3, 5, 41–45, 47–50, 52–54, 56–60, 62–64` kódokra ad igazat. **A kő eredeti térképkódja 4, ezért nem esik a tiltott újratöltések közé.**

**Következtetés:** a 04:00-s **teljes eredeti objektumtérkép-beolvasás** bizonyított. A natív port e ciklusban most **mind a 36 többször előforduló természeti erőforrást** helyreállítja a pontos eredeti lelőhelyén, összesen **1 024** lehetséges mezőn. A `data.y.a(int)` szerinti dinamikus állatok kihagyandók; a 96-os helykitöltő és az egyszeri (csak egyetlen eredeti példányú) történeti felszerelések védelmet kapnak. A megmaradt vagy megépített tárgyat nem írjuk felül. Ezek utóbbi biztosítékok tudatos natív játékbiztonsági eltérések az eredeti teljes térkép-felülírástól.

### Azonosítás és regresszióvédelem

A 36 visszatérő térképkódot a korábbi natív 0.6.0 APK-ba csomagolt, SHA-256-tal ellenőrzött `world.pack` **eredeti objektumrétegéből és 120 elemű tárgykatalógusából** ellenőriztük. Kódok (ezek **térképkódok, nem inventory-ID-k**):

`1, 2, 4, 7, 8, 9, 12, 14, 21, 22, 25, 26, 27, 28, 29, 31, 32, 33, 34, 36, 39, 40, 65, 66, 67, 68, 69, 71, 72, 73, 74, 75, 76, 91, 94, 99`

Ez a felsorolás **1 024** eredeti lelőhelyet fed le. A kezdeti, „egy kódból egynél több van a térképen” heurisztikát eltávolítottuk: ha egy kés, más egyedi felszerelés vagy új építmény több példányban található, az még **nem** jelent automatikusan újratermelődő erőforrást. A helyszín/objektumazonosító és jelenlegi foglaltság alapján történik a visszatöltés. Ha a világadatok új kiadása új természetes tárgytípust ad hozzá, azt külön audit után lehet engedélyezni.

A `ResourceRespawnChecks` tesztek külön ellenőrzik a 36 típus összes eredeti térképkódját, a két példányban elhelyezett történeti tárgyak tiltását, a helykitöltő és dinamikus állatok kihagyását, a játékos alatti mező védelmét és a naponta egyszeri idempotens frissítést.

## Megvalósított 04:00-s erőforrás-frissítés

1. Játékállás betöltésekor, **ha a játékosnál nincs kő és a jelenlegi bejárható területéről egy kő sem megközelíthető**, helyreállít egy követ.
2. Az eredeti 54 kőlelőhely közül csak **eredetileg követ tartalmazó, most üres és a játékos által bejárható mezőt** választ.
3. A választás determinisztikus: a játékoshoz legközelebb eső, a fenti feltételeknek megfelelő kőmezőt állítja vissza. A játékos aktuális mezőjét és más objektummal vagy építménnyel foglalt mezőt kihagyja.
4. A játékbeli **04:00 átlépésekor az összes visszatérő természeti gyűjthető tárgy** visszakerül az eredeti, üres lelőhelyére. A játékos mezőjét, az építményeket és más objektumokkal foglalt helyeket nem módosítjuk. A 00:00-s napváltás önmagában nem frissíti a készleteket. A kőhiányos régi mentés betöltésekor alkalmazott azonnali, egyetlen kő-visszaállítás ettől függetlenül megmarad.
5. Ha a játékos jelenlegi területén nincs biztonságosan elérhető üres, eredeti kőmező, nem jelenik meg új kő és nem adunk automatikusan készlettárgyat; ez külön hibajelzésként kezelendő.

## Mentés és visszafelé kompatibilitás

A mentési fejléc, `sourceHash`, CRC, teljes `objects[]` mező, receptállapot és más adatok **nem módosulnak**. Nem szükséges új mentésverzió; a változás csak a már sikeresen ellenőrzött v1 állapot betöltése UTÁN történik. Egy visszaállított kő egyszer megjelenik az objektumtömbben és a következő mentéssel megőrződik. Az ismételt betöltés nem hoz létre második követ, ha már van megközelíthető.

**Meglévő mentés:** ne töröljük, ne írjuk felül a 0.6.0-s APK-t addig, amíg az új APK aláírását és a migrációt valódi eszközön nem teszteltük.

## Regressziós tesztek

- Játékfüggetlen egységteszt (az eredeti JAR nélkül): `bash native/scripts/test_respawn.sh` (natív könyvtárból: `bash scripts/test_respawn.sh`).
- Privát világadatot használó motorregresszió: `bash native/scripts/test_engine.sh` a korábbi saját `world.pack`-kel. Tartalmazza a 0 köves v1 mentés és az elzárt térképrésszel kapcsolatos teszteket.
- A kezdőterületről a mostani bejárhatósági modell szerint az 54 eredeti kőlelőhelyből **5 közvetlenül megközelíthető** akkor, ha az összes korábbi követ felszedtük. Helyi térképaudit: `(13,136)`, `(23,147)`, `(33,139)`, `(38,153)`, `(35,164)` (0-tól számozott mezők).
- A friss, kifejezett 36-kódos engedélyezési listával **61/61** erőforrás-egységteszt, **321** teljes motorfeltétel és az Android `testDebugUnitTest assembleDebug` sikeres: [Actions #39147036018](https://github.com/fuloplevente1998/Crusoe_HD-Android/actions/runs/39147036018). A tesztek között szerepel a többször előforduló egyedi felszerelés tiltott újratermelődése is. Fizikai telefonos mentésteszt továbbra is szükséges.
- Fizikai telefonon még ellenőrzendő: régi mentés feloldása; 04:00-s utánpótlás; 00:00-nál korai visszaállás hiánya; tábori pihenéssel átlépett 04:00; crafting; kókuszminijáték; automatikus mentés; kilépés-visszatérés.

## Kiadási korlát

**Nincs jóváhagyott APK/release ebben a PR-ben.** A módosítást először le kell fordítani, majd a felhasználó 0 köves mentésével eszközön kell tesztelni. Végleges lezáráshoz fel kell tárni az eredeti játék teljes (nem csak kőre vonatkozó) erőforrás-regenerációját.
