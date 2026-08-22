# Ders Calisma Takibi - Android

Masaustu Python/PySide6 uygulamasinin (`study_tracker2.py`) Android portu.
Kamera ile yuz/goz takibi, Pomodoro, Takvim Takip Modu, Yonetici/Ogrenci
rol sistemi, bildirim/ses, CSV disa aktarma, yerel (Room) veritabani,
**arkaplanda calisan takip servisi** ve **diger uygulamalarda gecirilen
sureyi gosteren "Uygulama Kullanimi" ekrani** (v0.3.0) dahil olmak uzere
genis bir ozellik seti bu asamada tasindi. Detaylar asagida.

## Derleme durumu: GERCEKTEN DERLENDI VE APK URETILDI (v0.3.0)

Bu makineye artik KALICI bir Android SDK kurulu (`~/Android/Sdk`, platform
34 + build-tools 34) ve Android Studio da kurulu (`~/android-studio`,
uygulama menusunden acilabilir). Asagidaki komutlarin UCU DE bu ortamda
GERCEKTEN calistirildi ve basarili oldu:

- `gradle :app:compileDebugKotlin` -> **BASARILI**
- `gradle :app:assembleDebug` -> **BASARILI** - `app-debug.apk` uretildi
  (versionCode 3, versionName "0.3.0"), Room/KSP kod uretimi, kaynak/
  manifest birlestirme, FileProvider, OpenCV/MediaPipe native
  kutuphanelerinin paketlenmesi, D8 dexleme dahil TUM pipeline calisti.
- Room migration dogrulamasi: `app/schemas/.../2.json` (Room'un KENDI
  urettigi gercek sema ciktisi) ile `AppDatabase.kt` icindeki elle yazilan
  `MIGRATION_1_2` SQL'i satir satir karsilastirildi - BIREBIR ayni (bkz.
  asagidaki "v0.3.0'da EKLENEN" madde 1).

**Dogrulanamayan sey**: Gercek bir Android cihaz/emulator bu ortamda yok,
yani APK bir telefona kurulup CANLI test edilemedi. Ozellikle:
kamera/yuz takibinin gercek cihazda dogru calismasi, arkaplan servisinin
(madde 3) pil optimizasyonlarina ve OEM kisitlamalarina (Xiaomi/Samsung
gibi bazi markalar arkaplan servislerini agresif kapatabilir) karsi
dayanikliligi, ve eski surumden guncelleme sirasinda verinin GERCEKTEN
korundugu (kurulum sirasinda) runtime'da GORSEL olarak dogrulanmadi -
sadece statik SQL/sema karsilastirmasiyla dogrulandi. Derleme/paketleme
%100 basarili; runtime testini kendi telefonunda yapman gerekiyor.


## v0.3.0'da EKLENEN ozellikler (bu turun 4 istegi)

1. **Guncelleme artik veri SILMIYOR**: Onceki surumde
   `fallbackToDestructiveMigration()` kullanildigi icin versionCode her
   arttiginda TUM kayitli oturumlar siliniyordu. Artik gercek bir
   `MIGRATION_1_2` (Room migration) nesnesi var - eski APK'yi kaldirmadan
   bu yenisini "guncelleme" olarak kurabilirsin, `sessions` tablosundaki
   verilerin KORUNUR. (v0.1.0 -> v0.2.0 gecisinde kaybolan veri bu
   duzeltmeden ONCE oldugu icin geri getirilemez - sadece BUNDAN SONRAKI
   guncellemeler icin gecerlidir.)
2. **Konusma ozelligi artik goze carpiyor**: Backend zaten calisiyordu
   (`SpeakingDetector`), ama arayuzde sadece kucuk bir metindi. Artik Ana
   ekranda durum rozetinin yaninda buyuk bir "🎤 Sessiz / Konusuyor /
   Konusuyor (esik asildi)" rozeti var (masaustundeki `speaking_badge` ile
   ayni 3 renkli durum).
3. **Arkaplanda Takip** (`service/StudyForegroundService.kt`): Ana
   ekrandaki yeni bir anahtar (Switch) ile acilir/kapanir. Acikken kamera+
   MediaPipe+Pomodoro+Takvim pipeline'i bir Android Foreground Service
   icinde calisir - uygulamadan cikip baska bir uygulamaya gecsen bile
   takip DEVAM eder. Surekli goruntude bir bildirim gosterir ("Calisiyor ·
   00:12:34" gibi), bildirimdeki "Durdur" ile servis kapatilabilir. Acikken
   Ana ekrandaki canli kamera onizlemesi (pil tasarrufu + CameraX'in tek
   seferde tek baglanma kisitlamasi nedeniyle) gizlenir, sadece durum
   metni/bildirim gosterilir.
4. **Uygulama Kullanimi** (`ui/screens/UsageStatsScreen.kt`, YENI 5.
   sekme): Masaustunde karsiligi olmayan, tamamen yeni bir ozellik -
   Android'in `UsageStatsManager` API'siyle bugun hangi uygulamada ne
   kadar vakit gecirdigini azalan sirali bir liste olarak gosterir. Bu
   ozel bir izin (`PACKAGE_USAGE_STATS`) gerektirir, normal izin
   dialoguyla ISTENEMEZ - ekran acik bir "Ayarlara Git" butonuyla
   `Settings.ACTION_USAGE_ACCESS_SETTINGS`'e yonlendirir (adimlar asagida).


## Bu turda ONEMLI bir mimari degisiklik: StudyEngine

Kamera+Pomodoro+Takvim mantigi artik `viewmodel/StudyViewModel.kt` icinde
DEGIL, `core/StudyEngine.kt` icinde bir SINGLETON (`object`) olarak
yasiyor. `StudyViewModel` artik sadece ona ince bir cephe (facade) sunan
bir sinif. Boylece hem Activity (on planda) hem de `StudyForegroundService`
(arka planda) AYNI durumu (StateFlow) paylasir - Activity kapansa/arka
plana alinsa bile Servis calisirken veri/oturum kaybolmaz.


## Onceki turdan gelen (degismeyen) cekirdek

1. **Oturum Notlari diyaloğu** (masaustu: Ctrl+N + `_reset_goal` not istemi):
   Ana ekranda "Notlar" butonu -> mevcut notu/etiketi ONCEDEN DOLU acan bir
   dialog (uzerine yazmaz, ekler/gunceller). "Hedefi Sifirla" da (eger
   `sessionNotePrompt` acik ise) ayni dialogu once gosterir.
2. **Takvim Takip Modu** (`core/Schedule.kt`, `data/ScheduleSlotEntity.kt`,
   yeni "Takvim" sekmesi): Her gun icin "+ Calisma" / "+ Mola" araliklari
   eklenir. "Takvim Takibini Baslat" -> bugunun ozeti + onay -> Calisma
   araligina girince Pomodoro IDLE ise otomatik baslar; mola sirasinda
   calismaya devam edilirse veya bir calisma diliminde planlanandan az
   calisilirsa dilim bitince bildirim + oturum notuna satir eklenir
   (masaustundeki `_process_schedule_tracking`/`_close_active_schedule_slot`
   ile ayni mantik ve ayni not formati: `[Takvim] 08:00-09:00 Calisma:
   5/60 dk calisildi.`).
3. **Yonetici/Ogrenci rol sistemi**: Ust bar sag ustteki kilit ikonu.
   Varsayilan ADMIN; Ogrenci moduna gecis onay ister, Ogrenci'den
   Yonetici'ye donus PIN ister (varsayilan `1234`, Ayarlar ekranindan
   Yonetici degistirebilir). Ogrenci modunda: Ayarlar salt-okunur, Takvim
   ekleme/silme kilitli, "Hedefi Sifirla" engellenir.
4. **CSV disa aktarma**: Istatistikler ekraninda "Disa Aktar (CSV)" ->
   Room'daki tum oturumlari uygulama-ozel depoya yazar, ardindan Android'in
   paylasim (share sheet) penceresiyle istedigin yere (WhatsApp, Drive,
   e-posta...) gonderebilirsin (izin GEREKTIRMEZ, `FileProvider` kullanir).
5. **Bildirim + ses**: `util/NotificationHelper.kt` - Pomodoro
   calisma/mola bitince ve Takvim dilim ozetlerinde sistem bildirimi
   (`NotificationCompat`, Android 13+ icin `POST_NOTIFICATIONS` izni
   Manifest'e eklendi ve ilk acilista istenir) + kisa bir bip sesi
   (`ToneGenerator`). Ayarlar ekranindan ac/kapa.
6. **Tema secimi**: Ayarlar ekraninda Sistem/Koyu/Acik uc secenekli
   segment kontrolu; `MainActivity` bunu okuyup `DersCalismaTakibiTheme`'e
   uygular (tam tasarim/vurgu rengi editoru degil, ama masaustundeki
   THEME_MODE'un dogrudan karsiligi).
7. **Haftalik hedef gostergesi**: Ana ekranda gunluk ilerlemenin altina
   "Bu hafta" karti (Room'dan haftalik toplam + ilerleme cubugu) eklendi;
   Istatistikler ekraninda da ayni deger tekrar gosterilir.


## Onceki turdan gelen (degismeyen) cekirdek

Kamera+MediaPipe pipeline, EAR/MAR/kafa pozu hesaplamalari, Hysteresis
durum makinesi, Pomodoro mantigi (masaustunde bu oturumda duzeltilen BREAK/
LONG_BREAK buton hatasi dahil), Room `sessions` tablosu, DataStore ayarlari,
Takvim Takip Modu, Yonetici/Ogrenci rol sistemi, CSV disa aktarma, tema
secimi, haftalik hedef gostergesi (v0.2.0'da eklenenler) - bunlarin
HICBIRINE dokunulmadi, hepsi calismaya devam ediyor (bkz. asagidaki "Kod
haritasi").


## Arkaplan Takibi nasil kullanilir

1. Ana ekranda "Arkaplanda Takip" karti -> anahtari (Switch) ac.
2. Sistem bir bildirim gosterecek ("Ders Calisma Takibi arkaplanda
   calisiyor · Calisiyor · 00:00:05" gibi) - bu bildirim durdurulana kadar
   SUREKLI goruntude kalir (Android'in kamera kullanan arkaplan servisleri
   icin zorunlu kildigi bir seffaflik onlemi, gizlice calismaz).
3. Baska bir uygulamaya gecebilirsin, takip devam eder.
4. Durdurmak icin: ya bildirimdeki "Durdur" aksiyonuna bas, ya da
   uygulamaya donup ayni anahtari kapat.
5. **Pil notu**: Android 12+ cihazlarda "pil optimizasyonu" bu tur
   arkaplan servislerini agresif kapatabilir (ozellikle Xiaomi/Samsung/
   Huawei gibi bazi markalarin kendi ek kisitlamalari var) - servis
   beklenmedik sekilde duruyorsa telefonun Ayarlar > Pil > Uygulama pil
   kullanimi bolumunden bu uygulama icin "Kisitlama Yok"/"Optimize etme"
   secmen gerekebilir (cihaza gore menu adi degisir).


## Uygulama Kullanimi izni nasil verilir

"Kullanim" sekmesi acildiginda izin yoksa bir "Ayarlara Git" butonu
gorursun; ona basinca sistem Ayarlar uygulamasina yonlendirilirsin.
Orada bu uygulamayi (Ders Calisma Takibi) bulup "Izin ver"/"Ac" yapman
gerekir. Menu tam yeri cihaza/Android surumune gore degisir, genel olarak:
`Ayarlar > Uygulamalar > Ozel erisim > Kullanim erisimi > Ders Calisma
Takibi > Ac` (bazi cihazlarda: `Ayarlar > Guvenlik > Ozel izinler >
Kullanim erisimi`). Izni verdikten sonra uygulamaya geri donup "Kullanim"
sekmesine tekrar girmen yeterli - ekran her acilista izni otomatik
yeniden kontrol eder.


## Hala TASINMAYAN / basitlestirilmis kalan ozellikler

- **Tam tasarim/tema ozellestirme editoru** (DesignPageDialog, vurgu rengi
  secimi, layout slider'lari) - yok. Sadece Sistem/Koyu/Acik tema toggle'i
  var (madde 6, yukarida). Bu kasitli bir kapsam kararidir: masaustundeki
  editor kisisel-kullanim icin bile asiri detayliydi (bu konusmada bir
  bug'in da kaynagiydi), mobilde karsiligi daha da az onemli.
- **Kamera goruntusu uzerinde yuz landmark noktalarini cizme**
  (SHOW_FACE_LANDMARKS) - yok, sadece canli kamera onizlemesi var.
- **JSON disa/ice aktarma** - sadece CSV disa aktarma var (madde 4), JSON
  yok, ICE aktarma (baska bir cihazdan/masaustunden veri yukleme) hic yok.
- **matplotlib tarzi grafikler** - yok, Istatistikler ekraninda hala basit
  bir liste var (gunluk toplamlar).
- **Zaman dilimi/gece yarisi edge-case'i**: Takvim Takip Modu, ayni
  (baslangic,bitis,tur) degerine sahip iki farkli gunu (orn. gece yarisini
  gecerek) ayni slot sayabilir - masaustu sürümünde de belirtilen bilinen
  bir sinirlama, kisisel kullanim icin onemsiz.
- **Arkaplanda Takip acikken canli kamera onizlemesi**: Bilincli bir
  tasarim karari (CameraX'in tek-baglama kisitlamasi + pil tasarrufu) -
  onizleme yerine sadece durum metni/bildirim gosterilir (bkz. yukarida
  "Arkaplan Takibi nasil kullanilir").
- **Uygulama Kullanimi verisi Room'a kaydedilmiyor**: Android zaten kendi
  `UsageStatsManager` gecmisini tutuyor, biz sadece anlik sorguluyoruz;
  gecmis gunlerin/haftalarin trend grafigi gibi bir ozellik YOK, sadece
  "bugun".


## Nasil acilir / calistirilir

1. Uygulama menusunden **Android Studio**'yu ac (ya da `~/android-studio/bin/studio.sh`).
2. **File > Open** ile bu klasoru (`android/`) sec. SDK zaten
   `~/Android/Sdk`'da kurulu oldugu icin (`local.properties` bunu isaret
   ediyor) Gradle Sync hizlica tamamlanmali.
3. Fiziksel bir Android cihazi (USB hata ayiklama acik) veya bir emulator
   bagla. **Not**: Emulatorde on kamera genelde sanaldir (gercek yuz
   algilamaz); gercek testi fiziksel bir telefon/tablette yapman onerilir.
4. Run (yesil ok) ile calistir. Ilk acilista kamera + bildirim izinleri
   istenecek, ikisini de ver.

Ya da komut satirindan dogrudan derlemek icin:
```
cd android
export ANDROID_HOME=~/Android/Sdk
gradle :app:assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk` (ayrica
`../apk/DersCalismaTakibi-debug.apk` konumuna da kopyalandi).


## Olasi Ilk Kurulum Sorunlari

1. **Gradle/AGP surum uyumsuzlugu**: AGP 8.3.0 + Kotlin 1.9.22 + Gradle 8.7
   kullanildi. Android Studio farkli bir surum onerirse kabul etmen sorun
   cikarmaz.
2. **KSP surumu**: `com.google.devtools.ksp` (`1.9.22-1.0.17`) Kotlin
   surumune baglidir; Kotlin surumunu degistirirsen KSP'yi de
   https://github.com/google/ksp/releases 'ten eslesen surume guncelle.
3. **Room semasi (v1 -> v2) - ARTIK GUVENLI**: `schedule_slots` tablosu
   v0.2.0'da eklendi; o zaman `fallbackToDestructiveMigration()`
   kullanildigi icin v0.1.0'dan v0.2.0'a gecenlerin verisi silinmisti.
   v0.3.0'dan itibaren gercek bir `MIGRATION_1_2` var (`data/
   AppDatabase.kt`) - `app/schemas/.../2.json` (Room'un kendi urettigi
   sema) ile birebir karsilastirilarak dogrulandi. Bundan sonraki
   guncellemeler YEREL VERIYI SILMEZ. Yeni bir tablo/kolon eklersen
   (Room versionu artirirsan) YENI bir `MIGRATION_2_3` yazmayi unutma -
   `exportSchema = true` zaten acik, `gradle :app:assembleDebug` her
   calistiginda `app/schemas/` altina guncel semayi yazar, yeni migration'i
   ona gore hazirlayabilirsin.
4. **POST_NOTIFICATIONS izni reddedilirse**: Bildirimler sessizce
   gosterilmez (uygulama cokmez), sadece ekrandaki `pauseNotice`/anlik
   durum metinleriyle yetinilir.
5. **OpenCV/MediaPipe native kutuphaneler**: `assembleDebug` sirasinda
   basariyla paketlendigi dogrulandi; runtime'da `UnsatisfiedLinkError`
   alirsan `org.opencv:opencv` ve `com.google.mediapipe:tasks-vision`
   surumlerini Maven Central'daki en guncele yukselt.
6. **NDK gerekmiyor**: MediaPipe/OpenCV kendi native kutuphanelerini AAR
   icinde tasiyor, elle bir NDK kurulumu yapmana gerek yok.


## Kod haritasi

```
app/src/main/java/com/derscalismatakibi/app/
  core/                  Qt/Android'den bagimsiz saf Kotlin mantik
    State.kt             StudyState, PoseEstimate, FrameAnalysis
    AppConfig.kt         DEFAULT_CONFIG karsiligi (+ appPin/sound/notif/tema)
    FrameAnalyzer.kt     EAR/MAR/kafa pozu hesaplamalari (analyze_frame)
    SpeakingDetector.kt  Konusma tespiti + "dikkat dagitici" onay kapisi
    HysteresisStateMachine.kt
    PomodoroTimer.kt
    Session.kt           Oturum sayaclari + verimlilik skoru
    Role.kt              ADMIN / STUDENT
    Schedule.kt           Takvim Takip: slot turleri, current_schedule_slot vb.
    StudyEngine.kt        [YENI v0.3.0] Uygulama-genelinde SINGLETON motor -
                           Activity/Servis AYRIMINDAN bagimsiz tek durum kaynagi.
  data/                  Kalicilik
    SessionEntity.kt / SessionDao.kt                      (Room)
    ScheduleSlotEntity.kt / ScheduleDao.kt                 (Room)
    AppDatabase.kt                                         (Room, v2 + gercek Migration)
    SettingsRepository.kt                                  (DataStore)
  util/
    NotificationHelper.kt   Bildirim + ToneGenerator bip sesi
    ExportHelper.kt         CSV disa aktarma + FileProvider paylasim Intent'i
  camera/                CameraX <-> MediaPipe koprusu
    FaceLandmarkerHelper.kt
    CameraAnalyzer.kt
  service/
    StudyForegroundService.kt  [YENI v0.3.0] Arkaplanda kamera+takip; kendi
                                 CameraX baglamasini tutar, StudyEngine'i besler,
                                 surekli bildirim + "Durdur" aksiyonu gosterir.
  viewmodel/
    StudyViewModel.kt    [v0.3.0'da SADELESTI] Artik sadece StudyEngine'e ince
                          bir cephe (facade) - gercek mantik StudyEngine.kt'de.
  ui/
    AppNavigation.kt     5 sekmeli alt nav (Ana/Takvim/Istatistik/Kullanim/
                          Ayarlar) + ust bar rol ikonu/PIN dialogu
    theme/Theme.kt
    screens/MainScreen.kt / ScheduleScreen.kt / StatsScreen.kt /
            SettingsScreen.kt / UsageStatsScreen.kt [YENI v0.3.0]
  MainActivity.kt        Tema secimini uygular, OpenCV yukler
```
