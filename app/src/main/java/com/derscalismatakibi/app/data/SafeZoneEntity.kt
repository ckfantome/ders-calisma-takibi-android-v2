package com.derscalismatakibi.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Birden fazla Guvenli Bolge (geofence) tanimlanabilir - StudyEngine cihazin
 * herhangi bir ACIK bolgenin icinde olup olmadigini kontrol eder. */
@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Double,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
)
