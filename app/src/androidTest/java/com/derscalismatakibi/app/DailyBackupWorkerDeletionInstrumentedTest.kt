package com.derscalismatakibi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.derscalismatakibi.app.backup.BackupScheduler
import com.derscalismatakibi.app.backup.DailyBackupWorker
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.KeystrokeLogEntity
import com.derscalismatakibi.app.data.LocationLogEntity
import com.derscalismatakibi.app.data.SessionEntity
import com.derscalismatakibi.app.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug 8/D: "yedeklemeden sonra sil" mantigi araliklarla/incremental
 * yedeklemede de tetikleniyordu, oysa sadece GERCEK gunluk yedekleme
 * basarisinda tetiklenmeliydi. DailyBackupWorker.clearRawLogsAfterSuccess()
 * artik uc cagri noktasinda da (!isIntervalTrigger) sarti tasiyor - burada
 * gercek WorkManager test altyapisiyla (TestListenableWorkerBuilder) her iki
 * yolu da calistirip location_logs/keystroke_logs tablolarinin dogru
 * korundugunu/temizlendigini dogruluyoruz.
 *
 * E-posta agi cagrisi tetiklenmesin diye backupEmail/backupEmailAppPassword
 * bos birakiliyor - worker "sadece cihaza yaz" (device-only) yolundan gecer.
 *
 * Not: SessionDao'da delete metodu yok (mevcut tasarim) - test surecinde
 * eklenen ornek session satiri temizlenmiyor, bu tek-kullanimlik emulator
 * (test_avd) icin zararsiz.
 */
@RunWith(AndroidJUnit4::class)
class DailyBackupWorkerDeletionInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = AppDatabase.getInstance(context)

    @Before
    fun seed() {
        runBlocking {
        SettingsRepository(context).let { repo ->
            val cfg = repo.configFlow.first()
            repo.update(
                cfg.copy(
                    backupEmail = "",
                    backupEmailAppPassword = "",
                    dailyBackupEnabled = true,
                    lastBackupTimestamp = 0L,
                ),
            )
        }
        // hasNewData kontrolunun (interval-trigger yolunda) her zaman true
        // olmasini saglar - lastBackupTimestamp=0 yukarida ayarlandi.
        db.sessionDao().insert(
            SessionEntity(
                date = "2026-01-01",
                startTime = "10:00:00",
                endTime = "10:30:00",
                studyingSeconds = 1800.0,
                awaySeconds = 0.0,
                sleepingSeconds = 0.0,
                totalSeconds = 1800.0,
            ),
        )
        db.locationLogDao().insert(LocationLogEntity(lat = 41.0, lng = 29.0, timestamp = System.currentTimeMillis()))
        db.keystrokeLogDao().insert(
            KeystrokeLogEntity(packageName = "com.example.app", appLabel = "Example", text = "test", timestamp = System.currentTimeMillis()),
        )
        }
    }

    @Test
    fun intervalTriggerDoesNotClearRawLogs() = runBlocking {
        val worker = TestListenableWorkerBuilder<DailyBackupWorker>(context)
            .setInputData(workDataOf(BackupScheduler.WORK_DATA_KEY_IS_INTERVAL_TRIGGER to true))
            .build()

        worker.doWork()

        assertTrue("location log should survive an interval-trigger backup", db.locationLogDao().all().isNotEmpty())
        assertTrue("keystroke log should survive an interval-trigger backup", db.keystrokeLogDao().observeRecent().first().isNotEmpty())
    }

    @Test
    fun dailyTriggerClearsRawLogsAfterSuccess() = runBlocking {
        val worker = TestListenableWorkerBuilder<DailyBackupWorker>(context).build()

        worker.doWork()

        assertTrue("location log must be cleared after a real daily backup success", db.locationLogDao().all().isEmpty())
        assertTrue("keystroke log must be cleared after a real daily backup success", db.keystrokeLogDao().observeRecent().first().isEmpty())
    }
}
