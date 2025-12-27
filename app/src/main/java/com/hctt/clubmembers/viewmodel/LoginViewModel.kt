package com.hctt.clubmembers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hctt.clubmembers.data.repo.AuthRepository
import com.hctt.clubmembers.data.local.RememberMeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rememberMeStore: RememberMeStore
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    init {
        rememberMeStore.load()?.let { creds ->
            _state.value = _state.value.copy(
                email = creds.email,
                password = creds.password,
                rememberMe = true
            )
        }
    }

    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value) }
    fun onPasswordChange(value: String) { _state.value = _state.value.copy(password = value) }
    fun onRememberMeChange(value: Boolean) {
        _state.value = _state.value.copy(rememberMe = value)
        if (!value) rememberMeStore.clear()
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { authRepository.login(_state.value.email.trim(), _state.value.password) }
                .onSuccess {
                    if (_state.value.rememberMe) rememberMeStore.save(_state.value.email.trim(), _state.value.password)
                    else rememberMeStore.clear()
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun loginWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { authRepository.loginWithGoogle() }
                .onSuccess {
                    // Google login doesn't need stored credentials; clear if unchecked.
                    if (!_state.value.rememberMe) rememberMeStore.clear()
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun resumeIfLoggedIn(onSuccess: () -> Unit) {
        if (authRepository.currentSession() != null) onSuccess()
    }
}
