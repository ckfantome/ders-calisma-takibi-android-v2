package com.derscalismatakibi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derscalismatakibi.app.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug 4/A: "Arka plan takip" varsayilan ACIK olmali, ama kullanicinin AC/KAPALI
 * secimi process restart/reboot/update'te ezilmemeli. Kod incelemesi
 * (SettingsRepository.configFlow, elvis-operator ile AppConfig() varsayilanina
 * dusme) yapiyi zaten dogru buldu - burada gercek DataStore dosyasina yazip
 * YENI bir SettingsRepository ornegiyle (process restart'in proxy'si) okuma
 * regresyona karsi kilitleniyor. (Gercekten bos/hic-yazilmamis bir DataStore =
 * "ilk kurulum" durumu, DataStore'un surec-basina tek instance kurallari
 * yuzunden ayni test surecinde guvenle yeniden yaratilamiyor - varsayilan=true
 * davranisi zaten AppConfig() varsayilanindan yapisal olarak garanti, bkz.
 * SettingsRepository.kt configFlow.)
 */
@RunWith(AndroidJUnit4::class)
class BackgroundTrackingPersistenceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun explicitOffSurvivesNewRepositoryInstance() = runBlocking {
        val repo = SettingsRepository(context)
        val cfg = repo.configFlow.first()
        repo.update(cfg.copy(autoStartOnBootEnabled = false))

        val freshRepo = SettingsRepository(context)
        assertFalse(freshRepo.configFlow.first().autoStartOnBootEnabled)
    }

    @Test
    fun explicitOnSurvivesNewRepositoryInstance() = runBlocking {
        val repo = SettingsRepository(context)
        val cfg = repo.configFlow.first()
        repo.update(cfg.copy(autoStartOnBootEnabled = true))

        val freshRepo = SettingsRepository(context)
        assertTrue(freshRepo.configFlow.first().autoStartOnBootEnabled)
    }
}
