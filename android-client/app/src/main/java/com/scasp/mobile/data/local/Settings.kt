package com.scasp.mobile.data.local

/** User-changeable settings (login screen and home screen). */
class Settings(private val store: KeyValueStore, private val defaultBaseUrl: String) {
    /** e.g. http://10.0.2.2:8080/scasp on the emulator; http://<PC LAN IP>:8080/scasp on a phone. */
    var baseUrl: String
        get() = store.getString("settings.baseUrl") ?: defaultBaseUrl
        set(v) = store.putString("settings.baseUrl", v.trim().trimEnd('/'))

    /** When the server is unreachable and nothing is cached, show the built-in demo data instead of an error. */
    var allowDemoFallback: Boolean
        get() = store.getBoolean("settings.demoFallback", true)
        set(v) = store.putBoolean("settings.demoFallback", v)

    var backgroundSync: Boolean
        get() = store.getBoolean("settings.backgroundSync", false)
        set(v) = store.putBoolean("settings.backgroundSync", v)

    companion object {
        /** Accepts http(s)://host[:port]/path without spaces. */
        fun isValidBaseUrl(url: String): Boolean = Regex("^https?://[A-Za-z0-9.\\-\\[\\]:]+(:\\d{1,5})?(/[A-Za-z0-9._~\\-/]*)?$").matches(url.trim())
    }
}
