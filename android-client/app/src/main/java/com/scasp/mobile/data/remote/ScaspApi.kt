package com.scasp.mobile.data.remote

/**
 * The SCASP JSON API (server: web-application MobileApiServlet). GET methods return the raw JSON body so the
 * repository can cache exactly what the server sent. All methods throw [OfflineException] when the server cannot
 * be reached and [ApiException] for HTTP error statuses.
 */
interface ScaspApi {
    suspend fun login(userId: String, password: String): String
    suspend fun logout()
    suspend fun courses(query: String): String
    suspend fun registrations(): String
    suspend fun changeRegistration(action: String, courseCode: String): String
    suspend fun requests(): String
    suspend fun submitRequest(category: String, description: String, priority: Int): String
    suspend fun campusServices(): String
}
