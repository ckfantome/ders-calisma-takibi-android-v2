package com.derscalismatakibi.app.core

import android.content.Context
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.DailyTotal
import com.derscalismatakibi.app.data.LocationLogEntity
import com.derscalismatakibi.app.data.ScheduleSlotEntity
import com.derscalismatakibi.app.data.SessionEntity
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.util.ExportHelper
import com.derscalismatakibi.app.util.NotificationHelper
import com.derscalismatakibi.app.util.UsageStatsHelper
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Compose ekranlarinin gozlemleyecegi tum UI durumu - study_tracker2.py'deki
 * MainWindow'un ilgili QLabel/QProgressBar/QPushButton alanlarinin karsiligi. */
data class StudyUiState(
    val currentState: StudyState = StudyState.AWAY,
    val infoText: String = "",
    val studyingSeconds: Double = 0.0,
    val awaySeconds: Double = 0.0,
    val sleepingSeconds: Double = 0.0,
    val speakingSeconds: Double = 0.0,
    val productivityScore: Double = 0.0,
    val isSpeaking: Boolean = false,
    val speakingConfirmed: Boolean = false,
    val pomodoroState: PomodoroState = PomodoroState.IDLE,
    val pomodoroRemainingSeconds: Double = 0.0,
    val pomodoroCycles: Int = 0,
    val pauseNotice: String = "",
    val dailyGoalHours: Double = 4.0,
    val cameraError: String? = null,
    val sessionNotes: String = "",
    val sessionTags: String = "",
)

/**
 * Uygulama-genelinde TEK ornek (singleton) takip motoru. study_tracker2.py'deki
 * MainWindow'un durum/is mantigini (session, state machine, pomodoro, takvim
 * takibi) barindirir - ama bir Activity/ViewModel'e degil, `init(context)` ile
 * bir kez baslatilan bagimsiz bir nesneye baglidir. Boylece hem [StudyViewModel]
 * (Activity/Compose on planda) hem de arkaplan takibi surdiren
 * `StudyForegroundService` AYNI durumu (StateFlow'lari) paylasir - Activity
 * kapansa/arka plana alinsa bile takip (Servis calisiyorsa) kesintiye ugramaz.
 */
object StudyEngine {
    private lateinit var appContext: Context
    private lateinit var db: AppDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    private var cfg: AppConfig = AppConfig()
    private var examAllowedPackagesCache: Set<String> = emptySet()
    private var session = Session()
    private lateinit var stateMachine: HysteresisStateMachine
    private lateinit var pomodoro: PomodoroTimer
    private val speakingDetector = SpeakingDetector()
    private val speakingAwayGate = SpeakingAwayGate()

    private var lastFrameTimeMs = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    lateinit var configState: StateFlow<AppConfig>
        private set

    private val _role = MutableStateFlow(Role.ADMIN)
    val role: StateFlow<Role> = _role.asStateFlow()

    /** Arkaplan (Foreground Service) takibinin su an calisip calismadigi - MainScreen bu
     * alana bakip yerel (Activity-bagli) kamera onizlemesiyle CAKISMAMASI icin kendi
     * CameraX baglamasini devre disi birakir (ayni anda iki ayri baglama CameraX'te
     * birbirini ezer). */
    private val _backgroundTrackingActive = MutableStateFlow(false)
    val backgroundTrackingActive: StateFlow<Boolean> = _backgroundTrackingActive.asStateFlow()
    fun setBackgroundTrackingActive(active: Boolean) {
        _backgroundTrackingActive.value = active
    }

    lateinit var scheduleSlots: StateFlow<List<ScheduleSlotEntity>>
        private set

    lateinit var blockedApps: StateFlow<List<com.derscalismatakibi.app.data.BlockedAppEntity>>
        private set

    lateinit var safeZones: StateFlow<List<com.derscalismatakibi.app.data.SafeZoneEntity>>
        private set

    private val _scheduleTrackingEnabled = MutableStateFlow(false)
    val scheduleTrackingEnabled: StateFlow<Boolean> = _scheduleTrackingEnabled.asStateFlow()

    private var activeSlot: ScheduleSlot? = null
    private var slotStartStudyingSeconds = 0.0
    private var slotStartAwaySeconds = 0.0

    /** Idempotent baslatma: hem MainActivity/StudyViewModel hem de Servis kendi
     * `onCreate`'inde bunu cagirabilir, sadece ILK cagri gercekten kurulum yapar.
     *
     * ONEMLI: OpenCV'nin native kutuphanesi (kafa pozu icin solvePnP) BURADA
     * yuklenir, MainActivity'de DEGIL. Gercek cihazda dogrulanan hata: eger
     * yukleme sadece MainActivity.onCreate()'te olursa, Arkaplan Servisi bu
     * Activity hic calismadan (orn. sistem sureci ozellikle servisi ayakta
     * tutmak icin yeniden baslatirsa) baslatilirsa, org.opencv.core.Mat
     * kullanan HER kare "UnsatisfiedLinkError: No implementation found for
     * long org.opencv.core.Mat.n_Mat()" ile sessizce basarisiz oluyordu -
     * kamera kareleri geliyordu (K sayaci artiyordu) ama analiz hep hata
     * veriyordu, bu yuzden calisma suresi hicbir zaman ilerlemiyordu. */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        AppLogger.init(context)
        org.opencv.android.OpenCVLoader.initDebug()
        AppLogger.log("StudyEngine", "Motor baslatildi (OpenCV yuklendi)")
        appContext = context.applicationContext
        db = AppDatabase.getInstance(appContext)
        settingsRepository = SettingsRepository(appContext)
        notificationHelper = NotificationHelper(appContext)
        stateMachine = HysteresisStateMachine(cfg)
        pomodoro = PomodoroTimer(cfg)

        configState = settingsRepository.configFlow.stateIn(engineScope, SharingStarted.Eagerly, AppConfig())
        scheduleSlots = db.scheduleDao().observeAll().stateIn(engineScope, SharingStarted.Eagerly, emptyList())
        blockedApps = db.blockedAppDao().observeAll().stateIn(engineScope, SharingStarted.Eagerly, emptyList())
        safeZones = db.safeZoneDao().observeAll().stateIn(engineScope, SharingStarted.Eagerly, emptyList())

        engineScope.launch {
            settingsRepository.configFlow.collect { newCfg ->
                cfg = newCfg
                stateMachine.setConfig(newCfg)
                pomodoro.setConfig(newCfg)
                speakingDetector.setParams(newCfg.speakingWindowSize, newCfg.speakingMarStdThreshold, newCfg.speakingMarMinThreshold)
                speakingAwayGate.setParams(newCfg.confirmSpeakingSeconds)
                _uiState.value = _uiState.value.copy(dailyGoalHours = newCfg.dailyGoalHours)
                // isPackageBlocked() her on plan uygulama degisiminde (dakikada
                // onlarca kez) cagriliyor - string'i orada her seferinde yeniden
                // split/map/filter etmek yerine sadece ayar degisince BURADA bir
                // kere hesaplayip onbelleklendi.
                examAllowedPackagesCache = newCfg.examAllowedPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
        }
        // study_tracker2.py -> TICK_INTERVAL_MS (500ms) ile ayni periyotta Pomodoro'yu ve
        // (varsa) Takvim Takip Modu'nu ilerletir. Bu dongu artik Activity'den bagimsiz
        // calisir (engineScope), Servis arka planda calisirken de devam eder.
        engineScope.launch {
            while (true) {
                delay(500)
                val tick = pomodoro.tick()
                if (tick.justFinishedWork) {
                    session.pomodoroCycles = pomodoro.cyclesCompleted
                    notificationHelper.beep(cfg.soundEnabled)
                    val msg = if (tick.state == PomodoroState.LONG_BREAK) {
                        appContext.getString(R.string.notif_long_break_time, cfg.pomodoroLongBreakMin)
                    } else {
                        appContext.getString(R.string.notif_break_time, cfg.pomodoroBreakMin)
                    }
                    notificationHelper.notify(appContext.getString(R.string.notif_pomodoro_finished), msg, cfg.routineNotificationsEnabled)
                    AppLogger.log("Pomodoro", "Calisma tamamlandi -> ${tick.state} (dongu: ${pomodoro.cyclesCompleted})")
                } else if (tick.justFinishedBreak) {
                    notificationHelper.beep(cfg.soundEnabled)
                    notificationHelper.notify(appContext.getString(R.string.notif_break_over_title), appContext.getString(R.string.notif_break_over_body), cfg.routineNotificationsEnabled)
                    AppLogger.log("Pomodoro", "Mola bitti")
                }
                publishPomodoro(tick)
                processScheduleTracking()
            }
        }
        // Ebeveyn-denetim: guvenli bolge (geofence) - 30sn'de bir son bilinen
        // konumu kontrol eder, sadece icerde/disarda DURUMU DEGISINCE loglar/bildirir.
        engineScope.launch {
            while (true) {
                delay(30_000)
                checkSafeZone()
                checkExamModeAccessibility()
            }
        }
    }

    private var examModeAccessibilityBroken: Boolean? = null

    /** Sinav/Odev Modu acikken Erisilebilirlik Servisi (Uygulama Kilidi'ni
     * uygulayan servis) kapaliysa engelleme SESSIZCE calismiyor demektir - bu
     * OPPO/ColorOS gibi OEM'lerin izni kendiliginden geri almasinin bilinen bir
     * sonucu. Durum degisince (once calisirken kapanmis / tekrar acilmis) ebeveyne
     * bildirim+e-posta ile haber verilir, aksi halde fark edilmeden gecebilir. */
    private suspend fun checkExamModeAccessibility() {
        if (!cfg.examModeEnabled) {
            examModeAccessibilityBroken = null
            return
        }
        val broken = !com.derscalismatakibi.app.util.AccessibilityHelper.isAppBlockServiceEnabled(appContext)
        if (broken == examModeAccessibilityBroken) return
        examModeAccessibilityBroken = broken
        if (broken) {
            val msg = appContext.getString(R.string.notif_app_lock_accessibility_broken)
            AppLogger.log("UygulamaKilidi", msg)
            notificationHelper.notify(appContext.getString(R.string.notif_app_lock_disabled_title), msg, cfg.appLockAlertNotificationsEnabled)
            sendInstantAlertEmail(appContext.getString(R.string.notif_app_lock_disabled_email_subject), msg)
        } else {
            AppLogger.log("UygulamaKilidi", "Erisilebilirlik izni tekrar acik - engelleme calisiyor")
        }
    }

    private var insideAnySafeZone: Boolean? = null

    private suspend fun checkSafeZone() {
        if (!cfg.locationTrackingEnabled) return
        if (android.content.pm.PackageManager.PERMISSION_GRANTED !=
            androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
        ) return
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return
        val loc = try {
            lm.getProviders(true).firstNotNullOfOrNull { lm.getLastKnownLocation(it) }
        } catch (_: SecurityException) { null } ?: return

        // "Sadece anlik degil, surekli degisen TUM konum" gunluk yedek e-postasina
        // eklenebilsin diye HER kontrolde (30sn'de bir) konum gecmisine yazilir -
        // Guvenli Bolge tanimli olup olmadigina bakilmaksizin.
        try {
            db.locationLogDao().insert(LocationLogEntity(lat = loc.latitude, lng = loc.longitude, timestamp = System.currentTimeMillis()))
            db.locationLogDao().trimToRecent()
        } catch (t: Throwable) {
            AppLogger.logError("Konum", "Konum gecmisi yazilamadi", t)
        }

        val zones = try { db.safeZoneDao().all().filter { it.enabled } } catch (t: Throwable) { emptyList() }
        if (zones.isEmpty()) return
        val nearest = zones.minByOrNull {
            com.derscalismatakibi.app.util.LocationHelper.distanceMeters(loc.latitude, loc.longitude, it.lat, it.lng)
        }
        val inside = zones.any {
            com.derscalismatakibi.app.util.LocationHelper.distanceMeters(loc.latitude, loc.longitude, it.lat, it.lng) <= it.radiusMeters
        }
        if (inside == insideAnySafeZone) return
        insideAnySafeZone = inside
        val distance = nearest?.let { com.derscalismatakibi.app.util.LocationHelper.distanceMeters(loc.latitude, loc.longitude, it.lat, it.lng) }
        val msg = if (inside) {
            appContext.getString(R.string.notif_safe_zone_entered)
        } else {
            appContext.getString(R.string.notif_safe_zone_left) +
                (distance?.let { appContext.getString(R.string.notif_safe_zone_distance_suffix, it.toInt(), nearest?.name) } ?: "")
        }
        AppLogger.log("Konum", msg)
        notificationHelper.notify(appContext.getString(R.string.notif_safe_zone_title), msg, cfg.safeZoneAlertNotificationsEnabled)
        if (!inside) sendInstantAlertEmail(appContext.getString(R.string.notif_safe_zone_left_email_subject), msg)
    }

    /** Gunluk ozetin aksine (bkz. DailyBackupWorker), bunlar "hemen" gonderilmesi
     * gereken az sayida onemli olay icin (guvenli bolgeden cikma, sinav modunda
     * engellenen uygulama acilmaya calisilmasi) - ayni SMTP ayarlarini kullanir,
     * WorkManager beklemeden dogrudan IO thread'inde, sonucu beklemeden gonderir. */
    fun sendInstantAlertEmail(subject: String, body: String) {
        if (cfg.backupEmail.isBlank() || cfg.backupEmailAppPassword.isBlank()) return
        val email = cfg.backupEmail
        val password = cfg.backupEmailAppPassword
        engineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            when (com.derscalismatakibi.app.backup.SmtpBackupSender.send(email, password, subject = "${appContext.getString(R.string.app_name)} - $subject", body = body)) {
                is com.derscalismatakibi.app.backup.SmtpBackupSender.Result.Success ->
                    AppLogger.log("AnlikUyari", "E-posta gonderildi: $subject")
                else ->
                    AppLogger.logError("AnlikUyari", "E-posta gonderilemedi: $subject")
            }
        }
    }

    private var cachedSafePackages: Set<String>? = null

    /** Sinav Modu "her seyi engelle" moduna gecince BILE asla engellenmeyecek
     * paketler - aksi halde varsayilan ana ekran veya telefon/arama da
     * engellenip cihaz kullanilamaz hale gelebilir. */
    private fun essentialSafePackages(): Set<String> {
        cachedSafePackages?.let { return it }
        val pm = appContext.packageManager
        // com.android.systemui (durum cubugu/bildirim paneli/genel gorunum/hizli
        // ayarlar) TYPE_WINDOW_STATE_CHANGED tetikliyor - guvenli listede olmazsa
        // Sinav Modu bunu da "engellenen uygulama" sayip ustune BlockedActivity
        // aciyordu; kullaniciya uygulamanin/Sinav Modu'nun kendiliginden
        // kapaniyormus/bozuluyormus gibi gorunmesinin gercek cihazda dogrulanan
        // nedeni budur (canli test sirasinda yakalandi).
        val result = mutableSetOf(appContext.packageName, "com.android.systemui")
        try {
            pm.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName?.let { result.add(it) }
        } catch (_: Exception) {}
        try {
            pm.resolveActivity(android.content.Intent(android.content.Intent.ACTION_DIAL), 0)
                ?.activityInfo?.packageName?.let { result.add(it) }
        } catch (_: Exception) {}
        result.add("com.android.settings")
        // Ekran klavyesinin KENDI paketi de on plana gelince TYPE_WINDOW_STATE_CHANGED
        // tetikliyor - guvenli listede olmazsa, izinli bir uygulamada bile klavye
        // acilir acilmaz BlockedActivity'ye yonlendirilip yazi yazmak IMKANSIZ hale
        // geliyordu (gercek cihaz/emulator testinde dogrulandi).
        try {
            android.provider.Settings.Secure.getString(appContext.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')?.let { result.add(it) }
        } catch (_: Exception) {}
        cachedSafePackages = result
        return result
    }

    fun currentConfig(): AppConfig = cfg

    fun updateConfig(newCfg: AppConfig) {
        AppLogger.log("StudyEngine", "Ayarlar guncellendi")
        engineScope.launch { settingsRepository.update(newCfg) }
    }

    fun reportCameraError(message: String?) {
        if (message != null) AppLogger.logError("Kamera", message)
        _uiState.value = _uiState.value.copy(cameraError = message)
    }

    fun tryUnlockAdmin(pin: String): Boolean {
        if (pin == cfg.appPin) {
            _role.value = Role.ADMIN
            AppLogger.log("Rol", "Yonetici moduna gecildi")
            return true
        }
        AppLogger.log("Rol", "Yonetici PIN denemesi basarisiz")
        return false
    }

    fun switchToStudent() {
        _role.value = Role.STUDENT
        AppLogger.log("Rol", "Ogrenci moduna gecildi")
    }

    fun addScheduleSlot(day: Int, start: String, end: String, kind: String) {
        AppLogger.log("Takvim", "Aralik eklendi: gun=$day $start-$end ($kind)")
        engineScope.launch { db.scheduleDao().insert(ScheduleSlotEntity(day = day, startTime = start, endTime = end, kind = kind)) }
    }

    fun deleteScheduleSlot(entity: ScheduleSlotEntity) {
        AppLogger.log("Takvim", "Aralik silindi: gun=${entity.day} ${entity.startTime}-${entity.endTime}")
        engineScope.launch { db.scheduleDao().delete(entity) }
    }

    fun addBlockedApp(packageName: String, appLabel: String, dailyLimitMinutes: Int?, studyHoursOnly: Boolean) {
        AppLogger.log("UygulamaKilidi", "Eklendi: $packageName (limit=$dailyLimitMinutes, calismaSaati=$studyHoursOnly)")
        engineScope.launch {
            db.blockedAppDao().insert(
                com.derscalismatakibi.app.data.BlockedAppEntity(
                    packageName = packageName,
                    appLabel = appLabel,
                    dailyLimitMinutes = dailyLimitMinutes,
                    studyHoursOnly = studyHoursOnly,
                ),
            )
        }
    }

    fun deleteBlockedApp(entity: com.derscalismatakibi.app.data.BlockedAppEntity) {
        AppLogger.log("UygulamaKilidi", "Kaldirildi: ${entity.packageName}")
        engineScope.launch { db.blockedAppDao().delete(entity) }
    }

    fun addSafeZone(name: String, lat: Double, lng: Double, radiusMeters: Double) {
        AppLogger.log("Konum", "Guvenli bolge eklendi: $name")
        engineScope.launch {
            db.safeZoneDao().insert(com.derscalismatakibi.app.data.SafeZoneEntity(name = name, lat = lat, lng = lng, radiusMeters = radiusMeters))
        }
    }

    fun updateSafeZone(zone: com.derscalismatakibi.app.data.SafeZoneEntity) {
        engineScope.launch { db.safeZoneDao().update(zone) }
    }

    fun deleteSafeZone(zone: com.derscalismatakibi.app.data.SafeZoneEntity) {
        AppLogger.log("Konum", "Guvenli bolge silindi: ${zone.name}")
        engineScope.launch { db.safeZoneDao().delete(zone) }
    }

    fun todaysScheduleSummary(): String {
        val day = mondayFirstWeekday()
        val todays = scheduleSlots.value.filter { it.day == day }.sortedBy { it.startTime }
        if (todays.isEmpty()) return ""
        val labels = slotKindLabels(appContext)
        return todays.joinToString("\n") { "  ${it.startTime}–${it.endTime}  (${labels[it.kind] ?: it.kind})" }
    }

    fun startScheduleTracking() {
        _scheduleTrackingEnabled.value = true
        activeSlot = null
        AppLogger.log("Takvim", "Takvim takibi baslatildi")
        processScheduleTracking()
    }

    fun stopScheduleTracking() {
        closeActiveScheduleSlot()
        activeSlot = null
        _scheduleTrackingEnabled.value = false
        AppLogger.log("Takvim", "Takvim takibi durduruldu")
    }

    private fun processScheduleTracking() {
        if (!_scheduleTrackingEnabled.value) return
        val cal = Calendar.getInstance()
        val day = mondayFirstWeekday(cal)
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val slots = scheduleSlots.value.map { ScheduleSlot(it.day, it.startTime, it.endTime, it.kind) }
        val slot = currentScheduleSlot(slots, day, minuteOfDay)
        if (slot == activeSlot) return
        closeActiveScheduleSlot()
        activeSlot = slot
        // Ders programina gore otomatik profil: Calisma diliminde uzakta/uyku onay
        // surelerini yariya indirir (daha hassas takip), Mola'da veya dilim disinda
        // normal esiklere doner. ponytail: ayri "strict mode" config alanlari yerine
        // stateMachine.setConfig'i mevcut cfg'nin turetilmis bir kopyasiyla cagirmak
        // yeterli - kalici bir ayar degil, sadece dilim suresince gecerli.
        stateMachine.setConfig(
            if (slot?.kind == SLOT_KIND_WORK) {
                cfg.copy(confirmAwaySeconds = cfg.confirmAwaySeconds / 2, confirmSleepSeconds = cfg.confirmSleepSeconds / 2)
            } else {
                cfg
            }
        )
        if (slot != null) {
            AppLogger.log("Takvim", "Aktif dilim: ${slot.startTime}-${slot.endTime} (${slot.kind})")
            if (slot.kind == SLOT_KIND_WORK) AppLogger.log("Profil", "Calisma profiline gecildi (sikistirilmis onay sureleri)")
            slotStartStudyingSeconds = session.studyingSeconds
            slotStartAwaySeconds = session.awaySeconds
            if (slot.kind == SLOT_KIND_WORK && pomodoro.state == PomodoroState.IDLE) {
                pomodoro.start()
                publishPomodoro(pomodoro.tick())
            }
        }
    }

    private fun closeActiveScheduleSlot() {
        val slot = activeSlot ?: return
        val studied = (session.studyingSeconds - slotStartStudyingSeconds).coerceAtLeast(0.0)
        val away = (session.awaySeconds - slotStartAwaySeconds).coerceAtLeast(0.0)
        if (slot.kind == SLOT_KIND_WORK) {
            val plannedMin = slotDurationMinutes(slot.startTime, slot.endTime)
            val studiedMin = Math.round(studied / 60.0)
            var note = appContext.getString(R.string.schedule_note_work_summary, slot.startTime, slot.endTime, studiedMin, plannedMin)
            note += if (away >= 30) appContext.getString(R.string.schedule_note_away_suffix, fmtHms(away)) else "."
            appendSessionNote(note)
            notificationHelper.notify(appContext.getString(R.string.notif_work_slot_finished), note, cfg.routineNotificationsEnabled)
        } else if (studied >= 30) {
            val note = appContext.getString(R.string.schedule_note_break_summary, slot.startTime, slot.endTime, fmtHms(studied))
            appendSessionNote(note)
            notificationHelper.notify(appContext.getString(R.string.notif_studying_during_break), note, cfg.routineNotificationsEnabled)
        }
    }

    private fun appendSessionNote(line: String) {
        session.notes = if (session.notes.isBlank()) line else "${session.notes}\n$line"
    }

    suspend fun buildExportIntent(): Intent {
        val all = db.sessionDao().allSessions()
        return ExportHelper.exportToCsv(appContext, all)
    }

    suspend fun weeklyStudySeconds(): Double {
        val cal = Calendar.getInstance()
        val todayDow = mondayFirstWeekday(cal)
        cal.add(Calendar.DAY_OF_YEAR, -todayDow)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return db.sessionDao().weeklyTotal(fmt.format(cal.time))
    }

    fun onFrameAnalyzed(points: List<Point2D>?, width: Int, height: Int) {
        val now = System.currentTimeMillis()
        // Normal kare araligi ~30-100ms - ama kamera/MediaPipe ilk baslatilirken
        // (birkac saniye surebilir) veya arkaplandan donuste, lastFrameTimeMs ile
        // aradaki fark TAMAMEN o anki duruma (calisiyor/uzakta/uykulu) ekleniyordu,
        // ust sinir yoktu - her kamera baslangicinda birkac saniye sahte sure
        // sayiliyordu. 1 saniyeyle sinirlandirildi (gercek kare araliginin cok
        // uzerinde, normal kareleri kesmez, sadece boslugu kesip atar).
        val dtElapsed = ((now - lastFrameTimeMs) / 1000.0).coerceIn(0.0, 1.0)
        lastFrameTimeMs = now

        val frameLabels = FrameAnalysisLabels(
            faceNotFound = appContext.getString(R.string.frame_face_not_found),
            poseNotCalculated = appContext.getString(R.string.frame_pose_not_calculated),
            eyesClosedFmt = appContext.getString(R.string.frame_eyes_closed),
            studyingFmt = appContext.getString(R.string.frame_studying),
            headTurnedFmt = appContext.getString(R.string.frame_head_turned),
        )
        val analysis: FrameAnalysis = analyzeFrame(points, width, height, cfg, frameLabels)
        analysis.isSpeaking = speakingDetector.update(analysis.mar)
        analysis.speakingConfirmed = speakingAwayGate.update(analysis.isSpeaking, now)

        var effective = analysis
        if (analysis.speakingConfirmed && cfg.speakingCountsAsAway && analysis.observedState == StudyState.STUDYING) {
            effective = analysis.copy(
                observedState = StudyState.AWAY,
                infoText = appContext.getString(R.string.frame_speaking_distraction, analysis.infoText),
                forcedAwayBySpeaking = true,
            )
        }

        val state: StudyState = if (effective.forcedAwayBySpeaking) {
            stateMachine.forceState(StudyState.AWAY)
            StudyState.AWAY
        } else {
            stateMachine.update(effective.observedState, now)
        }

        if (state != _uiState.value.currentState) {
            AppLogger.log("Durum", "${_uiState.value.currentState} -> $state")
        }

        when (state) {
            StudyState.STUDYING -> session.studyingSeconds += dtElapsed
            StudyState.SLEEPING -> session.sleepingSeconds += dtElapsed
            StudyState.AWAY -> session.awaySeconds += dtElapsed
        }
        if (effective.isSpeaking) session.speakingSeconds += dtElapsed

        var pauseNotice = _uiState.value.pauseNotice
        if (state == StudyState.AWAY && cfg.autoPauseOnAway && pomodoro.state == PomodoroState.WORKING) {
            pomodoro.pause()
            pauseNotice = appContext.getString(R.string.pomodoro_paused_away)
        } else if (state == StudyState.SLEEPING && cfg.autoPauseOnSleep && pomodoro.state == PomodoroState.WORKING) {
            pomodoro.pause()
            pauseNotice = appContext.getString(R.string.pomodoro_paused_sleeping)
        } else if (state == StudyState.STUDYING && pomodoro.state == PomodoroState.PAUSED) {
            pomodoro.resume()
            pauseNotice = ""
        } else if (state == StudyState.STUDYING) {
            pauseNotice = ""
        }

        _uiState.value = _uiState.value.copy(
            currentState = state,
            infoText = effective.infoText,
            studyingSeconds = session.studyingSeconds,
            awaySeconds = session.awaySeconds,
            sleepingSeconds = session.sleepingSeconds,
            speakingSeconds = session.speakingSeconds,
            productivityScore = session.productivityScore(),
            isSpeaking = effective.isSpeaking,
            speakingConfirmed = effective.speakingConfirmed,
            pauseNotice = pauseNotice,
            cameraError = null,
        )
        publishPomodoro(pomodoro.tick())
    }

    private fun publishPomodoro(tick: PomodoroTick) {
        _uiState.value = _uiState.value.copy(
            pomodoroState = tick.state,
            pomodoroRemainingSeconds = tick.remainingSeconds,
            pomodoroCycles = tick.cyclesCompleted,
        )
    }

    fun togglePomodoro() {
        if (pomodoro.state == PomodoroState.IDLE) notificationHelper.beep(cfg.soundEnabled)
        AppLogger.log("Pomodoro", "Kullanici toggle - onceki durum: ${pomodoro.state}")
        when (pomodoro.state) {
            PomodoroState.IDLE -> pomodoro.start()
            PomodoroState.PAUSED -> pomodoro.resume()
            else -> pomodoro.stop()
        }
        publishPomodoro(pomodoro.tick())
    }

    fun manualBreak(): Boolean {
        if (pomodoro.state != PomodoroState.WORKING) {
            _uiState.value = _uiState.value.copy(
                pauseNotice = appContext.getString(R.string.pomodoro_manual_break_only_while_working),
            )
            return false
        }
        pomodoro.startManualBreak()
        AppLogger.log("Pomodoro", "Manuel mola baslatildi")
        publishPomodoro(pomodoro.tick())
        return true
    }

    fun updateSessionNotes(notes: String, tags: String) {
        session.notes = notes
        session.tags = tags
        _uiState.value = _uiState.value.copy(sessionNotes = notes, sessionTags = tags)
    }

    fun resetGoal(notes: String = "", tags: String = "") {
        AppLogger.log("StudyEngine", "Hedef sifirlaniyor (toplam: ${session.totalSeconds()} sn)")
        closeActiveScheduleSlot()
        activeSlot = null
        engineScope.launch {
            if (session.totalSeconds() > 0) {
                saveSession(session, notes.ifBlank { session.notes }, tags.ifBlank { session.tags })
            }
            session = Session()
            pomodoro.reset()
            _uiState.value = _uiState.value.copy(
                studyingSeconds = 0.0, awaySeconds = 0.0, sleepingSeconds = 0.0,
                speakingSeconds = 0.0, productivityScore = 0.0,
                sessionNotes = "", sessionTags = "",
            )
            publishPomodoro(pomodoro.tick())
        }
    }

    /** Uygulama tamamen kapandiginda (arkaplan takibi de KAPALIYKEN) cagrilmali. */
    fun finalizeSessionIfNeeded() {
        closeActiveScheduleSlot()
        activeSlot = null
        if (session.totalSeconds() > 0) {
            // session'i ONCE sifirla (async kaydetmeden once) - yoksa Servis.onDestroy
            // ve Activity.onStop art arda tetiklenince (bkz. log: ayni oturum 2-3 kez
            // kaydedilmisti) ayni oturum tekrar tekrar DB'ye insert ediliyordu.
            val toSave = session
            session = Session()
            engineScope.launch { saveSession(toSave, toSave.notes, toSave.tags) }
        }
    }

    private suspend fun saveSession(s: Session, notes: String, tags: String) {
        AppLogger.log("StudyEngine", "Oturum kaydediliyor: calisma=${s.studyingSeconds}sn uzakta=${s.awaySeconds}sn uyku=${s.sleepingSeconds}sn")
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val start = Date(s.startTimeMillis)
        val end = Date()
        db.sessionDao().insert(
            SessionEntity(
                date = dateFmt.format(start),
                startTime = timeFmt.format(start),
                endTime = timeFmt.format(end),
                studyingSeconds = s.studyingSeconds,
                awaySeconds = s.awaySeconds,
                sleepingSeconds = s.sleepingSeconds,
                totalSeconds = s.totalSeconds(),
                speakingSeconds = s.speakingSeconds,
                pomodoroCycles = s.pomodoroCycles,
                notes = notes,
                tags = tags,
                productivityScore = s.productivityScore(),
            )
        )
    }

    suspend fun dailyTotals(limit: Int = 30): List<DailyTotal> = db.sessionDao().dailyTotals(limit)

    private var usageCacheTimestamp = 0L
    private var usageCache: List<com.derscalismatakibi.app.util.AppUsageEntry> = emptyList()

    /** UsageStatsManager sorgusu (tum kurulu uygulamalarin gunluk kullanimi +
     * her biri icin ayri PackageManager.getApplicationLabel binder cagrisi) agir -
     * gunluk sure siniri tanimli bir uygulama varsa bu, her on plan degisiminde
     * (dakikada onlarca kez) calisiyordu. Ayarlar > Calisan Sistemler'deki
     * "Kullanim Suresi Kontrol Sikligi" (usageCheckIntervalSeconds) - 0 ise her
     * seferinde tazelenir (Anlik), aksi halde son sorgu o kadar taze ise
     * onbellekten donulur (kontrol birkac saniye gecikmeli tetiklenebilir). */
    private fun todaysUsageMinutes(pkg: String): Long {
        val intervalMs = cfg.usageCheckIntervalSeconds * 1000L
        val now = System.currentTimeMillis()
        if (intervalMs <= 0 || now - usageCacheTimestamp >= intervalMs) {
            usageCache = UsageStatsHelper.loadTodayUsage(appContext)
            usageCacheTimestamp = now
        }
        return usageCache.find { it.packageName == pkg }?.totalMillis?.div(60000) ?: 0L
    }

    /** Uygulama Kilidi: hem AppBlockAccessibilityService hem (ileride) UI ayni
     * mantigi kullansin diye tek yerde. Sinav Modu > gunluk sure siniri > calisma
     * saati oncelik sirasiyla kontrol edilir. */
    suspend fun isPackageBlocked(pkg: String): BlockReason? {
        // Sinav/Odev Modu: Kilitli Uygulamalar listesine BAKMAKSIZIN varsayilan
        // olarak HER SEY engellenir, sadece examAllowedPackages'taki (+ asla
        // engellenmeyecek zorunlu guvenlik listesi: kendi uygulamamiz, varsayilan
        // ana ekran, telefon/arama - cihazi kullanilamaz hale getirmemek icin)
        // paketler acilabilir.
        if (cfg.examModeEnabled) {
            if (pkg in essentialSafePackages()) return null
            if (pkg in examAllowedPackagesCache) return null
            return BlockReason.ExamMode
        }
        // db.blockedAppDao().all() yerine zaten canli tutulan blockedApps
        // StateFlow'u okunuyor - her on plan degisiminde tam tablo taramasi
        // yerine bellekteki listeye bakiliyor (Kilitli Uygulamalar listesi
        // bos olsa bile eskiden her seferinde DB'ye gidiliyordu).
        val entry = blockedApps.value.find { it.packageName == pkg } ?: return null
        val limitMin = entry.dailyLimitMinutes
        if (limitMin != null) {
            val usedMin = todaysUsageMinutes(pkg)
            if (usedMin >= limitMin) return BlockReason.DailyLimit
        }
        // "studyHoursOnly" onceden SADECE elle kurulmus haftalik Takvim'de aktif
        // bir "calisma" dilimi varsa tetikleniyordu - Takvim kurmayan kullanicilar
        // icin (coğunluk) tek bir uygulama eklense bile HICBIR ZAMAN engellenmiyordu.
        // Artik kameranin gercek zamanli tespit ettigi "Calisiyor" durumu da yeterli.
        val isStudyingNow = _uiState.value.currentState == StudyState.STUDYING
        if (entry.studyHoursOnly && (activeSlot?.kind == SLOT_KIND_WORK || isStudyingNow)) return BlockReason.StudyHours
        return null
    }
}

sealed class BlockReason {
    object StudyHours : BlockReason()
    object ExamMode : BlockReason()
    object DailyLimit : BlockReason()
}
