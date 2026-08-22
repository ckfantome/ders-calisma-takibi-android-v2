package com.derscalismatakibi.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.derscalismatakibi.app.core.UpdateChecker

/** AppNavigation (sessiz acilis kontrolu) ve SettingsScreen (manuel kontrol) ortak kullanir. */
@Composable
fun UpdateAvailableDialog(
    info: UpdateChecker.UpdateInfo,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni surum mevcut: v${info.version}") },
        text = { Text(info.notes.ifBlank { "Surum notu eklenmemis." }) },
        confirmButton = { TextButton(onClick = onInstall) { Text("Indir ve Kur") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Daha Sonra") } },
    )
}

/** "Bilinmeyen kaynaklardan yukleme" izni normal izin kutusuyla ISTENEMEZ - Ayarlar'a yonlendirir. */
@Composable
fun UnknownSourcesDialog(onGoToSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izin gerekiyor") },
        text = {
            Text(
                "Bu guncellemeyi kurabilmek icin \"Bilinmeyen kaynaklardan yukleme\" iznini " +
                    "Ayarlar'dan acikca vermen gerekiyor (bu, Android'in APK dosyalarini Play Store " +
                    "disindan kurarken istedigi standart bir guvenlik onayidir).",
            )
        },
        confirmButton = { TextButton(onClick = onGoToSettings) { Text("Ayarlara Git") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Iptal") } },
    )
}
