package com.hctt.clubmembers.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hctt.clubmembers.ui.theme.Teal
import com.hctt.clubmembers.ui.strings.LocalStrings
import com.hctt.clubmembers.viewmodel.MembersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiredSearchScreen(
    onBack: () -> Unit,
    onMemberSelected: (String) -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(strings.searchExpiredTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = strings.back) }
            }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::onSearchChange,
                label = { Text(strings.searchExpiredFieldLabel) },
                placeholder = { Text(strings.searchExpiredFieldLabel) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text(strings.backToMembers) }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (state.search.isNotEmpty()) {
                item { Text(strings.results, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
                items(state.expiredResults) { member ->
                    MemberRowExpired(
                        name = member.name,
                        expires = member.expiresAt ?: strings.expired,
                        avatarUrl = member.avatarUrl,
                        onDoubleTap = { member.id?.let { onMemberSelected(it) } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberRowExpired(name: String, expires: String, avatarUrl: String?, onDoubleTap: () -> Unit) {
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
        }
    }
}
