package com.hctt.clubmembers.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hctt.clubmembers.ui.theme.Teal
import com.hctt.clubmembers.ui.strings.LocalStrings
import com.hctt.clubmembers.viewmodel.MembersViewModel
import com.hctt.clubmembers.viewmodel.SortField
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ExpiredSearchScreen(
    onBack: () -> Unit,
    onMemberSelected: (String) -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        scope.launch {
            viewModel.sync()
            refreshing = false
        }
    })

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(strings.searchExpiredTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = strings.back) }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(strings.backToMembers) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(label = strings.sortName, selected = state.expiredSort == SortField.NAME, ascending = state.expiredAscending) {
                    viewModel.onExpiredSortSelected(SortField.NAME)
                }
                SortChip(label = strings.sortCreated, selected = state.expiredSort == SortField.CREATED, ascending = state.expiredAscending) {
                    viewModel.onExpiredSortSelected(SortField.CREATED)
                }
                SortChip(label = strings.sortExpiration, selected = state.expiredSort == SortField.EXPIRATION, ascending = state.expiredAscending) {
                    viewModel.onExpiredSortSelected(SortField.EXPIRATION)
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.expiredMembers) { member ->
                    MemberRow(
                        name = member.name,
                        expires = member.expiresAt ?: strings.expired,
                        avatarUrl = member.avatarUrl,
                        onDoubleTap = { member.id?.let { onMemberSelected(it) } },
                        onDelete = { member.id?.toLongOrNull()?.let { viewModel.deleteMember(it) } }
                    )
                }
                
                if (state.hasMoreExpired && !state.isLoadingExpired) {
                    item {
                        Button(
                            onClick = { viewModel.loadMoreExpired() },
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        ) {
                            Text("Load More")
                        }
                    }
                }
                
                if (state.isLoadingExpired) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, ascending: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, colors = ButtonDefaults.outlinedButtonColors()) {
        Text(label)
        if (selected) {
            val arrow = if (ascending) "↑" else "↓"
            Text(" $arrow")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberRow(name: String, expires: String, avatarUrl: String?, onDoubleTap: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(onClick = {}, onDoubleClick = onDoubleTap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = strings.avatar,
                    modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(expires, style = MaterialTheme.typography.bodySmall, color = Teal)
                }
            }
            Spacer(modifier = Modifier.weight(0.01f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete member")
            }
        }
    }
}
