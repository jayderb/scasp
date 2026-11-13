package com.scasp.mobile.util

import com.scasp.mobile.data.model.CampusServiceItem
import java.net.URLEncoder
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance in metres (haversine). */
    fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    fun nearest(lat: Double, lon: Double, services: List<CampusServiceItem>): Pair<CampusServiceItem, Double>? =
        services.map { it to distanceMetres(lat, lon, it.latitude, it.longitude) }.minByOrNull { it.second }

    fun formatDistance(m: Double): String = if (m < 1000) "${m.toInt()} m" else String.format(java.util.Locale.US, "%.1f km", m / 1000)

    /** geo: URI understood by Google Maps, OsmAnd, etc. Shows a labelled pin at the service. */
    fun geoUri(s: CampusServiceItem): String =
        "geo:${s.latitude},${s.longitude}?q=${s.latitude},${s.longitude}(" + URLEncoder.encode(s.name, "UTF-8").replace("+", "%20") + ")"

    /** Browser fallback when no map app is installed. */
    fun webMapUrl(s: CampusServiceItem): String = "https://www.google.com/maps/search/?api=1&query=${s.latitude},${s.longitude}"
}
