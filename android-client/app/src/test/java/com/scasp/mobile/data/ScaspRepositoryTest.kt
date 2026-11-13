package com.scasp.mobile.data

import com.scasp.mobile.STUDENT
import com.scasp.mobile.data.local.LocalCache
import com.scasp.mobile.data.local.MemoryStore
import com.scasp.mobile.data.local.SessionStore
import com.scasp.mobile.data.local.Settings
import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.OfflineException
import com.scasp.mobile.data.remote.ScaspApi
import com.scasp.mobile.fixture
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Offline/refresh policy of the repository (network first, cache second, demo data last). */
class ScaspRepositoryTest {
    /** Scripted API: online returns fixtures, offline throws OfflineException. */
    private class FakeApi : ScaspApi {
        var online = true
        var nextError: Exception? = null
        val calls = mutableListOf<String>()
        private fun r(name: String, body: String): String {
            calls += name
            nextError?.let { nextError = null; throw it }
            if (!online) throw OfflineException("offline")
            return body
        }
        override suspend fun login(userId: String, password: String) = r("login", fixture("login.json"))
        override suspend fun logout() { r("logout", "{}") }
        override suspend fun courses(query: String) = r("courses:$query", fixture("courses.json"))
        override suspend fun registrations() = r("registrations", fixture("registrations.json"))
        override suspend fun changeRegistration(action: String, courseCode: String) = r("$action:$courseCode", fixture("register-waitlisted.json"))
        override suspend fun requests() = r("requests", fixture("requests.json"))
        override suspend fun submitRequest(category: String, description: String, priority: Int) = r("submit", fixture("request-created.json"))
        override suspend fun campusServices() = r("campus", fixture("campus-services.json"))
    }

    private var now = 1_000L
    private val api = FakeApi()
    private val prefs = MemoryStore()
    private val session = SessionStore(prefs)
    private val settings = Settings(prefs, "http://10.0.2.2:8080/scasp")
    private val cache = LocalCache(MemoryStore()) { now }
    private val repo = ScaspRepository(api, cache, session, settings, clock = { now })

    @Test fun liveDataIsShownAndCached() = runTest {
        session.user = STUDENT
        val r = repo.registrations()
        assertEquals(Origin.LIVE, r.origin); assertEquals(2, r.data.size)
        assertNotNull(cache.get("STU001:registrations"))
    }

    @Test fun offlineShowsTheSavedCopyWithItsAge() = runTest {
        session.user = STUDENT
        repo.campusServices()                               // saved at t=1000
        api.online = false; now = 5_000L
        val r = repo.campusServices()
        assertEquals(Origin.CACHE, r.origin); assertEquals(1_000L, r.fetchedAtMillis); assertEquals(3, r.data.size)
    }

    @Test fun offlineSearchFiltersTheCachedCatalogue() = runTest {
        session.user = STUDENT
        repo.courses("")
        api.online = false
        val r = repo.courses("database")
        assertEquals(Origin.CACHE, r.origin); assertEquals(listOf("DB302"), r.data.map { it.code })
    }

    @Test fun offlineWithoutCacheUsesDemoDataOnlyWhenAllowed() = runTest {
        session.user = STUDENT; api.online = false
        val r = repo.courses("")
        assertEquals(Origin.DEMO, r.origin); assertEquals(3, r.data.size)
        settings.allowDemoFallback = false
        assertTrue(runCatching { repo.requests() }.exceptionOrNull() is OfflineException)
    }

    @Test fun changesNeedTheServerAndAreNotQueued() = runTest {
        session.user = STUDENT; api.online = false
        val e = runCatching { repo.register("CSC301") }.exceptionOrNull()
        assertTrue(e is OfflineException); assertTrue(e!!.message!!.contains("nothing was saved"))
        api.online = true
        assertEquals("HCI308 is full: STU003 was added to the waiting list.", repo.register("HCI308"))
    }

    @Test fun http401ForgetsTheSession() = runTest {
        session.user = STUDENT; session.cookie = "JSESSIONID=x"
        api.nextError = ApiException(401, "expired")
        assertTrue(runCatching { repo.courses("") }.exceptionOrNull() is ApiException)
        assertNull(session.user); assertNull(session.cookie)
    }

    @Test fun otherHttpErrorsKeepTheSession() = runTest {
        session.user = STUDENT
        api.nextError = ApiException(409, "Duplicate registration")
        assertEquals("Duplicate registration", runCatching { repo.register("CSC301") }.exceptionOrNull()!!.message)
        assertNotNull(session.user)
    }

    @Test fun loginOnlineStoresUserOfflineFallsBackToDemoAccount() = runTest {
        assertEquals("STU003", repo.login("stu003", "student123").userId)
        assertFalse(session.user!!.demo)
        api.online = false
        val demo = repo.login("stu001", "student123")
        assertTrue(demo.demo)
        assertEquals(Origin.DEMO, repo.courses("").origin)            // a demo session never calls the server
        assertTrue(api.calls.none { it.startsWith("courses") })
        assertEquals(401, (runCatching { repo.login("STU001", "wrong") }.exceptionOrNull() as ApiException).status)
    }

    @Test fun logoutClearsSessionAndPersonalCacheEvenOffline() = runTest {
        session.user = STUDENT
        repo.registrations(); repo.courses("")
        api.online = false
        repo.logout()
        assertNull(session.user)
        assertNull(cache.get("STU001:registrations"))
        assertNotNull("shared catalogue stays cached", cache.get("courses"))
    }

    @Test fun demoSessionChangesOnlyTheInMemoryDemoData() = runTest {
        api.online = false
        repo.login("STU001", "student123")
        assertTrue(repo.register("DB302").contains("demo"))
        assertEquals(listOf("DB302"), repo.registrations().data.map { it.courseCode })
        assertTrue(repo.submitRequest("TRANSCRIPT", "Need transcript", 3).contains("demo"))
        assertEquals(1, repo.requests().data.size)
        assertTrue(api.calls.none { it.startsWith("register") || it == "submit" })
    }
}
