package com.gondroid.quoteanime.presentation.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.gondroid.quoteanime.BuildConfig
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.presentation.common.AppLinks
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWebView: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // LocalContext can be a ContextWrapper, so casting it to Activity silently fails and the
    // subscribe button does nothing; LocalActivity resolves the host Activity directly.
    val activity = LocalActivity.current
    val context = LocalContext.current
    PaywallContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onOfferSelected = viewModel::onOfferSelected,
        onSubscribe = { activity?.let(viewModel::onSubscribe) },
        onMessageShown = viewModel::onMessageShown,
        onRemovePremiumForTesting = viewModel::onRemovePremiumForTesting,
        onOpenLegalLink = onNavigateToWebView,
        onManageSubscription = {
            // Cancelling is only possible in Play's subscription centre; the Billing Library
            // has no cancel API. Devices without the Play Store can't resolve this intent.
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, state.manageSubscriptionUrl.toUri()))
            } catch (_: ActivityNotFoundException) {
                viewModel.onManageSubscriptionUnavailable()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallContent(
    state: PaywallUiState,
    onNavigateBack: () -> Unit,
    onOfferSelected: (Int) -> Unit,
    onSubscribe: () -> Unit,
    onMessageShown: () -> Unit,
    onRemovePremiumForTesting: () -> Unit,
    onOpenLegalLink: (url: String, title: String) -> Unit,
    onManageSubscription: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelSheet by remember { mutableStateOf(false) }
    val pendingMessage = stringResource(R.string.paywall_purchase_pending)
    val cancelledMessage = stringResource(R.string.paywall_purchase_cancelled)
    val errorMessage = stringResource(R.string.paywall_purchase_error)
    val manageUnavailableMessage = stringResource(R.string.paywall_manage_unavailable)

    LaunchedEffect(state.message) {
        val text = when (state.message) {
            PaywallMessage.PENDING -> pendingMessage
            PaywallMessage.USER_CANCELLED -> cancelledMessage
            PaywallMessage.ERROR -> errorMessage
            PaywallMessage.MANAGE_UNAVAILABLE -> manageUnavailableMessage
            null -> null
        }
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PaywallBenefitRow(
                    icon = Icons.Filled.AllInclusive,
                    title = stringResource(R.string.paywall_benefit_habits_title),
                    body = stringResource(R.string.paywall_benefit_habits_body)
                )
                PaywallBenefitRow(
                    icon = Icons.Filled.Block,
                    title = stringResource(R.string.paywall_benefit_ads_title),
                    body = stringResource(R.string.paywall_benefit_ads_body)
                )
                PaywallBenefitRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = stringResource(R.string.paywall_benefit_themes_title),
                    body = stringResource(R.string.paywall_benefit_themes_body)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            if (state.isPremium) {
                Text(
                    text = stringResource(R.string.paywall_already_premium),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = { showCancelSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.paywall_manage_subscription))
                }
                // QA affordance only: there is no legitimate in-app way to drop a real
                // subscription, so this must never reach a release build.
                if (BuildConfig.DEBUG) {
                    TextButton(
                        onClick = onRemovePremiumForTesting,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.paywall_remove_debug))
                    }
                }

                if (showCancelSheet) {
                    CancelSubscriptionSheet(
                        onDismiss = { showCancelSheet = false },
                        onConfirmCancel = {
                            showCancelSheet = false
                            onManageSubscription()
                        }
                    )
                }
            } else {
                PaywallOfferSection(
                    state = state,
                    onOfferSelected = onOfferSelected,
                    onSubscribe = onSubscribe
                )
            }

            PaywallLegalLinks(onOpenLegalLink = onOpenLegalLink)
        }
    }
}

/** Terms and privacy, reachable from the paywall itself — Play expects both on the screen
 *  where the subscription is sold, not only buried in Settings. */
@Composable
private fun PaywallLegalLinks(onOpenLegalLink: (url: String, title: String) -> Unit) {
    val termsTitle = stringResource(R.string.terms_and_conditions)
    val privacyTitle = stringResource(R.string.politics_privacy)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onOpenLegalLink(AppLinks.TERMS_AND_CONDITIONS_URL, termsTitle) }) {
            Text(
                text = termsTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { onOpenLegalLink(AppLinks.PRIVACY_POLICY_URL, privacyTitle) }) {
            Text(
                text = privacyTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaywallOfferSection(
    state: PaywallUiState,
    onOfferSelected: (Int) -> Unit,
    onSubscribe: () -> Unit
) {
    when {
        state.isLoadingOffers -> {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        }
        state.offers.isEmpty() -> {
            Text(
                text = stringResource(R.string.paywall_offers_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.offers.forEachIndexed { index, offer ->
                    PaywallOfferRow(
                        offer = offer,
                        isSelected = index == state.selectedOfferIndex,
                        onClick = { onOfferSelected(index) }
                    )
                }
            }

            Button(
                onClick = onSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.paywall_subscribe))
            }
            Text(
                text = stringResource(R.string.paywall_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun PaywallOfferRow(offer: SubscriptionOffer, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 4.dp).weight(1f)) {
            Text(text = offer.formattedPrice, style = MaterialTheme.typography.titleSmall)
            if (offer.freeTrialDays != null) {
                Text(
                    text = stringResource(R.string.paywall_free_trial, offer.freeTrialDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaywallBenefitRow(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview(name = "Paywall — free, with offers", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PaywallContentFreePreview() {
    QuoteAnimeTheme {
        PaywallContent(
            state = PaywallUiState(
                isPremium = false,
                isLoadingOffers = false,
                offers = listOf(
                    SubscriptionOffer("t1", "monthly", "$2.99/mes", "P1M", freeTrialDays = 7),
                    SubscriptionOffer("t2", "annual", "$24.99/año", "P1Y", freeTrialDays = null)
                )
            ),
            onNavigateBack = {},
            onOfferSelected = {},
            onSubscribe = {},
            onMessageShown = {},
            onRemovePremiumForTesting = {},
            onOpenLegalLink = { _, _ -> },
            onManageSubscription = {}
        )
    }
}

@Preview(name = "Paywall — already premium", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PaywallContentPremiumPreview() {
    QuoteAnimeTheme {
        PaywallContent(
            state = PaywallUiState(isPremium = true),
            onNavigateBack = {},
            onOfferSelected = {},
            onSubscribe = {},
            onMessageShown = {},
            onRemovePremiumForTesting = {},
            onOpenLegalLink = { _, _ -> },
            onManageSubscription = {}
        )
    }
}
