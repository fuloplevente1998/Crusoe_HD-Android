# Natív minijátékok – 0.4.0

Saját Java-szimuláció és Android Canvas-felület, eredeti végrehajtható kód nélkül. A szabályok ellenőrzése a felhasználó helyi eredeti példányán történt. A nyilvános forrás nem tartalmazza ezt a példányt vagy a visszafejtett kódját.

## Az eredetiben ellenőrzött és most használt szabályok

- Horgászbot: 14-es eszköz, 90-es giliszta, szomszédos 5-ös terepkód. A lehetséges fogások 40, 56, 57, 58. A horog megakasztása után a halat a felszínre kell húzni.
- Háló: 83-as eszköz, 26-os burgonya, szomszédos 4-es terepkód. Legfeljebb öt merítés, legalább tíz elfogott hal. A siker egy tárgyat ad, nem tíz darabot. Az eredeti véletlen jutalomlista meglepő módon 59, 60, 2; a 60-as bejegyzés szőnyeg. Ezt egyelőre megtartjuk, nem értelmezzük át adatjavításnak.
- Íj: 81-es eszköz, 45-ös nyílbejegyzés. Egy eredeti készletegység tízlövéses minijátékot jelent; a natív felület ezért nyílcsomagként magyarázza. Csak lefelé tartó lövedék találata számít. Siker esetén az állat kikerül a világból, egy példány a tarisznyába kerül. A célpontokhoz az eredeti tárgyadat harmadik oszlopa szükséges: 81.
- Veszélyes vadászat sikertelenségekor a védelem nélküli sérülés: alligátor/jaguár 50, zöld kígyó 10, boa 20 életpont.

## Nyílt eltérések és hiányok

- Az érintéses irányítás, fizika, célméretek és sebességek új megvalósítások; viselkedési azonosságuk nincs igazolva. A bot jelenleg 45 másodperces korláttal működik, nem az eredeti teljes menekülési és horogmozgási modellel.
- A háló egyszerűsített, hét halból álló hullámokat használ. Az eredeti több halfajt és más mozgást használ.
- Egyszerre egy állat vadászható. A környező állatok közös vadászata, menekülési/üldözési viselkedése és a ruházat védelme még hiányzik.
- A minijáték saját futása alatt a világ ideje és túlélési értékei szünetelnek. Az eredeti összes erőnléti és morálhatása még nincs átültetve.
- A csali/nyílcsomag az indításkor fogy el, az eredetiben az eredmény lezárásakor. Ez megszakításkor is érvényes. Így háttérbe küldéssel vagy folyamatújraindítással nem jár vissza az elköltött csali.
- Egy folyamaton belüli háttérbe helyezés szünetelteti a játékot. A minijáték állapotát nem mentjük: folyamatkilövés után a szigetre tér vissza a játék, az elhasznált készlettel. A befejezett és lezárt jutalom mentődik.
- Az új állatgrafika ideiglenes atlasz, több faj még közös képet kap. Ez nem a teljes grafikai port.

## Ellenőrzés

A motorvizsgálat ellenőrzi a hely- és eszközfeltételeket, a csali egyszeri levonását, a duplázás kizárását, a horog és a háló fogását, a ballisztikus találatot, a tízlövéses korlátot és az állat eltávolítását. Az Android/Skia-tesztek valódi érintéses eseményekkel nyitják meg a műveletmenüt, horgásznak, húzzák ki a halat, lépnek vissza, állítják az íjat és indítják a merítést/lövést.

A helyi ellenőrzés egy ténylegesen a változatlan 0.3.1-es motorral készített mentést is betölt. A bináris mentés formátuma és fájlneve változatlan; ugyanaz a csomagnév és aláírás maradt az APK frissítéséhez.
