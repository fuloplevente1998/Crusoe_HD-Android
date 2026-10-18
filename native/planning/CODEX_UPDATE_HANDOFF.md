# Natív frissítési átadás – 0.6.2 (N12 és hordozható aláírás)

A korábbi, csak Codexre és 8-as verziókódra vonatkozó buildleírást a [közös helyi/GitHub útmutató](../docs/ANDROID_UPDATES.md) váltja fel a felhasználó új kérése alapján.

- Alkalmazásazonosító: `hu.crusoe.standalone`, a korábbi privát keystore és tanúsítvány változatlan.
- Verziónév: `0.6.2-native-alpha`; verziókód: UTC-másodperc 2020-01-01 óta. Nincs `.n12test` csomagra átírás.
- Egyetlen helyi parancs: `bash scripts/build_update.sh`. A régi `build_signed_update_codex.sh` ugyanide irányít.
- GitHubon egyszer be kell állítani a `CRUSOE_SIGNING_KEYSTORE_B64` Actions-titkot a meglévő kulccsal. A jelenlegi integráció ezt 403 Secrets API hibával megtagadta; a repó tulajdonosa állíthatja be.
- N12: mind a **36 természetes erőforrás / 1 024 eredeti lelőhely** megújul 04:00-kor, ha a mező szabad. Az egyszeri és épített tárgyak védettek; a kőhiányos régi mentés mentése változatlan.
- Helyben **61/61 erőforrás-feltétel, 323 motorfeltétel és 13/13 Android/Skia teszt** sikeres. Két régi helyi mentési bemenet nélkül a motorfeltételek száma 321.
- Az APK tényleges csomagját, aláírását, világadatát és növekvő verzióját a `verify_update_apk.py` ellenőrzi. Hiányzó vagy rossz kulccsal nem készül frissítő APK.
- A telefonon telepített fő appot **ne távolítsuk el**. Az alpha APK a meglévő alkalmazás frissítésére készült. Az eredeti kőhiányos mentés telefonos próbája, kilépés/visszatöltés és több napi 04:00 ellenőrzése még hátra van.

A [PR #17](https://github.com/fuloplevente1998/Crusoe_HD-Android/pull/17) és [N12 / #16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16) a fizikai próba beérkezéséig nyitva marad; végleges kiadás nincs. A teljes játék történeti portja továbbra is fejlesztés alatt áll.
