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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.data.SafeZoneEntity
import com.derscalismatakibi.app.util.LocationHelper
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ebeveyn-denetim: konum + birden fazla Guvenli Bolge. Konum kontrolu ve
 * gecmis kaydi StudyEngine'de (30sn'de bir, arkaplan servisi calisirken de)
 * yapilir - bu ekran sadece ayar+durum. Kok LazyColumn kullanir: birden fazla
 * bolge karti + alanlari kucuk ekranlarda tasip alt butonlarin kirpilmesine
 * yol acabiliyordu (AppBlockScreen'deki ayni kok nedenli duzeltmeyle tutarli). */
@Composable
fun LocationScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val zones by viewModel.safeZones.collectAsState()

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
            }
            delay(10_000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Konum", style = MaterialTheme.typography.headlineSmall)
        }

        // ONEMLI: burada 'return@Column'/'return@item' KULLANILMAZ - Compose'un
        // composer grup takibini bozup recompose sirasinda "Stack.pop: Index -1"
        // ile rastgele coktugu gercek cihaz/emulator testinde dogrulandi.
        if (!hasFine) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Konum izni gerekiyor", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { fineLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Izin Ver") }
                    }
                }
            }
        } else {
            if (!hasBackground) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Arkaplanda konum icin ek izin", style = MaterialTheme.typography.titleMedium)
                            Text("Uygulama kapaliyken de guvenli bolge kontrolu icin gerekiyor.", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) { Text("Izin Ver") }
                        }
                    }
                }
            }

            item {
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
                        Text(
                            "Not: konum gecmisi (sadece anlik degil, surekli degisen tum konum) StudyEngine tarafindan " +
                                "30sn'de bir kaydedilir ve gunluk yedek e-postasina eklenir.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                Text("Guvenli Bolgeler", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text(
                    "Birden fazla bolge eklenebilir - cihaz herhangi bir ACIK bolgenin icindeyse guvenli sayilir.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (zones.isEmpty()) {
                item {
                    Text("Henuz guvenli bolge eklenmedi.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(zones, key = { it.id }) { zone ->
                    SafeZoneCard(
                        zone = zone,
                        liveLat = liveLat,
                        liveLng = liveLng,
                        onSave = { viewModel.updateSafeZone(it) },
                        onDelete = { viewModel.deleteSafeZone(it) },
                    )
                }
            }
            item {
                Button(onClick = {
                    viewModel.addSafeZone("Bolge ${zones.size + 1}", liveLat ?: 0.0, liveLng ?: 0.0, 200.0)
                }) { Text("Yeni Guvenli Bolge Ekle") }
            }
        }
    }
}

@Composable
private fun SafeZoneCard(
    zone: SafeZoneEntity,
    liveLat: Double?,
    liveLng: Double?,
    onSave: (SafeZoneEntity) -> Unit,
    onDelete: (SafeZoneEntity) -> Unit,
) {
    var nameText by remember(zone.id) { mutableStateOf(zone.name) }
    var latText by remember(zone.id) { mutableStateOf(zone.lat.toString()) }
    var lngText by remember(zone.id) { mutableStateOf(zone.lng.toString()) }
    var radiusText by remember(zone.id) { mutableStateOf(zone.radiusMeters.toString()) }

    val distanceText = if (liveLat != null && liveLng != null) {
        val d = LocationHelper.distanceMeters(liveLat, liveLng, zone.lat, zone.lng)
        if (d <= zone.radiusMeters) "Su an bu bolgede" else "Su an ${d.toInt()}m uzakta"
    } else {
        "Konum henuz alinamadi"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(distanceText, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = zone.enabled, onCheckedChange = { onSave(zone.copy(enabled = it)) })
            }
            OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Bolge Adi") })
            OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Enlem") })
            OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Boylam") })
            OutlinedTextField(value = radiusText, onValueChange = { radiusText = it }, label = { Text("Yaricap (metre)") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (liveLat != null && liveLng != null) {
                        latText = liveLat.toString()
                        lngText = liveLng.toString()
                    }
                }) { Text("Suradan Ayarla") }
                Button(onClick = { onDelete(zone) }) { Text("Sil") }
            }
            Button(onClick = {
                onSave(
                    zone.copy(
                        name = nameText.ifBlank { zone.name },
                        lat = latText.toDoubleOrNull() ?: zone.lat,
                        lng = lngText.toDoubleOrNull() ?: zone.lng,
                        radiusMeters = radiusText.toDoubleOrNull() ?: zone.radiusMeters,
                    ),
                )
            }) { Text("Kaydet") }
        }
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
