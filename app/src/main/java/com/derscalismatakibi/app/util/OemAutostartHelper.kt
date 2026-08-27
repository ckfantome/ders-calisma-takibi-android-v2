package com.derscalismatakibi.app.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Bazi OEM'ler (Oppo/ColorOS, Xiaomi/MIUI, Huawei/EMUI, Vivo) standart Android
 * izinlerinin disinda ayrica "Otomatik Baslatma" listesine eklenmeyi sart kosar -
 * aksi halde arkaplan servisi/Erisilebilirlik Servisi sessizce oldurulur/kapatilir
 * (Sinav Modu'nun Oppo'da kendiliginden etkisiz kalmasinin bilinen nedeni budur).
 * Bu ekranlar standart bir Android API'siyle DEGIL, her OEM'in kendi (belgelenmemis,
 * surume gore degisebilen) Activity'siyle acilir - hicbiri bulunamazsa false doner,
 * cagiran taraf o zaman genel Uygulama Bilgisi ekranina dusmeli.
 */
object OemAutostartHelper {
    private val candidates = listOf(
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    )

    fun isKnownRestrictiveOem(): Boolean = Build.MANUFACTURER.lowercase().let {
        it.contains("oppo") || it.contains("xiaomi") || it.contains("redmi") || it.contains("huawei") || it.contains("honor") || it.contains("vivo")
    }

    /** Bilinen ekranlardan ilk acilabileni acar. */
    fun openAutostartSettings(context: Context): Boolean {
        for (component in candidates) {
            try {
                context.startActivity(Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
