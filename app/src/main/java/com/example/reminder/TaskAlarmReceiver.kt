package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.ChronoTaskApp
import com.example.R
import com.example.ui.reminder.ReminderAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.example.ACTION_TASK_REMINDER"
        const val ACTION_TASK_COMPLETE = "com.example.ACTION_TASK_COMPLETE"
        const val ACTION_TASK_SNOOZE = "com.example.ACTION_TASK_SNOOZE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DETAILS = "extra_task_details"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
        const val EXTRA_TASK_PRIORITY = "extra_task_priority"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Scheduled Task Reminder"
        val details = intent.getStringExtra(EXTRA_TASK_DETAILS) ?: ""
        val category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "PERSONAL"
        val priority = intent.getStringExtra(EXTRA_TASK_PRIORITY) ?: "MEDIUM"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (action) {
            ACTION_TASK_REMINDER -> {
                // Acquire temporary wake lock to ensure the device turns on and displays the alert
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "ChronoTask:ReminderWakeLock"
                )
                wakeLock?.acquire(5000L)

                // 1. Full-Screen Intent for Samsung-style Reminder Activity
                val fullScreenIntent = Intent(context, ReminderAlertActivity::class.java).apply {
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, title)
                    putExtra(EXTRA_TASK_DETAILS, details)
                    putExtra(EXTRA_TASK_CATEGORY, category)
                    putExtra(EXTRA_TASK_PRIORITY, priority)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    taskId.toInt().coerceAtLeast(100),
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. Complete Action Intent
                val completeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                    this.action = ACTION_TASK_COMPLETE
                    putExtra(EXTRA_TASK_ID, taskId)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
                    context,
                    (taskId.toInt() + 1000).coerceAtLeast(200),
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 3. Snooze Action Intent (15 mins)
                val snoozeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                    this.action = ACTION_TASK_SNOOZE
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, title)
                    putExtra(EXTRA_TASK_DETAILS, details)
                    putExtra(EXTRA_SNOOZE_MINUTES, 15)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    (taskId.toInt() + 2000).coerceAtLeast(300),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                // Build High-Priority Heads-Up & Lockscreen Notification
                val notification = NotificationCompat.Builder(context, ChronoTaskApp.REMINDER_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(if (details.isNotBlank()) details else "Scheduled task reminder")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
                    .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 15m", snoozePendingIntent)
                    .build()

                val notificationId = if (taskId > 0) taskId.toInt() else 9999
                notificationManager?.notify(notificationId, notification)

                // Try to launch the full-screen activity directly as well
                try {
                    context.startActivity(fullScreenIntent)
                } catch (e: Exception) {
                    // Full-screen intent in notification handles this on locked/restricted states
                }
            }

            ACTION_TASK_COMPLETE -> {
                val notificationId = if (taskId > 0) taskId.toInt() else 9999
                notificationManager?.cancel(notificationId)

                if (taskId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val repository = ChronoTaskApp.instance.repository
                        repository.toggleTaskComplete(taskId, currentlyCompleted = false)
                    }
                }
            }

            ACTION_TASK_SNOOZE -> {
                val notificationId = if (taskId > 0) taskId.toInt() else 9999
                notificationManager?.cancel(notificationId)

                val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
                if (taskId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val repository = ChronoTaskApp.instance.repository
                        val newTime = repository.snoozeTask(taskId, snoozeMins)
                        val task = repository.getTaskById(taskId)
                        if (task != null) {
                            ChronoTaskApp.instance.alarmScheduler.scheduleReminder(
                                task.copy(reminderTimeMillis = newTime)
                            )
                        }
                    }
                }
            }
        }
    }
}
