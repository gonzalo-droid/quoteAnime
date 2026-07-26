package com.gondroid.quoteanime.presentation.routine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/** Icons are stored as stable keys so the domain never depends on Compose. */
object HabitIcons {

    private val BY_KEY: Map<String, ImageVector> = mapOf(
        "dumbbell" to Icons.Filled.FitnessCenter,
        "book" to Icons.Filled.MenuBook,
        "self_improvement" to Icons.Filled.SelfImprovement,
        "water_drop" to Icons.Filled.WaterDrop,
        "bedtime" to Icons.Filled.Bedtime,
        "school" to Icons.Filled.School,
        "edit_note" to Icons.Filled.EditNote,
        "directions_walk" to Icons.Filled.DirectionsWalk
    )

    val ALL_KEYS: List<String> = BY_KEY.keys.toList()

    fun iconFor(key: String): ImageVector = BY_KEY[key] ?: Icons.Filled.CheckCircle
}

/**
 * Bundled templates carry a string-resource key (e.g. "template_train") as their title;
 * remote/custom ones carry literal text. Resolve the key to localized display text here,
 * shared by both the onboarding habit picker and the habit editor's template chips.
 */
@Composable
fun resolveTemplateTitle(title: String): String {
    val context = LocalContext.current
    val resId = remember(title) {
        context.resources.getIdentifier(title, "string", context.packageName)
    }
    return if (resId != 0) stringResource(resId) else title
}
