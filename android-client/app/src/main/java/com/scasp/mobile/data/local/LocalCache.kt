package com.scasp.mobile.data.local

/**
 * Small offline cache: the last successful JSON response per screen, with the time it was saved.
 * Stored in SharedPreferences ("scasp_cache"). Personal entries are keyed by user id and removed on logout.
 * Policy (documented in android-client/README.md): network first; on success the entry is replaced; when the
 * server is unreachable the saved copy is shown with its age; nothing is written while offline.
 */
class LocalCache(private val store: KeyValueStore, private val clock: () -> Long = System::currentTimeMillis) {
    data class Entry(val json: String, val savedAtMillis: Long)

    fun put(key: String, json: String) {
        store.putString(PREFIX + key, json)
        store.putLong(PREFIX + key + TIME, clock())
    }
    fun get(key: String): Entry? = store.getString(PREFIX + key)?.let { Entry(it, store.getLong(PREFIX + key + TIME)) }
    fun clearUser(userId: String) = store.removeWithPrefix("$PREFIX$userId:")
    fun clearAll() = store.removeWithPrefix(PREFIX)

    companion object {
        private const val PREFIX = "cache:"
        private const val TIME = "#savedAt"
        fun userKey(userId: String, name: String) = "$userId:$name"
    }
}
