package com.gondroid.quoteanime.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gondroid.quoteanime.MainActivity
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Quote
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "quote_notifications"
        const val NOTIFICATION_ID = 1001

        const val HABIT_CHANNEL_ID = "habit_reminders"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_OPEN_ROUTINE = "open_routine"
    }

    init {
        createNotificationChannel()
        createHabitChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /** Separate channel so users can mute habit reminders without losing quotes. */
    private fun createHabitChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            HABIT_CHANNEL_ID,
            context.getString(R.string.habit_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.habit_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showQuoteNotification(quote: Quote) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(quote.author.orEmpty())
            .setContentText(quote.quote.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote.quote.orEmpty()))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * The "Done" action targets [HabitReminderReceiver] (a BroadcastReceiver), not an
     * activity, so marking the habit complete never opens the app.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showHabitReminder(habitId: String, habitTitle: String, quoteText: String) {
        val notificationId = habitId.hashCode()

        // The "Done" and "content" PendingIntents must never share a request code, even
        // when two different habits' hashCode()s happen to be adjacent integers (the old
        // `notificationId` / `notificationId + 1` scheme could collide across habits in
        // that case). Doubling the hash guarantees every "Done" request code is even and
        // every "content" request code is odd, so the two intent TYPES can never collide
        // with each other by construction, regardless of the raw hashCode values.
        val doneRequestCode = notificationId * 2
        val contentRequestCode = notificationId * 2 + 1

        val doneIntent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            doneRequestCode,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            contentRequestCode,
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_ROUTINE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, HABIT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habitTitle)
            .setContentText(quoteText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quoteText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.habit_notification_action_done),
                donePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
