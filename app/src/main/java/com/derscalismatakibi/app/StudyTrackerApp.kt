package com.derscalismatakibi.app

import android.app.Application
import com.derscalismatakibi.app.backup.BackupScheduler

/**
 * Sadece surec basina BIR KEZ, Activity'nin hic acilmasina bagli olmadan
 * calisan bir tetikleyici noktasi lazim ("tam otomatik, sen dokunmadan"
 * gereksinimi icin) - gunluk yedekleme WorkManager gorevini burada zamanliyoruz.
 * ExistingPeriodicWorkPolicy.KEEP kullanildigi icin tekrar tekrar cagrilmasi
 * (orn. process restart) zararsizdir.
 */
class StudyTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BackupScheduler.scheduleDailyBackup(this)
    }
}
