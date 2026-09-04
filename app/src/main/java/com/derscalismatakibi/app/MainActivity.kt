package com.derscalismatakibi.app

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.ui.AppNavigation
import com.derscalismatakibi.app.ui.theme.DersCalismaTakibiTheme
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: StudyViewModel

    /** Uygulama ici dil secimi (Ayarlar > Dil) API seviyesinden bagimsiz calissin diye
     * Configuration'i burada elle yaminlaniyoruz - AppCompatDelegate/LocaleManager'a
     * (33+ ile sinirli, sadece sistem dilini degistiriyordu) gerek kalmadan tum ekran
     * metinleri (strings.xml) bu Context'in Locale'ini kullanir. Dil DataStore'da
     * (asenkron) tutuldugu icin burada SettingsRepository.LocalePrefs (duz
     * SharedPreferences, senkron) ile okunuyor. */
    override fun attachBaseContext(newBase: Context) {
        val language = SettingsRepository.LocalePrefs.read(newBase)
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log("MainActivity", "onCreate")

        // OpenCV yuklemesi artik StudyEngine.init() icinde (bkz. o dosyadaki not) -
        // BURADA DEGIL, cunku sadece burada olursa Arkaplan Servisi bu Activity hic
        // calismadan (orn. sistem sureci yeniden baslatinca) baslarsa native
        // kutuphane hic yuklenmemis oluyordu (UnsatisfiedLinkError, gercek cihazda
        // dogrulanan bir hata - bkz. StudyEngine.kt).
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[StudyViewModel::class.java]

        setContent {
            val cfg by viewModel.configState.collectAsState()
            // study_tracker2.py -> THEME_MODE ("dark"/"light"/"system"): manuel gecis, sistem
            // varsayilanini ezer.
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (cfg.themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            // Dil degisince Configuration attachBaseContext()'te uygulaniyor - yeni
            // dilin strings.xml kaynaklarinin devreye girmesi icin Activity'nin
            // yeniden olusturulmasi (recreate) gerekiyor. Ilk composition'da
            // (remember ile) tetiklenmemesi icin degisiklik kontrolu yapiliyor.
            val initialLanguage = remember { cfg.appLanguage }
            LaunchedEffect(cfg.appLanguage) {
                if (cfg.appLanguage != initialLanguage) {
                    recreate()
                }
            }
            // Tam Gizli Mod: baslatici simgesini tasiyan MainActivityAlias'in
            // component-enabled durumu, ayar degistikce burada senkronize edilir.
            // Idempotent oldugu icin her recomposition/cold-start'ta ayni degeri
            // yeniden uygulamak zararsiz - hatta faydali (onResume()'daki yeniden
            // uygulamayla birlikte, olasi bir surec-olumu sonrasi drift'e karsi
            // kendi kendini onarir).
            LaunchedEffect(cfg.fullyHiddenModeEnabled) {
                applyFullyHiddenMode(cfg.fullyHiddenModeEnabled)
            }
            DersCalismaTakibiTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Compose LaunchedEffect ilk karede henuz tetiklenmemis olabilir (orn.
        // gorev bildirimden yeniden acildiginda) - burada zaten kalici olan
        // ayari senkron olarak yeniden uyguluyoruz, yedek/onarici katman.
        if (::viewModel.isInitialized) {
            applyFullyHiddenMode(com.derscalismatakibi.app.core.StudyEngine.currentConfig().fullyHiddenModeEnabled)
        }
    }

    /** Tam Gizli Mod acikken baslatici simgesini tasiyan alias'i devre disi
     * birakir ve gorevi Son Kullanilanlar'dan haric tutar; kapaliyken ikisini
     * de geri geri acar. MainActivity'nin kendisi (ve ona giden PendingIntent'ler,
     * orn. takip bildirimi) bundan etkilenmez - sadece alias'in enabled durumu
     * degisir. */
    private fun applyFullyHiddenMode(hidden: Boolean) {
        val aliasComponent = ComponentName(this, "com.derscalismatakibi.app.MainActivityAlias")
        val newState = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        runCatching {
            packageManager.setComponentEnabledSetting(aliasComponent, newState, PackageManager.DONT_KILL_APP)
        }
        runCatching {
            val am = getSystemService(ActivityManager::class.java)
            am?.appTasks?.firstOrNull()?.setExcludeFromRecents(hidden)
        }.onFailure {
            AppLogger.logError("TamGizliMod", "setExcludeFromRecents basarisiz", it)
        }
    }

    override fun onStop() {
        super.onStop()
        AppLogger.log("MainActivity", "onStop (arkaplan servisi aktif: ${com.derscalismatakibi.app.core.StudyEngine.backgroundTrackingActive.value})")
        // study_tracker2.py -> MainWindow.closeEvent(): uygulama arka plana
        // atildiginda mevcut oturumu (bossa kaydetmeden) kaydeder. ANCAK
        // Arkaplan Takip Servisi calisiyorsa oturumu SONLANDIRMIYORUZ - takip
        // orada devam ediyor, StudyForegroundService.onDestroy() kendi
        // finalize'ini yapacak.
        if (::viewModel.isInitialized && !com.derscalismatakibi.app.core.StudyEngine.backgroundTrackingActive.value) {
            viewModel.finalizeSessionIfNeeded()
        }
    }
}
