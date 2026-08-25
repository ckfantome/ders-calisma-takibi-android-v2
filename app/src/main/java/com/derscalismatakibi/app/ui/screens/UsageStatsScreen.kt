package com.derscalismatakibi.app.ui.screens

import android.content.Intent
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
import androidx.compose.material3.TextButton
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
import com.derscalismatakibi.app.util.AppEventEntry
import com.derscalismatakibi.app.util.AppUsageEntry
import com.derscalismatakibi.app.util.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * YENI ozellik (masaustunde karsiligi yok): "baska uygulamalara girersem
 * nereye ne kadar girmisim onlarda olsun" istegi icin - Android'in
 * UsageStatsManager API'siyle bugun hangi uygulamada ne kadar vakit
 * gecirildigini VE ne zaman acilip kapandigini gosterir. PACKAGE_USAGE_STATS
 * ozel bir izindir, normal runtime izin dialoguyla ISTENEMEZ - kullanici
 * Ayarlar'dan acikca vermeli.
 *
 * Cekirdek mantik (izin kontrolu + veri okuma) artik util/UsageStatsHelper.kt'de -
 * gunluk otomatik yedekleme (backup/DailyBackupWorker.kt) de ayni mantigi
 * kullaniyor.
 */
@Composable
fun UsageStatsScreen() {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(UsageStatsHelper.hasUsageAccess(context)) }
    var entries by remember { mutableStateOf<List<AppUsageEntry>>(emptyList()) }
    var events by remember { mutableStateOf<List<AppEventEntry>>(emptyList()) }
    var showEvents by remember { mutableStateOf(false) }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Kullanici Ayarlar'dan izin verip bu ekrana donebilir - her gorunumde tekrar kontrol et.
    LaunchedEffect(Unit) {
        hasAccess = UsageStatsHelper.hasUsageAccess(context)
        if (hasAccess) {
            entries = UsageStatsHelper.loadTodayUsage(context)
            events = UsageStatsHelper.loadTodayEvents(context)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Uygulama Kullanimi", style = MaterialTheme.typography.headlineSmall)

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
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showEvents = false }) { Text(if (!showEvents) "• Sure" else "Sure") }
                TextButton(onClick = { showEvents = true }) { Text(if (showEvents) "• Olaylar" else "Olaylar") }
            }

            if (!showEvents) {
                Text("Bugun (gece yarisindan simdiye) hangi uygulamada ne kadar vakit gecirdigin.", style = MaterialTheme.typography.bodySmall)
                if (entries.isEmpty()) {
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
            } else {
                Text("Bugun hangi uygulama ne zaman acildi/kapandi.", style = MaterialTheme.typography.bodySmall)
                if (events.isEmpty()) {
                    Text("Bugun icin henuz olay yok.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(events) { ev ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${ev.label} - ${ev.type}", style = MaterialTheme.typography.bodyMedium)
                                    Text(timeFmt.format(Date(ev.timestamp)), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
