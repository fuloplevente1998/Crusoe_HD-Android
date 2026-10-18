# Crusoe – saját motoros Android-fejlesztés

**0.6.2-es alpha:** saját Android/Canvas-motor, natív felületek. Az eredeti öt térképréteg, 120 tárgy és 45 recept mellett mozgás, mentés, halászat, vadászat és kókuszdobálás működik. A teljes történeti port még fejlesztés alatt áll.

Új: folyamatos érintőkaros célzás és horogmozgatás, külön akciógombbal és kétujjas használattal. Az oldalirány a szöget, a fel/le irány az erőt változtatja; az elengedett kar megtartja a beállítást. A kókuszos játékban két találat kell egy kókuszhoz, tíz dobásból legalább ötöt kell leverni. [Irányítás képekkel](native/docs/CONTROLS.md).

Helyben ellenőrizve: 13 Android/Skia felületi teszt és 323 motorfeltétel és 61 erőforrás-feltétel, a 0.3.1-es és 0.5.0-s motorral készült helyi mentések visszatöltésével. A két régi mentési fájl helyi tesztbemenet. Az öt pályaréteg változatlan, az APK aláírója megegyezik a korábbi natív alpha aláírójával. Fizikai telefonos teljesítménymérés még nincs.

Új: **36 természetes erőforrástípus 1 024 eredeti lelőhelye naponta 04:00-kor megújul**, ha a mező szabad. A kőhiányos régi mentés biztonságos helyreállítása is része a frissítésnek. A helyi és GitHub buildek csomagneve, tanúsítványa és verziózása közös: [Frissítő APK készítése és egyszeri Actions-titok](native/docs/ANDROID_UPDATES.md). A telefonos mentéspróba még hátra van.

## Letöltés és projektanyagok

- [0.6.2 natív Android-frissítő tesztkiadás](https://github.com/fuloplevente1998/Crusoe_HD-Android/releases/tag/native-v0.6.2-alpha)
- [Natív forrás és helyi fordítás](native/README.md)
- [Generált grafikák](native/app/src/main/assets/art) és [előnézetek](native/docs/ARTWORK.md)
- [Minijátékok eredetihez viszonyított eltérései](native/planning/MINIGAMES.md)

## Hátralévő fejlesztések

[Részletes ROADMAP](native/planning/ROADMAP.md) · [GitHub-összesítő #4](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/4). Tizenkét részfeladat P0/P1 prioritással, jelenlegi állapottal, függőségekkel és ellenőrizhető befejezési feltételekkel.

## Development rules

1. Preserve original gameplay rules when porting; explicitly document behavior that is still simplified or absent.
2. Keep original game assets, extracted resources, and ROM/JAR files out of the public repository and CI artifacts.
3. Separate the original 180×180 tile map and game coordinates from physical screen pixel density.
4. Keep changes on feature branches, review them in draft PRs and test on Android before release.
5. A passing build or synthetic rendering test does not establish full game compatibility or measured phone performance.

The source branch excludes the original commercial JAR/JAD, directly extracted raw resources, local world-data package and signing keys. Generated project art and native UI previews are included. The native alpha APK is published separately on a download branch and linked from the prerelease. The Java ME game is not executed by this implementation.
