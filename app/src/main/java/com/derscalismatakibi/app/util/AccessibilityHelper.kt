package com.derscalismatakibi.app.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.derscalismatakibi.app.service.AppBlockAccessibilityService

/** AppBlockScreen ve StudyEngine (bekci) tarafindan ortak kullanilir - Erisilebilirlik
 * Servisi'nin sistemde etkin olup olmadigini kontrol eder. */
object AccessibilityHelper {
    fun isAppBlockServiceEnabled(context: Context): Boolean {
        // ENABLED_ACCESSIBILITY_SERVICES tam ComponentName.flattenToString() tutar
        // (paket/tam.sinif.Adi) - manifestteki "." kisayolu burda GECERSIZ, hep false donuyordu.
        val expected = ComponentName(context, AppBlockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
