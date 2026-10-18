# Tárgyazonosság és ikonok – 0.5.0

## Javított probléma

A korábbi rajzoló több tucat különböző tárgyhoz ugyanazt a kókuszképet választotta. A szerszámok többsége egy általános láda képét kapta. Ezért a 76-os kés is ládának látszott; a mellette álló eredeti dekorációs láda pedig bokornak.

Az ItemArt egy helyen választja ki a tárgy képét. Ezt használja a pálya, a natív tarisznya, a receptkártya és a hozzávalólista. A kagylók, tengeri csillag, sün, tojás, gyümölcsök, gombák és alapvető szerszámok külön rajzot kaptak, három új, összesen 48 illusztrációból álló atlaszból. A ruhákhoz és néhány készíthető felszereléshez saját Canvas-ikonok tartoznak. A teljes tárgykatalógus nem osztozik egyetlen gyümölcs- vagy ládaképen.

## Kezdőpart ellenőrzése

A helyi eredeti új játékot indító és mentést alaphelyzetbe állító rutinja egyaránt az (8,148) egyalapú koordinátát használja. Ez a natív rácson (7,147). Az új játék most innen indul; a korábbi natív mentések pozícióját nem módosítjuk.

A kés az eredeti Objects-réteg (11,165) cellájában található, a 68-as dekorációjú láda pedig a (10,165) cellában. A helyi, eredeti dekorációs sprite-hozzárendelésben a 68/69-es kód láda, nem bokor. A helyeket és a tárgyréteget nem írjuk át. A kés külön ikonja és felirata látható a közelben. A napló opcionális „Út a késhez” gombja az eredeti helyhez keres utat, és a kés felvétele után eltűnik.

## Natív menük

A felületek továbbra is Android View/ScrollView/AlertDialog elemek, WebView nélkül. A tarisznya és a készítés kétoszlopos képes kártyákat mutat. A recepteknél jelölés és szűrő mutatja, mi készíthető el. A részleteknél külön sorban, ikonnal és meglévő/szükséges mennyiséggel szerepelnek a fogyó alapanyagok, megmaradó eszközök és helyfeltételek.

## Korlátok

A grafika továbbra is fejlesztés alatt áll. Néhány állatfaj, építmény és növény közös rajzot használ; a dekorációk teljes eredeti alak- és mérethelyessége nincs igazolva. Az atlaszok és a Canvas-ikonok stílusa sem teljesen egységes. A napló eredeti bevezető szövegeinek egy része még angol. A térképrétegek pontossága nem jelenti a teljes történeti játékmenet kész állapotát.

A kereskedelmi eredeti képei és visszafejtett forrása nem kerülnek a nyilvános projektbe. A generált atlaszok a projekt `app/src/main/assets/art/` mappájában nyilvánosan elérhetők.
