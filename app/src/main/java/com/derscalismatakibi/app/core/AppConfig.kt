package com.derscalismatakibi.app.core

/**
 * study_tracker2.py -> DEFAULT_CONFIG sozlugunun Kotlin karsiligi. Varsayilan
 * degerler masaustu ile BIREBIR AYNI. DataStore'da bu alanlarin her biri ayri
 * bir Preferences key olarak saklanir (bkz. viewmodel/SettingsRepository.kt).
 */
data class AppConfig(
    val earClosedThreshold: Double = 0.21,
    val yawMaxDeg: Double = 45.0,
    val pitchDownMaxDeg: Double = 55.0,
    val pitchUpMaxDeg: Double = 25.0,
    val confirmAwaySeconds: Double = 6.0,
    val confirmSleepSeconds: Double = 4.0,
    val confirmResumeSeconds: Double = 2.0,
    val pomodoroWorkMin: Int = 25,
    val pomodoroBreakMin: Int = 5,
    val pomodoroLongBreakMin: Int = 15,
    val pomodoroCyclesBeforeLong: Int = 4,
    val dailyGoalHours: Double = 4.0,
    val weeklyGoalHours: Double = 20.0,
    val autoPauseOnAway: Boolean = true,
    val autoPauseOnSleep: Boolean = true,
    val sessionNotePrompt: Boolean = true,
    val speakingCountsAsAway: Boolean = false,
    val speakingMarStdThreshold: Double = 0.018,
    val speakingMarMinThreshold: Double = 0.028,
    val speakingWindowSize: Int = 12,
    val confirmSpeakingSeconds: Double = 8.0,
    val useFrontCamera: Boolean = true,
    val appPin: String = "1234",
    val soundEnabled: Boolean = true,
    // --- Bildirimler: tek "Bildirimler" anahtari yerine tur bazinda ayri kontrol -
    // eskiden "Yedekleme Basarisiz" hep zorla gosteriliyordu, artik o da secilebilir. ---
    /** Pomodoro tamamlandi/Mola bitti + Calisma Araligi Tamamlandi/Mola Sirasinda Calisma. */
    val routineNotificationsEnabled: Boolean = true,
    /** Sinav Modu acikken Erisilebilirlik izni kapaninca gelen kritik uyari. */
    val appLockAlertNotificationsEnabled: Boolean = true,
    /** Guvenli bolgeye girme/cikma bildirimi. */
    val safeZoneAlertNotificationsEnabled: Boolean = true,
    /** Gunluk yedekleme e-postasi basarisiz olunca gelen uyari. */
    val backupFailureNotificationsEnabled: Boolean = true,
    /** "dark" / "light" / "system" - study_tracker2.py -> THEME_MODE. */
    val themeMode: String = "system",
    /** "tr" / "en" - API 33+ cihazlarda LocaleManager ile uygulanir. */
    val appLanguage: String = "tr",
    /** Gunluk otomatik yedekleme: cihaza kaydet + (doluysa) bu adrese kendi
     * kendine e-posta gonder (Gmail SMTP, ayni hesap hem gonderen hem alici). */
    val backupEmail: String = "",
    val backupEmailAppPassword: String = "",
    val dailyBackupEnabled: Boolean = false,
    val backupHour: Int = 23,
    val backupMinute: Int = 0,
    val lastBackupTimestamp: Long = 0L,
    /** "" | "ok" | "ok (sadece cihaza)" | "error: ..." */
    val lastBackupStatus: String = "",
    /** legal/PrivacyConsent.kt -> KVKK/gizlilik onayi. */
    val privacyConsentAccepted: Boolean = false,
    val privacyConsentVersion: Int = 0,
    val privacyConsentTimestamp: Long = 0L,
    /** Uygulama Kilidi: acikken varsayilan olarak TUM uygulamalar engellenir,
     * SADECE examAllowedPackages'taki (+ zorunlu guvenlik listesi: kendi
     * uygulamamiz, varsayilan ana ekran, telefon/arama) uygulamalar acilabilir. */
    val examModeEnabled: Boolean = false,
    /** Virgulle ayrilmis paket adlari - Sinav/Odev Modu acikken acilmasina izin
     * verilenler (orn. Hesap Makinesi). Bos ise sadece zorunlu guvenlik listesi acik kalir. */
    val examAllowedPackages: String = "",
    /** Klavye Takibi (Erisilebilirlik ile yazilan metni izler) - varsayilan KAPALI,
     * cunku "ekran icerigini okumaz" garantisini degistirir; acildiginda KVKK
     * onayi yeniden istenir (bkz. PrivacyConsent.VERSION). Sifre alanlari HARIC tutulur. */
    val keyboardTrackingEnabled: Boolean = false,
    /** Cihaz yeniden baslayinca (reboot) arkaplan takip servisini kamera izni
     * varsa sessizce yeniden baslatir (bkz. service/BootAndRestartReceiver.kt). */
    val autoStartOnBootEnabled: Boolean = true,
    /** Uygulama Son Kullanilanlar'dan kaldirilinca (kaydirilarak kapatilinca)
     * servisin kendini yeniden baslatmasi - ebeveyn-denetim takibinin cocuk
     * tarafindan kolayca kapatilmasini zorlastirir. */
    val keepAliveEnabled: Boolean = true,
    /** Sinav/Odev Modu (examModeEnabled) acikken EK olarak Ekran Sabitleme
     * (Activity.startLockTask) kullanip kullanmayacagi - acikken examAllowedPackages
     * istisna listesi ISLEMEZ, telefon tek uygulamaya kilitlenir. Varsayilan KAPALI,
     * cunku mevcut istisna-listeli davranisi degistiriyor - admin bilerek acar. */
    val screenPinningEnabled: Boolean = false,

    // --- Calisan Sistemler: her biri ayri bir izleme/analiz sistemini acar-kapar. ---
    /** Kamera + MediaPipe yuz analizi (calisma tespiti). Kapaliyken arkaplan
     * servisi kamerayi hic acmadan (konum/klavye/uygulama kilidi icin) calisir. */
    val cameraAnalysisEnabled: Boolean = true,
    /** Guvenli Bolge kontrolu + surekli konum gecmisi kaydi (StudyEngine.checkSafeZone). */
    val locationTrackingEnabled: Boolean = true,
    /** Arama/SMS ozeti - kapaliyken Arama/SMS ekrani ve gunluk yedek hic veri okumaz. */
    val callSmsLogEnabled: Boolean = true,
    /** Gunluk sure siniri kontrolu (UsageStatsManager) kac saniyede bir tazelensin -
     * her on plan uygulama degisiminde sorgulamak agir/pilyicidir. 0 = anlik (her
     * seferinde tazele), varsayilan 10 (Normal), 60 = Pil Dostu. */
    val usageCheckIntervalSeconds: Int = 10,
    /** Diger uygulamalardan gelen bildirimlerin Loglar'a yazilmasi. */
    val notificationLogEnabled: Boolean = true,

    // --- Gonderilen Veriler: gunluk yedek e-postasina hangi dosyalarin eklenecegi
    // (cihaza yazma HER ZAMAN yapilir, bu sadece e-posta ekini kontrol eder). ---
    val sendSessionCsv: Boolean = true,
    val sendScheduleCsv: Boolean = true,
    val sendUsageCsv: Boolean = true,
    val sendCallSmsCsv: Boolean = true,
    val sendDeviceReport: Boolean = true,
    val sendBlockedAppsTxt: Boolean = true,
    val sendAppLog: Boolean = true,
    val sendLocationCsv: Boolean = true,
    val sendKeystrokeCsv: Boolean = true,
)
