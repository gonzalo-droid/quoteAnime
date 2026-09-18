package com.gondroid.quoteanime.presentation.routine

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TalkBack must say which icon and which color are chosen, not only draw a border.
 *
 * Scenarios covered:
 *  - The chosen icon cell is selected, a radio button, named and clickable; another one isn't selected
 *  - The chosen color swatch is selected, a radio button and named "Color N"; another one isn't selected
 */
@RunWith(AndroidJUnit4::class)
class HabitSelectionSemanticsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val isRadioButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun chosenIconCellIsAnnouncedAsSelected() {
        composeRule.setContent {
            QuoteAnimeTheme {
                HabitIconPickerContent(selectedKey = "dumbbell", onIconSelected = {}, onBack = {})
            }
        }

        composeRule.onNodeWithTag("icon_picker_cell_dumbbell")
            .assertIsSelected()
            .assert(isRadioButton)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        composeRule.onNodeWithTag("icon_picker_cell_running")
            .assertIsNotSelected()
            .assert(isRadioButton)
    }

    @Test
    fun chosenColorSwatchIsAnnouncedAsSelected() {
        composeRule.setContent {
            QuoteAnimeTheme {
                HabitEditorContent(
                    state = HabitEditorUiState(colorIndex = 2),
                    snackbarHostState = SnackbarHostState(),
                    onDismiss = {},
                    onTitleChanged = {},
                    onDescriptionChanged = {},
                    onTemplateSelected = { _, _, _ -> },
                    onPremiumTemplateTapped = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onStartDateChanged = {},
                    onEndDateChanged = {},
                    onReminderToggleRequested = {},
                    onReminderTimeChanged = {},
                    onReminderDayToggled = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithTag("color_2")
            .assertIsSelected()
            .assert(isRadioButton)
            .assert(hasContentDescription("Color 3"))
        composeRule.onNodeWithTag("color_0")
            .assertIsNotSelected()
            .assert(isRadioButton)
    }
}
