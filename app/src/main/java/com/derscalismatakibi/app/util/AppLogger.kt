package com.derscalismatakibi.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Merkezi, kalici log sistemi. "Beta" kullanim icin: sadece hatalar degil,
 * onemli TUM olaylar (servis yasam dongusu, durum degisimleri, kullanici
 * aksiyonlari, arkaplan islerinin sonuclari) buraya yazilir. Kullanici bir
 * sorun yasadiginda Loglar ekranindan "Paylas"a basip dosyayi kendine (sonra
 * bana) gonderebilir - bildirim metnini elle okuyup yazmasina gerek kalmaz.
 *
 * Hem bellekte (UI'in canli gozlemleyebilmesi icin StateFlow) hem diskte
 * (uygulama/servis yeniden baslasa da kaybolmasin diye) tutulur.
 */
object AppLogger {
    private const val MAX_MEMORY_LINES = 1000
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024 // 5MB - asilirsa en eski yari atilir.
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    @Volatile private var logFile: File? = null

    @Synchronized
    fun init(context: Context) {
        if (logFile != null) return
        val dir = File(context.applicationContext.getExternalFilesDir(null), "logs").apply { mkdirs() }
        logFile = File(dir, "debug_log.txt")
        log("AppLogger", "Log sistemi baslatildi")
    }

    fun log(tag: String, message: String) {
        val line = "${timeFmt.format(Date())} [$tag] $message"
        android.util.Log.d(tag, message)
        _logs.value = (_logs.value + line).let { if (it.size > MAX_MEMORY_LINES) it.takeLast(MAX_MEMORY_LINES) else it }
        ioScope.launch { appendToFile(line) }
    }

    fun logError(tag: String, message: String, t: Throwable? = null) {
        val suffix = t?.let { " - ${it::class.simpleName}: ${it.message}" } ?: ""
        val line = "${timeFmt.format(Date())} [$tag] HATA: $message$suffix"
        android.util.Log.w(tag, message, t)
        _logs.value = (_logs.value + line).let { if (it.size > MAX_MEMORY_LINES) it.takeLast(MAX_MEMORY_LINES) else it }
        ioScope.launch { appendToFile(line) }
    }

    fun clear() {
        _logs.value = emptyList()
        val file = logFile ?: return
        ioScope.launch {
            try {
                file.writeText("")
            } catch (_: Throwable) {
                // Temizleme basarisiz olsa bile uygulamayi bozmasin.
            }
        }
    }

    /** Paylasim ekrani icin FileProvider Uri'sina saran hazir bir Intent. */
    fun buildShareIntent(context: Context): Intent? {
        val file = logFile ?: return null
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Synchronized
    private fun appendToFile(line: String) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_FILE_BYTES) {
                val existing = file.readLines()
                file.writeText(existing.takeLast(existing.size / 2).joinToString("\n") + "\n")
            }
            file.appendText("$line\n")
        } catch (_: Throwable) {
            // Diske yazma basarisiz olsa bile bellekteki log/uygulama akisi bozulmasin.
        }
    }
}
