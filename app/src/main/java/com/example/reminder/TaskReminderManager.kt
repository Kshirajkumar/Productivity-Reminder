package com.example.reminder

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.example.ChronoTaskApp
import com.example.data.TaskEntity
import java.util.Calendar

/**
 * Unified manager for task reminders: snooze, completion, recurring daily tasks,
 * and alarm cancellation/rescheduling. Ensures consistent state across notification actions,
 * full-screen alert activity, and dashboard UI.
 */
object TaskReminderManager {

    private const val TAG = "TaskReminderManager"

    /**
     * Completes a task:
     * 1. Cancels the active notification
     * 2. Cancels any pending AlarmManager wakeups
     * 3. Marks the current task as completed in Room
     * 4. If recurring daily: creates/schedules the next day's reminder at the same scheduled hour/minute!
     */
    suspend fun completeTask(context: Context, taskId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = if (taskId > 0) taskId.toInt() else 9999
        notificationManager?.cancel(notificationId)

        if (taskId <= 0) return

        val app = ChronoTaskApp.instance
        val repository = app.repository
        val alarmScheduler = app.alarmScheduler

        // Cancel pending alarm for this task
        alarmScheduler.cancelReminder(taskId)

        val task = repository.getTaskById(taskId)
        repository.toggleTaskComplete(taskId, currentlyCompleted = false)

        // Handle recurring daily tasks: schedule next day's instance
        if (task != null && task.isRecurringDaily) {
            val nextCal = Calendar.getInstance().apply {
                timeInMillis = task.reminderTimeMillis
                add(Calendar.DAY_OF_YEAR, 1)
                // If it's already in the past compared to now, push to tomorrow same time
                if (timeInMillis <= System.currentTimeMillis()) {
                    val nowCal = Calendar.getInstance()
                    set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
                    set(Calendar.MONTH, nowCal.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_YEAR, nowCal.get(Calendar.DAY_OF_YEAR) + 1)
                }
            }

            val nextTask = task.copy(
                id = 0L,
                reminderTimeMillis = nextCal.timeInMillis,
                isCompleted = false,
                completedAtMillis = null,
                snoozeCount = 0
            )
            val newId = repository.insertTask(nextTask)
            val savedNext = nextTask.copy(id = newId)
            alarmScheduler.scheduleReminder(savedNext)
            Log.d(TAG, "Recurring daily reminder rescheduled for tomorrow at ${nextCal.time}")
        }
    }

    /**
     * Snoozes a task by the specified number of minutes:
     * 1. Cancels active notification
     * 2. Updates reminder time in database and increments snooze count
     * 3. Schedules exact alarm for the new trigger time
     */
    suspend fun snoozeTask(context: Context, taskId: Long, minutes: Int): Long {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = if (taskId > 0) taskId.toInt() else 9999
        notificationManager?.cancel(notificationId)

        val newReminderTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        if (taskId <= 0) return newReminderTime

        val app = ChronoTaskApp.instance
        val repository = app.repository
        val alarmScheduler = app.alarmScheduler

        repository.snoozeTask(taskId, minutes)
        val task = repository.getTaskById(taskId)
        if (task != null) {
            alarmScheduler.scheduleReminder(task.copy(reminderTimeMillis = newReminderTime))
        }
        return newReminderTime
    }
}
