package com.scasp.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.local.Settings
import com.scasp.mobile.data.model.UserSession
import com.scasp.mobile.ui.common.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(val loading: Boolean = false, val error: String? = null, val user: UserSession? = null)

class LoginViewModel(private val repo: CampusRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(user = repo.currentUser))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Validates input first (no network call for obviously bad input), then signs in on a background thread. */
    fun login(baseUrl: String, userId: String, password: String, applySettings: (String) -> Unit) {
        val problem = validate(baseUrl, userId, password)
        if (problem != null) { _state.value = LoginUiState(error = problem); return }
        applySettings(baseUrl.trim())
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            _state.value = try { LoginUiState(user = repo.login(userId, password)) }
                           catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; LoginUiState(error = UiText.error(e)) }
        }
    }

    companion object {
        fun validate(baseUrl: String, userId: String, password: String): String? = when {
            !Settings.isValidBaseUrl(baseUrl) -> "Enter a server address like http://10.0.2.2:8080/scasp"
            !userId.trim().matches(Regex("[A-Za-z0-9_-]{2,30}")) -> "Enter your user id (for example STU001)"
            password.isEmpty() -> "Enter your password"
            else -> null
        }
    }
}
