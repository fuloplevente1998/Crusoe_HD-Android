# Natív minijátékok – 0.6.0

Saját Java-szimuláció és Android Canvas-felület, eredeti végrehajtható kód nélkül. Az eredeti szabályokat a felhasználó helyi példányán ellenőriztük; a nyilvános forrás nem tartalmazza a kereskedelmi JAR/JAD-ot vagy a visszafejtett kódját.

## Irányítás

A vadászat, kókuszdobálás és horgászat közös, folyamatos érintőkart használ. Az oldalirány a szöget, a fel/le irány az erőt változtatja a ballisztikus játékokban; horgászatkor két tengelyen a horgot mozgatja. A külön akciógomb másik ujjal is megnyomható. Elengedéskor a kar középállásba tér, de a beállított szög és erő megmarad. A kar elengedése nem lő és nem akasztja meg magától a horgot.

Az érintésmegszakítás, menüváltás és háttérbe küldés elengedi az aktív vezérlést. Visszatéréskor új érintés kell a folytatáshoz. A még repülő lövedék iránya utólag nem változtatható. A szög 10–85°, az erő 40–100%; a kezdőérték 70° és 40%. A rajzolt célzási tartomány és a rövid röppályajelző követi az értékeket. A fizika és az érzékenység új megvalósítás, nem igazoltan azonos az eredetivel.

## Az eredetiben ellenőrzött, használt szabályok

- Horgászbot: 14-es eszköz, 90-es giliszta, szomszédos 5-ös terepkód. A lehetséges fogások 40, 56, 57, 58. A horog megakasztása után a halat a felszínre kell húzni. Az érintőkar mellett a közvetlen horoghúzás is használható.
- Háló: 83-as eszköz, 26-os burgonya, szomszédos 4-es terepkód. Legfeljebb öt merítés, legalább tíz elfogott hal. A siker egy tárgyat ad, nem tíz darabot. Az eredeti véletlen jutalomlista 59, 60, 2; a 60-as bejegyzés szőnyeg. Ezt egyelőre megtartjuk.
- Íj: 81-es eszköz, 45-ös nyílbejegyzés. Egy készletegység tízlövéses minijátékot jelent, ezért a felület nyílcsomagként magyarázza. Csak a lefelé tartó lövedék találata számít. Siker esetén az állat kikerül a világból, egy példány a tarisznyába kerül. A célpont harmadik tárgyadatoszlopának értéke 81.
- Kókusz: a világ 18-as dekorációs kódú pálmatípusa használható kővel (3). Egy kókuszt két külön dobás találata mozdít le. Az első találat 11 másodpercig érvényes, majd a kókusz visszaáll. Legalább öt kókuszt kell leverni, legfeljebb tíz dobásból. Egy dobás több külön kókuszt is érinthet. A leesés megjelenik, a sikeres kör jutalma egy 1-es kókuszbejegyzés.
- Veszélyes vadászat sikertelenségekor a védelem nélküli sérülés: alligátor/jaguár 50, zöld kígyó 10, boa 20 életpont.

## Nyílt eltérések és hiányok

- A lövedékfizika, célméretek, elrendezés és sebességek új megvalósítások. Az eredeti célzás minden lövés utáni visszaállítása helyett itt megmarad a beállítás. A bot 45 másodperces korláttal működik, az eredeti teljes menekülési/horogmozgási modell nincs átültetve.
- A kókuszjáték hat célpontot használ. Az eredeti öt–hét kókuszt választott változó pálmaelrendezésből. A natív változat minden közeli 18-as dekorációs pálmánál indítható; az eredetiben a pálma véletlen belső képkockája (8) is feltétel volt. Ez a belső dekorációállapot még nincs átültetve.
- Most egy kő fogy egy teljes kókuszos kör indításakor. Az eredeti kő-újrahasználati/kopási számlálója és az eredmény utáni automatikus következő minijáték-indítás nincs átültetve. A pálma nem tűnik el, a kör újraindítható új kővel.
- A háló hét halból álló egyszerűsített hullámokat használ. Egyszerre egy állat vadászható; a környező állatok közös vadászata, menekülése/üldözése és a ruházat védelme még hiányzik.
- A minijáték közben a világ ideje és túlélési értékei szünetelnek. Az eredeti összes erőnléti és morálhatása még nincs átültetve.
- A csali, kő vagy nyílcsomag indításkor fogy el. Megszakítás vagy folyamatújraindítás sem adja vissza. Folyamaton belüli háttérbe helyezés szüneteltet; folyamatkilövés után a szigetre tér vissza a játék az elhasznált készlettel. A lezárt jutalom mentődik.
- A grafika és az animáció fejlesztés alatt áll; több faj még közös képet használ.

## Ellenőrzés

A tiszta Java-motorvizsgálat valódi lövedékröptetésekkel éri el az öt kókuszos célt: az egy és két találat különbsége, 11 másodperces visszaállás, tízdobásos korlát, egyszeri költség és jutalom is ellenőrzött. A joystick két tengelye, elengedett és hibás bemenet, határok és halászati horogmozgás is vizsgált.

Az Android/Skia-tesztek valódi, külön ujjazonosítókkal küldött kétujjas eseményekkel tartják a kart és adják le a lövést; elengedés, ACTION_CANCEL és háttérből visszatérés után is ellenőrzik a viselkedést. A horgászati érintőkar és külön megakasztásgomb teljes fogási, felhúzási és mentési folyamata is lefut.

A helyi ellenőrzés a változatlan 0.3.1-es és a GitHubon rögzített 0.5.0-s motorral készített mentéseket is betölti. Ezek helyi, eredeti világadatokat tartalmazó tesztbemenetek. A mentési formátum/fájlnév, az Android csomagnév és az aláíró változatlan.
