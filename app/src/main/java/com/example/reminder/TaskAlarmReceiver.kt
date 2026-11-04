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
                // Short partial CPU wake lock to ensure the notification with full-screen intent is posted
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AuraTask:ReminderCpuWakeLock"
                )
                wakeLock?.acquire(3000L)

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

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                // Build High-Priority Heads-Up & Lockscreen Notification with full-screen intent
                val notification = NotificationCompat.Builder(context, ChronoTaskApp.REMINDER_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(if (details.isNotBlank()) details else "Scheduled reminder alert")
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
            }

            ACTION_TASK_COMPLETE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    TaskReminderManager.completeTask(context, taskId)
                }
            }

            ACTION_TASK_SNOOZE -> {
                val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
                CoroutineScope(Dispatchers.IO).launch {
                    TaskReminderManager.snoozeTask(context, taskId, snoozeMins)
                }
            }
        }
    }
}
