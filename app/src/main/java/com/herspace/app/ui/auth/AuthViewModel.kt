package com.herspace.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herspace.app.data.api.ApiClient
import com.herspace.app.data.api.AuthRequest
import com.herspace.app.data.api.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userEmail: String = ""
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(
        isLoggedIn = TokenManager.isLoggedIn(),
        userEmail = TokenManager.getEmail() ?: ""
    ))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(v: String) { _uiState.value = _uiState.value.copy(email = v, error = null) }
    fun updatePassword(v: String) { _uiState.value = _uiState.value.copy(password = v, error = null) }
    fun updateNickname(v: String) { _uiState.value = _uiState.value.copy(nickname = v, error = null) }
    fun toggleMode() { _uiState.value = _uiState.value.copy(isLoginMode = !_uiState.value.isLoginMode, error = null) }

    fun onModeChanged() {
        // 切换模式后重置登录状态
        _uiState.value = AuthUiState()
    }

    fun submit() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.length < 6) {
            _uiState.value = s.copy(error = "邮箱和密码（≥6位）必填")
            return
        }
        _uiState.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val resp = if (s.isLoginMode) {
                    ApiClient.api.login(AuthRequest(s.email.trim(), s.password))
                } else {
                    ApiClient.api.register(AuthRequest(s.email.trim(), s.password, s.nickname.trim()))
                }
                if (resp.success && resp.user != null) {
                    TokenManager.save(resp.user.token, resp.user.email, resp.user.nickname)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, isSuccess = true,
                        isLoggedIn = true, userEmail = resp.user.email
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = resp.error ?: "操作失败")
                }
            } catch (e: Exception) {
                val mode = if (ApiClient.isDebugMode()) "调试" else "线上"
                val url = if (s.isLoginMode) "${ApiClient.getBaseUrl()}api/auth/login" else "${ApiClient.getBaseUrl()}api/auth/register"
                _uiState.value = _uiState.value.copy(isLoading = false, error = "[$mode] $url — ${e.message ?: "网络错误"}")
            }
        }
    }

    fun logout() {
        TokenManager.clear()
        _uiState.value = AuthUiState()
    }
}
