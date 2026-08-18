package com.gondroid.quoteanime.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gondroid.quoteanime.presentation.catalog.CatalogScreen
import com.gondroid.quoteanime.presentation.home.HomeScreen
import com.gondroid.quoteanime.presentation.onboarding.OnboardingScreen
import com.gondroid.quoteanime.presentation.routine.HabitDetailScreen
import com.gondroid.quoteanime.presentation.routine.HabitEditorSheet
import com.gondroid.quoteanime.presentation.routine.RoutineScreen
import com.gondroid.quoteanime.presentation.settings.SettingsScreen
import com.gondroid.quoteanime.presentation.settings.WidgetTutorialScreen
import com.gondroid.quoteanime.presentation.splash.SplashScreen
import com.gondroid.quoteanime.presentation.subscription.PaywallScreen
import com.gondroid.quoteanime.presentation.web.WebViewScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home") {
        const val ARG_QUOTE_ID = "quoteId"
        val routeWithArg = "home?$ARG_QUOTE_ID={$ARG_QUOTE_ID}"
        fun createRoute(quoteId: String?) =
            if (quoteId != null) "home?$ARG_QUOTE_ID=$quoteId" else "home"
    }
    data object Settings : Screen("settings")
    data object WidgetTutorial : Screen("widget_tutorial")
    data object Catalog : Screen("catalog") {
        const val ARG = "categoryId"
        val routeWithArg = "catalog?$ARG={$ARG}"
        fun createRoute(categoryId: String?) =
            if (categoryId != null) "catalog?$ARG=$categoryId" else "catalog"
    }
    data object Routine : Screen("routine")
    data object HabitEditor : Screen("habit_editor") {
        const val ARG = "habitId"
        val routeWithArg = "habit_editor?$ARG={$ARG}"
        fun createRoute(habitId: String?) =
            if (habitId != null) "habit_editor?$ARG=$habitId" else "habit_editor"
    }
    data object HabitDetail : Screen("habit_detail") {
        const val ARG = "habitId"
        val routeWithArg = "habit_detail/{$ARG}"
        fun createRoute(habitId: String) = "habit_detail/$habitId"
    }
    data object Paywall : Screen("paywall")

    /** In-app browser — see [com.gondroid.quoteanime.presentation.web.WebViewScreen]. */
    data object WebView : Screen("webview") {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"
        val routeWithArgs = "webview?$ARG_URL={$ARG_URL}&$ARG_TITLE={$ARG_TITLE}"

        /** Both values are encoded: an un-escaped `://` or `&` would break the route match. */
        fun createRoute(url: String, title: String) =
            "webview?$ARG_URL=${android.net.Uri.encode(url)}&$ARG_TITLE=${android.net.Uri.encode(title)}"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startQuoteId: String? = null,
    openRoutine: Boolean = false
) {
    // If app was opened via widget tap or habit reminder tap, skip splash/onboarding
    // and go directly to the relevant destination.
    val start = when {
        startQuoteId != null -> Screen.Home.createRoute(startQuoteId)
        openRoutine -> Screen.Routine.route
        else -> Screen.Splash.route
    }

    // No outer Scaffold/bottom bar: Frases, Catálogo and Mi rutina are reached via normal
    // push/pop navigation (with a floating shortcut row on Frases), not persistent tabs —
    // this is what lets each screen's background go fully edge-to-edge with zero insets
    // the NavHost would otherwise have to reserve for a docked bar.
    NavHost(
        navController = navController,
        startDestination = start
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) }
            )
        }

        composable(
            route = Screen.Home.routeWithArg,
            arguments = listOf(
                navArgument(Screen.Home.ARG_QUOTE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            HomeScreen(
                onNavigateToCatalog = { categoryId ->
                    navController.navigate(Screen.Catalog.createRoute(categoryId))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToRoutine = { navController.navigate(Screen.Routine.route) }
            )
        }

        composable(
            route = Screen.Catalog.routeWithArg,
            arguments = listOf(
                navArgument(Screen.Catalog.ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CatalogScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWidgetTutorial = { navController.navigate(Screen.WidgetTutorial.route) },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                onNavigateToWebView = { url, title ->
                    navController.navigate(Screen.WebView.createRoute(url, title))
                }
            )
        }

        composable(Screen.WidgetTutorial.route) {
            WidgetTutorialScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Paywall.route) {
            PaywallScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWebView = { url, title ->
                    navController.navigate(Screen.WebView.createRoute(url, title))
                }
            )
        }

        composable(
            route = Screen.WebView.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.WebView.ARG_URL) { type = NavType.StringType },
                navArgument(Screen.WebView.ARG_TITLE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            WebViewScreen(
                url = backStackEntry.arguments?.getString(Screen.WebView.ARG_URL).orEmpty(),
                title = backStackEntry.arguments?.getString(Screen.WebView.ARG_TITLE).orEmpty(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Routine.route) {
            RoutineScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddHabit = {
                    navController.navigate(Screen.HabitEditor.createRoute(null))
                },
                onEditHabit = { habitId ->
                    navController.navigate(Screen.HabitEditor.createRoute(habitId))
                },
                onOpenHabitDetail = { habitId ->
                    navController.navigate(Screen.HabitDetail.createRoute(habitId))
                },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) }
            )
        }

        dialog(
            route = Screen.HabitDetail.routeWithArg,
            arguments = listOf(navArgument(Screen.HabitDetail.ARG) { type = NavType.StringType })
        ) {
            HabitDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditHabit = { habitId ->
                    navController.navigate(Screen.HabitEditor.createRoute(habitId))
                }
            )
        }

        dialog(
            route = Screen.HabitEditor.routeWithArg,
            arguments = listOf(
                navArgument(Screen.HabitEditor.ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            HabitEditorSheet(
                onDismiss = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) }
            )
        }
    }
}
