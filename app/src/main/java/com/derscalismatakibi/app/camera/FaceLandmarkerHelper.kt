package com.derscalismatakibi.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.derscalismatakibi.app.core.Point2D
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * study_tracker2.py -> CameraWorker.run() icindeki FaceLandmarker kurulum/kullanim
 * kismini sarmalar. Masaustundeki gibi RunningMode.VIDEO + artan zaman damgasi
 * kullanir (mediaPipe'in VIDEO modu, LIVE_STREAM'in aksine senkron/blocking calisir
 * ve CameraX'in kendi arka plan is parcaciginda cagrildigi surece sorun cikarmaz).
 *
 * Model dosyasi: app/src/main/assets/face_landmarker.task (masaustundekiyle AYNI dosya).
 */
class FaceLandmarkerHelper(context: Context) {
    private var lastTimestampMs = -1L
    private val startElapsedMs = System.currentTimeMillis()

    private val landmarker: FaceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build()
            )
            .setRunningMode(RunningMode.VIDEO)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
    )

    /** ImageProxy'yi dogru yonde (rotationDegrees) bir Bitmap'e cevirir. */
    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmapBuffer.copyPixelsFromBuffer(planes[0].buffer)
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) return bitmapBuffer
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
    }

    /**
     * Bir kareyi analiz eder. ImageAnalysis, ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
     * ile kurulmus olmali (bkz. CameraController.kt) - aksi halde copyPixelsFromBuffer
     * yanlis piksel formati uzerinde calisir.
     *
     * NOT (aynali gorunum): On kamera onizlemesi CameraX Preview tarafindan otomatik
     * aynalanir; analiz icin bitmap'i AYRICA aynalamiyoruz cunku EAR/MAR/abs(yaw)
     * hesaplari yatay aynalamadan etkilenmez (bkz. core/FrameAnalyzer.kt yorumlari) -
     * bu, masaustundeki cv2.flip() adiminin sonucu degistirmeden atlanmasidir.
     *
     * @return (landmark noktalari (normalize [0,1]) veya null, genislik, yukseklik)
     */
    fun analyze(imageProxy: ImageProxy): Triple<List<Point2D>?, Int, Int> {
        val bitmap = imageProxy.toUprightBitmap()
        val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
        val elapsedMs = System.currentTimeMillis() - startElapsedMs
        val timestampMs = maxOf(elapsedMs, lastTimestampMs + 1)
        lastTimestampMs = timestampMs
        val result: FaceLandmarkerResult = landmarker.detectForVideo(mpImage, timestampMs)
        val landmarks = result.faceLandmarks()
        val points = if (landmarks.isNotEmpty()) {
            landmarks[0].map { lm -> Point2D(lm.x().toDouble(), lm.y().toDouble()) }
        } else {
            null
        }
        return Triple(points, bitmap.width, bitmap.height)
    }

    fun close() {
        landmarker.close()
    }
}
