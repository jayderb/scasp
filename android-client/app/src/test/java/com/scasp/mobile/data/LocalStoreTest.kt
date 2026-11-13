package com.scasp.mobile.data

import com.scasp.mobile.STUDENT
import com.scasp.mobile.data.local.LocalCache
import com.scasp.mobile.data.local.MemoryStore
import com.scasp.mobile.data.local.SessionStore
import com.scasp.mobile.data.local.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalStoreTest {
    @Test fun cacheRemembersJsonAndTimeAndClearsPerUser() {
        var t = 10L
        val c = LocalCache(MemoryStore()) { t }
        c.put("courses", "[1]"); t = 20L; c.put("STU001:registrations", "[2]"); c.put("STU002:registrations", "[3]")
        assertEquals(LocalCache.Entry("[1]", 10L), c.get("courses"))
        c.clearUser("STU001")
        assertNull(c.get("STU001:registrations")); assertEquals("[3]", c.get("STU002:registrations")!!.json)
        c.clearAll(); assertNull(c.get("courses"))
    }
    @Test fun sessionRoundTripsWithoutPassword() {
        val store = MemoryStore(); val s = SessionStore(store)
        s.user = STUDENT; s.cookie = "JSESSIONID=1"
        assertEquals(STUDENT, SessionStore(store).user); assertEquals("csrf-1", s.csrfToken)
        s.clear(); assertNull(s.user); assertNull(s.cookie)
    }
    @Test fun settingsDefaultsAndUrlValidation() {
        val s = Settings(MemoryStore(), "http://10.0.2.2:8080/scasp")
        assertEquals("http://10.0.2.2:8080/scasp", s.baseUrl); assertTrue(s.allowDemoFallback); assertFalse(s.backgroundSync)
        s.baseUrl = " http://192.168.1.20:8080/scasp/ "; assertEquals("http://192.168.1.20:8080/scasp", s.baseUrl)
        listOf("http://10.0.2.2:8080/scasp", "https://scasp.example.ac.zm/scasp", "http://localhost:8080/scasp").forEach { assertTrue(it, Settings.isValidBaseUrl(it)) }
        listOf("", "10.0.2.2:8080", "ftp://x/scasp", "http://bad host/scasp", "javascript:alert(1)").forEach { assertFalse(it, Settings.isValidBaseUrl(it)) }
    }
}
