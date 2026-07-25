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
import androidx.compose.ui.graphics.vector.ImageVector

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
