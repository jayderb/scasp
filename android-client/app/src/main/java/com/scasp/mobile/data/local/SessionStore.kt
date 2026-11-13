package com.scasp.mobile.data.local

import com.scasp.mobile.data.model.UserSession

/**
 * Remembers the signed-in user, the server session cookie and the CSRF token so the app (and the background
 * sync worker) can call the API after a restart. Cleared on logout or when the server answers 401.
 * The password is never stored.
 */
class SessionStore(private val store: KeyValueStore) {
    var cookie: String?
        get() = store.getString("session.cookie")
        set(v) = store.putString("session.cookie", v)

    var user: UserSession?
        get() {
            val id = store.getString("session.userId") ?: return null
            return UserSession(id, store.getString("session.name") ?: id, store.getString("session.role") ?: "STUDENT",
                store.getBoolean("session.staff", false), store.getString("session.csrf") ?: "", store.getBoolean("session.demo", false))
        }
        set(u) {
            store.putString("session.userId", u?.userId); store.putString("session.name", u?.name)
            store.putString("session.role", u?.role); store.putString("session.csrf", u?.csrfToken)
            store.putBoolean("session.staff", u?.staff ?: false); store.putBoolean("session.demo", u?.demo ?: false)
        }

    val csrfToken: String? get() = store.getString("session.csrf")

    fun clear() { cookie = null; user = null }
}
