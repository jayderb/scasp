package com.scasp.mobile.data.model

/** Signed-in user as returned by POST /api/login (the CSRF token is needed for every change). */
data class UserSession(
    val userId: String,
    val name: String,
    val role: String,
    val staff: Boolean,
    val csrfToken: String,
    val demo: Boolean = false,
)

data class Course(val code: String, val title: String, val capacity: Int, val enrolled: Int, val waiting: Int) {
    val seatsLeft: Int get() = (capacity - enrolled).coerceAtLeast(0)
    val full: Boolean get() = enrolled >= capacity
}

/** status is REGISTERED or WAITLISTED; position is set for waiting-list entries. */
data class RegistrationItem(val courseCode: String, val status: String, val position: Int? = null)

data class ServiceRequestItem(
    val id: String,
    val category: String,
    val description: String,
    val priority: Int,
    val status: String,
    val createdAt: String?,
)

/** Campus service with map coordinates. The coordinates in the sample data are ILLUSTRATIVE only. */
data class CampusServiceItem(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val illustrative: Boolean = true,
)

/** Where the data on screen came from; shown to the user in the status banner. */
enum class Origin { LIVE, CACHE, DEMO }

data class Loaded<T>(val data: T, val origin: Origin, val fetchedAtMillis: Long)

val SERVICE_REQUEST_CATEGORIES = listOf("TRANSCRIPT", "CLEARANCE", "APPEAL", "REGISTRATION", "OTHER")
