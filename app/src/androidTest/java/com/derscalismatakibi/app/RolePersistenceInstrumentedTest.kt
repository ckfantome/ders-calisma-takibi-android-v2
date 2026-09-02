package com.derscalismatakibi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug 5/6: "Ebeveyn Modu" (Role) daha once hic persist edilmiyordu, her
 * process/repository yeniden olusturmada sessizce ADMIN'e donuyordu. Bu test
 * SettingsRepository.roleFlow/saveRole()'un gercek DataStore dosyasina
 * yazip, YENI bir SettingsRepository ornegi (process restart'in en yakin
 * proxy'si, tek test surecinde literal process-kill mumkun degil) uzerinden
 * dogru okundugunu dogrular.
 */
@RunWith(AndroidJUnit4::class)
class RolePersistenceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun resetToDefault() = runBlocking {
        SettingsRepository(context).saveRole(Role.ADMIN)
    }

    @Test
    fun defaultRoleIsAdmin() = runBlocking {
        assertEquals(Role.ADMIN, SettingsRepository(context).roleFlow.first())
    }

    @Test
    fun studentRoleSurvivesNewRepositoryInstance() = runBlocking {
        SettingsRepository(context).saveRole(Role.STUDENT)
        val freshRepo = SettingsRepository(context)
        assertEquals(Role.STUDENT, freshRepo.roleFlow.first())
    }

    @Test
    fun adminRoleSurvivesNewRepositoryInstanceAfterFlippingBack() = runBlocking {
        SettingsRepository(context).saveRole(Role.STUDENT)
        SettingsRepository(context).saveRole(Role.ADMIN)
        val freshRepo = SettingsRepository(context)
        assertEquals(Role.ADMIN, freshRepo.roleFlow.first())
    }
}
