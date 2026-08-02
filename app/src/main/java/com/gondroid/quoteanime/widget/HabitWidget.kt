package com.gondroid.quoteanime.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import com.gondroid.quoteanime.presentation.routine.HeatmapGrid
import java.time.LocalDate

@SuppressLint("RestrictedApi")
private val ColorTextPrimary = ColorProvider(Color(0xFFF0EAFF))
@SuppressLint("RestrictedApi")
private val ColorTextSecondary = ColorProvider(Color(0xFF9B8DB3))
@SuppressLint("RestrictedApi")
private val ColorAccent = ColorProvider(Color(0xFFA78BFA))
@SuppressLint("RestrictedApi")
private val ColorEmptyCell = ColorProvider(Color.White.copy(alpha = 0.08f))

private val SIZE_SMALL = DpSize(110.dp, 80.dp)
private val SIZE_LARGE = DpSize(180.dp, 120.dp)

/** Fewer weeks than the in-app heatmap (which shows 17) — a widget has far less room,
 *  and every cell here is a real Glance [Box] (RemoteViews has no Canvas), so the grid
 *  stays small enough to both fit and stay cheap to render. */
private const val WIDGET_HEATMAP_WEEKS = 9
private val CELL_SIZE = 15.dp
private val CELL_GAP = 3.dp

class HabitWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    fun Content() {
        val prefs = currentState<Preferences>()
        val title = prefs[HabitWidgetState.TITLE].orEmpty()
        val colorIndex = prefs[HabitWidgetState.COLOR_INDEX] ?: 0
        val completions = HabitWidgetState.decodeCompletions(prefs[HabitWidgetState.COMPLETIONS_DATA].orEmpty())
        val streakCurrent = prefs[HabitWidgetState.STREAK_CURRENT] ?: 0
        val isLoading = prefs[HabitWidgetState.IS_LOADING] ?: true
        val hasError = prefs[HabitWidgetState.HAS_ERROR] ?: false

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
                .padding(horizontal = if (isSmall) 12.dp else 14.dp, vertical = if (isSmall) 10.dp else 12.dp)
        ) {
            when {
                isLoading -> Text(
                    text = context.getString(R.string.routine_widget_loading),
                    style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp)
                )
                hasError -> Text(
                    text = context.getString(R.string.habit_widget_deleted),
                    style = TextStyle(color = ColorTextSecondary, fontSize = 12.sp)
                )
                else -> Column(modifier = GlanceModifier.fillMaxSize()) {
                    Text(
                        text = title,
                        style = TextStyle(color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Text(
                        text = context.getString(R.string.habit_widget_streak_short, streakCurrent),
                        style = TextStyle(color = ColorAccent, fontSize = 11.sp)
                    )
                    if (!isSmall) {
                        Spacer(GlanceModifier.height(8.dp))
                        HeatmapContent(colorIndex = colorIndex, completions = completions)
                    }
                }
            }
        }
    }

    @Composable
    private fun HeatmapContent(colorIndex: Int, completions: Set<LocalDate>) {
        val today = LocalDate.now()
        val gridStart = HeatmapGrid.gridStart(today, WIDGET_HEATMAP_WEEKS)
        val activeColor = ColorProvider(HabitPalette.colorAt(colorIndex))

        Row(horizontalAlignment = Alignment.Start) {
            for (column in 0 until WIDGET_HEATMAP_WEEKS) {
                Column {
                    for (row in 0 until HeatmapGrid.ROWS) {
                        val date = HeatmapGrid.dateAt(column, row, gridStart)
                        val isCompleted = date in completions && !date.isAfter(today)
                        Box(
                            modifier = GlanceModifier
                                .size(CELL_SIZE)
                                .cornerRadius(3.dp)
                                .background(if (isCompleted) activeColor else ColorEmptyCell)
                        ) {}
                        if (row != HeatmapGrid.ROWS - 1) Spacer(GlanceModifier.height(CELL_GAP))
                    }
                }
                if (column != WIDGET_HEATMAP_WEEKS - 1) Spacer(GlanceModifier.width(CELL_GAP))
            }
        }
    }
}
