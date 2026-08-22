package com.derscalismatakibi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.viewmodel.StudyViewModel

/**
 * study_tracker2.py -> SettingsDialog (FIELD_META) icin basitlestirilmis Android
 * karsiligi. En sik degistirilecek esikler burada; konusma-tespiti ince-ayar
 * alanlari (SPEAKING_MAR_*) bu turda kapsam disi, varsayilan degerleriyle calisir.
 * study_tracker2.py -> _require_admin(): Ogrenci rolunde tum kontroller salt-okunur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: StudyViewModel) {
    val cfg by viewModel.configState.collectAsState()
    val role by viewModel.role.collectAsState()
    val isAdmin = role == Role.ADMIN

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)
        if (!isAdmin) {
            Text(
                "Ogrenci modundasin: ayarlar salt okunur. Degistirmek icin ust bardaki kilit ikonundan yonetici moduna gec.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SettingsGroup("Tespit Esikleri") {
            LabeledSlider("Goz Kapali Esigi (EAR)", cfg.earClosedThreshold, 0.05f, 0.5f, isAdmin) {
                viewModel.updateConfig(cfg.copy(earClosedThreshold = it.toDouble()))
            }
            LabeledSlider("Maks. Yatay Aci (derece)", cfg.yawMaxDeg, 5f, 90f, isAdmin) {
                viewModel.updateConfig(cfg.copy(yawMaxDeg = it.toDouble()))
            }
            LabeledSlider("Maks. Asagi Egim (derece)", cfg.pitchDownMaxDeg, 5f, 90f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pitchDownMaxDeg = it.toDouble()))
            }
            LabeledSlider("Maks. Yukari Egim (derece)", cfg.pitchUpMaxDeg, 5f, 90f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pitchUpMaxDeg = it.toDouble()))
            }
        }

        SettingsGroup("Onay Sureleri (sn)") {
            LabeledSlider("Uzakta Onay", cfg.confirmAwaySeconds, 1f, 30f, isAdmin) {
                viewModel.updateConfig(cfg.copy(confirmAwaySeconds = it.toDouble()))
            }
            LabeledSlider("Uyku Onay", cfg.confirmSleepSeconds, 1f, 30f, isAdmin) {
                viewModel.updateConfig(cfg.copy(confirmSleepSeconds = it.toDouble()))
            }
            LabeledSlider("Devam Onay", cfg.confirmResumeSeconds, 0.5f, 15f, isAdmin) {
                viewModel.updateConfig(cfg.copy(confirmResumeSeconds = it.toDouble()))
            }
        }

        SettingsGroup("Pomodoro (dk)") {
            LabeledSlider("Calisma", cfg.pomodoroWorkMin.toFloat(), 1f, 120f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroWorkMin = it.toInt()))
            }
            LabeledSlider("Mola", cfg.pomodoroBreakMin.toFloat(), 1f, 60f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroBreakMin = it.toInt()))
            }
            LabeledSlider("Uzun Mola", cfg.pomodoroLongBreakMin.toFloat(), 1f, 120f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroLongBreakMin = it.toInt()))
            }
            LabeledSlider("Uzun Molaya Kadar Dongu", cfg.pomodoroCyclesBeforeLong.toFloat(), 1f, 12f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroCyclesBeforeLong = it.toInt()))
            }
        }

        SettingsGroup("Hedefler (saat)") {
            LabeledSlider("Gunluk Hedef", cfg.dailyGoalHours, 0.5f, 24f, isAdmin) {
                viewModel.updateConfig(cfg.copy(dailyGoalHours = it.toDouble()))
            }
            LabeledSlider("Haftalik Hedef", cfg.weeklyGoalHours, 1f, 168f, isAdmin) {
                viewModel.updateConfig(cfg.copy(weeklyGoalHours = it.toDouble()))
            }
        }

        SettingsGroup("Genel") {
            SwitchRow("Uzakta Otomatik Duraklat", cfg.autoPauseOnAway, isAdmin) {
                viewModel.updateConfig(cfg.copy(autoPauseOnAway = it))
            }
            SwitchRow("Uyku Otomatik Duraklat", cfg.autoPauseOnSleep, isAdmin) {
                viewModel.updateConfig(cfg.copy(autoPauseOnSleep = it))
            }
            SwitchRow("On Kamerayi Kullan", cfg.useFrontCamera, isAdmin) {
                viewModel.updateConfig(cfg.copy(useFrontCamera = it))
            }
            SwitchRow("Sesli Uyari", cfg.soundEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(soundEnabled = it))
            }
            SwitchRow("Bildirimler", cfg.notificationsEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(notificationsEnabled = it))
            }
        }

        SettingsGroup("Tema") {
            val options = listOf("system" to "Sistem", "dark" to "Koyu", "light" to "Acik")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = cfg.themeMode == value,
                        onClick = { if (isAdmin) viewModel.updateConfig(cfg.copy(themeMode = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        enabled = isAdmin,
                    ) { Text(label) }
                }
            }
        }

        if (isAdmin) {
            SettingsGroup("Yonetici PIN") {
                var pinField by remember { mutableStateOf(cfg.appPin) }
                OutlinedTextField(
                    value = pinField,
                    onValueChange = { pinField = it; viewModel.updateConfig(cfg.copy(appPin = it)) },
                    label = { Text("Ogrenci -> Yonetici gecisinde istenen PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Double, min: Float, max: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    LabeledSlider(label, value.toFloat(), min, max, enabled, onChange)
}

@Composable
private fun LabeledSlider(label: String, value: Float, min: Float, max: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${"%.2f".format(value)}", style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = min..max, enabled = enabled)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
