package com.aegis.app.ui.navigation

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aegis.app.ui.screens.ArticleDetailScreen
import com.aegis.app.ui.screens.AuthScreen
import com.aegis.app.ui.screens.ChatScreen
import com.aegis.app.ui.screens.DashboardScreen
import com.aegis.app.ui.screens.GatekeeperScreen
import com.aegis.app.ui.screens.OnboardingScreen
import com.aegis.app.ui.screens.SettingsScreen
import com.aegis.app.ui.screens.NotificationsScreen
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

@Composable
fun AegisNavGraph(
    supabaseClient: SupabaseClient,
    navController: NavHostController = rememberNavController(),
    initialDeepLink: String? = null
) {
    val startDestination = if (supabaseClient.auth.currentSessionOrNull() != null) {
        "gatekeeper"
    } else {
        "auth"
    }

    // Navigate to deep-link once NavHost is ready (e.g. article tapped in notification)
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null && supabaseClient.auth.currentSessionOrNull() != null) {
            // Route looks like "article/UUID" or "notifications"
            navController.navigate(initialDeepLink) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ─────────────────────────────────────────────────────────────
        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate("gatekeeper") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        // ── Gatekeeper ────────────────────────────────────────────────────────
        composable("gatekeeper") {
            GatekeeperScreen(
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("gatekeeper") { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo("gatekeeper") { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // ── Main (bottom-nav shell) ───────────────────────────────────────────
        composable("main") {
            MainShell(
                onArticleClick = { id -> navController.navigate("article/$id") },
                onSignedOut = {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        // ── Article Detail (full screen, no bottom nav) ───────────────────────
        composable(
            route = "article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: return@composable
            ArticleDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ── Inner shell that owns the BottomNav ───────────────────────────────────────
@Composable
private fun MainShell(
    onArticleClick: (String) -> Unit,
    onSignedOut: () -> Unit
) {
    val innerNav = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController = innerNav) },
        containerColor = Color(0xFF0A0E1A)
    ) { innerPadding ->
        NavHost(
            navController = innerNav,
            startDestination = "feed",
            modifier = Modifier.padding(
                bottom = innerPadding.calculateBottomPadding(),
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
        ) {
            composable("feed") {
                DashboardScreen(
                    onArticleClick = onArticleClick,
                    onDailyBriefingClick = {
                        innerNav.navigate("chat?briefing=true")
                    }
                )
            }
            composable("chat?briefing={briefing}",
                arguments = listOf(androidx.navigation.navArgument("briefing") { 
                    defaultValue = false
                    type = androidx.navigation.NavType.BoolType 
                })
            ) { backStackEntry ->
                val isBriefing = backStackEntry.arguments?.getBoolean("briefing") ?: false
                ChatScreen(isBriefingMode = isBriefing)
            }
            composable("notifications") {
                NotificationsScreen(onArticleClick = onArticleClick)
            }
            composable("settings") {
                SettingsScreen(onSignedOut = onSignedOut)
            }
        }
    }
}
