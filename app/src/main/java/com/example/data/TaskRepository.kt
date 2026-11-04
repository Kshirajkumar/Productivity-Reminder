package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class DailyProgress(
    val dayLabel: String,     // e.g. "Mon", "Tue"
    val dayTimestamp: Long,
    val completedCount: Int,
    val totalCount: Int
) {
    val completionRatio: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
}

data class WeeklyAnalyticsData(
    val dailyProgressList: List<DailyProgress>,
    val totalCompletedThisWeek: Int,
    val totalScheduledThisWeek: Int,
    val overallCompletionPercent: Int,
    val currentStreakDays: Int,
    val highPriorityCompleted: Int,
    val mediumPriorityCompleted: Int,
    val lowPriorityCompleted: Int
)

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val activeTasks: Flow<List<TaskEntity>> = taskDao.getActiveTasks()

    fun getUpcomingReminders(nowMillis: Long = System.currentTimeMillis()): Flow<List<TaskEntity>> {
        return taskDao.getUpcomingReminders(nowMillis)
    }

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun toggleTaskComplete(id: Long, currentlyCompleted: Boolean) {
        val newCompleted = !currentlyCompleted
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        taskDao.setTaskCompleted(id, newCompleted, completedAt)
    }

    suspend fun snoozeTask(id: Long, minutes: Int): Long {
        val newReminderTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        taskDao.snoozeTask(id, newReminderTime)
        return newReminderTime
    }

    suspend fun computeWeeklyAnalytics(tasks: List<TaskEntity>): WeeklyAnalyticsData {
        val calendar = Calendar.getInstance()
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // Build 7 days back ending today
        val dailyList = mutableListOf<DailyProgress>()
        var totalCompleted = 0
        var totalScheduled = 0
        var highCompleted = 0
        var medCompleted = 0
        var lowCompleted = 0

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1

            val dayLabel = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]

            // Tasks scheduled or completed on this day
            val tasksForDay = tasks.filter {
                (it.reminderTimeMillis in startOfDay..endOfDay) ||
                (it.completedAtMillis != null && it.completedAtMillis in startOfDay..endOfDay)
            }
            val completedForDay = tasksForDay.count { it.isCompleted }
            val countForDay = tasksForDay.size

            dailyList.add(
                DailyProgress(
                    dayLabel = dayLabel,
                    dayTimestamp = startOfDay,
                    completedCount = completedForDay,
                    totalCount = countForDay
                )
            )

            totalCompleted += completedForDay
            totalScheduled += countForDay
        }

        tasks.filter { it.isCompleted }.forEach { task ->
            when (task.priority) {
                PriorityLevel.HIGH.name -> highCompleted++
                PriorityLevel.MEDIUM.name -> medCompleted++
                PriorityLevel.LOW.name -> lowCompleted++
            }
        }

        // Calculate current streak of consecutive days with at least 1 completed task
        var streak = 0
        for (day in dailyList.reversed()) {
            if (day.completedCount > 0) {
                streak++
            } else if (streak > 0) {
                break
            }
        }

        val percent = if (totalScheduled > 0) {
            ((totalCompleted.toFloat() / totalScheduled.toFloat()) * 100).toInt()
        } else {
            0
        }

        return WeeklyAnalyticsData(
            dailyProgressList = dailyList,
            totalCompletedThisWeek = totalCompleted,
            totalScheduledThisWeek = totalScheduled,
            overallCompletionPercent = percent,
            currentStreakDays = streak,
            highPriorityCompleted = highCompleted,
            mediumPriorityCompleted = medCompleted,
            lowPriorityCompleted = lowCompleted
        )
    }

    suspend fun getPendingFutureReminders(now: Long = System.currentTimeMillis()): List<TaskEntity> {
        return taskDao.getPendingFutureReminders(now)
    }
}
