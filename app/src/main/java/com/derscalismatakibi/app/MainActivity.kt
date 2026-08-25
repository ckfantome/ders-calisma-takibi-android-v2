package com.derscalismatakibi.app

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.derscalismatakibi.app.ui.AppNavigation
import com.derscalismatakibi.app.ui.theme.DersCalismaTakibiTheme
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: StudyViewModel

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
            // ponytail: API 33 altinda per-app dil degistirme AppCompatDelegate (yeni
            // bagimlilik) gerektirir - eklenmedi, sadece 33+ native LocaleManager kullanildi.
            LaunchedEffect(cfg.appLanguage) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getSystemService(LocaleManager::class.java)?.applicationLocales =
                        LocaleList.forLanguageTags(cfg.appLanguage)
                }
            }
            DersCalismaTakibiTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
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
