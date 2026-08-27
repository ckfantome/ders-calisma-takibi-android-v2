package com.derscalismatakibi.app.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.util.ExportHelper
import com.derscalismatakibi.app.util.NotificationHelper
import com.derscalismatakibi.app.util.UsageStatsHelper
import kotlinx.coroutines.flow.first

/**
 * Gunde bir kez (bkz. BackupScheduler) calisir: o gune kadarki TUM veriyi
 * cihaza yazar, ayarlar izin veriyorsa ayrica ayni Gmail hesabina e-posta
 * gonderir. Basarisizlikta hem Ayarlar'daki durum metnine hem de bir bildirime
 * yansitilir (kullaniciyla netlesen "ikisi de" karari).
 */
class DailyBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        AppLogger.log("Yedekleme", "Gunluk yedekleme baslatildi")
        val settingsRepo = SettingsRepository(applicationContext)
        val cfg = settingsRepo.configFlow.first()
        val db = AppDatabase.getInstance(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)

        val sessions = db.sessionDao().allSessions()
        val slots = db.scheduleDao().all()

        // 1) HER ZAMAN cihaza yaz (yedekleme e-postadan bagimsiz calisir).
        val csvFile = try {
            ExportHelper.writeDailyBackupCsv(applicationContext, sessions)
        } catch (t: Throwable) {
            AppLogger.logError("Yedekleme", "CSV dosyasi yazilamadi", t)
            settingsRepo.update(cfg.copy(lastBackupStatus = "error: dosya yazilamadi (${t.message})"))
            return Result.retry()
        }
        val scheduleFile = try {
            ExportHelper.writeScheduleJson(applicationContext, slots)
        } catch (t: Throwable) {
            null // takvim verisi olmadan da yedekleme/e-posta devam edebilir.
        }
        // Uygulama Kullanimi izni ayrica verilmis olabilir (Kullanim ekraninden) -
        // yoksa sessizce atla, yedeklemenin geri kalanini engelleme.
        val usageFile = if (UsageStatsHelper.hasUsageAccess(applicationContext)) {
            try {
                ExportHelper.writeUsageCsv(applicationContext, UsageStatsHelper.loadTodayUsage(applicationContext))
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
        val callSmsFile = try { ExportHelper.writeCallSmsCsv(applicationContext) } catch (t: Throwable) { null }
        val deviceReportFile = try { ExportHelper.writeDeviceReportTxt(applicationContext) } catch (t: Throwable) { null }
        val blockedApps = try { db.blockedAppDao().all() } catch (t: Throwable) { emptyList() }
        val blockedAppsFile = try { ExportHelper.writeBlockedAppsTxt(applicationContext, blockedApps, cfg.examModeEnabled) } catch (t: Throwable) { null }
        val logFile = AppLogger.currentLogFile()?.takeIf { it.exists() }
        val attachments = listOfNotNull(csvFile, scheduleFile, usageFile, callSmsFile, deviceReportFile, blockedAppsFile, logFile)

        // 2) E-posta, sadece acik ve dolu ayarlanmissa.
        if (cfg.dailyBackupEnabled && cfg.backupEmail.isNotBlank() && cfg.backupEmailAppPassword.isNotBlank()) {
            when (val sendResult = SmtpBackupSender.send(cfg.backupEmail, cfg.backupEmailAppPassword, attachments)) {
                is SmtpBackupSender.Result.Success -> {
                    AppLogger.log("Yedekleme", "E-posta basariyla gonderildi (${attachments.size} ek)")
                    settingsRepo.update(
                        cfg.copy(lastBackupTimestamp = System.currentTimeMillis(), lastBackupStatus = "ok"),
                    )
                    return Result.success()
                }
                is SmtpBackupSender.Result.TransientFailure -> {
                    AppLogger.logError("Yedekleme", "E-posta gecici hata - tekrar denenecek: ${sendResult.message}")
                    settingsRepo.update(cfg.copy(lastBackupStatus = "error: gonderim basarisiz, tekrar denenecek"))
                    notifyFailure(notificationHelper, "Gunluk yedekleme e-postasi gonderilemedi, tekrar denenecek.")
                    return Result.retry()
                }
                is SmtpBackupSender.Result.PermanentFailure -> {
                    AppLogger.logError("Yedekleme", "E-posta kalici hata: ${sendResult.message}")
                    settingsRepo.update(cfg.copy(lastBackupStatus = "error: ${sendResult.message}"))
                    notifyFailure(notificationHelper, "Gunluk yedekleme e-postasi gonderilemedi: ${sendResult.message}")
                    return Result.failure()
                }
            }
        }

        // E-posta kapali/eksik - sadece cihaza yedekleme basarili sayilir.
        AppLogger.log("Yedekleme", "Sadece cihaza yazildi (e-posta kapali/eksik ayar)")
        settingsRepo.update(
            cfg.copy(lastBackupTimestamp = System.currentTimeMillis(), lastBackupStatus = "ok (sadece cihaza)"),
        )
        return Result.success()
    }

    private fun notifyFailure(helper: NotificationHelper, message: String) {
        // Kullaniciyla netlesen karar: basarisizlikta HEM durum metni HEM bildirim.
        // notificationsEnabled=false olsa bile bir yedekleme basarisizligi onemli
        // sayilip bildirilir (Pomodoro bildirimlerinin aksine, sessiz gecilmez).
        helper.notify("Yedekleme Basarisiz", message, notificationsEnabled = true)
    }
}
