package com.derscalismatakibi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.derscalismatakibi.app.ui.AppNavigation
import com.derscalismatakibi.app.ui.theme.DersCalismaTakibiTheme
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: StudyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OpenCV'nin (solvePnP - kafa pozu hesaplamasi icin) native kutuphanesini
        // yukler. NOT: org.opencv:opencv Maven Central AAR'inin surumune gore bu
        // cagriya HIC gerek kalmayabilir de (bazi surumler otomatik yukleniyor) -
        // eger FrameAnalyzer.kt'deki estimateHeadPose() calisirken
        // UnsatisfiedLinkError alirsan bu, README'deki "Olasi Ilk Kurulum
        // Sorunlari" bolumunde ayrintili anlatilan bilinen bir risk alanidir.
        OpenCVLoader.initDebug()

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
            DersCalismaTakibiTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
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
