package com.derscalismatakibi.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationLogDao {
    @Insert
    suspend fun insert(entry: LocationLogEntity): Long

    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<LocationLogEntity>>

    @Query("SELECT * FROM location_logs ORDER BY timestamp ASC")
    suspend fun all(): List<LocationLogEntity>

    @Query("DELETE FROM location_logs")
    suspend fun clear()

    // Sinirsiz buyume yerine en eskiyi budar - saklanacak kayit sayisi Ayarlar >
    // Gelismis'ten (locationLogRetentionCount, varsayilan 5000 - 30sn'de bir
    // ekleniyor, ~1.7 gunluk veriye denk gelir) kontrol edilir.
    @Query("DELETE FROM location_logs WHERE id NOT IN (SELECT id FROM location_logs ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimToRecent(limit: Int)
}
