package com.scasp.mobile.data.local

import android.content.SharedPreferences

/** Tiny key/value abstraction so the cache, session and settings can be unit-tested without Android. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun removeWithPrefix(prefix: String)
}

class SharedPrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) { prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply() }
    override fun getLong(key: String, default: Long) = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun removeWithPrefix(prefix: String) {
        val e = prefs.edit(); prefs.all.keys.filter { it.startsWith(prefix) }.forEach { e.remove(it) }; e.apply()
    }
}

class MemoryStore : KeyValueStore {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Any>()
    override fun getString(key: String) = map[key] as String?
    override fun putString(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
    override fun getLong(key: String, default: Long) = map[key] as Long? ?: default
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean) = map[key] as Boolean? ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun removeWithPrefix(prefix: String) { map.keys.removeIf { it.startsWith(prefix) } }
}
