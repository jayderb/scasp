package com.scasp.mobile.data

import com.scasp.mobile.data.local.MemoryStore
import com.scasp.mobile.data.local.SessionStore
import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.HttpScaspApi
import com.scasp.mobile.data.remote.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Optional end-to-end check of the app's real HTTP client against a running SCASP server with a FRESHLY loaded
 * sample database. Skipped unless SCASP_LIVE_URL is set, for example
 *   SCASP_LIVE_URL=http://localhost:8080/scasp ./gradlew testDebugUnitTest --tests '*LiveServerTest*'
 * (Windows: $env:SCASP_LIVE_URL = "http://localhost:8080/scasp"; .\gradlew.bat testDebugUnitTest --tests "*LiveServerTest*")
 */
class LiveServerTest {
    private val url: String? = System.getenv("SCASP_LIVE_URL")

    @Test fun studentJourneyAgainstTheRealServer() = runBlocking {
        assumeTrue("SCASP_LIVE_URL not set: live server test skipped", !url.isNullOrBlank())
        val session = SessionStore(MemoryStore())
        val api = HttpScaspApi({ url!! }, session)

        val user = JsonParser.user(api.login("STU004", "student123"))
        session.user = user
        assertEquals("STUDENT", user.role)
        assertTrue(user.csrfToken.isNotBlank())

        val courses = JsonParser.courses(api.courses(""))
        assertTrue(courses.map { it.code }.containsAll(listOf("CSC301", "DB302", "HCI308")))
        assertEquals(listOf("DB302"), JsonParser.courses(api.courses("database")).map { it.code })

        println("register: " + JsonParser.message(api.changeRegistration("register", "HCI308"), "?"))
        assertTrue(JsonParser.registrations(api.registrations()).any { it.courseCode == "HCI308" })
        try { api.changeRegistration("register", "HCI308"); fail("duplicate must be rejected") }
        catch (e: ApiException) { assertEquals(409, e.status); println("duplicate: HTTP 409 " + e.message) }

        println("request: " + JsonParser.message(api.submitRequest("TRANSCRIPT", "Transcript for the Android live test", 3), "?"))
        assertTrue(JsonParser.requests(api.requests()).any { it.description == "Transcript for the Android live test" })

        val services = JsonParser.campusServices(api.campusServices())
        assertTrue(services.size >= 3)
        services.forEach { println("campus service: ${it.name} ${it.latitude},${it.longitude}") }

        println("withdraw: " + JsonParser.message(api.changeRegistration("withdraw", "HCI308"), "?"))
        api.logout()
        try { api.registrations(); fail("signed out must be 401") }
        catch (e: ApiException) { assertEquals(401, e.status); println("after logout: HTTP 401") }
    }
}
