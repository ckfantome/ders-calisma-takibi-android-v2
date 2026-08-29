package com.derscalismatakibi.app.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.util.ExportHelper
import com.derscalismatakibi.app.util.NotificationHelper
import com.derscalismatakibi.app.util.UsageStatsHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                ExportHelper.writeUsageCsv(applicationContext, UsageStatsHelper.loadTodayUsage(applicationContext), cfg.backupLabel)
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
            try { ExportHelper.writeCallSmsCsv(applicationContext, cfg.backupLabel) } catch (t: Throwable) { null }
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

        // Araliklarla tetiklenen calismalarda son e-postadan bu yana yeni oturum
        // yoksa e-postayi atla - kisa araliklarda (orn. 15dk) ayni veriyi tekrar
        // tekrar postalayip kutuyu spam'lememek icin. Sabit-saat gunluk calisma ve
        // "Simdi Yedekle" butonu bu kontrolden MUAF - her zaman gonderilir.
        if (isIntervalTrigger) {
            val hasNewData = (db.sessionDao().maxCreatedAt() ?: 0L) > cfg.lastBackupTimestamp
            if (!hasNewData) {
                AppLogger.log("Yedekleme", "Araliklarla yedekleme atlandi - son gonderimden bu yana yeni veri yok")
                settingsRepo.update(cfg.copy(lastBackupStatus = applicationContext.getString(R.string.backup_status_ok_no_new_data)))
                clearRawLogsAfterSuccess(db)
                return Result.success()
            }
        }

        // 2) E-posta, sadece acik ve dolu ayarlanmissa.
        if ((cfg.dailyBackupEnabled || cfg.intervalBackupEnabled) && cfg.backupEmail.isNotBlank() && cfg.backupEmailAppPassword.isNotBlank()) {
            val labelSuffix = if (cfg.backupLabel.isNotBlank()) " - ${cfg.backupLabel}" else ""
            val subject = applicationContext.getString(
                R.string.backup_email_subject,
                applicationContext.getString(R.string.app_name),
                labelSuffix,
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date()),
            )
            when (val sendResult = SmtpBackupSender.send(cfg.backupEmail, cfg.backupEmailAppPassword, attachments, subject = subject)) {
                is SmtpBackupSender.Result.Success -> {
                    AppLogger.log("Yedekleme", "E-posta basariyla gonderildi (${attachments.size} ek)")
                    settingsRepo.update(
                        cfg.copy(lastBackupTimestamp = System.currentTimeMillis(), lastBackupStatus = "ok"),
                    )
                    clearRawLogsAfterSuccess(db)
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
        settingsRepo.update(
            cfg.copy(lastBackupTimestamp = System.currentTimeMillis(), lastBackupStatus = applicationContext.getString(R.string.backup_status_ok_device_only)),
        )
        clearRawLogsAfterSuccess(db)
        return Result.success()
    }

    private fun notifyFailure(helper: NotificationHelper, notificationsEnabled: Boolean, message: String) {
        // Onceden HER ZAMAN zorla gosteriliyordu (notificationsEnabled=false olsa
        // bile) - kullanicinin acik istegiyle artik kendi ayri anahtariyla
        // (backupFailureNotificationsEnabled) kapatilabilir hale getirildi.
        helper.notify(applicationContext.getString(R.string.backup_notify_failed_title), message, notificationsEnabled)
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
