package com.hctt.clubmembers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hctt.clubmembers.viewmodel.SendEmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendEmailScreen(
    onBack: () -> Unit,
    viewModel: SendEmailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Email to Active Members") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.subject,
                onValueChange = viewModel::updateSubject,
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.body,
                onValueChange = viewModel::updateBody,
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), 
                minLines = 5,
                maxLines = 15
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = viewModel::translate,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Translate (CN->EN)")
                }

                Button(
                    onClick = viewModel::send,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send All")
                }
            }
            
            Button(
                onClick = { viewModel.showTestEmailDialog(true) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Test Email")
            }
            
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.showTestEmailDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showTestEmailDialog(false) },
                    title = { Text("Send Test Email") },
                    text = {
                        Column {
                            Text("Enter email address for test:")
                            OutlinedTextField(
                                value = state.testEmailAddress,
                                onValueChange = viewModel::updateTestEmailAddress,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::sendTestEmail) { Text("Send") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showTestEmailDialog(false) }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
