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

    /** ENABLED_ACCESSIBILITY_SERVICES sadece OS'in "izni verilmis" kaydini yansitir -
     * reboot sonrasi bazi OEM'lerde (MIUI/ColorOS/EMUI/Vivo) servis listede
     * "etkin" gorunse bile OS tarafindan gercekten baglanip calistirilmamis
     * olabilir. Bu yuzden AppBlockAccessibilityService her event aldiginda
     * (ve baglandiginda) buraya bir "hala canliyim" zaman damgasi yazar; DataStore
     * degil duz SharedPreferences kullanilir ki StudyEngine'in 30sn'lik bekci
     * dongusu bunu senkron/hizli okuyabilsin (bkz. SettingsRepository.LocalePrefs
     * ayni desen icin). */
    private const val HEARTBEAT_PREFS_NAME = "accessibility_heartbeat"
    private const val KEY_LAST_HEARTBEAT = "last_heartbeat_ms"

    fun recordHeartbeat(context: Context) {
        context.getSharedPreferences(HEARTBEAT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_HEARTBEAT, System.currentTimeMillis()).apply()
    }

    fun lastHeartbeat(context: Context): Long =
        context.getSharedPreferences(HEARTBEAT_PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_HEARTBEAT, 0L)
}
