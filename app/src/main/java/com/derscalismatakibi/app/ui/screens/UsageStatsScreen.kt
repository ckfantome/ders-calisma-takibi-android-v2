package com.derscalismatakibi.app.ui.screens

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.core.fmtHms
import java.util.Calendar

/**
 * YENI ozellik (masaustunde karsiligi yok): "baska uygulamalara girersem
 * nereye ne kadar girmisim onlarda olsun" istegi icin - Android'in
 * UsageStatsManager API'siyle bugun hangi uygulamada ne kadar vakit
 * gecirildigini gosterir. PACKAGE_USAGE_STATS ozel bir izindir, normal
 * runtime izin dialoguyla ISTENEMEZ - kullanici Ayarlar'dan acikca vermeli.
 */
data class AppUsageEntry(val label: String, val packageName: String, val totalMillis: Long)

@Composable
fun UsageStatsScreen() {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(hasUsageAccess(context)) }
    var entries by remember { mutableStateOf<List<AppUsageEntry>>(emptyList()) }

    // Kullanici Ayarlar'dan izin verip bu ekrana donebilir - her gorunumde tekrar kontrol et.
    LaunchedEffect(Unit) {
        hasAccess = hasUsageAccess(context)
        if (hasAccess) entries = loadTodayUsage(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Uygulama Kullanimi", style = MaterialTheme.typography.headlineSmall)
        Text("Bugun (gece yarisindan simdiye) hangi uygulamada ne kadar vakit gecirdigin.", style = MaterialTheme.typography.bodySmall)

        if (!hasAccess) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Izin gerekiyor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Android bu tur hassas kullanim verisini normal izin kutusuyla vermiyor - " +
                            "\"Kullanim erisimi\"ni Ayarlar'dan acikca vermen gerekiyor " +
                            "(genelde: Ayarlar > Uygulamalar > Ozel erisim > Kullanim erisimi; cihaza gore menu adi degisebilir).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                        Text("Ayarlara Git")
                    }
                }
            }
        } else if (entries.isEmpty()) {
            Text("Bugun icin henuz veri yok.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                            Text(fmtHms(entry.totalMillis / 1000.0), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun loadTodayUsage(context: Context): List<AppUsageEntry> {
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
