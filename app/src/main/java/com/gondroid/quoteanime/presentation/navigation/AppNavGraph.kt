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
    data object Home : Screen("home")
    data object Settings : Screen("settings")

    // Soporta navegación opcional con categoryId preseleccionado
    // Uso: Screen.Catalog.createRoute("cat_id") o Screen.Catalog.createRoute(null) → Favoritos
    data object Catalog : Screen("catalog") {
        const val ARG = "categoryId"
        val routeWithArg = "catalog?$ARG={$ARG}"
        fun createRoute(categoryId: String?) =
            if (categoryId != null) "catalog?$ARG=$categoryId" else "catalog"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
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
