# Robinson Crusoe 0.6.2 – natív Android-frissítő

Saját motoros Android **0.6.2 alpha frissítő**, Android 8.0+.

**[APK letöltése – 19,2 MB](https://raw.githubusercontent.com/fuloplevente1998/Crusoe_HD-Android/apk-download-native-0.6.2/Crusoe-Native-0.6.2-Update.apk)**

- **Mind a 36 megújuló természetes erőforrás** visszatér naponta **04:00-kor**, az **1 024 eredeti lelőhelyen**, ha az adott mező szabad. Kő, ág, lián, gyümölcsök, gombák, kagylók, giliszták és más természetes anyagok. Az egyszeri felszereléseket, építményeket és foglalt mezőket megőrzi.
- A kőhiány miatt elakadt régi mentés betöltésekor egy biztonságosan elérhető eredeti kőlelőhely helyreállhat, készlet- és előrehaladás-reset nélkül.
- A korábbi saját motoros app **`hu.crusoe.standalone`** azonosítója és aláírója változatlan; nagyobb verziókód. **Frissítésként telepítsd, a meglévő appot ne távolítsd el**, hogy a mentés megmaradjon.
- A helyi és GitHub build közös aláírás-ellenőrzést és UTC-alapú verziókódot használ. A GitHubon egyszer be kell állítani a megőrzött kulcsot a **`CRUSOE_SIGNING_KEYSTORE_B64`** Actions-titokban. A Codex-integráció Secrets API jogosultsága 403 hibával tiltott, ezért ezt a tulajdonos végzi el. [Részletes útmutató](https://github.com/fuloplevente1998/Crusoe_HD-Android/blob/fix/native-update-signing/native/docs/ANDROID_UPDATES.md).

**Ellenőrizve helyben:** 61 erőforrás-feltétel, 323 motorfeltétel (két régi helyi mentéssel), 13/13 Android/Skia teszt. Debug és release build sikeres; tényleges APK-csomag, tanúsítvány és nagyobb verziókód összevetve a 0.6.0-val. A frissítő build hiányzó/rossz kulccsal vagy túl alacsony kóddal megáll. Saját natív motor, Java ME-futtatás és WebView nélkül.

**Még nincs fizikai telefonos rátelepítési és kőhiányos mentéspróba.** A teljes eredeti játék történeti portja továbbra is alpha. [N12 / #16](https://github.com/fuloplevente1998/Crusoe_HD-Android/issues/16), [PR #17](https://github.com/fuloplevente1998/Crusoe_HD-Android/pull/17) és [frissítő build PR #18](https://github.com/fuloplevente1998/Crusoe_HD-Android/pull/18) nyitva marad. A GitHubon aláírt build a titok beállításáig nem fut végig.

Forráscommit: `eee0d9e5a1cee905c9ad072e93f8f6fbb9fcab84`

Verziókód: `214509923`

SHA-256: `a877755912f7fd530244ada8aecdb8a0d14eb42c755037f8f73d3284fc66a6bc`

Az APK külön letöltési ágon van. A privát aláírókulcs és az eredeti JAR/JAD nincs közzétéve.
