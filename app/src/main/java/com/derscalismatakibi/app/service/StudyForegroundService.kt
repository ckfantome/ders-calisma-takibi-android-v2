package com.derscalismatakibi.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.PowerManager
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.derscalismatakibi.app.MainActivity
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.camera.CameraAnalyzer
import com.derscalismatakibi.app.camera.FaceLandmarkerHelper
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.core.StudyState
import com.derscalismatakibi.app.core.fmtHms
import com.derscalismatakibi.app.util.AppLogger
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * study_tracker2.py -> CameraWorker (QThread) + MainWindow'un Activity yasam
 * dongusunden BAGIMSIZ calisan karsiligi. Kullanici "Arkaplanda Takip"i
 * actiginda baslar; kamera+MediaPipe pipeline'ini KENDI yasam dongusunde
 * (LifecycleService de bir LifecycleOwner'dir) tutar, boylece Activity arka
 * plana alinsa/kapansa bile [StudyEngine] besleniyor ve takip devam eder.
 *
 * NOT: MainScreen'deki yerel kamera onizlemesiyle AYNI ANDA calismaz - ikisi de
 * ayni CameraX ProcessCameraProvider'i bagladigi icin cakisir. Bu Servis
 * calisirken MainScreen kendi baglamasini devre disi birakir (bkz.
 * StudyEngine.backgroundTrackingActive) ve sadece durum metnini gosterir.
 */
class StudyForegroundService : LifecycleService() {
    companion object {
        const val ACTION_START = "com.derscalismatakibi.app.action.START_TRACKING"
        const val ACTION_STOP = "com.derscalismatakibi.app.action.STOP_TRACKING"
        private const val CHANNEL_ID = "study_tracker_foreground_channel"
        private const val NOTIF_ID = 42

        fun startIntent(context: Context): Intent =
            Intent(context, StudyForegroundService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, StudyForegroundService::class.java).setAction(ACTION_STOP)
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var faceLandmarkerHelper: FaceLandmarkerHelper? = null
    private var dummySurfaceTexture: SurfaceTexture? = null
    private var dummySurface: Surface? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // TESHIS AMACLI (gecici): kamera donanimindan GERCEKTEN kare gelip gelmedigini
    // dogrudan olcuyor - StudyEngine/MediaPipe mantigindan bagimsiz en alt seviye
    // kontrol noktasi. Bildirimde "K:<sayi>" olarak gorunur; arka planda artmaya
    // devam ediyorsa sorun kamerada degil ustteki mantiktadir, artmiyorsa sorun
    // kameranin kendisinde/donanim-seviyesindedir.
    private val frameCount = AtomicInteger(0)
    private var lastFrameNotifyMs = 0L
    private val errorCount = AtomicInteger(0)
    private var lastErrorMessage: String? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.log("Servis", "onCreate")
        try {
            StudyEngine.init(applicationContext)
            createNotificationChannel()
        } catch (t: Throwable) {
            // Bu sandbox'ta gercek cihazda test edilemediginden, sessiz cokme yerine
            // hatayi StudyEngine uzerinden UI'da (kirmizi metin) gorunur kiliyoruz.
            AppLogger.logError("Servis", "onCreate basarisiz", t)
            StudyEngine.reportCameraError("Servis baslatilamadi (onCreate): ${t.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        AppLogger.log("Servis", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                AppLogger.log("Servis", "Durdur aksiyonu - stopSelf")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                try {
                    // Android 14'te CAMERA izni o an verilmemisse (orn. kullanici izni
                    // henuz onaylamadan anahtari actiysa) foregroundServiceType="camera"
                    // ile bu cagri SecurityException/IllegalStateException firlatir ve
                    // servis bildirim hic gorunmeden coker - bunu artik gorunur kiliyoruz.
                    startForeground(NOTIF_ID, buildNotification(StudyState.AWAY, 0.0, 0))
                    AppLogger.log("Servis", "startForeground basarili")
                } catch (t: Throwable) {
                    AppLogger.logError("Servis", "startForeground basarisiz", t)
                    StudyEngine.reportCameraError(
                        "Arkaplan bildirimi baslatilamadi (kamera izni verilmemis olabilir): ${t.message}",
                    )
                    stopSelf()
                    return START_NOT_STICKY
                }
                acquireWakeLock()
                startCamera()
                observeStateForNotification()
                StudyEngine.setBackgroundTrackingActive(true)
            }
        }
        // Sistem kaynak sikintisinda servisi oldururse yeniden baslatsin (kullanici
        // acikca "Durdur"a basmadikca takip devam etmeli).
        return START_STICKY
    }

    /** Ekran kapaliyken CPU'nun uykuya gecip kamera analizini durdurmasini onler.
     * Suresiz acquire() ASLA cagrilmaz - servis herhangi bir nedenle onDestroy'u
     * atlarsa (OEM sert kill) bile 10 saat sonra kendiliginden serbest kalir. */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "DersCalismaTakibi:BackgroundTracking",
        ).apply { acquire(10 * 60 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    /** Kullanici uygulamayi Son Kullanilanlar'dan kaydirinca cagrilir (OEM pil
     * yoneticilerinin servisi tamamen oldurdugu en yaygin senaryo). Kendi
     * kendini AlarmManager ile yeniden baslatmiyoruz (bkz. plan notu) - sadece
     * UI'in en azindan temiz/dogru bir durum gozlemlemesini sagliyoruz. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        AppLogger.log("Servis", "onTaskRemoved (uygulama Son Kullanilanlar'dan kaldirildi)")
        StudyEngine.setBackgroundTrackingActive(false)
        super.onTaskRemoved(rootIntent)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            val helper = try {
                FaceLandmarkerHelper(applicationContext)
            } catch (t: Throwable) {
                AppLogger.logError("Servis", "FaceLandmarkerHelper olusturulamadi", t)
                StudyEngine.reportCameraError(t.message ?: "Kamera hatasi (arkaplan)")
                return@addListener
            }
            faceLandmarkerHelper = helper

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            imageAnalysis.setAnalyzer(
                cameraExecutor,
                CameraAnalyzer(
                    helper,
                    onResult = { points, w, h -> StudyEngine.onFrameAnalyzed(points, w, h) },
                    onError = ::onDiagnosticFrameError,
                    onFrameReceived = ::onDiagnosticFrameReceived,
                ),
            )
            val selector = if (StudyEngine.currentConfig().useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            // Bazi telefonlarin (orn. Samsung) kamera donanim katmani, ekranda
            // GORUNEN bir Preview yuzeyi olmadan sadece-analiz (ImageAnalysis-only)
            // oturumlarini uygulama arka plana alinca sessizce yavaslatiyor/durduruyor
            // - CameraX API seviyesinde baglanti "aktif" gorunse bile kare akisi
            // kesiliyordu (bildirim donuyor, sayac ilerlemiyordu). Cozum: goze
            // GORUNMEYEN ama donanima "birisi izliyor" sinyali veren gercek bir
            // Preview yuzeyi (SurfaceTexture destekli) de baglamak.
            val dummyPreview = Preview.Builder().build().also { p ->
                val texture = SurfaceTexture(0).apply { setDefaultBufferSize(640, 480) }
                val surface = Surface(texture)
                dummySurfaceTexture = texture
                dummySurface = surface
                p.setSurfaceProvider { request ->
                    request.provideSurface(surface, androidx.core.content.ContextCompat.getMainExecutor(this)) {}
                }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, dummyPreview, imageAnalysis)
                AppLogger.log("Servis", "Kamera baglandi (${if (StudyEngine.currentConfig().useFrontCamera) "on" else "arka"} kamera)")
            } catch (t: Throwable) {
                AppLogger.logError("Servis", "Kamera baglanamadi", t)
                StudyEngine.reportCameraError(t.message ?: "Kamera baglanamadi (arkaplan)")
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    private fun observeStateForNotification() {
        lifecycleScope.launch {
            StudyEngine.uiState.collect { state ->
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIF_ID, buildNotification(state.currentState, state.studyingSeconds, frameCount.get(), lastErrorMessage))
            }
        }
    }

    /** TESHIS AMACLI (gecici): CameraAnalyzer'in her cagrilisinda tetiklenir (StudyEngine'e
     * hic ugramadan) - kamera donanimindan kare gelip gelmedigini dogrudan gosterir.
     * Bildirimi saniyede birden fazla guncellemeyi onlemek icin kaba bir throttle var. */
    private fun onDiagnosticFrameReceived() {
        val n = frameCount.incrementAndGet()
        val now = System.currentTimeMillis()
        if (now - lastFrameNotifyMs < 1000) return
        lastFrameNotifyMs = now
        AppLogger.log("Kare", "K=$n")
        val manager = getSystemService(NotificationManager::class.java)
        val current = StudyEngine.uiState.value
        manager?.notify(NOTIF_ID, buildNotification(current.currentState, current.studyingSeconds, n, lastErrorMessage))
    }

    /** TESHIS AMACLI (gecici): K sayaci artmaya devam edip studyingSeconds donuyorsa,
     * hatanin MediaPipe/StudyEngine tarafinda (CameraAnalyzer'in analiz/onResult
     * adiminda yakaladigi bir istisna) oldugunu gosterir - bunu artik BILDIRIME de
     * yaziyoruz ki uygulamayi tekrar acmadan gorulebilsin. */
    private fun onDiagnosticFrameError(t: Throwable) {
        val e = errorCount.incrementAndGet()
        lastErrorMessage = "HATA($e): ${t::class.simpleName}: ${t.message}"
        StudyEngine.reportCameraError(lastErrorMessage)
        val manager = getSystemService(NotificationManager::class.java)
        val current = StudyEngine.uiState.value
        manager?.notify(NOTIF_ID, buildNotification(current.currentState, current.studyingSeconds, frameCount.get(), lastErrorMessage))
    }

    private fun buildNotification(state: StudyState, studyingSeconds: Double, frameCount: Int, errorMessage: String? = null): Notification {
        val stateLabel = when (state) {
            StudyState.STUDYING -> "Calisiyor"
            StudyState.AWAY -> "Uzakta"
            StudyState.SLEEPING -> "Uykulu"
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this, 0, stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // "K:<n>" TESHIS AMACLI gecici bir sayac - her kamera karesinde artar,
        // studyingSeconds'tan bagimsizdir. Arka planda artmayi surdurup
        // surdurmedigi, sorunun kamerada mi mantikta mi oldugunu gosterecek.
        val text = "$stateLabel · ${fmtHms(studyingSeconds)} · K:$frameCount" +
            (errorMessage?.let { " ⚠" } ?: "")
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ders Calisma Takibi arkaplanda calisiyor")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Durdur", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (errorMessage != null) {
            // TESHIS AMACLI: hata varsa genisletilince tam mesaji goster - uygulamayi
            // tekrar acmadan, sadece bildirime dokunup genisleterek gorulebilsin.
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$errorMessage"))
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Arkaplan Takibi", NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Ders calisma takibinin arkaplanda calistigini gosteren surekli bildirim" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        AppLogger.log("Servis", "onDestroy")
        StudyEngine.setBackgroundTrackingActive(false)
        releaseWakeLock()
        cameraProvider?.unbindAll()
        faceLandmarkerHelper?.close()
        cameraExecutor.shutdown()
        dummySurface?.release()
        dummySurfaceTexture?.release()
        dummySurface = null
        dummySurfaceTexture = null
        StudyEngine.finalizeSessionIfNeeded()
        super.onDestroy()
    }
}
