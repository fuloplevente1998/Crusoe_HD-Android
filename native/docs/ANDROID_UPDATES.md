# Frissíthető Android APK helyben és GitHubon

A saját motoros alkalmazás azonosítója mindig **`hu.crusoe.standalone`**. A debug és release build is a korábbi natív APK **ugyanazon privát kulcsával** készül. A `.n12test` átírás megszűnt ezen az ágon. A fordítás kulcshiány vagy eltérő tanúsítvány esetén hibával megáll; véletlenszerű pótkulcs nem készül.

Várt aláíró tanúsítvány SHA-256:

```text
ce51e3426040b1497c65fd230f875e602d4766143ba3f505475e0996599b40c1
```

Ez nyilvános tanúsítvány-ujjlenyomat; a privát keystore nincs a repóban. A korábbi 0.6.0 verziókódja 7. A csomag, az aláíró és a nagyobb verziókód együtt teszi lehetővé a rátelepítést. **A meglévő játékot ne távolítsd el:** az eltávolítás törli a helyi mentést. A külön telepített `n12test` app mentése külön adat, nem kerül automatikusan át a fő appba.

## Egyszeri GitHub-beállítás

1. A meglévő natív aláírókulcs privát biztonsági másolatából nyisd meg a `CRUSOE_SIGNING_KEYSTORE_B64.txt` fájlt. Ez a keystore base64 alakja; ugyanúgy titokként kezeld, mint a kulcsot.
2. A repóban: **Settings → Secrets and variables → Actions → New repository secret**.
3. Név: **`CRUSOE_SIGNING_KEYSTORE_B64`**. Érték: a fenti szövegfájl teljes tartalma. Add secret.
4. A kulcsot, a base64 szöveget és a mentő ZIP-et ne commitold, ne csatold issue-hoz vagy nyilvános release-hez. A workflow csak az APK-t, ellenőrzőösszegét és nyilvános ellenőrzési jelentését teszi artifactba.

Ha a saját gépeden van a régi keystore, JDK 21 és megfelelő jogosultságú `gh` bejelentkezés, a 2–3. lépés helyett:

```bash
cd native
# A korábbi kulcs: private/signing/native-debug.keystore
bash scripts/configure_github_signing.sh
```

A jelenlegi Codex GitHub-integráció a Secrets API-t `403 Resource not accessible by integration` hibával utasította el; innen a titok nem volt beállítható. Nincs szükség token vagy kulcs chatbe másolására.

## GitHub-build

A változtatások jelenleg a **`feat/native-survival-v01`** ágon vannak; a `main` még a korábbi emulátoros alap. Az Actionsben az **android-debug.yml** munkafolyamatot válaszd, majd **Run workflow → Branch: feat/native-survival-v01 → Run workflow**. A listában a workflow régi neve még „Android debug baseline” lehet, amíg az alapértelmezett ág nem frissül. Az itt megadott ág kódja már a natív frissítőt építi.

A sikeres futás végén az **Artifacts → crusoe-native-update-…** ZIP-ben van a **`Crusoe-Native-Update.apk`**, a `SHA256SUMS` és a `verification.json`. Ehhez GitHub-bejelentkezés kell. Csomagold ki, és az APK-t a meglévő app frissítéseként telepítsd. A hiányzó titokkal futtatott build a tesztek után világos hibával áll meg, telepíthetetlen vagy külön csomagú APK-t nem publikál.

Az N12 workflow is ugyanezt a közös natív buildet használja. A kulcs csak megbízható ágról indított push vagy kézi futásban használható; nincs `pull_request_target` trigger, forkból érkező PR nem kapja meg. A repóba írási joggal rendelkező személyek és módosított workflow-k hozzáférhetnek az Actions-titokhoz, ezért a branch-védelmeket ehhez igazítsd.

## Helyi build

JDK 21, Android SDK 36 / build-tools 36.0.0 szükséges. A generált grafikák a repóban vannak. A `restore_world.py` a korábbi nyilvános natív APK rögzített commitjáról tölti vissza az adatcsomagot, az APK és a világ SHA-256 ellenőrzésével. Az eredeti végrehajtható Java ME osztályokat nem használja.

```bash
cd native
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
# A privát mentésből: private/signing/native-debug.keystore
bash scripts/build_update.sh
```

Eredmény: `artifacts/update/Crusoe-Native-Update.apk`. A régi `build_android.sh` és `build_signed_update_codex.sh` is ezt a közös parancsot hívja. Az alap kulcs aliasa és jelszavai a korábbi Android debug beállítások (`androiddebugkey`, `android`); a biztonságot a privát kulcs megőrzése adja. Más helyhez használható a `CRUSOE_SIGNING_STORE_FILE` környezeti változó. A tanúsítvány ujjlenyomatát új gépen sem szabad másikra átírni.

## Verziókód

Minden build a **2020-01-01 00:00:00 UTC óta eltelt másodpercek** számát kapja verziókódként. Ez helyben és Actionsben azonos szabály, a munkafolyamat futásszámától független. A később elkészült build nagyobb kódú, ha a gépek órája pontos; a másodpercen belüli buildek kódja azonos lehet. Egy korábban készült artifact újraletöltése nem lesz újabb verzió.

A verziónév most `0.7.0-native-alpha`. Kivételesen explicit kód is megadható `-PcrusoeVersionCode=...` Gradle-paraméterrel vagy `CRUSOE_VERSION_CODE` változóval, de ilyenkor neked kell biztosítani, hogy nagyobb legyen a telefonra telepítettnél és kisebb 2 100 000 001-nél. Ne adj önkényesen nagy kódot, mert az későbbi frissítést akadályozhat. A Gradle minimum 8-at fogad el.

Az elkészült APK független ellenőrzése egy korábbi APK-val:

```bash
python3 scripts/verify_update_apk.py artifacts/update/Crusoe-Native-Update.apk \
  --previous /private/Crusoe-Native-0.6.0.apk
```

Ez a tényleges APK aláírását, csomagnevét, adatcsomagját és verzióját ellenőrzi. A telefonos telepítés és a korábbi kőhiányos mentés folytatása még külön fizikai ellenőrzés; az N12 PR addig Draft marad.
