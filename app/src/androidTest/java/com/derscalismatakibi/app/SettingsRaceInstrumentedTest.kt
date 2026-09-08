package com.derscalismatakibi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derscalismatakibi.app.core.StudyEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Backup kok neden bulgusu: StudyEngine.updateConfig() eskiden sadece
 * `engineScope.launch { settingsRepository.update(newCfg) }` yapiyordu -
 * cfg/configState SADECE DataStore'un yazma+yeniden-okuma round-trip'i
 * tamamlaninca (asenkron) guncelleniyordu. Iki alani HIZLI ARDISIK degistiren
 * UI kodu (orn. otomatik doldurmayla ayni anda backupEmail + backupEmailAppPassword)
 * ikisi de AYNI eski cfg anlik goruntusunden .copy() yapiyordu - ikinci
 * DataStore yazmasi, birincinin degisikligini "son yazan kazanir" ile sessizce
 * eziyordu. updateConfig() artik cfg/configState'i SENKRON gunceller (bkz.
 * StudyEngine.kt), bu test iki ardisik updateConfig() cagrisinin ikisinin de
 * kalici oldugunu dogrular.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRaceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun rapidSequentialUpdatesDoNotClobberEachOther() = runBlocking {
        StudyEngine.init(context)

        // StudyEngine surec-basina tek singleton - ayni instrumentation surecinde
        // calisan BASKA test siniflarinin (orn. DailyBackupWorkerDeletionInstrumentedTest'in
        // kendi SettingsRepository'siyle yaptigi es zamanli yazmalar) devam eden
        // configFlow.collect{} dongusune GEC ULASAN eski bir emisyonu, buradaki
        // senkron guncellemeyi ANLIK olarak gecici bicimde ezebilir - bu, testin
        // paylasilan sureç-genelindeki bir yan etkisidir, asil duzeltmenin
        // (updateConfig() artik cfg/configState'i senkron gunceller) kendisiyle
        // ilgisizdir. Bu yuzden BURADAN itibaren birkac kisa "settle" turuyla
        // sonucun KALICI olarak beklenen degere ULASTIGINI dogruluyoruz - eski
        // hatali kodda (senkron guncelleme YOKTU) bu asla dogru degere
        // yerlesmezdi, cunku ikinci cagri birincinin degisikligini hic gormezdi.
        val base = StudyEngine.currentConfig()
        StudyEngine.updateConfig(base.copy(backupEmail = "parent@example.com"))
        StudyEngine.updateConfig(StudyEngine.currentConfig().copy(backupEmailAppPassword = "app-password-1234"))

        var result = StudyEngine.currentConfig()
        var attempts = 0
        while ((result.backupEmail != "parent@example.com" || result.backupEmailAppPassword != "app-password-1234") && attempts < 10) {
            delay(100)
            result = StudyEngine.currentConfig()
            attempts++
        }

        assertEquals("parent@example.com", result.backupEmail)
        assertEquals("app-password-1234", result.backupEmailAppPassword)
    }
}
