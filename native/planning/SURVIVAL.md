# Túlélés – 0.7.0, az N04 és N10 első bővítése

## Mi változott a játékban?

- A tarisznya fogyasztható tárgyainál látszik az élelem-, víz- és élethatás, a szükséges eszköz és az ártalmas hatás figyelmeztetése. Hiányzó eszközzel a Fogyasztás gomb inaktív. Az eszköz megmarad, egy tárgy fogy.
- A felső állapotsáv legfeljebb 10%-os élet-, víz-, élelem- vagy erőértéknél figyelmeztet. A tárgyfelvétel és más művelet üzenetét nem takarja el.
- Nullára csökkenő életnél a kaland megáll: nincs mozgás, időléptetés, evés, gyűjtés, készítés vagy minijáték-jutalom. A halálképernyőről az utolsó élő mentés, megerősített újrakezdés vagy a főmenü választható. A vadászati sérülés és a mérgező étel ugyanide vezet.
- A tábori pihenés előtt megerősítés látható a nyolcórás időugrással, az élelem/víz várható változásával és készlethiány esetén külön figyelmeztetéssel. Mégsem esetén semmi nem változik.

## Az eredetiben ellenőrzött alapok

A helyi eredeti JAR ellenőrzőösszege a `gameplay-requirements.json` állományban található. Az eredeti fájl és visszafejtett kód nincs a nyilvános repóban.

- Az eredeti `data.y` tárgyadat-betöltője és `data.ad.g()` fogyasztási művelete külön élelem-, víz- és életértéket használ. A kivont tárgyadat élelem/víz oszlopában a **-1 „nincs hatás”**, nem egy pont levonása.
- Példák a kivont adatokból: mangó +3 élelem, +15 víz, +8 élet; kókusz +3/+25/+8 és kőeszköz; vörös gomba -50 élet és tűzfeltétel. A natív motor a tárgyadat életmódosítóját egyszer alkalmazza. Az eredeti fogyasztási kód minden eszközfüggő ága és esetleges többszörös mellékhatása még nincs teljesen egyeztetve.
- Az eredeti `data.ad.b()` és a sérülést kezelő `b(int)` nulla életnél a halálállapotra vált; a natív motorban eddig nem volt működő halálállapot.
- Az eredeti HUD (`data.aa`) a legfeljebb 10%-os főértékeket kiemeli. A natív változat ehhez szöveges, magyar figyelmeztetést ad.

## Mentés és visszatöltés

Az `island-v1.save` formátuma és neve változatlan. Nulla életből a halálállapot új mező nélkül visszaállítható. A korábbi 0.3.1-es és 0.5.0-s natív mentések továbbra is a meglévő tesztbemenetek.

Új helyi fájl az **`island-checkpoint-v1.save`**. Élő játékos mentésekor először ez, utána a fő mentés készül el, fájlonként a korábbi atomikus írással. Halott állapot mentése csak a fő fájlt írja, az élő mentést nem. Régi élő mentés folytatásakor azonnal elkészül az élő másolat is. A fő fájl a halált megőrzi, így az app újraindítása önmagában nem támaszt fel.

A visszatöltés külön játékállapotban ellenőrzi a másolatot, az élő állapotot a fő fájlba menti, majd vált át rá. Sérült vagy halott másolatnál a jelenlegi állapotot nem cseréli le. Új kaland csak megerősítés után írja felül mindkét mentést; így egy régi kaland élő másolata nem kerül át az új játékba. Az élő másolat betöltése tudatos visszalépés a mentés időpontjához, az azóta végzett műveletek elvesznek.

Az N12 napi 04:00-s, **36 természetes tárgykódot / 1 024 lelőhelyet** érintő frissítése változatlan. Halott mentés betöltése nem indít kőhiányos mentésmentő visszatöltést.

## Tudatosan még egyszerűsített

Ez **nem a teljes eredeti túlélési rendszer**. A natív világóra sebessége, az éhezés/szomjazás fokozatos élethatása és az extra erősáv korábbi modellje megmaradt. Az eredeti teljes időfüggő fogyás, betegség/morál, ruházati védelem és alvásképlet további N04/N05/N02 munka. A nyolcórás pihenés, -8 élelem, -12 víz és teljes erő a jelenlegi natív modell előnézete, nem újonnan igazolt eredeti képlet.

Az élő mentéshez visszalépés a natív változat játékossegítő funkciója; az eredeti halál utáni teljes mentéspolitikával való egyezését nem állítjuk. A két mentés fájlonként atomikus, nem közös tranzakció. A teljes N10 mentési és folyamatkilövési vizsgálat még hátra van.

## Ellenőrzés

`bash scripts/test_engine.sh`: a korábbi 61 erőforrás- és 323 helyi motorfeltétel mellé 22 új túlélési feltétel került: eredeti ételadat, szükséges eszköz, eszközmegőrzés, mérgezés, nulla élet utáni változatlanság, jutalomtiltás, halott v1 mentés és kritikus jelzések. A motorfeltételek száma a két külön helyi régi mentés nélkül 321.

Az Android/Skia vizsgálatokban tényleges tarisznya- és fogyasztásgomb, mérgezés után bezáruló menük, két felbontású halálképernyő, app-újralétrehozás utáni halál, érintéses élőmentés-visszatöltés, pihenés megerősítése/megszakítása, sérült másolat, megszakított és megerősített újrakezdés, valamint halálos vadászati kudarc szerepel. Fizikai telefonon ezeket még ellenőrizni kell.
