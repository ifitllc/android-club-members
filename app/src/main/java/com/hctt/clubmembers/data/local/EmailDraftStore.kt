package com.hctt.clubmembers.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailDraftStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("email_draft", Context.MODE_PRIVATE)

    fun load(): Draft {
        val subject = prefs.getString(KEY_SUBJECT, "") ?: ""
        val body = prefs.getString(KEY_BODY, "") ?: ""
        return Draft(subject, body)
    }

    fun save(subject: String, body: String) {
        prefs.edit()
            .putString(KEY_SUBJECT, subject)
            .putString(KEY_BODY, body)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    data class Draft(val subject: String, val body: String)

    private companion object {
        const val KEY_SUBJECT = "draft_subject"
        const val KEY_BODY = "draft_body"
    }
}
