package com.scasp.mobile.data

import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.JsonParser
import com.scasp.mobile.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract tests: the fixtures are real responses captured from the deployed SCASP web application. */
class JsonParserTest {
    @Test fun parsesLogin() {
        val u = JsonParser.user(fixture("login.json"))
        assertEquals("STU003", u.userId); assertEquals("John Zulu", u.name); assertEquals("STUDENT", u.role)
        assertFalse(u.staff); assertEquals("TEST-TOKEN", u.csrfToken); assertFalse(u.demo)
    }
    @Test fun parsesCoursesWithSeatMaths() {
        val c = JsonParser.courses(fixture("courses.json"))
        assertEquals(listOf("CSC301", "DB302", "HCI308"), c.map { it.code })
        assertEquals(1, c[0].seatsLeft); assertTrue(c[2].full); assertEquals(1, c[2].waiting)
    }
    @Test fun parsesRegistrationsAndWaitingList() {
        val r = JsonParser.registrations(fixture("registrations.json"))
        assertEquals(2, r.size)
        assertEquals("REGISTERED", r[0].status); assertNull(r[0].position)
        assertEquals("HCI308", r[1].courseCode); assertEquals("WAITLISTED", r[1].status); assertEquals(1, r[1].position)
    }
    @Test fun parsesRequestsAndCampusServices() {
        val q = JsonParser.requests(fixture("requests.json")).single()
        assertEquals("1", q.id); assertEquals("TRANSCRIPT", q.category); assertEquals(4, q.priority); assertEquals("OPEN", q.status)
        val s = JsonParser.campusServices(fixture("campus-services.json"))
        assertEquals(3, s.size)
        assertTrue(s.all { it.illustrative && it.latitude < 0 && it.longitude > 0 })
        assertEquals(-15.4167, s.first { it.id == "LIB" }.latitude, 1e-9)
    }
    @Test fun readsMessagesAndErrors() {
        assertEquals("HCI308 is full: STU003 was added to the waiting list.", JsonParser.message(fixture("register-waitlisted.json"), "x"))
        assertEquals("Request #1 submitted.", JsonParser.message(fixture("request-created.json"), "x"))
        assertEquals("Already on waiting list", JsonParser.error(fixture("error-409.json")))
        assertEquals("Please correct the highlighted fields. (description: Must be 5-1000 characters)", JsonParser.error(fixture("error-400.json")))
        assertNull(JsonParser.error("<html>Payara error page</html>"))
        assertNull(JsonParser.error(""))
    }
    @Test fun emptyListsAndBrokenJson() {
        assertTrue(JsonParser.courses("""{"courses":[]}""").isEmpty())
        assertTrue(JsonParser.registrations("""{"studentId":"STU004","registrations":[],"waiting":[]}""").isEmpty())
        assertEquals(502, assertThrows(ApiException::class.java) { JsonParser.courses("<html>") }.status)
    }
}
