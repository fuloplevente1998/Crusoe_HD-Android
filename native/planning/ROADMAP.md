# Hátralévő fejlesztések – 0.6.0 → teljes eredeti játékmenet

Állapotfelvétel: **2026-10-18**, kiinduló változat: **0.6.0-native-alpha**. [Kiinduló forráscommit](https://github.com/fuloplevente1998/Crusoe_HD-Android/commit/4d88a00d69547fe2e63db86ba180cbd8d7e7bf70).

**Élő összesítő:** [#4](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/4). A kapcsolt GitHub issue-k mutatják az aktuális munkastátuszt; ez a fájl a kiinduló hiányokat, sorrendet és befejezési feltételeket rögzíti.

A cél az eredeti játékmenet minél hűbb átültetése saját Android-motorba, modern grafikával és natív érintéses felületekkel. A még nem igazolt eredeti viselkedés feltárandó, nem késznek állított szabály.

## Mi működik már?

- Saját Android/Canvas-motor, natív menük, érintőkaros mozgás és útkeresés.
- Öt bájtpontos 180×180-as térképréteg, 120 tárgybejegyzés, 45 recept költség/eszköz/helyfeltétele.
- Képes tarisznya és készítés, térkép, alapnapló, a kés és láda javított megjelenítése.
- Játszható vadászat/horgászat/hálózás/kókuszdobálás; kétujjas érintőkar és külön akciógomb. A teljes eredeti viselkedés még nincs igazolva.
- CRC-védett atomikus mentés, korábbi natív mentések helyi ellenőrzése; GitHubon elérhető forrás, generált grafika és teszt-APK.

Helyi ellenőrzési alap: **13 Android/Skia felületi teszt és 307 motorfeltétel**, a 0.3.1/0.5.0 motorral készített két külön helyi mentési bemenettel. Ez nem teljes végigjátszás és nem fizikai telefonon mért teljesítmény.

## Prioritásos feladatlista

**P0:** az előrehaladást vagy adatbiztonságot megalapozó/blokkoló munka. **P1:** az eredeti részletes viselkedéséhez és a kész kiadáshoz szükséges további fejlesztés. A prioritás nem határidő és nem becsült elkészültségi százalék.

| Azonosító | Prioritás | Feladat | Kiinduló állapot | Előfeltételek |
| --- | --- | --- | --- | --- |
| [N01](#n01) | P0 | [#5 Eredeti előrehaladás feltárása és történeti/naplóállapotok](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5) | hiányos / részben feltárandó | — |
| [N02](#n02) | P0 | [#7 Árapály, időjárás és a bejárható területek dinamikája](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/7) | hiányzik; az idő csak egyszerűsített | N01, N10 |
| [N03](#n03) | P0 | [#11 Speciális tárgyhasználat, tutaj/hajózás és az eredeti befejezés](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/11) | részben hiányzik | N01, N02, N05, N10 |
| [N04](#n04) | P0 | [#8 Túlélési értékek, táplálkozás, pihenés és halálállapot](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8) | egyszerűsített; halálállapot hiányzik | N01, N10 |
| [N05](#n05) | P1 | [#9 Építés, műhelyek, ruházat és tábori tárolás teljes működése](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9) | részben kész | N01, N04, N10 |
| [N06](#n06) | P1 | [#12 Állatok szigetbeli mozgása, veszélyei és harca](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/12) | hiányzik / részben alapozott | N02, N04, N05, N07, N10 |
| [N07](#n07) | P1 | [#10 Minijátékok pontosítása az eredeti fizikához és részletszabályokhoz](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/10) | játszható alap kész; egyezés részleges | N01, N04, N10 |
| [N08](#n08) | P1 | [#13 Grafikai hozzárendelések, méretek és animációk véglegesítése](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/13) | részben kész | N03, N05, N06, N07 |
| [N09](#n09) | P1 | [#14 Teljes magyarítás, napló és helyzetfüggő játékosi visszajelzés](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/14) | részben kész | N01, N02, N03, N04, N05, N06, N07 |
| [N10](#n10) | P0 | [#6 Mentések bővítése és migráció az új világállapotokhoz](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6) | alap kész; az új rendszerekhez bővítendő | — |
| [N12](#n12) | **P0** | [#16 Gyűjthető erőforrások újratermelődése – kőhiány miatti softlock](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16) | **hiányzik; játékosnál a kövek elfogytak, a haladás blokkolva** | N10 (mentés), N02 (eredeti időszabály feltárása) |
| [N11](#n11) | P1 | [#15 Telefonos végigjátszás, teljesítménymérés és kiadási ellenőrzés](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/15) | hiányzik; helyi motor/UI tesztek vannak | N01, N02, N03, N04, N05, N06, N07, N08, N09, N10 |

## Javasolt haladási sorrend

1. **N12 (P0, softlock):** a természetes gyűjthető tárgyak eredeti újratermelődési feltételeinek feltárása, különösen a kő (ID 3) pótlása és az elfogyott köveket tartalmazó régi mentések megmentése; az N10 mentési tervével egyeztetve.
2. **N01 és N10:** az eredeti kötelező útvonal feltárása, mentésbővítési/migrációs terv.
3. **N02 és N04:** árapály/területnyitás, pontos túlélés és vesztési állapotok.
4. **N05, N03, N07, N06:** tábor/felszerelés, speciális tárgyhasználat/hajózás/befejezés, minijátékok és állatok összekapcsolása.
5. **N08 és N09:** véglegesebb grafika/animáció, teljes magyarítás és visszajelzés, folyamatos telefonos próbákkal.
6. **N11:** igazolt teljes végigjátszás, frissítési próba és mért telefonos eredmények.

A sorrend a feltárás eredménye alapján módosítható; a részletes függőségek a táblázatban és az issue-kban szerepelnek.

## Mikor tekinthető teljesnek?

- A kötelező eredeti útvonal a kezdőparttól az igazolt befejezésig telepített APK-ban, csalás vagy előre feltöltött készlet nélkül végigjátszható.
- A természetes gyűjthető erőforrások (elsőként a kövek) az eredetinek megfelelő feltételekkel újratermelődnek; a korábban minden követ felhasználó játékos mentése újrakezdés nélkül folytatható.
- Az eredetihez kötött kötelező átmenetek, túlélési/vesztési állapotok, felszerelés és táborhasználat működnek; a tudatos eltérések tételesen dokumentáltak.
- A mentés/frissítés és az alapvető kezelhetőség ellenőrzött. A fizikai telefonos mérés nem helyettesíthető a helyi renderpróbákkal.

A 13 bevezető esemény nem bizonyít önmagában külön küldetésláncot. Az N01 feladata feltárni, milyen történeti/előrehaladási feltételek vannak ténylegesen az eredetiben; nem kitalált küldetésrendszert építünk.

## Részfeladatok és elfogadási feltételek

<a id="n01"></a>

### N01 · Eredeti előrehaladás feltárása és történeti/naplóállapotok

**P0 · [#5](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5) · Kiinduló állapot: hiányos / részben feltárandó.**

A 13 bevezető esemény és az „Út a késhez” segítség működik. Nincs az eredeti teljes végigjátszásával igazolt előrehaladási állapotmodell. A 13 esemény nem igazol önmagában egy külön küldetésrendszert.

Hátralévő munka:

- Feltárni az eredeti kötelező előrehaladási, tárgy-, hely-, idő- és befejezési feltételeket; a bizonyított szabályokat elkülöníteni a feltételezésektől.
- Elkészíteni a kötelező útvonal és az opcionális felfedezés függőségi leírását. Csak az eredetiben létező történeti/küldetésállapotok kerüljenek át.
- A releváns állapotokat, naplóbejegyzéseket és egyszeri eseményeket a natív motorhoz kötni.

Akkor kész, ha:

- [ ] Minden kötelező átmenethez dokumentált eredeti megfigyelés vagy szabály tartozik, az ismeretlen részek külön látszanak.
- [ ] Az események megfelelő feltételnél és egyszer történnek meg; visszatöltés után is helyesek.
- [ ] A teljes kötelező útvonalhoz van végigjátszási ellenőrzőlista, kezdet és igazolt befejezési feltételek.

Előfeltételek: nincs másik részfeladat előfeltételként rögzítve.

<a id="n02"></a>

### N02 · Árapály, időjárás és a bejárható területek dinamikája

**P0 · [#7](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/7) · Kiinduló állapot: hiányzik; az idő csak egyszerűsített.**

Az öt eredeti térképréteg pontos, de a passability statikus. Van egyszerű óra/napszak és éjszakai színezés, az eredeti árapály/időjárás szabályai nélkül.

Hátralévő munka:

- Feltárni és átültetni az eredeti apály/dagály időzítését, víz-/terepváltozásait és kapcsolódó veszélyeit.
- Az útkeresést, ütközést és a már kijelölt útvonalat a változó bejárhatósághoz kötni.
- Időjárás/napszak jelzés és az eredeti játékmeneti hatások megvalósítása, ahol az eredetiben bizonyítottak.

Akkor kész, ha:

- [ ] Konkrét eredeti átkelőknél az apály valóban megnyitja, a dagály lezárja az utat.
- [ ] Árapályváltás közben sem lehet érvénytelen útvonalon átmenni vagy helyreállíthatatlan helyzetbe kerülni a natív motor hibája miatt.
- [ ] Mentés/visszatöltés után az idő, árapály, időjárás és bejárhatóság konzisztens.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

<a id="n03"></a>

### N03 · Speciális tárgyhasználat, tutaj/hajózás és az eredeti befejezés

**P0 · [#11](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/11) · Kiinduló állapot: részben hiányzik.**

Roncs/tutaj/hajó receptek léteznek; a hajó/tutaj használata még „nem érhető el” üzenetet ad. Az általános tárgyeltávolítás nem bizonyítja az összes speciális interakció helyességét.

Hátralévő munka:

- Ellenőrizni és átültetni a szikla/óriásrönk és más akadályok, különleges eszközök és területnyitó műveletek tényleges szabályait, beleértve a fogyó eszközöket.
- Megvalósítani a tutaj használatát, a vízi bejárhatóságot és az eredeti helyhez/állapothoz kötött közlekedést.
- A kalózhajó helyreállítását az eredeti felszerelés- és állapotfeltételekhez, majd az igazolt menekülés/befejezéshez kötni.

Akkor kész, ha:

- [ ] Minden kötelező területnyitás és vízi útvonal játékból elérhető, tesztkészlet vagy teleport nélkül.
- [ ] Hiányzó követelmény esetén érthető visszajelzés van és nem fogy részlegesen a készlet.
- [ ] Az eredeti kötelező útvonal a kezdőparttól a győzelemig végigjátszható; a győzelem állapota menthető.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N02](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/7), [N05](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

<a id="n04"></a>

### N04 · Túlélési értékek, táplálkozás, pihenés és halálállapot

**P0 · [#8](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8) · Kiinduló állapot: egyszerűsített; halálállapot hiányzik.**

Négy közelítő érték (élet/élelem/víz/erő) és fix nyolcórás pihenés működik. A tárgyoszlopok teljes jelentése és az eredeti túlélési viselkedés nincs igazolva. A nulla élet jelenleg nem indít teljes game-over folyamatot.

Hátralévő munka:

- Az eredeti éhség, szomjúság, energia/erő, morál, súly és kapcsolódó értékek pontos jelentését és változását feltárni; nem azonosítani őket automatikusan a mostani négy mezővel.
- A főzés/fogyasztás, mérgezés, tábori pihenés/alvás és az eszköz-/építményfeltételek eredeti szabályait átültetni.
- Megvalósítani a bizonyított halál/vesztés feltételeket és a játék vége/újrakezdés/folytatás következetes kezelését.

Akkor kész, ha:

- [ ] Konkrét eredeti étel-, mérgezési-, pihenési- és kimerülési esetek összehasonlítható eredményt adnak.
- [ ] Az élet elfogyása után nem folytatható korlátlanul az aktív játék.
- [ ] Sikertelen fogyasztás nem veszít tárgyat; a játék vége és mentés együtt is ellenőrzött.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

<a id="n05"></a>

### N05 · Építés, műhelyek, ruházat és tábori tárolás teljes működése

**P1 · [#9](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9) · Kiinduló állapot: részben kész.**

A 45 recept költség/eszköz/helyfeltétele külön ellenőrzött. Az építményelhelyezés szabad szomszédos cellát választ, több elkészült tárgy használati hatása, a ruhák felvétele és a tábori készletkezelés hiányos.

Hátralévő munka:

- Az eredeti építési helyek, táborfejlesztések, műhelyek és kész építmények tényleges használatát átültetni.
- Ruházat felvétele/levétele, felszerelési helyek és a bizonyított védelmi/túlélési hatások.
- Ház/tábor tároló, be- és kipakolás, kapacitás/súly vagy más korlátok az eredeti szabályai szerint.

Akkor kész, ha:

- [ ] Minden recept eredménye a megfelelő helyen használható; a puszta létrehozás nem számít teljes működésnek.
- [ ] A viselt ruha külön látszik a tarisznyában lévő ruhától, hatásai és mentése ellenőrzöttek.
- [ ] Tárolás, készítés, felvétel/levétel nem dupláz és nem tüntet el készletet.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N04](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

<a id="n06"></a>

### N06 · Állatok szigetbeli mozgása, veszélyei és harca

**P1 · [#12](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/12) · Kiinduló állapot: hiányzik / részben alapozott.**

Az állatok a világban tárgycellák. Van egyállatos vadászati minijáték és néhány sikertelenségi sérülés, de nincs teljes szigetbeli állat-AI vagy ruházatfüggő védelem.

Hátralévő munka:

- Átültetni a bizonyított mozgás, élőhely, észlelés, menekülés/üldözés és támadás viselkedéseit.
- A veszélyes állatok sérülését, a ruházati védelmet és a túlélési/halálállapotot összekötni.
- Az eredeti több közeli állatot kezelő vadászatot összehangolni a világban lévő egyedekkel.

Akkor kész, ha:

- [ ] Konkrét állatfajok eredetihez viszonyított viselkedése dokumentált és ellenőrizhető.
- [ ] Állat nem megy érvénytelen terepre és nem ad többször zsákmányt ugyanazért az egyedért.
- [ ] Félbeszakítás, vadászat és mentés után is konzisztens a világban élő/eltávolított egyedek állapota.

Előfeltételek: [N02](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/7), [N04](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8), [N05](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9), [N07](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/10), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

<a id="n07"></a>

### N07 · Minijátékok pontosítása az eredeti fizikához és részletszabályokhoz

**P1 · [#10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/10) · Kiinduló állapot: játszható alap kész; egyezés részleges.**

A 0.6.0 érintőkaros vadászatot/horgászatot, hálózást és két találatos kókuszdobálást tartalmaz. Az eredetivel nem egyező részletek a planning/MINIGAMES.md fájlban szerepelnek.

Hátralévő munka:

- Az eredeti lövedék-/horogmozgás, célméretek, időzítés és erőnléti/morálhatások összehasonlítása és szükséges átültetése.
- A bot 45 másodperces pótszabályának, a háló halhullámainak, a kókuszcélpontok elrendezésének és a belső pálmaváltozat-feltételnek a rendezése.
- Kő újrahasználati/kopási számláló, költségelszámolás, eredmény utáni folyamat és többállatos vadászat; az eredeti furcsa jutalomazonosítók bizonyított ellenőrzése.
- A kért folyamatos, kétujjas érintőkaros vezérlés megőrzése, az esetleges tudatos modernizálási eltérések dokumentálásával.

Akkor kész, ha:

- [ ] Minden felsorolt eltérés igazoltan javítva vagy konkrétan dokumentált, tudatos eltérésként kezelve.
- [ ] Az eszköz/csali/kő/nyíl és jutalom sikernél, hibánál, megszakításnál és visszatöltésnél következetes.
- [ ] Érintőkar + akciógomb együtt használható, elengedésből/megszakításból nem lesz véletlen lövés vagy jutalom.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N04](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

<a id="n08"></a>

### N08 · Grafikai hozzárendelések, méretek és animációk véglegesítése

**P1 · [#13](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/13) · Kiinduló állapot: részben kész.**

Nyolc generált grafikai bemenet, közös ItemArt, külön kagyló/gyümölcs/szerszámikonok és négyirányú járás működik. Néhány állat/dekoráció/építmény még közös vagy eltérő stílusú képet használ.

Hátralévő munka:

- A teljes 120 tárgybejegyzés és az összes használt dekoráció/terepelem képét, méretét és horgonyzását végigellenőrizni az eredeti alapján; a helykitöltőket és téves társításokat cserélni.
- A karakter-, eszközhasználati-, állat-, építési- és harci animációkat a játékmenettel összekötni; atlaszáthallás, átlátszósági szélek és takarás javítása.
- A natív tarisznya/készítés/napló és a HUD egységes megjelenése, olvashatósága különböző telefonarányokon.

Akkor kész, ha:

- [ ] A használható tárgyak és fontos állatfajok felismerhetők; nincs félrevezető általános kókusz/láda/papagáj-helyettesítés.
- [ ] A kezdőpart, sűrű növényzet, vízpart és minijátékok képi ellenőrzése dokumentált több képarányon.
- [ ] Az ikonok a pályán, tarisznyában és recepteknél ugyanazt a tárgyat jelölik; az animáció nem takarja el a szükséges interakciót.

Előfeltételek: [N03](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/11), [N05](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9), [N06](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/12), [N07](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/10)

<a id="n09"></a>

### N09 · Teljes magyarítás, napló és helyzetfüggő játékosi visszajelzés

**P1 · [#14](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/14) · Kiinduló állapot: részben kész.**

A fő felületek és tárgynevek magyarok. Több eredeti tárgyleírás és bevezető szöveg angol; a napló elsősorban a bevezető eseményeket mutatja.

Hátralévő munka:

- A felhasznált eredeti leírások, bevezetők, történeti szövegek, állapotok és hibajelzések teljes magyarítása, egységes tárgy-/eszköznevekkel.
- A naplóban az igazolt előrehaladást és a jelenleg szükséges feltételeket következetesen megjeleníteni, kitalált küldetések hozzáadása nélkül.
- Hiányzó eszköz/hely/csali, árapály, veszély, pihenés és befejezés érthető visszajelzéseinek ellenőrzése.

Akkor kész, ha:

- [ ] A szokásos játékútvonalon nincs véletlen angol szöveg vagy félrevezető hiányjelzés.
- [ ] A hosszú magyar szövegek, mennyiségek és ékezetek a támogatott képarányokon olvashatók, nem lógnak ki.
- [ ] A napló és visszajelzés a mentett állapotból helyesen áll helyre.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N02](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/7), [N03](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/11), [N04](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8), [N05](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9), [N06](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/12), [N07](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/10)

<a id="n10"></a>

### N10 · Mentések bővítése és migráció az új világállapotokhoz

**P0 · [#6](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6) · Kiinduló állapot: alap kész; az új rendszerekhez bővítendő.**

CRC-vel védett atomikus mentés működik. A 0.3.1/0.5.0 motorral készült helyi mentések betöltése tesztelt. A formátum még nem tárol teljes történeti, árapály-, ruházat-, tároló- és állatállapotot.

Hátralévő munka:

- Verziózott bővítési/migrációs tervet készíteni a következő állapotok bevezetése előtt.
- Megőrizni a natív alpha mentéseinek betölthetőségét és a csomagnév/aláíró folytonosságát; az új mezők alapértékeit tudatosan megadni.
- Sérült/megszakított mentés, folyamatkilövés, háttérbe helyezés, befejezett/függő jutalom és frissítés közbeni állapotkezelés rendezése.

Akkor kész, ha:

- [ ] Rögzített korábbi mentésekkel automatikus visszafelé kompatibilitási vizsgálat fut, ha a helyi tesztbemenetek rendelkezésre állnak.
- [ ] Mentés/visszatöltés minden új rendszer releváns állapotát megőrzi; nincs részleges alkalmazás vagy készlet/jutalomduplázás.
- [ ] Állapot- és sémafrissítéshez leírt migráció tartozik, a privát aláírókulcs nem kerül a repóba.

Előfeltételek: nincs másik részfeladat előfeltételként rögzítve.

<a id="n12"></a>

### N12 · A gyűjthető tárgyak eredeti újratermelődése; kőhiány miatti softlock

**P0 · [#16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16) · Kiinduló állapot: hiányzik, a felhasználó jelenlegi mentésében már elfogyott az összes kő.**

**Felhasználói megfigyelés:** az eredeti Java ME-játékban a pályán lévő erőforrások idővel újra megjelentek. A 0.6.0 natív motorban az `interact()` egyszerűen `objects[index]=0` állapotra állítja a felszedett tárgyat, az `update()` csak a napot/órát lépteti, nem regenerál, a mentés pedig a kimerített `objects[]` tömböt tartósítja. A kő (ID **3**) fogyóeszköz a kókuszminijátékhoz is, ezért az elfogyása blokkolhatja a játékmenetet.

Hátralévő munka:

- Visszafejteni vagy eredeti játéktesztben megfigyelni a **valós respawn-szabályokat**: mely tárgyak, milyen időközzel, mely eredeti pozíciókon és milyen feltételekkel kerülnek vissza. A pontos időzítés **egyelőre nem ismert**; tilos kitalált fix „naponta visszanő minden” szabályt eredetinek nevezni.
- A természetes gyűjthető objektumokat és a történeti/egyedi/épített tárgyakat külön kezelni. Utóbbiak ne duplikálódjanak; az eredeti kőlelőhelyek pótlása legyen elsődleges.
- **Már beragadt 0 köves mentésnél** biztosítani, hogy a játék folytatható legyen a meglévő készlet, pozíció és előrehaladás elvesztése nélkül. A mentési séma és az esetleges migráció az [N10](#n10) feladattal közösen készül.
- Az események időzítését és szükséges számlálóit menteni, visszatöltéskor idempotensen alkalmazni; ne legyen dupla spawn és mentés–visszatöltés alapú farmolás.
- Motor- és telefonos regressziós tesztek: minden kő elfogyasztása → eredeti feltétel kivárása → újabb kő felszedhető; mentés–visszatöltés, régi mentés, épített/történeti objektumok sértetlensége, crafting/minijátékok.

Akkor kész, ha:

- [ ] A respawn feltételei eredeti forrás alapján dokumentáltak; a meg nem erősített részletek külön szerepelnek.
- [ ] A gyűjthető kő (ID 3) elfogyása után a világ eredeti szabály szerint pótolja az erőforrást.
- [ ] A jelenlegi kőhiányos 0.6.0 mentés új játék indítása nélkül, adatvesztésmentesen folytatható.
- [ ] Nincs történeti tárgy-/építmény-duplázás, végtelen erőforrás és save/load után duplán alkalmazott utánpótlás.
- [ ] Automatizált motorteszt és valódi telefonos regressziós próba zöld.

Előfeltételek: [N10](#n10) mentés/migráció, az [N02](#n02) eredeti idő- és világműködésének feltárandó szabályai. A hibát az [N11](#n11) teljes végigjátszási ellenőrzése is blokkolóként kezeli.

<a id="n11"></a>

### N11 · Telefonos végigjátszás, teljesítménymérés és kiadási ellenőrzés

**P1 · [#15](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/15) · Kiinduló állapot: hiányzik; helyi motor/UI tesztek vannak.**

A 0.6.0 helyben 13 Android/Skia teszten és 307 motorfeltételen ment át, két helyi régi mentési bemenettel. Ez nem teljes végigjátszás és nem fizikai telefonos FPS-/RAM-mérés.

Hátralévő munka:

- A kötelező eredeti útvonalat telepített APK-n, csalás és előkészített készlet nélkül végigjátszani; a blokkolókat visszakötni a megfelelő feladathoz.
- Valós Android 8+ eszközökön, eltérő képaránnyal és érintési/navigációs beállításokkal ellenőrizni az irányítást, olvashatóságot és életciklust.
- Mért FPS/képkockaidő, memória, hosszabb játék, szünet/visszatérés, frissítés és mentésvesztés ellenőrzése; az Android 8 szoftveres rajzolási visszaesés külön vizsgálata.
- GitHub-tesztkiadások, ellenőrzőösszeg, telepíthetőség és reprodukálható helyi build dokumentálása; stabil kiadás csak a bizonyított állapotot állítsa.

Akkor kész, ha:

- [ ] Legalább egy dokumentált teljes végigjátszás eljut a kezdőparttól az eredeti befejezésig, nyitott blokkoló nélkül.
- [ ] A tesztelt telefonok/Android-verziók és tényleges mérési eredmények szerepelnek a jegyzőkönyvben.
- [ ] A frissítés megőrzi a mentést, a kiadott APK visszatöltése/aláírása/ellenőrzőösszege ellenőrzött.

Előfeltételek: [N01](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/5), [N02](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/7), [N03](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/11), [N04](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/8), [N05](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/9), [N06](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/12), [N07](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/10), [N08](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/13), [N09](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/14), [N10](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/6)

## Kapcsolódó dokumentumok

- [P0 erőforrás-újratermelődés és kőhiány #16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16)
- [Minijátékok részletes eltérései](MINIGAMES.md)
- [Tárgygrafikák és ismert korlátok](ITEM_GRAPHICS.md)
- [Irányítás képekkel](../docs/CONTROLS.md)
- [Forrás, ellenőrzések és helyi fordítás](../README.md)

Új igazolt eltérésnél a megfelelő issue és dokumentáció is frissítendő. Egy sikeres build, adatkinyerés vagy egyedi receptteszt önmagában nem zár le teljes játékrendszert.
