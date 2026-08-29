package com.derscalismatakibi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.Telephony
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallEntry(val name: String, val type: String, val durationSec: Long, val date: Long)
data class SmsEntry(val address: String, val preview: String, val date: Long)

/** Ebeveyn-denetim: arama gecmisi + SMS ozeti. READ_CALL_LOG/READ_SMS normal
 * (dangerous) runtime izinlerdir - MainScreen'deki kamera izni deseniyle
 * ayni sekilde dogrudan istenir, Ayarlar'a yonlendirme gerekmez. */
@Composable
fun CallLogScreen() {
    val context = LocalContext.current
    val cfg by com.derscalismatakibi.app.core.StudyEngine.configState.collectAsState()
    if (!cfg.callSmsLogEnabled) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.call_log_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.call_log_disabled),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    var hasCallPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
    }
    var hasSmsPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasCallPerm = result[Manifest.permission.READ_CALL_LOG] == true
        hasSmsPerm = result[Manifest.permission.READ_SMS] == true
    }

    var calls by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var sms by remember { mutableStateOf<List<SmsEntry>>(emptyList()) }
    var showSms by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }

    LaunchedEffect(hasCallPerm, hasSmsPerm) {
        if (hasCallPerm) calls = loadCalls(context)
        if (hasSmsPerm) sms = loadSms(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.call_log_title), style = MaterialTheme.typography.headlineSmall)

        if (!hasCallPerm || !hasSmsPerm) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.call_log_permission_needed), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.call_log_permission_explanation), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        permLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_SMS))
                    }) { Text(stringResource(R.string.call_log_grant_permission)) }
                }
            }
        } else {
            val callsTabLabel = stringResource(R.string.call_log_tab_calls)
            val smsTabLabel = stringResource(R.string.call_log_tab_sms)
            val durationSecSuffix = stringResource(R.string.call_log_duration_seconds_suffix)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showSms = false }) { Text(if (!showSms) "• $callsTabLabel" else callsTabLabel) }
                TextButton(onClick = { showSms = true }) { Text(if (showSms) "• $smsTabLabel" else smsTabLabel) }
            }
            if (!showSms) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(calls) { c ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text("${c.name} - ${c.type}", style = MaterialTheme.typography.bodyMedium)
                                Text("${dateFmt.format(Date(c.date))} · ${c.durationSec}$durationSecSuffix", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sms) { s ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(s.address, style = MaterialTheme.typography.bodyMedium)
                                Text("${dateFmt.format(Date(s.date))} · ${s.preview}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun loadCalls(context: android.content.Context): List<CallEntry> {
    val result = mutableListOf<CallEntry>()
    context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
        null, null, "${CallLog.Calls.DATE} DESC",
    )?.use { cursor ->
        while (cursor.moveToNext() && result.size < 50) {
            val name = cursor.getString(0) ?: cursor.getString(1) ?: context.getString(R.string.call_log_unknown)
            val type = when (cursor.getInt(2)) {
                CallLog.Calls.INCOMING_TYPE -> context.getString(R.string.call_log_type_incoming)
                CallLog.Calls.OUTGOING_TYPE -> context.getString(R.string.call_log_type_outgoing)
                CallLog.Calls.MISSED_TYPE -> context.getString(R.string.call_log_type_missed)
                else -> context.getString(R.string.call_log_type_other)
            }
            result.add(CallEntry(name, type, cursor.getLong(3), cursor.getLong(4)))
        }
    }
    return result
}

fun loadSms(context: android.content.Context): List<SmsEntry> {
    val result = mutableListOf<SmsEntry>()
    context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
        null, null, "${Telephony.Sms.DATE} DESC",
    )?.use { cursor ->
        while (cursor.moveToNext() && result.size < 50) {
            val address = cursor.getString(0) ?: context.getString(R.string.call_log_unknown)
            val body = (cursor.getString(1) ?: "").take(50)
            result.add(SmsEntry(address, body, cursor.getLong(2)))
        }
    }
    return result
}
