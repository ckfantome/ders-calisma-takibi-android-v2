package com.derscalismatakibi.app.core

/**
 * Masaustu uygulamadaki `State` enum'unun birebir karsiligi.
 * Kaynak: study_tracker2.py -> class State(Enum)
 */
enum class StudyState {
    STUDYING,
    AWAY,
    SLEEPING,
}

/** Kaynak: study_tracker2.py -> @dataclass(frozen=True) class PoseEstimate */
data class PoseEstimate(
    val yaw: Double,
    val pitch: Double,
    val roll: Double,
)

/** Basit 2D nokta (MediaPipe normalized landmark'i piksel koordinatina cevirdikten sonra). */
data class Point2D(val x: Double, val y: Double)

/**
 * Kaynak: study_tracker2.py -> @dataclass class FrameAnalysis
 * `landmarks` alani bilerek tasinmadi (Android tarafinda ciziminin gerekmedigi
 * varsayildi - SHOW_FACE_LANDMARKS ozelligi bu turda kapsam disi).
 */
data class FrameAnalysis(
    val observedState: StudyState,
    val infoText: String,
    val ear: Double? = null,
    val pose: PoseEstimate? = null,
    val mar: Double? = null,
    var isSpeaking: Boolean = false,
    var speakingConfirmed: Boolean = false,
    var forcedAwayBySpeaking: Boolean = false,
)
