package com.derscalismatakibi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Insert
    suspend fun insert(app: BlockedAppEntity): Long

    @Delete
    suspend fun delete(app: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps ORDER BY app_label")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps ORDER BY app_label")
    suspend fun all(): List<BlockedAppEntity>
}
