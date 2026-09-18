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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.gondroid.quoteanime.notification.NotificationHelper
import com.gondroid.quoteanime.presentation.navigation.AppNavGraph
import com.gondroid.quoteanime.domain.usecase.RestorePurchasesUseCase
import com.gondroid.quoteanime.presentation.navigation.AppDeepLink
import com.gondroid.quoteanime.presentation.navigation.DeepLinkRouter
import com.gondroid.quoteanime.presentation.navigation.isInMainContent
import com.gondroid.quoteanime.presentation.navigation.showMainBackStack
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var restorePurchases: RestorePurchasesUseCase

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

        // Read on every onCreate, including the re-creation that CLEAR_TOP taps cause.
        val initialDeepLink = intent.toAppDeepLink()

        setContent {
            QuoteAnimeTheme {
                val navController = rememberNavController()
                val deepLinkRouter = rememberSaveable(saver = DeepLinkRouter.Saver) {
                    DeepLinkRouter(initialDeepLink)
                }

                // Widget tap / habit reminder tap while the activity is alive (onNewIntent):
                // applied now if the app is past the splash and the onboarding, parked until
                // they finish otherwise.
                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { newIntent ->
                        val link = newIntent.toAppDeepLink() ?: return@Consumer
                        deepLinkRouter.open(link, isMainReady = navController.isInMainContent())
                            ?.let(navController::showMainBackStack)
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                AppNavGraph(
                    navController = navController,
                    deepLinkRouter = deepLinkRouter
                )

                // Dialog shown when a flexible update has been fully downloaded
                if (showUpdateReadyDialog) {
                    AlertDialog(
                        onDismissRequest = { showUpdateReadyDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                stringResource(R.string.update_ready_title),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                stringResource(R.string.update_ready_body),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showUpdateReadyDialog = false
                                appUpdateManager.completeUpdate()
                            }) {
                                Text(
                                    stringResource(R.string.update_ready_restart),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateReadyDialog = false }) {
                                Text(
                                    stringResource(R.string.update_ready_later),
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

    /**
     * `Application.onCreate` only runs once per process, so a subscription that expires (or is
     * bought elsewhere) while the process stays alive in the background would never be noticed.
     * Re-syncing here catches it; the repository throttles the actual Play query, so alt-tabbing
     * costs nothing.
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { restorePurchases() }
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

private fun Intent.toAppDeepLink(): AppDeepLink? = AppDeepLink.from(
    quoteId = getStringExtra(AppDeepLink.EXTRA_QUOTE_ID),
    openRoutine = getBooleanExtra(NotificationHelper.EXTRA_OPEN_ROUTINE, false)
)
