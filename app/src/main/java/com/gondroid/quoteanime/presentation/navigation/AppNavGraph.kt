package com.gondroid.quoteanime.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gondroid.quoteanime.presentation.catalog.CatalogScreen
import com.gondroid.quoteanime.presentation.home.HomeScreen
import com.gondroid.quoteanime.presentation.onboarding.OnboardingScreen
import com.gondroid.quoteanime.presentation.routine.HabitEditorSheet
import com.gondroid.quoteanime.presentation.routine.RoutineScreen
import com.gondroid.quoteanime.presentation.settings.SettingsScreen
import com.gondroid.quoteanime.presentation.settings.WidgetTutorialScreen
import com.gondroid.quoteanime.presentation.splash.SplashScreen

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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Bottom bar only makes sense on the 3 tab destinations, not on splash/onboarding/settings/etc.
    val showBottomBar = BottomTab.entries.any { currentRoute?.startsWith(it.route) == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding)
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
                    }
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
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                    onNavigateToWidgetTutorial = { navController.navigate(Screen.WidgetTutorial.route) }
                )
            }

            composable(Screen.WidgetTutorial.route) {
                WidgetTutorialScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Routine.route) {
                RoutineScreen(
                    onAddHabit = {
                        navController.navigate(Screen.HabitEditor.createRoute(null))
                    },
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
                HabitEditorSheet(onDismiss = { navController.popBackStack() })
            }
        }
    }
}
