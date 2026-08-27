package com.derscalismatakibi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeZoneDao {
    @Insert
    suspend fun insert(zone: SafeZoneEntity): Long

    @Update
    suspend fun update(zone: SafeZoneEntity)

    @Delete
    suspend fun delete(zone: SafeZoneEntity)

    @Query("SELECT * FROM safe_zones ORDER BY name")
    fun observeAll(): Flow<List<SafeZoneEntity>>

    @Query("SELECT * FROM safe_zones ORDER BY name")
    suspend fun all(): List<SafeZoneEntity>
}
