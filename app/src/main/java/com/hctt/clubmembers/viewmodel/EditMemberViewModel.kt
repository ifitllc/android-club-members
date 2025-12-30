package com.hctt.clubmembers.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hctt.clubmembers.data.repo.MemberRepository
import com.hctt.clubmembers.util.compressImage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.net.URL

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

data class EditMemberState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val expiresAt: String = "",
    val avatarUrl: String? = null,
    val paymentAmount: String = "",
    val payments: List<PaymentItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class PaymentItem(val date: String, val amount: String)

@HiltViewModel
class EditMemberViewModel @Inject constructor(
    app: Application,
    private val repo: MemberRepository
) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(EditMemberState())
    val state: StateFlow<EditMemberState> = _state
    private var capturedBitmap: Bitmap? = null

    fun load(id: Long) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            repo.getMember(id)?.let { member ->
                val signedAvatar = member.avatarUrl?.let { repo.getAvatarUrl(it) } ?: member.avatarUrl
                val payments = repo.getPayments(id).map {
                    PaymentItem(
                        date = formatter.format(it.createdAt.atZone(ZoneOffset.UTC).toLocalDate()),
                        amount = it.amount.toString()
                    )
                }
                _state.value = _state.value.copy(
                    name = member.name,
                    email = member.email.orEmpty(),
                    phone = member.phone.orEmpty(),
                    expiresAt = member.expiration?.let { formatter.format(it) } ?: "",
                    paymentAmount = member.paymentAmount?.toString().orEmpty(),
                    avatarUrl = signedAvatar,
                    payments = payments
                )
            }
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onEmail(v: String) { _state.value = _state.value.copy(email = v) }
    fun onPhone(v: String) { _state.value = _state.value.copy(phone = v) }
    fun onExpiry(v: String) { _state.value = _state.value.copy(expiresAt = v) }
    fun onPayment(v: String) { _state.value = _state.value.copy(paymentAmount = v) }

    fun onCapturedBitmap(bitmap: Bitmap) {
        capturedBitmap = bitmap
    }

    fun save(existingId: Long?, photo: Uri?, rotationDegrees: Float, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val context = getApplication<Application>()
            val normalizedRotation = ((rotationDegrees % 360f) + 360f) % 360f
            val compressed = when {
                photo != null -> context.compressImage(photo, rotationDegrees = normalizedRotation)
                capturedBitmap != null -> {
                    val workingBitmap = capturedBitmap!!.let { bitmap ->
                        if (normalizedRotation == 0f) bitmap else bitmap.rotate(normalizedRotation)
                    }
                    val stream = java.io.ByteArrayOutputStream()
                    workingBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    java.io.ByteArrayInputStream(stream.toByteArray())
                }
                normalizedRotation != 0f && _state.value.avatarUrl != null -> {
                    val existing = loadExistingAvatarBitmap(context.contentResolver, _state.value.avatarUrl!!)
                    existing?.let { bitmap ->
                        val rotated = bitmap.rotate(normalizedRotation)
                        val stream = java.io.ByteArrayOutputStream()
                        rotated.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        java.io.ByteArrayInputStream(stream.toByteArray())
                    }
                }
                else -> null
            }
            if (normalizedRotation != 0f && photo == null && capturedBitmap == null && _state.value.avatarUrl != null && compressed == null) {
                _state.value = _state.value.copy(loading = false, error = "Unable to load existing photo for rotation")
                return@launch
            }

            val expirationDate = _state.value.expiresAt.takeIf { it.isNotBlank() }?.let {
                LocalDate.parse(it, formatter)
            }
            val payment = parsePayment(_state.value.paymentAmount)
            runCatching {
                repo.addOrUpdate(
                    name = _state.value.name,
                    email = _state.value.email.ifBlank { null },
                    phone = _state.value.phone.ifBlank { null },
                    expiration = expirationDate,
                    paymentAmount = payment,
                    avatarInput = compressed,
                    avatarFilename = photo?.lastPathSegment,
                    existingId = existingId
                )
            }.onSuccess { onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(loading = false)
        }
    }

    private fun parsePayment(raw: String): Double? {
        if (raw.isBlank()) return null
        val cleaned = raw.replace("[^\\d.,-]".toRegex(), "").replace(',', '.')
        return cleaned.toDoubleOrNull()
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private suspend fun loadExistingAvatarBitmap(resolver: ContentResolver, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(url) }.getOrNull()

        val httpStream = when {
            uri?.scheme == ContentResolver.SCHEME_CONTENT || uri?.scheme == ContentResolver.SCHEME_FILE ->
                runCatching { resolver.openInputStream(uri!!) }.getOrNull()
            uri?.scheme == "http" || uri?.scheme == "https" ->
                runCatching { URL(url).openStream() }.getOrNull()
            // Possibly a storage path (not signed); request a new signed URL then fetch.
            !url.contains("://") -> repo.getAvatarUrl(url)?.let { signed ->
                runCatching { URL(signed).openStream() }.getOrNull()
            }
            else -> null
        }

        httpStream?.use { BitmapFactory.decodeStream(it) }
    }
}
