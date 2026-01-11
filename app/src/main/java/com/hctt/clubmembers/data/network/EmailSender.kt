package com.hctt.clubmembers.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import android.util.Log

@Singleton
class EmailSender @Inject constructor() {

    suspend fun sendEmail(
        senderEmail: String,
        senderPassword: String,
        bccList: List<String> = emptyList(),
        toReceiver: String? = null,
        subject: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            // Trust the host to avoid certificate issues in some Android environments
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(senderEmail, senderPassword)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail))
                if (toReceiver != null) {
                    setRecipient(Message.RecipientType.TO, InternetAddress(toReceiver))
                } else if (bccList.isNotEmpty()) {
                     // If sending to BCC list without a specific TO receiver, set sender as TO
                     // to avoid "Undisclosed recipients" and ensure sender gets a copy.
                    setRecipient(Message.RecipientType.TO, InternetAddress(senderEmail))
                }
                
                if (bccList.isNotEmpty()) {
                    setRecipients(
                        Message.RecipientType.BCC,
                        bccList.map { InternetAddress(it) }.toTypedArray()
                    )
                }
                setSubject(subject)
                setText(body)
            }
            Transport.send(message)
            Log.d("EmailSender", "Email sent successfully. To: $toReceiver, BCC count: ${bccList.size}")
        } catch (e: Exception) {
            Log.e("EmailSender", "Failed to send email", e)
            throw e
        }
    }
}
