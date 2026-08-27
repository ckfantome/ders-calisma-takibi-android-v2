package com.derscalismatakibi.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.data.BlockedAppEntity
import com.derscalismatakibi.app.ui.rememberResumeTrigger
import com.derscalismatakibi.app.viewmodel.StudyViewModel

/**
 * "Uygulama Kilidi": AppBlockAccessibilityService'in engelleyecegi uygulama
 * listesi + Sinav/Odev Modu anahtari. Erisilebilirlik izni ozel bir izindir,
 * normal runtime izin kutusuyla ISTENEMEZ (UsageStatsScreen ile ayni desen).
 */
@Composable
fun AppBlockScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val cfg by viewModel.configState.collectAsState()
    val role by viewModel.role.collectAsState()
    val isAdmin = role == Role.ADMIN
    val blocked by viewModel.blockedApps.collectAsState()

    var hasAccess by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var installedApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val resumeTrigger = rememberResumeTrigger()

    LaunchedEffect(resumeTrigger) {
        hasAccess = isAccessibilityServiceEnabled(context)
        val pm = context.packageManager
        // FLAG_SYSTEM filtresi KALDIRILDI: bircok OEM'de (ozellikle Samsung) YouTube,
        // Samsung Internet, hatta bazen Instagram/Facebook gibi tam olarak ebeveynin
        // engellemek istedigi uygulamalar onceden yuklu geldigi icin FLAG_SYSTEM
        // tasiyor - bu yuzden liste bombos gorunuyordu. Baslatma (launcher) simgesi
        // olan her uygulama (kendimiz haric) yeterli bir kriter.
        installedApps = pm.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .mapNotNull { info -> pm.getLaunchIntentForPackage(info.packageName)?.let { info.packageName to pm.getApplicationLabel(info).toString() } }
            .sortedBy { it.second.lowercase() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Uygulama Kilidi", style = MaterialTheme.typography.headlineSmall)
        if (!isAdmin) {
            Text(
                "Ogrenci modundasin: bu ekran salt okunur. Degistirmek icin ust bardaki kilit ikonundan yonetici moduna gec.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (!hasAccess) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Erisilebilirlik izni gerekiyor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Engellenen bir uygulamanin on plana gelisini tespit edebilmek icin \"Erisilebilirlik Servisi\"ni " +
                            "Ayarlar'dan acikca acman gerekiyor. Bu servis ekran icerigini OKUMAZ, sadece hangi uygulamanin " +
                            "on planda oldugunu gorur.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                        Text("Ayarlara Git")
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Sınav/Ödev Modu", style = MaterialTheme.typography.titleMedium)
                        Text("Açıkken listedeki TÜM uygulamalar koşulsuz engellenir.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = cfg.examModeEnabled, onCheckedChange = { if (isAdmin) viewModel.updateConfig(cfg.copy(examModeEnabled = it)) }, enabled = isAdmin)
                }
            }

            Text("Kilitli Uygulamalar", style = MaterialTheme.typography.titleMedium)
            if (blocked.isEmpty()) {
                Text("Henuz uygulama eklenmedi.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    blocked.forEach { entry ->
                        BlockedAppRow(entry, isAdmin, onRemove = { viewModel.deleteBlockedApp(entry) })
                    }
                }
            }

            val blockedPackages = remember(blocked) { blocked.map { it.packageName }.toSet() }
            val addable = remember(installedApps, blockedPackages) { installedApps.filter { it.first !in blockedPackages } }
            Text("Uygulama Ekle", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(addable) { (pkg, label) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Button(enabled = isAdmin, onClick = { viewModel.addBlockedApp(pkg, label, null, true) }) { Text("Ekle") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedAppRow(entry: BlockedAppEntity, isAdmin: Boolean, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(entry.appLabel, style = MaterialTheme.typography.bodyMedium)
                Button(enabled = isAdmin, onClick = onRemove) { Text("Kaldir") }
            }
        }
    }
}

/** UsageStatsHelper.hasUsageAccess'in erisilebilirlik karsiligi - Settings.Secure
 * uzerinden servis ID'sinin etkin listede olup olmadigini kontrol eder. */
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    // ENABLED_ACCESSIBILITY_SERVICES tam ComponentName.flattenToString() tutar
    // (paket/tam.sinif.Adi) - manifestteki "." kisayolu burda GECERSIZ, hep false donuyordu.
    val expected = android.content.ComponentName(context, com.derscalismatakibi.app.service.AppBlockAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
