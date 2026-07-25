package com.gondroid.quoteanime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import com.gondroid.quoteanime.presentation.navigation.AppNavGraph
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    // Compose-observable state: true when a flexible update has finished downloading
    private var showUpdateReadyDialog by mutableStateOf(false)

    // Launcher for the flexible update flow (user sees download progress in Play overlay)
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* no-op: download result handled by installStateListener */ }

    // Notified when the download finishes
    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateReadyDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateListener)

        val initialQuoteId = intent.getStringExtra("widget_quote_id")

        setContent {
            QuoteAnimeTheme {
                val navController = rememberNavController()
                var pendingQuoteId by remember { mutableStateOf(initialQuoteId) }

                // Handle widget tap when app is already in foreground (onNewIntent)
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

                // Dialog shown when a flexible update has been fully downloaded
                if (showUpdateReadyDialog) {
                    AlertDialog(
                        onDismissRequest = { showUpdateReadyDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                "Actualización lista",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                "Una nueva versión de Quote Anime está lista. " +
                                        "Reinicia la app para aplicarla.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showUpdateReadyDialog = false
                                appUpdateManager.completeUpdate()
                            }) {
                                Text(
                                    "Reiniciar",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateReadyDialog = false }) {
                                Text(
                                    "Después",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkForUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                // A flexible update was already fully downloaded in a previous session
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    showUpdateReadyDialog = true
                }
                // A new update is available — start the flexible download
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }
            }
        }
    }
}
