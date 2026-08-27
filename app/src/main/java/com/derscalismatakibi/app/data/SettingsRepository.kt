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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "study_tracker_settings")

/**
 * study_tracker2.py -> load_config()/save_config() (config.json) karsiligi.
 * Her AppConfig alani icin ayri bir DataStore anahtari kullanilir; anahtar
 * yoksa AppConfig()'in varsayilani (masaustundeki DEFAULT_CONFIG ile ayni) kullanilir.
 */
class SettingsRepository(private val context: Context) {
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
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val BACKUP_EMAIL = stringPreferencesKey("backup_email")
        val BACKUP_EMAIL_APP_PASSWORD = stringPreferencesKey("backup_email_app_password")
        val DAILY_BACKUP_ENABLED = booleanPreferencesKey("daily_backup_enabled")
        val BACKUP_HOUR = intPreferencesKey("backup_hour")
        val BACKUP_MINUTE = intPreferencesKey("backup_minute")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        val PRIVACY_CONSENT_ACCEPTED = booleanPreferencesKey("privacy_consent_accepted")
        val PRIVACY_CONSENT_VERSION = intPreferencesKey("privacy_consent_version")
        val PRIVACY_CONSENT_TIMESTAMP = longPreferencesKey("privacy_consent_timestamp")
        val EXAM_MODE_ENABLED = booleanPreferencesKey("exam_mode_enabled")
        val EXAM_ALLOWED_PACKAGES = stringPreferencesKey("exam_allowed_packages")
        val KEYBOARD_TRACKING_ENABLED = booleanPreferencesKey("keyboard_tracking_enabled")
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
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: defaults.notificationsEnabled,
            themeMode = prefs[Keys.THEME_MODE] ?: defaults.themeMode,
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: defaults.appLanguage,
            backupEmail = prefs[Keys.BACKUP_EMAIL] ?: defaults.backupEmail,
            backupEmailAppPassword = prefs[Keys.BACKUP_EMAIL_APP_PASSWORD] ?: defaults.backupEmailAppPassword,
            dailyBackupEnabled = prefs[Keys.DAILY_BACKUP_ENABLED] ?: defaults.dailyBackupEnabled,
            backupHour = prefs[Keys.BACKUP_HOUR] ?: defaults.backupHour,
            backupMinute = prefs[Keys.BACKUP_MINUTE] ?: defaults.backupMinute,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: defaults.lastBackupTimestamp,
            lastBackupStatus = prefs[Keys.LAST_BACKUP_STATUS] ?: defaults.lastBackupStatus,
            privacyConsentAccepted = prefs[Keys.PRIVACY_CONSENT_ACCEPTED] ?: defaults.privacyConsentAccepted,
            privacyConsentVersion = prefs[Keys.PRIVACY_CONSENT_VERSION] ?: defaults.privacyConsentVersion,
            privacyConsentTimestamp = prefs[Keys.PRIVACY_CONSENT_TIMESTAMP] ?: defaults.privacyConsentTimestamp,
            examModeEnabled = prefs[Keys.EXAM_MODE_ENABLED] ?: defaults.examModeEnabled,
            examAllowedPackages = prefs[Keys.EXAM_ALLOWED_PACKAGES] ?: defaults.examAllowedPackages,
            keyboardTrackingEnabled = prefs[Keys.KEYBOARD_TRACKING_ENABLED] ?: defaults.keyboardTrackingEnabled,
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
            prefs[Keys.NOTIFICATIONS_ENABLED] = cfg.notificationsEnabled
            prefs[Keys.THEME_MODE] = cfg.themeMode
            prefs[Keys.APP_LANGUAGE] = cfg.appLanguage
            prefs[Keys.BACKUP_EMAIL] = cfg.backupEmail
            prefs[Keys.BACKUP_EMAIL_APP_PASSWORD] = cfg.backupEmailAppPassword
            prefs[Keys.DAILY_BACKUP_ENABLED] = cfg.dailyBackupEnabled
            prefs[Keys.BACKUP_HOUR] = cfg.backupHour
            prefs[Keys.BACKUP_MINUTE] = cfg.backupMinute
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = cfg.lastBackupTimestamp
            prefs[Keys.LAST_BACKUP_STATUS] = cfg.lastBackupStatus
            prefs[Keys.PRIVACY_CONSENT_ACCEPTED] = cfg.privacyConsentAccepted
            prefs[Keys.PRIVACY_CONSENT_VERSION] = cfg.privacyConsentVersion
            prefs[Keys.PRIVACY_CONSENT_TIMESTAMP] = cfg.privacyConsentTimestamp
            prefs[Keys.EXAM_MODE_ENABLED] = cfg.examModeEnabled
            prefs[Keys.EXAM_ALLOWED_PACKAGES] = cfg.examAllowedPackages
            prefs[Keys.KEYBOARD_TRACKING_ENABLED] = cfg.keyboardTrackingEnabled
        }
    }
}
