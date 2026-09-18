package com.gondroid.quoteanime.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    data object Splash : Screen("splash") {
        /** Set when a deep link is waiting: the user asked for a destination, so the intro is skipped. */
        const val ARG_SKIP_INTRO = "skipIntro"
        val routeWithArg = "splash?$ARG_SKIP_INTRO={$ARG_SKIP_INTRO}"
        fun createRoute(skipIntro: Boolean) =
            if (skipIntro) "splash?$ARG_SKIP_INTRO=true" else "splash"
    }
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
    deepLinkRouter: DeepLinkRouter = remember { DeepLinkRouter() }
) {
    // Every launch goes through the splash, which checks the onboarding: a widget or reminder
    // tap never skips an onboarding that isn't complete. Its destination waits in
    // [deepLinkRouter] and is applied when the app reaches Home (see [enterMain]); with one
    // waiting, the splash skips its intro animation. Saveable so a configuration change
    // doesn't swap the graph's start destination.
    val start = rememberSaveable {
        Screen.Splash.createRoute(skipIntro = deepLinkRouter.pending != null)
    }

    // No outer Scaffold/bottom bar: Frases, Catálogo and Mi rutina are reached via normal
    // push/pop navigation (with a floating shortcut row on Frases), not persistent tabs —
    // this is what lets each screen's background go fully edge-to-edge with zero insets
    // the NavHost would otherwise have to reserve for a docked bar.
    NavHost(
        navController = navController,
        startDestination = start
    ) {
        composable(
            route = Screen.Splash.routeWithArg,
            arguments = listOf(
                navArgument(Screen.Splash.ARG_SKIP_INTRO) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            SplashScreen(
                onNavigateToHome = {
                    navController.enterMain(deepLinkRouter.enterMain(), leaving = Screen.Splash.routeWithArg)
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.routeWithArg) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.enterMain(deepLinkRouter.enterMain(), leaving = Screen.Onboarding.route)
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

/**
 * Replaces the splash or the onboarding ([leaving]) with [routes], bottom first — Home, plus
 * whatever a pending deep link asked for on top of it.
 */
private fun NavHostController.enterMain(routes: List<String>, leaving: String) {
    routes.forEachIndexed { index, route ->
        navigate(route) {
            if (index == 0) popUpTo(leaving) { inclusive = true }
        }
    }
}

/** True once Home is on the back stack, i.e. the splash and the onboarding are behind us. */
fun NavHostController.isInMainContent(): Boolean =
    currentBackStack.value.any { it.destination.route == Screen.Home.routeWithArg }

/**
 * Shows a deep link's [routes] (see [mainBackStack]) while the app is already in its main
 * content: everything above Home is dropped, so "back" from the destination returns to Home.
 * The Home already on the stack is kept (with its pager position) unless the link asks for a
 * specific quote, which needs a Home positioned on it.
 */
fun NavHostController.showMainBackStack(routes: List<String>) {
    val home = routes.first()
    val above = routes.drop(1)
    if (home == Screen.Home.route) {
        val currentAboveHome = currentBackStack.value
            .map { it.destination.route }
            .dropWhile { it != Screen.Home.routeWithArg }
            .drop(1)
        // Already showing exactly this (e.g. Mi Rutina with nothing on top): leave it alone.
        if (currentAboveHome == above) return
        popBackStack(Screen.Home.routeWithArg, inclusive = false)
    } else {
        navigate(home) { popUpTo(Screen.Home.routeWithArg) { inclusive = true } }
    }
    above.forEach { navigate(it) }
}
