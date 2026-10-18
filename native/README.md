# Crusoe – saját motoros Android-játék

**0.3.0-native-alpha:** önálló Android-alkalmazás saját játéklogikával és Canvas-alapú rajzolással. Közvetlenül a játék főmenüje indul, nincs Java ME-kompatibilitás, emulátor, JAR-import, WebView vagy távoli tartalom. Android 8.0+ szükséges.

Ez az első játszható alap, **nem a teljes eredeti játék elkészült portja**. A korábbi emulátoros APK külön fejlesztési kísérlet volt.

## Elkészült

- Az eredeti öt, 180×180 cellás térképréteg helyi betöltése és a képernyőn látható világ rajzolása. A natív csomag rétegei bájtpontosan egyeznek az eredeti kivonattal.
- Mozgás érintőkar segítségével; érintésre rácsalapú útkeresés, látható célpont és simított kameramozgás.
- Az eredeti 120 tárgybejegyzés és 45 recept. Alapanyag-fogyasztás, megmaradó szerszámok, helyhez kötött készítés, közeli építmények és tárgyak kezelése.
- Tarisznya, készítéslista, szigettérkép és napló; ideiglenes túlélési/időmodell és tábori pihenés.
- Saját, ellenőrzőösszeggel védett mentés, automatikus és kézi mentéssel. Ez nem kompatibilis az eredeti RMS-mentéssel.
- Nagy felbontású, korábban készült grafikai alapok. A terep határai interpoláltak, a textúrák világkoordinátákhoz igazodnak. Nincs teljes szigetméretű, nagy felbontású bitmap lefoglalva.

## Ellenőrizve

A teljes Android APK lefordult JDK 21 / Gradle 9.6.0 / AGP 9.4.1 / SDK 36 környezetben. Öt Android/Skia felületi teszt átment: saját indítóképernyő és érintéses játékindítás, két portréfelbontás és térképrajzolás, érintőkaros mozgás, folytatás mentésből, a tarisznya/készítés/napló megnyitása és a szünet feloldása.

A külön motorvizsgálat 255 feltételt ellenőriz. Ebben szerepel az eredeti kezdőparti késhez vezető út, valódi tárgyfelvétel és fejszekészítés az eredeti pályáról gyűjtött anyagokkal, mind a 45 recept külön vizsgálata, eszközmegőrzés, hibás készítés atomikussága és sérült mentés elutasítása. Ez 255 ellenőrzési feltétel, nem 255 külön végigjátszás.

A DEX-vizsgálat igazolja a saját `GameActivity` jelenlétét, a Java ME- és J2ME Loader-osztályok hiányát. Az APK nem tartalmaz JAR/JAD fájlt, hálózati vagy tárhelyengedélyt nem kér. Az aláírás ellenőrzése sikeres.

## Még hiányzik

A teljes eredeti küldetéslánc és történeti állapotgép; az eredeti halászati/vadászati minijátékok; az állatok mozgása és harca; az időjárás és árapály szabályai; a sziget további területeit megnyitó speciális interakciók és a befejezés. A 13 kinyert bevezető esemény nem az összes küldetés.

A tereprétegek adatai pontosak, de a dekorációk és tárgyak grafikai hozzárendelése még közelítő; több tárgy közös illusztrációt használ. A túlélési/időmodell és az építés helyezése jelenleg egyszerűsített, az eredetihez való viselkedési egyezésük nincs igazolva. Egyes eredeti leírások és bevezető szövegek még angolul jelennek meg. A grafika és az animáció sem végleges.

Fizikai telefonos teszt még nincs. A Skia-tesztek nem bizonyítanak telefonon mért 60 FPS-t vagy alacsony RAM-használatot. Android 8-on a textúra-kompozit kompatibilitása miatt szoftveres rajzolási visszaesés lép életbe; Android 9+-on GPU-s Canvas az alapértelmezés.

## Helyi fordítás

A nyilvános forrás nem tartalmazza az eredeti játékból kinyert adatokat, a helyi világcsomagot vagy az APK-t. A tesztképek és a generált grafikai alapok is helyi bemenetek. Az eredeti JAR-ból csak adatokat olvasunk ki; a játék eredeti végrehajtható osztályai nem kerülnek a natív programba.

```sh
python3 scripts/extract_original.py /private/original.jar --out private/extracted
python3 scripts/prepare_data.py private/extracted
# Helyi grafika: app/src/main/assets/art/{objects,robinson,title,terrain}.png
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
bash scripts/build_android.sh
```

A magyar tárgynevek opcionális helyi fordítása: `private/hu-items.json`. Hiányában a csomag az eredeti angol nevekkel épül. A reprodukcióhoz SDK 36, build-tools 36.0.0 és az első Gradle-függőségletöltéshez hálózat szükséges. Az aktív környezet proxy- és CA-beállításait meg kell őrizni.

A motorfüggőség nélküli ellenőrzés: `bash scripts/test_engine.sh`. Az Android felületi tesztek: `./gradlew testDebugUnitTest`. A projekt saját mentési formátumát a `GameEngine` kezeli; a rajzolás a `GameView`, az Android életciklusa és menük a `GameActivity`, a tiszta adatbetöltés a `GameData` feladata.
