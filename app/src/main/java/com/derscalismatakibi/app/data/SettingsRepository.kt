package com.derscalismatakibi.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.derscalismatakibi.app.core.AppConfig
import com.derscalismatakibi.app.core.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "study_tracker_settings")

/**
 * study_tracker2.py -> load_config()/save_config() (config.json) karsiligi.
 * Her AppConfig alani icin ayri bir DataStore anahtari kullanilir; anahtar
 * yoksa AppConfig()'in varsayilani (masaustundeki DEFAULT_CONFIG ile ayni) kullanilir.
 */
class SettingsRepository(private val context: Context) {
    /** DataStore Flow-tabanli/asenkron oldugu icin Activity.attachBaseContext()
     * gibi tamamen senkron calisan yerlerde okunamiyor - dil secimi bu yuzden
     * ayrica duz SharedPreferences'a da yansitiliyor (bkz. LocalePrefs). */
    object LocalePrefs {
        private const val PREFS_NAME = "locale_prefs"
        private const val KEY_LANGUAGE = "app_language"

        fun read(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, "tr") ?: "tr"

        fun write(context: Context, language: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE, language).apply()
        }
    }

    private object Keys {
        val EAR = doublePreferencesKey("ear_closed_threshold")
        val YAW = doublePreferencesKey("yaw_max_deg")
        val PITCH_DOWN = doublePreferencesKey("pitch_down_max_deg")
        val PITCH_UP = doublePreferencesKey("pitch_up_max_deg")
        val CONFIRM_AWAY = doublePreferencesKey("confirm_away_seconds")
        val CONFIRM_SLEEP = doublePreferencesKey("confirm_sleep_seconds")
        val CONFIRM_RESUME = doublePreferencesKey("confirm_resume_seconds")
        val POMO_WORK = intPreferencesKey("pomodoro_work_min")
        val POMO_BREAK = intPreferencesKey("pomodoro_break_min")
        val POMO_LONG_BREAK = intPreferencesKey("pomodoro_long_break_min")
        val POMO_CYCLES = intPreferencesKey("pomodoro_cycles_before_long")
        val DAILY_GOAL = doublePreferencesKey("daily_goal_hours")
        val WEEKLY_GOAL = doublePreferencesKey("weekly_goal_hours")
        val AUTO_PAUSE_AWAY = booleanPreferencesKey("auto_pause_on_away")
        val AUTO_PAUSE_SLEEP = booleanPreferencesKey("auto_pause_on_sleep")
        val SESSION_NOTE_PROMPT = booleanPreferencesKey("session_note_prompt")
        val SPEAKING_AS_AWAY = booleanPreferencesKey("speaking_counts_as_away")
        val SPEAKING_STD = doublePreferencesKey("speaking_mar_std_threshold")
        val SPEAKING_MIN = doublePreferencesKey("speaking_mar_min_threshold")
        val SPEAKING_WINDOW = intPreferencesKey("speaking_window_size")
        val CONFIRM_SPEAKING = doublePreferencesKey("confirm_speaking_seconds")
        val FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
        val APP_PIN = stringPreferencesKey("app_pin")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ROUTINE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("routine_notifications_enabled")
        val APP_LOCK_ALERT_NOTIFICATIONS_ENABLED = booleanPreferencesKey("app_lock_alert_notifications_enabled")
        val SAFE_ZONE_ALERT_NOTIFICATIONS_ENABLED = booleanPreferencesKey("safe_zone_alert_notifications_enabled")
        val BACKUP_FAILURE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("backup_failure_notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val BACKUP_EMAIL = stringPreferencesKey("backup_email")
        val BACKUP_EMAIL_APP_PASSWORD = stringPreferencesKey("backup_email_app_password")
        val BACKUP_LABEL = stringPreferencesKey("backup_label")
        val DAILY_BACKUP_ENABLED = booleanPreferencesKey("daily_backup_enabled")
        val BACKUP_HOUR = intPreferencesKey("backup_hour")
        val BACKUP_MINUTE = intPreferencesKey("backup_minute")
        val INTERVAL_BACKUP_ENABLED = booleanPreferencesKey("interval_backup_enabled")
        val INTERVAL_BACKUP_MINUTES = intPreferencesKey("interval_backup_minutes")
        val INTERVAL_BACKUP_WIFI_ONLY = booleanPreferencesKey("interval_backup_wifi_only")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        val PRIVACY_CONSENT_ACCEPTED = booleanPreferencesKey("privacy_consent_accepted")
        val PRIVACY_CONSENT_VERSION = intPreferencesKey("privacy_consent_version")
        val PRIVACY_CONSENT_TIMESTAMP = longPreferencesKey("privacy_consent_timestamp")
        val EXAM_MODE_ENABLED = booleanPreferencesKey("exam_mode_enabled")
        val EXAM_ALLOWED_PACKAGES = stringPreferencesKey("exam_allowed_packages")
        val KEYBOARD_TRACKING_ENABLED = booleanPreferencesKey("keyboard_tracking_enabled")
        val AUTO_START_ON_BOOT_ENABLED = booleanPreferencesKey("auto_start_on_boot_enabled")
        val KEEP_ALIVE_ENABLED = booleanPreferencesKey("keep_alive_enabled")
        val SCREEN_PINNING_ENABLED = booleanPreferencesKey("screen_pinning_enabled")
        val CAMERA_ANALYSIS_ENABLED = booleanPreferencesKey("camera_analysis_enabled")
        val LOCATION_TRACKING_ENABLED = booleanPreferencesKey("location_tracking_enabled")
        val CALL_SMS_LOG_ENABLED = booleanPreferencesKey("call_sms_log_enabled")
        val NOTIFICATION_LOG_ENABLED = booleanPreferencesKey("notification_log_enabled")
        val USAGE_CHECK_INTERVAL_SECONDS = intPreferencesKey("usage_check_interval_seconds")
        val SEND_SESSION_CSV = booleanPreferencesKey("send_session_csv")
        val SEND_SCHEDULE_CSV = booleanPreferencesKey("send_schedule_csv")
        val SEND_USAGE_CSV = booleanPreferencesKey("send_usage_csv")
        val SEND_CALL_SMS_CSV = booleanPreferencesKey("send_call_sms_csv")
        val SEND_DEVICE_REPORT = booleanPreferencesKey("send_device_report")
        val SEND_BLOCKED_APPS_TXT = booleanPreferencesKey("send_blocked_apps_txt")
        val SEND_APP_LOG = booleanPreferencesKey("send_app_log")
        val SEND_LOCATION_CSV = booleanPreferencesKey("send_location_csv")
        val SEND_KEYSTROKE_CSV = booleanPreferencesKey("send_keystroke_csv")
        val CURRENT_ROLE = stringPreferencesKey("current_role")
    }

    /** Ebeveyn/Ogrenci modu (Role) - AppConfig'in disinda, kendi kucuk
     * anahtariyla ayrica saklanir (AppConfig().copy(...) ile her ayar
     * degisikliginde yeniden yazilan buyuk update()'e baglamamak icin).
     * Persist edilmezse her process restart/reboot/update'te sessizce
     * Role.ADMIN'e (kilit acik) donuyordu - bu KRITIK bir bug idi. */
    val roleFlow: Flow<Role> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.CURRENT_ROLE] == Role.STUDENT.name) Role.STUDENT else Role.ADMIN
    }

    suspend fun saveRole(role: Role) {
        context.dataStore.edit { prefs -> prefs[Keys.CURRENT_ROLE] = role.name }
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        val defaults = AppConfig()
        AppConfig(
            earClosedThreshold = prefs[Keys.EAR] ?: defaults.earClosedThreshold,
            yawMaxDeg = prefs[Keys.YAW] ?: defaults.yawMaxDeg,
            pitchDownMaxDeg = prefs[Keys.PITCH_DOWN] ?: defaults.pitchDownMaxDeg,
            pitchUpMaxDeg = prefs[Keys.PITCH_UP] ?: defaults.pitchUpMaxDeg,
            confirmAwaySeconds = prefs[Keys.CONFIRM_AWAY] ?: defaults.confirmAwaySeconds,
            confirmSleepSeconds = prefs[Keys.CONFIRM_SLEEP] ?: defaults.confirmSleepSeconds,
            confirmResumeSeconds = prefs[Keys.CONFIRM_RESUME] ?: defaults.confirmResumeSeconds,
            pomodoroWorkMin = prefs[Keys.POMO_WORK] ?: defaults.pomodoroWorkMin,
            pomodoroBreakMin = prefs[Keys.POMO_BREAK] ?: defaults.pomodoroBreakMin,
            pomodoroLongBreakMin = prefs[Keys.POMO_LONG_BREAK] ?: defaults.pomodoroLongBreakMin,
            pomodoroCyclesBeforeLong = prefs[Keys.POMO_CYCLES] ?: defaults.pomodoroCyclesBeforeLong,
            dailyGoalHours = prefs[Keys.DAILY_GOAL] ?: defaults.dailyGoalHours,
            weeklyGoalHours = prefs[Keys.WEEKLY_GOAL] ?: defaults.weeklyGoalHours,
            autoPauseOnAway = prefs[Keys.AUTO_PAUSE_AWAY] ?: defaults.autoPauseOnAway,
            autoPauseOnSleep = prefs[Keys.AUTO_PAUSE_SLEEP] ?: defaults.autoPauseOnSleep,
            sessionNotePrompt = prefs[Keys.SESSION_NOTE_PROMPT] ?: defaults.sessionNotePrompt,
            speakingCountsAsAway = prefs[Keys.SPEAKING_AS_AWAY] ?: defaults.speakingCountsAsAway,
            speakingMarStdThreshold = prefs[Keys.SPEAKING_STD] ?: defaults.speakingMarStdThreshold,
            speakingMarMinThreshold = prefs[Keys.SPEAKING_MIN] ?: defaults.speakingMarMinThreshold,
            speakingWindowSize = prefs[Keys.SPEAKING_WINDOW] ?: defaults.speakingWindowSize,
            confirmSpeakingSeconds = prefs[Keys.CONFIRM_SPEAKING] ?: defaults.confirmSpeakingSeconds,
            useFrontCamera = prefs[Keys.FRONT_CAMERA] ?: defaults.useFrontCamera,
            appPin = prefs[Keys.APP_PIN] ?: defaults.appPin,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: defaults.soundEnabled,
            routineNotificationsEnabled = prefs[Keys.ROUTINE_NOTIFICATIONS_ENABLED] ?: defaults.routineNotificationsEnabled,
            appLockAlertNotificationsEnabled = prefs[Keys.APP_LOCK_ALERT_NOTIFICATIONS_ENABLED] ?: defaults.appLockAlertNotificationsEnabled,
            safeZoneAlertNotificationsEnabled = prefs[Keys.SAFE_ZONE_ALERT_NOTIFICATIONS_ENABLED] ?: defaults.safeZoneAlertNotificationsEnabled,
            backupFailureNotificationsEnabled = prefs[Keys.BACKUP_FAILURE_NOTIFICATIONS_ENABLED] ?: defaults.backupFailureNotificationsEnabled,
            themeMode = prefs[Keys.THEME_MODE] ?: defaults.themeMode,
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: defaults.appLanguage,
            backupEmail = prefs[Keys.BACKUP_EMAIL] ?: defaults.backupEmail,
            backupEmailAppPassword = prefs[Keys.BACKUP_EMAIL_APP_PASSWORD] ?: defaults.backupEmailAppPassword,
            backupLabel = prefs[Keys.BACKUP_LABEL] ?: defaults.backupLabel,
            dailyBackupEnabled = prefs[Keys.DAILY_BACKUP_ENABLED] ?: defaults.dailyBackupEnabled,
            backupHour = prefs[Keys.BACKUP_HOUR] ?: defaults.backupHour,
            backupMinute = prefs[Keys.BACKUP_MINUTE] ?: defaults.backupMinute,
            intervalBackupEnabled = prefs[Keys.INTERVAL_BACKUP_ENABLED] ?: defaults.intervalBackupEnabled,
            intervalBackupMinutes = prefs[Keys.INTERVAL_BACKUP_MINUTES] ?: defaults.intervalBackupMinutes,
            intervalBackupWifiOnly = prefs[Keys.INTERVAL_BACKUP_WIFI_ONLY] ?: defaults.intervalBackupWifiOnly,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: defaults.lastBackupTimestamp,
            lastBackupStatus = prefs[Keys.LAST_BACKUP_STATUS] ?: defaults.lastBackupStatus,
            privacyConsentAccepted = prefs[Keys.PRIVACY_CONSENT_ACCEPTED] ?: defaults.privacyConsentAccepted,
            privacyConsentVersion = prefs[Keys.PRIVACY_CONSENT_VERSION] ?: defaults.privacyConsentVersion,
            privacyConsentTimestamp = prefs[Keys.PRIVACY_CONSENT_TIMESTAMP] ?: defaults.privacyConsentTimestamp,
            examModeEnabled = prefs[Keys.EXAM_MODE_ENABLED] ?: defaults.examModeEnabled,
            examAllowedPackages = prefs[Keys.EXAM_ALLOWED_PACKAGES] ?: defaults.examAllowedPackages,
            keyboardTrackingEnabled = prefs[Keys.KEYBOARD_TRACKING_ENABLED] ?: defaults.keyboardTrackingEnabled,
            autoStartOnBootEnabled = prefs[Keys.AUTO_START_ON_BOOT_ENABLED] ?: defaults.autoStartOnBootEnabled,
            keepAliveEnabled = prefs[Keys.KEEP_ALIVE_ENABLED] ?: defaults.keepAliveEnabled,
            screenPinningEnabled = prefs[Keys.SCREEN_PINNING_ENABLED] ?: defaults.screenPinningEnabled,
            cameraAnalysisEnabled = prefs[Keys.CAMERA_ANALYSIS_ENABLED] ?: defaults.cameraAnalysisEnabled,
            locationTrackingEnabled = prefs[Keys.LOCATION_TRACKING_ENABLED] ?: defaults.locationTrackingEnabled,
            callSmsLogEnabled = prefs[Keys.CALL_SMS_LOG_ENABLED] ?: defaults.callSmsLogEnabled,
            notificationLogEnabled = prefs[Keys.NOTIFICATION_LOG_ENABLED] ?: defaults.notificationLogEnabled,
            usageCheckIntervalSeconds = prefs[Keys.USAGE_CHECK_INTERVAL_SECONDS] ?: defaults.usageCheckIntervalSeconds,
            sendSessionCsv = prefs[Keys.SEND_SESSION_CSV] ?: defaults.sendSessionCsv,
            sendScheduleCsv = prefs[Keys.SEND_SCHEDULE_CSV] ?: defaults.sendScheduleCsv,
            sendUsageCsv = prefs[Keys.SEND_USAGE_CSV] ?: defaults.sendUsageCsv,
            sendCallSmsCsv = prefs[Keys.SEND_CALL_SMS_CSV] ?: defaults.sendCallSmsCsv,
            sendDeviceReport = prefs[Keys.SEND_DEVICE_REPORT] ?: defaults.sendDeviceReport,
            sendBlockedAppsTxt = prefs[Keys.SEND_BLOCKED_APPS_TXT] ?: defaults.sendBlockedAppsTxt,
            sendAppLog = prefs[Keys.SEND_APP_LOG] ?: defaults.sendAppLog,
            sendLocationCsv = prefs[Keys.SEND_LOCATION_CSV] ?: defaults.sendLocationCsv,
            sendKeystrokeCsv = prefs[Keys.SEND_KEYSTROKE_CSV] ?: defaults.sendKeystrokeCsv,
        )
    }

    suspend fun update(cfg: AppConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EAR] = cfg.earClosedThreshold
            prefs[Keys.YAW] = cfg.yawMaxDeg
            prefs[Keys.PITCH_DOWN] = cfg.pitchDownMaxDeg
            prefs[Keys.PITCH_UP] = cfg.pitchUpMaxDeg
            prefs[Keys.CONFIRM_AWAY] = cfg.confirmAwaySeconds
            prefs[Keys.CONFIRM_SLEEP] = cfg.confirmSleepSeconds
            prefs[Keys.CONFIRM_RESUME] = cfg.confirmResumeSeconds
            prefs[Keys.POMO_WORK] = cfg.pomodoroWorkMin
            prefs[Keys.POMO_BREAK] = cfg.pomodoroBreakMin
            prefs[Keys.POMO_LONG_BREAK] = cfg.pomodoroLongBreakMin
            prefs[Keys.POMO_CYCLES] = cfg.pomodoroCyclesBeforeLong
            prefs[Keys.DAILY_GOAL] = cfg.dailyGoalHours
            prefs[Keys.WEEKLY_GOAL] = cfg.weeklyGoalHours
            prefs[Keys.AUTO_PAUSE_AWAY] = cfg.autoPauseOnAway
            prefs[Keys.AUTO_PAUSE_SLEEP] = cfg.autoPauseOnSleep
            prefs[Keys.SESSION_NOTE_PROMPT] = cfg.sessionNotePrompt
            prefs[Keys.SPEAKING_AS_AWAY] = cfg.speakingCountsAsAway
            prefs[Keys.SPEAKING_STD] = cfg.speakingMarStdThreshold
            prefs[Keys.SPEAKING_MIN] = cfg.speakingMarMinThreshold
            prefs[Keys.SPEAKING_WINDOW] = cfg.speakingWindowSize
            prefs[Keys.CONFIRM_SPEAKING] = cfg.confirmSpeakingSeconds
            prefs[Keys.FRONT_CAMERA] = cfg.useFrontCamera
            prefs[Keys.APP_PIN] = cfg.appPin
            prefs[Keys.SOUND_ENABLED] = cfg.soundEnabled
            prefs[Keys.ROUTINE_NOTIFICATIONS_ENABLED] = cfg.routineNotificationsEnabled
            prefs[Keys.APP_LOCK_ALERT_NOTIFICATIONS_ENABLED] = cfg.appLockAlertNotificationsEnabled
            prefs[Keys.SAFE_ZONE_ALERT_NOTIFICATIONS_ENABLED] = cfg.safeZoneAlertNotificationsEnabled
            prefs[Keys.BACKUP_FAILURE_NOTIFICATIONS_ENABLED] = cfg.backupFailureNotificationsEnabled
            prefs[Keys.THEME_MODE] = cfg.themeMode
            prefs[Keys.APP_LANGUAGE] = cfg.appLanguage
            prefs[Keys.BACKUP_EMAIL] = cfg.backupEmail
            prefs[Keys.BACKUP_EMAIL_APP_PASSWORD] = cfg.backupEmailAppPassword
            prefs[Keys.BACKUP_LABEL] = cfg.backupLabel
            prefs[Keys.DAILY_BACKUP_ENABLED] = cfg.dailyBackupEnabled
            prefs[Keys.BACKUP_HOUR] = cfg.backupHour
            prefs[Keys.BACKUP_MINUTE] = cfg.backupMinute
            prefs[Keys.INTERVAL_BACKUP_ENABLED] = cfg.intervalBackupEnabled
            prefs[Keys.INTERVAL_BACKUP_MINUTES] = cfg.intervalBackupMinutes
            prefs[Keys.INTERVAL_BACKUP_WIFI_ONLY] = cfg.intervalBackupWifiOnly
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = cfg.lastBackupTimestamp
            prefs[Keys.LAST_BACKUP_STATUS] = cfg.lastBackupStatus
            prefs[Keys.PRIVACY_CONSENT_ACCEPTED] = cfg.privacyConsentAccepted
            prefs[Keys.PRIVACY_CONSENT_VERSION] = cfg.privacyConsentVersion
            prefs[Keys.PRIVACY_CONSENT_TIMESTAMP] = cfg.privacyConsentTimestamp
            prefs[Keys.EXAM_MODE_ENABLED] = cfg.examModeEnabled
            prefs[Keys.EXAM_ALLOWED_PACKAGES] = cfg.examAllowedPackages
            prefs[Keys.KEYBOARD_TRACKING_ENABLED] = cfg.keyboardTrackingEnabled
            prefs[Keys.AUTO_START_ON_BOOT_ENABLED] = cfg.autoStartOnBootEnabled
            prefs[Keys.KEEP_ALIVE_ENABLED] = cfg.keepAliveEnabled
            prefs[Keys.SCREEN_PINNING_ENABLED] = cfg.screenPinningEnabled
            prefs[Keys.CAMERA_ANALYSIS_ENABLED] = cfg.cameraAnalysisEnabled
            prefs[Keys.LOCATION_TRACKING_ENABLED] = cfg.locationTrackingEnabled
            prefs[Keys.CALL_SMS_LOG_ENABLED] = cfg.callSmsLogEnabled
            prefs[Keys.NOTIFICATION_LOG_ENABLED] = cfg.notificationLogEnabled
            prefs[Keys.USAGE_CHECK_INTERVAL_SECONDS] = cfg.usageCheckIntervalSeconds
            prefs[Keys.SEND_SESSION_CSV] = cfg.sendSessionCsv
            prefs[Keys.SEND_SCHEDULE_CSV] = cfg.sendScheduleCsv
            prefs[Keys.SEND_USAGE_CSV] = cfg.sendUsageCsv
            prefs[Keys.SEND_CALL_SMS_CSV] = cfg.sendCallSmsCsv
            prefs[Keys.SEND_DEVICE_REPORT] = cfg.sendDeviceReport
            prefs[Keys.SEND_BLOCKED_APPS_TXT] = cfg.sendBlockedAppsTxt
            prefs[Keys.SEND_APP_LOG] = cfg.sendAppLog
            prefs[Keys.SEND_LOCATION_CSV] = cfg.sendLocationCsv
            prefs[Keys.SEND_KEYSTROKE_CSV] = cfg.sendKeystrokeCsv
        }
        LocalePrefs.write(context, cfg.appLanguage)
    }
}
