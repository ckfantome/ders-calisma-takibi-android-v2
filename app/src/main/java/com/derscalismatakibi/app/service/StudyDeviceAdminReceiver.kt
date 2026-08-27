package com.derscalismatakibi.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.derscalismatakibi.app.util.AppLogger

/** Cihaz Yoneticisi (Device Admin) - aktifken Android, kullanicinin uygulamayi
 * once Ayarlar > Cihaz Yoneticileri'nden bu izni kapatmadan kaldiramamasini
 * otomatik olarak saglar (ozel bir politika tanimlamamiza gerek yok). Ayni
 * receiver, ileride Device Owner'a (adb ile) yukseltilirse de kullanilir. */
class StudyDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        AppLogger.log("CihazYoneticisi", "Etkinlestirildi")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        AppLogger.log("CihazYoneticisi", "Devre disi birakildi")
    }
}
