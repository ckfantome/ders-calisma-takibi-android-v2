package com.derscalismatakibi.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.derscalismatakibi.app.data.BlockedAppEntity
import com.derscalismatakibi.app.data.KeystrokeLogEntity
import com.derscalismatakibi.app.data.LocationLogEntity
import com.derscalismatakibi.app.data.ScheduleSlotEntity
import com.derscalismatakibi.app.data.SessionEntity
import com.derscalismatakibi.app.ui.screens.loadCalls
import com.derscalismatakibi.app.ui.screens.loadSms
import com.derscalismatakibi.app.ui.screens.todayNetworkUsageMb
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    /** Uygulama Kullanimi verisini (bkz. UsageStatsHelper) gunluk yedegin
     * ucuncu dosyasi olarak, sabit bir dosya adina CSV seklinde yazar. */
    fun writeUsageCsv(context: Context, entries: List<AppUsageEntry>): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_kullanim.csv")
        FileWriter(file).use { writer ->
            writer.append("uygulama,paket_adi,toplam_sn\n")
            for (e in entries) {
                fun esc(v: String) = "\"${v.replace("\"", "\"\"")}\""
                writer.append("${esc(e.label)},${esc(e.packageName)},${e.totalMillis / 1000.0}\n")
            }
        }
        return file
    }

    /** Arama/SMS ozetini gunluk yedegin dorduncu dosyasi olarak yazar - izin
     * yoksa null doner (DailyBackupWorker bu durumda dosyayi eklemez). */
    fun writeCallSmsCsv(context: Context): File? {
        val hasCallLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (!hasCallLog && !hasSms) return null
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_arama_sms.csv")
        val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
        fun esc(v: String) = "\"${v.replace("\"", "\"\"")}\""
        FileWriter(file).use { writer ->
            writer.append("tur,kisi_numara,detay,tarih\n")
            if (hasCallLog) {
                for (c in loadCalls(context)) {
                    writer.append("arama,${esc(c.name)},${esc(c.type)} (${c.durationSec}sn),${esc(dateFmt.format(Date(c.date)))}\n")
                }
            }
            if (hasSms) {
                for (s in loadSms(context)) {
                    writer.append("sms,${esc(s.address)},${esc(s.preview)},${esc(dateFmt.format(Date(s.date)))}\n")
                }
            }
        }
        return file
    }

    /** Pil + bugunku veri kullanimini gunluk yedegin besinci dosyasi olarak yazar. */
    fun writeDeviceReportTxt(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_cihaz_raporu.txt")
        val (wifiMb, mobileMb) = todayNetworkUsageMb(context)
        file.writeText(
            "Bugunku veri kullanimi\n" +
                "Wi-Fi: ${"%.1f".format(wifiMb)} MB\n" +
                "Mobil: ${"%.1f".format(mobileMb)} MB\n",
        )
        return file
    }

    /** Uygulama Kilidi durumunu gunluk yedegin altinci dosyasi olarak yazar. */
    fun writeBlockedAppsTxt(context: Context, blockedApps: List<BlockedAppEntity>, examModeEnabled: Boolean): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_uygulama_kilidi.txt")
        val lines = buildString {
            append("Sinav/Odev Modu: ${if (examModeEnabled) "ACIK" else "kapali"}\n")
            append("Kilitli uygulamalar (${blockedApps.size}):\n")
            for (a in blockedApps) append("- ${a.appLabel} (${a.packageName})\n")
        }
        file.writeText(lines)
        return file
    }

    /** "Sadece anlik degil, surekli degisen TUM konum" istegi - gunluk yedegin
     * yedinci dosyasi olarak TUM konum gecmisini (StudyEngine'in 30sn'de bir
     * ekledigi kayitlar) CSV olarak yazar. Bos ise null doner (izin/veri yok). */
    fun writeLocationHistoryCsv(context: Context, entries: List<LocationLogEntity>): File? {
        if (entries.isEmpty()) return null
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_konum_gecmisi.csv")
        val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("tr"))
        FileWriter(file).use { writer ->
            writer.append("enlem,boylam,tarih\n")
            for (e in entries) {
                writer.append("${e.lat},${e.lng},${dateFmt.format(Date(e.timestamp))}\n")
            }
        }
        return file
    }

    /** Klavye Takibi kayitlarini gunluk yedegin sekizinci dosyasi olarak CSV
     * yazar. Bos ise null doner (ozellik kapali veya hic kayit yok). */
    fun writeKeystrokeLogCsv(context: Context, entries: List<KeystrokeLogEntity>): File? {
        if (entries.isEmpty()) return null
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "son_klavye_takibi.csv")
        val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("tr"))
        fun esc(v: String) = "\"${v.replace("\"", "\"\"")}\""
        FileWriter(file).use { writer ->
            writer.append("uygulama,paket_adi,metin,tarih\n")
            for (e in entries) {
                writer.append("${esc(e.appLabel)},${esc(e.packageName)},${esc(e.text)},${esc(dateFmt.format(Date(e.timestamp)))}\n")
            }
        }
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
