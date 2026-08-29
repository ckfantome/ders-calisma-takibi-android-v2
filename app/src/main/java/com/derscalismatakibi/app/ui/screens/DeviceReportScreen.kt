package com.derscalismatakibi.app.ui.screens

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.ui.rememberResumeTrigger
import com.derscalismatakibi.app.util.UsageStatsHelper

/**
 * Pil + bugunku veri kullanimi ozeti. Ekstra izin gerektirmez (pil: sticky
 * broadcast; veri: mevcut Kullanim Erisimi izniyle NetworkStatsManager).
 */
@Composable
fun DeviceReportScreen() {
    val context = LocalContext.current
    var batteryPct by remember { mutableStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    var wifiMb by remember { mutableStateOf(-1.0) }
    var mobileMb by remember { mutableStateOf(-1.0) }
    val resumeTrigger = rememberResumeTrigger()

    LaunchedEffect(resumeTrigger) {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        if (UsageStatsHelper.hasUsageAccess(context)) {
            val (w, m) = todayNetworkUsageMb(context)
            wifiMb = w
            mobileMb = m
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.device_report_title), style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.device_report_battery), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (batteryPct >= 0) {
                        stringResource(R.string.device_report_battery_percent, batteryPct) +
                            (if (charging) " " + stringResource(R.string.device_report_charging) else "")
                    } else {
                        stringResource(R.string.device_report_unknown)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.device_report_data_usage_title), style = MaterialTheme.typography.titleMedium)
                if (!UsageStatsHelper.hasUsageAccess(context)) {
                    Text(stringResource(R.string.device_report_usage_access_needed), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        stringResource(R.string.device_report_data_usage_value, wifiMb, mobileMb),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** ponytail: TYPE_WIFI/TYPE_MOBILE deprecated ama tek-cihaz ozet raporu icin
 * NetworkCapabilities'e gecmeye gerek yok - basit ve calisiyor. Izin/veri
 * yoksa sessizce 0 doner (kart "izin gerekiyor" mesajiyla zaten kapatiliyor). */
fun todayNetworkUsageMb(context: Context): Pair<Double, Double> {
    val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return 0.0 to 0.0
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis
    val end = System.currentTimeMillis()
    fun sumFor(type: Int): Double = try {
        val bucket = nsm.querySummaryForDevice(type, null, start, end)
        (bucket.rxBytes + bucket.txBytes) / (1024.0 * 1024.0)
    } catch (t: Throwable) {
        0.0
    }
    return sumFor(ConnectivityManager.TYPE_WIFI) to sumFor(ConnectivityManager.TYPE_MOBILE)
}
