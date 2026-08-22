package com.derscalismatakibi.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.derscalismatakibi.app.data.ScheduleSlotEntity
import com.derscalismatakibi.app.data.SessionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

/**
 * study_tracker2.py -> export_to_csv() karsiligi: Room'daki sessions tablosunu
 * uygulamaya-ozel harici depolamaya (izin GEREKTIRMEZ, Android 10+ scoped
 * storage uyumlu) CSV olarak yazar. Dosya-yazma ve paylasim-Intent'i kurma
 * ayri fonksiyonlara bolundu ki DailyBackupWorker (arkaplan, UI/Intent
 * gerektirmez) sadece dosya-yazma kismini yeniden kullanabilsin.
 */
object ExportHelper {
    fun writeSessionsCsv(context: Context, sessions: List<SessionEntity>): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "calisma_verileri_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("tarih,baslangic,bitis,calisma_sn,uzakta_sn,uyku_sn,toplam_sn,konusma_sn,pomodoro_sayisi,notlar,etiketler,verimlilik\n")
            for (s in sessions) {
                writer.append(csvRow(s))
            }
        }
        return file
    }

    /** Gunluk yedekleme icin: her seferinde SABIT bir dosya adina uzerine yazar
     * (veri zaten kumulatif oldugu icin zaman damgali dosyalar biriktirmek
     * depolamanin sinirsiz buyumesine yol acardi). */
    fun writeDailyBackupCsv(context: Context, sessions: List<SessionEntity>): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_yedek.csv")
        FileWriter(file).use { writer ->
            writer.append("tarih,baslangic,bitis,calisma_sn,uzakta_sn,uyku_sn,toplam_sn,konusma_sn,pomodoro_sayisi,notlar,etiketler,verimlilik\n")
            for (s in sessions) {
                writer.append(csvRow(s))
            }
        }
        return file
    }

    /** Takvim Takip (schedule_slots) verisini, gunluk yedegin ikinci dosyasi
     * olarak, sabit bir dosya adina JSON dizisi seklinde yazar. */
    fun writeScheduleJson(context: Context, slots: List<ScheduleSlotEntity>): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_takvim.json")
        val array = JSONArray()
        for (s in slots) {
            array.put(
                JSONObject().apply {
                    put("gun", s.day)
                    put("baslangic", s.startTime)
                    put("bitis", s.endTime)
                    put("tur", s.kind)
                }
            )
        }
        file.writeText(array.toString(2))
        return file
    }

    fun buildShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun exportToCsv(context: Context, sessions: List<SessionEntity>): Intent =
        buildShareIntent(context, writeSessionsCsv(context, sessions))

    private fun csvRow(s: SessionEntity): String {
        fun esc(v: String?): String = "\"${(v ?: "").replace("\"", "\"\"")}\""
        return listOf(
            s.date, s.startTime, s.endTime,
            s.studyingSeconds, s.awaySeconds, s.sleepingSeconds, s.totalSeconds, s.speakingSeconds,
            s.pomodoroCycles, esc(s.notes), esc(s.tags), s.productivityScore ?: 0.0,
        ).joinToString(",") + "\n"
    }
}
