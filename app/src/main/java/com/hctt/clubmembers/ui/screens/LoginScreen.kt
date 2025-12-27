package com.hctt.clubmembers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hctt.clubmembers.ui.strings.LocalStrings
import com.hctt.clubmembers.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { viewModel.resumeIfLoggedIn(onLoggedIn) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = strings.adminLogin)
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(strings.email) },
            modifier = Modifier.padding(top = 16.dp)
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(strings.password) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.rememberMe, onCheckedChange = viewModel::onRememberMeChange)
            Text(strings.rememberMe, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.login(onLoggedIn) },
            enabled = !state.loading,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            if (state.loading) CircularProgressIndicator(modifier = Modifier.padding(4.dp)) else Text(strings.login)
        }
        Button(
            onClick = { viewModel.loginWithGoogle(onLoggedIn) },
            enabled = !state.loading,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            if (state.loading) CircularProgressIndicator(modifier = Modifier.padding(4.dp)) else Text(strings.continueWithGoogle)
        }
        state.error?.let { Text(text = it, modifier = Modifier.padding(top = 8.dp)) }
    }
}
