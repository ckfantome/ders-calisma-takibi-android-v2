package com.derscalismatakibi.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Haversine - iki koordinat arasi mesafe (metre). Play Services/harita SDK
 * gerekmez, native android.location.Location.distanceTo de ayni isi gorur
 * ama Haversine baglamsiz test edilebilir saf fonksiyon olsun diye burada. */
object LocationHelper {
    private const val EARTH_RADIUS_M = 6371000.0

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
