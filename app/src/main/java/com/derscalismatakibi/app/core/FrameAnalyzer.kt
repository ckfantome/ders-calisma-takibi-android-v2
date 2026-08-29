package com.derscalismatakibi.app.core

import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point
import org.opencv.core.Point3
import kotlin.math.sqrt

/**
 * Yuz/goz/agiz olcum mantigi - study_tracker2.py icindeki
 * eye_aspect_ratio / mouth_aspect_ratio / estimate_head_pose / analyze_frame
 * fonksiyonlarinin birebir Kotlin karsiligi. Ayni landmark index'leri ve ayni
 * 3D model noktalarini kullanir ki masaustu ile sayisal olarak ayni davransin.
 */

/** analyzeFrame() Context'siz (per-frame, saniyede 15-30 kez cagrilan) saf bir
 * fonksiyon oldugu icin infoText etiketleri disaridan (StudyEngine.appContext
 * uzerinden) hazirlanip parametre olarak geciriliyor - res/values(-en)/strings.xml. */
data class FrameAnalysisLabels(
    val faceNotFound: String,
    val poseNotCalculated: String,
    val eyesClosedFmt: String,
    val studyingFmt: String,
    val headTurnedFmt: String,
)

val LEFT_EYE_EAR_IDX = intArrayOf(33, 160, 158, 133, 153, 144)
val RIGHT_EYE_EAR_IDX = intArrayOf(362, 385, 387, 263, 373, 380)
val MOUTH_MAR_IDX = intArrayOf(61, 291, 13, 14, 78, 308) // [sol, sag, ust-ic, alt-ic, ust-dis, alt-dis]
val HEAD_POSE_LANDMARK_IDX = intArrayOf(1, 152, 33, 263, 61, 291)

/** study_tracker2.py -> HEAD_POSE_MODEL_POINTS (nose tip, chin, sol/sag goz, sol/sag agiz kosesi, mm cinsinden). */
private val HEAD_POSE_MODEL_POINTS: MatOfPoint3f by lazy {
    MatOfPoint3f(
        Point3(0.0, 0.0, 0.0),
        Point3(0.0, -63.6, -12.5),
        Point3(-43.3, 32.7, -26.0),
        Point3(43.3, 32.7, -26.0),
        Point3(-28.9, -28.9, -24.1),
        Point3(28.9, -28.9, -24.1),
    )
}

private fun dist(a: Point2D, b: Point2D): Double {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

/** study_tracker2.py -> eye_aspect_ratio() */
fun eyeAspectRatio(points: List<Point2D>, idx: IntArray, w: Int, h: Int): Double {
    val pts = idx.map { i -> Point2D(points[i].x * w, points[i].y * h) }
    val v1 = dist(pts[1], pts[5])
    val v2 = dist(pts[2], pts[4])
    val horizontal = dist(pts[0], pts[3])
    if (horizontal == 0.0) return 0.3
    return (v1 + v2) / (2.0 * horizontal)
}

/** study_tracker2.py -> mouth_aspect_ratio() */
fun mouthAspectRatio(points: List<Point2D>, idx: IntArray, w: Int, h: Int): Double {
    val pts = idx.map { i -> Point2D(points[i].x * w, points[i].y * h) }
    val left = pts[0]
    val right = pts[1]
    val topIn = pts[2]
    val bottomIn = pts[3]
    val topOut = pts[4]
    val bottomOut = pts[5]
    val vertical = (dist(topIn, bottomIn) + dist(topOut, bottomOut)) / 2.0
    val horizontal = dist(left, right)
    if (horizontal == 0.0) return 0.0
    return vertical / horizontal
}

/**
 * study_tracker2.py -> estimate_head_pose(). OpenCV'nin Calib3d.solvePnP +
 * Rodrigues + decomposeProjectionMatrix fonksiyonlari, Python'daki cv2 ile
 * AYNI C++ OpenCV kutuphanesini sardigi icin sonuclar sayisal olarak eslesir.
 */
fun estimateHeadPose(points: List<Point2D>, w: Int, h: Int): PoseEstimate? {
    val imagePoints = MatOfPoint2f(
        *HEAD_POSE_LANDMARK_IDX.map { i -> Point(points[i].x * w, points[i].y * h) }.toTypedArray()
    )
    val focalLength = w.toDouble()
    val centerX = w / 2.0
    val centerY = h / 2.0
    val cameraMatrix = Mat(3, 3, CvType.CV_64F)
    cameraMatrix.put(0, 0, focalLength, 0.0, centerX, 0.0, focalLength, centerY, 0.0, 0.0, 1.0)
    val distCoeffs = MatOfDouble(0.0, 0.0, 0.0, 0.0)
    val rvec = Mat()
    val tvec = Mat()

    val ok = Calib3d.solvePnP(
        HEAD_POSE_MODEL_POINTS, imagePoints, cameraMatrix, distCoeffs, rvec, tvec,
        false, Calib3d.SOLVEPNP_ITERATIVE,
    )
    if (!ok) return null

    val rotationMat = Mat()
    Calib3d.Rodrigues(rvec, rotationMat)
    val projMatrix = Mat(3, 4, CvType.CV_64F)
    for (r in 0 until 3) {
        for (c in 0 until 3) {
            projMatrix.put(r, c, rotationMat.get(r, c)[0])
        }
        projMatrix.put(r, 3, 0.0)
    }

    val cameraMatrixOut = Mat()
    val rotMatrixOut = Mat()
    val transVectOut = Mat()
    val rotMatrixX = Mat()
    val rotMatrixY = Mat()
    val rotMatrixZ = Mat()
    val eulerAngles = Mat()
    Calib3d.decomposeProjectionMatrix(
        projMatrix, cameraMatrixOut, rotMatrixOut, transVectOut,
        rotMatrixX, rotMatrixY, rotMatrixZ, eulerAngles,
    )
    // eulerAngles 3x1: [pitch, yaw, roll] - OpenCV decomposeProjectionMatrix konvansiyonu,
    // Python tarafindaki `pitch, yaw, roll = (float(a[0]) for a in euler_angles)` ile ayni sira.
    var pitch = eulerAngles.get(0, 0)[0]
    val yaw = eulerAngles.get(1, 0)[0]
    val roll = eulerAngles.get(2, 0)[0]
    if (pitch < -90) {
        pitch = -(180 + pitch)
    } else if (pitch > 90) {
        pitch = 180 - pitch
    }
    return PoseEstimate(yaw = yaw, pitch = pitch, roll = roll)
}

/**
 * study_tracker2.py -> analyze_frame(). `landmarks` null ise (yuz bulunamadi)
 * dogrudan AWAY doner; degilse EAR -> uyku, yaw/pitch -> calisiyor/uzakta karari.
 */
fun analyzeFrame(points: List<Point2D>?, w: Int, h: Int, cfg: AppConfig, labels: FrameAnalysisLabels): FrameAnalysis {
    if (points == null) {
        return FrameAnalysis(StudyState.AWAY, labels.faceNotFound)
    }
    val ear = (
        eyeAspectRatio(points, LEFT_EYE_EAR_IDX, w, h) +
            eyeAspectRatio(points, RIGHT_EYE_EAR_IDX, w, h)
        ) / 2.0
    val mar = mouthAspectRatio(points, MOUTH_MAR_IDX, w, h)
    val pose = estimateHeadPose(points, w, h)
        ?: return FrameAnalysis(StudyState.AWAY, labels.poseNotCalculated, ear = ear, mar = mar)

    if (ear < cfg.earClosedThreshold) {
        return FrameAnalysis(
            StudyState.SLEEPING,
            String.format(labels.eyesClosedFmt, ear),
            ear = ear, pose = pose, mar = mar,
        )
    }
    val yawOk = kotlin.math.abs(pose.yaw) <= cfg.yawMaxDeg
    val pitchOk = -cfg.pitchDownMaxDeg <= pose.pitch && pose.pitch <= cfg.pitchUpMaxDeg
    return if (yawOk && pitchOk) {
        FrameAnalysis(
            StudyState.STUDYING,
            String.format(labels.studyingFmt, pose.yaw.toInt(), pose.pitch.toInt(), ear),
            ear = ear, pose = pose, mar = mar,
        )
    } else {
        FrameAnalysis(
            StudyState.AWAY,
            String.format(labels.headTurnedFmt, pose.yaw.toInt(), pose.pitch.toInt()),
            ear = ear, pose = pose, mar = mar,
        )
    }
}
