package com.derscalismatakibi.app.core

/** study_tracker2.py -> fmt_hms() */
fun fmtHms(seconds: Double): String {
    val total = maxOf(0, seconds.toInt())
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/**
 * study_tracker2.py -> @dataclass class Session
 *
 * `startTimeMillis`, oturumun basladigi epoch milisaniye zaman damgasi
 * (masaustundeki `start_time: dt.datetime` alaninin Android karsiligi).
 */
data class Session(
    val startTimeMillis: Long = System.currentTimeMillis(),
    var studyingSeconds: Double = 0.0,
    var awaySeconds: Double = 0.0,
    var sleepingSeconds: Double = 0.0,
    var speakingSeconds: Double = 0.0,
    var pomodoroCycles: Int = 0,
    var notes: String = "",
    var tags: String = "",
) {
    fun totalSeconds(): Double = studyingSeconds + awaySeconds + sleepingSeconds

    fun productivityScore(): Double {
        val total = totalSeconds()
        if (total <= 0) return 0.0
        return Math.round((studyingSeconds / total) * 100 * 10) / 10.0
    }
}
