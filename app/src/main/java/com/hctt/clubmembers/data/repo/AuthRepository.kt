package com.hctt.clubmembers.data.repo

import com.hctt.clubmembers.BuildConfig
import com.hctt.clubmembers.data.network.SupabaseClientProvider
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClientProvider
) {
    private val auth: Auth get() = supabase.client.auth

    suspend fun login(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun loginWithGoogle() {
        val redirect = BuildConfig.SUPABASE_REDIRECT_URL.ifBlank { "com.hctt.clubmembers://auth-callback" }
        auth.signInWith(Google, redirectUrl = redirect)
    }

    suspend fun logout() {
        auth.signOut()
    }

    fun currentSession(): UserSession? = auth.currentSessionOrNull()
}
