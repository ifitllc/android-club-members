package com.hctt.clubmembers.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(strings.settingsTitle) })
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
        ) {
            Text(strings.offlineCache(state.cacheState))
            Text(strings.lastSync(state.lastSync), modifier = Modifier.padding(top = 4.dp))
            Text(strings.locallyModified(state.locallyModifiedCount), modifier = Modifier.padding(top = 4.dp))
            Button(onClick = viewModel::syncNow, modifier = Modifier.padding(top = 16.dp)) {
                Text(strings.syncNow)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(strings.emailConfiguration, style = MaterialTheme.typography.titleMedium)
            
            var email by remember(state.gmailAddress) { mutableStateOf(state.gmailAddress) }
            var apiKey by remember(state.gmailApiKey) { mutableStateOf(state.gmailApiKey) }
            var passwordVisible by remember { mutableStateOf(false) }
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(strings.gmailAddress) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(strings.appPasswordApiKey) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) strings.hidePassword else strings.showPassword

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = { viewModel.saveEmailConfig(email, apiKey) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(strings.saveEmailConfig)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text(strings.back) }
            state.error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
