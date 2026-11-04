package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ChronoTaskApp
import com.example.data.DailyProgress
import com.example.data.PriorityLevel
import com.example.data.Subtask
import com.example.data.TaskCategory
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import com.example.data.WeeklyAnalyticsData
import com.example.reminder.TaskAlarmScheduler
import com.example.ui.components.InAppBannerData
import com.example.ui.components.StarterTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed")
}

data class DashboardUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val completedCountToday: Int = 0,
    val totalCountToday: Int = 0,
    val completionRatioToday: Float = 0f,
    val streakDays: Int = 0,
    val activeFilter: TaskFilter = TaskFilter.ALL,
    val activeCategory: String = "ALL",
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class TaskViewModel : ViewModel() {

    private val repository: TaskRepository = ChronoTaskApp.instance.repository
    private val alarmScheduler: TaskAlarmScheduler = ChronoTaskApp.instance.alarmScheduler

    private val _activeFilter = MutableStateFlow(TaskFilter.ALL)
    val activeFilter = _activeFilter.asStateFlow()

    private val _activeCategory = MutableStateFlow("ALL")
    val activeCategory = _activeCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _inAppBanner = MutableStateFlow<InAppBannerData?>(null)
    val inAppBanner: StateFlow<InAppBannerData?> = _inAppBanner.asStateFlow()

    private val _weeklyAnalytics = MutableStateFlow(
        WeeklyAnalyticsData(
            dailyProgressList = emptyList(),
            totalCompletedThisWeek = 0,
            totalScheduledThisWeek = 0,
            overallCompletionPercent = 0,
            currentStreakDays = 0,
            highPriorityCompleted = 0,
            mediumPriorityCompleted = 0,
            lowPriorityCompleted = 0
        )
    )
    val weeklyAnalytics: StateFlow<WeeklyAnalyticsData> = _weeklyAnalytics.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTasks,
        _activeFilter,
        _activeCategory,
        _searchQuery
    ) { allTasks, filter, category, query ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        val endOfToday = startOfToday + (24 * 60 * 60 * 1000L) - 1

        val todayTasks = allTasks.filter {
            (it.reminderTimeMillis in startOfToday..endOfToday) ||
            (it.completedAtMillis != null && it.completedAtMillis in startOfToday..endOfToday)
        }
        val completedToday = todayTasks.count { it.isCompleted }
        val totalToday = todayTasks.size
        val ratio = if (totalToday > 0) completedToday.toFloat() / totalToday.toFloat() else 0f

        val filtered = allTasks.filter { task ->
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.TODAY -> (task.reminderTimeMillis in startOfToday..endOfToday)
                TaskFilter.UPCOMING -> (!task.isCompleted && task.reminderTimeMillis > now)
                TaskFilter.HIGH_PRIORITY -> (task.priority == PriorityLevel.HIGH.name)
                TaskFilter.COMPLETED -> task.isCompleted
            }

            val matchesCategory = if (category == "ALL") true else task.category == category

            val matchesQuery = if (query.isBlank()) true else {
                task.title.contains(query, ignoreCase = true) ||
                        task.details.contains(query, ignoreCase = true)
            }

            matchesFilter && matchesCategory && matchesQuery
        }

        // Compute weekly metrics async
        viewModelScope.launch {
            _weeklyAnalytics.value = repository.computeWeeklyAnalytics(allTasks)
        }

        DashboardUiState(
            tasks = filtered,
            completedCountToday = completedToday,
            totalCountToday = totalToday,
            completionRatioToday = ratio,
            streakDays = _weeklyAnalytics.value.currentStreakDays,
            activeFilter = filter,
            activeCategory = category,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setFilter(filter: TaskFilter) {
        _activeFilter.value = filter
    }

    fun setCategory(category: String) {
        _activeCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNewTask(
        title: String,
        details: String,
        category: TaskCategory,
        priority: PriorityLevel,
        reminderTimeMillis: Long,
        subtasks: List<Subtask>
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                details = details,
                category = category.name,
                priority = priority.name,
                reminderTimeMillis = reminderTimeMillis,
                subtasksJson = TaskEntity.encodeSubtasks(subtasks)
            )
            val id = repository.insertTask(task)
            val savedTask = task.copy(id = id)
            alarmScheduler.scheduleReminder(savedTask)

            _inAppBanner.value = InAppBannerData(
                id = id,
                title = "Reminder Scheduled!",
                message = "Alert set for '${title}'"
            )
        }
    }

    fun addFromTemplate(template: StarterTemplate) {
        val reminderTime = System.currentTimeMillis() + (template.minutesFromNow * 60 * 1000L)
        addNewTask(
            title = template.title,
            details = template.details,
            category = template.category,
            priority = template.priority,
            reminderTimeMillis = reminderTime,
            subtasks = emptyList()
        )
    }

    fun toggleTaskComplete(taskId: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskComplete(taskId, currentlyCompleted)
            if (!currentlyCompleted) {
                alarmScheduler.cancelReminder(taskId)
                _inAppBanner.value = InAppBannerData(
                    id = taskId,
                    title = "Task Completed! ✨",
                    message = "Great work keeping your momentum!"
                )
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            alarmScheduler.cancelReminder(taskId)
            repository.deleteTaskById(taskId)
            _inAppBanner.value = InAppBannerData(
                id = taskId,
                title = "Task Deleted",
                message = "The reminder has been cancelled."
            )
        }
    }

    fun snoozeTask(taskId: Long, minutes: Int = 15) {
        viewModelScope.launch {
            val newTime = repository.snoozeTask(taskId, minutes)
            val task = repository.getTaskById(taskId)
            if (task != null) {
                alarmScheduler.scheduleReminder(task.copy(reminderTimeMillis = newTime))
                _inAppBanner.value = InAppBannerData(
                    id = taskId,
                    title = "Snoozed for $minutes mins",
                    message = "We'll remind you again soon."
                )
            }
        }
    }

    fun toggleSubtask(taskId: Long, subtask: Subtask) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val subtasks = task.getSubtaskList().toMutableList()
            val index = subtasks.indexOfFirst { it.id == subtask.id }
            if (index != -1) {
                subtasks[index] = subtask.copy(isDone = !subtask.isDone)
                val updated = task.copy(subtasksJson = TaskEntity.encodeSubtasks(subtasks))
                repository.updateTask(updated)
            }
        }
    }

    fun triggerTestReminder(seconds: Int = 5) {
        alarmScheduler.scheduleTestAlarm(
            delaySeconds = seconds,
            title = "Test Reminder (Samsung-Style)",
            details = "Full-screen alert waking up your device with Dismiss, Complete, and Snooze controls!"
        )
        _inAppBanner.value = InAppBannerData(
            id = -999L,
            title = "Test Reminder Firing in ${seconds}s!",
            message = "Lock your screen or wait to see the full-screen alert."
        )
    }

    fun dismissInAppBanner() {
        _inAppBanner.value = null
    }
}
