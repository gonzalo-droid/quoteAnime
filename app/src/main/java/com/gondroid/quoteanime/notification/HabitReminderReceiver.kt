package com.gondroid.quoteanime.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the "Done" action so the habit can be marked without opening the app —
 * no activity is launched here, only a background completion write (plus the Mi Rutina
 * widget refresh), delegated to [HabitReminderDoneHandler].
 */
@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var doneHandler: HabitReminderDoneHandler

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(NotificationHelper.EXTRA_HABIT_ID) ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                doneHandler.markDone(habitId)
                context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
