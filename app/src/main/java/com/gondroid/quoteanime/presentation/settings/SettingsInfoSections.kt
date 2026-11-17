package com.gondroid.quoteanime.presentation.settings

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.presentation.ads.AdsEntryPoint
import com.gondroid.quoteanime.presentation.common.AppLinks
import com.gondroid.quoteanime.presentation.web.openExternalLink
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/*
 * Rating, share, social and legal rows of the settings list. Split out of SettingsScreen.kt,
 * which had grown past a thousand lines, and made `internal` so the instrumented tests can
 * drive these composables directly instead of re-creating look-alikes of them.
 */

private fun openPlayStore(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "market://details?id=${context.packageName}".toUri()
            )
        )
    }.onFailure {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
            )
        )
    }
}

@Composable
internal fun RatingSection() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.rating_app),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = {
            Text(
                stringResource(R.string.subtitle_rating_section),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable {
            if (activity != null) {
                scope.launch {
                    runCatching {
                        val manager = ReviewManagerFactory.create(context)
                        val reviewInfo = manager.requestReview()
                        manager.launchReview(activity, reviewInfo)
                    }.onFailure {
                        openPlayStore(context)
                    }
                }
            } else {
                openPlayStore(context)
            }
        },
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.share_app),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = {
            Text(
                stringResource(R.string.share_app_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { shareApp(context) },
        colors = listItemColors
    )
}

internal fun shareApp(context: android.content.Context) {
    val message = context.getString(R.string.share_app_message)
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(intent, null))
}

// ── Social ───────────────────────────────────────────────────────────────────
@Composable
internal fun SocialSection(
    onNavigateToWebView: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val instagramTitle = stringResource(R.string.social_instagram)
    val facebookTitle = stringResource(R.string.social_facebook)

    SocialItem(
        iconRes = R.drawable.ic_instagram,
        name = instagramTitle,
        handle = stringResource(R.string.social_instagram_handle),
        iconTint = Color(0xFFE1306C),
        onClick = {
            // Native app if it's installed, our own WebView otherwise —
            // never an external browser.
            val target = "https://www.instagram.com/animequoteapp/"
            openExternalLink(context, target) {
                onNavigateToWebView(target, instagramTitle)
            }
        }
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    SocialItem(
        iconRes = R.drawable.ic_facebook,
        name = facebookTitle,
        handle = stringResource(R.string.social_facebook_handle),
        iconTint = Color(0xFF1877F2),
        onClick = {
            // Native app if it's installed, our own WebView otherwise —
            // never an external browser.
            val target = "https://www.facebook.com/share/1Ay18mtNZh/?mibextid=wwXIfr"
            openExternalLink(context, target) {
                onNavigateToWebView(target, facebookTitle)
            }
        }
    )

    /*
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    SocialItem(
        iconRes = R.drawable.ic_tiktok,
        name = stringResource(R.string.social_tiktok),
        handle = stringResource(R.string.social_tiktok_handle),
        iconTint = Color.White,
        onClick = {
            // Native app if it's installed, our own WebView otherwise —
            // never an external browser.
            val target = "https://www.tiktok.com/@frasesanime"
            openExternalLink(context, target) {
                onNavigateToWebView(target, context.getString(R.string.social_tiktok))
            }
        }
    )*/
}

@Composable
internal fun SocialItem(
    iconRes: Int,
    name: String,
    handle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = name,
                tint = iconTint
            )
        },
        headlineContent = {
            Text(name, color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                handle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = listItemColors
    )
}

// ── Time picker dialog ────────────────────────────────────────────────────────

@Composable
internal fun InformationSection(
    versionName: String,
    onNavigateToWebView: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val privacyTitle = stringResource(R.string.politics_privacy)
    val termsTitle = stringResource(R.string.terms_and_conditions)
    val inPreview = LocalInspectionMode.current
    val consentManager = remember {
        if (inPreview) null
        else EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java)
            .adsConsentManager()
    }
    val showAdPrivacy = consentManager?.privacyOptionsRequired?.collectAsState()?.value == true

    // Required by Google's consent rules wherever the consent form applies: a way to change
    // the ad choice later. Elsewhere UMP says it is not required and the row stays hidden.
    if (showAdPrivacy) {
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.ad_privacy_options),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clickable {
                context.findActivity()?.let { consentManager?.showPrivacyOptionsForm(it) }
            },
            colors = listItemColors
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.politics_privacy),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable {
            onNavigateToWebView(AppLinks.PRIVACY_POLICY_URL, privacyTitle)
        },
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.terms_and_conditions),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable {
            onNavigateToWebView(AppLinks.TERMS_AND_CONDITIONS_URL, termsTitle)
        },
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text(stringResource(R.string.version), color = MaterialTheme.colorScheme.onBackground)
        },
        trailingContent = {
            Text(
                text = versionName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = listItemColors
    )
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Rating + Share", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewRatingSection() {
    QuoteAnimeTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader(stringResource(R.string.rating))
            RatingSection()
        }
    }
}

@Preview(name = "Síguenos", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewSocialSection() {
    QuoteAnimeTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader(stringResource(R.string.follow_us))
            SocialSection()
        }
    }
}

@Preview(name = "Información", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewInformationSection() {
    QuoteAnimeTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader(stringResource(R.string.version))
            InformationSection(versionName = "1.0.4")
        }
    }
}
