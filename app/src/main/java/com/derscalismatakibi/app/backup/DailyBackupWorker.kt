package com.derscalismatakibi.app.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.util.DateTimeFormats
import com.derscalismatakibi.app.util.ExportHelper
import com.derscalismatakibi.app.util.NotificationHelper
import com.derscalismatakibi.app.util.UsageStatsHelper
import kotlinx.coroutines.flow.first

/**
 * Gunde bir kez sabit saatte VEYA (ayarlandiysa) belirli araliklarla (bkz.
 * BackupScheduler) calisir: o ana kadarki TUM veriyi cihaza yazar, ayarlar
 * izin veriyorsa ayrica ayni Gmail hesabina e-posta gonderir. Basarisizlikta
 * hem Ayarlar'daki durum metnine hem de bir bildirime yansitilir (kullaniciyla
 * netlesen "ikisi de" karari).
 */
class DailyBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val isIntervalTrigger = inputData.getBoolean(BackupScheduler.WORK_DATA_KEY_IS_INTERVAL_TRIGGER, false)
        val triggerType = inputData.getString(BackupScheduler.WORK_DATA_KEY_TRIGGER_TYPE)
            ?: if (isIntervalTrigger) BackupScheduler.TRIGGER_TYPE_INTERVAL else BackupScheduler.TRIGGER_TYPE_DAILY
        AppLogger.log("Yedekleme", if (isIntervalTrigger) "Araliklarla yedekleme baslatildi" else "Gunluk yedekleme baslatildi")
        val settingsRepo = SettingsRepository(applicationContext)
        val cfg = settingsRepo.configFlow.first()
        val db = AppDatabase.getInstance(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)

        val sessions = db.sessionDao().allSessions()
        val slots = db.scheduleDao().all()

        // 1) HER ZAMAN cihaza yaz (yedekleme e-postadan bagimsiz calisir).
        val csvFile = try {
            ExportHelper.writeDailyBackupCsv(applicationContext, sessions, cfg.backupLabel)
        } catch (t: Throwable) {
            AppLogger.logError("Yedekleme", "CSV dosyasi yazilamadi", t)
            settingsRepo.update(cfg.copy(lastBackupStatus = applicationContext.getString(R.string.backup_status_file_write_error, t.message)))
            return Result.retry()
        }
        val scheduleFile = try {
            ExportHelper.writeScheduleJson(applicationContext, slots, cfg.backupLabel)
        } catch (t: Throwable) {
            null // takvim verisi olmadan da yedekleme/e-posta devam edebilir.
        }
        // Uygulama Kullanimi izni ayrica verilmis olabilir (Kullanim ekraninden) -
        // yoksa sessizce atla, yedeklemenin geri kalanini engelleme.
        val usageFile = if (UsageStatsHelper.hasUsageAccess(applicationContext)) {
            try {
                ExportHelper.writeUsageCsv(applicationContext, UsageStatsHelper.loadTodaySessions(applicationContext), cfg.backupLabel)
            } catch (t: Throwable) {
                null
            }
        } else {
            null
        }
        // Kullanicinin istegi: "tum bilgiler" gonderilsin - izin verilmis her
        // veri kaynagi (arama/SMS, cihaz raporu, uygulama kilidi durumu, tam
        // debug logu) da gunluk e-postaya eklenir. Izin/veri yoksa ilgili
        // fonksiyon sessizce null/bos doner, gunluk yedeklemenin geri kalanini
        // engellemez.
        val callSmsFile = if (cfg.callSmsLogEnabled) {
            try { ExportHelper.writeCallSmsCsv(applicationContext, cfg.backupLabel, cfg.callSmsLogMaxEntries) } catch (t: Throwable) { null }
        } else null
        val deviceReportFile = try { ExportHelper.writeDeviceReportTxt(applicationContext, cfg.backupLabel) } catch (t: Throwable) { null }
        val blockedApps = try { db.blockedAppDao().all() } catch (t: Throwable) { emptyList() }
        val blockedAppsFile = try { ExportHelper.writeBlockedAppsTxt(applicationContext, blockedApps, cfg.examModeEnabled, cfg.backupLabel) } catch (t: Throwable) { null }
        val logFile = AppLogger.currentLogFile()?.takeIf { it.exists() }
        // "Sadece anlik degil, surekli degisen TUM konum" + klavye takibi kayitlari
        // da eklensin istegi - ikisi de bos/kapaliysa fonksiyonlar null doner.
        val locationFile = try {
            ExportHelper.writeLocationHistoryCsv(applicationContext, db.locationLogDao().all(), cfg.backupLabel)
        } catch (t: Throwable) { null }
        val keystrokeFile = try {
            ExportHelper.writeKeystrokeLogCsv(applicationContext, db.keystrokeLogDao().observeRecent().first(), cfg.backupLabel)
        } catch (t: Throwable) { null }
        // "Gonderilen Veriler" ayarlari: cihaza yazma HER ZAMAN yukarida yapildi,
        // bu sadece e-postaya hangi dosyalarin eklenecegini secer.
        val attachments = listOfNotNull(
            csvFile.takeIf { cfg.sendSessionCsv },
            scheduleFile.takeIf { cfg.sendScheduleCsv },
            usageFile.takeIf { cfg.sendUsageCsv },
            callSmsFile.takeIf { cfg.sendCallSmsCsv },
            deviceReportFile.takeIf { cfg.sendDeviceReport },
            blockedAppsFile.takeIf { cfg.sendBlockedAppsTxt },
            logFile.takeIf { cfg.sendAppLog },
            locationFile.takeIf { cfg.sendLocationCsv },
            keystrokeFile.takeIf { cfg.sendKeystrokeCsv },
        )

        // NOT: Araliklarla tetiklenen calismalar ONCEDEN "son gonderimden bu yana
        // yeni SESSION (bitmis calisma blogu) yoksa e-postayi atla" kontrolunden
        // geciyordu - ancak bir session genelde 15dk'dan uzun surdugu icin bu
        // kontrol neredeyse HER interval calismasinda e-postayi sessizce
        // atliyordu (kullanicinin "15dk'da bir yedekle" ayarladigi halde hicbir
        // mail gelmemesinin kok nedeni). Interval tetiklemeler artik gunluk/manuel
        // ile ayni sekilde HER ZAMAN e-posta gonderir - ham log SILME'si ayri bir
        // kontroldur ve interval tetiklemede hala tetiklenmez (asagida).

        // 2) E-posta, sadece acik ve dolu ayarlanmissa.
        if ((cfg.dailyBackupEnabled || cfg.intervalBackupEnabled) && cfg.backupEmail.isNotBlank() && cfg.backupEmailAppPassword.isNotBlank()) {
            val labelSuffix = if (cfg.backupLabel.isNotBlank()) " - ${cfg.backupLabel}" else ""
            val triggerLabel = applicationContext.getString(
                when (triggerType) {
                    BackupScheduler.TRIGGER_TYPE_INTERVAL -> R.string.backup_trigger_type_interval
                    BackupScheduler.TRIGGER_TYPE_MANUAL -> R.string.backup_trigger_type_manual
                    else -> R.string.backup_trigger_type_daily
                },
            )
            val subject = applicationContext.getString(
                R.string.backup_email_subject,
                applicationContext.getString(R.string.app_name),
                labelSuffix,
                DateTimeFormats.display(System.currentTimeMillis()),
                triggerLabel,
            )
            when (val sendResult = SmtpBackupSender.send(cfg.backupEmail, cfg.backupEmailAppPassword, attachments, subject = subject, smtpHost = cfg.smtpHost, smtpPort = cfg.smtpPort)) {
                is SmtpBackupSender.Result.Success -> {
                    AppLogger.log("Yedekleme", "E-posta basariyla gonderildi (${attachments.size} ek)")
                    val now = System.currentTimeMillis()
                    settingsRepo.update(
                        cfg.copy(
                            lastBackupTimestamp = now,
                            lastBackupStatus = "ok",
                            lastRealDailyBackupTimestamp = if (!isIntervalTrigger) now else cfg.lastRealDailyBackupTimestamp,
                        ),
                    )
                    // Silme sadece GERCEK gunluk yedekleme basarisinda - araliklarla
                    // tetiklenen (incremental) calisma silme islemini tetiklemez.
                    if (!isIntervalTrigger) maybeDeleteOldRecords(cfg, now, db)
                    return Result.success()
                }
                is SmtpBackupSender.Result.TransientFailure -> {
                    AppLogger.logError("Yedekleme", "E-posta gecici hata - tekrar denenecek: ${sendResult.message}")
                    settingsRepo.update(cfg.copy(lastBackupStatus = applicationContext.getString(R.string.backup_status_transient_error)))
                    notifyFailure(notificationHelper, cfg.backupFailureNotificationsEnabled, applicationContext.getString(R.string.backup_notify_transient_failure))
                    return Result.retry()
                }
                is SmtpBackupSender.Result.PermanentFailure -> {
                    AppLogger.logError("Yedekleme", "E-posta kalici hata: ${sendResult.message}")
                    settingsRepo.update(cfg.copy(lastBackupStatus = applicationContext.getString(R.string.backup_status_permanent_error, sendResult.message)))
                    notifyFailure(notificationHelper, cfg.backupFailureNotificationsEnabled, applicationContext.getString(R.string.backup_notify_permanent_failure, sendResult.message))
                    return Result.failure()
                }
            }
        }

        // E-posta kapali/eksik - sadece cihaza yedekleme basarili sayilir.
        AppLogger.log("Yedekleme", "Sadece cihaza yazildi (e-posta kapali/eksik ayar)")
        val now = System.currentTimeMillis()
        settingsRepo.update(
            cfg.copy(
                lastBackupTimestamp = now,
                lastBackupStatus = applicationContext.getString(R.string.backup_status_ok_device_only),
                lastRealDailyBackupTimestamp = if (!isIntervalTrigger) now else cfg.lastRealDailyBackupTimestamp,
            ),
        )
        // Silme sadece GERCEK gunluk yedekleme basarisinda - araliklarla
        // tetiklenen (incremental) calisma silme islemini tetiklemez.
        if (!isIntervalTrigger) maybeDeleteOldRecords(cfg, now, db)
        return Result.success()
    }

    private fun notifyFailure(helper: NotificationHelper, notificationsEnabled: Boolean, message: String) {
        // Onceden HER ZAMAN zorla gosteriliyordu (notificationsEnabled=false olsa
        // bile) - kullanicinin acik istegiyle artik kendi ayri anahtariyla
        // (backupFailureNotificationsEnabled) kapatilabilir hale getirildi.
        helper.notify(applicationContext.getString(R.string.backup_notify_failed_title), message, notificationsEnabled)
    }

    /** Eski Kayitlari Otomatik Sil ayari: SADECE bu fonksiyonun cagrildigi yerden
     * (yani zaten gercek/interval-olmayan bir gunluk yedekleme basariyla
     * tamamlandiginda) tetiklenir - bagimsiz bir zamanlayici/timer/cron YOKTUR.
     * Ayar kapaliysa hicbir sey silinmez. Ayar acik olsa bile, bir onceki gercek
     * yedeklemeden (cfg.lastRealDailyBackupTimestamp, BU calismadan ONCEKI deger)
     * bu yana 24 saatten az gectiyse yine silinmez - amac, art arda hizli
     * "Simdi Yedekle" tetiklemelerinin her seferinde silme yapmasini engellemek. */
    private suspend fun maybeDeleteOldRecords(cfg: com.derscalismatakibi.app.core.AppConfig, now: Long, db: AppDatabase) {
        if (!cfg.autoDeleteOldRecordsEnabled) {
            AppLogger.log("Yedekleme", "Eski kayit silme kapali - atlaniyor")
            return
        }
        val previous = cfg.lastRealDailyBackupTimestamp
        if (previous != 0L && now - previous < 24L * 60 * 60 * 1000) {
            AppLogger.log("Yedekleme", "Eski kayit silme atlandi - son gercek yedeklemeden bu yana 24 saat gecmedi")
            return
        }
        clearRawLogsAfterSuccess(db)
    }

    /** Konum/klavye takibi verisi bu noktada zaten cihaza (ve varsa e-postaya)
     * yazildi - DB'de sinirsiz birikip zamanla yedek dosyasini/e-postayi
     * sisirmemesi icin ham kayitlar temizlenir. Sessions/takvim/kilitli
     * uygulamalar gibi "kalici" veriler buna DAHIL DEGIL, sadece bu iki ham log. */
    private suspend fun clearRawLogsAfterSuccess(db: AppDatabase) {
        try { db.locationLogDao().clear() } catch (t: Throwable) { AppLogger.logError("Yedekleme", "Konum gecmisi temizlenemedi", t) }
        try { db.keystrokeLogDao().clear() } catch (t: Throwable) { AppLogger.logError("Yedekleme", "Klavye takibi gecmisi temizlenemedi", t) }
    }
}
