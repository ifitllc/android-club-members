package com.hctt.clubmembers

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
import com.hctt.clubmembers.ui.screens.EditMemberScreen
import com.hctt.clubmembers.ui.screens.ExpiredSearchScreen
import com.hctt.clubmembers.ui.screens.ListScreen
import com.hctt.clubmembers.ui.screens.LoginScreen
import com.hctt.clubmembers.ui.screens.SettingsScreen
import com.hctt.clubmembers.ui.theme.ClubMembersTheme
import com.hctt.clubmembers.ui.strings.AppLanguage
import com.hctt.clubmembers.ui.strings.ProvideStrings
import com.hctt.clubmembers.data.network.SupabaseClientProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.auth

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var supabase: SupabaseClientProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClubMembersTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(supabase)
                }
            }
        }
        supabase.client.handleDeeplinks(intent)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            supabase.client.handleDeeplinks(intent)
        }
    }
}

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Members : Screen("members")
    data object EditMember : Screen("edit/{memberId}") {
        fun route(memberId: String) = "edit/$memberId"
    }
    data object Settings : Screen("settings")
    data object AddMember : Screen("add")
    data object ExpiredSearch : Screen("expired")
}

@Composable
fun AppNavigation(supabase: SupabaseClientProvider) {
    var language by rememberSaveable { mutableStateOf(AppLanguage.ZH) }
    ProvideStrings(language) {
        val navController = rememberNavController()
        val startDestination = remember {
            if (supabase.client.auth.currentSessionOrNull() != null) Screen.Members.route else Screen.Login.route
        }
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screen.Login.route) {
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
        }
    }
}
