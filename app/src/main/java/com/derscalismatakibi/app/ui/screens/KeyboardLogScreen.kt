package com.derscalismatakibi.app.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Klavye Takibi kayitlarini gosterir (bkz. AppConfig.keyboardTrackingEnabled,
 * AppBlockAccessibilityService.handleTextChanged). Hassas veri oldugu icin
 * SADECE yonetici goruntuleyebilir - CallLogScreen/LogsScreen'in aksine
 * ogrenci modunda ekranin tamami gizlenir.
 */
@Composable
fun KeyboardLogScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val role by viewModel.role.collectAsState()
    val isAdmin = role == Role.ADMIN
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getInstance(context).keystrokeLogDao() }
    val entries by dao.observeRecent().collectAsState(initial = emptyList())
    val dateFmt = remember { SimpleDateFormat("dd.MM HH:mm:ss", Locale("tr")) }

    // ONEMLI: burada 'return@Column' KULLANILMAZ - LocationScreen.kt'de
    // gercek cihaz/emulator testinde dogrulanan Compose composer-grup bozulmasi
    // (Stack.pop: Index -1) hatasina yol aciyordu. Tum govde tek if/else.
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Klavye Takibi", style = MaterialTheme.typography.headlineSmall)
        if (!isAdmin) {
            Text(
                "Bu ekran sadece yonetici modunda goruntulenebilir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                "AppConfig.keyboardTrackingEnabled (Uygulama Kilidi ekranindan acilir/kapanir) acikken diger " +
                    "uygulamalarda yazilan metinler burada listelenir. Sifre alanlari HARIC tutulur.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = { scope.launch { dao.clear() } }) { Text("Temizle") }
            if (entries.isEmpty()) {
                Text("Henuz kayit yok.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(entries) { e ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(e.appLabel, style = MaterialTheme.typography.bodyMedium)
                                    Text(dateFmt.format(Date(e.timestamp)), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(e.text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
