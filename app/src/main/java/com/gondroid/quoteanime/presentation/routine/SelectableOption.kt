package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * One option of a pick-one grid whose look carries no text (the icon picker's cells, the
 * editor's color swatches): clickable, named, and announced as a radio button with its selected
 * state — TalkBack reads "Selected, Color 3, radio button" instead of a bare "button", so a
 * screen-reader user knows which one is chosen without seeing the border.
 *
 * Replaces `clickable` rather than adding to it, so the option still has a single click action.
 */
fun Modifier.selectableOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
): Modifier = this
    .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    .semantics { contentDescription = label }
