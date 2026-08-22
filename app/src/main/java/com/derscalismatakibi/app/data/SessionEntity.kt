package com.derscalismatakibi.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * study_tracker2.py -> _init_db() icindeki `sessions` tablosunun Room karsiligi.
 * Alan adlari ve turleri masaustu SQLite semasiyla birebir eslesir.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // ISO-8601 (YYYY-MM-DD)
    @ColumnInfo(name = "start_time") val startTime: String, // HH:mm:ss
    @ColumnInfo(name = "end_time") val endTime: String, // HH:mm:ss
    @ColumnInfo(name = "studying_seconds") val studyingSeconds: Double,
    @ColumnInfo(name = "away_seconds") val awaySeconds: Double,
    @ColumnInfo(name = "sleeping_seconds") val sleepingSeconds: Double,
    @ColumnInfo(name = "total_seconds") val totalSeconds: Double,
    @ColumnInfo(name = "speaking_seconds") val speakingSeconds: Double = 0.0,
    @ColumnInfo(name = "pomodoro_cycles") val pomodoroCycles: Int = 0,
    val notes: String? = null,
    val tags: String? = null,
    @ColumnInfo(name = "productivity_score") val productivityScore: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
