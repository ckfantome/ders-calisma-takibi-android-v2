package com.derscalismatakibi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derscalismatakibi.app.core.StudyEngine
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

        val base = StudyEngine.currentConfig()
        // Eski hatali davranista, ikinci cagri BIRINCI cagrinin henuz DataStore'a
        // yazilmamis backupEmail degisikligini goremiyordu (ayni eski `base`den
        // .copy() yapiyordu) - simdi ise her `updateConfig()` cfg'yi SENKRON
        // guncelledigi icin ikinci cagri, birinci cagrinin degisikligini hemen gorur.
        StudyEngine.updateConfig(base.copy(backupEmail = "parent@example.com"))
        StudyEngine.updateConfig(StudyEngine.currentConfig().copy(backupEmailAppPassword = "app-password-1234"))

        val result = StudyEngine.currentConfig()
        assertEquals("parent@example.com", result.backupEmail)
        assertEquals("app-password-1234", result.backupEmailAppPassword)
    }
}
