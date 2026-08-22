package com.derscalismatakibi.app.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.util.ExportHelper
import com.derscalismatakibi.app.util.NotificationHelper
import kotlinx.coroutines.flow.first

/**
 * Gunde bir kez (bkz. BackupScheduler) calisir: o gune kadarki TUM veriyi
 * cihaza yazar, ayarlar izin veriyorsa ayrica ayni Gmail hesabina e-posta
 * gonderir. Basarisizlikta hem Ayarlar'daki durum metnine hem de bir bildirime
 * yansitilir (kullaniciyla netlesen "ikisi de" karari).
 */
class DailyBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
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
            settingsRepo.update(cfg.copy(lastBackupStatus = "error: dosya yazilamadi (${t.message})"))
            return Result.retry()
        }
        val scheduleFile = try {
            ExportHelper.writeScheduleJson(applicationContext, slots)
        } catch (t: Throwable) {
            null // takvim verisi olmadan da yedekleme/e-posta devam edebilir.
        }
        val attachments = listOfNotNull(csvFile, scheduleFile)

        // 2) E-posta, sadece acik ve dolu ayarlanmissa.
        if (cfg.dailyBackupEnabled && cfg.backupEmail.isNotBlank() && cfg.backupEmailAppPassword.isNotBlank()) {
            when (val sendResult = SmtpBackupSender.send(cfg.backupEmail, cfg.backupEmailAppPassword, attachments)) {
                is SmtpBackupSender.Result.Success -> {
                    settingsRepo.update(
                        cfg.copy(lastBackupTimestamp = System.currentTimeMillis(), lastBackupStatus = "ok"),
                    )
                    return Result.success()
                }
                is SmtpBackupSender.Result.TransientFailure -> {
                    settingsRepo.update(cfg.copy(lastBackupStatus = "error: gonderim basarisiz, tekrar denenecek"))
                    notifyFailure(notificationHelper, "Gunluk yedekleme e-postasi gonderilemedi, tekrar denenecek.")
                    return Result.retry()
                }
                is SmtpBackupSender.Result.PermanentFailure -> {
                    settingsRepo.update(cfg.copy(lastBackupStatus = "error: ${sendResult.message}"))
                    notifyFailure(notificationHelper, "Gunluk yedekleme e-postasi gonderilemedi: ${sendResult.message}")
                    return Result.failure()
                }
            }
        }

        // E-posta kapali/eksik - sadece cihaza yedekleme basarili sayilir.
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
