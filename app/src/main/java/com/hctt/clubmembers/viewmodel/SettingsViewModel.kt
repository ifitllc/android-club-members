package com.hctt.clubmembers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: MemberRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    fun syncNow() {
        viewModelScope.launch {
            runCatching { repo.syncBidirectional() }
                .onSuccess {
                    _state.value = _state.value.copy(
                        lastSync = formatter.format(Instant.now().atZone(ZoneId.systemDefault())),
                        error = null
                    )
                }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
