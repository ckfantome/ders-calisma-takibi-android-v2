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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.R
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
            Text(stringResource(R.string.location_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (!cfg.locationTrackingEnabled) {
            item {
                Text(
                    stringResource(R.string.location_tracking_disabled),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // ONEMLI: burada 'return@Column'/'return@item' KULLANILMAZ - Compose'un
        // composer grup takibini bozup recompose sirasinda "Stack.pop: Index -1"
        // ile rastgele coktugu gercek cihaz/emulator testinde dogrulandi.
        if (!hasFine) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.location_permission_needed), style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { fineLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text(stringResource(R.string.call_log_grant_permission)) }
                    }
                }
            }
        } else {
            if (!hasBackground) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.location_background_permission_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.location_background_permission_explanation), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) { Text(stringResource(R.string.call_log_grant_permission)) }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.location_live_title), style = MaterialTheme.typography.titleMedium)
                        if (liveLat != null && liveLng != null) {
                            Text(stringResource(R.string.location_latitude, liveLat.toString()), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.location_longitude, liveLng.toString()), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.location_updated_at, liveUpdatedAt), style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(stringResource(R.string.location_gps_not_available), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            stringResource(R.string.location_history_note),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.location_safe_zones_title), style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text(
                    stringResource(R.string.location_safe_zones_explanation),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (zones.isEmpty()) {
                item {
                    Text(stringResource(R.string.location_no_safe_zones), style = MaterialTheme.typography.bodySmall)
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
                val newZoneNamePrefix = stringResource(R.string.location_new_zone_name_prefix)
                Button(onClick = {
                    viewModel.addSafeZone("$newZoneNamePrefix ${zones.size + 1}", liveLat ?: 0.0, liveLng ?: 0.0, 200.0)
                }) { Text(stringResource(R.string.location_add_safe_zone)) }
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
        if (d <= zone.radiusMeters) stringResource(R.string.location_currently_in_zone) else stringResource(R.string.location_currently_away, d.toInt())
    } else {
        stringResource(R.string.location_not_available_yet)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(distanceText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = zone.enabled, onCheckedChange = { onSave(zone.copy(enabled = it)) })
            }
            OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text(stringResource(R.string.location_zone_name_label)) })
            OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text(stringResource(R.string.location_latitude_label)) })
            OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text(stringResource(R.string.location_longitude_label)) })
            OutlinedTextField(value = radiusText, onValueChange = { radiusText = it }, label = { Text(stringResource(R.string.location_radius_label)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (liveLat != null && liveLng != null) {
                        latText = liveLat.toString()
                        lngText = liveLng.toString()
                    }
                }) { Text(stringResource(R.string.location_set_from_current)) }
                Button(onClick = { onDelete(zone) }) { Text(stringResource(R.string.action_delete)) }
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
            }) { Text(stringResource(R.string.action_save)) }
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
