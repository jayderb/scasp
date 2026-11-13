package com.scasp.mobile.ui.campus

import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.ui.common.ListViewModel
import com.scasp.mobile.util.Geo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Location result text is kept here so it is still shown after rotation. */
class CampusViewModel(private val repo: CampusRepository) : ListViewModel<CampusServiceItem>() {
    private val _location = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location.asStateFlow()
    private val _permanentlyDenied = MutableStateFlow(false)
    val permanentlyDenied: StateFlow<Boolean> = _permanentlyDenied.asStateFlow()

    override suspend fun fetch() = repo.campusServices()

    fun onLocation(lat: Double, lon: Double, accuracyM: Float?) {
        val near = Geo.nearest(lat, lon, state.value.items)
        _location.value = buildString {
            append(String.format(java.util.Locale.US, "You are at %.5f, %.5f", lat, lon))
            accuracyM?.let { append(" (\u00b1${it.toInt()} m)") }
            near?.let { append(".\nNearest service: ${it.first.name}, ${Geo.formatDistance(it.second)} away") }
            append(".")
        }
    }
    fun onLocationUnavailable(reason: String) { _location.value = reason }
    fun onPermissionDenied(permanently: Boolean) {
        _permanentlyDenied.value = permanently
        _location.value = "Location permission was denied. Your position is not shown, but you can still open every campus service in a map app."
    }
    fun onPermissionGranted() { _permanentlyDenied.value = false }
}
