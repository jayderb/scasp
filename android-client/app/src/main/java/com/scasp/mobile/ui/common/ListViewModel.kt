package com.scasp.mobile.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scasp.mobile.data.model.Loaded
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base for list screens: loads in viewModelScope (off the main thread inside the repository), keeps the result in a
 * StateFlow that survives configuration changes, and maps errors to user text.
 */
abstract class ListViewModel<T> : ViewModel() {
    protected val _state = MutableStateFlow(ListUiState<T>())
    val state: StateFlow<ListUiState<T>> = _state.asStateFlow()
    private var loadJob: Job? = null

    protected abstract suspend fun fetch(): Loaded<List<T>>

    /** Loads once; rotation re-subscribes to the existing state instead of loading again. */
    fun loadIfNeeded() { if (!_state.value.loadedOnce && loadJob == null) refresh() }

    fun refresh() {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val r = fetch()
                _state.update { it.copy(loading = false, loadedOnce = true, items = r.data, origin = r.origin, fetchedAtMillis = r.fetchedAtMillis) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(loading = false, loadedOnce = true, error = UiText.error(e), sessionExpired = UiText.isSessionExpired(e)) }
            } finally { loadJob = null }
        }
    }

    /** Runs a change (register, withdraw, submit) then reloads the list. */
    protected fun runAction(action: suspend () -> String, onSuccess: () -> Unit = {}) {
        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val msg = action()
                _state.update { it.copy(message = msg) }
                onSuccess()
                refresh()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(loading = false, error = UiText.error(e), sessionExpired = UiText.isSessionExpired(e)) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
