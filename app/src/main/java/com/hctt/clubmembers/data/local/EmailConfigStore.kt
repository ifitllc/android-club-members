package com.hctt.clubmembers.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailConfigStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("email_config", Context.MODE_PRIVATE)

    fun load(): Config? {
        val email = prefs.getString(KEY_EMAIL, null)
        val apiKey = prefs.getString(KEY_API_KEY, null)
        return if (email.isNullOrBlank() || apiKey.isNullOrBlank()) null else Config(email, apiKey)
    }

    fun save(email: String, apiKey: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_API_KEY, apiKey)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    data class Config(val email: String, val apiKey: String)

    private companion object {
        const val KEY_EMAIL = "email_address"
        const val KEY_API_KEY = "api_key"
    }
}
