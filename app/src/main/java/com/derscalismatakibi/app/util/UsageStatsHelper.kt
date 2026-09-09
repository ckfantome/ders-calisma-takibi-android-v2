package com.derscalismatakibi.app.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.derscalismatakibi.app.R
import java.util.Calendar

/**
 * ui/screens/UsageStatsScreen.kt'nin Compose'a bagimli olmayan cekirdegi -
 * hem o ekran hem de backup/DailyBackupWorker.kt (Compose'suz, arkaplan
 * gorevi) tarafindan ortak kullanilabilsin diye buraya tasindi. Davranis
 * (bugun 00:00 - simdi araligi, kendi paketini haric tutma, azalan sirali
 * ilk 50 uygulama) AYNEN korunuyor.
 */
data class AppUsageEntry(val label: String, val packageName: String, val totalMillis: Long)
data class AppEventEntry(val label: String, val timestamp: Long, val type: String)
data class AppUsageSession(val label: String, val packageName: String, val startMillis: Long, val endMillis: Long)

object UsageStatsHelper {
    /** Bugun (00:00 - simdi) acilan/kapanan TUM uygulama olaylari, en yeni once.
     * Onceden ilk 100'e kesiliyordu - yogun kullanimda gunun ilerleyen saatlerinde
     * sabahki olaylar hicbir uyari olmadan listeden dusuyordu. Tek gunluk olay
     * sayisi (en yogun kullanicida bile) birkac yuzu gecmez, liste zaten
     * LazyColumn oldugu icin kesmeye gerek yok. */
    fun loadTodayEvents(context: Context): List<AppEventEntry> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val pm = context.packageManager
        val events = usm.queryEvents(cal.timeInMillis, System.currentTimeMillis())
        val result = mutableListOf<AppEventEntry>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> context.getString(R.string.usage_event_opened)
                UsageEvents.Event.MOVE_TO_BACKGROUND -> context.getString(R.string.usage_event_closed)
                else -> continue
            }
            if (event.packageName == context.packageName) continue
            result.add(AppEventEntry(appLabel(pm, event.packageName), event.timeStamp, type))
        }
        return result.asReversed()
    }
    /** Bugunku (00:00 - simdi) kullanim oturumlari, ayni uygulamanin farkli
     * kullanim araliklari TOPLAMA/BIRLESTIRME yapilmadan kronolojik sirayla,
     * ayri kayitlar olarak dondurulur (orn. Chrome 09:15->09:45, VS Code
     * 09:45->10:30, Chrome 10:30->11:05 - Chrome icin TEK bir "toplam 2 saat"
     * degil, iki ayri oturum). Ham acilis/kapanis olaylarini (loadTodayEvents
     * ile ayni UsageEvents sorgusu) paket bazinda esletirerek uretilir. Henuz
     * kapanmamis (su an on planda olan) acik bir oturum varsa DAHIL EDILMEZ -
     * rapor sadece TAMAMLANMIS oturumlari icerir. */
    fun loadTodaySessions(context: Context): List<AppUsageSession> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val pm = context.packageManager
        val events = usm.queryEvents(cal.timeInMillis, System.currentTimeMillis())
        val event = UsageEvents.Event()
        val pendingStart = mutableMapOf<String, Long>()
        val sessions = mutableListOf<AppUsageSession>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == context.packageName) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> pendingStart.putIfAbsent(event.packageName, event.timeStamp)
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = pendingStart.remove(event.packageName) ?: continue
                    sessions.add(AppUsageSession(appLabel(pm, event.packageName), event.packageName, start, event.timeStamp))
                }
                else -> continue
            }
        }
        return sessions.sortedBy { it.startMillis }
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Bugunku (00:00 - simdi) her uygulamanin ham UsageStatsManager kaydi,
     * paket adina gore gruplanmis - HICBIR sinir/sirlama uygulanmadan. Gunluk
     * sure siniri gibi TEK bir paketin gercek suresine ihtiyac duyan cagrilar
     * (bkz. StudyEngine.todaysUsageMinutes) buradan gecmeli - asagidaki
     * loadTodayUsage()'in gosterim amacli sinirlamasindan ETKILENMEMELI,
     * aksi halde yogun kullanimda (50+ farkli uygulama) sinirli uygulamanin
     * kendisi listeden dusup sure sinirinin SESSIZCE calismamasina yol acabilir. */
    fun loadTodayUsageRaw(context: Context): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: emptyList()
        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.sumOf { s -> s.totalTimeInForeground } }
    }

    /** Kullanim ekraninin "sure" sekmesi icin - gosterim amacli, en fazla
     * [limit] farkli uygulama (varsayilan 50, Ayarlar > Gelismis'ten
     * usageStatsTopAppsLimit ile degistirilebilir). Sinir enforcement
     * (gunluk sure siniri) icin KULLANILMAMALI - bkz. loadTodayUsageRaw. */
    fun loadTodayUsage(context: Context, limit: Int = 50): List<AppUsageEntry> {
        val pm = context.packageManager
        return loadTodayUsageRaw(context)
            .map { (pkg, totalMillis) -> AppUsageEntry(label = appLabel(pm, pkg), packageName = pkg, totalMillis = totalMillis) }
            .sortedByDescending { it.totalMillis }
            .take(limit)
    }

    private fun appLabel(pm: PackageManager, pkg: String): String = try {
        val ai = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(ai).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }
}
