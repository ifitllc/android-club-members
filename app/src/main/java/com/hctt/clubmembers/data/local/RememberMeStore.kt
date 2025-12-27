package com.hctt.clubmembers.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RememberMeStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("remember_me", Context.MODE_PRIVATE)

    fun load(): Credentials? {
        val email = prefs.getString(KEY_EMAIL, null)
        val password = prefs.getString(KEY_PASSWORD, null)
        return if (email.isNullOrBlank() || password.isNullOrBlank()) null else Credentials(email, password)
    }

    fun save(email: String, password: String) {
        prefs.edit().putString(KEY_EMAIL, email).putString(KEY_PASSWORD, password).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    data class Credentials(val email: String, val password: String)

    private companion object {
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
    }
}
