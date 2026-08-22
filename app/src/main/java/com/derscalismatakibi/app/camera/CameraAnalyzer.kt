package com.derscalismatakibi.app.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.derscalismatakibi.app.core.Point2D

/**
 * CameraX ImageAnalysis.Analyzer: her kareyi FaceLandmarkerHelper'a verir ve
 * sonucu (landmark noktalari + goruntu boyutu) bir callback ile disariya iletir.
 * study_tracker2.py -> CameraWorker.run() icindeki `while self._running` dongusunun
 * her bir iterasyonunun karsiligidir (burada CameraX'in kendi arka plan thread'i
 * bu isi ustleniyor, ayri bir QThread yazmaya gerek yok).
 */
class CameraAnalyzer(
    private val helper: FaceLandmarkerHelper,
    private val onResult: (points: List<Point2D>?, width: Int, height: Int) -> Unit,
    private val onError: (Throwable) -> Unit,
    // TESHIS AMACLI (gecici): CameraX'in bu analyzer'i GERCEKTEN cagirip cagirmadigini
    // (kamera donaniminin arka planda kare gonderip gondermedigini) ayri ve en alt
    // seviyede olcmek icin - MediaPipe/state-machine mantigindan tamamen bagimsiz.
    private val onFrameReceived: () -> Unit = {},
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        onFrameReceived()
        try {
            val (points, w, h) = helper.analyze(imageProxy)
            onResult(points, w, h)
        } catch (t: Throwable) {
            onError(t)
        } finally {
            imageProxy.close()
        }
    }
}
