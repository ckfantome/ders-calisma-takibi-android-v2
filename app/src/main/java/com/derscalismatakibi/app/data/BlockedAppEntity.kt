package com.derscalismatakibi.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ebeveyn-denetim: engellenen/sinirlanan uygulama listesi. */
@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "daily_limit_minutes") val dailyLimitMinutes: Int? = null,
    @ColumnInfo(name = "study_hours_only") val studyHoursOnly: Boolean = true,
)
