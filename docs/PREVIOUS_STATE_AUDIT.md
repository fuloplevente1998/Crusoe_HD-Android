# Korábbi állapotok visszaellenőrzése – 2026-10-18

A vizsgálat a megosztott beszélgetés, a megmaradt helyi fájlok és a GitHub-előzmények alapján készült. A régi tesztnaplók állításait külön kezeljük a most megismételt ellenőrzésektől.

| Állapot | Most elvégzett ellenőrzés | Eredmény és korlát |
|---|---|---|
| Eredeti S60v5 JAR és kinyert adatok | Újrakinyerés és bájtpontos JSON/receptek összehasonlítása; öt réteg közvetlen összevetése a JAR-ral | Egyezik: 120 tárgybejegyzés (ebből egy helykitöltő), 45 recept, 5×180×180 térképcella, 1379 kezdeti tárgyelhelyezés, 13 térképhez kötött bevezető eseménytípus. A 13 esemény nem a játék összes küldetése. |
| Korai webes/Capacitor prototípus | A meglévő hat logikai teszt és Playwright-felületi teszt újrafuttatva 1440×900, 390×844 és 915×412 méreten | Átment. Saját, csökkentett játékmenetet vizsgál, az eredeti 45 receptből csak kilenc szerepel benne. Nem tekinthető az eredeti teljes átiratának. |
| Korábbi teljes térképelőnézet | Az alatta lévő térképadatok összevetése az eredeti öt réteggel | Az adatok egyeznek. A modern rajz és a tile-kódok vizuális értelmezése közelítés; a kép nem a HD Android futtatókörnyezetből készült. |
| Saját Java ME/AWT futtatási kísérlet | A runtime-JAR 33 osztályának hash-összevetése; a tesztprogram forrásának és naplójának áttekintése | Egy osztály módosult: `data/k.class` (képernyőméret-patch). A napló `SUCCESS` sora pusztán a program végére érését jelenti; nem igazolja a mozgást, feladatokat vagy mentések helyes visszatöltését. Ezt a kísérletet nem emeljük át a Loader-ágba. |
| GitHub baseline | A baseline és a HD-ág tartalmának összevetése | A korábbi zöld Actions-build a HD nélküli Loadert fordította. Önmagában nem HD-ellenőrzés. |
| PR #2 történeti patchjei | Tíz korábbi installer futtatva külön tiszta másolatokon, ugyanarra az upstream commitra | Mind a tíz illeszkedik. Ez forrásillesztési ellenőrzés; nem tíz külön telefonos teszt. |
| PR #2 korábbi feje: `0137b6a` | Tényleges helyi `assembleOpenDebug` JDK 21, SDK 36 és NDK 29 környezetben | Sikeres APK-fordítás. A forrásvizsgálat közben talált rajzolási hibák miatt további javítás szükséges volt. |
| A beszélgetésben hivatkozott modern JAR-ok | A letöltési hivatkozások és a megmaradt fájlok felmérése | Az egykori sandbox-fájlok nem érhetők el itt. A modern splash-, v02 karakter- és húszképes HD JAR korábbi bájtazonossági/átlátszósági állításai most nem ismételhetők meg. |

Az eredeti, megmaradt JAR SHA-256 értéke:

`f7066eacb94da5e624d488a9b0e058b1f1b70f8c3cb39c6a37a8b8e8e68c74a8`

A renderelő mind a húsz karakterkivágásának koordinátája és mind az öt atlasz-pixelazonosítója egyezik ezzel a JAR-ral. Az eredeti JAR, a kinyert tartalom és a privát vizsgálati munkafájlok nem kerültek a nyilvános repóba.

## Talált hibák és javításuk

- A korábbi patch csak a Canvas két megjelenítési pufferét növelte meg. A Crusoe által használt **GameCanvas saját rajzfelülete** kis felbontású maradt volna. Az új patch ezt is 4× felbontással hozza létre.
- A részleges `flushGraphics` forráskoordinátái nem követték a nagyobb bitmapet. A másolás most a logikai célterülethez tartozó natív képpontokat használja.
- Átméretezés után a képmásolás a teljes korábban lefoglalt bitmapet nyomta volna az aktív területbe. Most az aktív, méretezett határokat másolja.
- A `drawImage` most megtartja a HD puffer logikai méretét és horgonyát.
- A nyers képernyőmentés korábban a HD kép bal felső részét vágta volna ki. Most a teljes aktív natív képet veszi.
- Másik játék betöltésekor törlődik a HD képek és hiányzó képek gyorsítótára.
- A korábbi offline asset-audit csak PNG-fejléceket és fájlneveket nézett. Most PNG-chunk ellenőrzőösszeget, csonkolást, az eredeti osztályok/adatok bájtazonosságát és az új osztályok hiányát is ellenőrzi.

## Nyitott ellenőrzések

A modern képcsomagot újra vizsgálni kell, ha ismét rendelkezésre áll. A tényleges Android-játékban még ellenőrizendő a teljes GameCanvas-útvonal, az érintés, négy mozgásirány, leltár, készítés, minijátékok, mentés/visszatöltés, pályaszéli kivágás, forgatás és memóriahasználat.

A renderelő jelenleg csak **húsz karakterrégióhoz** kínál opcionális képcserét. A terep és a többi grafika felújítása még nincs kész. Részben levágott karakterrégió esetén jelenleg az eredeti kép rajzolódik ki; ezt a pályaszéleken külön vizsgálni kell.

Az új automatizált ellenőrzések eredményét és a buildállapotot a [HD_RENDERER_QA.md](HD_RENDERER_QA.md) rögzíti. A PR az eszköztesztekig draft marad.
