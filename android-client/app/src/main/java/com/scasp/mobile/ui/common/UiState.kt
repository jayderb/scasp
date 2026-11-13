package com.scasp.mobile.ui.common

import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.OfflineException
import java.text.DateFormat
import java.util.Date

/** Screen state kept in a ViewModel, so it survives rotation without reloading. */
data class ListUiState<T>(
    val loading: Boolean = false,
    val items: List<T> = emptyList(),
    val loadedOnce: Boolean = false,
    val origin: Origin? = null,
    val fetchedAtMillis: Long = 0,
    val message: String? = null,     // one-off success text (shown once)
    val error: String? = null,
    val sessionExpired: Boolean = false,
) {
    val empty: Boolean get() = loadedOnce && !loading && items.isEmpty() && error == null
}

object UiText {
    fun banner(origin: Origin?, fetchedAtMillis: Long, format: (Long) -> String = ::time): String? = when (origin) {
        Origin.LIVE -> "Live from server \u00b7 updated ${format(fetchedAtMillis)}"
        Origin.CACHE -> "Offline \u2013 showing data saved at ${format(fetchedAtMillis)}. Pull down to retry."
        Origin.DEMO -> "Demo data \u2013 the SCASP server could not be reached (nothing is sent to the server)."
        null -> null
    }
    fun error(t: Throwable): String = when (t) {
        is OfflineException -> t.message ?: "You are offline."
        is ApiException -> t.message ?: "Server error"
        else -> "Something went wrong: ${t.javaClass.simpleName}"
    }
    fun isSessionExpired(t: Throwable) = t is ApiException && t.status == 401
    private fun time(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
}
