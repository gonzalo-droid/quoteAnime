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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.gondroid.quoteanime.R

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

    /** Human-readable description for each icon key, used as a screen-reader label. */
    private val DESCRIPTION_RES_BY_KEY: Map<String, Int> = mapOf(
        "dumbbell" to R.string.icon_dumbbell,
        "book" to R.string.icon_book,
        "self_improvement" to R.string.icon_self_improvement,
        "water_drop" to R.string.icon_water_drop,
        "bedtime" to R.string.icon_bedtime,
        "school" to R.string.icon_school,
        "edit_note" to R.string.icon_edit_note,
        "directions_walk" to R.string.icon_directions_walk
    )

    val ALL_KEYS: List<String> = BY_KEY.keys.toList()

    fun iconFor(key: String): ImageVector = BY_KEY[key] ?: Icons.Filled.CheckCircle

    /** Resolves the string-resource id for an icon key's accessibility description, if mapped. */
    fun descriptionResFor(key: String): Int? = DESCRIPTION_RES_BY_KEY[key]
}

/**
 * Bundled templates' title keys (see [com.gondroid.quoteanime.domain.model.DefaultHabitTemplates])
 * mapped at compile time so [resolveTemplateTitle] never needs reflection — R8/resource
 * shrinking can't see a `getIdentifier()` lookup and may strip the referenced strings.
 */
private val TEMPLATE_TITLE_RES_BY_KEY: Map<String, Int> = mapOf(
    "template_train" to R.string.template_train,
    "template_read" to R.string.template_read,
    "template_meditate" to R.string.template_meditate,
    "template_water" to R.string.template_water,
    "template_sleep_early" to R.string.template_sleep_early,
    "template_study" to R.string.template_study,
    "template_write" to R.string.template_write,
    "template_walk" to R.string.template_walk
)

/**
 * Resolves a human-readable accessibility description for an icon key. Falls back to the
 * raw key for any future icon that doesn't yet have a mapped string resource.
 */
@Composable
fun describeIcon(key: String): String {
    val resId = HabitIcons.descriptionResFor(key)
    return if (resId != null) stringResource(resId) else key
}

/**
 * Bundled templates carry a string-resource key (e.g. "template_train") as their title;
 * remote/custom ones carry literal text. Resolve the key to localized display text here,
 * shared by both the onboarding habit picker and the habit editor's template chips.
 */
@Composable
fun resolveTemplateTitle(title: String): String {
    val resId = TEMPLATE_TITLE_RES_BY_KEY[title]
    return if (resId != null) stringResource(resId) else title
}
