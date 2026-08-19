package com.gondroid.quoteanime.presentation.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.presentation.common.AppLinks
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real settings sections. The previous version of this file rendered look-alike
 * `ListItem`s declared inside the test, which only ever proved that Material3 renders text —
 * it could not have caught a wrong URL, a renamed string or a section that stopped rendering.
 *
 * Labels are read from resources so the suite doesn't depend on the device locale.
 */
@RunWith(AndroidJUnit4::class)
class SettingsShareAndSocialUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun ratingSection_showsRatingAndShareRows() {
        composeRule.setContent { QuoteAnimeTheme { Column { RatingSection() } } }

        composeRule.onNodeWithText(string(R.string.rating_app)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.share_app)).assertIsDisplayed()
    }

    @Test
    fun socialSection_showsInstagramAndFacebook() {
        composeRule.setContent { QuoteAnimeTheme { Column { SocialSection() } } }

        composeRule.onNodeWithText(string(R.string.social_instagram)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.social_instagram_handle)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.social_facebook)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.social_facebook_handle)).assertIsDisplayed()
    }

    @Test
    fun informationSection_showsLegalRowsAndVersion() {
        composeRule.setContent {
            QuoteAnimeTheme { Column { InformationSection(versionName = "9.9.9") } }
        }

        composeRule.onNodeWithText(string(R.string.politics_privacy)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.terms_and_conditions)).assertIsDisplayed()
        composeRule.onNodeWithText("9.9.9").assertIsDisplayed()
    }

    /** The row must open *this* app's policy, not just any URL — a silent typo here ships. */
    @Test
    fun privacyRow_navigatesToThePrivacyPolicyUrl() {
        var opened: Pair<String, String>? = null
        composeRule.setContent {
            QuoteAnimeTheme {
                Column {
                    InformationSection(
                        versionName = "1.0.0",
                        onNavigateToWebView = { url, title -> opened = url to title }
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.politics_privacy)).performClick()

        assertEquals(AppLinks.PRIVACY_POLICY_URL, opened?.first)
        assertEquals(string(R.string.politics_privacy), opened?.second)
    }

    @Test
    fun termsRow_navigatesToTheTermsUrl() {
        var opened: Pair<String, String>? = null
        composeRule.setContent {
            QuoteAnimeTheme {
                Column {
                    InformationSection(
                        versionName = "1.0.0",
                        onNavigateToWebView = { url, title -> opened = url to title }
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.terms_and_conditions)).performClick()

        assertEquals(AppLinks.TERMS_AND_CONDITIONS_URL, opened?.first)
        assertEquals(string(R.string.terms_and_conditions), opened?.second)
    }
}
