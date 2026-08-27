package com.derscalismatakibi.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.util.LocationHelper
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ebeveyn-denetim: konum + guvenli bolge ayarlari. Konum kontrolu StudyEngine'de
 * (30sn'de bir, arkaplan servisi calisirken de) yapilir - bu ekran sadece ayar+durum. */
@Composable
fun LocationScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val cfg by viewModel.configState.collectAsState()

    var hasFine by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var hasBackground by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val fineLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasFine = it }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasBackground = it }

    var latText by remember { mutableStateOf(cfg.safeZoneLat.toString()) }
    var lngText by remember { mutableStateOf(cfg.safeZoneLng.toString()) }
    var radiusText by remember { mutableStateOf(cfg.safeZoneRadiusMeters.toString()) }
    var distanceText by remember { mutableStateOf("") }
    var liveLat by remember { mutableStateOf<Double?>(null) }
    var liveLng by remember { mutableStateOf<Double?>(null) }
    var liveUpdatedAt by remember { mutableStateOf("") }

    // Ekran acikken anlik konumu 10sn'de bir tazele - StudyEngine'deki arkaplan
    // guvenli-bolge kontrolunden (30sn) BAGIMSIZ, sadece bu ekran gorunurken calisir.
    LaunchedEffect(hasFine) {
        while (hasFine) {
            val loc = lastKnownLocation(context)
            if (loc != null) {
                liveLat = loc.first
                liveLng = loc.second
                liveUpdatedAt = SimpleDateFormat("HH:mm:ss", Locale("tr")).format(Date())
                val d = LocationHelper.distanceMeters(loc.first, loc.second, cfg.safeZoneLat, cfg.safeZoneLng)
                distanceText = if (d <= cfg.safeZoneRadiusMeters) "Guvenli bolgede" else "Disarida, ${d.toInt()}m uzakta"
            } else {
                distanceText = "Konum henuz alinamadi"
            }
            delay(10_000)
        }
    }

    // Guvenli Bolge karti (anahtar + 3 alan + 2 buton) + Anlik Konum karti +
    // izin kartlari toplamda kucuk ekranlarda tasabiliyordu - "Kaydet" butonunun
    // kirpilip goze sadece ust kenarinin ince bir cizgi olarak takilmasina yol
    // aciyordu ("aşağıda mavi çizgi" raporu). Kaydirilabilir yapildi.
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Konum", style = MaterialTheme.typography.headlineSmall)

        // ONEMLI: burada 'return@Column' KULLANILMAZ - Compose'un composer grup
        // takibini bozup recompose sirasinda "Stack.pop: Index -1" ile rastgele
        // coktugu gercek cihaz/emulator testinde dogrulandi. Bunun yerine TUM
        // govde tek bir if/else agaciyla kapsanip yapisal olarak tutarli tutulur.
        if (!hasFine) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Konum izni gerekiyor", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { fineLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Izin Ver") }
                }
            }
        } else {
            if (!hasBackground) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Arkaplanda konum icin ek izin", style = MaterialTheme.typography.titleMedium)
                        Text("Uygulama kapaliyken de guvenli bolge kontrolu icin gerekiyor.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) { Text("Izin Ver") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anlik Konum", style = MaterialTheme.typography.titleMedium)
                    if (liveLat != null && liveLng != null) {
                        Text("Enlem: $liveLat", style = MaterialTheme.typography.bodyMedium)
                        Text("Boylam: $liveLng", style = MaterialTheme.typography.bodyMedium)
                        Text("Guncellendi: $liveUpdatedAt (10sn'de bir tazelenir)", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Konum henuz alinamadi (GPS/konum servisi acik mi kontrol et).", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guvenli Bolge", style = MaterialTheme.typography.titleMedium)
                    Text(distanceText, style = MaterialTheme.typography.bodyMedium)
                    Row2(cfg.safeZoneEnabled) { checked -> viewModel.updateConfig(cfg.copy(safeZoneEnabled = checked)) }
                    OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Enlem") })
                    OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Boylam") })
                    OutlinedTextField(value = radiusText, onValueChange = { radiusText = it }, label = { Text("Yaricap (metre)") })
                    Button(onClick = {
                        val loc = lastKnownLocation(context)
                        if (loc != null) { latText = loc.first.toString(); lngText = loc.second.toString() }
                    }) { Text("Suradan Ayarla: Su anki konumum") }
                    Button(onClick = {
                        viewModel.updateConfig(
                            cfg.copy(
                                safeZoneLat = latText.toDoubleOrNull() ?: cfg.safeZoneLat,
                                safeZoneLng = lngText.toDoubleOrNull() ?: cfg.safeZoneLng,
                                safeZoneRadiusMeters = radiusText.toDoubleOrNull() ?: cfg.safeZoneRadiusMeters,
                            )
                        )
                    }) { Text("Kaydet") }
                }
            }
        }
    }
}

@Composable
private fun Row2(checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Text("Guvenli bolge kontrolu acik", style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    for (provider in lm.getProviders(true)) {
        val loc = try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
        if (loc != null) return loc.latitude to loc.longitude
    }
    return null
}
