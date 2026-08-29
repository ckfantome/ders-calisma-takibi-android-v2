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
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun loadTodayUsage(context: Context): List<AppUsageEntry> {
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
        val pm = context.packageManager
        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .map { (pkg, list) -> AppUsageEntry(label = appLabel(pm, pkg), packageName = pkg, totalMillis = list.sumOf { s -> s.totalTimeInForeground }) }
            .sortedByDescending { it.totalMillis }
            .take(50)
    }

    private fun appLabel(pm: PackageManager, pkg: String): String = try {
        val ai = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(ai).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }
}
