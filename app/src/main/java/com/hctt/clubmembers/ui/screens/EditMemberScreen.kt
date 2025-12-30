package com.hctt.clubmembers.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import com.hctt.clubmembers.viewmodel.EditMemberViewModel
import com.hctt.clubmembers.ui.strings.LocalStrings
import coil.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(
    memberId: String?,
    onBack: () -> Unit,
    viewModel: EditMemberViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val rotation = remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    fun showDatePicker() {
        val initialDate = runCatching { LocalDate.parse(state.expiresAt, dateFormatter) }.getOrNull() ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.onExpiry(dateFormatter.format(picked))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri.value = uri
        rotation.value = 0f
    }

    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val output = result.data?.let { UCrop.getOutput(it) }
            if (output != null) {
                photoUri.value = output
                rotation.value = 0f
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        // In this simplified flow we still reuse the photo picker pipeline. For real apps you'd write to a URI.
        bitmap?.let {
            val src = File(context.cacheDir, "capture_${UUID.randomUUID()}.jpg")
            FileOutputStream(src).use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out) }

            val dest = File(context.cacheDir, "crop_${UUID.randomUUID()}.jpg")
            val srcUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", src)
            val destUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
            val intent = UCrop.of(srcUri, destUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1080, 1080)
                .getIntent(context)
                .apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            cropLauncher.launch(intent)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(if (memberId == null) strings.addMemberTitle else strings.editMemberTitle) })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AsyncImage(
                model = photoUri.value ?: state.avatarUrl,
                contentDescription = strings.avatar,
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .graphicsLayer { rotationZ = rotation.value }
            )

            if (state.error != null) {
                Text(
                    state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text(strings.name) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                label = { Text(strings.email) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhone,
                label = { Text(strings.phone) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.expiresAt,
                onValueChange = {},
                label = { Text(strings.expiration) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker() },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker() }) {
                        Icon(Icons.Filled.DateRange, contentDescription = strings.expiration)
                    }
                }
            )
            OutlinedTextField(
                value = state.paymentAmount,
                onValueChange = viewModel::onPayment,
                label = { Text(strings.paymentAmount) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) { Text(strings.attachAvatar) }
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(strings.takePhoto) }
                OutlinedButton(
                    onClick = { rotation.value = (rotation.value + 90f) % 360f },
                    enabled = photoUri.value != null || state.avatarUrl != null,
                    modifier = Modifier.weight(1f)
                ) { Text(strings.rotatePhoto) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.save(memberId?.toLongOrNull(), photoUri.value, rotation.value, onBack) },
                    modifier = Modifier.weight(1f)
                ) { Text(strings.save) }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(strings.paymentHistory, style = MaterialTheme.typography.titleMedium)
                if (state.payments.isEmpty()) {
                    Text(strings.noPayments, style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.payments.forEach {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(it.date, style = MaterialTheme.typography.bodyMedium)
                            Text(it.amount, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(memberId) {
        memberId?.toLongOrNull()?.let { viewModel.load(it) }
    }
}
