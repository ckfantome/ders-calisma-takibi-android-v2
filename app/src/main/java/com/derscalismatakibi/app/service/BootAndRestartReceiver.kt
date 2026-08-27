package com.derscalismatakibi.app.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.util.AppLogger

/** Otomatik Baslatma (reboot sonrasi) + Surekli Acik Kalma (uygulama Son
 * Kullanilanlar'dan kaldirilinca StudyForegroundService.onTaskRemoved'in
 * AlarmManager ile gecikmeli tetikledigi yeniden baslatma) icin ortak alici. */
class BootAndRestartReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_RESTART_SERVICE = "com.derscalismatakibi.app.action.RESTART_SERVICE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        AppLogger.log("OtoBaslat", "onReceive action=$action")
        StudyEngine.init(context.applicationContext)
        val cfg = StudyEngine.currentConfig()
        val allowed = when (action) {
            Intent.ACTION_BOOT_COMPLETED -> cfg.autoStartOnBootEnabled
            ACTION_RESTART_SERVICE -> cfg.keepAliveEnabled
            else -> false
        }
        if (!allowed) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.log("OtoBaslat", "Kamera izni yok - baslatilamadi")
            return
        }
        try {
            ContextCompat.startForegroundService(context, StudyForegroundService.startIntent(context))
            AppLogger.log("OtoBaslat", "Servis baslatildi ($action)")
        } catch (t: Throwable) {
            AppLogger.logError("OtoBaslat", "Servis baslatilamadi", t)
        }
    }
}
