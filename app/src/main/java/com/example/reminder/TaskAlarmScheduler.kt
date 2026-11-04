package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.TaskEntity

class TaskAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Checks if the app can schedule exact alarms on Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Creates an Intent to navigate the user to the Exact Alarm Settings page if needed.
     */
    fun getExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }
    }

    fun scheduleReminder(task: TaskEntity) {
        if (task.isCompleted) return
        val reminderTime = task.reminderTimeMillis
        val now = System.currentTimeMillis()
        if (reminderTime <= now) return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_DETAILS, task.details)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_CATEGORY, task.category)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_PRIORITY, task.priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmSafely(reminderTime, pendingIntent, task.title)
    }

    fun scheduleTestAlarm(
        delaySeconds: Int,
        title: String = "Test Reminder",
        details: String = "This is a live test of your full-screen Samsung-style reminder alert!"
    ) {
        val triggerAt = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -999L)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_DETAILS, details)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_CATEGORY, "WORK")
            putExtra(TaskAlarmReceiver.EXTRA_TASK_PRIORITY, "HIGH")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmSafely(triggerAt, pendingIntent, "Test Alarm")
    }

    /**
     * Safely schedules alarms using exact alarms when permitted, falling back
     * to setAndAllowWhileIdle if exact alarm permission is absent on API 31+.
     */
    private fun scheduleAlarmSafely(triggerAtMillis: Long, pendingIntent: PendingIntent, label: String) {
        val am = alarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d("TaskAlarmScheduler", "Scheduled exact alarm for '$label' at $triggerAtMillis")
                } else {
                    am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.w("TaskAlarmScheduler", "Exact alarms not allowed. Scheduled inexact alarm for '$label'")
                }
            } else {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d("TaskAlarmScheduler", "Scheduled pre-S exact alarm for '$label'")
            }
        } catch (e: SecurityException) {
            Log.e("TaskAlarmScheduler", "SecurityException scheduling exact alarm: ${e.message}. Falling back.")
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (fallbackEx: Exception) {
                Log.e("TaskAlarmScheduler", "Fallback failed: ${fallbackEx.message}")
            }
        } catch (e: Exception) {
            Log.e("TaskAlarmScheduler", "Error scheduling alarm: ${e.message}")
        }
    }

    fun cancelReminder(taskId: Long) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
