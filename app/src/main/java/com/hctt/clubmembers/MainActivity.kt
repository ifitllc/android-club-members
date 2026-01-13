package com.hctt.clubmembers

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import com.hctt.clubmembers.ui.screens.EditMemberScreen
import com.hctt.clubmembers.ui.screens.ExpiredSearchScreen
import com.hctt.clubmembers.ui.screens.ListScreen
import com.hctt.clubmembers.ui.screens.LoginScreen
import com.hctt.clubmembers.ui.screens.SendEmailScreen
import com.hctt.clubmembers.ui.screens.SettingsScreen
import com.hctt.clubmembers.ui.theme.ClubMembersTheme
import com.hctt.clubmembers.ui.strings.AppLanguage
import com.hctt.clubmembers.ui.strings.ProvideStrings
import com.hctt.clubmembers.data.network.SupabaseClientProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus

import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var supabase: SupabaseClientProvider

    private var intentHolder by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentHolder = intent

        setContent {
            val currentIntent = intentHolder
            LaunchedEffect(currentIntent) {
                if (currentIntent != null) {
                    val data = currentIntent.data
                    Log.d("MainActivity", "Handling deep link intent: $data")
                    if (data != null && data.toString().contains("access_token")) {
                        try {
                            supabase.client.handleDeeplinks(currentIntent) {
                                Log.d("MainActivity", "Deep link processed successfully. User: ${it.user?.email}")
                            }
                        } catch(e: Exception) {
                            Log.e("MainActivity", "Error handling handling deep link", e)
                        }
                    }
                }
            }

            ClubMembersTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isDeepLink = currentIntent?.data?.toString()?.contains("access_token") == true
                    Log.d("MainActivity", "isDeepLink=$isDeepLink, intent data: ${currentIntent?.data}")
                    AppNavigation(supabase, isDeepLink)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        intentHolder = intent
    }
}

sealed class Screen(val route: String) {
    data object Loading : Screen("loading")
    data object Login : Screen("login")
    data object Members : Screen("members")
    data object EditMember : Screen("edit/{memberId}") {
        fun route(memberId: String) = "edit/$memberId"
    }
    data object Settings : Screen("settings")
    data object AddMember : Screen("add")
    data object ExpiredSearch : Screen("expired")
    data object SendEmail : Screen("send_email")
}

@Composable
fun AppNavigation(supabase: SupabaseClientProvider, isDeepLink: Boolean) {
    var language by rememberSaveable { mutableStateOf(AppLanguage.ZH) }
    ProvideStrings(language) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Screen.Loading.route) {
            composable(Screen.Loading.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                val sessionStatus by supabase.client.auth.sessionStatus.collectAsState()
                
                // If it's a deep link, we give it some time to process before deciding to go to Login.
                // We don't want to race effectively against the handleDeeplinks call.
                var deepLinkTimeoutPassed by remember { mutableStateOf(!isDeepLink) }
                
                if (isDeepLink && !deepLinkTimeoutPassed) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000) // Wait up to 2 seconds for deep link processing
                        deepLinkTimeoutPassed = true
                    }
                }

                LaunchedEffect(sessionStatus, deepLinkTimeoutPassed) {
                    Log.d("MainActivity", "LoadingScreen status=$sessionStatus, deepLinkTimeoutPassed=$deepLinkTimeoutPassed")
                    when (sessionStatus) {
                        is SessionStatus.Authenticated -> {
                            Log.d("MainActivity", "Authenticated -> Navigate to Members")
                            navController.navigate(Screen.Members.route) {
                                popUpTo(Screen.Loading.route) { inclusive = true }
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            // Only navigate to login if we are not waiting for a deep link, or if timeout passed
                            if (deepLinkTimeoutPassed) {
                                Log.d("MainActivity", "NotAuthenticated -> Navigate to Login")
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Loading.route) { inclusive = true }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
            composable(Screen.Login.route) {
                // Monitor session status in case of deep link login
                val sessionStatus by supabase.client.auth.sessionStatus.collectAsState()
                LaunchedEffect(sessionStatus) {
                    if (sessionStatus is SessionStatus.Authenticated) {
                        navController.navigate(Screen.Members.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
                LoginScreen(onLoggedIn = {
                    navController.navigate(Screen.Members.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Members.route) {
                ListScreen(
                    onMemberSelected = { navController.navigate(Screen.EditMember.route(it)) },
                    onAddNew = { navController.navigate(Screen.AddMember.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenExpired = { navController.navigate(Screen.ExpiredSearch.route) },
                    onToggleLanguage = { language = language.toggle() },
                    onSendEmail = { navController.navigate(Screen.SendEmail.route) },
                    currentLanguage = language
                )
            }
            composable(Screen.EditMember.route) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId") ?: return@composable
                EditMemberScreen(memberId = memberId, onBack = { navController.popBackStack() })
            }
            composable(Screen.AddMember.route) {
                EditMemberScreen(memberId = null, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ExpiredSearch.route) {
                ExpiredSearchScreen(
                    onBack = { navController.popBackStack() },
                    onMemberSelected = { navController.navigate(Screen.EditMember.route(it)) }
                )
            }
            composable(Screen.SendEmail.route) {
                SendEmailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
