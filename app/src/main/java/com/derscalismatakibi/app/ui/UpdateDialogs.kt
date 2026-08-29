package com.derscalismatakibi.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.derscalismatakibi.app.R
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
        title = { Text(stringResource(R.string.update_available_title, info.version)) },
        text = { Text(info.notes.ifBlank { stringResource(R.string.update_no_notes) }) },
        confirmButton = { TextButton(onClick = onInstall) { Text(stringResource(R.string.update_download_install)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } },
    )
}

/** "Bilinmeyen kaynaklardan yukleme" izni normal izin kutusuyla ISTENEMEZ - Ayarlar'a yonlendirir. */
@Composable
fun UnknownSourcesDialog(onGoToSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.usage_stats_permission_needed)) },
        text = {
            Text(
                stringResource(R.string.update_unknown_sources_explanation),
            )
        },
        confirmButton = { TextButton(onClick = onGoToSettings) { Text(stringResource(R.string.usage_stats_go_to_settings)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
