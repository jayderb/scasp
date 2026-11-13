package com.scasp.mobile.data

import com.scasp.mobile.data.local.LocalCache
import com.scasp.mobile.data.local.SessionStore
import com.scasp.mobile.data.local.Settings
import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.data.model.Course
import com.scasp.mobile.data.model.Loaded
import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.model.RegistrationItem
import com.scasp.mobile.data.model.ServiceRequestItem
import com.scasp.mobile.data.model.UserSession
import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.JsonParser
import com.scasp.mobile.data.remote.MockBackend
import com.scasp.mobile.data.remote.OfflineException
import com.scasp.mobile.data.remote.ScaspApi

/**
 * Offline/refresh policy (also in android-client/README.md):
 *  1. Network first: every screen asks the server; a successful answer is shown as LIVE and saved in [LocalCache].
 *  2. Server unreachable: the last saved answer is shown as CACHE with its time ("Offline - saved 14:03").
 *  3. Unreachable and nothing saved: built-in demo data (DEMO) if the user allowed it, otherwise an error.
 *  4. Changes (register, withdraw, new request) need the server; offline they fail with a clear message and
 *     nothing is queued. In a demo session they change only the in-memory demo data.
 *  5. HTTP 401 clears the saved session, so the app returns to the login screen.
 */
class ScaspRepository(
    private val api: ScaspApi,
    private val cache: LocalCache,
    private val session: SessionStore,
    private val settings: Settings,
    private val mock: MockBackend = MockBackend(),
    private val clock: () -> Long = System::currentTimeMillis,
) : CampusRepository {

    override val currentUser: UserSession? get() = session.user
    private val demo get() = session.user?.demo == true
    private fun userKey(name: String) = LocalCache.userKey(session.user?.userId ?: "anonymous", name)

    override suspend fun login(userId: String, password: String): UserSession {
        val user = try {
            JsonParser.user(api.login(userId.trim(), password))
        } catch (e: OfflineException) {
            if (!settings.allowDemoFallback) throw e
            mock.login(userId, password)                    // controlled mock, clearly flagged as demo
        }
        session.user = user
        return user
    }

    override suspend fun logout() {
        val user = session.user
        if (user != null && !user.demo) try { api.logout() } catch (_: Exception) { /* offline or expired: still sign out locally */ }
        user?.let { cache.clearUser(it.userId) }
        session.clear()
    }

    override suspend fun courses(query: String): Loaded<List<Course>> {
        if (demo) return demoLoaded(mock.courses(query))
        return guarded {
            try {
                val json = api.courses(query)
                if (query.isBlank()) cache.put(COURSES, json)
                Loaded(JsonParser.courses(json), Origin.LIVE, clock())
            } catch (e: OfflineException) {
                cache.get(COURSES)?.let { c ->
                    Loaded(JsonParser.courses(c.json).filter { query.isBlank() || it.code.contains(query, true) || it.title.contains(query, true) },
                        Origin.CACHE, c.savedAtMillis)
                } ?: fallback(e) { mock.courses(query) }
            }
        }
    }

    override suspend fun registrations() =
        if (demo) demoLoaded(mock.registrations()) else load(userKey(REGISTRATIONS), { api.registrations() }, JsonParser::registrations) { mock.registrations() }

    override suspend fun requests() =
        if (demo) demoLoaded(mock.requests()) else load(userKey(REQUESTS), { api.requests() }, JsonParser::requests) { mock.requests() }

    override suspend fun campusServices() =
        if (demo) demoLoaded(mock.campusServices) else load(CAMPUS, { api.campusServices() }, JsonParser::campusServices) { mock.campusServices }

    override suspend fun register(courseCode: String): String =
        if (demo) mock.register(courseCode) else change { JsonParser.message(api.changeRegistration("register", courseCode), "Registered for $courseCode.") }

    override suspend fun withdraw(courseCode: String): String =
        if (demo) mock.withdraw(courseCode) else change { JsonParser.message(api.changeRegistration("withdraw", courseCode), "Withdrawn from $courseCode.") }

    override suspend fun submitRequest(category: String, description: String, priority: Int): String =
        if (demo) mock.submit(category, description, priority)
        else change { JsonParser.message(api.submitRequest(category, description, priority), "Request submitted.") }

    // ------------------------------------------------------------------------------------------------ helpers
    private suspend fun <T> load(key: String, fetch: suspend () -> String, parse: (String) -> T, mockData: () -> T): Loaded<T> = guarded {
        try {
            val json = fetch()
            val parsed = parse(json)                        // parse before caching: never cache a broken response
            cache.put(key, json)
            Loaded(parsed, Origin.LIVE, clock())
        } catch (e: OfflineException) {
            cache.get(key)?.let { Loaded(parse(it.json), Origin.CACHE, it.savedAtMillis) } ?: fallback(e, mockData)
        }
    }

    private fun <T> fallback(e: OfflineException, mockData: () -> T): Loaded<T> =
        if (settings.allowDemoFallback) Loaded(mockData(), Origin.DEMO, clock()) else throw e

    private fun <T> demoLoaded(data: T) = Loaded(data, Origin.DEMO, clock())

    private suspend fun change(block: suspend () -> String): String = guarded {
        try { block() } catch (e: OfflineException) {
            throw OfflineException("You are offline. Changes need a connection to the SCASP server; nothing was saved.", e)
        }
    }

    /** 401 = server session gone: forget it so the UI returns to login. */
    private suspend fun <T> guarded(block: suspend () -> T): T = try { block() } catch (e: ApiException) {
        if (e.status == 401) session.clear()
        throw e
    }

    companion object {
        const val COURSES = "courses"
        const val CAMPUS = "campus-services"
        const val REGISTRATIONS = "registrations"
        const val REQUESTS = "requests"
    }
}
