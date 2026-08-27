package com.derscalismatakibi.app.core

/** study_tracker2.py -> class PomodoroState(Enum) */
enum class PomodoroState {
    IDLE, WORKING, BREAK, LONG_BREAK, PAUSED,
}

/** study_tracker2.py -> @dataclass class PomodoroTick */
data class PomodoroTick(
    val state: PomodoroState,
    val remainingSeconds: Double = 0.0,
    val justFinishedWork: Boolean = false,
    val justFinishedBreak: Boolean = false,
    val cyclesCompleted: Int = 0,
    val totalPausedSeconds: Double = 0.0,
)

/**
 * study_tracker2.py -> class PomodoroTimer
 *
 * Zaman kaynagi olarak System.currentTimeMillis()/1000.0 (saniye, epoch)
 * kullanilir; masaustundeki time.time() ile ayni birim.
 */
class PomodoroTimer(private var cfg: AppConfig) {
    var state: PomodoroState = PomodoroState.IDLE
        private set
    var cyclesCompleted: Int = 0
        private set
    private var startTime: Double? = null
    private var pauseTime: Double? = null
    private var pausedSeconds: Double = 0.0
    private var pausedState: PomodoroState? = null

    fun setConfig(cfg: AppConfig) {
        this.cfg = cfg
    }

    fun start(now: Double = nowSeconds()) {
        if (state == PomodoroState.IDLE) {
            state = PomodoroState.WORKING
            startTime = now
            pausedSeconds = 0.0
        }
    }

    fun stop() {
        state = PomodoroState.IDLE
        startTime = null
        pauseTime = null
        pausedSeconds = 0.0
        pausedState = null
    }

    fun pause(now: Double = nowSeconds()) {
        if (state != PomodoroState.IDLE && state != PomodoroState.PAUSED) {
            pauseTime = now
            pausedState = state
            state = PomodoroState.PAUSED
        }
    }

    fun resume(now: Double = nowSeconds()) {
        val pt = pauseTime
        if (state == PomodoroState.PAUSED && pt != null) {
            pausedSeconds += now - pt
            state = pausedState ?: PomodoroState.WORKING
            pauseTime = null
            pausedState = null
        }
    }

    fun startManualBreak(now: Double = nowSeconds()) {
        if (state == PomodoroState.WORKING) {
            state = PomodoroState.BREAK
            startTime = now
            pausedSeconds = 0.0
        }
    }

    fun reset() {
        state = PomodoroState.IDLE
        startTime = null
        pauseTime = null
        pausedSeconds = 0.0
        pausedState = null
        cyclesCompleted = 0
    }

    private fun durationFor(s: PomodoroState): Double = when (s) {
        PomodoroState.WORKING -> cfg.pomodoroWorkMin * 60.0
        PomodoroState.BREAK -> cfg.pomodoroBreakMin * 60.0
        PomodoroState.LONG_BREAK -> cfg.pomodoroLongBreakMin * 60.0
        else -> 0.0
    }

    fun tick(now: Double = nowSeconds()): PomodoroTick {
        val st = startTime
        if (state == PomodoroState.IDLE || st == null) {
            return PomodoroTick(state = state, cyclesCompleted = cyclesCompleted, totalPausedSeconds = pausedSeconds)
        }
        if (state == PomodoroState.PAUSED) {
            return PomodoroTick(state = state, cyclesCompleted = cyclesCompleted, totalPausedSeconds = pausedSeconds)
        }
        val elapsed = now - st - pausedSeconds
        val remaining = durationFor(state) - elapsed
        if (remaining > 0) {
            return PomodoroTick(
                state = state, remainingSeconds = remaining,
                cyclesCompleted = cyclesCompleted, totalPausedSeconds = pausedSeconds,
            )
        }
        if (state == PomodoroState.WORKING) {
            cyclesCompleted += 1
            startTime = now
            pausedSeconds = 0.0
            // Ayarlar ekrani 1-12 araligina sinirlar ama bozuk/elle degistirilmis
            // bir DataStore degeri 0 tasirsa % ile ArithmeticException cokerdi.
            state = if (cyclesCompleted % cfg.pomodoroCyclesBeforeLong.coerceAtLeast(1) == 0) {
                PomodoroState.LONG_BREAK
            } else {
                PomodoroState.BREAK
            }
            return PomodoroTick(
                state = state, remainingSeconds = durationFor(state),
                justFinishedWork = true, cyclesCompleted = cyclesCompleted,
                totalPausedSeconds = pausedSeconds,
            )
        }
        state = PomodoroState.IDLE
        startTime = null
        pausedSeconds = 0.0
        return PomodoroTick(state = state, justFinishedBreak = true, cyclesCompleted = cyclesCompleted, totalPausedSeconds = pausedSeconds)
    }

    companion object {
        fun nowSeconds(): Double = System.currentTimeMillis() / 1000.0
    }
}
