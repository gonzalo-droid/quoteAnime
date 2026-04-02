package com.gondroid.quoteanime.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gondroid.quoteanime.presentation.catalog.CatalogScreen
import com.gondroid.quoteanime.presentation.home.HomeScreen
import com.gondroid.quoteanime.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home") {
        const val ARG_QUOTE_ID = "quoteId"
        val routeWithArg = "home?$ARG_QUOTE_ID={$ARG_QUOTE_ID}"
        fun createRoute(quoteId: String?) =
            if (quoteId != null) "home?$ARG_QUOTE_ID=$quoteId" else "home"
    }
    data object Settings : Screen("settings")
    data object Catalog : Screen("catalog") {
        const val ARG = "categoryId"
        val routeWithArg = "catalog?$ARG={$ARG}"
        fun createRoute(categoryId: String?) =
            if (categoryId != null) "catalog?$ARG=$categoryId" else "catalog"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startQuoteId: String? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.createRoute(startQuoteId)
    ) {
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
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
