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

    // ponytail: 5000 sabit sinir (30sn'de bir ekleniyor, ~1.7 gunluk veri),
    // gerektiginde artir - sinirsiz buyume yerine en eskiyi budar.
    @Query("DELETE FROM location_logs WHERE id NOT IN (SELECT id FROM location_logs ORDER BY timestamp DESC LIMIT 5000)")
    suspend fun trimToRecent()
}
