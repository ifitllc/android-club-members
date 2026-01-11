package com.hctt.clubmembers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hctt.clubmembers.data.local.EmailConfigStore
import com.hctt.clubmembers.data.local.EmailDraftStore
import com.hctt.clubmembers.data.network.EmailSender
import com.hctt.clubmembers.data.repo.MemberRepository
import com.hctt.clubmembers.util.MemberTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SendEmailState(
    val subject: String = "",
    val body: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showTestEmailDialog: Boolean = false,
    val testEmailAddress: String = "info@funplaysports-md.com"
)

@HiltViewModel
class SendEmailViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val emailConfigStore: EmailConfigStore,
    private val emailDraftStore: EmailDraftStore,
    private val emailSender: EmailSender,
    private val translator: MemberTranslator
) : ViewModel() {

    private val _state = MutableStateFlow(SendEmailState())
    val state: StateFlow<SendEmailState> = _state

    init {
        loadDraft()
    }

    private fun loadDraft() {
        val draft = emailDraftStore.load()
        _state.value = _state.value.copy(
            subject = draft.subject,
            body = draft.body
        )
    }

    fun updateSubject(subject: String) {
        _state.value = _state.value.copy(subject = subject)
        saveDraft()
    }

    fun updateBody(body: String) {
        _state.value = _state.value.copy(body = body)
        saveDraft()
    }

    fun showTestEmailDialog(show: Boolean) {
        _state.value = _state.value.copy(showTestEmailDialog = show)
    }

    fun updateTestEmailAddress(email: String) {
        _state.value = _state.value.copy(testEmailAddress = email)
    }

    private fun saveDraft() {
        emailDraftStore.save(_state.value.subject, _state.value.body)
    }

    fun translate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val currentSubject = _state.value.subject
                val currentBody = _state.value.body

                // Only translate if not already seemingly translated (simple check or always translate)
                // Appending translations as English first, then Chinese
                
                val translatedSubject = if (currentSubject.isNotBlank()) translator.translateChineseToEnglish(currentSubject) else ""
                val translatedBody = if (currentBody.isNotBlank()) translator.translateChineseToEnglish(currentBody) else ""
                
                // Construct English + Chinese format
                val newSubject = if (translatedSubject.isNotBlank()) "$translatedSubject / $currentSubject" else currentSubject
                val newBody = if (translatedBody.isNotBlank()) "$translatedBody\n\n----------------\n\n$currentBody" else currentBody

                _state.value = _state.value.copy(
                    subject = newSubject,
                    body = newBody,
                    isLoading = false
                )
                saveDraft() 
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Translation failed: ${e.localizedMessage}")
            }
        }
    }

    fun sendTestEmail() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, successMessage = null, showTestEmailDialog = false)
            try {
                val config = emailConfigStore.load()
                if (config == null) throw Exception("Email configuration not set. Please go to Settings.")

                 emailSender.sendEmail(
                    senderEmail = config.email,
                    senderPassword = config.apiKey,
                    toReceiver = _state.value.testEmailAddress,
                    subject = _state.value.subject,
                    body = _state.value.body
                )
                 _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Test email sent to ${_state.value.testEmailAddress}"
                )
            } catch (e: Exception) {
                 _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun send() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val config = emailConfigStore.load()
                if (config == null) {
                    throw Exception("Email configuration not set. Please go to Settings.")
                }

                val activeMembers = memberRepository.observeActive().first()
                val emails = activeMembers.mapNotNull { it.email }.filter { it.isNotBlank() }

                if (emails.isEmpty()) {
                     throw Exception("No active members with email addresses found.")
                }

                emailSender.sendEmail(
                    senderEmail = config.email,
                    senderPassword = config.apiKey,
                    bccList = emails,
                    subject = _state.value.subject,
                    body = _state.value.body
                )

                _state.value = _state.value.copy(isLoading = false, subject = "", body = "", successMessage = "Email sent to ${emails.size} members.")
                saveDraft() // Clear the draft (saved empty strings)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error")
            }
        }
    }
    
    fun clearMessages() {
         _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
