package com.hctt.clubmembers.data.network

import android.content.Context
import com.hctt.clubmembers.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class SupabaseClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    encodeDefaults = true
                }
            )
            install(Postgrest)
            install(Storage)
            install(Auth) {
                alwaysAutoRefresh = true
                autoSaveToStorage = true
                flowType = io.github.jan.supabase.gotrue.FlowType.IMPLICIT
                scheme = "com.hctt.clubmembers"
                host = "auth-callback"
                sessionManager = SharedPrefsSessionManager(context)
                // Add debug logger for auth
                // debug = true  <-- Removed invalid property
            }
            httpEngine = OkHttp.create()
        }
    }
}

private class SharedPrefsSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override suspend fun saveSession(session: UserSession) {
        Log.d("SupabaseSession", "Saving session")
        val jsonStr = json.encodeToString(session)
        prefs.edit().putString("session", jsonStr).apply()
    }

    override suspend fun loadSession(): UserSession? {
        Log.d("SupabaseSession", "Loading session")
        val sessionStr = prefs.getString("session", null) ?: return null.also { Log.d("SupabaseSession", "No session found in prefs") }
        return try {
            json.decodeFromString<UserSession>(sessionStr).also { Log.d("SupabaseSession", "Session loaded successfully") }
        } catch (e: Exception) {
            Log.e("SupabaseSession", "Failed to decode session", e)
            null
        }
    }

    override suspend fun deleteSession() {
        Log.d("SupabaseSession", "Deleting session")
        prefs.edit().remove("session").apply()
    }
}
