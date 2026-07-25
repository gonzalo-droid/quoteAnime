package com.gondroid.quoteanime.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.gondroid.quoteanime.R

enum class BottomTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    QUOTES(Screen.Home.route, R.string.nav_quotes, Icons.Filled.FormatQuote),
    ROUTINE(Screen.Routine.route, R.string.nav_routine, Icons.Filled.LocalFireDepartment),
    CATALOG(Screen.Catalog.route, R.string.nav_catalog, Icons.AutoMirrored.Filled.MenuBook)
}

@Composable
fun BottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute?.startsWith(tab.route) == true,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
