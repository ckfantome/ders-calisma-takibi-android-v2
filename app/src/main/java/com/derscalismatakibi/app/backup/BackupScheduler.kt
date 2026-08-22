package com.derscalismatakibi.app.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.derscalismatakibi.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Gunluk yedekleme/e-posta gorevinin WorkManager zamanlamasi. Kesin bir saat
 * garantisi VERMEZ (Doze/pil durumuna gore kayabilir) - "gun sonunda" gereksinimi
 * icin yeterli hassasiyette. AlarmManager+exact-time yerine WorkManager secildi
 * cunku reboot/reinstall sonrasi kendi kendine ayaga kalkar, ekstra BOOT_COMPLETED
 * receiver'i gerekmez.
 */
object BackupScheduler {
    private const val UNIQUE_WORK_NAME = "daily_backup"
    private const val UNIQUE_WORK_NAME_MANUAL = "daily_backup_manual"

    /** Uygulama surec basina bir kez (StudyTrackerApp.onCreate) cagrilir. Ayarlarda
     * kayitli saat/dakikayi DataStore'dan okuyup KEEP politikasiyle kurar - zaten
     * kurulu bir periyodik is varsa DOKUNMAZ (idempotent). */
    fun scheduleDailyBackup(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val cfg = SettingsRepository(appContext).configFlow.first()
            enqueue(appContext, cfg.backupHour, cfg.backupMinute, ExistingPeriodicWorkPolicy.KEEP)
        }
    }

    /** Ayarlar ekranindan saat degistirildiginde cagrilir - UPDATE politikasiyle
     * mevcut periyodik isi iptal etmeden yeni parametrelerle gunceller. */
    fun reschedule(context: Context, hour: Int, minute: Int) {
        enqueue(context.applicationContext, hour, minute, ExistingPeriodicWorkPolicy.UPDATE)
    }

    /** "Simdi Yedekle" test butonu - bir gun beklemeden ayarlari dogrulamak icin. */
    fun scheduleOneOffNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyBackupWorker>().build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME_MANUAL, ExistingWorkPolicy.REPLACE, request)
    }

    private fun enqueue(context: Context, hour: Int, minute: Int, policy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request)
    }

    private fun initialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_MONTH, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
