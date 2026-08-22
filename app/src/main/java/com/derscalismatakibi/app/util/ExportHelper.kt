package com.derscalismatakibi.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.derscalismatakibi.app.data.SessionEntity
import java.io.File
import java.io.FileWriter

/**
 * study_tracker2.py -> export_to_csv() karsiligi: Room'daki sessions tablosunu
 * uygulamaya-ozel harici depolamaya (izin GEREKTIRMEZ, Android 10+ scoped
 * storage uyumlu) CSV olarak yazar, sonra kullanicinin istedigi yere
 * (WhatsApp, Drive, e-posta...) gonderebilmesi icin bir paylasim Intent'i uretir.
 */
object ExportHelper {
    fun exportToCsv(context: Context, sessions: List<SessionEntity>): Intent {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "calisma_verileri_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("tarih,baslangic,bitis,calisma_sn,uzakta_sn,uyku_sn,toplam_sn,konusma_sn,pomodoro_sayisi,notlar,etiketler,verimlilik\n")
            for (s in sessions) {
                writer.append(csvRow(s))
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun csvRow(s: SessionEntity): String {
        fun esc(v: String?): String = "\"${(v ?: "").replace("\"", "\"\"")}\""
        return listOf(
            s.date, s.startTime, s.endTime,
            s.studyingSeconds, s.awaySeconds, s.sleepingSeconds, s.totalSeconds, s.speakingSeconds,
            s.pomodoroCycles, esc(s.notes), esc(s.tags), s.productivityScore ?: 0.0,
        ).joinToString(",") + "\n"
    }
}
