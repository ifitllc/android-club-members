package com.hctt.clubmembers.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hctt.clubmembers.BuildConfig
import com.hctt.clubmembers.data.repo.MemberRepository
import com.hctt.clubmembers.domain.model.Member
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

data class MemberUi(val id: String?, val name: String, val expiresAt: String?, val avatarUrl: String?)

data class MembersState(
    val activeMembers: List<MemberUi> = emptyList(),
    val expiredMembers: List<MemberUi> = emptyList(),
    val expiredResults: List<MemberUi> = emptyList(),
    val search: String = "",
    val sort: SortField = SortField.EXPIRATION,
    val ascending: Boolean = false,
    val expiredSort: SortField = SortField.EXPIRATION,
    val expiredAscending: Boolean = false,
    val expiredPage: Int = 0,
    val expiredPageSize: Int = 20,
    val hasMoreExpired: Boolean = true,
    val isLoadingExpired: Boolean = false
)

enum class SortField { NAME, CREATED, EXPIRATION }

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val repo: MemberRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MembersState())
    val state: StateFlow<MembersState> = _state.asStateFlow()
    val settingsIcon: ImageVector = Icons.Default.Settings
    private var searchJob: Job? = null

    init {
        repo.observeActive()
            .onEach { list ->
                _state.value = _state.value.copy(activeMembers = sort(list.map { it.toUi() }))
            }
            .launchIn(viewModelScope)

        // Pull fresh data on startup so the list isn't empty when local cache is cold.
        viewModelScope.launch { runCatching { repo.pullLatest() } }
        
        // Load first page of expired members
        loadExpiredMembers()
    }

    fun onSearchChange(value: String) {
        _state.value = _state.value.copy(search = value)
        if (value.isBlank()) {
            _state.value = _state.value.copy(expiredResults = emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = repo.searchExpired(value)
            .onEach { list -> _state.value = _state.value.copy(expiredResults = list.map { it.toUi() }) }
            .launchIn(viewModelScope)
    }

    fun sync() {
        viewModelScope.launch { repo.pullLatest() }
    }

    fun onSortSelected(field: SortField) {
        val ascending = if (_state.value.sort == field) !_state.value.ascending else true
        _state.value = _state.value.copy(sort = field, ascending = ascending)
        _state.value = _state.value.copy(activeMembers = sort(_state.value.activeMembers))
    }

    fun onExpiredSortSelected(field: SortField) {
        val ascending = if (_state.value.expiredSort == field) !_state.value.expiredAscending else true
        _state.value = _state.value.copy(expiredSort = field, expiredAscending = ascending)
        loadExpiredMembers()
    }

    private fun loadExpiredMembers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingExpired = true)
            try {
                val sortBy = when (_state.value.expiredSort) {
                    SortField.NAME -> "name"
                    SortField.CREATED -> "created"
                    SortField.EXPIRATION -> "expiration"
                }
                val members = repo.getExpiredPaginated(
                    offset = 0,
                    limit = _state.value.expiredPageSize,
                    sortBy = sortBy,
                    ascending = _state.value.expiredAscending
                )
                _state.value = _state.value.copy(
                    expiredMembers = members.map { it.toUi() },
                    expiredPage = 0,
                    hasMoreExpired = members.size >= _state.value.expiredPageSize,
                    isLoadingExpired = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingExpired = false)
            }
        }
    }

    fun loadMoreExpired() {
        if (_state.value.isLoadingExpired || !_state.value.hasMoreExpired) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingExpired = true)
            try {
                val nextPage = _state.value.expiredPage + 1
                val offset = nextPage * _state.value.expiredPageSize
                val sortBy = when (_state.value.expiredSort) {
                    SortField.NAME -> "name"
                    SortField.CREATED -> "created"
                    SortField.EXPIRATION -> "expiration"
                }
                val newMembers = repo.getExpiredPaginated(
                    offset = offset,
                    limit = _state.value.expiredPageSize,
                    sortBy = sortBy,
                    ascending = _state.value.expiredAscending
                )
                
                _state.value = _state.value.copy(
                    expiredMembers = _state.value.expiredMembers + newMembers.map { it.toUi() },
                    expiredPage = nextPage,
                    hasMoreExpired = newMembers.size >= _state.value.expiredPageSize,
                    isLoadingExpired = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingExpired = false)
            }
        }
    }

    private fun sort(list: List<MemberUi>): List<MemberUi> {
        val sorted = when (_state.value.sort) {
            SortField.NAME -> list.sortedBy { it.name.lowercase() }
            SortField.CREATED -> list.sortedBy { it.id?.toLongOrNull() ?: Long.MAX_VALUE }
            SortField.EXPIRATION -> list.sortedBy { it.expiresAt ?: "9999-12-31" }
        }
        return if (_state.value.ascending) sorted else sorted.reversed()
    }

    private fun sortExpired(list: List<MemberUi>): List<MemberUi> {
        val sorted = when (_state.value.expiredSort) {
            SortField.NAME -> list.sortedBy { it.name.lowercase() }
            SortField.CREATED -> list.sortedBy { it.id?.toLongOrNull() ?: Long.MAX_VALUE }
            SortField.EXPIRATION -> list.sortedBy { it.expiresAt ?: "9999-12-31" }
        }
        return if (_state.value.expiredAscending) sorted else sorted.reversed()
    }
}

private fun Member.toUi(): MemberUi = MemberUi(
    id = id?.toString(),
    name = name,
    expiresAt = expiration?.let { formatter.format(it) },
    avatarUrl = toPublicUrl(avatarUrl)
)

private fun toPublicUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http")) return path
    return "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/avatars/$path"
}
