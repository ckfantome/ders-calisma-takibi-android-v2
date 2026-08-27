package com.derscalismatakibi.app.ui.screens

import android.content.Intent
import android.net.Uri
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
 * listesi + Sinav/Odev Modu anahtari + (Sinav Modunda) izin verilenler listesi
 * + Klavye Takibi anahtari. Erisilebilirlik izni ozel bir izindir, normal
 * runtime izin kutusuyla ISTENEMEZ (UsageStatsScreen ile ayni desen).
 *
 * Kok LazyColumn kullanir (duz, kaydirilamayan bir Column DEGIL): birden fazla
 * kart + iki ayri uygulama listesi (Izin Verilen / Uygulama Ekle) toplam
 * yukseklik ekran boyunu asabiliyordu, bu da alttaki butonlarin kirpilip
 * erisilemez hale gelmesine yol aciyordu (LocationScreen'deki "aşağıda mavi
 * çizgi" hatasiyla ayni kok neden). LazyColumn+item{}/items{} hem tum icerigi
 * kaydirilabilir yapar hem de ic ice iki LazyColumn koymanin (crash'e yol acan
 * "infinity height constraint" hatasi) onune gecer.
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

    // NOT: 'remember' sadece @Composable baglaminda cagrilabilir - LazyColumn{}'un
    // govdesi (item{}/items{} disindaki kod) LazyListScope DSL'i olup composable
    // DEGILDIR. Bu yuzden tum turetilmis listeler LazyColumn'a girmeden ONCE
    // burada hesaplaniyor.
    val allowedPackages = remember(cfg.examAllowedPackages) {
        cfg.examAllowedPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val addableAllowed = remember(installedApps, allowedPackages) { installedApps.filter { it.first !in allowedPackages } }
    val blockedPackages = remember(blocked) { blocked.map { it.packageName }.toSet() }
    val addable = remember(installedApps, blockedPackages) { installedApps.filter { it.first !in blockedPackages } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Uygulama Kilidi", style = MaterialTheme.typography.headlineSmall)
        }
        if (!isAdmin) {
            item {
                Text(
                    "Ogrenci modundasin: bu ekran salt okunur. Degistirmek icin ust bardaki kilit ikonundan yonetici moduna gec.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (!hasAccess) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Erisilebilirlik izni gerekiyor", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Engellenen bir uygulamanin on plana gelisini tespit edebilmek icin \"Erisilebilirlik Servisi\"ni " +
                                "Ayarlar'dan acikca acman gerekiyor. Varsayilan olarak bu servis ekran icerigini OKUMAZ, sadece " +
                                "hangi uygulamanin on planda oldugunu gorur - Klavye Takibi'ni asagidan ayrica acarsan yazilan " +
                                "metinleri de kaydetmeye baslar.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                            Text("Ayarlara Git")
                        }
                        if (com.derscalismatakibi.app.util.OemAutostartHelper.isKnownRestrictiveOem()) {
                            Text(
                                "Bu telefon markasinda (Oppo/Xiaomi/Huawei/Vivo) izin acik olsa bile " +
                                    "sistem servisi zamanla kapatabiliyor - asagidan Otomatik Baslatma " +
                                    "listesine de eklemen gerekiyor.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(onClick = {
                                if (!com.derscalismatakibi.app.util.OemAutostartHelper.openAutostartSettings(context)) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                                    )
                                }
                            }) { Text("Otomatik Baslatma Ayarlarini Ac") }
                        }
                    }
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Sınav/Ödev Modu", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Acikken Kilitli Uygulamalar listesine bakilmaksizin HER SEY engellenir - sadece asagidaki " +
                                        "'Izin Verilen Uygulamalar' listesindekiler ve zorunlu (ana ekran/telefon) uygulamalar acilabilir.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(checked = cfg.examModeEnabled, onCheckedChange = { if (isAdmin) viewModel.updateConfig(cfg.copy(examModeEnabled = it)) }, enabled = isAdmin)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("+ Ekrani Tamamen Sabitle", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Sinav Modu acikken telefon SADECE bu uygulamaya kilitlenir - 'Izin Verilen " +
                                        "Uygulamalar' listesi de dahil BASKA HICBIR SEY acilamaz (ana ekran/Son " +
                                        "Kullanilanlar da devre disi). Erisilebilirlik izninin OEM tarafindan " +
                                        "kapatilmasindan etkilenmez.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(checked = cfg.screenPinningEnabled, onCheckedChange = { if (isAdmin) viewModel.updateConfig(cfg.copy(screenPinningEnabled = it)) }, enabled = isAdmin)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Klavye Takibi", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "ACIKKEN: diger uygulamalarda yazilan metinler kaydedilir (sifre alanlari HARIC). " +
                                        "Bu, Erisilebilirlik'in 'ekran icerigini okumaz' varsayilanini DEGISTIRIR - acmadan " +
                                        "once Gizlilik onay metnini tekrar okuman istenecek.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(checked = cfg.keyboardTrackingEnabled, onCheckedChange = { if (isAdmin) viewModel.updateConfig(cfg.copy(keyboardTrackingEnabled = it)) }, enabled = isAdmin)
                        }
                    }
                }
            }

            if (cfg.examModeEnabled) {
                item {
                    Text("Izin Verilen Uygulamalar (Sinav Modunda)", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Text(
                        "Sinav/Odev Modu acikken bunlar disinda hicbir uygulama acilamaz (orn. Hesap Makinesi).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (allowedPackages.isEmpty()) {
                    item {
                        Text("Henuz izin verilen uygulama eklenmedi.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(allowedPackages.toList()) { pkg ->
                        val label = installedApps.find { it.first == pkg }?.second ?: pkg
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Button(enabled = isAdmin, onClick = {
                                    viewModel.updateConfig(cfg.copy(examAllowedPackages = (allowedPackages - pkg).joinToString(",")))
                                }) { Text("Kaldir") }
                            }
                        }
                    }
                }
                items(addableAllowed) { (pkg, label) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Button(enabled = isAdmin, onClick = {
                                viewModel.updateConfig(cfg.copy(examAllowedPackages = (allowedPackages + pkg).joinToString(",")))
                            }) { Text("Izin Ver") }
                        }
                    }
                }
            }

            item {
                Text("Kilitli Uygulamalar", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text(
                    "Sinav/Odev Modu kapaliyken bu uygulamalar sadece kamera 'Calisiyor' durumunu tespit ettiginde engellenir.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (blocked.isEmpty()) {
                item {
                    Text("Henuz uygulama eklenmedi.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(blocked, key = { it.id }) { entry ->
                    BlockedAppRow(entry, isAdmin, onRemove = { viewModel.deleteBlockedApp(entry) })
                }
            }

            item {
                Text("Uygulama Ekle", style = MaterialTheme.typography.titleMedium)
            }
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

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean =
    com.derscalismatakibi.app.util.AccessibilityHelper.isAppBlockServiceEnabled(context)
