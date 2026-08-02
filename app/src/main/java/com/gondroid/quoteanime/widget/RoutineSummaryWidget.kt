package com.gondroid.quoteanime.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gondroid.quoteanime.MainActivity
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.notification.NotificationHelper
import com.gondroid.quoteanime.presentation.routine.HabitPalette

@SuppressLint("RestrictedApi")
private val ColorTextPrimary = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF0EAFF))
@SuppressLint("RestrictedApi")
private val ColorTextSecondary = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9B8DB3))
@SuppressLint("RestrictedApi")
private val ColorAccent = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA78BFA))

private val SIZE_SMALL = DpSize(110.dp, 80.dp)
private val SIZE_LARGE = DpSize(180.dp, 120.dp)

/** Maximum rows drawn — a widget has no scrolling once it goes past the free-tier habit
 *  count, so this only ever truncates for very large premium habit lists. */
private const val MAX_VISIBLE_HABITS = 6

class RoutineSummaryWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    fun Content() {
        val prefs = currentState<Preferences>()
        val habits = RoutineSummaryWidgetState.decode(prefs[RoutineSummaryWidgetState.HABITS_DATA].orEmpty())
        val isLoading = prefs[RoutineSummaryWidgetState.IS_LOADING] ?: true
        val hasError = prefs[RoutineSummaryWidgetState.HAS_ERROR] ?: false

        val size = LocalSize.current
        val isSmall = size.width < SIZE_LARGE.width

        val context = LocalContext.current
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(NotificationHelper.EXTRA_OPEN_ROUTINE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg))
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(openIntent))
                .padding(horizontal = if (isSmall) 12.dp else 16.dp, vertical = if (isSmall) 10.dp else 12.dp)
        ) {
            when {
                isLoading -> Text(
                    text = context.getString(R.string.routine_widget_loading),
                    style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp)
                )
                hasError -> Text(
                    text = context.getString(R.string.routine_widget_error),
                    style = TextStyle(color = ColorTextSecondary, fontSize = 12.sp)
                )
                habits.isEmpty() -> Text(
                    text = context.getString(R.string.routine_widget_empty),
                    style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp)
                )
                else -> HabitsContent(habits = habits, compact = isSmall)
            }
        }
    }

    @Composable
    private fun HabitsContent(habits: List<RoutineWidgetHabitSummary>, compact: Boolean) {
        val context = LocalContext.current
        val completedToday = habits.count { it.completedToday }

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = context.getString(R.string.routine_widget_title),
                style = TextStyle(color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                text = context.getString(R.string.routine_widget_progress, completedToday, habits.size),
                style = TextStyle(color = ColorAccent, fontSize = 11.sp)
            )
            if (!compact) {
                Spacer(GlanceModifier.height(8.dp))
                habits.take(MAX_VISIBLE_HABITS).forEach { habit ->
                    HabitRow(habit)
                }
            }
        }
    }

    @Composable
    private fun HabitRow(habit: RoutineWidgetHabitSummary) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(ColorProvider(HabitPalette.colorAt(habit.colorIndex)))
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = habit.title,
                style = TextStyle(color = ColorTextPrimary, fontSize = 12.sp),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = if (habit.completedToday) "✓" else "○",
                style = TextStyle(
                    color = if (habit.completedToday) ColorAccent else ColorTextSecondary,
                    fontSize = 13.sp
                )
            )
        }
    }
}
