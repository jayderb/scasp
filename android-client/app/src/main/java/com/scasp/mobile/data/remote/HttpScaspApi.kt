package com.scasp.mobile.data.remote

import com.scasp.mobile.data.local.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * HttpURLConnection client (no extra library). Every call runs on [io] (never the main thread), uses short
 * timeouts, sends the session cookie and, for changes, the X-CSRF-Token header expected by the server.
 */
class HttpScaspApi(
    private val baseUrl: () -> String,
    private val session: SessionStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 10_000,
) : ScaspApi {

    override suspend fun login(userId: String, password: String): String {
        session.cookie = null                                   // always start a fresh server session
        return call("POST", "/api/login", mapOf("userId" to userId, "password" to password), csrf = false)
    }
    override suspend fun logout() { call("POST", "/api/logout", emptyMap(), csrf = true) }
    override suspend fun courses(query: String) = call("GET", "/api/courses" + if (query.isBlank()) "" else "?q=" + enc(query.trim()))
    override suspend fun registrations() = call("GET", "/api/registrations")
    override suspend fun changeRegistration(action: String, courseCode: String) =
        call("POST", "/api/registrations", mapOf("action" to action, "courseCode" to courseCode), csrf = true)
    override suspend fun requests() = call("GET", "/api/requests")
    override suspend fun submitRequest(category: String, description: String, priority: Int) =
        call("POST", "/api/requests", mapOf("category" to category, "description" to description, "priority" to priority.toString()), csrf = true)
    override suspend fun campusServices() = call("GET", "/api/campus-services")

    private suspend fun call(method: String, path: String, form: Map<String, String>? = null, csrf: Boolean = false): String = withContext(io) {
        val conn = try { URL(baseUrl().trimEnd('/') + path).openConnection() as HttpURLConnection }
                   catch (e: Exception) { throw OfflineException("Invalid server address: ${baseUrl()}", e) }
        try {
            conn.requestMethod = method
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-SCASP-Client", "android")
            session.cookie?.let { conn.setRequestProperty("Cookie", it) }
            if (csrf) session.csrfToken?.let { conn.setRequestProperty("X-CSRF-Token", it) }
            if (form != null) {
                val body = form.entries.joinToString("&") { enc(it.key) + "=" + enc(it.value) }.toByteArray(Charsets.UTF_8)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val status = conn.responseCode
            rememberSessionCookie(conn)
            val text = (if (status >= 400) conn.errorStream else conn.inputStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (status >= 400) throw ApiException(status, JsonParser.error(text) ?: defaultMessage(status))
            if (status in 300..399) throw ApiException(status, "Unexpected redirect: check the server address ends with /scasp")
            text
        } catch (e: IOException) {
            throw OfflineException("Cannot reach the SCASP server at ${baseUrl()}", e)
        } finally {
            conn.disconnect()
        }
    }

    private fun rememberSessionCookie(conn: HttpURLConnection) {
        conn.headerFields.entries.filter { it.key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value }
            .map { it.substringBefore(';').trim() }
            .firstOrNull { it.startsWith("JSESSIONID=") }
            ?.let { session.cookie = it }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun defaultMessage(status: Int) = when (status) {
        401 -> "Your session has expired. Please sign in again."
        403 -> "You are not allowed to do that."
        404 -> "Not found."
        409 -> "That conflicts with existing data."
        else -> "Server error (HTTP $status)."
    }
}
