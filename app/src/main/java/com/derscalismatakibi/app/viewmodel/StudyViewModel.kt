package com.derscalismatakibi.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.derscalismatakibi.app.core.AppConfig
import com.derscalismatakibi.app.core.Point2D
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.core.StudyUiState
import com.derscalismatakibi.app.data.DailyTotal
import com.derscalismatakibi.app.data.ScheduleSlotEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * Ince bir Compose/Activity katmani: tum gercek durum ve is mantigi artik
 * uygulama-genelinde tek ornek olan [StudyEngine]'de yasiyor (Activity arka
 * plana alinsa/kapansa bile `StudyForegroundService` calisirken kesintiye
 * ugramamasi icin). Bu sinif sadece StudyEngine'e ince bir ViewModel-uyumlu
 * cephe (facade) sunar - Compose ekranlari ayni API'yi kullanmaya devam eder.
 */
class StudyViewModel(application: Application) : AndroidViewModel(application) {
    init {
        StudyEngine.init(application)
    }

    val uiState: StateFlow<StudyUiState> get() = StudyEngine.uiState
    val configState: StateFlow<AppConfig> get() = StudyEngine.configState
    val role: StateFlow<Role> get() = StudyEngine.role
    val scheduleSlots: StateFlow<List<ScheduleSlotEntity>> get() = StudyEngine.scheduleSlots
    val blockedApps: StateFlow<List<com.derscalismatakibi.app.data.BlockedAppEntity>> get() = StudyEngine.blockedApps
    val safeZones: StateFlow<List<com.derscalismatakibi.app.data.SafeZoneEntity>> get() = StudyEngine.safeZones
    val scheduleTrackingEnabled: StateFlow<Boolean> get() = StudyEngine.scheduleTrackingEnabled
    val backgroundTrackingActive: StateFlow<Boolean> get() = StudyEngine.backgroundTrackingActive

    fun currentConfig(): AppConfig = StudyEngine.currentConfig()
    fun updateConfig(newCfg: AppConfig) = StudyEngine.updateConfig(newCfg)
    fun reportCameraError(message: String?) = StudyEngine.reportCameraError(message)

    fun tryUnlockAdmin(pin: String): Boolean = StudyEngine.tryUnlockAdmin(pin)
    fun switchToStudent() = StudyEngine.switchToStudent()

    fun addScheduleSlot(day: Int, start: String, end: String, kind: String) = StudyEngine.addScheduleSlot(day, start, end, kind)
    fun deleteScheduleSlot(entity: ScheduleSlotEntity) = StudyEngine.deleteScheduleSlot(entity)
    fun addBlockedApp(packageName: String, appLabel: String, dailyLimitMinutes: Int?, studyHoursOnly: Boolean) =
        StudyEngine.addBlockedApp(packageName, appLabel, dailyLimitMinutes, studyHoursOnly)
    fun deleteBlockedApp(entity: com.derscalismatakibi.app.data.BlockedAppEntity) = StudyEngine.deleteBlockedApp(entity)
    fun addSafeZone(name: String, lat: Double, lng: Double, radiusMeters: Double) = StudyEngine.addSafeZone(name, lat, lng, radiusMeters)
    fun updateSafeZone(zone: com.derscalismatakibi.app.data.SafeZoneEntity) = StudyEngine.updateSafeZone(zone)
    fun deleteSafeZone(zone: com.derscalismatakibi.app.data.SafeZoneEntity) = StudyEngine.deleteSafeZone(zone)
    fun todaysScheduleSummary(): String = StudyEngine.todaysScheduleSummary()
    fun startScheduleTracking() = StudyEngine.startScheduleTracking()
    fun stopScheduleTracking() = StudyEngine.stopScheduleTracking()

    suspend fun buildExportIntent(): Intent = StudyEngine.buildExportIntent()
    suspend fun weeklyStudySeconds(): Double = StudyEngine.weeklyStudySeconds()

    fun onFrameAnalyzed(points: List<Point2D>?, width: Int, height: Int) = StudyEngine.onFrameAnalyzed(points, width, height)
    fun togglePomodoro() = StudyEngine.togglePomodoro()
    fun manualBreak(): Boolean = StudyEngine.manualBreak()
    fun updateSessionNotes(notes: String, tags: String) = StudyEngine.updateSessionNotes(notes, tags)
    fun resetGoal(notes: String = "", tags: String = "") = StudyEngine.resetGoal(notes, tags)

    /** Uygulama arka plana atildiginda / kapandiginda cagrilmali (bkz. MainActivity onStop).
     * Arkaplan Servisi calisiyorsa bu cagriyi YAPMIYORUZ (bkz. MainActivity) - aksi halde
     * servis hala takip ederken oturum burada erken sonlandirilir. */
    fun finalizeSessionIfNeeded() = StudyEngine.finalizeSessionIfNeeded()

    suspend fun dailyTotals(limit: Int = 30): List<DailyTotal> = StudyEngine.dailyTotals(limit)
}
