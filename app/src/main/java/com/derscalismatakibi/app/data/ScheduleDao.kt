package com.derscalismatakibi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insert(slot: ScheduleSlotEntity): Long

    @Delete
    suspend fun delete(slot: ScheduleSlotEntity)

    @Query("SELECT * FROM schedule_slots ORDER BY day, start_time")
    fun observeAll(): Flow<List<ScheduleSlotEntity>>

    @Query("SELECT * FROM schedule_slots ORDER BY day, start_time")
    suspend fun all(): List<ScheduleSlotEntity>
}
