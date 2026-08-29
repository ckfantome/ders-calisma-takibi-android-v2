package com.derscalismatakibi.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.derscalismatakibi.app.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * study_tracker2.py -> class NotificationCenter (pygame beep + plyer masaustu
 * bildirimi) karsiligi: Android sistem bildirimi (NotificationCompat) + kisa
 * bir bip sesi (ToneGenerator - ekstra izin/kaynak dosyasi gerektirmez).
 */
class NotificationHelper(private val context: Context) {
    private val channelId = "study_tracker_channel"
    private val idCounter = AtomicInteger(1000)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_description) }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notify(title: String, message: String, notificationsEnabled: Boolean) {
        if (!notificationsEnabled) return
        // POST_NOTIFICATIONS (Android 13+) reddedilmis olabilir - bu durumda
        // NotificationManagerCompat.notify() sessizce SecurityException atar,
        // yakalayip yok sayiyoruz (uygulama akisini bozmamali).
        try {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(idCounter.incrementAndGet(), notification)
        } catch (_: SecurityException) {
            // Bildirim izni yok - sessizce gec.
        }
    }

    fun beep(soundEnabled: Boolean) {
        if (!soundEnabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 300)
        } catch (_: RuntimeException) {
            // Ses cihazi mesgul/yok - sessizce gec.
        }
    }
}
