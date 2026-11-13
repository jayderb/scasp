package com.scasp.mobile.data

import com.scasp.mobile.STUDENT
import com.scasp.mobile.data.local.MemoryStore
import com.scasp.mobile.data.local.SessionStore
import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.HttpScaspApi
import com.scasp.mobile.data.remote.OfflineException
import com.scasp.mobile.fixture
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

/** HTTP client against a local MockWebServer: headers, cookies, CSRF, form encoding and error mapping. */
class HttpScaspApiTest {
    private val server = MockWebServer()
    private val session = SessionStore(MemoryStore())
    private lateinit var api: HttpScaspApi

    @Before fun start() { server.start(); api = HttpScaspApi({ server.url("/scasp").toString() }, session, connectTimeoutMs = 1000, readTimeoutMs = 2000) }
    @After fun stop() { server.shutdown() }

    @Test fun loginSendsFormAndClientHeaderAndKeepsSessionCookie() = runTest {
        server.enqueue(MockResponse().setBody(fixture("login.json")).addHeader("Set-Cookie", "JSESSIONID=abc123; Path=/scasp; HttpOnly"))
        assertTrue(api.login("STU003", "p@ss word").contains("STU003"))
        val req = server.takeRequest()
        assertEquals("POST", req.method); assertEquals("/scasp/api/login", req.path)
        assertEquals("android", req.getHeader("X-SCASP-Client"))
        assertEquals("userId=STU003&password=p%40ss+word", req.body.readUtf8())
        assertEquals("JSESSIONID=abc123", session.cookie)
    }

    @Test fun changesSendCookieAndCsrfHeader() = runTest {
        session.cookie = "JSESSIONID=abc123"
        session.user = STUDENT.copy(csrfToken = "tok-9")
        server.enqueue(MockResponse().setBody(fixture("register-waitlisted.json")))
        api.changeRegistration("register", "HCI308")
        val req = server.takeRequest()
        assertEquals("JSESSIONID=abc123", req.getHeader("Cookie"))
        assertEquals("tok-9", req.getHeader("X-CSRF-Token"))
        assertEquals("action=register&courseCode=HCI308", req.body.readUtf8())
    }

    @Test fun readsDoNotSendCsrfAndEncodeTheQuery() = runTest {
        session.user = STUDENT
        server.enqueue(MockResponse().setBody(fixture("courses.json")))
        api.courses("adv java")
        val req = server.takeRequest()
        assertEquals("/scasp/api/courses?q=adv+java", req.path)
        assertNull(req.getHeader("X-CSRF-Token"))
    }

    @Test fun httpErrorsBecomeApiExceptionsWithTheServerMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody(fixture("error-409.json")))
        val e = runCatching { api.changeRegistration("register", "HCI308") }.exceptionOrNull() as ApiException
        assertEquals(409, e.status); assertEquals("Already on waiting list", e.message)
        server.enqueue(MockResponse().setResponseCode(401).setBody("<html>not json</html>"))
        val e2 = runCatching { api.registrations() }.exceptionOrNull() as ApiException
        assertEquals(401, e2.status); assertTrue(e2.message!!.contains("session"))
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/scasp/login"))
        assertEquals(302, (runCatching { api.courses("") }.exceptionOrNull() as ApiException).status)
    }

    @Test fun unreachableServerIsOffline() = runTest {
        val freePort = ServerSocket(0).use { it.localPort }
        val offline = HttpScaspApi({ "http://127.0.0.1:$freePort/scasp" }, session, connectTimeoutMs = 500, readTimeoutMs = 500)
        assertTrue(runCatching { offline.courses("") }.exceptionOrNull() is OfflineException)
        val bad = HttpScaspApi({ "not a url" }, session)
        assertTrue(runCatching { bad.courses("") }.exceptionOrNull() is OfflineException)
    }
}
