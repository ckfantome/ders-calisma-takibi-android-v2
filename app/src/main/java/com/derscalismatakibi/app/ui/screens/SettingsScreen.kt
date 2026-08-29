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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.backup.BackupScheduler
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.UpdateChecker
import com.derscalismatakibi.app.service.StudyForegroundService
import com.derscalismatakibi.app.ui.UnknownSourcesDialog
import com.derscalismatakibi.app.ui.UpdateAvailableDialog
import com.derscalismatakibi.app.ui.rememberResumeTrigger
import com.derscalismatakibi.app.util.AccessibilityHelper
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
    // Klavye Takibi'nin calismasi icin gereken Erisilebilirlik izni onceden
    // sadece Uygulama Kilidi ekraninda gorunup ayarlanabiliyordu - buradan
    // (Calisan Sistemler) acmaya calisan biri izin durumunu hic goremiyordu.
    val resumeTrigger = rememberResumeTrigger()
    var hasAccessibility by remember { mutableStateOf(AccessibilityHelper.isAppBlockServiceEnabled(context)) }
    androidx.compose.runtime.LaunchedEffect(resumeTrigger) {
        hasAccessibility = AccessibilityHelper.isAppBlockServiceEnabled(context)
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        if (!isAdmin) {
            Text(
                stringResource(R.string.settings_student_mode_notice),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Ogrenci modunda sistemi etkilemeyen bu ucu HER ZAMAN gorunur+degistirilebilir
        // kalir (kullaniciyla netlesen karar) - geri kalan gruplarin tamami asagida
        // isAdmin ile TAMAMEN gizlenir (salt-okunur degil, hic gorunmez).
        SettingsGroup(stringResource(R.string.settings_group_theme)) {
            val options = listOf("system" to stringResource(R.string.settings_theme_system), "dark" to stringResource(R.string.settings_theme_dark), "light" to stringResource(R.string.settings_theme_light))
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

        SettingsGroup(stringResource(R.string.settings_group_language)) {
            val langOptions = listOf("tr" to "Türkçe", "en" to "English")
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
                stringResource(R.string.settings_language_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SettingsGroup(stringResource(R.string.settings_group_sound_notifications)) {
            SwitchRow(stringResource(R.string.settings_sound_alert), cfg.soundEnabled, true) {
                viewModel.updateConfig(cfg.copy(soundEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_routine_reminders), cfg.routineNotificationsEnabled, true) {
                viewModel.updateConfig(cfg.copy(routineNotificationsEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_app_lock_exam_alert), cfg.appLockAlertNotificationsEnabled, true) {
                viewModel.updateConfig(cfg.copy(appLockAlertNotificationsEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_safe_zone_alert), cfg.safeZoneAlertNotificationsEnabled, true) {
                viewModel.updateConfig(cfg.copy(safeZoneAlertNotificationsEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_backup_failure_alert), cfg.backupFailureNotificationsEnabled, true) {
                viewModel.updateConfig(cfg.copy(backupFailureNotificationsEnabled = it))
            }
        }

        if (isAdmin) {
        SettingsGroup(stringResource(R.string.settings_group_detection_thresholds)) {
            LabeledSlider(stringResource(R.string.settings_ear_threshold), cfg.earClosedThreshold, 0.05f, 0.5f, isAdmin) {
                viewModel.updateConfig(cfg.copy(earClosedThreshold = it.toDouble()))
            }
            LabeledSlider(stringResource(R.string.settings_max_yaw), cfg.yawMaxDeg, 5f, 90f, isAdmin) {
                viewModel.updateConfig(cfg.copy(yawMaxDeg = it.toDouble()))
            }
            LabeledSlider(stringResource(R.string.settings_max_pitch_down), cfg.pitchDownMaxDeg, 5f, 90f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pitchDownMaxDeg = it.toDouble()))
            }
            LabeledSlider(stringResource(R.string.settings_max_pitch_up), cfg.pitchUpMaxDeg, 5f, 90f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pitchUpMaxDeg = it.toDouble()))
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_confirm_durations)) {
            LabeledSlider(stringResource(R.string.settings_confirm_away), cfg.confirmAwaySeconds, 1f, 30f, isAdmin) {
                viewModel.updateConfig(cfg.copy(confirmAwaySeconds = it.toDouble()))
            }
            LabeledSlider(stringResource(R.string.settings_confirm_sleep), cfg.confirmSleepSeconds, 1f, 30f, isAdmin) {
                viewModel.updateConfig(cfg.copy(confirmSleepSeconds = it.toDouble()))
            }
            LabeledSlider(stringResource(R.string.settings_confirm_resume), cfg.confirmResumeSeconds, 0.5f, 15f, isAdmin) {
                viewModel.updateConfig(cfg.copy(confirmResumeSeconds = it.toDouble()))
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_pomodoro)) {
            LabeledSlider(stringResource(R.string.settings_pomodoro_work), cfg.pomodoroWorkMin.toFloat(), 1f, 120f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroWorkMin = it.toInt()))
            }
            LabeledSlider(stringResource(R.string.schedule_kind_break), cfg.pomodoroBreakMin.toFloat(), 1f, 60f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroBreakMin = it.toInt()))
            }
            LabeledSlider(stringResource(R.string.settings_pomodoro_long_break), cfg.pomodoroLongBreakMin.toFloat(), 1f, 120f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroLongBreakMin = it.toInt()))
            }
            LabeledSlider(stringResource(R.string.settings_pomodoro_cycles), cfg.pomodoroCyclesBeforeLong.toFloat(), 1f, 12f, isAdmin) {
                viewModel.updateConfig(cfg.copy(pomodoroCyclesBeforeLong = it.toInt()))
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_goals)) {
            LabeledSlider(stringResource(R.string.settings_daily_goal), cfg.dailyGoalHours, 0.5f, 24f, isAdmin) {
                viewModel.updateConfig(cfg.copy(dailyGoalHours = it.toDouble()))
            }
            LabeledSlider(stringResource(R.string.settings_weekly_goal), cfg.weeklyGoalHours, 1f, 168f, isAdmin) {
                viewModel.updateConfig(cfg.copy(weeklyGoalHours = it.toDouble()))
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_general)) {
            SwitchRow(stringResource(R.string.settings_auto_pause_away), cfg.autoPauseOnAway, isAdmin) {
                viewModel.updateConfig(cfg.copy(autoPauseOnAway = it))
            }
            SwitchRow(stringResource(R.string.settings_auto_pause_sleep), cfg.autoPauseOnSleep, isAdmin) {
                viewModel.updateConfig(cfg.copy(autoPauseOnSleep = it))
            }
            SwitchRow(stringResource(R.string.settings_use_front_camera), cfg.useFrontCamera, isAdmin) {
                viewModel.updateConfig(cfg.copy(useFrontCamera = it))
            }
            SwitchRow(stringResource(R.string.settings_note_prompt_on_end), cfg.sessionNotePrompt, isAdmin) {
                viewModel.updateConfig(cfg.copy(sessionNotePrompt = it))
            }
            SwitchRow(stringResource(R.string.settings_speaking_counts_as_away), cfg.speakingCountsAsAway, isAdmin) {
                viewModel.updateConfig(cfg.copy(speakingCountsAsAway = it))
            }
            SwitchRow(stringResource(R.string.settings_auto_start_on_boot), cfg.autoStartOnBootEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(autoStartOnBootEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_keep_alive), cfg.keepAliveEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(keepAliveEnabled = it))
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_active_systems)) {
            Text(
                stringResource(R.string.settings_active_systems_explanation),
                style = MaterialTheme.typography.bodySmall,
            )
            val cameraPermissionNeededMessage = stringResource(R.string.settings_camera_permission_needed_for_background)
            val backgroundServiceStartFailedPrefix = stringResource(R.string.settings_background_service_start_failed)
            SwitchRow(stringResource(R.string.settings_background_tracking), backgroundActive, isAdmin) { checked ->
                AppLogger.log("Ayarlar", "Arkaplanda Takip anahtari: $checked")
                if (checked) {
                    if (!hasCameraPermission) {
                        AppLogger.log("Ayarlar", "Kamera izni yok - izin isteniyor")
                        viewModel.reportCameraError(cameraPermissionNeededMessage)
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
                            viewModel.reportCameraError("$backgroundServiceStartFailedPrefix: ${e.message}")
                        }
                    }
                } else {
                    context.startService(StudyForegroundService.stopIntent(context))
                }
            }
            Text(
                stringResource(R.string.settings_oem_battery_note),
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
                }) { Text(stringResource(R.string.app_block_open_autostart_settings)) }
            }
            SwitchRow(stringResource(R.string.settings_camera_analysis), cfg.cameraAnalysisEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(cameraAnalysisEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_location_tracking), cfg.locationTrackingEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(locationTrackingEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_call_sms_log), cfg.callSmsLogEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(callSmsLogEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_notification_log), cfg.notificationLogEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(notificationLogEnabled = it))
            }
            SwitchRow(stringResource(R.string.settings_keyboard_tracking), cfg.keyboardTrackingEnabled, isAdmin) {
                viewModel.updateConfig(cfg.copy(keyboardTrackingEnabled = it))
            }
            Text(
                stringResource(R.string.settings_keyboard_tracking_note),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!hasAccessibility) {
                Text(
                    stringResource(R.string.settings_accessibility_not_granted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                    Text(stringResource(R.string.settings_grant_accessibility))
                }
            } else {
                Text(stringResource(R.string.settings_accessibility_granted), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.settings_usage_check_frequency_title), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.settings_usage_check_frequency_explanation),
                style = MaterialTheme.typography.bodySmall,
            )
            val usageIntervalOptions = listOf(0 to stringResource(R.string.settings_usage_interval_instant), 10 to stringResource(R.string.settings_usage_interval_normal), 60 to stringResource(R.string.settings_usage_interval_battery_friendly))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                usageIntervalOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = cfg.usageCheckIntervalSeconds == value,
                        onClick = { if (isAdmin) viewModel.updateConfig(cfg.copy(usageCheckIntervalSeconds = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = usageIntervalOptions.size),
                        enabled = isAdmin,
                    ) { Text(label) }
                }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_sent_data)) {
            Text(
                stringResource(R.string.settings_sent_data_explanation),
                style = MaterialTheme.typography.bodySmall,
            )
            SwitchRow(stringResource(R.string.settings_send_session_csv), cfg.sendSessionCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendSessionCsv = it))
            }
            SwitchRow(stringResource(R.string.settings_send_schedule_json), cfg.sendScheduleCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendScheduleCsv = it))
            }
            SwitchRow(stringResource(R.string.settings_send_usage_csv), cfg.sendUsageCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendUsageCsv = it))
            }
            SwitchRow(stringResource(R.string.settings_send_call_sms_csv), cfg.sendCallSmsCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendCallSmsCsv = it))
            }
            SwitchRow(stringResource(R.string.settings_send_device_report), cfg.sendDeviceReport, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendDeviceReport = it))
            }
            SwitchRow(stringResource(R.string.settings_send_blocked_apps_txt), cfg.sendBlockedAppsTxt, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendBlockedAppsTxt = it))
            }
            SwitchRow(stringResource(R.string.settings_send_app_log), cfg.sendAppLog, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendAppLog = it))
            }
            SwitchRow(stringResource(R.string.settings_send_location_csv), cfg.sendLocationCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendLocationCsv = it))
            }
            SwitchRow(stringResource(R.string.settings_send_keystroke_csv), cfg.sendKeystrokeCsv, isAdmin) {
                viewModel.updateConfig(cfg.copy(sendKeystrokeCsv = it))
            }
        }
        }

        if (isAdmin) {
            SettingsGroup(stringResource(R.string.settings_group_admin_pin)) {
                var pinField by remember { mutableStateOf(cfg.appPin) }
                OutlinedTextField(
                    value = pinField,
                    onValueChange = { pinField = it; viewModel.updateConfig(cfg.copy(appPin = it)) },
                    label = { Text(stringResource(R.string.settings_admin_pin_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        }

        if (isAdmin) {
            SettingsGroup(stringResource(R.string.settings_group_backup_email)) {
                var emailField by remember { mutableStateOf(cfg.backupEmail) }
                OutlinedTextField(
                    value = emailField,
                    onValueChange = { emailField = it; viewModel.updateConfig(cfg.copy(backupEmail = it)) },
                    label = { Text(stringResource(R.string.settings_admin_email_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                var passwordField by remember { mutableStateOf(cfg.backupEmailAppPassword) }
                OutlinedTextField(
                    value = passwordField,
                    onValueChange = { passwordField = it; viewModel.updateConfig(cfg.copy(backupEmailAppPassword = it)) },
                    label = { Text(stringResource(R.string.settings_app_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.settings_gmail_app_password_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var labelField by remember { mutableStateOf(cfg.backupLabel) }
                OutlinedTextField(
                    value = labelField,
                    onValueChange = { labelField = it; viewModel.updateConfig(cfg.copy(backupLabel = it)) },
                    label = { Text(stringResource(R.string.settings_backup_label_field)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.settings_backup_label_explanation),
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
                        label = { Text(stringResource(R.string.settings_backup_hour_label)) },
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
                        label = { Text(stringResource(R.string.settings_backup_minute_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp),
                    )
                }

                SwitchRow(stringResource(R.string.settings_daily_backup_enabled), cfg.dailyBackupEnabled, isAdmin) {
                    viewModel.updateConfig(cfg.copy(dailyBackupEnabled = it))
                }

                SwitchRow(stringResource(R.string.settings_interval_backup_enabled), cfg.intervalBackupEnabled, isAdmin) { enabled ->
                    viewModel.updateConfig(cfg.copy(intervalBackupEnabled = enabled))
                    BackupScheduler.rescheduleInterval(context, enabled, cfg.intervalBackupMinutes, cfg.intervalBackupWifiOnly)
                }
                if (cfg.intervalBackupEnabled) {
                    var intervalMinutesField by remember { mutableStateOf(cfg.intervalBackupMinutes.toString()) }
                    OutlinedTextField(
                        value = intervalMinutesField,
                        onValueChange = { text ->
                            intervalMinutesField = text
                            text.toIntOrNull()?.let { m ->
                                if (m >= 15) {
                                    viewModel.updateConfig(cfg.copy(intervalBackupMinutes = m))
                                    BackupScheduler.rescheduleInterval(context, true, m, cfg.intervalBackupWifiOnly)
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.settings_interval_minutes_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(200.dp),
                    )
                    SwitchRow(stringResource(R.string.settings_wifi_only), cfg.intervalBackupWifiOnly, isAdmin) { wifiOnly ->
                        viewModel.updateConfig(cfg.copy(intervalBackupWifiOnly = wifiOnly))
                        BackupScheduler.rescheduleInterval(context, true, cfg.intervalBackupMinutes, wifiOnly)
                    }
                    Text(
                        stringResource(R.string.settings_interval_wifi_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val lastBackupText = if (cfg.lastBackupTimestamp > 0) {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(cfg.lastBackupTimestamp))
                    stringResource(R.string.settings_last_backup_at, dateStr, cfg.lastBackupStatus)
                } else {
                    stringResource(R.string.settings_last_backup_never)
                }
                Text(lastBackupText, style = MaterialTheme.typography.bodySmall)

                Button(onClick = { BackupScheduler.scheduleOneOffNow(context) }) {
                    Text(stringResource(R.string.settings_backup_now))
                }
            }
        }

        if (isAdmin) {
        SettingsGroup(stringResource(R.string.settings_group_notification_access)) {
            val nlContext = LocalContext.current
            val enabled = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(nlContext).contains(nlContext.packageName)
            Text(
                if (enabled) stringResource(R.string.settings_notification_access_on)
                else stringResource(R.string.settings_notification_access_off),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!enabled) {
                Button(onClick = { nlContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                    Text(stringResource(R.string.usage_stats_go_to_settings))
                }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_device_admin)) {
            val dpmContext = LocalContext.current
            val dpm = dpmContext.getSystemService(android.app.admin.DevicePolicyManager::class.java)
            val adminComponent = android.content.ComponentName(dpmContext, com.derscalismatakibi.app.service.StudyDeviceAdminReceiver::class.java)
            val isAdminActive = dpm?.isAdminActive(adminComponent) == true
            Text(
                if (isAdminActive) stringResource(R.string.settings_device_admin_active)
                else stringResource(R.string.settings_device_admin_inactive),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!isAdminActive && isAdmin) {
                val deviceAdminExplanation = stringResource(R.string.settings_device_admin_explanation)
                Button(onClick = {
                    dpmContext.startActivity(
                        android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                deviceAdminExplanation,
                            )
                        },
                    )
                }) { Text(stringResource(R.string.settings_device_admin_enable)) }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_update)) {
            Text(
                stringResource(R.string.settings_update_explanation),
                style = MaterialTheme.typography.bodySmall,
            )
            val upToDateMessage = stringResource(R.string.settings_update_up_to_date)
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
                            updateCheckMessage = upToDateMessage
                        }
                    }
                },
            ) { Text(if (checkingUpdate) stringResource(R.string.settings_update_checking) else stringResource(R.string.settings_update_check)) }
            updateCheckMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        }

        SettingsGroup(stringResource(R.string.settings_group_privacy)) {
            Button(onClick = { showPrivacyDialog = true }) { Text(stringResource(R.string.settings_view_privacy_policy)) }
        }
    }

    if (showPrivacyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showPrivacyDialog = false }) { Text(stringResource(R.string.action_close)) }
            },
            title = { Text(stringResource(R.string.settings_group_privacy_policy_title)) },
            text = {
                Text(
                    com.derscalismatakibi.app.legal.PrivacyConsent.text(context),
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
