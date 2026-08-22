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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
        Text("Loglar", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Uygulamadaki tum onemli olaylar (durum degisimleri, servis yasam dongusu, " +
                "hatalar, yedekleme/guncelleme sonuclari) burada kaydedilir. Bir sorun " +
                "yasarsan \"Paylas\"a basip dosyayi kendine gonderip bana iletebilirsin.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val intent = AppLogger.buildShareIntent(context)
                if (intent != null) context.startActivity(android.content.Intent.createChooser(intent, "Log dosyasini paylas"))
            }) { Text("Paylaş") }
            OutlinedButton(onClick = { showClearConfirm = true }) { Text("Temizle") }
        }
        if (reversedLogs.isEmpty()) {
            Text("Henuz log yok.", style = MaterialTheme.typography.bodyMedium)
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
            title = { Text("Loglari Temizle") },
            text = { Text("Tum log kayitlari (bellek ve dosya) silinecek. Devam edilsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    AppLogger.clear()
                    showClearConfirm = false
                }) { Text("Evet") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Iptal") } },
        )
    }
}
