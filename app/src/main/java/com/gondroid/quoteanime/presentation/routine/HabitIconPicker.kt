package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.R

private const val ICON_GRID_COLUMNS = 6

/**
 * Full-screen icon picker (not a dialog): close (X) up top, search, and icons grouped into the
 * categories defined in [HabitIcons.CATEGORIES]. Hosted inline by [HabitEditorSheet] — the
 * surrounding ModalBottomSheet already opens fully expanded, so swapping its content in place
 * reads as a real screen without adding a new navigation destination.
 */
@Composable
fun HabitIconPickerContent(
    selectedKey: String,
    onIconSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    val visibleCategories = HabitIcons.CATEGORIES
        .map { category -> category to category.keys.filter { matchesQuery(describeIcon(it), query) } }
        .filter { (_, keys) -> keys.isNotEmpty() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close)
                )
            }
            Text(
                text = stringResource(R.string.habit_icon_picker_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.habit_icon_picker_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("icon_picker_search")
        )

        if (visibleCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.habit_icon_picker_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = visibleCategories, key = { it.first.titleRes }) { (category, keys) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    keys.chunked(ICON_GRID_COLUMNS).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                IconCell(
                                    iconKey = key,
                                    selected = key == selectedKey,
                                    onClick = { onIconSelected(key) }
                                )
                            }
                            repeat(ICON_GRID_COLUMNS - rowKeys.size) {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun matchesQuery(description: String, query: String): Boolean =
    query.isBlank() || description.contains(query, ignoreCase = true)

@Composable
private fun IconCell(
    iconKey: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = describeIcon(iconKey)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .semantics { contentDescription = description }
            .testTag("icon_picker_cell_$iconKey"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = HabitIcons.iconFor(iconKey),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
