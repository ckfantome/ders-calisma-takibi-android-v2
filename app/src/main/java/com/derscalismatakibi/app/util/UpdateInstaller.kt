package com.derscalismatakibi.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.derscalismatakibi.app.core.UpdateChecker
import java.io.File

/**
 * GitHub Release'ten indirilen APK'yi DownloadManager ile indirip kurulum
 * ekranini acar. study_tracker2.py'de karsiligi yok - sideload guncelleme
 * akisi Android'e ozgu. ExportHelper.kt ile ayni FileProvider desenini kullanir.
 */
object UpdateInstaller {
    private const val FILE_NAME = "guncelleme.apk"

    /** Android 8+'ta "bilinmeyen kaynaklardan yukleme" izni normal izin kutusuyla ISTENEMEZ. */
    fun canInstallUnknownApps(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun unknownAppsSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Indirmeyi baslatir; indirme bitince sistem kurulum ekranini otomatik acar. */
    fun downloadAndInstall(context: Context, info: UpdateChecker.UpdateInfo) {
        AppLogger.log("Guncelleme", "Indirme baslatildi: v${info.version}")
        val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        if (targetFile.exists()) targetFile.delete()

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("Ders Calisma Takibi guncellemesi")
            .setDescription("v${info.version} indiriliyor")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val appContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                appContext.unregisterReceiver(this)
                AppLogger.log("Guncelleme", "Indirme tamamlandi, kurulum ekrani aciliyor")
                val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", targetFile)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                appContext.startActivity(installIntent)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }
}
