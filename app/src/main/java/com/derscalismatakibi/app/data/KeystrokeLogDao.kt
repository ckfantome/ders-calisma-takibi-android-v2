package com.derscalismatakibi.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeystrokeLogDao {
    @Insert
    suspend fun insert(entry: KeystrokeLogEntity): Long

    @Query("SELECT * FROM keystroke_logs ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<KeystrokeLogEntity>>

    @Query("DELETE FROM keystroke_logs")
    suspend fun clear()

    // ponytail: 500 sabit sinir, gerektiginde artir - sinirsiz buyume yerine
    // en eski kayitlari periyodik olarak budar (arka planda cagirilir).
    @Query("DELETE FROM keystroke_logs WHERE id NOT IN (SELECT id FROM keystroke_logs ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trimToRecent()
}
