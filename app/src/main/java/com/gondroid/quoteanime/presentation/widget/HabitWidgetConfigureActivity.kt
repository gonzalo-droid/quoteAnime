package com.gondroid.quoteanime.presentation.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.presentation.routine.HabitIcons
import com.gondroid.quoteanime.presentation.routine.HabitPalette
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import com.gondroid.quoteanime.widget.HabitWidget
import com.gondroid.quoteanime.widget.HabitWidgetReceiver
import com.gondroid.quoteanime.widget.HabitWidgetState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Standard Android app-widget configuration flow: launched by the system (via
 * `android:configure` in habit_widget_info.xml) right after the user drags a [HabitWidget]
 * onto the home screen, so they can pick which habit THIS instance tracks. The system
 * expects [Activity.RESULT_CANCELED] as the default result — set immediately, before the
 * user picks anything — so a back-press or task-switch away cleanly aborts adding the
 * widget instead of leaving it stuck in a broken, unconfigured state.
 */
@AndroidEntryPoint
class HabitWidgetConfigureActivity : ComponentActivity() {

    private val viewModel: HabitWidgetConfigureViewModel by viewModels()
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            QuoteAnimeTheme {
                HabitWidgetConfigureScreen(
                    habits = viewModel.habits.collectAsState().value,
                    isLoading = viewModel.isLoading.collectAsState().value,
                    onNavigateBack = { finish() },
                    onHabitPicked = ::onHabitPicked
                )
            }
        }
    }

    private fun onHabitPicked(habit: Habit) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@HabitWidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(
                this@HabitWidgetConfigureActivity,
                PreferencesGlanceStateDefinition,
                glanceId
            ) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[HabitWidgetState.HABIT_ID] = habit.id
                    this[HabitWidgetState.TITLE] = habit.title
                    this[HabitWidgetState.COLOR_INDEX] = habit.colorIndex
                    this[HabitWidgetState.IS_LOADING] = true
                    this[HabitWidgetState.HAS_ERROR] = false
                }
            }
            HabitWidget().update(this@HabitWidgetConfigureActivity, glanceId)
            HabitWidgetReceiver.enqueueUpdateAllWork(this@HabitWidgetConfigureActivity)

            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitWidgetConfigureScreen(
    habits: List<Habit>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onHabitPicked: (Habit) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.habit_widget_configure_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

            habits.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.habit_widget_configure_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(items = habits, key = { it.id }) { habit ->
                    ListItem(
                        headlineContent = { Text(habit.title) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(HabitPalette.colorAt(habit.colorIndex).copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HabitIcons.iconFor(habit.iconKey),
                                    contentDescription = null,
                                    tint = HabitPalette.colorAt(habit.colorIndex)
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable { onHabitPicked(habit) }
                            .testTag("habit_widget_configure_item_${habit.id}")
                    )
                }
            }
        }
    }
}
