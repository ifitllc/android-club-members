package com.hctt.clubmembers.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hctt.clubmembers.ui.strings.LocalStrings
import com.hctt.clubmembers.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(strings.settingsTitle) })
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(strings.offlineCache(state.cacheState))
            Text(strings.lastSync(state.lastSync), modifier = Modifier.padding(top = 4.dp))
            Button(onClick = viewModel::syncNow, modifier = Modifier.padding(top = 16.dp)) {
                Text(strings.syncNow)
            }
            Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text(strings.back) }
            state.error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
