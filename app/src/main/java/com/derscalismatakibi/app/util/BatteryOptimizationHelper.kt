package com.derscalismatakibi.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Arkaplan takip servisinin Doze/App Standby veya OEM pil yoneticileri
 * tarafindan durdurulma ihtimalini azaltmak icin kullaniciyi pil optimizasyonu
 * istisnasi vermeye yonlendirir. UpdateInstaller.kt'deki "bilinmeyen kaynaklar"
 * izin akisiyla AYNI desen: normal izin kutusuyla ISTENEMEZ, kullanici Ayarlar
 * ekranindan acikca vermeli.
 */
object BatteryOptimizationHelper {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
}
