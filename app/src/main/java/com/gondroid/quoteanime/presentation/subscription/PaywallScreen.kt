package com.gondroid.quoteanime.presentation.subscription

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaywallContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onSubscribe = viewModel::onSubscribe,
        onRemovePremiumForTesting = viewModel::onRemovePremiumForTesting
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallContent(
    state: PaywallUiState,
    onNavigateBack: () -> Unit,
    onSubscribe: () -> Unit,
    onRemovePremiumForTesting: () -> Unit
) {
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
                TextButton(
                    onClick = onRemovePremiumForTesting,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.paywall_remove_debug))
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
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

@Preview(name = "Paywall — free", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PaywallContentFreePreview() {
    QuoteAnimeTheme {
        PaywallContent(
            state = PaywallUiState(isPremium = false),
            onNavigateBack = {},
            onSubscribe = {},
            onRemovePremiumForTesting = {}
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
            onSubscribe = {},
            onRemovePremiumForTesting = {}
        )
    }
}
