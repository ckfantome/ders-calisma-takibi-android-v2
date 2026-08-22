package com.derscalismatakibi.app.core

import kotlin.math.sqrt

/**
 * study_tracker2.py -> class SpeakingDetector
 *
 * Konusma, tek karede agzin acik olmasiyla degil, MAR (Agiz Acikligi Orani)
 * degerinin kisa bir zaman penceresinde surekli DALGALANMASI ile ayirt edilir.
 * Sabit acik/kapali agiz (esneme, sessizce durma) konusma sayilmaz.
 */
class SpeakingDetector(
    private var windowSize: Int = 12,
    private var marStdThreshold: Double = 0.018,
    private var marMinThreshold: Double = 0.028,
) {
    private val history = ArrayDeque<Double>()

    fun setParams(windowSize: Int, marStdThreshold: Double, marMinThreshold: Double) {
        if (windowSize != this.windowSize) history.clear()
        this.windowSize = windowSize
        this.marStdThreshold = marStdThreshold
        this.marMinThreshold = marMinThreshold
    }

    fun reset() = history.clear()

    fun update(mar: Double?): Boolean {
        if (mar == null) {
            history.clear()
            return false
        }
        history.addLast(mar)
        while (history.size > windowSize) history.removeFirst()
        if (history.size < maxOf(4, windowSize / 2)) return false
        val mean = history.average()
        val variance = history.sumOf { (it - mean) * (it - mean) } / history.size
        val std = sqrt(variance)
        val max = history.max()
        return std > marStdThreshold && max > marMinThreshold
    }
}

/**
 * study_tracker2.py -> class SpeakingAwayGate
 *
 * Konusmanin 'dikkat dagitici' sayilmasi icin KENDI BAGIMSIZ onay suresi.
 * Diger durumlarin (uzakta/uyku/calisma) onay surelerinden tamamen ayridir.
 */
class SpeakingAwayGate(private var confirmSeconds: Double = 8.0) {
    private var speakingSinceMs: Long? = null

    fun setParams(confirmSeconds: Double) {
        this.confirmSeconds = confirmSeconds
    }

    fun reset() {
        speakingSinceMs = null
    }

    /** @param nowMs System.currentTimeMillis() gibi bir milisaniye zaman damgasi. */
    fun update(isSpeaking: Boolean, nowMs: Long): Boolean {
        if (!isSpeaking) {
            speakingSinceMs = null
            return false
        }
        val since = speakingSinceMs ?: nowMs.also { speakingSinceMs = it }
        return (nowMs - since) / 1000.0 >= confirmSeconds
    }
}
