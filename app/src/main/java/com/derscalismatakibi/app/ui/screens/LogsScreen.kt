package com.derscalismatakibi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.util.AppLogger

/**
 * Beta/tani ekrani: sadece hatalar degil, StudyEngine/Servis/UI'daki TUM onemli
 * olaylar (bkz. AppLogger cagrilari) burada goruntulenir. Kullanici bir sorun
 * yasadiginda "Paylas"a basip log dosyasini kendine (WhatsApp/Telegram/e-posta)
 * gonderip sonra bana iletebilir - bildirim metnini elle okuyup yazmasina gerek
 * kalmaz (bkz. arkaplan servisi hatasinin OpenCV native kutuphanesi oldugunun
 * bulunmasi surecinde yasanan zorluk).
 */
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val logs by AppLogger.logs.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    // En yeni satir en ustte gorunsun - kullanicinin son olayi gormek icin
    // asagi kaydirmasina gerek kalmasin.
    val reversedLogs = remember(logs) { logs.asReversed() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.logs_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.logs_explanation),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val shareChooserTitle = stringResource(R.string.logs_share_chooser_title)
            Button(onClick = {
                val intent = AppLogger.buildShareIntent(context)
                if (intent != null) context.startActivity(android.content.Intent.createChooser(intent, shareChooserTitle))
            }) { Text(stringResource(R.string.action_share)) }
            OutlinedButton(onClick = { showClearConfirm = true }) { Text(stringResource(R.string.action_clear)) }
        }
        if (reversedLogs.isEmpty()) {
            Text(stringResource(R.string.logs_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(reversedLogs) { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.logs_clear_dialog_title)) },
            text = { Text(stringResource(R.string.logs_clear_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    AppLogger.clear()
                    showClearConfirm = false
                }) { Text(stringResource(R.string.action_yes)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
