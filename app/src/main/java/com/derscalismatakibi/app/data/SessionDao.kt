package com.derscalismatakibi.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int = 30): List<SessionEntity>

    /** Gunluk toplam calisma suresi (saniye) - istatistik ekrani icin. */
    @Query(
        """
        SELECT date, SUM(studying_seconds) AS total
        FROM sessions
        GROUP BY date
        ORDER BY date DESC
        LIMIT :limit
        """
    )
    suspend fun dailyTotals(limit: Int = 30): List<DailyTotal>

    /** study_tracker2.py -> get_weekly_stats(): fromDateIso (dahil) sonrasindaki toplam calisma suresi. */
    @Query("SELECT COALESCE(SUM(studying_seconds), 0.0) FROM sessions WHERE date >= :fromDateIso")
    suspend fun weeklyTotal(fromDateIso: String): Double

    /** CSV disa aktarim icin: kaydedilmis tum oturumlar (en yeniden en eskiye). */
    @Query("SELECT * FROM sessions ORDER BY date DESC, id DESC")
    suspend fun allSessions(): List<SessionEntity>

    /** Araliklarla yedeklemede "son gonderimden bu yana yeni veri var mi" kontrolu icin. */
    @Query("SELECT MAX(created_at) FROM sessions")
    suspend fun maxCreatedAt(): Long?
}

data class DailyTotal(
    val date: String,
    val total: Double,
)
