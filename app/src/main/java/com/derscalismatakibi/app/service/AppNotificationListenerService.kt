package com.derscalismatakibi.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.util.AppLogger

/** Ebeveyn-denetim: diger uygulamalardan gelen bildirimleri Loglar'a yazar.
 * Ozel izin (BIND_NOTIFICATION_LISTENER_SERVICE) - kullanici Ayarlar > Bildirim
 * erisimi'nden acikca vermeli (bkz. ui/screens/SettingsScreen.kt'deki buton). */
class AppNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == applicationContext.packageName) return
        StudyEngine.init(applicationContext)
        if (!StudyEngine.currentConfig().notificationLogEnabled) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        AppLogger.log("Bildirim", "$pkg: $title - $text")
    }
}
