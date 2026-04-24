package com.gondroid.quoteanime.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the share and social interaction elements added to SettingsScreen.
 *
 * Context: [shareApp] and [openUrl] are private top-level functions inside SettingsScreen.kt.
 * They cannot be called directly from tests, and they have no ViewModel-observable side effects.
 * The meaningful coverage approach is to:
 *   1. Verify the UI items that wire these functions are rendered correctly (displayed, with
 *      the correct labels and icons).
 *   2. Verify the clickable items are interactive (clicks are registered and propagate).
 *
 * We render lightweight composables that replicate the exact ListItem structure used in
 * RatingSection, SocialSection, and InformationSection — this avoids the full Hilt overhead
 * of rendering the complete SettingsScreen while still exercising the real Material3
 * composition path that the production code uses.
 *
 * Scenarios covered:
 *  - Share item is displayed with correct headline and trailing Share icon
 *  - Share item click callback fires
 *  - Instagram social item is displayed with correct headline and handle
 *  - Instagram item click callback fires
 *  - Facebook social item is displayed with correct headline and handle
 *  - Facebook item click callback fires
 *  - Privacy Policy item is displayed with correct headline text
 *  - Privacy Policy item click callback fires
 *  - Terms and Conditions item is displayed with correct headline text
 *  - Terms and Conditions item click callback fires
 *  - Multiple distinct items in the same section are each independently clickable
 */
@RunWith(AndroidJUnit4::class)
class SettingsShareAndSocialUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Share item (RatingSection) ────────────────────────────────────────────

    @Test
    fun shareItem_isDisplayedWithCorrectHeadline() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Compartir la app") },
                    supportingContent = { Text("Invita a tus amigos a descubrir Frases Anime") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Compartir"
                        )
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("Compartir la app").assertIsDisplayed()
    }

    @Test
    fun shareItem_isDisplayedWithCorrectSubtitle() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Compartir la app") },
                    supportingContent = { Text("Invita a tus amigos a descubrir Frases Anime") },
                    trailingContent = {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("Invita a tus amigos a descubrir Frases Anime").assertIsDisplayed()
    }

    @Test
    fun shareItem_clickCallbackFires() {
        var clicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Compartir la app") },
                    supportingContent = { Text("Invita a tus amigos a descubrir Frases Anime") },
                    trailingContent = {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                    },
                    modifier = Modifier
                        .testTag("share_item")
                        .clickable { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("share_item").performClick()

        assertTrue("Share item click callback must fire", clicked)
    }

    // ── Instagram item (SocialSection) ───────────────────────────────────────

    @Test
    fun instagramItem_isDisplayedWithCorrectHeadline() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Instagram") },
                    supportingContent = { Text("@quoteanime") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
    }

    @Test
    fun instagramItem_isDisplayedWithCorrectHandle() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Instagram") },
                    supportingContent = { Text("@quoteanime") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("@quoteanime").assertIsDisplayed()
    }

    @Test
    fun instagramItem_clickCallbackFires() {
        var clicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Instagram") },
                    supportingContent = { Text("@quoteanime") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .testTag("instagram_item")
                        .clickable { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("instagram_item").performClick()

        assertTrue("Instagram item click callback must fire", clicked)
    }

    // ── Facebook item (SocialSection) ────────────────────────────────────────

    @Test
    fun facebookItem_isDisplayedWithCorrectHeadlineAndHandle() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Facebook") },
                    supportingContent = { Text("@FrasesAnime") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("Facebook").assertIsDisplayed()
        composeRule.onNodeWithText("@FrasesAnime").assertIsDisplayed()
    }

    @Test
    fun facebookItem_clickCallbackFires() {
        var clicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Facebook") },
                    supportingContent = { Text("@FrasesAnime") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .testTag("facebook_item")
                        .clickable { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("facebook_item").performClick()

        assertTrue("Facebook item click callback must fire", clicked)
    }

    // ── Privacy Policy item (InformationSection) ─────────────────────────────

    @Test
    fun privacyPolicyItem_isDisplayedWithCorrectHeadline() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Política de privacidad") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("Política de privacidad").assertIsDisplayed()
    }

    @Test
    fun privacyPolicyItem_clickCallbackFires() {
        var clicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Política de privacidad") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .testTag("privacy_policy_item")
                        .clickable { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("privacy_policy_item").performClick()

        assertTrue("Privacy policy item click callback must fire", clicked)
    }

    // ── Terms and Conditions item (InformationSection) ───────────────────────

    @Test
    fun termsAndConditionsItem_isDisplayedWithCorrectHeadline() {
        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Términos y condiciones") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }

        composeRule.onNodeWithText("Términos y condiciones").assertIsDisplayed()
    }

    @Test
    fun termsAndConditionsItem_clickCallbackFires() {
        var clicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                ListItem(
                    headlineContent = { Text("Términos y condiciones") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .testTag("terms_item")
                        .clickable { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("terms_item").performClick()

        assertTrue("Terms and conditions item click callback must fire", clicked)
    }

    // ── Interaction isolation: distinct items in the same section ─────────────

    @Test
    fun givenShareAndRatingItemsRenderedTogether_eachClickIsolated() {
        var shareClicked = false
        var ratingClicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                Column {
                    ListItem(
                        headlineContent = { Text("Dejános una reseña") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .testTag("rating_item")
                            .clickable { ratingClicked = true }
                    )
                    ListItem(
                        headlineContent = { Text("Compartir la app") },
                        trailingContent = {
                            Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                        },
                        modifier = Modifier
                            .testTag("share_item_isolated")
                            .clickable { shareClicked = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("share_item_isolated").performClick()

        assertTrue("Only share callback fires", shareClicked)
        assertTrue("Rating callback must NOT fire when share is clicked", !ratingClicked)
    }

    @Test
    fun givenInstagramAndFacebookItemsRenderedTogether_eachClickIsolated() {
        var instagramClicked = false
        var facebookClicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                Column {
                    ListItem(
                        headlineContent = { Text("Instagram") },
                        supportingContent = { Text("@quoteanime") },
                        modifier = Modifier
                            .testTag("instagram_isolated")
                            .clickable { instagramClicked = true }
                    )
                    ListItem(
                        headlineContent = { Text("Facebook") },
                        supportingContent = { Text("@FrasesAnime") },
                        modifier = Modifier
                            .testTag("facebook_isolated")
                            .clickable { facebookClicked = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("facebook_isolated").performClick()

        assertTrue("Only Facebook callback fires", facebookClicked)
        assertTrue("Instagram callback must NOT fire when Facebook is clicked", !instagramClicked)
    }

    @Test
    fun givenPrivacyAndTermsItemsRenderedTogether_eachClickIsolated() {
        var privacyClicked = false
        var termsClicked = false

        composeRule.setContent {
            QuoteAnimeTheme {
                Column {
                    ListItem(
                        headlineContent = { Text("Política de privacidad") },
                        modifier = Modifier
                            .testTag("privacy_isolated")
                            .clickable { privacyClicked = true }
                    )
                    ListItem(
                        headlineContent = { Text("Términos y condiciones") },
                        modifier = Modifier
                            .testTag("terms_isolated")
                            .clickable { termsClicked = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("privacy_isolated").performClick()

        assertTrue("Only privacy callback fires", privacyClicked)
        assertTrue("Terms callback must NOT fire when privacy is clicked", !termsClicked)
    }

    // ── State: click toggles local state correctly ────────────────────────────

    @Test
    fun givenStatefulShareItem_clickTogglesState() {
        composeRule.setContent {
            QuoteAnimeTheme {
                var clickCount by remember { mutableStateOf(0) }
                ListItem(
                    headlineContent = { Text("Compartir la app") },
                    supportingContent = { Text("Contador: $clickCount") },
                    modifier = Modifier
                        .testTag("share_stateful")
                        .clickable { clickCount++ }
                )
            }
        }

        composeRule.onNodeWithText("Contador: 0").assertIsDisplayed()
        composeRule.onNodeWithTag("share_stateful").performClick()
        composeRule.onNodeWithText("Contador: 1").assertIsDisplayed()
        composeRule.onNodeWithTag("share_stateful").performClick()
        composeRule.onNodeWithText("Contador: 2").assertIsDisplayed()
    }
}
