package com.derscalismatakibi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.backup.BackupScheduler
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.UpdateChecker
import com.derscalismatakibi.app.service.StudyForegroundService
import com.derscalismatakibi.app.ui.UnknownSourcesDialog
import com.derscalismatakibi.app.ui.UpdateAvailableDialog
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.util.BatteryOptimizationHelper
import com.derscalismatakibi.app.util.UpdateInstaller
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUnknownSourcesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // "Arkaplanda Takip" MainScreen'den buraya (Calisan Sistemler) tasindi -
    // ayni baslatma/durdurma mantigi.
    val backgroundActive by viewModel.backgroundTrackingActive.collectAsState()
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    val batteryOptimizationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)
        if (!isAdmin) {
            Text(
                "Ogrenci modundasin: sistemi etkileyen ayarlarin cogu gizli. Sadece Tema/Dil/Ses/Bildirim " +
                    "burada kaliyor - digerlerini gormek/degistirmek icin ust bardaki kilit ikonundan yonetici moduna gec.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Ogrenci modunda sistemi etkilemeyen bu ucu HER ZAMAN gorunur+degistirilebilir
        // kalir (kullaniciyla netlesen karar) - geri kalan gruplarin tamami asagida
        // isAdmin ile TAMAMEN gizlenir (salt-okunur degil, hic gorunmez).
        SettingsGroup("Tema") {
            val options = listOf("system" to "Sistem", "dark" to "Koyu", "light" to "Acik")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = cfg.themeMode == value,
                        onClick = { viewModel.updateConfig(cfg.copy(themeMode = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) { Text(label) }
                }
            }
        }

        SettingsGroup("Dil / Language") {
            val langOptions = listOf("tr" to "Turkce", "en" to "English")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                langOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = cfg.appLanguage == value,
                        onClick = { viewModel.updateConfig(cfg.copy(appLanguage = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = langOptions.size),
                    ) { Text(label) }
                }
            }
            Text(
                "Not: Sadece sistem dilini degistirir (Android 13+). Uygulama ekranlarindaki metinler henuz cevrilmedi.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SettingsGroup("Ses & Bildirim") {
            SwitchRow("Sesli Uyari", cfg.soundEnabled, true) {
                viewModel.updateConfig(cfg.copy(soundEnabled = it))
            }
            SwitchRow("Bildirimler", cfg.notificationsEnabled, true) {
                viewModel.updateConfig(cfg.copy(notificationsEnabled = it))
            }
        }

        if (isAdmin) {
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
            SwitchRow("Oturum Sonunda Not Sor", cfg.sessionNotePrompt, isAdmin) {
                viewModel.updateConfig(cfg.copy(sessionNotePrompt = it))
            }
            SwitchRow("Konusurken 'Uzakta' Sayilsin", cfg.speakingCountsAsAway, isAdmin) {
                viewModel.updateConfig(cfg.copy(speakingCountsAsAway = it))
            }
            SwitchRow("Cihaz Yeniden Baslayinca Otomatik Baslat", cfg.autoStartOnBootEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(autoStartOnBootEnabled = it))
            }
            SwitchRow("Kapatilinca Otomatik Yeniden Baslat (Surekli Acik Kal)", cfg.keepAliveEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(keepAliveEnabled = it))
            }
        }

        SettingsGroup("Calisan Sistemler") {
            Text(
                "Her biri ayri bir izleme/analiz sistemini acar-kapar - kapatilan sistem hem " +
                    "arkaplanda calismayi hem gunluk yedege veri eklenmesini durdurur.",
                style = MaterialTheme.typography.bodySmall,
            )
            SwitchRow("Arkaplanda Takip", backgroundActive, isAdmin) { checked ->
                AppLogger.log("Ayarlar", "Arkaplanda Takip anahtari: $checked")
                if (checked) {
                    if (!hasCameraPermission) {
                        AppLogger.log("Ayarlar", "Kamera izni yok - izin isteniyor")
                        viewModel.reportCameraError("Arkaplan takibi icin once kamera iznini vermen gerekiyor.")
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                            try {
                                batteryOptimizationLauncher.launch(BatteryOptimizationHelper.requestIgnoreBatteryOptimizationsIntent(context))
                            } catch (e: Exception) {
                                AppLogger.logError("Ayarlar", "Pil optimizasyonu izin ekrani acilamadi", e)
                            }
                        }
                        try {
                            ContextCompat.startForegroundService(context, StudyForegroundService.startIntent(context))
                        } catch (e: Exception) {
                            AppLogger.logError("Ayarlar", "startForegroundService cagrisi basarisiz", e)
                            viewModel.reportCameraError("Arkaplan servisi baslatilamadi: ${e.message}")
                        }
                    }
                } else {
                    context.startService(StudyForegroundService.stopIntent(context))
                }
            }
            Text(
                "Bazi telefon markalarinda (Xiaomi/MIUI, Oppo/ColorOS, Huawei/EMUI, Samsung) arkaplan " +
                    "takibinin kesintisiz calismasi icin Ayarlar > Uygulamalar > Ders Calisma Takibi > " +
                    "Pil/Otomatik baslatma bolumunden uygulamaya izin vermen gerekebilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (com.derscalismatakibi.app.util.OemAutostartHelper.isKnownRestrictiveOem()) {
                Button(onClick = {
                    if (!com.derscalismatakibi.app.util.OemAutostartHelper.openAutostartSettings(context)) {
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    }
                }) { Text("Otomatik Baslatma Ayarlarini Ac") }
            }
            SwitchRow("Kamera / MediaPipe Analizi (calisma tespiti)", cfg.cameraAnalysisEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(cameraAnalysisEnabled = it))
            }
            SwitchRow("Konum Takibi (Guvenli Bolge + konum gecmisi)", cfg.locationTrackingEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(locationTrackingEnabled = it))
            }
            SwitchRow("Arama/SMS Ozeti Kaydi", cfg.callSmsLogEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(callSmsLogEnabled = it))
            }
            SwitchRow("Bildirim Erisimi Kaydi", cfg.notificationLogEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(notificationLogEnabled = it))
            }
        }

        SettingsGroup("Gonderilen Veriler") {
            Text(
                "Gunluk yedek e-postasina hangi dosyalarin eklenecegini secer - cihaza yazma " +
                    "bundan bagimsiz HER ZAMAN yapilir, bu sadece e-posta ekini kontrol eder.",
                style = MaterialTheme.typography.bodySmall,
            )
            SwitchRow("Calisma Oturumlari (CSV)", cfg.sendSessionCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendSessionCsv = it))
            }
            SwitchRow("Takvim (JSON)", cfg.sendScheduleCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendScheduleCsv = it))
            }
            SwitchRow("Uygulama Kullanimi (CSV)", cfg.sendUsageCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendUsageCsv = it))
            }
            SwitchRow("Arama/SMS Ozeti (CSV)", cfg.sendCallSmsCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendCallSmsCsv = it))
            }
            SwitchRow("Cihaz Raporu (TXT)", cfg.sendDeviceReport, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendDeviceReport = it))
            }
            SwitchRow("Uygulama Kilidi Listesi (TXT)", cfg.sendBlockedAppsTxt, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendBlockedAppsTxt = it))
            }
            SwitchRow("Uygulama Loglari", cfg.sendAppLog, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendAppLog = it))
            }
            SwitchRow("Konum Gecmisi (CSV)", cfg.sendLocationCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendLocationCsv = it))
            }
            SwitchRow("Klavye Takibi (CSV)", cfg.sendKeystrokeCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendKeystrokeCsv = it))
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

        if (isAdmin) {
            SettingsGroup("Yedekleme / E-posta") {
                var emailField by remember { mutableStateOf(cfg.backupEmail) }
                OutlinedTextField(
                    value = emailField,
                    onValueChange = { emailField = it; viewModel.updateConfig(cfg.copy(backupEmail = it)) },
                    label = { Text("Admin E-posta (gonderen ve alici)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                var passwordField by remember { mutableStateOf(cfg.backupEmailAppPassword) }
                OutlinedTextField(
                    value = passwordField,
                    onValueChange = { passwordField = it; viewModel.updateConfig(cfg.copy(backupEmailAppPassword = it)) },
                    label = { Text("Uygulama Sifresi (Gmail App Password)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Gmail icin 2 Adimli Dogrulama acik olmali, normal sifre degil " +
                        "'Uygulama Sifresi' kullan (myaccount.google.com/apppasswords).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var hourField by remember { mutableStateOf(cfg.backupHour.toString()) }
                    OutlinedTextField(
                        value = hourField,
                        onValueChange = { text ->
                            hourField = text
                            text.toIntOrNull()?.let { h ->
                                if (h in 0..23) {
                                    viewModel.updateConfig(cfg.copy(backupHour = h))
                                    BackupScheduler.reschedule(context, h, cfg.backupMinute)
                                }
                            }
                        },
                        label = { Text("Saat (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp),
                    )
                    var minuteField by remember { mutableStateOf(cfg.backupMinute.toString()) }
                    OutlinedTextField(
                        value = minuteField,
                        onValueChange = { text ->
                            minuteField = text
                            text.toIntOrNull()?.let { m ->
                                if (m in 0..59) {
                                    viewModel.updateConfig(cfg.copy(backupMinute = m))
                                    BackupScheduler.reschedule(context, cfg.backupHour, m)
                                }
                            }
                        },
                        label = { Text("Dakika (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp),
                    )
                }

                SwitchRow("Gunluk Otomatik Yedekleme", cfg.dailyBackupEnabled, isAdmin) {
                    viewModel.updateConfig(cfg.copy(dailyBackupEnabled = it))
                }

                val lastBackupText = if (cfg.lastBackupTimestamp > 0) {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(cfg.lastBackupTimestamp))
                    "Son yedekleme: $dateStr - ${cfg.lastBackupStatus}"
                } else {
                    "Son yedekleme: henuz yapilmadi"
                }
                Text(lastBackupText, style = MaterialTheme.typography.bodySmall)

                Button(onClick = { BackupScheduler.scheduleOneOffNow(context) }) {
                    Text("Simdi Yedekle")
                }
            }
        }

        if (isAdmin) {
        SettingsGroup("Bildirim Erisimi") {
            val nlContext = LocalContext.current
            val enabled = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(nlContext).contains(nlContext.packageName)
            Text(
                if (enabled) "Bildirim erisimi acik - diger uygulamalardan gelen bildirimler Loglar'a yaziliyor."
                else "Diger uygulamalardan gelen bildirimleri kaydetmek icin ozel bir izin gerekiyor (normal izin kutusuyla verilmez).",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!enabled) {
                Button(onClick = { nlContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                    Text("Ayarlara Git")
                }
            }
        }

        SettingsGroup("Cihaz Yoneticisi") {
            val dpmContext = LocalContext.current
            val dpm = dpmContext.getSystemService(android.app.admin.DevicePolicyManager::class.java)
            val adminComponent = android.content.ComponentName(dpmContext, com.derscalismatakibi.app.service.StudyDeviceAdminReceiver::class.java)
            val isAdminActive = dpm?.isAdminActive(adminComponent) == true
            Text(
                if (isAdminActive) "Aktif - uygulama, once bu izin Ayarlar'dan kapatilmadan kaldirilamaz."
                else "Kapali - aktif edilirse uygulamayi kaldirmadan once bu izni Ayarlar'dan kapatman gerekir (ekstra korumal).",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!isAdminActive && isAdmin) {
                Button(onClick = {
                    dpmContext.startActivity(
                        android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Ders Calisma Takibi'nin ebeveyn-denetim ayarlarindan kolayca kaldirilmamasi icin.",
                            )
                        },
                    )
                }) { Text("Etkinlestir") }
            }
        }

        SettingsGroup("Guncelleme") {
            Text(
                "Uygulama GitHub uzerinden dagitiliyor (Play Store degil). Yeni bir surum " +
                    "cikinca acilista otomatik haber verilir; istersen simdi de kontrol edebilirsin.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                enabled = !checkingUpdate,
                onClick = {
                    checkingUpdate = true
                    updateCheckMessage = null
                    scope.launch {
                        val currentVersion = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
                        } catch (e: Exception) {
                            "0.0.0"
                        }
                        val result = withContext(Dispatchers.IO) { UpdateChecker.checkForUpdate(currentVersion) }
                        checkingUpdate = false
                        if (result != null) {
                            updateInfo = result
                        } else {
                            updateCheckMessage = "En son surumdesin."
                        }
                    }
                },
            ) { Text(if (checkingUpdate) "Kontrol ediliyor..." else "Guncellemeleri Kontrol Et") }
            updateCheckMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        }

        SettingsGroup("Gizlilik") {
            Button(onClick = { showPrivacyDialog = true }) { Text("Gizlilik Politikasini Goruntule") }
        }
    }

    if (showPrivacyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showPrivacyDialog = false }) { Text("Kapat") }
            },
            title = { Text("Gizlilik Politikasi") },
            text = {
                Text(
                    com.derscalismatakibi.app.legal.PrivacyConsent.TEXT,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(
            info = info,
            onDismiss = { updateInfo = null },
            onInstall = {
                if (UpdateInstaller.canInstallUnknownApps(context)) {
                    UpdateInstaller.downloadAndInstall(context, info)
                    updateInfo = null
                } else {
                    showUnknownSourcesDialog = true
                }
            },
        )
    }

    if (showUnknownSourcesDialog) {
        UnknownSourcesDialog(
            onGoToSettings = {
                showUnknownSourcesDialog = false
                context.startActivity(UpdateInstaller.unknownAppsSettingsIntent(context))
            },
            onDismiss = { showUnknownSourcesDialog = false },
        )
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
    // weight(1f) olmadan uzun etiketler (orn. "Kapatilinca Otomatik Yeniden
    // Baslat (Surekli Acik Kal)") Switch'i ekran disina itip kesiyordu -
    // metin artik kalan alana sarip Switch her zaman gorunur/tikla nabilir kaliyor.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
