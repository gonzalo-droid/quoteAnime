package com.gondroid.quoteanime.presentation.routine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.gondroid.quoteanime.R

/** One group of related icons shown together in the full-screen picker. */
data class HabitIconCategory(val titleRes: Int, val keys: List<String>)

/** Icons are stored as stable keys so the domain never depends on Compose. */
object HabitIcons {

    /** Grouping shown by the picker (see [com.gondroid.quoteanime.presentation.routine.HabitIconPickerScreen]). */
    val CATEGORIES: List<HabitIconCategory> = listOf(
        HabitIconCategory(
            R.string.icon_category_physical,
            listOf("dumbbell", "running", "directions_walk", "cycling", "swimming", "self_improvement")
        ),
        HabitIconCategory(
            R.string.icon_category_mind,
            listOf("book", "headphones", "bedtime", "wb_sunny", "nights_stay", "water_drop")
        ),
        HabitIconCategory(
            R.string.icon_category_study,
            listOf("school", "edit_note", "laptop", "notebook", "folder", "alarm")
        ),
        HabitIconCategory(
            R.string.icon_category_routine,
            listOf("restaurant", "clean_hands", "cleaning", "bed", "spa", "pets")
        )
    )

    private val BY_KEY: Map<String, ImageVector> = mapOf(
        "dumbbell" to Icons.Filled.FitnessCenter,
        "book" to Icons.Filled.MenuBook,
        "self_improvement" to Icons.Filled.SelfImprovement,
        "water_drop" to Icons.Filled.WaterDrop,
        "bedtime" to Icons.Filled.Bedtime,
        "school" to Icons.Filled.School,
        "edit_note" to Icons.Filled.EditNote,
        "directions_walk" to Icons.Filled.DirectionsWalk,
        "running" to Icons.AutoMirrored.Filled.DirectionsRun,
        "cycling" to Icons.AutoMirrored.Filled.DirectionsBike,
        "swimming" to Icons.Filled.Pool,
        "headphones" to Icons.Filled.Headphones,
        "wb_sunny" to Icons.Filled.WbSunny,
        "nights_stay" to Icons.Filled.NightsStay,
        "laptop" to Icons.Filled.Laptop,
        "notebook" to Icons.Filled.Description,
        "folder" to Icons.Filled.Folder,
        "alarm" to Icons.Filled.Alarm,
        "restaurant" to Icons.Filled.Restaurant,
        "clean_hands" to Icons.Filled.CleanHands,
        "cleaning" to Icons.Filled.CleaningServices,
        "bed" to Icons.Filled.Bed,
        "spa" to Icons.Filled.Spa,
        "pets" to Icons.Filled.Pets
    )

    /** Human-readable description for each icon key, used as a screen-reader label and as the
     *  searchable text in the icon picker. */
    private val DESCRIPTION_RES_BY_KEY: Map<String, Int> = mapOf(
        "dumbbell" to R.string.icon_dumbbell,
        "book" to R.string.icon_book,
        "self_improvement" to R.string.icon_self_improvement,
        "water_drop" to R.string.icon_water_drop,
        "bedtime" to R.string.icon_bedtime,
        "school" to R.string.icon_school,
        "edit_note" to R.string.icon_edit_note,
        "directions_walk" to R.string.icon_directions_walk,
        "running" to R.string.icon_running,
        "cycling" to R.string.icon_cycling,
        "swimming" to R.string.icon_swimming,
        "headphones" to R.string.icon_headphones,
        "wb_sunny" to R.string.icon_wb_sunny,
        "nights_stay" to R.string.icon_nights_stay,
        "laptop" to R.string.icon_laptop,
        "notebook" to R.string.icon_notebook,
        "folder" to R.string.icon_folder,
        "alarm" to R.string.icon_alarm,
        "restaurant" to R.string.icon_restaurant,
        "clean_hands" to R.string.icon_clean_hands,
        "cleaning" to R.string.icon_cleaning,
        "bed" to R.string.icon_bed,
        "spa" to R.string.icon_spa,
        "pets" to R.string.icon_pets
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
    "template_walk" to R.string.template_walk,
    "template_theme_ninja" to R.string.template_theme_ninja,
    "template_theme_one_piece" to R.string.template_theme_one_piece,
    "template_theme_saiyan" to R.string.template_theme_saiyan
)

/** Thematic filler text shown/prefilled when a themed template ([HabitTemplate.themeKey]) is selected. */
private val THEME_DESCRIPTION_RES_BY_KEY: Map<String, Int> = mapOf(
    "ninja" to R.string.habit_theme_description_ninja,
    "one_piece" to R.string.habit_theme_description_one_piece,
    "saiyan" to R.string.habit_theme_description_saiyan
)

@Composable
fun resolveThemeDescription(themeKey: String?): String? {
    val resId = themeKey?.let { THEME_DESCRIPTION_RES_BY_KEY[it] } ?: return null
    return stringResource(resId)
}

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
