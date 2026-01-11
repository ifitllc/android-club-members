package com.hctt.clubmembers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hctt.clubmembers.data.local.EmailConfigStore
import com.hctt.clubmembers.data.repo.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

data class SettingsState(
    val cacheState: String = "Local DB ready",
    val lastSync: String? = null,
    val locallyModifiedCount: Int = 0,
    val error: String? = null,
    val gmailAddress: String = "",
    val gmailApiKey: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: MemberRepository,
    private val emailConfigStore: EmailConfigStore
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init {
        updateLocallyModifiedCount()
        loadEmailConfig()
    }

    private fun loadEmailConfig() {
        val config = emailConfigStore.load()
        if (config != null) {
            _state.value = _state.value.copy(
                gmailAddress = config.email,
                gmailApiKey = config.apiKey
            )
        }
    }

    fun saveEmailConfig(email: String, apiKey: String) {
        emailConfigStore.save(email, apiKey)
        _state.value = _state.value.copy(
            gmailAddress = email,
            gmailApiKey = apiKey
        )
    }

    fun syncNow() {
        viewModelScope.launch {
            runCatching { repo.syncBidirectional() }
                .onSuccess {
                    _state.value = _state.value.copy(
                        lastSync = formatter.format(Instant.now().atZone(ZoneId.systemDefault())),
                        error = null
                    )
                    updateLocallyModifiedCount()
                }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    private fun updateLocallyModifiedCount() {
        viewModelScope.launch {
            val count = repo.getLocallyModifiedCount()
            _state.value = _state.value.copy(locallyModifiedCount = count)
        }
    }
}
