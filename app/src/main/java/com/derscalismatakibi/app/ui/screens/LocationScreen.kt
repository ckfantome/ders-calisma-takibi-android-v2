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

    LaunchedEffect(hasFine, cfg.safeZoneLat, cfg.safeZoneLng) {
        if (hasFine) {
            val loc = lastKnownLocation(context)
            distanceText = if (loc != null) {
                val d = LocationHelper.distanceMeters(loc.first, loc.second, cfg.safeZoneLat, cfg.safeZoneLng)
                if (d <= cfg.safeZoneRadiusMeters) "Guvenli bolgede" else "Disarida, ${d.toInt()}m uzakta"
            } else "Konum henuz alinamadi"
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Konum", style = MaterialTheme.typography.headlineSmall)

        if (!hasFine) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Konum izni gerekiyor", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { fineLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Izin Ver") }
                }
            }
            return@Column
        }
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
