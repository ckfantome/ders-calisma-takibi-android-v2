package com.derscalismatakibi.app.core

/**
 * study_tracker2.py -> class HysteresisStateMachine
 *
 * Gozlemlenen durum (observed) hemen uygulanmaz; ilgili durumun onay suresi
 * (CONFIRM_*_SECONDS) kadar KESINTISIZ surmesi gerekir. Boylece tek bir yanlis
 * karenin (orn. goz kirpma) durumu degistirmesi engellenir.
 */
class HysteresisStateMachine(
    private var cfg: AppConfig,
    initial: StudyState = StudyState.AWAY,
) {
    var state: StudyState = initial
        private set
    private var candidate: StudyState? = null
    private var candidateSinceMs: Long? = null

    fun setConfig(cfg: AppConfig) {
        this.cfg = cfg
    }

    /** Test/ozel durumlar icin state machine'i disaridan zorlamak istersen kullan. */
    fun forceState(newState: StudyState) {
        state = newState
        candidate = null
        candidateSinceMs = null
    }

    /** @param nowMs System.currentTimeMillis() gibi bir milisaniye zaman damgasi. */
    fun update(observed: StudyState, nowMs: Long): StudyState {
        val requiredSecondsByState = mapOf(
            StudyState.STUDYING to cfg.confirmResumeSeconds,
            StudyState.AWAY to cfg.confirmAwaySeconds,
            StudyState.SLEEPING to cfg.confirmSleepSeconds,
        )
        if (observed != state) {
            if (candidate != observed) {
                candidate = observed
                candidateSinceMs = nowMs
            } else {
                val heldForSeconds = (nowMs - (candidateSinceMs ?: nowMs)) / 1000.0
                if (heldForSeconds >= (requiredSecondsByState[observed] ?: 0.0)) {
                    state = observed
                    candidate = null
                    candidateSinceMs = null
                }
            }
        } else {
            candidate = null
            candidateSinceMs = null
        }
        return state
    }
}
