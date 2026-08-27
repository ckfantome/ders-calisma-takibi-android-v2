package com.derscalismatakibi.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Konum gecmisi: StudyEngine 30sn'de bir (arkaplan servisi calisirken de)
 * ekler - "anlik" degil, surekli degisen TUM konum gecmisi icin (bkz.
 * gunluk yedek e-postasi, ExportHelper.writeLocationHistoryCsv). */
@Entity(tableName = "location_logs")
data class LocationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
)
