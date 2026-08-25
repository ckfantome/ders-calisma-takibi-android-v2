package com.derscalismatakibi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.legal.PrivacyConsent
import com.derscalismatakibi.app.viewmodel.StudyViewModel

/** Onay verilene kadar AppNavigation NavHost yerine SADECE bu ekrani gosterir
 * (bkz. AppNavigation.kt basi) - "ogrenciden acik onay" gereksinimi. */
@Composable
fun PrivacyConsentScreen(viewModel: StudyViewModel) {
    val cfg by viewModel.configState.collectAsState()
    var checked by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gizlilik Bilgilendirmesi", style = MaterialTheme.typography.headlineSmall)
        Text(
            PrivacyConsent.TEXT,
            modifier = Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Text("Okudum, anladım, kabul ediyorum")
        }
        Button(
            enabled = checked,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            onClick = {
                viewModel.updateConfig(
                    cfg.copy(
                        privacyConsentAccepted = true,
                        privacyConsentVersion = PrivacyConsent.VERSION,
                        privacyConsentTimestamp = System.currentTimeMillis(),
                    )
                )
            },
        ) { Text("Devam Et") }
    }
}
