package com.gondroid.quoteanime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import com.gondroid.quoteanime.presentation.navigation.AppNavGraph
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialQuoteId = intent.getStringExtra("widget_quote_id")

        setContent {
            QuoteAnimeTheme {
                val navController = rememberNavController()
                var pendingQuoteId by remember { mutableStateOf(initialQuoteId) }

                // Handle widget tap when app is already in the foreground (onNewIntent)
                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { newIntent ->
                        newIntent.getStringExtra("widget_quote_id")?.let { quoteId ->
                            navController.navigate(
                                com.gondroid.quoteanime.presentation.navigation.Screen
                                    .Home.createRoute(quoteId)
                            ) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                AppNavGraph(
                    navController = navController,
                    startQuoteId = pendingQuoteId
                )
            }
        }
    }
}
