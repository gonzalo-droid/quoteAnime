package com.gondroid.quoteanime.presentation.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

/**
 * Shown before deep-linking to Play's subscription centre, which is the only place a
 * subscription can actually be cancelled.
 *
 * The benefits are the paywall's, struck through, so the user *sees* what they're giving up
 * instead of reading about it. Staying is the primary action, but [onConfirmCancel] stays a
 * plainly legible button on purpose — burying the exit is a dark pattern, and both Play policy
 * and consumer law in several markets treat it as one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSubscriptionSheet(
    onDismiss: () -> Unit,
    onConfirmCancel: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        CancelSubscriptionSheetContent(onKeep = onDismiss, onConfirmCancel = onConfirmCancel)
    }
}

@Composable
private fun CancelSubscriptionSheetContent(
    onKeep: () -> Unit,
    onConfirmCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.cancel_sheet_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.cancel_sheet_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LostBenefitRow(
                icon = Icons.Filled.AllInclusive,
                title = stringResource(R.string.paywall_benefit_habits_title),
                body = stringResource(R.string.cancel_sheet_loss_habits)
            )
            LostBenefitRow(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.paywall_benefit_ads_title),
                body = stringResource(R.string.cancel_sheet_loss_ads)
            )
            LostBenefitRow(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.paywall_benefit_themes_title),
                body = stringResource(R.string.cancel_sheet_loss_themes)
            )
        }

        // Play gives the client no expiry date (that lives in the server-side Developer API),
        // so this reassurance stays deliberately date-free.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.cancel_sheet_reassurance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Button(
            onClick = onKeep,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.cancel_sheet_keep))
        }

        TextButton(
            onClick = onConfirmCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.cancel_sheet_confirm),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LostBenefitRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.alpha(0.6f),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = TextDecoration.LineThrough
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview(name = "Cancel subscription sheet", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun CancelSubscriptionSheetPreview() {
    QuoteAnimeTheme {
        CancelSubscriptionSheetContent(onKeep = {}, onConfirmCancel = {})
    }
}
