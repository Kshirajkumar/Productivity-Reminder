package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriorityLevel
import com.example.data.TaskEntity
import com.example.ui.components.InteractiveEmptyState
import com.example.ui.components.TaskItemCard
import com.example.ui.components.bounceClick
import com.example.ui.dashboard.TaskEditorBottomSheet
import com.example.ui.theme.AuraAmberImportant
import com.example.ui.theme.AuraCyanAccent
import com.example.ui.theme.AuraEmeraldDone
import com.example.ui.theme.AuraIrisPrimary
import com.example.ui.theme.AuraRoseUrgent
import com.example.ui.viewmodel.TaskViewModel
import java.util.Calendar

@Composable
fun RemindersTimelineScreen(
    viewModel: TaskViewModel,
    tasks: List<TaskEntity>,
    onOpenLivePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val activeTasks = remember(tasks) {
        tasks.filter { !it.isCompleted }.sortedBy { it.reminderTimeMillis }
    }

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis
    val endOfToday = Calendar.getInstance().apply {
        timeInMillis = startOfToday
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    val endOfTomorrow = Calendar.getInstance().apply {
        timeInMillis = startOfToday
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    val todayReminders = activeTasks.filter { it.reminderTimeMillis in startOfToday..endOfToday }
    val tomorrowReminders = activeTasks.filter { it.reminderTimeMillis in (endOfToday + 1)..endOfTomorrow }
    val laterReminders = activeTasks.filter { it.reminderTimeMillis > endOfTomorrow }
    val overdueReminders = activeTasks.filter { it.reminderTimeMillis < startOfToday }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Prominent Page Heading
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "TIMELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = AuraIrisPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Scheduled",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraIrisPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${activeTasks.size} active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraIrisPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upcoming alarm-grade reminders and scheduled works",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Alarm Test Banner
            item {
                RemindersHeroBanner(
                    activeCount = activeTasks.size,
                    onTriggerTest5s = { viewModel.triggerTestReminder(5) },
                    onOpenPreview = onOpenLivePreview
                )
            }

            if (activeTasks.isEmpty()) {
                item {
                    InteractiveEmptyState(
                        title = "No Upcoming Reminders",
                        subtitle = "You are all caught up! Tap '+ Add Reminder' below to set up an alarm.",
                        onCreateClick = { showCreateSheet = true },
                        onTemplateClick = { template -> viewModel.addFromTemplate(template) },
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            } else {
                // Overdue Section
                if (overdueReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Overdue",
                            count = overdueReminders.size,
                            color = AuraRoseUrgent
                        )
                    }
                    items(overdueReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }

                // Today Section
                if (todayReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Today",
                            count = todayReminders.size,
                            color = AuraIrisPrimary
                        )
                    }
                    items(todayReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onEdit = { editingTask = task },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }

                // Tomorrow Section
                if (tomorrowReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Tomorrow",
                            count = tomorrowReminders.size,
                            color = AuraCyanAccent
                        )
                    }
                    items(tomorrowReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onEdit = { editingTask = task },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }

                // Later This Week Section
                if (laterReminders.isNotEmpty()) {
                    item {
                        TimelineSectionHeader(
                            title = "Upcoming Later",
                            count = laterReminders.size,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(laterReminders, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id, task.isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onSnooze15m = { viewModel.snoozeTask(task.id, 15) },
                                onEdit = { editingTask = task },
                                onSubtaskToggle = { subtask -> viewModel.toggleSubtask(task.id, subtask) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Reminder right from the Scheduled page
        FloatingActionButton(
            onClick = { showCreateSheet = true },
            containerColor = AuraIrisPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .size(56.dp)
                .testTag("fab_add_scheduled_reminder")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new reminder",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showCreateSheet) {
        TaskEditorBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            onSaveTask = { title, details, category, priority, reminderTime, isRecurring, subtasks ->
                viewModel.addNewTask(
                    title = title,
                    details = details,
                    category = category,
                    priority = priority,
                    reminderTimeMillis = reminderTime,
                    isRecurringDaily = isRecurring,
                    subtasks = subtasks
                )
                showCreateSheet = false
            }
        )
    }

    editingTask?.let { task ->
        TaskEditorBottomSheet(
            existingTask = task,
            onDismissRequest = { editingTask = null },
            onSaveTask = { title, details, category, priority, reminderTime, isRecurring, subtasks ->
                viewModel.updateExistingTask(
                    taskId = task.id,
                    title = title,
                    details = details,
                    category = category,
                    priority = priority,
                    reminderTimeMillis = reminderTime,
                    isRecurringDaily = isRecurring,
                    subtasks = subtasks
                )
                editingTask = null
            }
        )
    }
}

@Composable
fun RemindersHeroBanner(
    activeCount: Int,
    onTriggerTest5s: () -> Unit,
    onOpenPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AuraIrisPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Lockscreen Alarms",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$activeCount active alarms scheduled",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "When time triggers, the full-screen alert overlay wakes your device even when locked.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTriggerTest5s,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraIrisPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("trigger_test_alarm_btn")
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test in 5s", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onOpenPreview,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("preview_alert_screen_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview Alert", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TimelineSectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
