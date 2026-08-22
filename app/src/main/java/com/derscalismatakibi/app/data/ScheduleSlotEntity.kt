package com.derscalismatakibi.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** study_tracker2.py -> schedule.json icindeki (baslangic, bitis, tur) satirlarinin Room karsiligi. */
@Entity(tableName = "schedule_slots")
data class ScheduleSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int, // 0=Pazartesi .. 6=Pazar
    @ColumnInfo(name = "start_time") val startTime: String, // "HH:mm"
    @ColumnInfo(name = "end_time") val endTime: String, // "HH:mm"
    val kind: String, // "calisma" / "mola"
)
